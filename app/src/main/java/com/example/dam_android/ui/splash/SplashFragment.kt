package com.example.dam_android.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private lateinit var logoSplash: ImageView
    private lateinit var titleSplash: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logoSplash = view.findViewById(R.id.logo_splash)
        titleSplash = view.findViewById(R.id.title_splash)

        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        lifecycleScope.launch {
            // Step 1: Logo appears in center with fade in and scale (0-800ms)
            delay(300)
            val fadeInAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.logo_fade_in)
            logoSplash.startAnimation(fadeInAnim)
            logoSplash.alpha = 1f

            // Step 2: Logo moves down slightly (800-1400ms)
            delay(1100)
            val moveDownAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.logo_move_down)
            logoSplash.startAnimation(moveDownAnim)

            // Step 3: Logo moves down more (1400-2000ms)
            delay(700)
            val moveDownAnim2 = AnimationUtils.loadAnimation(requireContext(), R.anim.logo_move_down)
            logoSplash.startAnimation(moveDownAnim2)

            // Step 4: Logo moves to right side and text appears (2000-2600ms)
            delay(700)
            val moveToCornerAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.logo_move_to_corner)
            logoSplash.startAnimation(moveToCornerAnim)

            // Show text at the same time
            delay(100)
            val textFadeInAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.text_fade_in)
            titleSplash.startAnimation(textFadeInAnim)
            titleSplash.alpha = 1f

            // Step 5: Wait a bit then navigate (2600-3500ms)
            delay(1500)
            findNavController().navigate(R.id.action_splashFragment_to_welcomeFragment)
        }
    }
}
