package com.example.windrose.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.windrose.R
import com.example.windrose.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        if(auth.currentUser != null){
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.windRoseImageView.setOnClickListener{
            val intent = Intent(requireContext(), DeviceFinderActivity::class.java)
            startActivity(intent)
        }

        binding.createAccountTextView.setOnClickListener{
            findNavController().navigate(R.id.registerFragment)
        }


        binding.loginButton.setOnClickListener{
            signInUser()
        }
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onResume() {
        super.onResume()
        binding.loginEmailEditText.text.clear()
        binding.loginPasswordEditText.text.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun signInUser() {
        lifecycleScope.launch {
            val email = binding.loginEmailEditText.text.toString()
            val password = binding.loginPasswordEditText.text.toString()

            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val currentUser = result.user

                if (currentUser != null){
                    Toast.makeText(requireContext(), "Olá, ${currentUser.displayName}!", Toast.LENGTH_LONG).show()
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Não foi possível fazer o login",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (ex: Exception){
                Toast.makeText(requireContext(), ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }

}