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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlin.math.cos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerZoneScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State management
    var hasLocationPermission by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var dangerZones by remember { mutableStateOf<List<DangerZone>>(emptyList()) }
    var children by remember { mutableStateOf<List<ChildModel>>(emptyList()) }
    var selectedZone by remember { mutableStateOf<DangerZone?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showEventsDialog by remember { mutableStateOf(false) }
    var showZonesList by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var zoneEvents by remember { mutableStateOf<List<DangerZoneEvent>>(emptyList()) }
    
    // Function to fetch danger zones
    fun fetchDangerZones() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = ApiService.getAllDangerZones()
            isLoading = false
            
            result.onSuccess { zones ->
                dangerZones = zones
                Log.d("DangerZoneScreen", "✅ Loaded ${zones.size} danger zones")
            }.onFailure { error ->
                errorMessage = error.message
                Log.e("DangerZoneScreen", "❌ Failed to load danger zones: ${error.message}")
            }
        }
    }
    
    // Function to fetch children
    fun fetchChildren() {
        scope.launch {
            val result = ApiService.getParentChildren()
            result.onSuccess { childrenList ->
                children = childrenList
                Log.d("DangerZoneScreen", "✅ Loaded ${childrenList.size} children")
            }.onFailure { error ->
                Log.e("DangerZoneScreen", "❌ Failed to load children: ${error.message}")
            }
        }
    }
    
    // Function to fetch zone events
    fun fetchZoneEvents(zoneId: String) {
        scope.launch {
            val result = ApiService.getDangerZoneEvents(zoneId)
            result.onSuccess { events ->
                zoneEvents = events
                Log.d("DangerZoneScreen", "✅ Loaded ${events.size} events")
            }.onFailure { error ->
                Log.e("DangerZoneScreen", "❌ Failed to load events: ${error.message}")
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
                showEventsDialog = false
            }.onFailure { error ->
                errorMessage = error.message
            }
        }
    }
    
    // Fetch data on first composition
    LaunchedEffect(Unit) {
        fetchDangerZones()
        fetchChildren()
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
    
    // Default location (Casablanca, Morocco)
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
                        // Create polygon circle
                        val circle = Polygon(updatedMapView)
                        circle.points = Polygon.pointsAsCircle(
                            GeoPoint(zone.center.lat, zone.center.lng),
                            zone.radiusMeters
                        )
                        
                        // Style based on status
                        if (zone.status == ZoneStatus.ACTIVE) {
                            circle.fillPaint.color = android.graphics.Color.argb(50, 255, 87, 34) // Transparent orange
                            circle.outlinePaint.color = android.graphics.Color.argb(200, 255, 87, 34) // Orange border
                        } else {
                            circle.fillPaint.color = android.graphics.Color.argb(30, 158, 158, 158) // Transparent gray
                            circle.outlinePaint.color = android.graphics.Color.argb(150, 158, 158, 158) // Gray border
                        }
                        circle.outlinePaint.strokeWidth = 3f
                        
                        updatedMapView.overlays.add(circle)
                        
                        // Add center marker
                        val marker = Marker(updatedMapView)
                        marker.position = GeoPoint(zone.center.lat, zone.center.lng)
                        marker.title = zone.name
                        marker.snippet = "${zone.radiusMeters.toInt()}m - ${zone.status}"
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        marker.setOnMarkerClickListener { _, _ ->
                            selectedZone = zone
                            fetchZoneEvents(zone.id)
                            showEventsDialog = true
                            true
                        }
                        
                        updatedMapView.overlays.add(marker)
                    } catch (e: Exception) {
                        Log.e("DangerZoneScreen", "Error creating zone overlay: ${e.message}")
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
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = OrangeButton,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Danger Zones",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
                }
            }
            
            IconButton(
                onClick = { fetchDangerZones() },
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
                .zIndex(2f)
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
        
        // Zones count badge
        if (dangerZones.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .zIndex(2f)
                    .clickable { showZonesList = true },
                shape = RoundedCornerShape(24.dp),
                color = White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = OrangeButton,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${dangerZones.size}",
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Active Zones",
                            fontSize = 12.sp,
                            color = Gray600,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap to view all",
                            fontSize = 14.sp,
                            color = Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = OrangeButton,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Floating action button to create zone
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .zIndex(3f)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            containerColor = OrangeButton,
            contentColor = White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Zone",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Create Zone",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        
        // Create/Edit Zone Dialog
        if (showCreateDialog || showEditDialog) {
            CreateEditZoneDialog(
                zone = if (showEditDialog) selectedZone else null,
                children = children,
                currentLocation = currentLocation ?: defaultLocation,
                onDismiss = {
                    showCreateDialog = false
                    showEditDialog = false
                },
                onConfirm = { name, desc, lat, lng, radius, selectedChildren, notifyEntry, notifyExit ->
                    scope.launch {
                        isLoading = true
                        val result = if (showEditDialog && selectedZone != null) {
                            ApiService.updateDangerZone(
                                zoneId = selectedZone!!.id,
                                name = name,
                                description = desc,
                                centerLat = lat,
                                centerLng = lng,
                                radiusMeters = radius,
                                children = selectedChildren,
                                notifyOnEntry = notifyEntry,
                                notifyOnExit = notifyExit
                            )
                        } else {
                            ApiService.createDangerZone(
                                name = name,
                                description = desc,
                                centerLat = lat,
                                centerLng = lng,
                                radiusMeters = radius,
                                children = selectedChildren,
                                notifyOnEntry = notifyEntry,
                                notifyOnExit = notifyExit
                            )
                        }
                        isLoading = false
                        
                        result.onSuccess {
                            fetchDangerZones()
                            showCreateDialog = false
                            showEditDialog = false
                        }.onFailure { error ->
                            errorMessage = error.message
                        }
                    }
                }
            )
        }
        
        // Zone Events Dialog
        if (showEventsDialog && selectedZone != null) {
            ZoneEventsDialog(
                zone = selectedZone!!,
                events = zoneEvents,
                onDismiss = {
                    showEventsDialog = false
                    selectedZone = null
                },
                onEdit = {
                    showEventsDialog = false
                    showEditDialog = true
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
                            showEventsDialog = false
                        }.onFailure { error ->
                            errorMessage = error.message
                        }
                    }
                }
            )
        }
        
        // Zones List Dialog
        if (showZonesList) {
            ZonesListDialog(
                zones = dangerZones,
                onDismiss = { showZonesList = false },
                onZoneClick = { zone ->
                    showZonesList = false
                    selectedZone = zone
                    fetchZoneEvents(zone.id)
                    showEventsDialog = true
                    mapView?.controller?.animateTo(GeoPoint(zone.center.lat, zone.center.lng))
                    mapView?.controller?.setZoom(15.0)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditZoneDialog(
    zone: DangerZone?,
    children: List<ChildModel>,
    currentLocation: GeoPoint,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Double, Double, Double, List<String>, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(zone?.name ?: "") }
    var description by remember { mutableStateOf(zone?.description ?: "") }
    var latitude by remember { mutableStateOf(zone?.center?.lat?.toString() ?: currentLocation.latitude.toString()) }
    var longitude by remember { mutableStateOf(zone?.center?.lng?.toString() ?: currentLocation.longitude.toString()) }
    var radius by remember { mutableStateOf(zone?.radiusMeters?.toString() ?: "500") }
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
                        text = if (zone != null) "Edit Zone" else "Create Danger Zone",
                        fontSize = 24.sp,
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
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
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton
                    )
                )
                
                // Location section
                Text(
                    text = "📍 Location",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeButton,
                            focusedLabelColor = OrangeButton
                        )
                    )
                    
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeButton,
                            focusedLabelColor = OrangeButton
                        )
                    )
                }
                
                // Radius field
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (meters)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text("Recommended: 100-1000m") },
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
                            Text(
                                text = "👶 Monitor Children",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                if (showChildrenPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = OrangeButton
                            )
                        }
                        
                        Text(
                            text = if (selectedChildren.isEmpty()) "All children" else "${selectedChildren.size} selected",
                            fontSize = 12.sp,
                            color = Gray600
                        )
                        
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
                Text(
                    text = "🔔 Notifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GradientEnd
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                colors = SwitchDefaults.colors(checkedThumbColor = OrangeButton, checkedTrackColor = OrangeButton.copy(alpha = 0.5f))
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
                                colors = SwitchDefaults.colors(checkedThumbColor = OrangeButton, checkedTrackColor = OrangeButton.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OrangeButton
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val lat = latitude.toDoubleOrNull() ?: currentLocation.latitude
                            val lng = longitude.toDoubleOrNull() ?: currentLocation.longitude
                            val rad = radius.toDoubleOrNull() ?: 500.0
                            onConfirm(name, description.ifEmpty { null }, lat, lng, rad, selectedChildren, notifyOnEntry, notifyOnExit)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeButton)
                    ) {
                        Text(if (zone != null) "Update" else "Create")
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneEventsDialog(
    zone: DangerZone,
    events: List<DangerZoneEvent>,
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
                .fillMaxHeight(0.8f)
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow(Icons.Default.LocationOn, "Center", String.format("%.4f, %.4f", zone.center.lat, zone.center.lng))
                        InfoRow(Icons.Default.Place, "Radius", "${zone.radiusMeters.toInt()} meters")
                        InfoRow(
                            Icons.Default.CheckCircle,
                            "Status",
                            zone.status.name,
                            zone.status == ZoneStatus.ACTIVE
                        )
                        if (zone.children.isNotEmpty()) {
                            InfoRow(Icons.Default.Person, "Children", "${zone.children.size} monitored")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Events section
                Text(
                    text = "📊 Event History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Events list
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Gray600
                            )
                            Text(
                                text = "No events yet",
                                color = Gray600,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events) { event ->
                            EventItem(event)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                        Text(if (zone.status == ZoneStatus.ACTIVE) "Disable" else "Enable")
                    }
                    
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeButton)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
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
            text = { Text("Are you sure you want to delete \"${zone.name}\"? This action cannot be undone and will also delete all event history.") },
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

@Composable
fun ZonesListDialog(
    zones: List<DangerZone>,
    onDismiss: () -> Unit,
    onZoneClick: (DangerZone) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Danger Zones",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Gray600)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(zones) { zone ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (zone.status == ZoneStatus.ACTIVE) GradientStart else Gray600.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onZoneClick(zone) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (zone.status == ZoneStatus.ACTIVE) OrangeButton else Gray600,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = zone.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Black
                                    )
                                    Text(
                                        text = "${zone.radiusMeters.toInt()}m radius",
                                        fontSize = 14.sp,
                                        color = Gray600
                                    )
                                    zone.description?.let {
                                        Text(
                                            text = it,
                                            fontSize = 12.sp,
                                            color = Gray600,
                                            maxLines = 1
                                        )
                                    }
                                }
                                
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (zone.status == ZoneStatus.ACTIVE) 
                                        OrangeButton.copy(alpha = 0.2f) 
                                    else 
                                        Gray600.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = zone.status.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (zone.status == ZoneStatus.ACTIVE) OrangeButton else Gray600
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(event: DangerZoneEvent) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (event.type == EventType.ENTER) 
            OrangeButton.copy(alpha = 0.1f) 
        else 
            Color(0xFF4CAF50).copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (event.type == EventType.ENTER) OrangeButton else Color(0xFF4CAF50),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (event.type == EventType.ENTER) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.childName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (event.type == EventType.ENTER) "Entered zone" else "Exited zone",
                    fontSize = 12.sp,
                    color = Gray600
                )
                Text(
                    text = event.createdAt,
                    fontSize = 10.sp,
                    color = Gray600
                )
            }
            
            if (event.notificationSent) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notification sent",
                    tint = OrangeButton,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isActive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isActive) OrangeButton else Gray600,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Gray600,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) OrangeButton else Black
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
                Log.e("DangerZoneScreen", "Error getting location from $provider: ${e.message}")
            }
        }
        
        callback(bestLocation)
    } catch (e: Exception) {
        Log.e("DangerZoneScreen", "Error getting location: ${e.message}")
        callback(null)
    }
}

