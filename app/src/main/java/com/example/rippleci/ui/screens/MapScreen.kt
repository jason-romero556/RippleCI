package com.example.rippleci.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.rippleci.data.CampusLocation
import com.example.rippleci.data.OpenStreetMapTileProvider
import com.example.rippleci.data.campusLocations
import com.example.rippleci.data.routeDistanceMeters
import com.example.rippleci.data.walkingRoute
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.roundToInt

private enum class CampusMapStyle {
    GOOGLE,
    OSM,
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val campusSites = remember { campusLocations() }
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val osmTileProvider = remember(context) { OpenStreetMapTileProvider(cacheDirectory = context.cacheDir) }
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(34.1621, -119.0435), 16f)
        }

    var mapStyle by rememberSaveable { mutableStateOf(CampusMapStyle.GOOGLE) }
    var directoryExpanded by rememberSaveable { mutableStateOf(false) }
    var plannerExpanded by rememberSaveable { mutableStateOf(false) }
    var routePanelCollapsed by rememberSaveable { mutableStateOf(false) }
    var selectedLocationId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeStartId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeEndId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedLocation = campusSites.find { it.id == selectedLocationId }
    val routeStart = campusSites.find { it.id == routeStartId }
    val routeEnd = campusSites.find { it.id == routeEndId }
    val visiblePins =
        remember(directoryExpanded, plannerExpanded, selectedLocationId, routeStartId, routeEndId, campusSites) {
            when {
                directoryExpanded -> campusSites
                plannerExpanded -> {
                    val visibleIds = listOfNotNull(selectedLocationId, routeStartId, routeEndId).toSet()
                    campusSites.filter { it.id in visibleIds }
                }
                else -> {
                    val visibleIds = listOfNotNull(selectedLocationId).toSet()
                    campusSites.filter { it.id in visibleIds }
                }
            }
        }
    val routePoints =
        remember(routeStart, routeEnd) {
            if (routeStart != null && routeEnd != null) {
                walkingRoute(routeStart, routeEnd)
            } else {
                emptyList()
            }
        }
    val displayedRoutePoints =
        remember(plannerExpanded, routePoints) {
            if (plannerExpanded) routePoints else emptyList()
        }

    LaunchedEffect(selectedLocation?.id) {
        selectedLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it.coordinate, 17f), 700)
        }
    }

    LaunchedEffect(routeStart?.id, routeEnd?.id, displayedRoutePoints) {
        if (routeStart == null || routeEnd == null) return@LaunchedEffect

        if (routeStart.id == routeEnd.id) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(routeStart.coordinate, 18f), 700)
            return@LaunchedEffect
        }

        if (displayedRoutePoints.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            displayedRoutePoints.forEach(boundsBuilder::include)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 180), 900)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    isMyLocationEnabled = locationPermissionState.status.isGranted,
                    mapType = if (mapStyle == CampusMapStyle.OSM) MapType.NONE else MapType.NORMAL,
                ),
            uiSettings =
                MapUiSettings(
                    compassEnabled = true,
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = locationPermissionState.status.isGranted,
                    mapToolbarEnabled = false,
                ),
        ) {
            if (mapStyle == CampusMapStyle.OSM) {
                TileOverlay(tileProvider = osmTileProvider)
            }

            visiblePins.forEach { location ->
                Marker(
                    state = MarkerState(position = location.coordinate),
                    title = location.name,
                    snippet = formatCoordinate(location.coordinate),
                    onClick = {
                        selectedLocationId = location.id
                        false
                    },
                )
            }

            if (displayedRoutePoints.size >= 2) {
                Polyline(
                    points = displayedRoutePoints,
                    color = Color(0xFF006D5B),
                    width = 16f,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    jointType = JointType.ROUND,
                )
            }
        }

        if (mapStyle == CampusMapStyle.OSM) {
            Card(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
            ) {
                Text(
                    text = "(C) OpenStreetMap contributors",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                )
            }
        }

        // Overlay Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = directoryExpanded,
                    onClick = {
                        directoryExpanded = !directoryExpanded
                        if (directoryExpanded) plannerExpanded = false
                    },
                    label = { Text("Directory") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                FilterChip(
                    selected = plannerExpanded,
                    onClick = {
                        plannerExpanded = !plannerExpanded
                        if (plannerExpanded) directoryExpanded = false
                    },
                    label = { Text("Route") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Map Style Toggle
                Box {
                    var styleMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { styleMenuExpanded = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "Map Style")
                    }
                    DropdownMenu(
                        expanded = styleMenuExpanded,
                        onDismissRequest = { styleMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Google Maps") },
                            onClick = { mapStyle = CampusMapStyle.GOOGLE; styleMenuExpanded = false },
                            leadingIcon = { if (mapStyle == CampusMapStyle.GOOGLE) Icon(Icons.Default.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("OpenStreetMap") },
                            onClick = { mapStyle = CampusMapStyle.OSM; styleMenuExpanded = false },
                            leadingIcon = { if (mapStyle == CampusMapStyle.OSM) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            }

            if (directoryExpanded) {
                DirectoryPanel(
                    locations = campusSites,
                    selectedLocationId = selectedLocationId,
                    onSelectLocation = { location ->
                        selectedLocationId = location.id
                        routeEndId = location.id
                        directoryExpanded = false
                    },
                )
            }

            if (plannerExpanded) {
                FloatingPanel(
                    title = "Walking Route",
                    collapsed = routePanelCollapsed,
                    onToggleCollapsed = { routePanelCollapsed = !routePanelCollapsed },
                ) {
                    RoutePlannerPanel(
                        locations = campusSites,
                        routeStartId = routeStartId,
                        routeEndId = routeEndId,
                        routeDistanceMeters = routeDistanceMeters(routePoints),
                        onStartSelected = { routeStartId = it.id },
                        onEndSelected = {
                            routeEndId = it.id
                            selectedLocationId = it.id
                        },
                    )
                }
            }

            if (!locationPermissionState.status.isGranted) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Enable location for real-time tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                            Text("Enable")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingPanel(
    title: String,
    collapsed: Boolean,
    actionLabel: String? = null,
    onToggleCollapsed: () -> Unit,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onToggleCollapsed,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text(actionLabel ?: if (collapsed) "Open" else "Collapse")
                }
            }
            if (!collapsed) {
                HorizontalDivider()
                content()
            }
        }
    }
}

@Composable
private fun DirectoryPanel(
    locations: List<CampusLocation>,
    selectedLocationId: String?,
    onSelectLocation: (CampusLocation) -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Building Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Tap a building to jump to its marker and exact coordinates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(locations, key = { it.id }) { location ->
                    val isSelected = location.id == selectedLocationId
                    TextButton(
                        onClick = { onSelectLocation(location) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(location.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            Text(
                                formatCoordinate(location.coordinate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutePlannerPanel(
    locations: List<CampusLocation>,
    routeStartId: String?,
    routeEndId: String?,
    routeDistanceMeters: Int,
    onStartSelected: (CampusLocation) -> Unit,
    onEndSelected: (CampusLocation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CampusLocationDropdown(
            label = "Start",
            locations = locations,
            selectedLocationId = routeStartId,
            onSelected = onStartSelected,
        )
        CampusLocationDropdown(
            label = "Destination",
            locations = locations,
            selectedLocationId = routeEndId,
            onSelected = onEndSelected,
        )

        if (routeStartId != null && routeEndId != null) {
            Text(
                text =
                    if (routeStartId == routeEndId) {
                        "You are already at the selected destination."
                    } else {
                        "Estimated campus walk: ${formatDistance(routeDistanceMeters)}"
                    },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CampusLocationDropdown(
    label: String,
    locations: List<CampusLocation>,
    selectedLocationId: String?,
    onSelected: (CampusLocation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = locations.find { it.id == selectedLocationId }?.name.orEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            locations.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.name) },
                    onClick = {
                        onSelected(location)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatCoordinate(coordinate: LatLng): String =
    "${"%.6f".format(coordinate.latitude)}, ${"%.6f".format(coordinate.longitude)}"

private fun formatDistance(distanceMeters: Int): String {
    val distanceFeet = (distanceMeters * 3.28084).roundToInt()
    val walkMinutes = (distanceMeters / 80.0).coerceAtLeast(1.0).roundToInt()
    return "$distanceFeet ft - about $walkMinutes min"
}
