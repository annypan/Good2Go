package com.example.good2go

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.select

class PostListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_list)

        val latitude = intent.getDoubleExtra("latitude", 1000.0)
        val longitude = intent.getDoubleExtra("longitude", 1000.0)
        val recyclerview : RecyclerView = findViewById(R.id.recyclerView)

        if (latitude == 1000.0 && longitude == 1000.0) {
            // No LatLng constraint: display all posts
            val posts = database.use {
                select("Posts", "id", "title", "description", "imageUri", "latitude", "longitude").parseList(classParser<Post>())
            }
            // Feed data into the recyclerView
            recyclerview.layoutManager = LinearLayoutManager(this)
            recyclerview.adapter = PostAdapter(this, posts, database) {}
        } else if (latitude == 2000.0 && longitude == 2000.0) {
            // Feeling Lucky mode
            val posts = database.use{
                select("Posts", "id", "title", "description", "imageUri", "latitude", "longitude").parseList(classParser<Post>())
            }
            // Randomization by shuffling the post list and take the first
            val randomPost = posts.shuffled()[0]
            recyclerview.layoutManager = LinearLayoutManager(this)
            recyclerview.adapter = PostAdapter(this, arrayListOf(randomPost), database) {}
        } else {
            // LatLng constraint exists: choose places within 0.2 latitude and longitude
            val posts = database.use {
                select("Posts", "id", "title", "description", "imageUri", "latitude", "longitude")
                    .whereArgs("(latitude < {lat} + 0.2 and latitude > {lat} - 0.2) and (longitude < {lng} + 0.2 and longitude > {lng} - 0.2)",
                        "lat" to latitude, "lng" to longitude).parseList(classParser<Post>())
            }

            // Display different contents according to the number of posts in the list
            when {
                posts.isEmpty() -> {
                    Toast.makeText(this, "No Marker Nearby :(", Toast.LENGTH_SHORT).show()
                }
                posts.size == 1 -> {
                    Toast.makeText(this, "Found 1 Marker nearby!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Found " + posts.size.toString() + " Markers nearby!", Toast.LENGTH_SHORT).show()
                }
            }
            recyclerview.layoutManager = LinearLayoutManager(this)
            recyclerview.adapter = PostAdapter(this, posts, database) {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu._menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMap -> {
                val i = Intent(this, MapsActivity::class.java)
                startActivity(i)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}