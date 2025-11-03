package com.example.dam_android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        // Get current user
        val currentUser = sessionManager.getUser()

        // Update profile information
        view.findViewById<TextView>(R.id.tv_profile_name)?.text = currentUser?.name ?: "Utilisateur"
        view.findViewById<TextView>(R.id.tv_profile_email)?.text = currentUser?.email ?: ""
        view.findViewById<TextView>(R.id.tv_profile_role)?.text = when (currentUser?.role?.name) {
            "PARENT" -> "Parent"
            "CHILD" -> "Enfant"
            else -> "Utilisateur"
        }

        // Setup logout button
        view.findViewById<MaterialButton>(R.id.btn_logout)?.setOnClickListener {
            showLogoutConfirmation()
        }

        // Setup back button
        view.findViewById<View>(R.id.btn_back_profile)?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ ->
                logout()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun logout() {
        // Clear session
        sessionManager.logout()

        // Navigate to sign in screen
        findNavController().navigate(R.id.action_profileFragment_to_signInFragment)
    }
}

