package com.application.geoguess



import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.icu.text.NumberFormat
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.volley.toolbox.JsonObjectRequest
import com.application.geoguess.model.City
import com.application.geoguess.model.Player
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.Locale.getDefault
import java.util.Random


open class GameActivity : AppCompatActivity(), OnMapReadyCallback, MapboxMap.OnMapClickListener,
    LocationEngineCallback<LocationEngineResult> {
    private var mapboxMap: MapboxMap? = null
    private var userLocation: Location? = null
    private var engine: LocationEngine? = null
    private var locComponent: LocationComponent? = null
    private var userIconSymbol: Symbol? = null
    private var currentCityToGuess: City? = null
    private var isSinglePlayerGame = false
    private var playerOneSymbol: Symbol? = null
    private var playerTwoSymbol: Symbol? = null
    private var bullsEyeSymbol: Symbol? = null
    private lateinit var symbolManager: SymbolManager
    private lateinit var lineManager: LineManager
    private lateinit var line: Line
    private lateinit var playerOne: Player
    private var playerTwo: Player? = null
    private var distanceBooleanFeature: Boolean = false
    private lateinit var listOfCityFeatures: List<Feature>
    private var gamePlayed:Int = 0
    private var kilometres:Float = 0f
    private var singleWins:Int = 0
    private var textViewFlashingAnimation: Animation = AlphaAnimation(0.0f, 1.0f).apply {
        duration = 500
        startOffset = 1
        repeatMode = Animation.REVERSE
        repeatCount = 16
    }
    private var textViewFlashingAnimationTwo: Animation = AlphaAnimation(0.0f, 1.0f).apply {
        duration = 500
        startOffset = 1
        repeatMode = Animation.REVERSE
        repeatCount = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))


        setContentView(R.layout.game_activity_layout)
        val playerOnePointsTextView: TextView = findViewById(R.id.player_one_points_textID)
        val playerTwoPointsTextView: TextView = findViewById(R.id.player_two_points_textID)
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)

        // One/two Players?
        isSinglePlayerGame = intent?.getStringExtra(StringConstants.TYPE_OF_GAME_KEY) == StringConstants.ONE_PLAYER_GAME

        // Handle visibility of players' texts according to single/multi player
        playerOne = Player(intent?.getStringExtra(StringConstants.PLAYER_ONE_NAME_KEY))

        if (!isSinglePlayerGame) {
            playerOnePointsTextView.visibility = View.VISIBLE
            playerTwoPointsTextView.visibility = View.VISIBLE
            playerTwo = Player(intent.getStringExtra(StringConstants.PLAYER_TWO_NAME_KEY)!!)
            displayPlayersPoints()
        }

        if (isSinglePlayerGame) {
            // Check Google Play Location service on device & request localization permission
            val api = GoogleApiAvailability.getInstance()
            when (val apiCode = api.isGooglePlayServicesAvailable(applicationContext)) {
                ConnectionResult.SUCCESS -> {
                    val locationPermissionRequest = registerForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        when {
                            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                                // Precise location access granted.
                                distanceBooleanFeature = true
                            }
                            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                                // Only approximate location access granted.
                                distanceBooleanFeature = true
                            } else -> {
                            // No location access granted.
                            val text = "Permission Denied: some features won't work as intended!"
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

                    if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                        locationPermissionRequest.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION))
                    } else {
                        distanceBooleanFeature = true
                    }

                }
                ConnectionResult.SERVICE_MISSING, ConnectionResult.SERVICE_UPDATING, ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, ConnectionResult.SERVICE_DISABLED -> {
                    api.getErrorDialog(this, apiCode, 100)?.show()
                }
                else -> {
                    Toast.makeText(applicationContext, "There is an error with Google API", Toast.LENGTH_LONG).show()
                }
            }
        }


        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(Style.MAPBOX_STREETS)
                .withImage(PLAYER_ONE_ICON_ID, BitmapFactory.decodeResource(this.resources, R.drawable.player_one_icon))
                .withImage(PLAYER_TWO_ICON_ID, BitmapFactory.decodeResource(this.resources, R.drawable.player_two_icon))
                .withImage(BULLSEYE_ICON_ID, BitmapFactory.decodeResource(this.resources, R.drawable.target_bullseye_icon))
                .withImage(HOUSE_ICON_ID, ContextCompat.getDrawable(baseContext, R.drawable.house_icon)!!)
        ) {
            CoroutineScope(context = Dispatchers.Default + Job()).launch {
                try {

                    val featureCollection = FeatureCollection.fromJson(loadGeoJsonFromAsset("cities.geojson"))
                    listOfCityFeatures = featureCollection.features()!!

                    runOnUiThread {

                        setNewRandomCityToGuess()

                        // Set up a SymbolManager and LineManager instance
                        symbolManager = SymbolManager(mapView, mapboxMap, it)
                        symbolManager.iconAllowOverlap = true
                        symbolManager.iconIgnorePlacement = true

                        lineManager = LineManager(mapView, mapboxMap, it)

                        mapboxMap.addOnMapClickListener(this@GameActivity)
                        handleUserGPSLocation(it)
                        initAnswerButton()
                    }
                } catch (exception: Exception) {
                    Log.d(TAG, "getFeatureCollectionFromJson exception: $exception")
                }
            }
        }
    }


     // Set the onClickListener for the answer button.

    private fun initAnswerButton() {
        val checkAnswerButton: com.google.android.material.floatingactionbutton.FloatingActionButton = findViewById(R.id.check_answer_button)
        checkAnswerButton.setOnClickListener {
            if (isSinglePlayerGame) {
                gamePlayed += 1
                val distanceForWin = getDistanceBetweenTargetAndGuess(playerOne)
                if (distanceForWin.split(",").size == 1){
                    singleWins += 1
                    Snackbar.make(findViewById(android.R.id.content),
                        resources.getString(R.string.player_guess_distance_won,
                            getDistanceBetweenTargetAndGuess(playerOne), currentCityToGuess?.name),
                        Snackbar.LENGTH_INDEFINITE).show()
                    drawLine(playerOne.selectedLatLng!!, currentCityToGuess!!.latLng!!)
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                        resources.getString(R.string.player_guess_distance_loss,
                            getDistanceBetweenTargetAndGuess(playerOne), currentCityToGuess?.name),
                        Snackbar.LENGTH_INDEFINITE).show()
                    drawLine(playerOne.selectedLatLng!!, currentCityToGuess!!.latLng!!)
                }


                resetUiAfterAnswerDistanceCheck()
                checkAnswerButton.hide()
            } else {
                if (!playerOne.hasGuessed) {
                    playerOne.hasGuessed = true
                    Toast.makeText(this, String.format(getString(R.string.player_two_turn_to_guess), playerTwo!!.name), Toast.LENGTH_SHORT).show()
                    checkAnswerButton.hide()
                } else if (playerOne.hasGuessed && !playerTwo?.hasGuessed!!) {
                    calculateAndGivePointToWinner()
                    resetUiAfterAnswerDistanceCheck()
                    playerOne.hasGuessed = false
                    playerTwo?.hasGuessed = false
                    checkAnswerButton.hide()
                }
            }
        }
    }


     //Reset UI after round

    private fun resetUiAfterAnswerDistanceCheck() {
        setBullsEyeMarker(currentCityToGuess?.latLng!!)
        setCameraBoundsToSelectedAndTargetMarkers()

        if (isSinglePlayerGame && userIconSymbol != null){
            // Wait before launching second Snack-bar/Line/info's
            CoroutineScope(context = Dispatchers.Main + Job()).launch {
                val coordinatesOfHome = LatLng(userLocation)
                val distance =  getDistanceBetweenHomeAndGuess(playerOne)
                delay(7000) //wait
                val snackBar = Snackbar.make(findViewById(android.R.id.content),
                    resources.getString(R.string.home_distance_from_city,
                        distance),
                    Snackbar.LENGTH_INDEFINITE)
                snackBar.show()

                val split = distance.split(".")[0]
                kilometres += NumberFormat.getNumberInstance(java.util.Locale.US).parse(split).toInt()

                lineManager.delete(line)
                drawLine(playerOne.selectedLatLng!!,coordinatesOfHome)

                // move camera

                val latLngBounds: LatLngBounds =
                    LatLngBounds.Builder()
                        .include(playerOne.selectedLatLng!!)
                        .include(coordinatesOfHome)
                        .build()

                mapboxMap?.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,
                    CAMERA_BOUNDS_PADDING), EASE_CAMERA_SPEED_IN_MS_SLOWER)
                delay(7000) //wait
                snackBar.dismiss()
                lineManager.deleteAll()
            }
        }

        setNewRandomCityToGuess()
    }



    override fun onMapClick(mapClickPoint: LatLng): Boolean {
        val checkAnswerButton: com.google.android.material.floatingactionbutton.FloatingActionButton = findViewById(R.id.check_answer_button)
        if (isSinglePlayerGame) {
            setPlayerOneMarker(mapClickPoint)
            playerOne.selectedLatLng = mapClickPoint
        } else {
            if (!playerOne.hasGuessed && !playerTwo!!.hasGuessed) {
                setPlayerOneMarker(mapClickPoint)
                playerOne.selectedLatLng = mapClickPoint
            } else if (playerOne.hasGuessed && !playerTwo!!.hasGuessed) {
                setPlayerTwoMarker(mapClickPoint)
                playerTwo!!.selectedLatLng = mapClickPoint
            }
        }
        checkAnswerButton.show()
        return true
    }


      // Adjust the first player's guess icon

    private fun setPlayerOneMarker(newLatLng: LatLng) {
        if (playerOneSymbol == null) {
            playerOneSymbol = symbolManager.create(SymbolOptions()
                    .withLatLng(newLatLng)
                    .withIconImage(PLAYER_ONE_ICON_ID)
                    .withIconSize(ICON_SIZE)
                    .withDraggable(false))
        } else {
            playerOneSymbol!!.let {
                it.latLng = newLatLng
                symbolManager.update(it)
            }
        }
    }

    //Draw a line connecting two points
    private fun drawLine(firstPoint: LatLng, secondPoint: LatLng){
        line = lineManager.create(LineOptions()
            .withLatLngs(listOf(firstPoint,secondPoint))
            .withDraggable(false)
            .withLineColor("#FF4081")
        )

    }

    // Adjust the second player's guess icon

    private fun setPlayerTwoMarker(newLatLng: LatLng) {
        if (playerTwoSymbol == null) {
            playerTwoSymbol = symbolManager.create(SymbolOptions()
                    .withLatLng(newLatLng)
                    .withIconImage(PLAYER_TWO_ICON_ID)
                    .withIconSize(ICON_SIZE)
                    .withDraggable(false))
        } else {
            playerTwoSymbol!!.let {
                it.latLng = newLatLng
                symbolManager.update(it)
            }
        }
    }


     // Create/adjust the target city icon

    private fun setBullsEyeMarker(bullsEyeLocation: LatLng) {
        if (bullsEyeSymbol == null) {
            bullsEyeSymbol = symbolManager.create(SymbolOptions()
                    .withLatLng(bullsEyeLocation)
                    .withIconImage(BULLSEYE_ICON_ID)
                    .withIconSize(ICON_SIZE)
                    .withDraggable(false))
        } else {
            bullsEyeSymbol!!.let {
                it.latLng = bullsEyeLocation
                symbolManager.update(it)
            }
        }
    }


     // Move the map camera to show certain coordinates.

    private fun setCameraBoundsToSelectedAndTargetMarkers() {
        val latLngBounds: LatLngBounds = if (isSinglePlayerGame) {
            LatLngBounds.Builder()
                    .include(playerOne.selectedLatLng!!)
                    .include(currentCityToGuess!!.latLng!!)
                    .build()
        } else {
            LatLngBounds.Builder()
                    .include(currentCityToGuess!!.latLng!!)
                    .include(playerOne.selectedLatLng!!)
                    .include(playerTwo!!.selectedLatLng!!)
                    .build()
        }
        mapboxMap?.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,
                CAMERA_BOUNDS_PADDING), EASE_CAMERA_SPEED_IN_MS)
    }


     //Retrieve and set a new city as the target city to guess.

    @SuppressLint("SetTextI18n")
    private fun setNewRandomCityToGuess() {
        val locationToGuess: TextView = findViewById(R.id.location_to_guess_textview)
        if (listOfCityFeatures.isNotEmpty()) {
            val randomCityFromList = listOfCityFeatures[Random().nextInt(listOfCityFeatures.size).plus(1)]
            val randomCityAsPoint = randomCityFromList.geometry() as Point
            currentCityToGuess = City(randomCityFromList.getStringProperty(FEATURE_CITY_PROPERTY_KEY),
                    LatLng(randomCityAsPoint.latitude(), randomCityAsPoint.longitude()))
            locationToGuess.text = "Searching city for next round ..."
            locationToGuess.startAnimation(textViewFlashingAnimation)

            // wait for animations + user reading of the snack-bars results
            CoroutineScope(context = Dispatchers.Default + Job()).launch {
                delay(8000)
                runOnUiThread(Runnable{
                    locationToGuess.text = resources.getString(
                        R.string.location_to_guess,
                        currentCityToGuess!!.name
                    )
                    locationToGuess.startAnimation(textViewFlashingAnimationTwo)
                })

            }
        }
    }


     // Mapbox's Turf to get distance between two [Point]s.

    @SuppressLint("ObsoleteSdkInt")
    private fun getDistanceBetweenTargetAndGuess(playerToCheck: Player?): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NumberFormat.getNumberInstance(getDefault()).format(TurfMeasurement.distance(
                    Point.fromLngLat(currentCityToGuess!!.latLng?.longitude!!,
                            currentCityToGuess!!.latLng?.latitude!!),
                    Point.fromLngLat(playerToCheck!!.selectedLatLng?.longitude!!,
                            playerToCheck.selectedLatLng?.latitude!!)))
        } else {
            TurfMeasurement.distance(
                    Point.fromLngLat(currentCityToGuess!!.latLng?.longitude!!,
                            currentCityToGuess!!.latLng?.latitude!!),
                    Point.fromLngLat(playerToCheck!!.selectedLatLng?.longitude!!,
                            playerToCheck.selectedLatLng?.latitude!!)).toString()
        }
    }


    // Mapbox's Turf to get distance between user Home and his target selection

    @SuppressLint("ObsoleteSdkInt")
    private fun getDistanceBetweenHomeAndGuess(playerToCheck: Player?): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NumberFormat.getNumberInstance(getDefault()).format(TurfMeasurement.distance(
                Point.fromLngLat(userLocation!!.longitude,
                    userLocation!!.latitude),
                Point.fromLngLat(playerToCheck!!.selectedLatLng?.longitude!!,
                    playerToCheck.selectedLatLng?.latitude!!)))
        } else {
            TurfMeasurement.distance(
                Point.fromLngLat(userLocation!!.longitude,
                    userLocation!!.latitude),
                Point.fromLngLat(playerToCheck!!.selectedLatLng?.longitude!!,
                    playerToCheck.selectedLatLng?.latitude!!)).toString()
        }
    }


     // Calculate which player's guess was closer to the target city

    private fun calculateAndGivePointToWinner() {
        //val playerOnePointsTextView: TextView = findViewById(R.id.player_one_points_textID)
        //val playerTwoPointsTextView: TextView = findViewById(R.id.player_two_points_textID)
        when (getDistanceBetweenTargetAndGuess(playerOne) < getDistanceBetweenTargetAndGuess(playerTwo)) {
            true -> {
                playerOne.points = playerOne.points.plus(1)
                //playerOnePointsTextView.startAnimation(textViewFlashingAnimation)
            }
            false -> {
                playerTwo?.points = playerTwo?.points!!.plus(1)
                //playerTwoPointsTextView.startAnimation(textViewFlashingAnimation)
            }
        }
        Snackbar.make(findViewById(android.R.id.content),
                resources.getString(R.string.winner_announcement,
                        if (getDistanceBetweenTargetAndGuess(playerOne) <
                                getDistanceBetweenTargetAndGuess(playerTwo))
                            playerOne.name else playerTwo!!.name),
                Snackbar.LENGTH_SHORT).show()
        displayPlayersPoints()
    }


     // Update the players' points.

    private fun displayPlayersPoints() {
        val playerOnePointsTextView: TextView = findViewById(R.id.player_one_points_textID)
        val playerTwoPointsTextView: TextView = findViewById(R.id.player_two_points_textID)
        setPlayerTextViews(playerOnePointsTextView, R.string.player_one_points, if (playerOne.name!!.isEmpty()) getString(R.string.default_player_one_name) else playerOne.name!!, playerOne.points.toInt())
        setPlayerTextViews(playerTwoPointsTextView, R.string.player_two_points, if (playerTwo?.name!!.isEmpty()) getString(R.string.default_player_two_name) else playerTwo!!.name!!, playerTwo!!.points.toInt())
    }

    private fun setPlayerTextViews(textView: TextView, stringId: Int, playerName: String, numOfPoints: Int) {
        textView.text = String.format(getString(stringId), playerName, numOfPoints, getString(R.string.points))
    }


    // load the GeoJSON file function

    @Suppress("SameParameterValue")
    private fun loadGeoJsonFromAsset(filename: String): String {
        return try {
            // Load GeoJSON file from local asset folder
            val `is` = assets.open(filename)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (exception: Exception) {
            throw RuntimeException(exception)
        }
    }

    private fun handleUserGPSLocation(mapStyle:Style) {
        // With location permissions, if single player THEN retrieve the user location
        if (isSinglePlayerGame && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {

            if (locComponent == null) {
                locComponent = mapboxMap!!.locationComponent
                val options = LocationComponentActivationOptions
                    .Builder(baseContext, mapStyle)
                    .build()
                locComponent!!.activateLocationComponent(options)
                locComponent!!.setLocationComponentEnabled(true)
                locComponent!!.setCameraMode(CameraMode.TRACKING_GPS)
                locComponent!!.setRenderMode(RenderMode.GPS)
            }

            //if ( userLocation == null){           really needed? Maybe to improve performance...
                engine = locComponent!!.locationEngine
                engine?.requestLocationUpdates(LocationEngineRequest.Builder(5000).build(),this,
                    Looper.getMainLooper())
            //}

        }
    }

    public override fun onResume() {
        super.onResume()
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        mapView.onStop()
    }

    public override fun onPause() {
        super.onPause()
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        handleUserGPSLocation(mapboxMap?.style!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        mapboxMap?.removeOnMapClickListener(this)
        mapView.onDestroy()

        //Handle app user info update on closure with one last coroutine

        val extractedId = intent?.getStringExtra("ID_OF_LOGGED")
        if (isSinglePlayerGame && extractedId != "null"){
            //lifecycleScope.launch(Dispatchers.IO) {

            val jsonParams = JSONObject()
            try {
                //JSONfy
                jsonParams.put("GamesPlayed", gamePlayed)
                jsonParams.put("Kilometres", kilometres)
                jsonParams.put("Wins", singleWins)
                Log.d("API-JSON", jsonParams.toString())
            } catch (e: JSONException) {
                Log.d("API", e.stackTraceToString())
            }

                val jsonRequest =
                     object: JsonObjectRequest(
                        Method.POST, "${StringConstants.SERVER_URL}/$extractedId", jsonParams,
                         { response ->
                             // response
                             val strResp = response.toString()
                             Log.d("APIResponse", "The server updated user with:" + strResp)
                         },
                         { error ->
                             Log.d("API", "error => $error")
                             Log.d("API", error.stackTraceToString())
                         }
                    ){
                         override fun getHeaders(): MutableMap<String, String> {
                             val headers = HashMap<String, String>()
                             headers["Content-Type"] = "application/json"
                             return headers
                         }
                     }
                MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
                gamePlayed = 0
                kilometres = 0f
                singleWins = 0
                //}
            }


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapView: com.mapbox.mapboxsdk.maps.MapView = findViewById(R.id.mapView_textID)
        symbolManager.deleteAll()
        userIconSymbol = null
        lineManager.deleteAll()
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val CAMERA_BOUNDS_PADDING = 190
        private const val EASE_CAMERA_SPEED_IN_MS = 1000
        private const val EASE_CAMERA_SPEED_IN_MS_SLOWER = 2000
        private const val FEATURE_CITY_PROPERTY_KEY = "city"
        private const val ICON_SIZE = 1.2f
        private const val PLAYER_ONE_ICON_ID = "PLAYER_ONE_ICON_ID"
        private const val PLAYER_TWO_ICON_ID = "PLAYER_TWO_ICON_ID"
        private const val BULLSEYE_ICON_ID = "BULLSEYE_ICON_ID"
        private const val HOUSE_ICON_ID = "HOUSE_ICON_ID"
        private const val TAG = "GameActivity"
    }

    override fun onSuccess(p0: LocationEngineResult?) {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            userLocation = p0?.lastLocation

            if (userIconSymbol == null) {
                userIconSymbol = symbolManager.create(
                    SymbolOptions()
                        .withLatLng(LatLng(userLocation))
                        .withIconImage(HOUSE_ICON_ID)
                        .withIconSize(ICON_SIZE)
                        .withDraggable(false)
                )
            }

            val text = "Now I know where you are! " + ("\ud83d\ude0e")
            val centeredText: Spannable = SpannableString(text)
            centeredText.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0, text.length - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            Toast.makeText(applicationContext, centeredText, Toast.LENGTH_LONG).show()

            engine?.removeLocationUpdates(this)
            locComponent!!.setLocationComponentEnabled(false)
        }
    }
    override fun onFailure(p0: java.lang.Exception) {
        Log.d("ErrorLocation", "Error on retrieving the position")
    }
}