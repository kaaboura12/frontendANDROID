package com.example.dam_android.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.SessionManager
import com.example.dam_android.data.local.UserFileManager
import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.model.UserRole
import com.example.dam_android.data.repository.AuthRepository
import com.example.dam_android.viewmodel.SignUpViewModel
import com.example.dam_android.viewmodel.ViewModelFactory
import com.google.android.material.button.MaterialButton

class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by viewModels {
        val userFileManager = UserFileManager.getInstance(requireContext())
        val authRepository = AuthRepository.getInstance(userFileManager)
        ViewModelFactory(authRepository)
    }

    private lateinit var sessionManager: SessionManager
    private lateinit var inputName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirmPassword: EditText
    private lateinit var radioGroupRole: RadioGroup
    private lateinit var btnSignUp: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        inputName = view.findViewById(R.id.input_name)
        inputEmail = view.findViewById(R.id.input_email)
        inputPassword = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_password)
        inputConfirmPassword = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_confirm_password)
        radioGroupRole = view.findViewById(R.id.radio_group_role)
        btnSignUp = view.findViewById(R.id.btn_sign_up)

        setupListeners(view)
        observeViewModel()
    }

    private fun setupListeners(view: View) {
        // Navigation vers Sign In
        view.findViewById<TextView>(R.id.link_sign_in)?.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }

        // Text watchers pour le ViewModel
        inputName.addTextChangedListener {
            viewModel.onNameChanged(it.toString())
        }

        inputEmail.addTextChangedListener {
            viewModel.onEmailChanged(it.toString())
        }

        inputPassword.addTextChangedListener {
            viewModel.onPasswordChanged(it.toString())
        }

        inputConfirmPassword.addTextChangedListener {
            viewModel.onConfirmPasswordChanged(it.toString())
        }

        // RadioGroup pour sélectionner le rôle
        radioGroupRole.setOnCheckedChangeListener { _, checkedId ->
            val role = when (checkedId) {
                R.id.radio_parent -> UserRole.PARENT
                R.id.radio_child -> UserRole.CHILD
                else -> UserRole.CHILD
            }
            viewModel.onRoleChanged(role)
        }

        // Bouton d'inscription
        btnSignUp.setOnClickListener {
            viewModel.signUp()
        }
    }

    private fun observeViewModel() {
        // Observer le résultat de l'inscription
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    btnSignUp.isEnabled = false
                    btnSignUp.text = "Inscription..."
                }
                is AuthResult.Success -> {
                    btnSignUp.isEnabled = true
                    btnSignUp.text = getString(R.string.sign_up)

                    // Sauvegarder la session
                    sessionManager.saveUser(result.user)

                    Toast.makeText(requireContext(), "Inscription réussie! Bienvenue ${result.user.name}!", Toast.LENGTH_SHORT).show()

                    // Rediriger directement vers HomePage selon le rôle
                    when (result.user.role) {
                        UserRole.PARENT -> {
                            findNavController().navigate(R.id.action_signUpFragment_to_parentHomeFragment)
                        }
                        UserRole.CHILD -> {
                            findNavController().navigate(R.id.action_signUpFragment_to_childHomeFragment)
                        }
                    }
                }
                is AuthResult.Error -> {
                    btnSignUp.isEnabled = true
                    btnSignUp.text = getString(R.string.sign_up)
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observer la validité du formulaire
        viewModel.isFormValid.observe(viewLifecycleOwner) { isValid ->
            btnSignUp.isEnabled = isValid
        }
    }
}
