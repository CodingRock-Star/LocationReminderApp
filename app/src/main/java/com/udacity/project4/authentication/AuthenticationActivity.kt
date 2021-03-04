package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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


    private lateinit var binding : ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)
        binding.authButton.setOnClickListener { signInFlow() }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == SIGN_IN_RESULT_CODE) {
        val response = IdpResponse.fromResultIntent(data)
        if (resultCode == Activity.RESULT_OK) {
            Log.i(TAG,
                    "Successfully Signed ${FirebaseAuth.getInstance().currentUser?.displayName}!"
            )
            Toast.makeText(this,"SignIn Success full",Toast.LENGTH_LONG).show()
            val intent = Intent(this, RemindersActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            Toast.makeText(this,"SignIn failed",Toast.LENGTH_LONG).show()
        }
    }
}


    private fun signInFlow() {
        val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build()
        )

        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                        providers
                ).build(), SIGN_IN_RESULT_CODE
        )
    }

    companion object {
        private  val TAG = AuthenticationActivity::class.java.simpleName
        const val SIGN_IN_RESULT_CODE = 1001
    }

}
