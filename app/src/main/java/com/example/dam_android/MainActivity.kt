package com.example.dam_android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.dam_android.network.api.RetrofitClient
import com.example.dam_android.network.local.SessionManager
import com.example.dam_android.models.UserRole
import com.example.dam_android.screens.*
import com.example.dam_android.ui.theme.DamAndroidTheme
import android.net.Uri
import com.example.dam_android.screens.ChatRoomScreen

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "onCreate: Démarrage MainActivity")

            // Initialiser RetrofitClient avec SessionManager
            val sessionManager = SessionManager.getInstance(this)
            RetrofitClient.init(sessionManager)
            Log.d(TAG, "onCreate: RetrofitClient initialisé")

            setContent {
                DamAndroidTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }

            Log.d(TAG, "onCreate: MainActivity prête")

        } catch (e: Exception) {
            Log.e(TAG, "onCreate: ERREUR", e)
            e.printStackTrace()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToWelcome = {
                    navController.navigate("welcome") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("welcome") {
            WelcomeScreen(
                onNavigateToSignIn = {
                    navController.navigate("signin")
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                }
            )
        }

        composable("signin") {
            SignInScreen(
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
                onNavigateToForgotPassword = {
                    navController.navigate("forgot_password")
                },
                onNavigateToParentHome = {
                    navController.navigate("parent_home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChildHome = {
                    navController.navigate("child_home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChildQrLogin = {
                    navController.navigate("login_child_qr")
                }
            )
        }

        composable("login_child_qr") {
            LoginChildQrScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChildHome = {
                    navController.navigate("child_home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                onNavigateToSignIn = {
                    navController.popBackStack()
                },
                onNavigateToVerification = { email ->
                    navController.navigate("verification/$email")
                }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResetPassword = { email ->
                    navController.navigate("reset_password/$email")
                }
            )
        }

        composable(
            route = "reset_password/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetSuccess = {
                    navController.navigate("signin") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "verification/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerificationScreen(
                email = email,
                onNavigateToParentHome = {
                    navController.navigate("parent_home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChildHome = {
                    navController.navigate("child_home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("parent_home") {
            ParentHomeScreen(
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToChild = {
                    navController.navigate("child_management")
                },
                onNavigateToLocation = {
                    navController.navigate("location")
                },
                onNavigateToChat = {
                    navController.navigate("chat")
                }
            )
        }

        composable("child_home") {
            ChildHomeScreen(
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToChat = {
                    navController.navigate("child_chat")
                },
                onNavigateToLocation = {
                    navController.navigate("location")
                },
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("profile") {
            val context = LocalContext.current
            val sessionManagerProfile = remember { SessionManager.getInstance(context) }
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = {
                    navController.navigate("edit_profile")
                },
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("parent_home") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                onNavigateToChild = {
                    navController.navigate("child_management")
                },
                onNavigateToLocation = {
                    navController.navigate("location")
                },
                onNavigateToChat = {
                    val role = sessionManagerProfile.getUser()?.role
                    if (role == UserRole.CHILD) {
                        navController.navigate("child_chat")
                    } else {
                        navController.navigate("chat")
                    }
                }
            )
        }

        composable("edit_profile") {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileUpdated = {
                    navController.popBackStack()
                }
            )
        }

        composable("child_management") {
            ChildManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddChild = {
                    navController.navigate("add_child")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToHome = {
                    navController.navigate("parent_home")
                },
                onNavigateToLocation = {
                    navController.navigate("location")
                },
                onNavigateToChat = {
                    navController.navigate("chat")
                },
                onNavigateToQRCode = { qrCode, childName ->
                    navController.navigate("qr_code/$qrCode/$childName")
                }
            )
        }

        composable("add_child") {
            AddChildScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToQRCode = { qrCode, childName ->
                    navController.navigate("qr_code/$qrCode/$childName") {
                        popUpTo("child_management") { inclusive = false }
                    }
                },
                onNavigateToLinkChild = {
                    navController.navigate("link_child")
                }
            )
        }

        composable("link_child") {
            LinkChildQrScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLinkSuccess = {
                    navController.navigate("child_management") {
                        popUpTo("child_management") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "qr_code/{qrCode}/{childName}",
            arguments = listOf(
                navArgument("qrCode") { type = NavType.StringType },
                navArgument("childName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val qrCode = backStackEntry.arguments?.getString("qrCode") ?: ""
            val childName = backStackEntry.arguments?.getString("childName") ?: ""
            QRCodeScreen(
                qrCodeData = qrCode,
                childName = childName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChildManagement = {
                    navController.navigate("child_management") {
                        popUpTo("child_management") { inclusive = true }
                    }
                }
            )
        }

        composable("location") {
            LocationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("chat") {
            ChatScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenRoom = { room ->
                    val encodedChildName = Uri.encode(room.childName)
                    navController.navigate("chat_room/${room.roomId}?childName=$encodedChildName")
                }
            )
        }

        composable("child_chat") {
            ChildChatScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenRoom = { roomId, childName ->
                    val encodedName = childName?.let { Uri.encode(it) } ?: ""
                    navController.navigate("chat_room/$roomId?childName=$encodedName") {
                        popUpTo("child_chat") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "chat_room/{roomId}?childName={childName}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("childName") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val childName = backStackEntry.arguments?.getString("childName")
            ChatRoomScreen(
                roomId = roomId,
                childNameHint = childName?.takeIf { it.isNotBlank() },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}