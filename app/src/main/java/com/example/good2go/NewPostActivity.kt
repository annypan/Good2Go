package com.example.good2go

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.getBitmap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_new_post.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import org.jetbrains.anko.db.*

class NewPostActivity : AppCompatActivity() {

    private val CAMERA = 0
    private val GALLERY = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_post)

        // Fetch data from the intent
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        // Saves the new marker to the database and send the information back to the map activity
        buttonSavePost.setOnClickListener {
            // Fetch data from the edit text boxes
            val title = editTextTitle.text.toString()
            val description = editTextDesc.text.toString()

            // Ask for image if none was uploaded
            // Otherwise, save to database and pass the data back to MapsActivity
            if (imageViewPreview.tag == null) {
                Toast.makeText(this, "Please Upload an Image!", Toast.LENGTH_SHORT).show()
            } else {
                saveToDatabase(title, description, imageViewPreview.tag as String, latitude, longitude)
                val i = Intent(this, MapsActivity::class.java)
                i.putExtra("latitude", latitude)
                i.putExtra("longitude", longitude)
                i.putExtra("title", title)
                i.putExtra("description", description)
                this.setResult(RESULT_OK, i)
                finish()
            }
        }

        // Cancel and go back to the map
        fabCancel.setOnClickListener {
            this.setResult(Activity.RESULT_CANCELED)
            finish()
        }

        buttonTakePic.setOnClickListener {
            takePhotoFromCamera()
        }

        buttonChooseFromGallery.setOnClickListener {
            choosePhotoFromGallery()
        }
    }

    // Helper function for taking photo with the camera
    private fun takePhotoFromCamera() {
        if (!checkPermission()) {
            requestPermission(CAMERA)
        } else {
            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(i, CAMERA)
        }
    }

    // Helper function for choosing photo from gallery
    private fun choosePhotoFromGallery() {
        if (!checkPermission()) {
            requestPermission(GALLERY)
        } else {
            val galleryIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA) {
            takePhotoFromCamera()
        } else if (requestCode == GALLERY) {
            choosePhotoFromGallery()
        }
    }

    // Gets run after you return from the gallery or camera
    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY) {
            if (data != null){
                val contentURI = data.data
                try {
                    lateinit var bitmap: Bitmap
                    contentURI?.let {
                        bitmap = if (Build.VERSION.SDK_INT < 28) {
                            getBitmap(this.contentResolver, contentURI)
                        } else {
                            val source = ImageDecoder.createSource(this.contentResolver, contentURI)
                            ImageDecoder.decodeBitmap(source)
                        }
                    }
                    val croppedBM = adjustWidthHeight(bitmap)
                    imageViewPreview!!.setImageBitmap(croppedBM)
                    imageViewPreview.tag = getPathFromURI(contentURI)
                }
                catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else if (requestCode == CAMERA) {
            var thumbnail = data!!.extras!!.get("data") as Bitmap
            thumbnail = Bitmap.createScaledBitmap(thumbnail, 450, 600, true)
            val croppedBM = adjustWidthHeight(thumbnail)
            imageViewPreview!!.setImageBitmap(croppedBM)
            imageViewPreview.tag = saveImage(croppedBM)
        }
    }

    // Checks for permissions
    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun requestPermission(code: Int) {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                code)
    }

    // gets path of image from uri
    private fun getPathFromURI(ContentUri: Uri?): String? {
        var res: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver
                .query(ContentUri!!, proj, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            res = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            cursor.close()
        }
        return res
    }

    // Saves photo to local storage and returns the photo Uri
    private fun saveImage(myBitmap: Bitmap): String {
        val bytes = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val wallpaperDirectory = getExternalFilesDir(null)
        // have the object build the directory structure, if needed.
        if (wallpaperDirectory != null) {
            if (!wallpaperDirectory.exists()) { wallpaperDirectory.mkdirs()}
        }

        try {
            val f = File(wallpaperDirectory, ((Calendar.getInstance()
                    .timeInMillis).toString() + ".jpg"))
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(this,
                    arrayOf(f.path),
                    arrayOf("image/jpeg"), null)
            fo.close()
            return f.absolutePath
        }
        catch (e1: IOException) {
            e1.printStackTrace()
        }
        return ""
    }

    private fun saveToDatabase(title: String, description: String, imageUri: String, latitude: Double, longitude: Double) {
        database.use {
            insert("Posts",
            "title" to title,
            "description" to description,
            "imageUri" to imageUri,
            "latitude" to latitude,
            "longitude" to longitude
            )
        }
        Toast.makeText(this, "Post Created!", Toast.LENGTH_SHORT).show()
    }

    private fun adjustWidthHeight(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val widthFactor = width / 600.00
        val heightFactor = height / 600.00

        if (widthFactor <= 1 && heightFactor <= 1) {
            return bitmap
        }

        return if (widthFactor > heightFactor) {
            Bitmap.createScaledBitmap(bitmap, 600, (600 / widthFactor * heightFactor).toInt(), true)
        } else {
            Bitmap.createScaledBitmap(bitmap, (600 * widthFactor / heightFactor).toInt(), 600, true)
        }
    }
}