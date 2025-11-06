package com.example.dam_android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        // Get current user
        val currentUser = sessionManager.getUser()
        viewModel.setUser(currentUser)

        // Update profile information
        view.findViewById<TextView>(R.id.tv_profile_name)?.text = currentUser?.name ?: "Utilisateur"
        view.findViewById<TextView>(R.id.tv_profile_email)?.text = currentUser?.email ?: ""
        view.findViewById<TextView>(R.id.tv_profile_phone)?.text = currentUser?.phoneNumber ?: "+216 00000000"

        // Setup edit profile button
        view.findViewById<MaterialButton>(R.id.btn_edit_profile)?.setOnClickListener {
            viewModel.onEditProfileClicked()
        }

        // Setup logout button
        view.findViewById<MaterialButton>(R.id.btn_logout)?.setOnClickListener {
            viewModel.onLogoutClicked()
        }

        // Setup back button
        view.findViewById<View>(R.id.btn_back_profile)?.setOnClickListener {
            viewModel.onBackPressed()
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning from edit screen
        val currentUser = sessionManager.getUser()
        view?.findViewById<TextView>(R.id.tv_profile_name)?.text = currentUser?.name ?: "Utilisateur"
        view?.findViewById<TextView>(R.id.tv_profile_email)?.text = currentUser?.email ?: ""
        view?.findViewById<TextView>(R.id.tv_profile_phone)?.text = currentUser?.phoneNumber ?: "+216 00000000"
    }

    private fun observeViewModel() {
        viewModel.showLogoutDialog.observe(viewLifecycleOwner) { showDialog ->
            if (showDialog) {
                showLogoutConfirmation()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ProfileViewModel.NavigationEvent.NavigateToSignIn -> {
                    logout()
                    viewModel.onNavigationComplete()
                }
                is ProfileViewModel.NavigationEvent.NavigateBack -> {
                    findNavController().navigateUp()
                    viewModel.onNavigationComplete()
                }
                is ProfileViewModel.NavigationEvent.NavigateToEditProfile -> {
                    findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
                    viewModel.onNavigationComplete()
                }
                null -> {}
            }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ ->
                viewModel.onLogoutConfirmed()
            }
            .setNegativeButton("Annuler") { _, _ ->
                viewModel.onLogoutCancelled()
            }
            .show()
    }

    private fun logout() {
        // Clear session
        sessionManager.logout()

        // Navigate to sign in screen
        findNavController().navigate(R.id.action_profileFragment_to_signInFragment)
    }
}
