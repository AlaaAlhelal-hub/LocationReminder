package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    companion object {
        const val TAG = "AuthenticationActivity"
        const val SIGN_IN_RESULT_CODE = 1001
    }

    private val viewModel: AuthenticationViewModel by lazy {
        ViewModelProvider(this)[AuthenticationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityAuthenticationBinding.inflate(layoutInflater)

        binding.loginBtn.setOnClickListener { launchSignInFlow(R.layout.login_layout) }

        viewModel.authenticationState.observe(this, Observer {
            state ->
            when (state) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> startActivity(Intent(applicationContext, RemindersActivity::class.java))
                AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED -> launchSignInFlow(R.layout.register_layout)
                else -> binding.loginBtn.setOnClickListener {
                    launchSignInFlow(R.layout.login_layout)
                }

            }
        })

        setContentView(binding.root)
    }


    private fun launchSignInFlow(layoutId: Int){
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build())

        val customLayout = AuthMethodPickerLayout.Builder(layoutId)
            .setGoogleButtonId(R.id.google_btn)
            .setEmailButtonId(R.id.email_button)
            .build()

        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setAuthMethodPickerLayout(customLayout)
            .build(), SIGN_IN_RESULT_CODE)
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode.equals(SIGN_IN_RESULT_CODE)) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK){
                Log.i(TAG, "Successfully sign in user ${FirebaseAuth.getInstance().currentUser?.displayName}")
            }
            else {
                Log.i(TAG, "Sign in unsuccessfully ${response?.error?.errorCode}")
            }
        }
    }


}
