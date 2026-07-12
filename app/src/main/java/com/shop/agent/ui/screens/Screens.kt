package com.shop.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.agent.data.Product
import com.shop.agent.data.Sale
import com.shop.agent.ui.ShopViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreens(vm: ShopViewModel) {
    var tab by remember { mutableStateOf(0) }
    val cartCount by vm.cartCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar messages
    LaunchedEffect(Unit) {
        vm.uiMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Shop Agent", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Inventory2, null) },
                    label = { Text("Catalogue") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (cartCount > 0) Badge { Text(cartCount.toString()) }
                        }) { Icon(Icons.Filled.ShoppingCart, null) }
                    },
                    label = { Text("Panier") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.ReceiptLong, null) },
                    label = { Text("Ventes") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (tab) {
            0 -> CatalogueTab(vm, Modifier.padding(padding))
            1 -> CartTab(vm, Modifier.padding(padding))
            2 -> SalesTab(vm, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueTab(vm: ShopViewModel, modifier: Modifier = Modifier) {
    val products by vm.products.collectAsState()
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Nouveau produit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Nom du produit") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Prix (FCFA)") }, modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stock") }, modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Button(
                onClick = {
                    val p = price.toDoubleOrNull(); val s = stock.toIntOrNull()
                    if (name.isNotBlank() && p != null && s != null && p > 0) {
                        vm.addProduct(name.trim(), p, s); name = ""; price = ""; stock = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(12.dp)
            ) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Ajouter au catalogue", fontSize = 16.sp) }
            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))
            Text("${products.size} article(s) en stock", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (products.isEmpty()) {
            item {
                EmptyState(Icons.Filled.Inventory2, "Aucun produit", "Ajoute ton premier article ci-dessus")
            }
        } else {
            items(products) { p -> ProductCard(p) { vm.addToCart(p) } }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun ProductCard(p: Product, onAdd: () -> Unit) {
    val out = p.stock <= 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${p.price.toInt()} FCFA", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(if (out) "Rupture de stock" else "Stock: ${p.stock}", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (out) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        labelColor = if (out) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                )
            }
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onAdd, modifier = Modifier.size(52.dp),
                containerColor = if (out) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Filled.AddShoppingCart, if (out) "Indisponible" else "Ajouter au panier") }
        }
    }
}

@Composable
fun CartTab(vm: ShopViewModel, modifier: Modifier = Modifier) {
    val lines by vm.cartLines.collectAsState()
    val total by vm.cartTotal.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            if (lines.isEmpty()) {
                item { EmptyState(Icons.Filled.ShoppingCart, "Panier vide", "Ajoute des produits depuis le Catalogue") }
            }
            items(lines) { line ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.product.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("${line.qty} x ${line.product.price.toInt()} = ${(line.qty * line.product.price).toInt()} FCFA",
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.removeFromCart(line.product.id) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.RemoveCircle, "Retirer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${total.toInt()} FCFA", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.checkout() }, modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp), enabled = lines.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Icon(Icons.Filled.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("Valider la vente", fontSize = 17.sp) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.clearCart() }, modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp), enabled = lines.isNotEmpty()
                ) { Text("Vider le panier") }
            }
        }
    }
}

@Composable
fun SalesTab(vm: ShopViewModel, modifier: Modifier = Modifier) {
    val sales by vm.sales.collectAsState()
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        if (sales.isEmpty()) {
            item { EmptyState(Icons.Filled.ReceiptLong, "Aucune vente", "Les ventes validées apparaîtront ici") }
        }
        items(sales) { s: Sale ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vente #${s.id}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(s.timestamp.toString().take(19).replace("T", " "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${s.total.toInt()} FCFA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}
