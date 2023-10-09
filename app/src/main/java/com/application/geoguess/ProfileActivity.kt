package com.application.geoguess

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Properties


class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity_layout)
        val playerName:TextView = findViewById(R.id.profile_name_id)
        val gamesPlayed:TextView = findViewById(R.id.games_played_id)
        val email:TextView = findViewById(R.id.email_id)
        val hashMap = hashMapOf<String, String?>()
        val prop = Properties()

        if (playerName.text == "nome cognome"){
            lifecycleScope.launch(Dispatchers.IO) {
                val userFile = File(applicationContext.filesDir, "logged_user_info")
                prop.load(userFile.inputStream())

                for (key in prop.stringPropertyNames()) {
                    hashMap.put(key, prop.get(key).toString())
                }
                playerName.setText(hashMap["Name"] + " " + hashMap["Surname"])
                email.setText(hashMap["Mail"])
            }

        }

    }
}