package com.application.geoguess


import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
        val backArrow:ImageView = findViewById(R.id.back_arrow_id)
        val playerName:TextView = findViewById(R.id.profile_name_id)
        val playerIcon:ImageView = findViewById(R.id.profile_icon_id)
        val gamesPlayed:TextView = findViewById(R.id.games_played_id)
        val id:TextView = findViewById(R.id.id_id)
        val email:TextView = findViewById(R.id.email_id)
        val hashMap = hashMapOf<String, String?>()
        val prop = Properties()

        backArrow.setOnClickListener {
            this.finish()
        }

        if (playerName.text == "nome cognome"){
            lifecycleScope.launch(Dispatchers.IO) {
                val userFile = File(applicationContext.filesDir, "logged_user_info")
                prop.load(userFile.inputStream())

                for (key in prop.stringPropertyNames()) {
                    hashMap.put(key, prop.get(key).toString())
                }

                playerName.setText(hashMap["Name"] + " " + hashMap["Surname"])
                id.setText(hashMap["ID"])
                email.setText(hashMap["Mail"])

                val url:Uri = Uri.parse(hashMap["Url"])
                Glide.with(baseContext)
                    .asBitmap()
                    .load(url)
                    .centerCrop()
                    .into(BitmapThumbnailImageViewTarget(playerIcon))


            }
            playerIcon.invalidate()
        }

    }
}