package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Area
import com.example.model.SeiriObject
import com.example.model.Zone
import com.example.ui.theme.*

/**
 * Reusable dropdown selector for Material 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchyDropdownItem(
    label: String,
    selectedValue: String,
    items: List<Pair<String, String>>, // (id/value, display label)
    icon: ImageVector,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Pilih..."
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (selectedValue.isBlank()) placeholder else selectedValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) PrimaryBlue else SecondarySlateLight,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = SurfaceVariantLight.copy(alpha = 0.5f),
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = OutlineLight
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = enabled)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .heightIn(max = 260.dp)
        ) {
            if (items.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Tidak ada opsi tersedia", fontSize = 12.sp, color = SecondarySlate) },
                    onClick = { expanded = false }
                )
            } else {
                items.forEach { (id, display) ->
                    val isSelected = display == selectedValue || id == selectedValue
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = display,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp)) }
                        } else null,
                        onClick = {
                            onItemSelected(id)
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 4-Level Cascading Hierarchy Filter Card:
 * GROUP -> AREA -> ZONE -> OBJECT
 */
@Composable
fun HierarchyCascadingFilterCard(
    allAreas: List<Area>,
    allZones: List<Zone>,
    allObjects: List<SeiriObject>,
    selectedGroup: String?,
    selectedAreaId: String?,
    selectedZoneId: String?,
    selectedObjectId: String?,
    onGroupSelected: (String?) -> Unit,
    onAreaSelected: (String?) -> Unit,
    onZoneSelected: (String?) -> Unit,
    onObjectSelected: (String?) -> Unit,
    onResetFilter: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Filter Hirarki Lokasi & Objek",
    showObjectFilter: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(true) }

    // Unique groups from areas
    val availableGroups = remember(allAreas) {
        listOf("Semua Group") + allAreas.map { it.group }.distinct().sorted()
    }

    // Areas filtered by selectedGroup
    val filteredAreas = remember(allAreas, selectedGroup) {
        if (selectedGroup == null) allAreas else allAreas.filter { it.group == selectedGroup }
    }

    // Zones filtered by selectedAreaId (or by areas in selectedGroup)
    val filteredZones = remember(allZones, selectedAreaId, filteredAreas) {
        if (selectedAreaId != null) {
            allZones.filter { it.areaId == selectedAreaId }
        } else {
            val validAreaIds = filteredAreas.map { it.id }.toSet()
            allZones.filter { it.areaId in validAreaIds }
        }
    }

    // Objects filtered by selectedZoneId or selectedAreaId
    val filteredObjectOptions = remember(allObjects, selectedZoneId, selectedAreaId) {
        when {
            selectedZoneId != null -> allObjects.filter { it.zoneId == selectedZoneId }
            selectedAreaId != null -> allObjects.filter { it.areaId == selectedAreaId }
            else -> allObjects
        }
    }

    val hasActiveFilter = selectedGroup != null || selectedAreaId != null || selectedZoneId != null || selectedObjectId != null

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialNavy
                        )
                        Text(
                            text = "GROUP -> AREA -> ZONE -> OBJECT",
                            fontSize = 10.sp,
                            color = SecondarySlate
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasActiveFilter) {
                        TextButton(
                            onClick = onResetFilter,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusRed)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Reset", fontSize = 11.sp, color = StatusRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Ciutkan" else "Perluas",
                            tint = SecondarySlate
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: GROUP & AREA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. GROUP Dropdown
                        val currentGroupDisplay = selectedGroup ?: "Semua Group"
                        val groupOptions = availableGroups.map { it to it }

                        HierarchyDropdownItem(
                            label = "1. GROUP",
                            selectedValue = currentGroupDisplay,
                            items = groupOptions,
                            icon = Icons.Default.Factory,
                            onItemSelected = { sel ->
                                if (sel == "Semua Group") {
                                    onGroupSelected(null)
                                } else {
                                    onGroupSelected(sel)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 2. AREA Dropdown
                        val currentArea = allAreas.find { it.id == selectedAreaId }
                        val currentAreaDisplay = currentArea?.areaName ?: "Semua Area"
                        val areaOptions = listOf("" to "Semua Area") + filteredAreas.map { it.id to it.areaName }

                        HierarchyDropdownItem(
                            label = "2. AREA",
                            selectedValue = currentAreaDisplay,
                            items = areaOptions,
                            icon = Icons.Default.Domain,
                            onItemSelected = { selId ->
                                if (selId.isBlank()) {
                                    onAreaSelected(null)
                                } else {
                                    onAreaSelected(selId)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: ZONE & OBJECT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 3. ZONE Dropdown
                        val currentZone = allZones.find { it.id == selectedZoneId }
                        val currentZoneDisplay = currentZone?.zoneName ?: "Semua Zona"
                        val zoneOptions = listOf("" to "Semua Zona") + filteredZones.map { it.id to it.zoneName }

                        HierarchyDropdownItem(
                            label = "3. ZONA",
                            selectedValue = currentZoneDisplay,
                            items = zoneOptions,
                            icon = Icons.Default.Place,
                            onItemSelected = { selId ->
                                if (selId.isBlank()) {
                                    onZoneSelected(null)
                                } else {
                                    onZoneSelected(selId)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 4. OBJECT Dropdown (Optional if screen uses it)
                        if (showObjectFilter) {
                            val currentObj = allObjects.find { it.id == selectedObjectId }
                            val currentObjDisplay = currentObj?.name ?: "Semua Objek"
                            val objectOptions = listOf("" to "Semua Objek") + filteredObjectOptions.map { it.id to "${it.objectCode} - ${it.name}" }

                            HierarchyDropdownItem(
                                label = "4. OBJEK",
                                selectedValue = currentObjDisplay,
                                items = objectOptions,
                                icon = Icons.Default.Inventory2,
                                onItemSelected = { selId ->
                                    if (selId.isBlank()) {
                                        onObjectSelected(null)
                                    } else {
                                        onObjectSelected(selId)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Active Breadcrumb
                    Surface(
                        color = SurfaceVariantLight,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val gText = selectedGroup ?: "Semua Group"
                        val aText = allAreas.find { it.id == selectedAreaId }?.areaName ?: "Semua Area"
                        val zText = allZones.find { it.id == selectedZoneId }?.zoneName ?: "Semua Zona"
                        val oText = allObjects.find { it.id == selectedObjectId }?.let { "${it.objectCode} (${it.name})" } ?: "Semua Objek"

                        Text(
                            text = "Filter: $gText  >  $aText  >  $zText" + if (showObjectFilter) "  >  $oText" else "",
                            fontSize = 10.sp,
                            color = SecondarySlate,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Autocomplete/Live Search Bar for Area or Object search
 */
@Composable
fun AreaOrObjectSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cari nama area atau objek...",
    modifier: Modifier = Modifier,
    onClear: () -> Unit = { onQueryChange("") }
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = SecondarySlate, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = OutlineLight
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_area_or_object_field")
    )
}
