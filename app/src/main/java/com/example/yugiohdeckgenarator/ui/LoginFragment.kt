package com.example.yugiohdeckgenarator.ui

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.yugiohdeckgenarator.R
import com.example.yugiohdeckgenarator.base.BaseFragment
import com.example.yugiohdeckgenarator.data.entity.UserData
import com.example.yugiohdeckgenarator.databinding.FragmentLoginBinding
import com.facebook.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(FragmentLoginBinding::inflate) {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth by lazy { Firebase.auth }
    var callbackManager: CallbackManager? = null
    private val db by lazy { Firebase.firestore }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser

        if (user != null) {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToDeckFragment())
        }
        callbackManager = CallbackManager.Factory.create()

        binding.registerbutton.setOnClickListener {
            registerButton(binding.etMail.text.toString(), binding.etPassword.text.toString())


        }

        binding.loginbutton.setOnClickListener {
            loginButton(binding.etMail.text.toString(), binding.etPassword.text.toString())


        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        binding.googlebutton.setOnClickListener {
            googleSignIn()
        }

        binding.facebookbutton.setReadPermissions("email", "public_profile")
        binding.facebookbutton.setFragment(this)
        FacebookSdk.sdkInitialize(requireContext())

        binding.facebookbutton.registerCallback(callbackManager!!, object :
            FacebookCallback<com.facebook.login.LoginResult> {
            override fun onCancel() {
            }

            override fun onError(error: FacebookException) {
            }

            override fun onSuccess(result: com.facebook.login.LoginResult) {
                handleFacebookAccessToken(result.accessToken)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        hideBottomBar()
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e(ContentValues.TAG, "Google sing in failed", e)
            }
        }
        callbackManager?.onActivityResult(requestCode, resultCode, data)

    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (task.result.additionalUserInfo!!.isNewUser){
                        db.collection("user").document(user!!.uid).set(UserData())
                    }
                    updateUI(user)

                } else {
                    updateUI(null)
                }
            }
    }

    private fun registerEmailAndPassword(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {

            if (it.isSuccessful) {
                Toast.makeText(requireContext(), "Register Successful", Toast.LENGTH_LONG).show()
                db.collection("user").document(auth.currentUser!!.uid).set(UserData()).addOnCompleteListener {task ->

                }.addOnFailureListener {excepiton ->
                    Toast.makeText(context,excepiton.localizedMessage,Toast.LENGTH_LONG).show()
                }

            }

        }.addOnFailureListener {
            Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun loginEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToDeckFragment())
            Toast.makeText(
                requireContext(),
                "Welcome ${auth.currentUser?.email.toString()}",
                Toast.LENGTH_LONG
            ).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun googleSignIn() {
        val singInIntent = googleSignInClient.signInIntent
        startActivityForResult(singInIntent, RC_SIGN_IN)
    }

    companion object {
        const val RC_SIGN_IN = 1001
        const val EXTRA_NAME = "EXTRA NAME"
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToDeckFragment())
        }
    }


    private fun handleFacebookAccessToken(accessToken: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(accessToken.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                val user = auth.currentUser
                updateUI(user)
                db.collection("user").document(user!!.uid).set(UserData())
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun registerButton(email: String, password: String) {

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Email and password cannot be empty",
                Toast.LENGTH_LONG
            ).show()
        } else {
            registerEmailAndPassword(email, password)
        }
    }


    private fun loginButton(email: String, password: String) {

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Email and password cannot be empty",
                Toast.LENGTH_LONG
            ).show()
        } else {
            loginEmailAndPassword(email, password)
        }

    }
}