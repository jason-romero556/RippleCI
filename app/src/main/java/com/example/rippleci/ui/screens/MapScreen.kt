package com.example.rippleci.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.rippleci.data.CampusLocation
import com.example.rippleci.data.GeoCoordinate
import com.example.rippleci.data.campusLocations
import com.example.rippleci.data.routeDistanceMeters
import com.example.rippleci.data.walkingRoute
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polyline as OsmPolyline
import java.io.File
import kotlin.math.roundToInt

private const val LIVE_LOCATION_ID = "live_location"
private const val MAX_LIVE_LOCATION_ACCURACY_METERS = 75f
private const val MAX_LIVE_LOCATION_AGE_MS = 120_000L

private val CAMPUS_CENTER = GeoCoordinate(34.1621, -119.0435)

private enum class MapProvider {
    Google,
    OSM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val campusSites = remember { campusLocations() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissions =
        remember {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
    val osmMapView =
        remember(context) {
            val osmCacheDirectory = File(context.cacheDir, "osmdroid")
            Configuration.getInstance().apply {
                userAgentValue = context.packageName
                osmdroidBasePath = osmCacheDirectory
                osmdroidTileCache = File(osmCacheDirectory, "tiles")
            }
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(16.0)
                controller.setCenter(CAMPUS_CENTER.toGeoPoint())
            }
        }
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(CAMPUS_CENTER.toLatLng(), 16f)
        }

    var mapProvider by rememberSaveable { mutableStateOf(MapProvider.OSM) }
    var directoryExpanded by rememberSaveable { mutableStateOf(false) }
    var plannerExpanded by rememberSaveable { mutableStateOf(false) }
    var routePanelCollapsed by rememberSaveable { mutableStateOf(false) }
    var showLiveLocation by rememberSaveable { mutableStateOf(true) }
    var liveLocation by remember { mutableStateOf<GeoCoordinate?>(null) }
    var hasCenteredOnLiveLocation by rememberSaveable { mutableStateOf(false) }
    var hasFineLocationPermission by remember { mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) }
    var hasCoarseLocationPermission by remember { mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) }
    var selectedLocationId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeStartId by rememberSaveable { mutableStateOf<String?>(LIVE_LOCATION_ID) }
    var routeEndId by rememberSaveable { mutableStateOf(campusSites.firstOrNull()?.id) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            hasFineLocationPermission =
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            hasCoarseLocationPermission =
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    val visibleLiveLocation = liveLocation.takeIf { showLiveLocation }
    val liveLocationRouteOption = visibleLiveLocation?.let { CampusLocation(LIVE_LOCATION_ID, "My Live Location", it) }
    val routeStartLocations =
        remember(campusSites, liveLocationRouteOption) {
            listOfNotNull(liveLocationRouteOption) + campusSites
        }
    val selectedLocation = campusSites.find { it.id == selectedLocationId }
    val routeStart = routeStartLocations.find { it.id == routeStartId }
    val routeEnd = campusSites.find { it.id == routeEndId }
    val isWaitingForLiveRouteStart = showLiveLocation && routeStartId == LIVE_LOCATION_ID && visibleLiveLocation == null
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

    DisposableEffect(lifecycleOwner, osmMapView, locationPermissionLauncher) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        osmMapView.onResume()
                        hasFineLocationPermission = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        hasCoarseLocationPermission = context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (!hasFineLocationPermission) {
                            locationPermissionLauncher.launch(locationPermissions)
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> osmMapView.onPause()
                    else -> Unit
                }
            }

        lifecycle.addObserver(observer)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            osmMapView.onResume()
            if (!hasFineLocationPermission) {
                locationPermissionLauncher.launch(locationPermissions)
            }
        }

        onDispose {
            lifecycle.removeObserver(observer)
            osmMapView.onPause()
        }
    }

    DisposableEffect(osmMapView) {
        onDispose { osmMapView.onDetach() }
    }

    LaunchedEffect(showLiveLocation, routeStartId, campusSites) {
        if (!showLiveLocation && routeStartId == LIVE_LOCATION_ID) {
            routeStartId = campusSites.firstOrNull()?.id
        }
    }

    DisposableEffect(hasFineLocationPermission, showLiveLocation, fusedLocationClient) {
        if (!hasFineLocationPermission || !showLiveLocation) {
            liveLocation = null
            hasCenteredOnLiveLocation = false
            onDispose { }
        } else {
            val observation =
                observeLiveLocation(
                    fusedLocationClient = fusedLocationClient,
                    onLocationChanged = { location -> liveLocation = location },
                )
            onDispose {
                observation.cancellationTokenSource.cancel()
                fusedLocationClient.removeLocationUpdates(observation.locationCallback)
            }
        }
    }

    LaunchedEffect(hasFineLocationPermission, showLiveLocation, visibleLiveLocation, mapProvider) {
        val location = visibleLiveLocation ?: return@LaunchedEffect
        if (hasFineLocationPermission && showLiveLocation && !hasCenteredOnLiveLocation) {
            if (mapProvider == MapProvider.Google) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location.toLatLng(), 17f), 700)
            } else {
                osmMapView.controller.setZoom(17.0)
                osmMapView.controller.animateTo(location.toGeoPoint())
            }
            hasCenteredOnLiveLocation = true
        }
    }

    LaunchedEffect(selectedLocation?.id, mapProvider) {
        selectedLocation?.let {
            if (mapProvider == MapProvider.Google) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it.coordinate.toLatLng(), 17f), 700)
            } else {
                osmMapView.controller.setZoom(17.0)
                osmMapView.controller.animateTo(it.coordinate.toGeoPoint())
            }
        }
    }

    LaunchedEffect(routeStart?.id, routeEnd?.id, displayedRoutePoints, mapProvider) {
        if (routeStart == null || routeEnd == null) return@LaunchedEffect

        if (routeStart.id == routeEnd.id) {
            if (mapProvider == MapProvider.Google) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(routeStart.coordinate.toLatLng(), 18f), 700)
            } else {
                osmMapView.controller.setZoom(18.0)
                osmMapView.controller.animateTo(routeStart.coordinate.toGeoPoint())
            }
            return@LaunchedEffect
        }

        if (displayedRoutePoints.size >= 2) {
            if (mapProvider == MapProvider.Google) {
                val boundsBuilder = LatLngBounds.builder()
                displayedRoutePoints.forEach { boundsBuilder.include(it.toLatLng()) }
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 180), 900)
            } else {
                val bounds = BoundingBox.fromGeoPoints(displayedRoutePoints.map { it.toGeoPoint() })
                osmMapView.post {
                    osmMapView.zoomToBoundingBox(bounds, true, 120)
                }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds(),
    ) {
        if (mapProvider == MapProvider.Google) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = PaddingValues(top = 112.dp, end = 16.dp, bottom = 16.dp, start = 16.dp),
                properties =
                    MapProperties(
                        isMyLocationEnabled = hasFineLocationPermission && showLiveLocation,
                        mapType = MapType.NORMAL,
                    ),
                uiSettings =
                    MapUiSettings(
                        compassEnabled = true,
                        mapToolbarEnabled = false,
                        myLocationButtonEnabled = hasFineLocationPermission && showLiveLocation,
                        rotationGesturesEnabled = true,
                        scrollGesturesEnabled = true,
                        scrollGesturesEnabledDuringRotateOrZoom = true,
                        tiltGesturesEnabled = true,
                        zoomControlsEnabled = true,
                        zoomGesturesEnabled = true,
                    ),
                onMyLocationButtonClick = {
                    if (!hasFineLocationPermission || !showLiveLocation) {
                        locationPermissionLauncher.launch(locationPermissions)
                        true
                    } else {
                        false
                    }
                },
            ) {
                visiblePins.forEach { location ->
                    val isHighlighted =
                        location.id == selectedLocationId ||
                            location.id == routeStartId ||
                            location.id == routeEndId

                    Marker(
                        state = MarkerState(position = location.coordinate.toLatLng()),
                        title = location.name,
                        snippet = formatCoordinate(location.coordinate),
                        icon =
                            BitmapDescriptorFactory.defaultMarker(
                                if (isHighlighted) {
                                    BitmapDescriptorFactory.HUE_RED
                                } else {
                                    BitmapDescriptorFactory.HUE_GREEN
                                },
                            ),
                        onClick = {
                            selectedLocationId = location.id
                            false
                        },
                    )
                }

                visibleLiveLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location.toLatLng()),
                        title = "My Live Location",
                        snippet = formatCoordinate(location),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        zIndex = 20f,
                    )
                }

                if (displayedRoutePoints.size >= 2) {
                    val googleRoutePoints = displayedRoutePoints.map { it.toLatLng() }
                    Polyline(
                        points = googleRoutePoints,
                        color = Color.White.copy(alpha = 0.9f),
                        width = 24f,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        jointType = JointType.ROUND,
                        zIndex = 9f,
                    )
                    Polyline(
                        points = googleRoutePoints,
                        color = Color(0xFF0B57D0),
                        width = 14f,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        jointType = JointType.ROUND,
                        zIndex = 10f,
                    )
                }
            }
        } else {
            AndroidView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                factory = { osmMapView },
                update = { map ->
                    map.overlays.clear()

                    if (displayedRoutePoints.size >= 2) {
                        map.addRouteOverlay(displayedRoutePoints)
                    }

                    visiblePins.forEach { location ->
                        map.overlays +=
                            createCampusMarker(
                                context = context,
                                mapView = map,
                                location = location,
                                isHighlighted =
                                    location.id == selectedLocationId ||
                                        location.id == routeStartId ||
                                        location.id == routeEndId,
                                onClick = { selectedLocationId = location.id },
                            )
                    }

                    visibleLiveLocation?.let { location ->
                        map.overlays += createLiveLocationMarker(context, map, location)
                    }

                    map.invalidate()
                },
            )
        }

        if (mapProvider == MapProvider.OSM) {
            Text(
                text = "(C) OpenStreetMap contributors",
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 6.dp, start = 6.dp)
                        .zIndex(2f),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 5.sp,
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                        .zIndex(2f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MapZoomButton(label = "+", onClick = { osmMapView.controller.zoomIn() })
                MapZoomButton(label = "-", onClick = { osmMapView.controller.zoomOut() })
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard {
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MapControlChip(
                                selected = directoryExpanded,
                                onClick = {
                                    directoryExpanded = !directoryExpanded
                                    if (directoryExpanded) plannerExpanded = false
                                },
                                label = "Directory",
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            )

                            MapControlChip(
                                selected = plannerExpanded,
                                onClick = {
                                    plannerExpanded = !plannerExpanded
                                    if (plannerExpanded) directoryExpanded = false
                                },
                                label = "Route",
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            )

                            MapControlChip(
                                selected = showLiveLocation,
                                onClick = {
                                    val nextVisible = !showLiveLocation
                                    showLiveLocation = nextVisible
                                    if (nextVisible && !hasFineLocationPermission) {
                                        locationPermissionLauncher.launch(locationPermissions)
                                    }
                                },
                                label = "Live",
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MapControlChip(
                                selected = mapProvider == MapProvider.Google,
                                onClick = { mapProvider = MapProvider.Google },
                                label = "Google",
                            )

                            MapControlChip(
                                selected = mapProvider == MapProvider.OSM,
                                onClick = { mapProvider = MapProvider.OSM },
                                label = "OSM",
                            )
                        }
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
                        startLocations = routeStartLocations,
                        destinationLocations = campusSites,
                        routeStartId = routeStartId,
                        routeEndId = routeEndId,
                        routeDistanceMeters = routeDistanceMeters(routePoints),
                        isWaitingForLiveLocation = isWaitingForLiveRouteStart,
                        onStartSelected = { routeStartId = it.id },
                        onEndSelected = {
                            routeEndId = it.id
                            selectedLocationId = it.id
                        },
                    )
                }
            }

            if (showLiveLocation && !hasFineLocationPermission) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Enable precise location for accurate live tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { locationPermissionLauncher.launch(locationPermissions) }) {
                            Text("Enable")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapControlChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    FilterChip(
        modifier = Modifier.heightIn(min = 28.dp),
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, fontSize = 12.sp) },
        leadingIcon = leadingIcon,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    )
}

@Composable
private fun MapZoomButton(
    label: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.size(36.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun observeLiveLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationChanged: (GeoCoordinate) -> Unit,
): LiveLocationObservation {
    val locationCallback =
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations
                    .lastOrNull { it.isUsablePreciseFix() }
                    ?.let { onLocationChanged(it.toCoordinate()) }
            }
        }
    val locationRequest =
        LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(2f)
            .setWaitForAccurateLocation(true)
            .build()
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
        .addOnSuccessListener { location ->
            if (location?.isUsablePreciseFix() == true) {
                onLocationChanged(location.toCoordinate())
            }
        }

    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

    return LiveLocationObservation(locationCallback, cancellationTokenSource)
}

private data class LiveLocationObservation(
    val locationCallback: LocationCallback,
    val cancellationTokenSource: CancellationTokenSource,
)

private fun Location.isUsablePreciseFix(): Boolean {
    if (hasAccuracy() && accuracy > MAX_LIVE_LOCATION_ACCURACY_METERS) return false

    val ageMillis =
        if (elapsedRealtimeNanos > 0L) {
            SystemClock.elapsedRealtime() - (elapsedRealtimeNanos / 1_000_000L)
        } else {
            System.currentTimeMillis() - time
        }

    return ageMillis in 0..MAX_LIVE_LOCATION_AGE_MS
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Location.toCoordinate(): GeoCoordinate = GeoCoordinate(latitude, longitude)

private fun GeoCoordinate.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

private fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun MapView.addRouteOverlay(routePoints: List<GeoCoordinate>) {
    val geoPoints = routePoints.map { it.toGeoPoint() }
    overlays +=
        OsmPolyline(this).apply {
            setPoints(geoPoints)
            outlinePaint.color = AndroidColor.WHITE
            outlinePaint.strokeWidth = 24f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }
    overlays +=
        OsmPolyline(this).apply {
            setPoints(geoPoints)
            outlinePaint.color = AndroidColor.rgb(11, 87, 208)
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }
}

private fun createCampusMarker(
    context: Context,
    mapView: MapView,
    location: CampusLocation,
    isHighlighted: Boolean,
    onClick: () -> Unit,
): OsmMarker =
    OsmMarker(mapView).apply {
        position = location.coordinate.toGeoPoint()
        title = location.name
        snippet = formatCoordinate(location.coordinate)
        setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
        icon =
            createCircleDrawable(
                context = context,
                color = if (isHighlighted) AndroidColor.rgb(211, 47, 47) else AndroidColor.rgb(0, 137, 123),
                sizeDp = if (isHighlighted) 26 else 22,
            )
        setOnMarkerClickListener { marker, _ ->
            onClick()
            marker.showInfoWindow()
            true
        }
    }

private fun createLiveLocationMarker(
    context: Context,
    mapView: MapView,
    location: GeoCoordinate,
): OsmMarker =
    OsmMarker(mapView).apply {
        position = location.toGeoPoint()
        title = "My Live Location"
        snippet = formatCoordinate(location)
        setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
        icon =
            createCircleDrawable(
                context = context,
                color = AndroidColor.rgb(25, 118, 210),
                sizeDp = 24,
            )
    }

private fun createCircleDrawable(
    context: Context,
    color: Int,
    sizeDp: Int,
): GradientDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).roundToInt()
    val strokePx = (3 * density).roundToInt()

    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(strokePx, AndroidColor.WHITE)
        setSize(sizePx, sizePx)
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
    startLocations: List<CampusLocation>,
    destinationLocations: List<CampusLocation>,
    routeStartId: String?,
    routeEndId: String?,
    routeDistanceMeters: Int,
    isWaitingForLiveLocation: Boolean,
    onStartSelected: (CampusLocation) -> Unit,
    onEndSelected: (CampusLocation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CampusLocationDropdown(
            label = "Start",
            locations = startLocations,
            selectedLocationId = routeStartId,
            missingSelectionText =
                if (isWaitingForLiveLocation) {
                    "Waiting for live location"
                } else {
                    "Select start"
                },
            onSelected = onStartSelected,
        )
        CampusLocationDropdown(
            label = "Destination",
            locations = destinationLocations,
            selectedLocationId = routeEndId,
            missingSelectionText = "Select destination",
            onSelected = onEndSelected,
        )

        if (isWaitingForLiveLocation) {
            Text(
                text = "Waiting for your live location to calculate the route.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (routeStartId != null && routeEndId != null) {
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
    missingSelectionText: String,
    onSelected: (CampusLocation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = locations.find { it.id == selectedLocationId }?.name ?: missingSelectionText

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

private fun formatCoordinate(coordinate: GeoCoordinate): String =
    "${"%.6f".format(coordinate.latitude)}, ${"%.6f".format(coordinate.longitude)}"

private fun formatDistance(distanceMeters: Int): String {
    val distanceFeet = (distanceMeters * 3.28084).roundToInt()
    val walkMinutes = (distanceMeters / 80.0).coerceAtLeast(1.0).roundToInt()
    return "$distanceFeet ft - about $walkMinutes min"
}
