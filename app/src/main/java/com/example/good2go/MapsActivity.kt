package com.example.good2go

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.MarkerOptions
import org.jetbrains.anko.db.*

class MapsActivity : AppCompatActivity() {

    private lateinit var map : AMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for permission
        if (!checkPermission()) {
            requestPermission(0)
        }

        // Set up the Gaode Map
        setContentView(R.layout.activity_maps)
        val mapView = findViewById<MapView>(R.id.map)
        mapView.onCreate(savedInstanceState)
        map = mapView.map

        // Retrieve all the markers from the database
        val markers = database.use {
            select("Posts", "id", "title", "description", "imageUri", "latitude", "longitude").parseList(classParser<Post>())
        }
        displayMarkers(markers)

        // Short click: Adds a new marker to the event
        map.setOnMapClickListener {
            val i = Intent(this, NewPostActivity::class.java)
            i.putExtra("latitude", it.latitude)
            i.putExtra("longitude", it.longitude)
            startActivityForResult(i, 1)
        }

        map.setOnMapLongClickListener {
            // Retrieve all posts close to the current marker
            val i = Intent(this, PostListActivity::class.java)
            i.putExtra("latitude", it.latitude)
            i.putExtra("longitude", it.longitude)
            startActivity(i)
        }

    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun requestPermission(code: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE), code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Retrieve data from intent
            val title = data!!.getStringExtra("title")!!
            var description = data.getStringExtra("description")!!

            // Clip the description if too long
            if (description.length > 15) {
                description = description.substring(0, 15) + "..."
            }
            val latitude = data.getDoubleExtra("latitude", 0.0)
            val longitude = data.getDoubleExtra("longitude", 0.0)
            val latLng = LatLng(latitude, longitude)

            // Make a new marker
            map.addMarker(MarkerOptions().position(latLng).title(title).snippet(description))
            Toast.makeText(this, "Marker Created!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayMarkers(markers: List<Post>) {
        markers.forEach {
            val latLng = LatLng(it.latitude, it.longitude)
            var desc = it.description
            if (desc.length > 15) {
                desc = desc.substring(0, 15) + "..."
            }
            map.addMarker(MarkerOptions().position(latLng).title(it.title).snippet(desc))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_maps, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuList -> {
                val i = Intent(this, PostListActivity::class.java)
                startActivity(i)
                true
            }
            R.id.lightbulb -> {
                val i = Intent(this, PostListActivity::class.java)
                // Special requirements for Feeling Lucky mode
                i.putExtra("latitude", 2000.0)
                i.putExtra("longitude", 2000.0)
                startActivity(i)
                Toast.makeText(this, "Feeling Lucky!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}