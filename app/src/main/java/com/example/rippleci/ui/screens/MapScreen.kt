package com.example.rippleci.ui.screens

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    var liveLocationEnabled by rememberSaveable { mutableStateOf(true) }
    var routePanelCollapsed by rememberSaveable { mutableStateOf(false) }
    var selectedLocationId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeStartId by rememberSaveable { mutableStateOf(campusSites.firstOrNull()?.id) }
    var routeEndId by rememberSaveable { mutableStateOf(campusSites.getOrNull(1)?.id) }
    var directorySearchQuery by rememberSaveable { mutableStateOf("") }

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
                    isMyLocationEnabled = liveLocationEnabled && locationPermissionState.status.isGranted,
                    mapType = if (mapStyle == CampusMapStyle.OSM) MapType.NONE else MapType.NORMAL,
                ),
            uiSettings =
                MapUiSettings(
                    compassEnabled = true,
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                ),
        ) {
            if (mapStyle == CampusMapStyle.OSM) {
                TileOverlay(tileProvider = osmTileProvider, zIndex = -1f)
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
                    color = Color.White,
                    width = 22f,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    jointType = JointType.ROUND,
                    zIndex = 5f,
                )
                Polyline(
                    points = displayedRoutePoints,
                    color = Color(0xFF006D5B),
                    width = 12f,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    jointType = JointType.ROUND,
                    zIndex = 6f,
                )
            }
        }

        if (mapStyle == CampusMapStyle.OSM) {
            Card(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = "(C) OpenStreetMap contributors",
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 6.sp,
                )
            }
        }

        MapZoomControls(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 18.dp)
                    .zIndex(2f),
            onZoomIn = { cameraPositionState.move(CameraUpdateFactory.zoomIn()) },
            onZoomOut = { cameraPositionState.move(CameraUpdateFactory.zoomOut()) },
        )

        // Overlay Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapToolbarChip(
                    label = "Directory",
                    selected = directoryExpanded,
                    leadingIcon = Icons.Default.Search,
                    onClick = {
                        directoryExpanded = !directoryExpanded
                        if (directoryExpanded) plannerExpanded = false
                    },
                )

                MapToolbarChip(
                    label = "Route",
                    selected = plannerExpanded,
                    leadingIcon = Icons.Default.LocationOn,
                    onClick = {
                        plannerExpanded = !plannerExpanded
                        if (plannerExpanded) directoryExpanded = false
                    },
                )

                MapStyleSelector(
                    mapStyle = mapStyle,
                    onMapStyleSelected = { mapStyle = it },
                )

                MapToolbarChip(
                    label = "Live",
                    selected = liveLocationEnabled && locationPermissionState.status.isGranted,
                    leadingIcon = Icons.Default.Place,
                    onClick = {
                        if (locationPermissionState.status.isGranted) {
                            liveLocationEnabled = !liveLocationEnabled
                        } else {
                            liveLocationEnabled = true
                            locationPermissionState.launchPermissionRequest()
                        }
                    },
                )
            }

            if (directoryExpanded) {
                DirectoryPanel(
                    locations = campusSites,
                    searchQuery = directorySearchQuery,
                    onSearchQueryChange = { directorySearchQuery = it },
                    selectedLocationId = selectedLocationId,
                    onSelectLocation = { location ->
                        selectedLocationId = location.id
                        routeEndId = location.id
                        directoryExpanded = false
                        directorySearchQuery = ""
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
private fun RowScope.MapToolbarChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .weight(1f)
                .height(38.dp),
        shape = RoundedCornerShape(9.dp),
        color = containerColor,
        contentColor = contentColor,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RowScope.MapStyleSelector(
    mapStyle: CampusMapStyle,
    onMapStyleSelected: (CampusMapStyle) -> Unit,
) {
    var styleMenuExpanded by remember { mutableStateOf(false) }
    val providerLabel =
        when (mapStyle) {
            CampusMapStyle.GOOGLE -> "Google"
            CampusMapStyle.OSM -> "OSM"
        }

    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(38.dp),
    ) {
        Surface(
            onClick = { styleMenuExpanded = true },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Maps",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = providerLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                )
            }
        }

        DropdownMenu(
            expanded = styleMenuExpanded,
            onDismissRequest = { styleMenuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Google Maps") },
                onClick = {
                    onMapStyleSelected(CampusMapStyle.GOOGLE)
                    styleMenuExpanded = false
                },
                leadingIcon = {
                    if (mapStyle == CampusMapStyle.GOOGLE) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("OpenStreetMap") },
                onClick = {
                    onMapStyleSelected(CampusMapStyle.OSM)
                    styleMenuExpanded = false
                },
                leadingIcon = {
                    if (mapStyle == CampusMapStyle.OSM) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                },
            )
        }
    }
}

@Composable
private fun MapZoomControls(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) {
    ElevatedCard(modifier = modifier.width(44.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
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
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedLocationId: String?,
    onSelectLocation: (CampusLocation) -> Unit,
) {
    val filteredLocations = remember(locations, searchQuery) {
        if (searchQuery.isBlank()) {
            locations
        } else {
            locations.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Building Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search buildings...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider()

            if (filteredLocations.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No buildings found matching \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredLocations, key = { it.id }) { location ->
                        val isSelected = location.id == selectedLocationId
                        TextButton(
                            onClick = { onSelectLocation(location) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = MaterialTheme.shapes.small
                                    ),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text(
                                    text = location.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    formatCoordinate(location.coordinate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampusLocationDropdown(
    label: String,
    locations: List<CampusLocation>,
    selectedLocationId: String?,
    onSelected: (CampusLocation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLocation = locations.find { it.id == selectedLocationId }
    val selectedName = selectedLocation?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            locations.forEach { location ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (location.id == selectedLocationId) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelected(location)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
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
