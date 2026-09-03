package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.EcoMindViewModel
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.components.GeminiProductFetchDialog
import com.example.ui.components.ProductEditDialog
import com.example.ui.theme.EcoBadgeBad
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoBadgeWarning

import com.example.ui.components.ShimmerListLoading
import androidx.compose.ui.graphics.Brush

@Composable
fun CatalogScreen(
    viewModel: EcoMindViewModel,
    onSelectProduct: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val backendSyncStatus by viewModel.backendSyncStatus.collectAsState()
    val isBackendSyncing by viewModel.isBackendSyncing.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showGeminiFetchDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    val categories = listOf("All", "Food", "Plastic", "Glass", "Paper", "Electronics", "Metal")

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Aesthetic Banner
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_database_sync_header")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF16261F),
                                        Color(0xFF1E3A2F),
                                        Color(0xFF0D1C15)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EcoBadgeGood.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EcoBadgeGood.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Eco, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ECO PRODUCTS CATALOG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoBadgeGood)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Environmental Product Registry",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Text(
                                text = if (!backendSyncStatus.isNullOrBlank()) backendSyncStatus!! else "Room Local DB & Cloud Firestore Synchronized • Real-Time Traceability",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showGeminiFetchDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EcoBadgeGood),
                                    modifier = Modifier.weight(1.3f).testTag("btn_open_gemini_fetch_dialog")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("✨ AI Product Fetch", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.fetchProductsFromFirestore() },
                                    enabled = !isBackendSyncing,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_pull_db_catalog")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(13.dp), tint = EcoBadgeGood)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Pull Cloud", fontSize = 10.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.syncProductsToFirestore() },
                                    enabled = !isBackendSyncing,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_sync_db_catalog")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(13.dp), tint = EcoBadgeGood)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Push Cloud", fontSize = 10.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        productToEdit = ProductEntity(
                                            id = "10${(10..99).random()}",
                                            name = "New Eco Product",
                                            category = "Plastics",
                                            carbon = "0.5 kg",
                                            water = "10 L",
                                            ecoScore = 75,
                                            recycling = "100% Recyclable",
                                            impact = "Low environmental footprint.",
                                            alternative = "Biodegradable materials",
                                            isEcoFriendly = true
                                        )
                                        showEditDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(0.9f).testTag("btn_add_product_db")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Add", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search Products, RFID Tags or Categories...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { /* Voice search action */ }) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Search", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("catalog_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Category Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedCategory(category) },
                            label = { Text(category, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("chip_$category")
                        )
                    }
                }
            }

            // Product Count Header
            item {
                Text(
                    text = "Showing ${filteredProducts.size} Environmental Products",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Product Items List
            if (isBackendSyncing) {
                item {
                    ShimmerListLoading(count = 4)
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCatalogCard(
                        product = product,
                        onClick = {
                            viewModel.onRfidTagScanned(product.id)
                            onSelectProduct(product)
                        },
                        onEdit = {
                            productToEdit = product
                            showEditDialog = true
                        },
                        onDelete = {
                            viewModel.deleteProduct(product.id)
                        }
                    )
                }
            }
        }

        // Floating Action Button to Add Product
        ExtendedFloatingActionButton(
            onClick = {
                productToEdit = ProductEntity(
                    id = "10${(10..99).random()}",
                    name = "New Eco Product",
                    category = "Plastics",
                    carbon = "0.5 kg",
                    water = "10 L",
                    ecoScore = 75,
                    recycling = "100% Recyclable",
                    impact = "Low environmental footprint.",
                    alternative = "Biodegradable materials",
                    isEcoFriendly = true
                )
                showEditDialog = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = "Add Product") },
            text = { Text("New Product", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_product")
        )

        // Add/Edit Dialog
        if (showEditDialog && productToEdit != null) {
            ProductEditDialog(
                product = productToEdit!!,
                onDismiss = {
                    showEditDialog = false
                    productToEdit = null
                },
                onSave = { updatedProduct, syncToBackend ->
                    viewModel.updateProduct(updatedProduct, syncToBackend)
                    showEditDialog = false
                    productToEdit = null
                }
            )
        }

        // Gemini AI Product Fetch Dialog
        if (showGeminiFetchDialog) {
            GeminiProductFetchDialog(
                initialQuery = searchQuery,
                onDismiss = { showGeminiFetchDialog = false },
                onSaveProduct = { fetched ->
                    viewModel.updateProduct(fetched, syncToBackend = true)
                    viewModel.syncProductsToFirestore()
                    showGeminiFetchDialog = false
                }
            )
        }
    }
}

@Composable
private fun ProductCatalogCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isGood = product.ecoScore >= 60

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Eco Score Badge Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGood) EcoBadgeGood.copy(alpha = 0.15f)
                        else EcoBadgeBad.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${product.ecoScore}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGood) EcoBadgeGood else EcoBadgeBad
                    )
                    Text(
                        text = "ECO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGood) EcoBadgeGood else EcoBadgeBad
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = product.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${product.id}",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Co2, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = product.carbon, fontSize = 10.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = product.water, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            // Edit & Delete Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp).testTag("btn_edit_catalog_${product.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Product", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("btn_delete_catalog_${product.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

