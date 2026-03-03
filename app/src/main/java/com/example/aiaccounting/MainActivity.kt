package com.example.aiaccounting

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.security.SecurityManager
import com.example.aiaccounting.ui.navigation.AppNavigation
import com.example.aiaccounting.ui.navigation.Screen
import com.example.aiaccounting.ui.theme.AIAccountingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var appStateManager: AppStateManager

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "需要存储权限才能导出文件", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AIAccountingTheme {
                MainApp(
                    securityManager = securityManager,
                    appStateManager = appStateManager,
                    onRequestStoragePermission = { requestStoragePermission() }
                )
            }
        }
    }

    private fun requestStoragePermission() {
        when {
            // Android 13+ (API 33+) - Use granular media permissions
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                // For Android 13+, we need to request READ_MEDIA_IMAGES permission
                // or use Storage Access Framework for saving to Downloads
                // For now, we use app-specific directory which doesn't need permission
                // If you need to save to Downloads, implement Storage Access Framework
            }
            // Android 10+ (API 29+) - Scoped storage
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                // Use app-specific directory or Storage Access Framework
                // No permission needed for app-specific directories
            }
            // Android 9 and below
            else -> {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}

@Composable
fun MainApp(
    securityManager: SecurityManager,
    appStateManager: AppStateManager,
    onRequestStoragePermission: () -> Unit
) {
    // 使用持久化的状态
    var isPinSet by remember { mutableStateOf(securityManager.isPinSet()) }
    var isDatabaseInitialized by remember { mutableStateOf(appStateManager.isDatabaseInitialized()) }
    var hasInitialSetup by remember { mutableStateOf(appStateManager.isInitialSetupCompleted()) }

    // Determine start destination - 跳过PIN码验证
    val startDestination = when {
        !hasInitialSetup -> Screen.InitialSetup.route
        else -> "overview"  // 主界面使用底部导航，默认显示总览
    }

    val navController = rememberNavController()

    // Global PIN holder for database initialization
    var globalPin by remember { mutableStateOf<String?>(null) }

    AppNavigation(
        navController = navController,
        startDestination = startDestination,
        appStateManager = appStateManager,
        onSetupComplete = { pin ->
            // PIN setup completed
            isPinSet = true
            globalPin = pin
            // 持久化数据库初始化状态
            appStateManager.setDatabaseInitialized(true)
            isDatabaseInitialized = true
        },
        onLoginSuccess = { pin ->
            // Login successful
            globalPin = pin
            // 持久化数据库初始化状态
            appStateManager.setDatabaseInitialized(true)
            isDatabaseInitialized = true
        },
        onInitialSetupComplete = {
            // 持久化初始设置完成状态
            appStateManager.setInitialSetupCompleted(true)
            hasInitialSetup = true
        }
    )
}
