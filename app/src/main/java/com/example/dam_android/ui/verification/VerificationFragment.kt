package com.example.dam_android.ui.verification

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.SessionManager
import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.model.UserRole
import com.google.android.material.button.MaterialButton

class VerificationFragment : Fragment() {

    private val TAG = "VerificationFragment"
    private val viewModel: VerificationViewModel by viewModels()

    private lateinit var sessionManager: SessionManager
    private lateinit var tvEmail: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvResend: TextView
    private lateinit var btnVerify: MaterialButton
    private lateinit var codeInputs: List<EditText>
    private var userEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        // Récupérer l'email depuis les arguments
        userEmail = arguments?.getString("email") ?: ""

        // Initialiser les vues
        tvEmail = view.findViewById(R.id.tv_email)
        tvTimer = view.findViewById(R.id.tv_timer)
        tvResend = view.findViewById(R.id.tv_resend_code)
        btnVerify = view.findViewById(R.id.btn_verify)

        // Initialiser les 6 champs de saisie du code
        codeInputs = listOf(
            view.findViewById(R.id.code_input_1),
            view.findViewById(R.id.code_input_2),
            view.findViewById(R.id.code_input_3),
            view.findViewById(R.id.code_input_4),
            view.findViewById(R.id.code_input_5),
            view.findViewById(R.id.code_input_6)
        )

        // Afficher l'email
        tvEmail.text = userEmail

        setupCodeInputs()
        setupListeners()
        observeViewModel()

        // Démarrer le timer
        viewModel.startTimer()
    }

    private fun setupCodeInputs() {
        codeInputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        // Passer au champ suivant
                        if (index < codeInputs.size - 1) {
                            codeInputs[index + 1].requestFocus()
                        }
                        // Notifier le ViewModel du code complet
                        updateCode()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            // Gérer la suppression (backspace)
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        codeInputs[index - 1].requestFocus()
                        codeInputs[index - 1].text.clear()
                    }
                }
                false
            }
        }
    }

    private fun updateCode() {
        val code = codeInputs.joinToString("") { it.text.toString() }
        viewModel.onCodeChanged(code)
    }

    private fun setupListeners() {
        // Bouton retour
        view?.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Bouton de vérification
        btnVerify.setOnClickListener {
            val code = codeInputs.joinToString("") { it.text.toString() }
            viewModel.verifyCode(userEmail, code)
        }

        // Lien pour renvoyer le code
        tvResend.setOnClickListener {
            // TODO: Implémenter l'API pour renvoyer le code
            Toast.makeText(requireContext(), "Code renvoyé!", Toast.LENGTH_SHORT).show()
            viewModel.resetTimer()
        }
    }

    private fun observeViewModel() {
        // Observer le résultat de la vérification
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    btnVerify.isEnabled = false
                    btnVerify.text = "Vérification..."
                }
                is AuthResult.Success -> {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify"

                    // Sauvegarder la session
                    sessionManager.saveUser(result.user)

                    Toast.makeText(
                        requireContext(),
                        "Compte vérifié avec succès!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Rediriger vers la page d'accueil selon le rôle
                    when (result.user.role) {
                        UserRole.PARENT -> {
                            findNavController().navigate(R.id.action_verificationFragment_to_parentHomeFragment)
                        }
                        UserRole.CHILD -> {
                            findNavController().navigate(R.id.action_verificationFragment_to_childHomeFragment)
                        }
                    }
                }
                is AuthResult.Error -> {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify"
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()

                    // Effacer les champs
                    codeInputs.forEach { it.text.clear() }
                    codeInputs[0].requestFocus()
                }
            }
        }

        // Observer la validité du code
        viewModel.isCodeValid.observe(viewLifecycleOwner) { isValid ->
            btnVerify.isEnabled = isValid
        }

        // Observer le timer
        viewModel.remainingTime.observe(viewLifecycleOwner) { time ->
            tvTimer.text = "(${time}s)"
            tvResend.isEnabled = time == 0

            if (time == 0) {
                tvTimer.text = ""
            }
        }
    }
}
