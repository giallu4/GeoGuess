package com.application.geoguess

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Properties


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var gsc: GoogleSignInClient
    private var currentUserGoogleMail:String? = null
    private var currentUserGoogleName:String? = null
    private var currentUserGoogleSurname:String? = null
    private var currentUserGoogleID:String? = null
    private var currentUserGooglePhoto:Uri? = null
    private var currentUserGooglePhotoBitmap:Bitmap? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

        val signInButton:com.google.android.gms.common.SignInButton = findViewById(R.id.google_sign_in)
        signInButton.setOnClickListener(this)

        val toProfileButton:TextView = findViewById(R.id.user_name)
        toProfileButton.setOnClickListener {
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            startActivityForResult(intent, 9999)
        }

        val toProfileButtonTwo:ImageView = findViewById((R.id.user_icon))
        toProfileButtonTwo.setOnClickListener {
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            startActivityForResult(intent, 9999)
        }

        // animate the logo/subtitles/buttons
        val appLogo:ImageView = findViewById(R.id.main_logo)
        val slideDownAnim: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slidedown)
        slideDownAnim.startOffset = 1000
        appLogo.startAnimation(slideDownAnim)
        appLogo.visibility = View.VISIBLE

        val singlePlayerButton: androidx.cardview.widget.CardView =
            findViewById(R.id.single_player_button)
        singlePlayerButton.alpha = 0f
        singlePlayerButton.setTranslationY(50f)
        singlePlayerButton.animate().alpha(1f).translationYBy(-50f).setDuration(2000).setStartDelay(1000)


        val multiPlayerButton: androidx.cardview.widget.CardView =
            findViewById(R.id.multi_player_button)
        multiPlayerButton.alpha = 0f
        multiPlayerButton.setTranslationY(50f)
        multiPlayerButton.animate().alpha(1f).translationYBy(-50f).setDuration(2000).setStartDelay(1000)

        val subtitle:TextView = findViewById(R.id.subtitle)
        subtitle.alpha = 0f
        subtitle.animate().alpha(1f).setDuration(2500).setStartDelay(2000)



        singlePlayerButton.setOnClickListener {
            singlePlayerListener()
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

        gsc = GoogleSignIn.getClient(this, gso) // base-context vs
        //gsc = GoogleSignIn.getClient(applicationContext, gso) // application-context


        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null ..
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null){
            //show button instead
            viewswitch.reset()
            viewswitch.showNext()
        } else {
            currentUserGoogleMail = account.email
            currentUserGoogleName = account.givenName
            currentUserGoogleSurname = account.familyName
            currentUserGoogleID = account.id
            currentUserGooglePhoto = account.photoUrl
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

    private fun singlePlayerListener() {
        val intent = Intent(applicationContext, GameActivity::class.java)
        intent.putExtra(StringConstants.TYPE_OF_GAME_KEY, StringConstants.ONE_PLAYER_GAME)
        if (currentUserGoogleID != null) {
            intent.putExtra("ID_OF_LOGGED", currentUserGoogleID)
        } else {
            intent.putExtra("ID_OF_LOGGED", "null")
        }
        startActivity(intent)
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
            lifecycleScope.launch(Dispatchers.Main) {
                handleSignInResult(task)
            }

        } else if (requestCode == 9999) {
            if (data?.getStringExtra("Logout") == "True") {

                val viewswitch:ViewSwitcher = findViewById(R.id.viewswitcher)

                gsc.signOut()
                gsc.revokeAccess()

                // reset everything

                currentUserGoogleMail = null
                currentUserGoogleName = null
                currentUserGoogleSurname = null
                currentUserGoogleID = null
                currentUserGooglePhoto = null

                // reset switcher

                viewswitch.reset()
                viewswitch.showNext()


                // Confirmation Toast
                val text = "User successfully logged out"
                val centeredText: Spannable = SpannableString(text)
                centeredText.setSpan(
                    AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                    0, text.length - 1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                Toast.makeText(applicationContext, centeredText, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val hashMap = hashMapOf<String, String?>()
            val prop = Properties()

            // Signed in successfully, show authenticated UI.
            currentUserGoogleMail = account.email
            currentUserGoogleName = account.givenName
            currentUserGoogleSurname = account.familyName
            currentUserGoogleID = account.id
            currentUserGooglePhoto = account.photoUrl

            hashMap["Mail"] = currentUserGoogleMail
            hashMap["Name"] = currentUserGoogleName
            hashMap["Surname"] = currentUserGoogleSurname
            hashMap["ID"] = currentUserGoogleID
            hashMap["Url"] = currentUserGooglePhoto.toString()
            prop.putAll(hashMap)


            lifecycleScope.launch(Dispatchers.IO) {
                val userFile = File(applicationContext.filesDir, "logged_user_info")
                prop.store(userFile.bufferedWriter(), null)
                }

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
            .asBitmap()
            .load(currentUserGooglePhoto)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    currentUserGooglePhotoBitmap = resource
                    userAvatar.setImageBitmap(currentUserGooglePhotoBitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        if (currentUserGoogleName != null && currentUserGoogleSurname != null){
            userName.setText("Hi! " +currentUserGoogleName+" "+currentUserGoogleSurname)
            userName.paintFlags = userName.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else if (currentUserGoogleName != null){
            userName.setText("Hi! " +currentUserGoogleName)
            userName.paintFlags = userName.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else {
            userName.setText("Hi! -No Name-")
            userName.paintFlags = userName.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }

        // Confirmation Toast
        val text = "User successfully logged in"
        val centeredText: Spannable = SpannableString(text)
        centeredText.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0, text.length - 1,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        Toast.makeText(applicationContext, centeredText, Toast.LENGTH_LONG).show()

        userName.invalidate()
        userAvatar.invalidate()
    }
}

