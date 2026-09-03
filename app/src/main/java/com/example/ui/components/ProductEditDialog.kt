package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ProductEntity

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ai.GeminiEcoAssistant
import com.example.ui.theme.EcoBadgeGood
import kotlinx.coroutines.launch

@Composable
fun ProductEditDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, Boolean) -> Unit
) {
    var productId by remember { mutableStateOf(product.id) }
    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var ecoScore by remember { mutableFloatStateOf(product.ecoScore.toFloat()) }
    var carbon by remember { mutableStateOf(product.carbon) }
    var water by remember { mutableStateOf(product.water) }
    var recycling by remember { mutableStateOf(product.recycling) }
    var impact by remember { mutableStateOf(product.impact) }
    var alternative by remember { mutableStateOf(product.alternative) }
    var syncToBackend by remember { mutableStateOf(true) }
    var isAiFetching by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Update Product Specifications",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = productId,
                    onValueChange = { productId = it },
                    label = { Text("Product / RFID Tag ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_id")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_name")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            isAiFetching = true
                            scope.launch {
                                val fetched = GeminiEcoAssistant.fetchProductDetailsViaGemini(name)
                                name = fetched.name
                                category = fetched.category
                                carbon = fetched.carbon
                                water = fetched.water
                                ecoScore = fetched.ecoScore.toFloat()
                                recycling = fetched.recycling
                                impact = fetched.impact
                                alternative = fetched.alternative
                                isAiFetching = false
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !isAiFetching,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EcoBadgeGood),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_autofill_gemini")
                ) {
                    if (isAiFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = androidx.compose.ui.graphics.Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetching via AI...", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✨ Auto-Fill Specs via AI", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Plastics, Apparel, Electronics)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_category")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Eco Score Slider
                Text(
                    text = "Eco Rating Score: ${ecoScore.toInt()}/100",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = ecoScore,
                    onValueChange = { ecoScore = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_score_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbon,
                        onValueChange = { carbon = it },
                        label = { Text("Carbon (CO2)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_product_carbon")
                    )

                    OutlinedTextField(
                        value = water,
                        onValueChange = { water = it },
                        label = { Text("Water Usage") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_product_water")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = recycling,
                    onValueChange = { recycling = it },
                    label = { Text("Recycling Guidelines") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_recycling")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = impact,
                    onValueChange = { impact = it },
                    label = { Text("Environmental Impact Summary") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_impact")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = alternative,
                    onValueChange = { alternative = it },
                    label = { Text("Greener Alternative Recommendation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_product_alternative")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Backend Database Sync Option
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Push Update to Backend Database",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sends POST /product payload to remote server REST API",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Switch(
                            checked = syncToBackend,
                            onCheckedChange = { syncToBackend = it },
                            modifier = Modifier.testTag("switch_sync_product_to_backend")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_cancel_edit_product")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val finalId = productId.trim().ifBlank { product.id }
                            val updated = product.copy(
                                id = finalId,
                                name = name.ifBlank { product.name },
                                category = category.ifBlank { product.category },
                                ecoScore = ecoScore.toInt(),
                                carbon = carbon.ifBlank { product.carbon },
                                water = water.ifBlank { product.water },
                                recycling = recycling.ifBlank { product.recycling },
                                impact = impact.ifBlank { product.impact },
                                alternative = alternative.ifBlank { product.alternative },
                                isEcoFriendly = ecoScore >= 60
                            )
                            onSave(updated, syncToBackend)
                        },
                        enabled = productId.isNotBlank() && name.isNotBlank(),
                        modifier = Modifier.testTag("btn_save_edit_product")
                    ) {
                        Text("Save Product")
                    }
                }
            }
        }
    }
}
