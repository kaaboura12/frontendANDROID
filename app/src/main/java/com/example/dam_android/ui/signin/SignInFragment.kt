package com.example.dam_android.ui.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.SessionManager
import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.model.UserRole
import com.example.dam_android.data.repository.AuthRepository
import com.example.dam_android.ui.signin.SignInViewModel
import com.example.dam_android.viewmodel.ViewModelFactory
import com.google.android.material.button.MaterialButton

class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by viewModels {
        val authRepository = AuthRepository()
        ViewModelFactory(authRepository)
    }

    private lateinit var sessionManager: SessionManager
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnSignIn: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        inputEmail = view.findViewById(R.id.input_email)
        inputPassword = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_password)
        btnSignIn = view.findViewById(R.id.btn_sign_in)

        setupListeners(view)
        observeViewModel()
    }

    private fun setupListeners(view: View) {
        // Navigation vers Sign Up
        view.findViewById<TextView>(R.id.link_sign_up)?.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        // Navigation vers Forgot Password
        view.findViewById<TextView>(R.id.forgot_password)?.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_forgotPasswordFragment)
        }

        // Text watchers pour le ViewModel
        inputEmail.addTextChangedListener {
            viewModel.onEmailChanged(it.toString())
        }

        inputPassword.addTextChangedListener {
            viewModel.onPasswordChanged(it.toString())
        }

        // Bouton de connexion
        btnSignIn.setOnClickListener {
            viewModel.signIn()
        }
    }

    private fun observeViewModel() {
        // Observer le résultat de l'authentification
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    btnSignIn.isEnabled = false
                    btnSignIn.text = "Connexion..."
                }
                is AuthResult.Success -> {
                    btnSignIn.isEnabled = true
                    btnSignIn.text = getString(R.string.sign_in)

                    // Sauvegarder la session utilisateur AVEC le token
                    sessionManager.saveUser(result.user, result.token)

                    Toast.makeText(requireContext(), "Bienvenue ${result.user.name}!", Toast.LENGTH_SHORT).show()

                    // Rediriger selon le rôle
                    when (result.user.role) {
                        UserRole.PARENT -> {
                            findNavController().navigate(R.id.action_signInFragment_to_parentHomeFragment)
                        }
                        UserRole.CHILD -> {
                            findNavController().navigate(R.id.action_signInFragment_to_childHomeFragment)
                        }
                    }
                }
                is AuthResult.Error -> {
                    btnSignIn.isEnabled = true
                    btnSignIn.text = getString(R.string.sign_in)
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observer la validité du formulaire
        viewModel.isFormValid.observe(viewLifecycleOwner) { isValid ->
            btnSignIn.isEnabled = isValid
        }
    }
}
