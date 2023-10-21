package com.application.geoguess


import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapThumbnailImageViewTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Properties


class ProfileActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity_layout)
        val playerName: TextView = findViewById(R.id.profile_name_id)
        val playerIcon: ImageView = findViewById(R.id.profile_icon_id)
        val gamesPlayed: TextView = findViewById(R.id.games_played_id)
        val totalKilometres: TextView = findViewById(R.id.total_kilometres_id)
        val toolbar: ActionBar? = supportActionBar
        val id: TextView = findViewById(R.id.id_id)
        val email: TextView = findViewById(R.id.email_id)
        val hashMap = hashMapOf<String, String?>()
        val prop = Properties()

        toolbar?.setDisplayHomeAsUpEnabled(true)

        if (playerName.text == "nome cognome") {
            lifecycleScope.launch(Dispatchers.IO) {
                val userFile = File(applicationContext.filesDir, "logged_user_info")
                prop.load(userFile.inputStream())

                for (key in prop.stringPropertyNames()) {
                    hashMap.put(key, prop.get(key).toString())
                }

                playerName.setText(hashMap["Name"] + " " + hashMap["Surname"])
                id.setText(hashMap["ID"])
                email.setText(hashMap["Mail"])

                val url: Uri = Uri.parse(hashMap["Url"])
                Glide.with(baseContext)
                    .asBitmap()
                    .load(url)
                    .centerCrop()
                    .into(BitmapThumbnailImageViewTarget(playerIcon))


            }

            // check server data
            //lifecycleScope.launch(Dispatchers.IO) {

            val jsonRequest = JsonObjectRequest(Request.Method.GET, StringConstants.SERVER_URL + "/" + hashMap["ID"], null,
                { response ->
                    val strResp = response.toString()
                    Log.d("APIonPROFILE", strResp)
                    gamesPlayed.text = response.getInt("GamesPlayed").toString()
                    totalKilometres.text = response.getInt("Kilometres").toString()
                },
                { error ->
                    Log.d("APIonPROFILE", "error => $error")
                }
            )
            MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
            //}

            playerIcon.invalidate()
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
            // User chooses the "Favorite" action. Mark the current item as a
            // favorite.
            true
        }

        else -> {
            // The user's action isn't recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

}

