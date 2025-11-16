package com.example.dam_android.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.models.AddChildRequest
import com.example.dam_android.models.DeviceType
import com.example.dam_android.network.api.RetrofitClient
import com.example.dam_android.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQRCode: (String, String) -> Unit,
    onNavigateToLinkChild: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedDeviceType by remember { mutableStateOf(DeviceType.PHONE) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
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
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPeach)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(OrangeButton, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        tint = White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Add Child",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add a new child to your account",
                    fontSize = 14.sp,
                    color = Black.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // First Name Field
                Text(
                    text = "First Name :",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = OrangeButton,
                        unfocusedBorderColor = White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Last Name Field
                Text(
                    text = "Last Name :",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = OrangeButton,
                        unfocusedBorderColor = White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Device Type
                Text(
                    text = "Device Type :",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Phone Button
                    Button(
                        onClick = { selectedDeviceType = DeviceType.PHONE },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDeviceType == DeviceType.PHONE) OrangeButton else White,
                            contentColor = if (selectedDeviceType == DeviceType.PHONE) White else Black
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Phone",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Watch Button
                    Button(
                        onClick = { selectedDeviceType = DeviceType.WATCH },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDeviceType == DeviceType.WATCH) OrangeButton else White,
                            contentColor = if (selectedDeviceType == DeviceType.WATCH) White else Black
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Watch",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }


                Spacer(modifier = Modifier.height(32.dp))

                // Add Child Button
                Button(
                    onClick = {
                        if (firstName.isBlank() || lastName.isBlank()) {
                            Toast.makeText(
                                context,
                                "Please fill in all required fields",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        // Validate names - check for special characters that might cause issues
                        val namePattern = "^[a-zA-Z\\s-']+$".toRegex()
                        if (!firstName.trim().matches(namePattern) || !lastName.trim().matches(namePattern)) {
                            Toast.makeText(
                                context,
                                "Names can only contain letters, spaces, hyphens and apostrophes",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val childApi = RetrofitClient.childApi

                                // Capitalize first letter of names (like iOS does)
                                val formattedFirstName = firstName.trim().lowercase()
                                    .replaceFirstChar { it.uppercase() }
                                val formattedLastName = lastName.trim().lowercase()
                                    .replaceFirstChar { it.uppercase() }

                                val request = AddChildRequest(
                                    firstName = formattedFirstName,
                                    lastName = formattedLastName,
                                    deviceType = selectedDeviceType.name // PHONE or WATCH
                                )

                                Log.d("AddChildScreen", "📤 Sending request: firstName=${request.firstName}, lastName=${request.lastName}, deviceType=${request.deviceType}")

                                val response = childApi.addChild(request)

                                Log.d("AddChildScreen", "📥 Response code: ${response.code()}")
                                Log.d("AddChildScreen", "📥 Response success: ${response.isSuccessful}")

                                isLoading = false

                                if (response.isSuccessful && response.body() != null) {
                                    val childResponse = response.body()!!
                                    val qrCode = childResponse.qrCode
                                    val childName = "$firstName $lastName"

                                    Log.d("AddChildScreen", "✅ Child added successfully!")
                                    Log.d("AddChildScreen", "✅ Child ID: ${childResponse._id}")
                                    Log.d("AddChildScreen", "✅ QR Code: $qrCode")

                                    Toast.makeText(
                                        context,
                                        "Child added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onNavigateToQRCode(qrCode, childName)
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Log.e("AddChildScreen", "❌ Error response body: $errorBody")

                                    val errorMessage = try {
                                        if (errorBody != null) {
                                            val json = JSONObject(errorBody)

                                            // Check if message is an array
                                            if (json.has("message")) {
                                                val messageValue = json.get("message")
                                                when (messageValue) {
                                                    is String -> messageValue
                                                    else -> {
                                                        // It's an array, get the first message
                                                        val jsonArray = json.getJSONArray("message")
                                                        if (jsonArray.length() > 0) {
                                                            jsonArray.getString(0)
                                                        } else {
                                                            "Failed to add child"
                                                        }
                                                    }
                                                }
                                            } else {
                                                "Failed to add child"
                                            }
                                        } else {
                                            "Server returned error ${response.code()}"
                                        }
                                    } catch (e: Exception) {
                                        Log.e("AddChildScreen", "❌ Error parsing JSON: ${e.message}")
                                        "Server error: ${response.code()}"
                                    }

                                    Log.e("AddChildScreen", "❌ Showing error to user: $errorMessage")

                                    // Special message for 500 errors
                                    val displayMessage = if (response.code() == 500) {
                                        "⚠️ Server Error (500)\n\n" +
                                        "The backend is experiencing issues. This is not a problem with your data.\n\n" +
                                        "Please contact the backend team or try again later."
                                    } else {
                                        "⚠️ $errorMessage"
                                    }

                                    Toast.makeText(
                                        context,
                                        displayMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                Log.e("AddChildScreen", "💥 Exception occurred: ${e.message}", e)
                                Log.e("AddChildScreen", "💥 Exception type: ${e.javaClass.simpleName}")

                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}\n\nPlease try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeButton,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Add Child",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Black.copy(alpha = 0.2f))
                    )
                    Text(
                        text = "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black.copy(alpha = 0.5f)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Black.copy(alpha = 0.2f))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Link to Existing Child Button
                OutlinedButton(
                    onClick = onNavigateToLinkChild,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = White,
                        contentColor = OrangeButton
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, OrangeButton),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Link,
                            contentDescription = "Link",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Link to Existing Child",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info text
                Text(
                    text = "Already have a child registered? Link yourself as a parent by scanning their QR code.",
                    fontSize = 12.sp,
                    color = Black.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
