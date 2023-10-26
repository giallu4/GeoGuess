package com.application.geoguess


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapThumbnailImageViewTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Properties


class ProfileActivity : AppCompatActivity(), View.OnClickListener {
    private var imageBitmap: Bitmap? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity_layout)
        val playerName: TextView = findViewById(R.id.profile_name_id)
        val playerIcon: ImageView = findViewById(R.id.profile_icon_id)
        val gamesPlayed: TextView = findViewById(R.id.games_played_id)
        val totalKilometres: TextView = findViewById(R.id.total_kilometres_id)
        val wins: TextView = findViewById(R.id.single_wins)
        val toolbar: ActionBar? = supportActionBar
        val id: TextView = findViewById(R.id.id_id)
        val email: TextView = findViewById(R.id.email_id)
        val hashMap = hashMapOf<String, String?>()
        val prop = Properties()
        val logOut:Button = findViewById(R.id.log_out)

        toolbar?.setDisplayHomeAsUpEnabled(true)

        logOut.setOnClickListener(this)

        playerIcon.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1337)

            } else if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                // create the image file

                try {
                    val ifFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "logged_user_new_avatar.jpg")
                    dispatchTakePictureIntent(ifFile)
                } catch (ex: IOException) {
                    Log.d("FilerError", "Error on creating the file ...")
                }
            }
        }

        //if (playerName.text == "nome cognome") {

            lifecycleScope.launch(Dispatchers.IO) {
                val userFile = File(applicationContext.filesDir, "logged_user_info")
                prop.load(userFile.inputStream())

                for (key in prop.stringPropertyNames()) {
                    hashMap.put(key, prop.get(key).toString())
                }

                playerName.setText(hashMap["Name"] + " " + hashMap["Surname"])
                id.setText(hashMap["ID"])
                email.setText(hashMap["Mail"])

                val ifFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "logged_user_new_avatar.jpg")
                if (!ifFile.exists()) {
                    val url: Uri = Uri.parse(hashMap["Url"])
                    Glide.with(baseContext)
                        .asBitmap()
                        .load(url)
                        .centerCrop()
                        .into(BitmapThumbnailImageViewTarget(playerIcon))
                } else {
                    runOnUiThread(Runnable {
                        imageBitmap = BitmapFactory.decodeFile(ifFile.absolutePath)
                        playerIcon.setImageBitmap(imageBitmap)
                    })
                }

                // check server data

                val jsonRequest = JsonObjectRequest(Request.Method.GET, StringConstants.SERVER_URL + "/" + hashMap["ID"], null,
                    { response ->
                        val strResp = response.toString()
                        Log.d("APIonPROFILE", strResp)
                        gamesPlayed.text = response.getInt("GamesPlayed").toString()
                        totalKilometres.text = response.getInt("Kilometres").toString()
                        wins.text = response.getInt("Wins").toString()
                    },
                    { error ->
                        Log.d("APIonPROFILE", "error => $error")
                    }
                )
                MySingleton.getInstance(applicationContext).addToRequestQueue(jsonRequest)

            }

            playerIcon.invalidate()
        //}
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1337 && ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // create the image file
            try {
                val ifFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "logged_user_new_avatar.jpg")
                dispatchTakePictureIntent(ifFile)
            } catch (ex: IOException) {
                Log.d("FilerError", "Error on creating the file ...")
            }
        } else {
            val text = "You need to grant the permission to use the Camera to change your avatar!"
            val centeredText: Spannable = SpannableString(text)
            centeredText.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0, text.length - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            Toast.makeText(applicationContext, centeredText, Toast.LENGTH_LONG).show()
        }


    }


    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1380 && resultCode == RESULT_OK) {
            Log.d("ReturnIntent", "The picture was success")
            val ifFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "logged_user_new_avatar.jpg")
            Log.d("File-Path", ifFile.absolutePath)
            val playerIcon: ImageView = findViewById(R.id.profile_icon_id)
            imageBitmap = BitmapFactory.decodeFile(ifFile.absolutePath)
            playerIcon.setImageBitmap(imageBitmap)
        }
    }



    @Suppress("DEPRECATION")
    private fun dispatchTakePictureIntent(file:File) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Continue only if the File was successfully created
                file.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.application.geoguess.fileprovider",
                        it
                    )

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, 1380)
                }
            }
        }
    }





    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        for (i in 0 until menu!!.size()) {
            val item = menu.getItem(i)
            val spanString = SpannableString(menu.getItem(i).title.toString())
            spanString.setSpan(
                ForegroundColorSpan(Color.RED),
                0,
                spanString.length,
                0
            ) //fix the color to white
            item.title = spanString
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }

        R.id.context_menu_delete_account -> {
            // delete account on context-menu clicked
            // coroutine for account delete on server
            //lifecycleScope.launch(Dispatchers.IO) {
                val id: TextView = findViewById(R.id.id_id)
                val stringRequest = StringRequest(Request.Method.DELETE, StringConstants.SERVER_URL + "/" + id.text,
                    { response ->
                        val strResp = response.toString()
                        Log.d("APIonPROFILE-DELETE", "User Deleted from server: " + strResp)
                    },
                    { error ->
                        Log.d("APIonPROFILE-DELETE", "error => $error")
                    }
                )
                MySingleton.getInstance(applicationContext).addToRequestQueue(stringRequest)

                // Send Back activity with right result to log off the user

                val intent = Intent()
                intent.putExtra("Logout", "True")
                setResult(9999, intent)
                finish()
            //}
            true
        }

        else -> {
            // The user's action isn't recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(view: View?) {
        val intent = Intent()
        intent.putExtra("Logout", "True")
        setResult(9999, intent)
        finish()
    }

}

