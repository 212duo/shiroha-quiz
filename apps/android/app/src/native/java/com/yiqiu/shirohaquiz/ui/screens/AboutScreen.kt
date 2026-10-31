package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "About",
            title = "关于 Shiroha Quiz",
            subtitle = "我是 Shiroha，欢迎关注。"
        )

        GlassCard {
            Text(
                text = "项目地址",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "https://github.com/reiqr/shiroha-quiz",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            ActionPillButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                text = "打开 GitHub 项目页",
                primary = true,
                modifier = Modifier.height(42.dp),
                onClick = { uriHandler.openUri("https://github.com/reiqr/shiroha-quiz") }
            )
        }

        GlassCard {
            Text(
                text = "项目说明",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Shiroha Quiz 是一个面向本地题库导入、练习、考试、错题复习和学习记录管理的刷题工具。原生安卓版本重点优化移动端操作体验、题库核对和本地数据管理。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlassCard {
            Text(
                text = "开源与数据",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "题库、错题本和学习记录默认保存在本机。发布包不应包含用户本地导入数据。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "本安装包是 Shiroha Quiz 的 GPL-3.0 衍生版「焊工刷题」，首次打开会载入预置的熔化焊接与热切割作业理论题库。以后更新安装包时，只替换这份预置题库，自己导入的题库不动。上游项目、贡献者与许可证以本页链接和仓库 LICENSE 为准。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ActionPillButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            text = "返回设置",
            primary = false,
            modifier = Modifier.height(42.dp),
            onClick = onBack
        )
    }
}
