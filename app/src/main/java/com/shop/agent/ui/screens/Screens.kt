package com.shop.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shop.agent.data.*
import com.shop.agent.ui.SaleItemInput
import com.shop.agent.ui.TailorViewModel
import java.text.SimpleDateFormat
import java.util.*

// ===================== TAILLEUR (existant) =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    Scaffold(
        topBar = { TopAppBar(title = { Text("Shop Agent") }) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tableau de bord", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard("Clients", st.clients.size.toString(), Modifier.weight(1f)) { nav.navigate("clients") }
                DashboardCard("Commandes", st.allOrders.size.toString(), Modifier.weight(1f)) { nav.navigate("orders") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard("Produits", st.products.size.toString(), Modifier.weight(1f)) { nav.navigate("shop") }
                DashboardCard("Ventes", st.sales.size.toString(), Modifier.weight(1f)) { nav.navigate("sales") }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { nav.navigate("clients") }, Modifier.fillMaxWidth()) { Text("Gérer les clients (tailleur)") }
            Button(onClick = { nav.navigate("orders") }, Modifier.fillMaxWidth()) { Text("Suivre les commandes (tailleur)") }
            Button(onClick = { nav.navigate("shop") }, Modifier.fillMaxWidth()) { Text("Gérer la boutique") }
            if (st.message.isNotEmpty()) {
                Text(st.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(title: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }.padding(4.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Clients") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) },
        floatingActionButton = { FloatingActionButton({ showDialog = true }) { Icon(Icons.Filled.Add, "Ajouter") } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            items(st.clients) { c ->
                Card(Modifier.fillMaxWidth().padding(4.dp).clickable { nav.navigate("client/${c.id}") },
                    elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Person, "Client")
                        Column {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            if (c.phone.isNotEmpty()) Text(c.phone, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    if (showDialog) {
        ClientFormDialog(
            onDismiss = { showDialog = false },
            onSave = { name, phone, email, notes ->
                vm.addClient(name, phone, email, notes)
                showDialog = false
            }
        )
    }
}

@Composable
fun ClientFormDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau client") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom *") })
                OutlinedTextField(phone, { phone = it }, label = { Text("Téléphone") })
                OutlinedTextField(email, { email = it }, label = { Text("Email") })
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, phone, email, notes) }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(nav: NavController, vm: TailorViewModel, clientId: Long) {
    val st = vm.state.collectAsState().value
    val client = st.clients.firstOrNull { it.id == clientId }
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(client?.name ?: "Client") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(tab) {
                Tab(tab == 0, { tab = 0 }) { Text("Mesures") }
                Tab(tab == 1, { tab = 1 }) { Text("Commandes") }
            }
            when (tab) {
                0 -> MeasurementsTab(vm, clientId)
                1 -> OrdersTab(nav, vm, clientId)
            }
        }
    }
}

@Composable
fun MeasurementsTab(vm: TailorViewModel, clientId: Long) {
    val st = vm.state.collectAsState().value
    val m = st.measurements
    var cou by remember { mutableStateOf((m?.cou ?: 0.0).toString()) }
    var epaules by remember { mutableStateOf((m?.epaules ?: 0.0).toString()) }
    var poitrine by remember { mutableStateOf((m?.poitrine ?: 0.0).toString()) }
    var bras by remember { mutableStateOf((m?.bras ?: 0.0).toString()) }
    var taille by remember { mutableStateOf((m?.taille ?: 0.0).toString()) }
    var hanches by remember { mutableStateOf((m?.hanches ?: 0.0).toString()) }
    var longueur by remember { mutableStateOf((m?.longueurJambe ?: 0.0).toString()) }
    var msg by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Mesures complètes (cm)", style = MaterialTheme.typography.titleMedium) }
        item { MeasField("Cou", cou) { cou = it } }
        item { MeasField("Épaules", epaules) { epaules = it } }
        item { MeasField("Poitrine", poitrine) { poitrine = it } }
        item { MeasField("Bras", bras) { bras = it } }
        item { MeasField("Taille", taille) { taille = it } }
        item { MeasField("Hanches", hanches) { hanches = it } }
        item { MeasField("Longueur jambe", longueur) { longueur = it } }
        item {
            Button(onClick = {
                vm.saveMeasurements(
                    com.shop.agent.data.Measurement(
                        clientId = clientId,
                        cou = cou.toDoubleOrNull() ?: 0.0,
                        epaules = epaules.toDoubleOrNull() ?: 0.0,
                        poitrine = poitrine.toDoubleOrNull() ?: 0.0,
                        bras = bras.toDoubleOrNull() ?: 0.0,
                        taille = taille.toDoubleOrNull() ?: 0.0,
                        hanches = hanches.toDoubleOrNull() ?: 0.0,
                        longueurJambe = longueur.toDoubleOrNull() ?: 0.0
                    )
                )
                msg = "Mesures enregistrées"
            }, Modifier.fillMaxWidth()) { Text("Enregistrer les mesures") }
        }
        if (msg.isNotEmpty()) item { Text(msg, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun MeasField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun OrdersTab(nav: NavController, vm: TailorViewModel, clientId: Long) {
    val st = vm.state.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Button(onClick = { showDialog = true }, Modifier.fillMaxWidth().padding(8.dp)) { Text("Nouvelle commande") }
        LazyColumn {
            items(st.orders) { o ->
                Card(Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${o.garmentType} — Taille ${o.size}", style = MaterialTheme.typography.titleSmall)
                            Text("Tissu: ${o.fabricM} m", style = MaterialTheme.typography.bodySmall)
                            Text("Statut: ${o.status.label}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton({ vm.setOrderStatus(o.id, nextStatus(o.status)) }) { Icon(Icons.Filled.Check, "Avancer") }
                            IconButton({ vm.deleteOrder(o.id) }) { Icon(Icons.Filled.Delete, "Supprimer") }
                        }
                    }
                }
            }
        }
    }
    if (showDialog) {
        OrderFormDialog(
            garments = listOf("chemise", "pantalon", "costume", "robe", "jupe", "veste", "manteau"),
            onDismiss = { showDialog = false },
            onSave = { g, s, st ->
                vm.addOrder(clientId, g, s, OrderStatus.EN_ATTENTE, 0.0)
                showDialog = false
            }
        )
    }
}

fun nextStatus(s: OrderStatus): OrderStatus = when (s) {
    OrderStatus.EN_ATTENTE -> OrderStatus.COUPE
    OrderStatus.COUPE -> OrderStatus.COUTURE
    OrderStatus.COUTURE -> OrderStatus.ESSAYAGE
    OrderStatus.ESSAYAGE -> OrderStatus.LIVRE
    OrderStatus.LIVRE -> OrderStatus.LIVRE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormDialog(garments: List<String>, onDismiss: () -> Unit, onSave: (String, Int, OrderStatus) -> Unit) {
    var garment by remember { mutableStateOf(garments.firstOrNull() ?: "chemise") }
    var size by remember { mutableStateOf("40") }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle commande") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded, { expanded = !expanded }, Modifier) {
                    OutlinedTextField(garment, {}, readOnly = true, label = { Text("Vêtement") },
                        modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        garments.forEach {
                            DropdownMenuItem({ Text(it) }, onClick = { garment = it; expanded = false })
                        }
                    }
                }
                OutlinedTextField(size, { size = it }, label = { Text("Taille") })
            }
        },
        confirmButton = { Button(onClick = { onSave(garment, size.toIntOrNull() ?: 40, OrderStatus.EN_ATTENTE) }) { Text("Créer") } },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    Scaffold(
        topBar = { TopAppBar(title = { Text("Toutes les commandes") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            items(st.allOrders) { o ->
                Card(Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${o.garmentType} — T${o.size}", style = MaterialTheme.typography.titleSmall)
                            val client = st.clients.firstOrNull { it.id == o.clientId }
                            Text(client?.name ?: "Client #${o.clientId}", style = MaterialTheme.typography.bodySmall)
                        }
                        AssistChip(onClick = { vm.setOrderStatus(o.id, nextStatus(o.status)) },
                            label = { Text(o.status.label) }, leadingIcon = { Icon(Icons.Filled.Check, null) })
                    }
                }
            }
        }
    }
}

// ===================== BOUTIQUE / RETAIL =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    Scaffold(
        topBar = { TopAppBar(title = { Text("Boutique") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Gestion de la boutique", style = MaterialTheme.typography.headlineSmall)
            val low = vm.lowStockProducts()
            if (low.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⚠ Stock bas : ${low.size} produit(s)", style = MaterialTheme.typography.titleSmall)
                        low.take(3).forEach { Text("• ${it.name} (${it.stock}/${it.alertThreshold})") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("Produits", st.products.size.toString(), Modifier.weight(1f)) { nav.navigate("products") }
                DashboardCard("Clients", st.customers.size.toString(), Modifier.weight(1f)) { nav.navigate("shop_customers") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("Ventes", st.sales.size.toString(), Modifier.weight(1f)) { nav.navigate("sales") }
                DashboardCard("Stock", st.stockMovements.size.toString(), Modifier.weight(1f)) { nav.navigate("stock") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardCard("Fournisseurs", st.suppliers.size.toString(), Modifier.weight(1f)) { nav.navigate("suppliers") }
                DashboardCard("CA total", String.format(Locale.getDefault(), "%.0f", st.totalRevenue), Modifier.weight(1f)) { nav.navigate("sales") }
            }
            Button(onClick = { nav.navigate("new_sale") }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Store, null); Spacer(Modifier.width(8.dp)); Text("Enregistrer une vente") }
            Button(onClick = { nav.navigate("products") }, Modifier.fillMaxWidth()) { Text("Gérer les produits") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Produits") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) },
        floatingActionButton = { FloatingActionButton({ showDialog = true }) { Icon(Icons.Filled.Add, "Ajouter") } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            items(st.products) { p ->
                val low = vm.lowStockProducts().any { it.id == p.id }
                Card(Modifier.fillMaxWidth().padding(4.dp).clickable { nav.navigate("product/${p.id}") },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = if (low) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer) else CardDefaults.cardColors()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            Text("${p.category.label} — ${String.format(Locale.getDefault(), "%.0f", p.sellPrice)} F", style = MaterialTheme.typography.bodySmall)
                            Text("Stock: ${p.stock}" + if (low) " ⚠" else "", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ vm.removeProduct(p.id) }) { Icon(Icons.Filled.Delete, "Supprimer") }
                    }
                }
            }
        }
    }
    if (showDialog) {
        ProductFormDialog(onDismiss = { showDialog = false }, onSave = { name, cat, desc, buy, sell, stock, alert, barcode ->
            vm.addProduct(name, cat, desc, buy, sell, stock, alert, barcode)
            showDialog = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    onDismiss: () -> Unit,
    onSave: (String, ProductCategory, String, Double, Double, Int, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(ProductCategory.VETEMENT) }
    var desc by remember { mutableStateOf("") }
    var buy by remember { mutableStateOf("0") }
    var sell by remember { mutableStateOf("0") }
    var stock by remember { mutableStateOf("0") }
    var alert by remember { mutableStateOf("0") }
    var barcode by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau produit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom *") })
                ExposedDropdownMenuBox(expanded, { expanded = !expanded }, Modifier) {
                    OutlinedTextField(cat.label, {}, readOnly = true, label = { Text("Catégorie") }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        ProductCategory.values().forEach {
                            DropdownMenuItem({ Text(it.label) }, onClick = { cat = it; expanded = false })
                        }
                    }
                }
                OutlinedTextField(desc, { desc = it }, label = { Text("Description") })
                OutlinedTextField(buy, { buy = it }, label = { Text("Prix d'achat") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(sell, { sell = it }, label = { Text("Prix de vente") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(stock, { stock = it }, label = { Text("Stock initial") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(alert, { alert = it }, label = { Text("Seuil d'alerte") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(barcode, { barcode = it }, label = { Text("Code-barre (optionnel)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onSave(
                    name, cat, desc,
                    buy.toDoubleOrNull() ?: 0.0, sell.toDoubleOrNull() ?: 0.0,
                    stock.toIntOrNull() ?: 0, alert.toIntOrNull() ?: 0,
                    barcode.ifBlank { null }
                )
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(nav: NavController, vm: TailorViewModel, productId: Long) {
    val st = vm.state.collectAsState().value
    val p = st.products.firstOrNull { it.id == productId } ?: return
    var restockQty by remember { mutableStateOf("") }
    Scaffold(
        topBar = { TopAppBar(title = { Text(p.name) }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Catégorie: ${p.category.label}", style = MaterialTheme.typography.bodyMedium) }
            item { Text("Description: ${p.description.ifBlank { "—" }}") }
            item { Text("Prix achat: ${String.format(Locale.getDefault(), "%.0f", p.buyPrice)} F") }
            item { Text("Prix vente: ${String.format(Locale.getDefault(), "%.0f", p.sellPrice)} F") }
            item { Text("Marge: ${String.format(Locale.getDefault(), "%.0f", (p.sellPrice - p.buyPrice))} F") }
            item { Text("Stock: ${p.stock}  (seuil ${p.alertThreshold})" + if (p.stock <= p.alertThreshold) " ⚠" else "") }
            item { Text("Code-barre: ${p.barcode ?: "—"}") }
            item { Text("Ajouté le: ${p.addedAt}") }
            item {
                OutlinedTextField(restockQty, { restockQty = it }, label = { Text("Quantité à réapprovisionner") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            item {
                Button(onClick = {
                    val q = restockQty.toIntOrNull() ?: 0
                    if (q > 0) { vm.restockProduct(p.id, q, "réappro manuel"); restockQty = "" }
                }, Modifier.fillMaxWidth()) { Text("Réapprovisionner") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopCustomersScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Clients boutique") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) },
        floatingActionButton = { FloatingActionButton({ showDialog = true }) { Icon(Icons.Filled.Add, "Ajouter") } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            items(st.customers) { c ->
                Card(Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            if (c.phone.isNotEmpty()) Text(c.phone, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ vm.removeCustomer(c.id) }) { Icon(Icons.Filled.Delete, "Supprimer") }
                    }
                }
            }
        }
    }
    if (showDialog) {
        CustomerFormDialog(onDismiss = { showDialog = false }, onSave = { name, phone, email, address ->
            vm.addCustomer(name, phone, email, address)
            showDialog = false
        })
    }
}

@Composable
fun CustomerFormDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau client") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom *") })
                OutlinedTextField(phone, { phone = it }, label = { Text("Téléphone") })
                OutlinedTextField(email, { email = it }, label = { Text("Email") })
                OutlinedTextField(address, { address = it }, label = { Text("Adresse") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, phone, email.ifBlank { null }, address.ifBlank { null }) }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    var selectedCustomer by remember { mutableStateOf<Long?>(null) }
    var expandedCustomer by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(listOf<SaleItemInput>()) }
    var payment by remember { mutableStateOf(PaymentMode.ESPECE) }
    var expandedPayment by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var addProductId by remember { mutableStateOf<Long?>(null) }
    var addQty by remember { mutableStateOf("1") }
    var addDiscount by remember { mutableStateOf("0") }
    var expandedProduct by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    val total = com.shop.agent.engine.TailorEngine.calculateSaleTotal(
        items.map { SaleItem(saleId = 0, productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice, discount = it.discount) }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nouvelle vente") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Client (optionnel)", style = MaterialTheme.typography.titleSmall) }
            item {
                ExposedDropdownMenuBox(expandedCustomer, { expandedCustomer = !expandedCustomer }, Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = st.customers.firstOrNull { it.id == selectedCustomer }?.name ?: "Vente comptoir",
                        onValueChange = {}, readOnly = true, label = { Text("Client") }, modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expandedCustomer, { expandedCustomer = false }) {
                        DropdownMenuItem({ Text("Vente comptoir") }, onClick = { selectedCustomer = null; expandedCustomer = false })
                        st.customers.forEach {
                            DropdownMenuItem({ Text(it.name) }, onClick = { selectedCustomer = it.id; expandedCustomer = false })
                        }
                    }
                }
            }
            item { Text("Ajouter un produit", style = MaterialTheme.typography.titleSmall) }
            item {
                ExposedDropdownMenuBox(expandedProduct, { expandedProduct = !expandedProduct }, Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = st.products.firstOrNull { it.id == addProductId }?.name ?: "Choisir produit…",
                        onValueChange = {}, readOnly = true, label = { Text("Produit") }, modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expandedProduct, { expandedProduct = false }) {
                        st.products.forEach {
                            DropdownMenuItem({ Text("${it.name} (${it.stock} en stock)") }, onClick = { addProductId = it.id; expandedProduct = false })
                        }
                    }
                }
            }
            item { OutlinedTextField(addQty, { addQty = it }, label = { Text("Quantité") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }
            item { OutlinedTextField(addDiscount, { addDiscount = it }, label = { Text("Remise") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }
            item {
                Button(onClick = {
                    val pid = addProductId
                    val prod = st.products.firstOrNull { it.id == pid }
                    val q = addQty.toIntOrNull() ?: 0
                    if (prod != null && q > 0) {
                        items = items + SaleItemInput(
                            productId = prod.id, quantity = q,
                            unitPrice = prod.sellPrice, discount = addDiscount.toDoubleOrNull() ?: 0.0
                        )
                        addProductId = null; addQty = "1"; addDiscount = "0"
                    } else { msg = "Choisissez un produit et une quantité valide" }
                }, Modifier.fillMaxWidth()) { Text("Ajouter la ligne") }
            }
            items(items) { it ->
                val prod = st.products.firstOrNull { p -> p.id == it.productId }
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(prod?.name ?: "Produit #${it.productId}", style = MaterialTheme.typography.titleSmall)
                            Text("${it.quantity} × ${String.format(Locale.getDefault(), "%.0f", it.unitPrice)}  | remise ${String.format(Locale.getDefault(), "%.0f", it.discount)}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ items = items.filter { x -> x !== it } }) { Icon(Icons.Filled.Delete, "Retirer") }
                    }
                }
            }
            item { Text("Mode de paiement", style = MaterialTheme.typography.titleSmall) }
            item {
                ExposedDropdownMenuBox(expandedPayment, { expandedPayment = !expandedPayment }, Modifier.fillMaxWidth()) {
                    OutlinedTextField(payment.label, {}, readOnly = true, label = { Text("Paiement") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expandedPayment, { expandedPayment = false }) {
                        PaymentMode.values().forEach {
                            DropdownMenuItem({ Text(it.label) }, onClick = { payment = it; expandedPayment = false })
                        }
                    }
                }
            }
            item { OutlinedTextField(note, { note = it }, label = { Text("Note") }, singleLine = true) }
            item { Text("Total: ${String.format(Locale.getDefault(), "%.0f", total)} F", style = MaterialTheme.typography.headlineSmall) }
            item {
                Button(onClick = {
                    if (items.isEmpty()) { msg = "Ajoutez au moins une ligne"; return@Button }
                    val id = vm.recordSale(selectedCustomer, items, payment, note)
                    if (id > 0) { msg = "Vente #$id enregistrée"; items = emptyList(); nav.popBackStack() }
                    else if (id < 0 && msg.isBlank()) msg = "Échec de l'enregistrement"
                }, Modifier.fillMaxWidth()) { Text("Valider la vente") }
            }
            if (msg.isNotEmpty()) item { Text(msg, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFmt.format(Date())
    Scaffold(
        topBar = { TopAppBar(title = { Text("Ventes") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Aujourd'hui: ${String.format(Locale.getDefault(), "%.0f", st.todayRevenue)} F")
                    Text("Total: ${String.format(Locale.getDefault(), "%.0f", st.totalRevenue)} F")
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(st.sales) { s ->
                    val cust = st.customers.firstOrNull { it.id == s.customerId }
                    Card(Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Vente #${s.id} — ${String.format(Locale.getDefault(), "%.0f", s.totalAmount)} F", style = MaterialTheme.typography.titleSmall)
                            Text("${s.saleDate} • ${s.paymentMode.label} • ${cust?.name ?: "Comptoir"}", style = MaterialTheme.typography.bodySmall)
                            if (s.note.isNotBlank()) Text("Note: ${s.note}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mouvements de stock") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            items(st.stockMovements) { m ->
                val prod = st.products.firstOrNull { it.id == m.productId }
                Card(Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${m.type.label} ${m.quantity} — ${prod?.name ?: "Produit #${m.productId}"}", style = MaterialTheme.typography.titleSmall)
                            Text("${m.date} • ${m.reason}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Fournisseurs") }, navigationIcon = {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Retour") }
        }) },
        floatingActionButton = { FloatingActionButton({ showDialog = true }) { Icon(Icons.Filled.Add, "Ajouter") } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            items(st.suppliers) { s ->
                Card(Modifier.fillMaxWidth().padding(4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(s.name, style = MaterialTheme.typography.titleSmall)
                            if (s.contact.isNotEmpty()) Text(s.contact, style = MaterialTheme.typography.bodySmall)
                            if (s.productsSupplied.isNotEmpty()) Text("Fournit: ${s.productsSupplied}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ vm.removeSupplier(s.id) }) { Icon(Icons.Filled.Delete, "Supprimer") }
                    }
                }
            }
        }
    }
    if (showDialog) {
        SupplierFormDialog(onDismiss = { showDialog = false }, onSave = { name, contact, products ->
            vm.addSupplier(name, contact, products)
            showDialog = false
        })
    }
}

@Composable
fun SupplierFormDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var products by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau fournisseur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom *") })
                OutlinedTextField(contact, { contact = it }, label = { Text("Contact") })
                OutlinedTextField(products, { products = it }, label = { Text("Produits fournis") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, contact, products) }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } }
    )
}
