package top.funcun.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import top.funcun.companion.plugin.onboarding.OnboardingPlugin
import top.funcun.companion.plugin.onboarding.PermissionScreen
import top.funcun.companion.shell.UIShell
import top.funcun.companion.theme.TianyiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TianyiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 权限引导门禁：首次启动先授予权限，完成后才加载主题主界面
                    var showOnboarding by remember {
                        mutableStateOf(!OnboardingPlugin.isOnboardingComplete(this@MainActivity))
                    }

                    if (showOnboarding) {
                        PermissionScreen(
                            hostContext = this@MainActivity,
                            onAllGranted = {
                                OnboardingPlugin.markOnboardingComplete(this@MainActivity)
                                showOnboarding = false
                            },
                        )
                    } else {
                        UIShell()
                    }
                }
            }
        }
    }
}