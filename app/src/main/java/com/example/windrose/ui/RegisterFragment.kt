package com.example.windrose.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.windrose.R
import com.example.windrose.databinding.FragmentRegisterBinding
import com.example.windrose.network.API
import com.example.windrose.network.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class RegisterFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.accountLoginTextView.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.registerButton.setOnClickListener {
            createAccount()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createAccount() {
        val name = binding.registerNameEditText.text.toString()
        val email = binding.registerEmailEditText.text.toString()
        val password = binding.registerPasswordEditText.text.toString()

        lifecycleScope.launch {

            try {

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val currentUser = result.user


                if (currentUser != null) {
                    val profileRequest = userProfileChangeRequest {
                        displayName = name
                    }
                    Log.i("TOPA", "So pra ver")
                    currentUser.updateProfile(profileRequest).await()
                    registerUser(name, email, password, currentUser)

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Não foi possivel criar sua conta",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun registerUser(
        name: String,
        email: String,
        password: String,
        currentUser: FirebaseUser?
    ) {

        lifecycleScope.launch {

            try {
                val firebaseUuid = auth.currentUser!!.uid
                val userDto = UserDto(name, email, password, firebaseUuid)
                val authService = API.buildAuthService()
                val response = authService.registerUser(userDto)


                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Olá, ${currentUser!!.displayName}!",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao criar conta: ${response.errorBody()?.string()}",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (ex: Exception) {
                Toast.makeText(requireContext(), ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }


}