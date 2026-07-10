package com.shop.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shop.agent.ui.TailorViewModel
import com.shop.agent.ui.screens.HomeScreen
import com.shop.agent.ui.screens.ClientsScreen
import com.shop.agent.ui.screens.ClientDetailScreen
import com.shop.agent.ui.screens.OrdersScreen
import com.shop.agent.ui.screens.ShopScreen
import com.shop.agent.ui.screens.ProductsScreen
import com.shop.agent.ui.screens.ProductDetailScreen
import com.shop.agent.ui.screens.ShopCustomersScreen
import com.shop.agent.ui.screens.NewSaleScreen
import com.shop.agent.ui.screens.SalesScreen
import com.shop.agent.ui.screens.StockScreen
import com.shop.agent.ui.screens.SuppliersScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = TailorViewModel(application)
        setContent {
            MaterialTheme {
                Surface {
                    ShopNav(viewModel)
                }
            }
        }
    }
}

@Composable
fun ShopNav(viewModel: TailorViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav, viewModel) }
        composable("clients") { ClientsScreen(nav, viewModel) }
        composable("client/{id}") { back ->
            val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
            viewModel.selectClient(id)
            ClientDetailScreen(nav, viewModel, id)
        }
        composable("orders") { OrdersScreen(nav, viewModel) }
        // ---- Boutique ----
        composable("shop") { ShopScreen(nav, viewModel) }
        composable("products") { ProductsScreen(nav, viewModel) }
        composable("product/{id}") { back ->
            val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
            ProductDetailScreen(nav, viewModel, id)
        }
        composable("shop_customers") { ShopCustomersScreen(nav, viewModel) }
        composable("new_sale") { NewSaleScreen(nav, viewModel) }
        composable("sales") { SalesScreen(nav, viewModel) }
        composable("stock") { StockScreen(nav, viewModel) }
        composable("suppliers") { SuppliersScreen(nav, viewModel) }
    }
}
