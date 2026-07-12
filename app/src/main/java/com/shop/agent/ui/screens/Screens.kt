package com.shop.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shop.agent.data.Product
import com.shop.agent.data.Sale
import com.shop.agent.ui.ShopViewModel

@Composable
fun ShopScreens(vm: ShopViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("Catalogue") }
            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("Panier") }
            Tab(selected = tab == 2, onClick = { tab = 2 }) { Text("Ventes") }
        }
        when (tab) {
            0 -> CatalogueTab(vm)
            1 -> CartTab(vm)
            2 -> SalesTab(vm)
        }
    }
}

@Composable
fun CatalogueTab(vm: ShopViewModel) {
    val products by vm.products.collectAsState()
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        Text("Ajouter un produit", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(price, { price = it }, label = { Text("Prix") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val p = price.toDoubleOrNull(); val s = stock.toIntOrNull()
            if (name.isNotBlank() && p != null && s != null) { vm.addProduct(name, p, s); name=""; price=""; stock="" }
        }) { Text("Ajouter") }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(products) { p ->
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(p.name); Text("${p.price} FCFA x${p.stock}") }
                    Button(onClick = { vm.addToCart(p) }) { Text("+") }
                }
            }
        }
    }
}

@Composable
fun CartTab(vm: ShopViewModel) {
    val lines by vm.cartLines.collectAsState()
    val total by vm.cartTotal.collectAsState()
    Column(Modifier.padding(16.dp)) {
        LazyColumn {
            items(lines) { line ->
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${line.product.name} x${line.qty}")
                    Text("${line.qty * line.product.price} FCFA")
                    Button(onClick = { vm.removeFromCart(line.product.id) }) { Text("-") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Total: ${total} FCFA", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { vm.checkout() }) { Text("Valider la vente") }
        Button(onClick = { vm.clearCart() }) { Text("Vider") }
    }
}

@Composable
fun SalesTab(vm: ShopViewModel) {
    val sales by vm.sales.collectAsState()
    LazyColumn(Modifier.padding(16.dp)) {
        items(sales) { s: Sale ->
            Card(Modifier.fillMaxWidth().padding(8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Vente #${s.id} — ${s.total} FCFA")
                    Text(s.timestamp.toString())
                }
            }
        }
    }
}
