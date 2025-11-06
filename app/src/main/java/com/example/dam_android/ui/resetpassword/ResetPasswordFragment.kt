package com.example.dam_android.ui.resetpassword

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
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
import com.example.dam_android.data.repository.AuthRepository
import com.example.dam_android.ui.resetpassword.ResetPasswordViewModel
import com.example.dam_android.viewmodel.ViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ResetPasswordFragment : Fragment() {

    private var email: String = ""

    private val viewModel: ResetPasswordViewModel by viewModels {
        val authRepository = AuthRepository()
        ViewModelFactory(authRepository)
    }

    private lateinit var codeDigits: List<EditText>
    private lateinit var inputNewPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var textSubtitle: TextView
    private lateinit var linkResendCode: TextView

    private var countDownTimer: CountDownTimer? = null
    private var canResend = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reset_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Récupérer l'email depuis les arguments
        email = arguments?.getString("email") ?: ""

        initViews(view)
        setupCodeInputs()
        setupListeners()
        observeViewModel()

        // Afficher l'email dans le subtitle
        textSubtitle.text = "Enter the code sent to\n$email"

        // Démarrer le compte à rebours
        startResendTimer()
    }

    private fun initViews(view: View) {
        codeDigits = listOf(
            view.findViewById(R.id.code_digit_1),
            view.findViewById(R.id.code_digit_2),
            view.findViewById(R.id.code_digit_3),
            view.findViewById(R.id.code_digit_4),
            view.findViewById(R.id.code_digit_5),
            view.findViewById(R.id.code_digit_6)
        )

        inputNewPassword = view.findViewById(R.id.input_new_password)
        inputConfirmPassword = view.findViewById(R.id.input_confirm_password)
        btnResetPassword = view.findViewById(R.id.btn_reset_password)
        textSubtitle = view.findViewById(R.id.text_subtitle)
        linkResendCode = view.findViewById(R.id.link_resend_code)
    }

    private fun setupCodeInputs() {
        codeDigits.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < codeDigits.size - 1) {
                        // Auto-focus sur le champ suivant
                        codeDigits[index + 1].requestFocus()
                    }

                    // Mettre à jour le ViewModel avec le code complet
                    val code = codeDigits.joinToString("") { it.text.toString() }
                    viewModel.onCodeChanged(code)
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            // Gérer le retour arrière
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                    editText.text.isEmpty() &&
                    index > 0) {
                    codeDigits[index - 1].requestFocus()
                    codeDigits[index - 1].text.clear()
                }
                false
            }
        }
    }

    private fun setupListeners() {
        val btnBack = view?.findViewById<ImageView>(R.id.btn_back)

        btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Text watchers pour les mots de passe
        inputNewPassword.addTextChangedListener {
            viewModel.onNewPasswordChanged(it.toString())
        }

        inputConfirmPassword.addTextChangedListener {
            viewModel.onConfirmPasswordChanged(it.toString())
        }

        // Reset password button
        btnResetPassword.setOnClickListener {
            viewModel.resetPassword(email)
        }

        // Resend code
        linkResendCode.setOnClickListener {
            if (canResend) {
                viewModel.resendCode(email)
                startResendTimer()
            }
        }
    }

    private fun observeViewModel() {
        // Observer la validité du formulaire
        viewModel.isFormValid.observe(viewLifecycleOwner) { isValid ->
            btnResetPassword.isEnabled = isValid
        }

        // Observer le résultat de la réinitialisation
        viewModel.resetResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResetPasswordViewModel.ResetPasswordResult.Loading -> {
                    btnResetPassword.isEnabled = false
                    btnResetPassword.text = "Resetting..."
                }
                is ResetPasswordViewModel.ResetPasswordResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    // Rediriger vers la page de connexion
                    findNavController().navigate(R.id.action_resetPasswordFragment_to_signInFragment)
                }
                is ResetPasswordViewModel.ResetPasswordResult.Error -> {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Reset Password"
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observer le résultat du renvoi du code
        viewModel.resendResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResetPasswordViewModel.ResendResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is ResetPasswordViewModel.ResendResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                null -> {} // Ignore
            }
        }
    }

    private fun startResendTimer() {
        canResend = false
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                linkResendCode.text = "Didn't receive the code? (${seconds}s)"
                linkResendCode.isEnabled = false
                linkResendCode.alpha = 0.5f
            }

            override fun onFinish() {
                linkResendCode.text = "Didn't receive the code? Resend"
                linkResendCode.isEnabled = true
                linkResendCode.alpha = 1.0f
                canResend = true
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}
