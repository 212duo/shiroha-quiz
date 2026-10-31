package com.yiqiu.shirohaquiz.state

import android.content.Context
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.security.TestKeystoreRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BundledQuizBankSeederTest {
    @get:Rule val keystore = TestKeystoreRule()

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val preferences get() = context.getSharedPreferences("shiroha_quiz_native", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        QuizRepository.resetForTesting()
        preferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        QuizRepository.resetForTesting()
        preferences.edit().clear().commit()
    }

    @Test
    fun seedParsesBundledBankInto1784QuestionsAndDoesNotDuplicate() {
        val assetText = context.assets.open(QuizRepository.BUNDLED_QUIZBANK_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val parsed = QuizRepository.parseImportJsonBank(assetText)
        assertEquals("熔化焊接与热切割作业·理论题库v2", parsed?.name)
        assertEquals("焊工证", parsed?.groupName)
        assertEquals(1784, parsed?.questions?.size)
        assertEquals(1295, parsed?.questions?.count { it.type == QuestionType.JUDGE })
        assertEquals(489, parsed?.questions?.count { it.type == QuestionType.SINGLE })
        val first = parsed!!.questions.first()
        assertEquals("1", first.number)
        assertTrue(first.question.startsWith("特种作业人员必须经专门安全技术培训"))
        assertEquals(listOf("A", "B"), first.options.map { it.key })
        assertEquals(listOf("正确", "错误"), first.options.map { it.text })
        assertEquals(listOf("正确"), first.answer)

        assertTrue(QuizRepository.seedBundledQuestionBank(context))
        val seeded = QuizRepository.banks.single()
        assertEquals(parsed.name, seeded.name)
        assertEquals(parsed.groupName, seeded.groupName)
        assertEquals(1784, seeded.questions.size)
        assertEquals(seeded.id, QuizRepository.activeBankId)
        assertTrue(
            QuizRepository.startPracticeSession(
                questionCount = 5,
                allowedTypes = setOf(QuestionType.JUDGE, QuestionType.SINGLE),
                randomize = false
            )
        )
        assertEquals(5, QuizRepository.practiceQuestions.size)

        assertFalse(QuizRepository.seedBundledQuestionBank(context))
        assertEquals(1, QuizRepository.banks.size)
        assertEquals(1784, QuizRepository.banks.single().questions.size)

        assertTrue(QuizRepository.renameBank(context, seeded.id, "我的焊工题库"))
        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals(1, QuizRepository.banks.size)
        assertEquals(1784, QuizRepository.banks.single().questions.size)
        assertEquals("我的焊工题库", QuizRepository.banks.single().name)
        assertFalse(QuizRepository.seedBundledQuestionBank(context))
        assertEquals(1, QuizRepository.banks.size)
        assertEquals("我的焊工题库", QuizRepository.banks.single().name)

        QuizRepository.deleteBank(context, QuizRepository.banks.single().id)
        assertTrue(QuizRepository.banks.isEmpty())
        assertFalse(QuizRepository.seedBundledQuestionBank(context))
        assertTrue(QuizRepository.banks.isEmpty())
    }
}
