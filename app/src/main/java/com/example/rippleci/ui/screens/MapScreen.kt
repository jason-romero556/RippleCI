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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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

@OptIn(ExperimentalPermissionsApi::class)
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
    var plannerExpanded by rememberSaveable { mutableStateOf(true) }
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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .zIndex(2f),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopControlBar(
                    mapStyle = mapStyle,
                    directoryExpanded = directoryExpanded,
                    plannerExpanded = plannerExpanded,
                    onMapStyleChange = { mapStyle = it },
                    onToggleDirectory = { directoryExpanded = !directoryExpanded },
                    onTogglePlanner = {
                        plannerExpanded = !plannerExpanded
                        if (!plannerExpanded) {
                            routePanelCollapsed = false
                        }
                    },
                )

                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (directoryExpanded) {
                        FloatingPanel(
                            title = "Building Directory",
                            collapsed = false,
                            actionLabel = "Close",
                            onToggleCollapsed = { directoryExpanded = false },
                        ) {
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
                        Card {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Map is available without location access.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                                    Text("Enable My Location")
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties =
                    MapProperties(
                        isMyLocationEnabled = false,
                        mapType = if (mapStyle == CampusMapStyle.OSM) MapType.NONE else MapType.NORMAL,
                        maxZoomPreference = if (mapStyle == CampusMapStyle.OSM) 19f else 21f,
                    ),
                uiSettings =
                    MapUiSettings(
                        compassEnabled = true,
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = false,
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
                            .padding(3.dp),
                ) {
                    Text(
                        text = "(C) OpenStreetMap contributors",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                    )
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
private fun TopControlBar(
    mapStyle: CampusMapStyle,
    directoryExpanded: Boolean,
    plannerExpanded: Boolean,
    onMapStyleChange: (CampusMapStyle) -> Unit,
    onToggleDirectory: () -> Unit,
    onTogglePlanner: () -> Unit,
) {
    val items =
        listOf(
            Triple("Google", mapStyle == CampusMapStyle.GOOGLE, { onMapStyleChange(CampusMapStyle.GOOGLE) }),
            Triple("OSM", mapStyle == CampusMapStyle.OSM, { onMapStyleChange(CampusMapStyle.OSM) }),
            Triple(if (directoryExpanded) "Hide Dir" else "Directory", directoryExpanded, onToggleDirectory),
            Triple(if (plannerExpanded) "Hide Route" else "Route", plannerExpanded, onTogglePlanner),
        )
    val selectedIndex = items.indexOfFirst { it.second }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = { HorizontalDivider() },
    ) {
        items.forEachIndexed { index, item ->
            Tab(
                selected = index == selectedIndex,
                onClick = item.third,
                text = {
                    Text(
                        text = item.first,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
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
