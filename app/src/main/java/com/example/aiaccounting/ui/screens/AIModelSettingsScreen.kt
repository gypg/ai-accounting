package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.data.local.prefs.AppStateManager

/**
 * AI 模型设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIModelSettingsScreen(
    appStateManager: AppStateManager,
    onNavigateBack: () -> Unit
) {
    var selectedModelType by remember { mutableStateOf(appStateManager.getAIModelType()) }
    var customModelUrl by remember { mutableStateOf(appStateManager.getCustomModelUrl()) }
    var customModelApiKey by remember { mutableStateOf(appStateManager.getCustomModelApiKey()) }
    var showApiKey by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 模型设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            appStateManager.setAIModelType(selectedModelType)
                            appStateManager.setCustomModelUrl(customModelUrl)
                            appStateManager.setCustomModelApiKey(customModelApiKey)
                            showSaveSuccess = true
                        }
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 说明卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI 模型配置",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "选择使用默认的本地 AI 解析模型，或配置自定义的云端模型 API。自定义模型可以提供更精准的解析效果，但需要提供有效的 API 地址和密钥。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // 模型选择
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "选择 AI 模型",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 默认模型选项
                    ModelOption(
                        title = "默认本地模型",
                        subtitle = "使用内置的自然语言处理模型，无需网络连接",
                        icon = Icons.Default.PhoneAndroid,
                        selected = selectedModelType == AppStateManager.AI_MODEL_DEFAULT,
                        onClick = { selectedModelType = AppStateManager.AI_MODEL_DEFAULT }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 自定义模型选项
                    ModelOption(
                        title = "自定义云端模型",
                        subtitle = "使用第三方 AI 服务 API，如 OpenAI、Claude 等",
                        icon = Icons.Default.Cloud,
                        selected = selectedModelType == AppStateManager.AI_MODEL_CUSTOM,
                        onClick = { selectedModelType = AppStateManager.AI_MODEL_CUSTOM }
                    )
                }
            }

            // 自定义模型配置
            if (selectedModelType == AppStateManager.AI_MODEL_CUSTOM) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "自定义模型配置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // API 地址
                        OutlinedTextField(
                            value = customModelUrl,
                            onValueChange = { customModelUrl = it },
                            label = { Text("API 地址") },
                            placeholder = { Text("https://api.example.com/v1/chat/completions") },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // API 密钥
                        OutlinedTextField(
                            value = customModelApiKey,
                            onValueChange = { customModelApiKey = it },
                            label = { Text("API 密钥") },
                            placeholder = { Text("sk-...") },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showApiKey) "隐藏" else "显示"
                                    )
                                }
                            },
                            visualTransformation = if (showApiKey) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                androidx.compose.ui.text.input.PasswordVisualTransformation()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 测试连接按钮
                        Button(
                            onClick = { /* TODO: 测试连接 */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试连接")
                        }
                    }
                }

                // 支持的模型说明
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "支持的模型格式",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• OpenAI GPT-3.5/GPT-4\n• Claude 3 (Anthropic)\n• 文心一言 (百度)\n• 通义千问 (阿里)\n• 其他兼容 OpenAI API 格式的服务",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 保存成功提示
    if (showSaveSuccess) {
        androidx.compose.material3.Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showSaveSuccess = false }) {
                    Text("确定")
                }
            }
        ) {
            Text("设置已保存")
        }

        LaunchedEffect(showSaveSuccess) {
            kotlinx.coroutines.delay(2000)
            showSaveSuccess = false
        }
    }
}

@Composable
fun ModelOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) {
            BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
