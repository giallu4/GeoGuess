package com.application.geoguess


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapThumbnailImageViewTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Properties


class ProfileActivity : AppCompatActivity(), View.OnClickListener {
    private var imageBitmap: Bitmap? = null

    @Suppress("DEPRECATION")
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
        val file = File(applicationContext.filesDir, "new_profile_icon")

        toolbar?.setDisplayHomeAsUpEnabled(true)

        logOut.setOnClickListener(this)

        playerIcon.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 1337)
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

                if (!file.exists()) {
                    val url: Uri = Uri.parse(hashMap["Url"])
                    Glide.with(baseContext)
                        .asBitmap()
                        .load(url)
                        .centerCrop()
                        .into(BitmapThumbnailImageViewTarget(playerIcon))
                } else {
                    imageBitmap = BitmapFactory.decodeFile(userFile.absolutePath)
                    playerIcon.setImageBitmap(imageBitmap)
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

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1337 && resultCode == RESULT_OK) {
            val file = File(applicationContext.filesDir, "new_profile_icon")
            imageBitmap = data?.extras?.get("data") as Bitmap
            val playerIcon: ImageView = findViewById(R.id.profile_icon_id)
            playerIcon.setImageBitmap(imageBitmap)

            //save the picture
            //lifecycleScope.launch(Dispatchers.IO) {
                val outputStream = FileOutputStream(file)
                imageBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
            //}
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

