package com.example.dam_android.ui.forgotpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.UserFileManager
import com.example.dam_android.data.repository.AuthRepository
import com.example.dam_android.viewmodel.ForgotPasswordViewModel
import com.example.dam_android.viewmodel.ViewModelFactory
import com.google.android.material.button.MaterialButton

class ForgotPasswordFragment : Fragment() {

    private val viewModel: ForgotPasswordViewModel by viewModels {
        val userFileManager = UserFileManager.getInstance(requireContext())
        val authRepository = AuthRepository.getInstance(userFileManager)
        ViewModelFactory(authRepository)
    }

    private lateinit var inputEmail: EditText
    private lateinit var btnResetPassword: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputEmail = view.findViewById(R.id.input_email_forgot)
        btnResetPassword = view.findViewById(R.id.btn_reset_password)

        setupListeners(view)
        observeViewModel()
    }

    private fun setupListeners(view: View) {
        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val linkSignIn = view.findViewById<TextView>(R.id.link_sign_in)

        // Back button
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Sign In link
        linkSignIn.setOnClickListener {
            findNavController().navigateUp()
        }

        // Text watcher pour le ViewModel
        inputEmail.addTextChangedListener {
            viewModel.onEmailChanged(it.toString())
        }

        // Reset password button
        btnResetPassword.setOnClickListener {
            viewModel.resetPassword()
        }
    }

    private fun observeViewModel() {
        // Observer la validité de l'email
        viewModel.isEmailValid.observe(viewLifecycleOwner) { isValid ->
            btnResetPassword.isEnabled = isValid
        }

        // Observer le résultat de la réinitialisation
        viewModel.resetResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ForgotPasswordViewModel.ResetPasswordResult.Loading -> {
                    btnResetPassword.isEnabled = false
                    btnResetPassword.text = "Envoi en cours..."
                }
                is ForgotPasswordViewModel.ResetPasswordResult.Success -> {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Reset password"
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is ForgotPasswordViewModel.ResetPasswordResult.Error -> {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Reset password"
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
