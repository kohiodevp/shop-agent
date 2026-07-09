package com.shop.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shop.agent.data.OrderStatus
import com.shop.agent.ui.TailorViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController, vm: TailorViewModel) {
    val st = vm.state.collectAsState().value
    Scaffold(
        topBar = { TopAppBar(title = { Text("Tailor Agent") }) }
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
            Spacer(Modifier.height(8.dp))
            Button(onClick = { nav.navigate("clients") }, Modifier.fillMaxWidth()) { Text("Gérer les clients") }
            Button(onClick = { nav.navigate("orders") }, Modifier.fillMaxWidth()) { Text("Suivre les commandes") }
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
        item {
            Button(onClick = {
                val path = vm.exportSelectedPdf()
                msg = if (path != null) "PDF exporté: $path" else "Export impossible"
            }, Modifier.fillMaxWidth()) { Text("Exporter la fiche en PDF") }
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
            garments = vm.listGarments(),
            onDismiss = { showDialog = false },
            onSave = { g, s, st ->
                val fabric = vm.computeFabric(g, s, false, false)
                val fm = (fabric?.get("fabric_m") as? String)?.toDoubleOrNull() ?: 0.0
                vm.addOrder(clientId, g, s, st, fm)
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
