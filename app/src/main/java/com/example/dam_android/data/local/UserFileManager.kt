package com.example.dam_android.data.local

import android.content.Context
import com.example.dam_android.data.model.User
import com.example.dam_android.data.model.UserRole
import java.io.File

class UserFileManager(private val context: Context) {

    private val fileName = "users.txt"
    private val file: File
        get() = File(context.filesDir, fileName)

    companion object {
        @Volatile
        private var instance: UserFileManager? = null

        fun getInstance(context: Context): UserFileManager {
            return instance ?: synchronized(this) {
                instance ?: UserFileManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Format du fichier users.txt :
     * email|name|password|role
     * exemple: user@test.com|John Doe|123456|PARENT
     */

    // Sauvegarder un utilisateur
    fun saveUser(user: User): Boolean {
        return try {
            // Vérifier si l'email existe déjà
            if (userExists(user.email)) {
                return false
            }

            // Créer le fichier s'il n'existe pas
            if (!file.exists()) {
                file.createNewFile()
            }

            // Ajouter l'utilisateur
            val line = "${user.email}|${user.name}|${user.password}|${user.role.name}\n"
            file.appendText(line)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Vérifier si un utilisateur existe avec cet email
    fun userExists(email: String): Boolean {
        return try {
            if (!file.exists()) return false

            file.readLines().any { line ->
                val parts = line.split("|")
                parts.isNotEmpty() && parts[0] == email
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Authentifier un utilisateur
    fun authenticateUser(email: String, password: String): User? {
        return try {
            if (!file.exists()) return null

            file.readLines().forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 4) {
                    val userEmail = parts[0]
                    val userName = parts[1]
                    val userPassword = parts[2]
                    val userRole = try {
                        UserRole.valueOf(parts[3])
                    } catch (e: IllegalArgumentException) {
                        UserRole.CHILD
                    }

                    // Vérifier email et password
                    if (userEmail == email && userPassword == password) {
                        return User(
                            id = userEmail.hashCode().toString(),
                            email = userEmail,
                            name = userName,
                            password = userPassword,
                            role = userRole
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Obtenir tous les utilisateurs (pour debug)
    fun getAllUsers(): List<User> {
        return try {
            if (!file.exists()) return emptyList()

            file.readLines().mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 4) {
                    User(
                        id = parts[0].hashCode().toString(),
                        email = parts[0],
                        name = parts[1],
                        password = parts[2],
                        role = try {
                            UserRole.valueOf(parts[3])
                        } catch (e: IllegalArgumentException) {
                            UserRole.CHILD
                        }
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Supprimer tous les utilisateurs (pour reset)
    fun clearAllUsers() {
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Obtenir le chemin du fichier (pour debug)
    fun getFilePath(): String {
        return file.absolutePath
    }
}

