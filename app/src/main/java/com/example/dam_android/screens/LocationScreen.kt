package com.example.dam_android.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.dam_android.R
import com.example.dam_android.models.*
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.ui.theme.*
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State management
    var hasLocationPermission by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var children by remember { mutableStateOf<List<ChildModel>>(emptyList()) }
    var dangerZones by remember { mutableStateOf<List<DangerZone>>(emptyList()) }
    var selectedChild by remember { mutableStateOf<ChildModel?>(null) }
    var selectedZone by remember { mutableStateOf<DangerZone?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentChildIndex by remember { mutableStateOf(0) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Danger zone creation state
    var isCreatingZone by remember { mutableStateOf(false) }
    var creationCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var creationRadius by remember { mutableStateOf(500.0) }
    var showZoneForm by remember { mutableStateOf(false) }
    var isDraggingZone by remember { mutableStateOf(false) }
    
    // Zone editing
    var editingZone by remember { mutableStateOf<DangerZone?>(null) }
    var editRadius by remember { mutableStateOf(500.0) }
    var editCenter by remember { mutableStateOf<GeoPoint?>(null) }
    
    // Animated radius for visual feedback
    var animatedRadius by remember { mutableStateOf(500f) }
    val radiusAnimation = animateFloatAsState(
        targetValue = if (isCreatingZone) creationRadius.toFloat() else editRadius.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    // Function to fetch children locations
    fun fetchChildrenLocations() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = ApiService.getParentChildren()
            isLoading = false
            
            result.onSuccess { childrenList ->
                children = childrenList.filter { it.location != null }
                currentChildIndex = 0
                Log.d("LocationScreen", "✅ Loaded ${children.size} children with locations")
            }.onFailure { error ->
                errorMessage = error.message
                Log.e("LocationScreen", "❌ Failed to load children: ${error.message}")
            }
        }
    }
    
    // Function to cycle through children
    fun goToNextChild() {
        if (children.isEmpty()) {
            Log.w("LocationScreen", "⚠️ No children available to navigate to")
            return
        }
        
        val nextIndex = (currentChildIndex + 1) % children.size
        
        if (nextIndex >= children.size || nextIndex < 0) {
            Log.e("LocationScreen", "❌ Invalid index: $nextIndex for children size: ${children.size}")
            currentChildIndex = 0
            return
        }
        
        val child = children[nextIndex]
        
        if (child.location == null) {
            Log.w("LocationScreen", "⚠️ Child ${child.firstName} has no location data, skipping")
            currentChildIndex = nextIndex
            goToNextChild()
            return
        }
        
        val location = child.location!!
        
        if (location.lat == 0.0 && location.lng == 0.0) {
            Log.w("LocationScreen", "⚠️ Child ${child.firstName} has invalid coordinates (0,0), skipping")
            currentChildIndex = nextIndex
            goToNextChild()
            return
        }
        
        if (mapView == null) {
            Log.e("LocationScreen", "❌ MapView is null, cannot navigate")
            return
        }
        
        try {
            currentChildIndex = nextIndex
            val geoPoint = GeoPoint(location.lat, location.lng)
            
            mapView?.controller?.apply {
                setZoom(16.0)
                animateTo(geoPoint)
            }
            
            selectedChild = child
            
            Log.d("LocationScreen", "✅ Successfully navigated to ${child.firstName} ${child.lastName} at (${location.lat}, ${location.lng})")
        } catch (e: Exception) {
            Log.e("LocationScreen", "❌ Error navigating to child: ${e.message}", e)
            currentChildIndex = 0
        }
    }
    
    // Function to fetch danger zones
    fun fetchDangerZones() {
        scope.launch {
            val result = ApiService.getAllDangerZones()
            result.onSuccess { zones ->
                dangerZones = zones
                Log.d("LocationScreen", "✅ Loaded ${zones.size} danger zones")
            }.onFailure { error ->
                Log.e("LocationScreen", "❌ Failed to load danger zones: ${error.message}")
            }
        }
    }
    
    // Function to start creating a zone
    fun startCreatingZone() {
        isCreatingZone = true
        creationCenter = mapView?.mapCenter as? GeoPoint ?: currentLocation ?: GeoPoint(33.5731, -7.6598)
        creationRadius = 500.0
        showZoneForm = false
    }
    
    // Function to save the zone
    fun saveZone(name: String, description: String?, selectedChildren: List<String>, notifyEntry: Boolean, notifyExit: Boolean) {
        scope.launch {
            isLoading = true
            val center = if (editingZone != null) editCenter!! else creationCenter!!
            val radius = if (editingZone != null) editRadius else creationRadius
            
            val result = if (editingZone != null) {
                ApiService.updateDangerZone(
                    zoneId = editingZone!!.id,
                    name = name,
                    description = description,
                    centerLat = center.latitude,
                    centerLng = center.longitude,
                    radiusMeters = radius,
                    children = selectedChildren,
                    notifyOnEntry = notifyEntry,
                    notifyOnExit = notifyExit
                )
            } else {
                ApiService.createDangerZone(
                    name = name,
                    description = description,
                    centerLat = center.latitude,
                    centerLng = center.longitude,
                    radiusMeters = radius,
                    children = selectedChildren,
                    notifyOnEntry = notifyEntry,
                    notifyOnExit = notifyExit
                )
            }
            
            isLoading = false
            
            result.onSuccess {
                isCreatingZone = false
                editingZone = null
                showZoneForm = false
                fetchDangerZones()
            }.onFailure { error ->
                errorMessage = error.message
            }
        }
    }
    
    // Function to delete zone
    fun deleteZone(zoneId: String) {
        scope.launch {
            isLoading = true
            val result = ApiService.deleteDangerZone(zoneId)
            isLoading = false
            
            result.onSuccess {
                fetchDangerZones()
                selectedZone = null
            }.onFailure { error ->
                errorMessage = error.message
            }
        }
    }
    
    // Fetch data on first composition
    LaunchedEffect(Unit) {
        fetchChildrenLocations()
        fetchDangerZones()
    }
    
    // Request location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            getCurrentLocationHelper(context) { location ->
                location?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                }
            }
        }
    }
    
    // Check permission on composition
    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        hasLocationPermission = permissionStatus == PackageManager.PERMISSION_GRANTED
        
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getCurrentLocationHelper(context) { location ->
                location?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                }
            }
        }
    }
    
    // Default location
    val defaultLocation = GeoPoint(33.5731, -7.6598)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        // Map View
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0
                    controller.setZoom(13.0)
                    controller.setCenter(currentLocation ?: defaultLocation)
                    
                    if (hasLocationPermission) {
                        val myLocationOverlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx),
                            this
                        )
                        myLocationOverlay.enableMyLocation()
                        overlays.add(myLocationOverlay)
                    }
                    
                    // Add map listener for dragging zone center
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            if (isCreatingZone || editingZone != null) {
                                val newCenter = mapCenter as? GeoPoint
                                if (newCenter != null) {
                                    if (isCreatingZone) {
                                        creationCenter = newCenter
                                    } else if (editingZone != null) {
                                        editCenter = newCenter
                                    }
                                }
                            }
                            return true
                        }
                        
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            return true
                        }
                    })
                    
                    mapView = this
                }
            },
            update = { updatedMapView ->
                mapView = updatedMapView
                
                // Clear existing overlays except location overlay
                updatedMapView.overlays.removeAll { it is Marker || it is Polygon }
                
                // Add danger zones as circles
                dangerZones.forEach { zone ->
                    try {
                        val circle = Polygon(updatedMapView)
                        circle.points = Polygon.pointsAsCircle(
                            GeoPoint(zone.center.lat, zone.center.lng),
                            zone.radiusMeters
                        )
                        
                        if (zone.status == ZoneStatus.ACTIVE) {
                            circle.fillPaint.color = android.graphics.Color.argb(50, 255, 87, 34)
                            circle.outlinePaint.color = android.graphics.Color.argb(200, 255, 87, 34)
                        } else {
                            circle.fillPaint.color = android.graphics.Color.argb(30, 158, 158, 158)
                            circle.outlinePaint.color = android.graphics.Color.argb(150, 158, 158, 158)
                        }
                        circle.outlinePaint.strokeWidth = 3f
                        
                        updatedMapView.overlays.add(circle)
                        
                        // Add center marker
                        val marker = Marker(updatedMapView)
                        marker.position = GeoPoint(zone.center.lat, zone.center.lng)
                        marker.title = zone.name
                        marker.snippet = "${zone.radiusMeters.toInt()}m"
                        
                        marker.setOnMarkerClickListener { _, _ ->
                            selectedZone = zone
                            true
                        }
                        
                        updatedMapView.overlays.add(marker)
                    } catch (e: Exception) {
                        Log.e("LocationScreen", "Error creating zone overlay: ${e.message}")
                    }
                }
                
                // Draw creation/edit circle
                if (isCreatingZone && creationCenter != null) {
                    val circle = Polygon(updatedMapView)
                    circle.points = Polygon.pointsAsCircle(creationCenter!!, creationRadius)
                    circle.fillPaint.color = android.graphics.Color.argb(80, 76, 175, 80) // Green
                    circle.outlinePaint.color = android.graphics.Color.argb(255, 76, 175, 80)
                    circle.outlinePaint.strokeWidth = 5f
                    updatedMapView.overlays.add(circle)
                    
                    // Add crosshair at center
                    val centerMarker = Marker(updatedMapView)
                    centerMarker.position = creationCenter
                    centerMarker.icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                    centerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    updatedMapView.overlays.add(centerMarker)
                }
                
                if (editingZone != null && editCenter != null) {
                    val circle = Polygon(updatedMapView)
                    circle.points = Polygon.pointsAsCircle(editCenter!!, editRadius)
                    circle.fillPaint.color = android.graphics.Color.argb(80, 33, 150, 243) // Blue
                    circle.outlinePaint.color = android.graphics.Color.argb(255, 33, 150, 243)
                    circle.outlinePaint.strokeWidth = 5f
                    updatedMapView.overlays.add(circle)
                    
                    val centerMarker = Marker(updatedMapView)
                    centerMarker.position = editCenter
                    centerMarker.icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                    centerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    updatedMapView.overlays.add(centerMarker)
                }
                
                // Add lollipop markers for each child
                children.forEach { child ->
                    child.location?.let { location ->
                        if (location.lat != 0.0 || location.lng != 0.0) {
                            try {
                                val marker = Marker(updatedMapView)
                                marker.position = GeoPoint(location.lat, location.lng)
                                marker.title = "${child.firstName} ${child.lastName}"
                                marker.snippet = "Tap to view details"
                                
                                try {
                                    val lollipopDrawable = ContextCompat.getDrawable(context, R.drawable.ic_lollipop)
                                    marker.icon = lollipopDrawable
                                } catch (e: Exception) {
                                    Log.e("LocationScreen", "Failed to load lollipop icon: ${e.message}")
                                }
                                
                                marker.setOnMarkerClickListener { _, _ ->
                                    selectedChild = child
                                    currentChildIndex = children.indexOf(child).coerceAtLeast(0)
                                    true
                                }
                                
                                updatedMapView.overlays.add(marker)
                            } catch (e: Exception) {
                                Log.e("LocationScreen", "Error creating marker: ${e.message}")
                            }
                        }
                    }
                }
                
                updatedMapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .shadow(8.dp, CircleShape)
                        .background(White, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Black
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OrangeButton,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isCreatingZone) "Creating Zone..." else if (editingZone != null) "Editing Zone" else "Locations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
                }
            }
            
            IconButton(
                onClick = { 
                    fetchChildrenLocations()
                    fetchDangerZones()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .shadow(8.dp, CircleShape)
                    .background(White, CircleShape)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = if (isLoading) Gray600 else OrangeButton
                )
            }
        }
        
        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(2f),
                color = OrangeButton
            )
        }
        
        // Error message
        AnimatedVisibility(
            visible = errorMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .zIndex(2f),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            errorMessage?.let { error ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Info badge showing counts
        AnimatedVisibility(
            visible = !isCreatingZone && editingZone == null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .zIndex(2f),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = White,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Children count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = OrangeButton,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${children.size}",
                                    color = White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Text(
                            text = "Children",
                            fontSize = 12.sp,
                            color = Gray600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Divider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = Gray600.copy(alpha = 0.3f)
                    )
                    
                    // Zones count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${dangerZones.size}",
                                    color = White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Text(
                            text = "Zones",
                            fontSize = 12.sp,
                            color = Gray600,
                            fontWeight = FontWeight.Medium
                        )
                        }
                    }
                    
                    // Current child name (if available)
                    if (children.isNotEmpty() && currentChildIndex < children.size) {
                        val currentChild = children[currentChildIndex]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OrangeButton.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "📍 ${currentChild.firstName} ${currentChild.lastName}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeButton
                            )
                        }
                    }
                }
            }
        }
        
        // Creation/Edit mode instruction
        AnimatedVisibility(
            visible = isCreatingZone || editingZone != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .zIndex(2f),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isCreatingZone) Color(0xFF4CAF50) else Color(0xFF2196F3),
                shadowElevation = 12.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Drag map to position zone center",
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "Current radius: ${if (isCreatingZone) creationRadius.toInt() else editRadius.toInt()}m",
                        color = White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Radius control buttons (when creating/editing)
        AnimatedVisibility(
            visible = isCreatingZone || editingZone != null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .zIndex(3f),
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Increase radius
                FloatingActionButton(
                    onClick = {
                        if (isCreatingZone) {
                            creationRadius = (creationRadius + 50).coerceAtMost(5000.0)
                        } else if (editingZone != null) {
                            editRadius = (editRadius + 50).coerceAtMost(5000.0)
                        }
                    },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = White,
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase radius",
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Current radius display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "${if (isCreatingZone) creationRadius.toInt() else editRadius.toInt()}m",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Black
                    )
                }
                
                // Decrease radius
                FloatingActionButton(
                    onClick = {
                        if (isCreatingZone) {
                            creationRadius = (creationRadius - 50).coerceAtLeast(50.0)
                        } else if (editingZone != null) {
                            editRadius = (editRadius - 50).coerceAtLeast(50.0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = White,
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease radius",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        // Bottom action buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .zIndex(3f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Create/Edit mode action buttons
            AnimatedVisibility(
                visible = isCreatingZone || editingZone != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = {
                            isCreatingZone = false
                            editingZone = null
                            creationCenter = null
                            editCenter = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = White,
                            contentColor = OrangeButton
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, OrangeButton)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    // Continue button
                    Button(
                        onClick = { showZoneForm = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCreatingZone) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Normal mode buttons
            AnimatedVisibility(
                visible = !isCreatingZone && editingZone == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Next Child button (only if 2+ children)
                    if (children.size > 1) {
                        FloatingActionButton(
                            onClick = { goToNextChild() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp)),
                            containerColor = OrangeButton,
                            contentColor = White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = "Next Child",
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Next Child",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    
                    // Add Danger Zone button
                    FloatingActionButton(
                        onClick = { startCreatingZone() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp)),
                        containerColor = Color(0xFF4CAF50),
                        contentColor = White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AddLocationAlt,
                                contentDescription = "Add Zone",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Add Danger Zone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Zone form dialog
        if (showZoneForm) {
            ZoneFormDialog(
                zone = editingZone,
                children = children,
                onDismiss = {
                    showZoneForm = false
                },
                onSave = { name, desc, selectedChildren, notifyEntry, notifyExit ->
                    saveZone(name, desc, selectedChildren, notifyEntry, notifyExit)
                    showZoneForm = false
                }
            )
        }
        
        // Child details dialog
        selectedChild?.let { child ->
            ChildDetailsDialog(
                child = child,
                onDismiss = { selectedChild = null }
            )
        }
        
        // Zone details dialog
        if (selectedZone != null && !isCreatingZone && editingZone == null) {
            ZoneDetailsDialog(
                zone = selectedZone!!,
                onDismiss = { selectedZone = null },
                onEdit = {
                    editingZone = selectedZone
                    editCenter = GeoPoint(selectedZone!!.center.lat, selectedZone!!.center.lng)
                    editRadius = selectedZone!!.radiusMeters
                    selectedZone = null
                },
                onDelete = {
                    deleteZone(selectedZone!!.id)
                },
                onToggleStatus = {
                    scope.launch {
                        val newStatus = if (selectedZone!!.status == ZoneStatus.ACTIVE) "INACTIVE" else "ACTIVE"
                        val result = ApiService.updateDangerZone(
                            zoneId = selectedZone!!.id,
                            status = newStatus
                        )
                        result.onSuccess {
                            fetchDangerZones()
                            selectedZone = null
                        }.onFailure { error ->
                            errorMessage = error.message
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ZoneFormDialog(
    zone: DangerZone?,
    children: List<ChildModel>,
    onDismiss: () -> Unit,
    onSave: (String, String?, List<String>, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(zone?.name ?: "") }
    var description by remember { mutableStateOf(zone?.description ?: "") }
    var selectedChildren by remember { mutableStateOf(zone?.children ?: emptyList()) }
    var notifyOnEntry by remember { mutableStateOf(zone?.notifyOnEntry ?: true) }
    var notifyOnExit by remember { mutableStateOf(zone?.notifyOnExit ?: false) }
    var showChildrenPicker by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (zone != null) "Edit Zone Details" else "Name Your Zone",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Gray600)
                    }
                }
                
                Divider()
                
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Zone Name *") },
                    placeholder = { Text("e.g., School Area") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null, tint = OrangeButton)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton
                    )
                )
                
                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Add details about this zone") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Gray600)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton
                    )
                )
                
                // Children selection
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GradientStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChildrenPicker = !showChildrenPicker }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = OrangeButton,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Monitor Children",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (selectedChildren.isEmpty()) "All children" else "${selectedChildren.size} selected",
                                        fontSize = 12.sp,
                                        color = Gray600
                                    )
                                }
                            }
                            Icon(
                                if (showChildrenPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = OrangeButton
                            )
                        }
                        
                        AnimatedVisibility(visible = showChildrenPicker) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                children.forEach { child ->
                                    val isSelected = selectedChildren.contains(child._id)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) OrangeButton.copy(alpha = 0.1f) else White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedChildren = if (isSelected) {
                                                    selectedChildren - child._id
                                                } else {
                                                    selectedChildren + child._id
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = OrangeButton)
                                            )
                                            Text(
                                                text = "${child.firstName} ${child.lastName}",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Notification settings
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GradientEnd
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = OrangeButton,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Notifications",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Notify on Entry",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Alert when child enters zone",
                                    fontSize = 12.sp,
                                    color = Gray600
                                )
                            }
                            Switch(
                                checked = notifyOnEntry,
                                onCheckedChange = { notifyOnEntry = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OrangeButton,
                                    checkedTrackColor = OrangeButton.copy(alpha = 0.5f)
                                )
                            )
                        }
                        
                        Divider()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Notify on Exit",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Alert when child leaves zone",
                                    fontSize = 12.sp,
                                    color = Gray600
                                )
                            }
                            Switch(
                                checked = notifyOnExit,
                                onCheckedChange = { notifyOnExit = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OrangeButton,
                                    checkedTrackColor = OrangeButton.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
                
                // Save button
                Button(
                    onClick = {
                        onSave(
                            name,
                            description.ifEmpty { null },
                            selectedChildren,
                            notifyOnEntry,
                            notifyOnExit
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeButton)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (zone != null) "Update Zone" else "Create Zone",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ChildDetailsDialog(
    child: ChildModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(OrangeButton, Orange700)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = White.copy(alpha = 0.2f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "${child.firstName} ${child.lastName}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Location details
                child.location?.let { location ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GradientStart
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = OrangeButton,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Location",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Latitude", color = Gray600, fontSize = 13.sp)
                                Text(
                                    String.format("%.6f", location.lat),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Longitude", color = Gray600, fontSize = 13.sp)
                                Text(
                                    String.format("%.6f", location.lng),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            location.updatedAt?.let { updatedAt ->
                                Divider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🕐 Updated: $updatedAt",
                                        fontSize = 11.sp,
                                        color = Gray600
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Device info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GradientEnd
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = OrangeButton.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (child.deviceType == "PHONE") "📱" else "⌚",
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            Column {
                                Text("Device", fontSize = 11.sp, color = Gray600)
                                Text(
                                    child.deviceType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (child.isOnline) 
                                OrangeButton.copy(alpha = 0.1f) 
                            else 
                                Gray600.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (child.isOnline) OrangeButton else Gray600,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = if (child.isOnline) "Online" else "Offline",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (child.isOnline) OrangeButton else Gray600
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeButton),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Close",
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ZoneDetailsDialog(
    zone: DangerZone,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = zone.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        zone.description?.let {
                            Text(
                                text = it,
                                fontSize = 14.sp,
                                color = Gray600,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Gray600)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Zone info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GradientStart
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Gray600,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Center", fontSize = 14.sp, color = Gray600)
                            }
                            Text(
                                String.format("%.4f, %.4f", zone.center.lat, zone.center.lng),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Gray600,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Radius", fontSize = 14.sp, color = Gray600)
                            }
                            Text(
                                "${zone.radiusMeters.toInt()} meters",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (zone.status == ZoneStatus.ACTIVE) OrangeButton else Gray600,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Status", fontSize = 14.sp, color = Gray600)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (zone.status == ZoneStatus.ACTIVE) 
                                    OrangeButton.copy(alpha = 0.1f) 
                                else 
                                    Gray600.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    zone.status.name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (zone.status == ZoneStatus.ACTIVE) OrangeButton else Gray600
                                )
                            }
                        }
                        
                        if (zone.children.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Gray600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Children", fontSize = 14.sp, color = Gray600)
                                }
                                Text(
                                    "${zone.children.size} monitored",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Notification info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GradientEnd
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = OrangeButton,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Notifications", fontWeight = FontWeight.Bold)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Entry", fontSize = 14.sp, color = Gray600)
                            Icon(
                                if (zone.notifyOnEntry) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (zone.notifyOnEntry) Color(0xFF4CAF50) else Gray600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exit", fontSize = 14.sp, color = Gray600)
                            Icon(
                                if (zone.notifyOnExit) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (zone.notifyOnExit) Color(0xFF4CAF50) else Gray600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleStatus,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (zone.status == ZoneStatus.ACTIVE) Gray600 else OrangeButton
                        )
                    ) {
                        Icon(
                            if (zone.status == ZoneStatus.ACTIVE) Icons.Default.Close else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (zone.status == ZoneStatus.ACTIVE) "Disable" else "Enable", fontSize = 14.sp)
                    }
                    
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeButton)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 14.sp)
                    }
                    
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Zone?") },
            text = { Text("Are you sure you want to delete \"${zone.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getCurrentLocationHelper(context: Context, callback: (Location?) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        
        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) {
                    bestLocation = location
                }
            } catch (e: SecurityException) {
                Log.e("LocationScreen", "Error getting location from $provider: ${e.message}")
            }
        }
        
        callback(bestLocation)
    } catch (e: Exception) {
        Log.e("LocationScreen", "Error getting location: ${e.message}")
        callback(null)
    }
}

