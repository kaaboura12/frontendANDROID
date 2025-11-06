package com.example.dam_android.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return try {
            inflater.inflate(R.layout.fragment_splash, container, false)
        } catch (e: Exception) {
            e.printStackTrace()
            // Créer une vue de secours si le layout échoue
            android.widget.TextView(requireContext()).apply {
                text = "Loading..."
                textSize = 24f
                setPadding(50, 50, 50, 50)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            try {
                delay(2000)
                findNavController().navigate(R.id.action_splashFragment_to_welcomeFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
