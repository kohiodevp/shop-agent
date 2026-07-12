package com.shop.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shop.agent.ui.ShopDarkColorScheme
import com.shop.agent.ui.ShopLightColorScheme
import com.shop.agent.ui.ShopViewModel
import com.shop.agent.ui.screens.ShopScreens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ShopApplication
        setContent {
            ShopTheme {
                val vm: ShopViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        ShopViewModel(app.db) as T
                })
                ShopScreens(vm)
            }
        }
    }
}

@Composable
fun ShopTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) ShopDarkColorScheme else ShopLightColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
