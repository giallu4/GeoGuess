package com.application.geoguess

//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task


//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.application.geoguess.ui.theme.GeoGuessTheme



class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var gsc: GoogleSignInClient
    private var currentUserGoogleMail:String? = null
    private var currentUserGoogleName:String? = null
    private var currentUserGoogleSurname:String? = null
    private var currentUserGoogleID:String? = null
    private var currentUserGooglePhoto:Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

        val signInButton:com.google.android.gms.common.SignInButton = findViewById(R.id.google_sign_in)
        signInButton.setOnClickListener(this)


        val singlePlayerButton: androidx.cardview.widget.CardView =
            findViewById(R.id.single_player_button)
        val multiPlayerButton: androidx.cardview.widget.CardView =
            findViewById(R.id.multi_player_button)
        singlePlayerButton.setOnClickListener {
            val intent = Intent(applicationContext, GameActivity::class.java)
            intent.putExtra(StringConstants.TYPE_OF_GAME_KEY, StringConstants.ONE_PLAYER_GAME)
            startActivity(intent)
        }
        multiPlayerButton.setOnClickListener {
            showPlayerNameDialog()
        }

        val gso:GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val viewswitch:ViewSwitcher = findViewById(R.id.viewswitcher)
        viewswitch.reset()
        viewswitch.showNext()

        gsc = GoogleSignIn.getClient(this, gso)


        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null. .
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null){
            viewswitch.reset()
            viewswitch.showNext()
        } else {
            updateLoginUI()
        }
    }


    //{ dialog, whichButton ->
    @SuppressLint("InflateParams")
    private fun showPlayerNameDialog() {
        val dialogView = this.layoutInflater.inflate(R.layout.player_name_dialog, null)
        AlertDialog.Builder(this).apply {
            setView(dialogView)
            val playerOneName = dialogView.findViewById<EditText>(R.id.player_one_editText_name)
            val playerTwoName = dialogView.findViewById<EditText>(R.id.player_two_editText_name)
            setTitle(R.string.dialog_title)
            setPositiveButton(getString(R.string.dialog_positive_button)) { _, _ ->
                Intent(baseContext, GameActivity::class.java).apply {
                    putExtra(StringConstants.TYPE_OF_GAME_KEY, StringConstants.TWO_PLAYER_GAME)
                    putExtra(StringConstants.PLAYER_ONE_NAME_KEY, playerOneName.text.toString())
                    putExtra(StringConstants.PLAYER_TWO_NAME_KEY, playerTwoName.text.toString())
                    startActivity(this)
                }
            }
            create().show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onClick(view: View?) {
        val signinIntent:Intent = gsc.signInIntent
        startActivityForResult(signinIntent, 1000)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)

        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            currentUserGoogleMail = account.email
            currentUserGoogleName = account.givenName
            currentUserGoogleSurname = account.familyName
            currentUserGoogleID = account.id
            currentUserGooglePhoto = account.photoUrl

            updateLoginUI()
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Toast.makeText(applicationContext, "Something went wrong ...", Toast.LENGTH_SHORT).show()

        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateLoginUI() {
        val viewswitch:ViewSwitcher = findViewById(R.id.viewswitcher)
        val userAvatar:ImageView = findViewById(R.id.user_icon)
        val userName:TextView = findViewById(R.id.user_name)

        viewswitch.showNext()
        Glide.with(this)
            .load(currentUserGooglePhoto) // image url
            .placeholder(R.drawable.user_default_icon) // any placeholder to load at start
            .error(R.drawable.ic_two_people_white_24dp)  // any image in case of error
            //.override(200, 200) // resizing
            .centerCrop()
            .into(userAvatar)  // imageview object
        if (currentUserGoogleName != null && currentUserGoogleSurname != null){
            userName.setText("Hi! " +currentUserGoogleName+" "+currentUserGoogleSurname)
        } else if (currentUserGoogleName != null){
            userName.setText("Hi! " +currentUserGoogleName)
        } else {
            userName.setText("Hi! -No Name-")
        }

        userName.invalidate()
        userAvatar.invalidate()
    }

}