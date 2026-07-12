package com.shop.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    LaunchedEffect(Unit) {
        vm.uiMessage.collectLatest { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Shop Agent", fontWeight = FontWeight.Bold, fontSize = 21.sp)
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
                    icon = { Icon(Icons.Filled.Inventory2, null) }, label = { Text("Catalogue") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = {
                        BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                            Icon(Icons.Filled.ShoppingCart, null)
                        }
                    },
                    label = { Text("Panier") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.ReceiptLong, null) }, label = { Text("Ventes") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.ShoppingCart, null) },
                    text = { Text("Panier ($cartCount)") },
                    containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White
                )
            }
        }
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
    val products by vm.filteredProducts.collectAsState()
    val query by vm.productQuery.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<Product?>(null) }

    editTarget?.let { p ->
        EditProductDialog(
            product = p,
            onDismiss = { editTarget = null },
            onSave = { n, pr, st -> vm.updateProduct(p.id, n, pr, st); editTarget = null },
            onDelete = { vm.deleteProduct(p); editTarget = null },
            onRestock = { q -> vm.restockProduct(p.id, q); editTarget = null }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { vm.setProductQuery(it) },
                label = { Text("Rechercher un produit") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { vm.setProductQuery("") }) { Icon(Icons.Filled.Clear, null) } }
                } else null
            )
        }
        item {
            Card(
                onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AddBusiness, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Text("Ajouter un produit", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        if (expanded) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du produit") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Prix (FCFA)") }, modifier = Modifier.weight(1f), singleLine = true,
                                shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } },
                                label = { Text("Stock") }, modifier = Modifier.weight(1f), singleLine = true,
                                shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val p = price.toDoubleOrNull(); val s = stock.toIntOrNull()
                                if (name.isNotBlank() && p != null && s != null && p > 0) {
                                    vm.addProduct(name.trim(), p, s); name = ""; price = ""; stock = ""; expanded = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)
                        ) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("Confirmer", fontSize = 16.sp) }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("${products.size} article(s)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (products.isEmpty()) {
            item { EmptyState(Icons.Filled.Inventory2, "Aucun produit", "Ajoute ou recherche un article") }
        } else {
            items(products) { p ->
                ProductCard(
                    p,
                    onAdd = { vm.addToCart(p) },
                    onEdit = { editTarget = p }
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun ProductCard(p: Product, onAdd: () -> Unit, onEdit: () -> Unit) {
    val out = p.stock <= 0
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sell, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("${p.price.toInt()} FCFA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
                AssistChip(
                    onClick = {}, label = { Text(if (out) "Rupture" else "Stock: ${p.stock}", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (out) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        labelColor = if (out) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Edit, "Modifier", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FloatingActionButton(
                onClick = onAdd, modifier = Modifier.size(50.dp),
                containerColor = if (out) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Filled.AddShoppingCart, if (out) "Indisponible" else "Ajouter") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int) -> Unit,
    onDelete: () -> Unit,
    onRestock: (Int) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var stock by remember { mutableStateOf(product.stock.toString()) }
    var restock by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ${product.name}") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Prix (FCFA)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } }, label = { Text("Stock") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                Text("Réapprovisionner", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = restock, onValueChange = { restock = it.filter { c -> c.isDigit() } }, label = { Text("+ qte") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { restock.toIntOrNull()?.let { onRestock(it) } }, enabled = restock.toIntOrNull() != null) { Text("+") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = price.toDoubleOrNull(); val s = stock.toIntOrNull()
                if (name.isNotBlank() && p != null && s != null && p > 0) onSave(name.trim(), p, s)
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Supprimer") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
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
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(line.product.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.removeFromCart(line.product.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Delete, "Retirer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.incCartQty(line.product.id, -1) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.RemoveCircle, "Moins", tint = MaterialTheme.colorScheme.primary)
                                }
                                Text("${line.qty}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { vm.incCartQty(line.product.id, 1) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.AddCircle, "Plus", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("${(line.qty * line.product.price).toInt()} FCFA", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesTab(vm: ShopViewModel, modifier: Modifier = Modifier) {
    val sales by vm.filteredSales.collectAsState()
    val query by vm.saleQuery.collectAsState()
    val allSales by vm.sales.collectAsState()
    val totalCumule = allSales.sumOf { it.total.toInt() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { vm.setSaleQuery(it) },
                label = { Text("Rechercher une vente") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { vm.setSaleQuery("") }) { Icon(Icons.Filled.Clear, null) } }
                } else null
            )
        }
        item {
            if (allSales.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { vm.clearSales() }) { Text("Effacer l'historique", fontSize = 13.sp) }
                }
            }
        }
        if (sales.isEmpty()) {
            item { EmptyState(Icons.Filled.ReceiptLong, "Aucune vente", "Les ventes validées apparaîtront ici") }
        }
        items(sales) { s: Sale ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Vente #${s.id}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(s.timestamp.toString().take(19).replace("T", " "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${s.total.toInt()} FCFA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
        if (allSales.isNotEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total cumulé (${allSales.size} ventes)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$totalCumule FCFA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}
