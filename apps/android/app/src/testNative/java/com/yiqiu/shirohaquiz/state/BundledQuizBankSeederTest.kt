package com.yiqiu.shirohaquiz.state

import android.content.Context
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
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
    fun parsesBundledBankInto3154Questions() {
        val parsed = parseBundledBank()
        assertEquals("熔化焊接与热切割作业·理论题库v3", parsed.name)
        assertEquals("焊工证", parsed.groupName)
        assertEquals(3154, parsed.questions.size)
        assertEquals(1678, parsed.questions.count { it.type == QuestionType.JUDGE })
        assertEquals(1476, parsed.questions.count { it.type == QuestionType.SINGLE })
        val first = parsed.questions.first()
        assertEquals("1", first.number)
        assertTrue(first.question.startsWith("特种作业人员必须经专门安全技术培训"))
        assertEquals(listOf("A", "B"), first.options.map { it.key })
        assertEquals(listOf("正确", "错误"), first.options.map { it.text })
        assertEquals(listOf("正确"), first.answer)
    }

    @Test
    fun freshInstallSeedsOnceAndLaterLaunchesDoNotDuplicate() {
        assertTrue(QuizRepository.seedBundledQuestionBank(context))
        val seeded = QuizRepository.banks.single()
        assertEquals("熔化焊接与热切割作业·理论题库v3", seeded.name)
        assertEquals("焊工证", seeded.groupName)
        assertEquals(3154, seeded.questions.size)
        assertEquals(1678, seeded.questions.count { it.type == QuestionType.JUDGE })
        assertEquals(1476, seeded.questions.count { it.type == QuestionType.SINGLE })
        assertEquals(seeded.id, QuizRepository.activeBankId)
        assertEquals(QuizRepository.BUNDLED_QUIZBANK_SEED_ID, preferences.getString(SEED_KEY, null))
        assertEquals(seeded.id, preferences.getString(BANK_KEY, null))
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

        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals(3154, QuizRepository.banks.single().questions.size)
        assertFalse(QuizRepository.seedBundledQuestionBank(context))
        assertEquals(1, QuizRepository.banks.size)

        QuizRepository.deleteBank(context, QuizRepository.banks.single().id)
        assertTrue(QuizRepository.banks.isEmpty())
        assertFalse(QuizRepository.seedBundledQuestionBank(context))
        assertTrue(QuizRepository.banks.isEmpty())
    }

    @Test
    fun upgradeFromV2ReplacesSeededBankWithoutDuplicatingOrTouchingManualBanks() {
        QuizRepository.init(context)
        val parsed = parseBundledBank()
        val kept = parsed.questions.first()
        QuizRepository.importBank(
            context = context,
            name = "熔化焊接与热切割作业·理论题库v2",
            questions = v2SizedQuestions(kept.copy(analysis = "旧解析")),
            groupName = "焊工证"
        )
        val previous = QuizRepository.banks.single()
        val keptQuestion = previous.questions.first()
        assertEquals(kept.id, keptQuestion.id)
        QuizRepository.wrongBook += WrongQuestionEntry(
            bankId = previous.id,
            bankName = previous.name,
            question = keptQuestion,
            lastAnswer = listOf("错误"),
            source = "practice",
            timestamp = 1000L
        )
        QuizRepository.favoriteQuestions += FavoriteQuestionEntry(
            bankId = previous.id,
            bankName = previous.name,
            question = keptQuestion,
            favoritedAt = 2000L
        )
        QuizRepository.practiceSequentialProgress["BANK:${previous.id}"] = 20
        QuizRepository.importBank(
            context = context,
            name = "自编题库",
            questions = listOf(placeholderQuestion(9000)),
            groupName = "我的导入"
        )
        val manual = QuizRepository.banks.single { it.name == "自编题库" }
        preferences.edit().putString(SEED_KEY, "rhhj-quizbank-v2").commit()

        assertTrue(QuizRepository.seedBundledQuestionBank(context))

        assertEquals(2, QuizRepository.banks.size)
        val upgraded = QuizRepository.banks.single { it.id == previous.id }
        assertEquals("熔化焊接与热切割作业·理论题库v3", upgraded.name)
        assertEquals("焊工证", upgraded.groupName)
        assertEquals(3154, upgraded.questions.size)
        assertEquals(kept.id, upgraded.questions.first().id)
        assertEquals(kept.analysis, upgraded.questions.first().analysis)
        val manualAfter = QuizRepository.banks.single { it.id == manual.id }
        assertEquals("自编题库", manualAfter.name)
        assertEquals(1, manualAfter.questions.size)
        assertEquals(manual.questions.first().question, manualAfter.questions.first().question)

        val wrong = QuizRepository.wrongBook.single()
        assertEquals(previous.id, wrong.bankId)
        assertEquals(kept.id, wrong.question.id)
        assertEquals(kept.analysis, wrong.question.analysis)
        assertEquals(listOf("错误"), wrong.lastAnswer)
        val favorite = QuizRepository.favoriteQuestions.single()
        assertEquals(previous.id, favorite.bankId)
        assertEquals(kept.id, favorite.question.id)
        assertEquals(2000L, favorite.favoritedAt)
        assertEquals(20, QuizRepository.practiceSequentialProgress["BANK:${previous.id}"])
        assertEquals(QuizRepository.BUNDLED_QUIZBANK_SEED_ID, preferences.getString(SEED_KEY, null))
        assertEquals(previous.id, preferences.getString(BANK_KEY, null))

        assertFalse(QuizRepository.seedBundledQuestionBank(context))
        assertEquals(2, QuizRepository.banks.size)
        assertEquals(3154, QuizRepository.banks.single { it.id == previous.id }.questions.size)

        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertFalse(QuizRepository.seedBundledQuestionBank(context))
        assertEquals(2, QuizRepository.banks.size)
        assertEquals(3154, QuizRepository.banks.single { it.id == previous.id }.questions.size)
        assertEquals(1, QuizRepository.banks.single { it.id == manual.id }.questions.size)
        assertEquals(1, QuizRepository.wrongBook.size)
        assertEquals(kept.id, QuizRepository.wrongBook.single().question.id)
    }

    @Test
    fun renamedV2BankIsReplacedInPlaceAndADeletedV2BankDoesNotConsumeManualBanks() {
        QuizRepository.init(context)
        val parsed = parseBundledBank()
        QuizRepository.importBank(
            context = context,
            name = "熔化焊接与热切割作业·理论题库v2",
            questions = v2SizedQuestions(parsed.questions.first()),
            groupName = "焊工证"
        )
        val previousId = QuizRepository.banks.single().id
        assertTrue(QuizRepository.renameBank(context, previousId, "我的焊工题库"))
        QuizRepository.importBank(
            context = context,
            name = "自编题库",
            questions = listOf(placeholderQuestion(1), placeholderQuestion(2)),
            groupName = "我的导入"
        )
        preferences.edit().putString(SEED_KEY, "rhhj-quizbank-v2").commit()

        assertTrue(QuizRepository.seedBundledQuestionBank(context))
        assertEquals(2, QuizRepository.banks.size)
        val upgraded = QuizRepository.banks.single { it.id == previousId }
        assertEquals("我的焊工题库", upgraded.name)
        assertEquals(3154, upgraded.questions.size)
        assertEquals(2, QuizRepository.banks.single { it.name == "自编题库" }.questions.size)
        assertFalse(QuizRepository.seedBundledQuestionBank(context))

        QuizRepository.resetForTesting()
        preferences.edit().clear().commit()
        QuizRepository.importBank(
            context = context,
            name = "自编题库",
            questions = listOf(placeholderQuestion(3)),
            groupName = "我的导入"
        )
        val manualId = QuizRepository.banks.single().id
        preferences.edit().putString(SEED_KEY, "rhhj-quizbank-v2").commit()

        assertTrue(QuizRepository.seedBundledQuestionBank(context))
        assertEquals(2, QuizRepository.banks.size)
        assertEquals(1, QuizRepository.banks.single { it.id == manualId }.questions.size)
        assertEquals(3154, QuizRepository.banks.single { it.name == "熔化焊接与热切割作业·理论题库v3" }.questions.size)
    }

    private fun parseBundledBank(): QuizBank {
        val assetText = context.assets.open(QuizRepository.BUNDLED_QUIZBANK_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return QuizRepository.parseImportJsonBank(assetText) ?: error("bundled bank did not parse")
    }

    private fun v2SizedQuestions(kept: Question): List<Question> {
        return listOf(kept) + List(1783) { index -> placeholderQuestion(index) }
    }

    private fun placeholderQuestion(index: Int): Question {
        return Question(
            number = "placeholder-$index",
            type = QuestionType.JUDGE,
            question = "占位题$index，仅用于模拟旧版预置题库，不应与正式题干相同。",
            options = listOf(Option(key = "A", text = "正确"), Option(key = "B", text = "错误")),
            answer = listOf("正确")
        )
    }

    private companion object {
        const val SEED_KEY = "bundled_quizbank_seed_id"
        const val BANK_KEY = "bundled_quizbank_bank_id"
    }
}
