package com.example.dam_android.ui.parent.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.dam_android.R
import com.example.dam_android.data.local.SessionManager

class ParentHomeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    private lateinit var navBtnHome: LinearLayout
    private lateinit var navBtnChild: LinearLayout
    private lateinit var navBtnLocation: LinearLayout
    private lateinit var navBtnActivity: LinearLayout
    private lateinit var navBtnProfile: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_parent_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        // Get current user
        val currentUser = sessionManager.getUser()

        // Update greeting
        view.findViewById<TextView>(R.id.tv_greeting)?.text =
            "bonjour ${currentUser?.name ?: "Parent"}"

        // Initialize navigation buttons
        navBtnHome = view.findViewById(R.id.nav_btn_home)
        navBtnChild = view.findViewById(R.id.nav_btn_child)
        navBtnLocation = view.findViewById(R.id.nav_btn_location)
        navBtnActivity = view.findViewById(R.id.nav_btn_activity)
        navBtnProfile = view.findViewById(R.id.nav_btn_profile)

        // Setup navigation button listeners
        setupNavigationListeners()

        // Set Home as selected by default
        setSelectedNavButton(navBtnHome)

        // Setup click listeners for child cards
        setupChildCardListeners(view)
    }

    private fun setupNavigationListeners() {
        navBtnHome.setOnClickListener {
            setSelectedNavButton(navBtnHome)
            // Already on home
        }

        navBtnChild.setOnClickListener {
            setSelectedNavButton(navBtnChild)
            Toast.makeText(requireContext(), "Child section", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to Child list
        }

        navBtnLocation.setOnClickListener {
            setSelectedNavButton(navBtnLocation)
            Toast.makeText(requireContext(), "Location section", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to Location
        }

        navBtnActivity.setOnClickListener {
            setSelectedNavButton(navBtnActivity)
            Toast.makeText(requireContext(), "Activity section", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to Activity
        }

        navBtnProfile.setOnClickListener {
            setSelectedNavButton(navBtnProfile)
            // Navigate to Profile
            findNavController().navigate(R.id.action_parentHomeFragment_to_profileFragment)
        }
    }

    private fun setSelectedNavButton(selectedButton: LinearLayout) {
        // Reset all buttons to default (black)
        resetNavButton(navBtnHome, R.id.nav_icon_home, R.id.nav_text_home)
        resetNavButton(navBtnChild, R.id.nav_icon_child, R.id.nav_text_child)
        resetNavButton(navBtnLocation, R.id.nav_icon_location, R.id.nav_text_location)
        resetNavButton(navBtnActivity, R.id.nav_icon_activity, R.id.nav_text_activity)
        resetNavButton(navBtnProfile, R.id.nav_icon_profile, R.id.nav_text_profile)

        // Set selected button to orange
        val icon = selectedButton.findViewById<ImageView>(
            when (selectedButton.id) {
                R.id.nav_btn_home -> R.id.nav_icon_home
                R.id.nav_btn_child -> R.id.nav_icon_child
                R.id.nav_btn_location -> R.id.nav_icon_location
                R.id.nav_btn_activity -> R.id.nav_icon_activity
                R.id.nav_btn_profile -> R.id.nav_icon_profile
                else -> R.id.nav_icon_home
            }
        )

        val text = selectedButton.findViewById<TextView>(
            when (selectedButton.id) {
                R.id.nav_btn_home -> R.id.nav_text_home
                R.id.nav_btn_child -> R.id.nav_text_child
                R.id.nav_btn_location -> R.id.nav_text_location
                R.id.nav_btn_activity -> R.id.nav_text_activity
                R.id.nav_btn_profile -> R.id.nav_text_profile
                else -> R.id.nav_text_home
            }
        )

        icon.setColorFilter(resources.getColor(R.color.orange_700, null))
        text.setTextColor(resources.getColor(R.color.orange_700, null))
    }

    private fun resetNavButton(button: LinearLayout, iconId: Int, textId: Int) {
        button.findViewById<ImageView>(iconId).setColorFilter(resources.getColor(android.R.color.black, null))
        button.findViewById<TextView>(textId).setTextColor(resources.getColor(android.R.color.black, null))
    }

    private fun setupChildCardListeners(view: View) {
        // Child 1 buttons
        view.findViewById<View>(R.id.btn_check_profile_1)?.setOnClickListener {
            Toast.makeText(requireContext(), "Profile enfant 1", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to child profile
        }
        view.findViewById<View>(R.id.btn_check_map_1)?.setOnClickListener {
            Toast.makeText(requireContext(), "Carte enfant 1", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to map with child location
        }

        // Child 2 buttons
        view.findViewById<View>(R.id.btn_check_profile_2)?.setOnClickListener {
            Toast.makeText(requireContext(), "Profile enfant 2", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to child profile
        }
        view.findViewById<View>(R.id.btn_check_map_2)?.setOnClickListener {
            Toast.makeText(requireContext(), "Carte enfant 2", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to map with child location
        }

        // Child 3 buttons
        view.findViewById<View>(R.id.btn_check_profile_3)?.setOnClickListener {
            Toast.makeText(requireContext(), "Profile enfant 3", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to child profile
        }
        view.findViewById<View>(R.id.btn_check_map_3)?.setOnClickListener {
            Toast.makeText(requireContext(), "Carte enfant 3", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to map with child location
        }
    }
}
