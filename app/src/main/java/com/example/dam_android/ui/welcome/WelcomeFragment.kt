package com.example.dam_android.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.google.android.material.button.MaterialButton

class WelcomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            inflater.inflate(R.layout.fragment_welcome, container, false)
        } catch (e: Exception) {
            e.printStackTrace()
            // Vue de secours
            android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(50, 50, 50, 50)
                addView(android.widget.TextView(requireContext()).apply {
                    text = "Welcome"
                    textSize = 24f
                })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            view.findViewById<MaterialButton>(R.id.btn_get_started)?.setOnClickListener {
                findNavController().navigate(R.id.action_welcomeFragment_to_signInFragment)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
