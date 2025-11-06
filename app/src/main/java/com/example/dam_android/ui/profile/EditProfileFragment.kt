package com.example.dam_android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    private lateinit var inputFirstName: TextInputEditText
    private lateinit var inputLastName: TextInputEditText
    private lateinit var inputPhoneNumber: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())
        val currentUser = sessionManager.getUser()

        initViews(view)
        populateFields(currentUser)
        setupListeners()
        observeViewModel()

        viewModel.setUser(currentUser)
    }

    private fun initViews(view: View) {
        inputFirstName = view.findViewById(R.id.input_first_name)
        inputLastName = view.findViewById(R.id.input_last_name)
        inputPhoneNumber = view.findViewById(R.id.input_phone_number)
        inputPassword = view.findViewById(R.id.input_password)
        btnSave = view.findViewById(R.id.btn_save_profile)
        btnCancel = view.findViewById(R.id.btn_cancel)
    }

    private fun populateFields(user: com.example.dam_android.data.model.User?) {
        if (user != null) {
            // Extraire le prénom et nom (le name est "firstName lastName")
            val nameParts = user.name.split(" ", limit = 2)
            inputFirstName.setText(nameParts.getOrNull(0) ?: "")
            inputLastName.setText(nameParts.getOrNull(1) ?: user.lastName)
            inputPhoneNumber.setText(user.phoneNumber)
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveProfile()
        }

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        view?.findViewById<View>(R.id.btn_back_edit)?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun saveProfile() {
        val currentUser = sessionManager.getUser() ?: return

        val firstName = inputFirstName.text.toString().trim()
        val lastName = inputLastName.text.toString().trim()
        val phoneNumber = inputPhoneNumber.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        // Validation
        if (firstName.isEmpty()) {
            inputFirstName.error = "Le prénom est requis"
            return
        }

        if (lastName.isEmpty()) {
            inputLastName.error = "Le nom est requis"
            return
        }

        if (phoneNumber.isEmpty()) {
            inputPhoneNumber.error = "Le numéro de téléphone est requis"
            return
        }

        // Mettre à jour le profil
        viewModel.updateProfile(
            userId = currentUser.id,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber,
            password = if (password.isEmpty()) null else password
        )
    }

    private fun observeViewModel() {
        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ProfileViewModel.UpdateResult.Loading -> {
                    btnSave.isEnabled = false
                    btnSave.text = "Enregistrement..."
                }
                is ProfileViewModel.UpdateResult.Success -> {
                    btnSave.isEnabled = true
                    btnSave.text = "Enregistrer"

                    // Mettre à jour la session avec les nouvelles données
                    sessionManager.saveUser(result.user)

                    Toast.makeText(requireContext(), "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show()

                    // Retourner à l'écran de profil
                    findNavController().navigateUp()

                    viewModel.onUpdateResultHandled()
                }
                is ProfileViewModel.UpdateResult.Error -> {
                    btnSave.isEnabled = true
                    btnSave.text = "Enregistrer"
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    viewModel.onUpdateResultHandled()
                }
                null -> {}
            }
        }
    }
}

