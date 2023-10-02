package com.application.geoguess

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.application.geoguess.ui.theme.GeoGuessTheme



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)
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
                Intent(applicationContext, GameActivity::class.java).apply {
                    putExtra(StringConstants.TYPE_OF_GAME_KEY, StringConstants.TWO_PLAYER_GAME)
                    putExtra(StringConstants.PLAYER_ONE_NAME_KEY, playerOneName.text.toString())
                    putExtra(StringConstants.PLAYER_TWO_NAME_KEY, playerTwoName.text.toString())
                    startActivity(this)
                }
            }
            create().show()
        }
    }
}