package com.example.dam_android.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun LinkChildQrScreen(
    onNavigateBack: () -> Unit,
    onLinkSuccess: () -> Unit
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var manualQrCode by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var linkedChildName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            errorMessage = "Camera permission denied"
        }
    }

    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        hasCameraPermission = permissionStatus == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun linkToChild(qrCode: String) {
        if (isLoading) return
        
        Log.d("LinkChildQrScreen", "🔗 Attempting to link with QR code: $qrCode")
        isLoading = true
        errorMessage = null
        
        coroutineScope.launch {
            try {
                val result = ApiService.linkParentByQr(qrCode)
                isLoading = false
                
                result.onSuccess { response ->
                    Log.d("LinkChildQrScreen", "✅ Successfully linked to: ${response.child.firstName} ${response.child.lastName}")
                    linkedChildName = "${response.child.firstName} ${response.child.lastName}"
                    showSuccessDialog = true
                }.onFailure { error ->
                    Log.e("LinkChildQrScreen", "❌ Link failed: ${error.message}")
                    errorMessage = error.message
                    Toast.makeText(
                        context,
                        "Failed to link: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                isLoading = false
                Log.e("LinkChildQrScreen", "💥 Exception: ${e.message}", e)
                errorMessage = e.message
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Successfully Linked!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You are now linked to:",
                        fontSize = 16.sp,
                        color = Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = linkedChildName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeButton,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You can now chat with this child, view their location, and receive safety notifications.",
                        fontSize = 14.sp,
                        color = Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onLinkSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeButton
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPeach)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OrangeButton)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Link to Child",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }
            }

            if (!hasCameraPermission) {
                // Permission denied view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Black.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please grant camera permission to scan QR codes",
                        fontSize = 14.sp,
                        color = Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeButton
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }
            } else if (isScanning) {
                // Camera view
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(
                        onQrCodeScanned = { qrCode ->
                            isScanning = false
                            linkToChild(qrCode)
                        }
                    )
                    
                    // Scanning overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.3f))
                        
                        // Scanning frame
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = White.copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Scan Child's QR Code",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Position the QR code within the frame",
                                    fontSize = 14.sp,
                                    color = Black.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(0.7f))
                        
                        // Cancel button
                        OutlinedButton(
                            onClick = { isScanning = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = White,
                                contentColor = Black
                            ),
                            border = BorderStroke(2.dp, White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Loading overlay
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = White)
                        }
                    }
                }
            } else {
                // Main screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(OrangeButton.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = OrangeButton
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Link to Existing Child",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Scan the child's QR code to link yourself as a parent",
                        fontSize = 16.sp,
                        color = Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // Scan button
                    Button(
                        onClick = { isScanning = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeButton
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Scan QR Code",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Black.copy(alpha = 0.1f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Manual entry
                    Text(
                        text = "Or enter code manually",
                        fontSize = 14.sp,
                        color = Black.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = manualQrCode,
                        onValueChange = { manualQrCode = it },
                        label = { Text("QR Code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                            focusedBorderColor = OrangeButton,
                            unfocusedBorderColor = Black.copy(alpha = 0.2f)
                        ),
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (manualQrCode.isNotBlank()) {
                                linkToChild(manualQrCode.trim())
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please enter a QR code",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        enabled = !isLoading && manualQrCode.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Link Now",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Error message
                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFC62828),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Info card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = White.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "ℹ️ How it works",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "1. Ask the main parent to show you the child's QR code\n\n" +
                                        "2. Scan the QR code or enter it manually\n\n" +
                                        "3. You'll be linked as a parent and can:\n" +
                                        "   • Chat with the child\n" +
                                        "   • View their location\n" +
                                        "   • Receive safety notifications\n" +
                                        "   • Create danger zones",
                                fontSize = 14.sp,
                                color = Black.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(onQrCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var hasScanned by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val barcodeScanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            processImageProxy(barcodeScanner, imageProxy, hasScanned) { qrCode ->
                                if (!hasScanned) {
                                    hasScanned = true
                                    onQrCodeScanned(qrCode)
                                }
                            }
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error binding camera", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    hasScanned: Boolean,
    onQrCodeScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null && !hasScanned) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.valueType == Barcode.TYPE_TEXT && barcode.rawValue != null) {
                        Log.d("CameraPreview", "✅ QR Code scanned: ${barcode.rawValue}")
                        onQrCodeScanned(barcode.rawValue!!)
                        break
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CameraPreview", "❌ Error scanning QR code", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

