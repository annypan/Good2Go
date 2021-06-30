package com.example.good2go

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_new_post.*
import org.jetbrains.anko.db.delete

class PostAdapter(
        private val activity: Activity,
        private val postList: List<Post>,
        private val database: MyDatabaseOpenHelper,
        private val getPosition: (Int) -> Unit) : RecyclerView.Adapter<PostViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        context = parent.context
        return PostViewHolder(
                    LayoutInflater.from(activity).inflate(R.layout.post_item, parent, false)
            )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.bind(post, position, database, context)
    }

    override fun getItemCount() = postList.size
}

class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val textViewTitle: TextView = view.findViewById(R.id.textViewTitle)
    private val textViewDesc: TextView = view.findViewById(R.id.textViewDesc)
    private val imageView: ImageView = view.findViewById(R.id.imageViewPhoto)
    private val buttonDelete: Button = view.findViewById(R.id.buttonDelete)

    fun bind(item: Post, position: Int, database: MyDatabaseOpenHelper, context: Context) {
        textViewTitle.text = item.title
        textViewDesc.text = item.description
        imageView.setImageBitmap(BitmapFactory.decodeFile(item.imageUri))
        // imageView.setImageURI(Uri.parse(item.imageUri))
        Log.d("++++", item.imageUri)

        buttonDelete.setOnClickListener{
            database.use {
                delete("Posts", "id = {id}", "id" to item.id)
            }
            Toast.makeText(context, "Post Deleted!", Toast.LENGTH_SHORT).show()
            val i = Intent(context, MapsActivity::class.java)
            context.startActivity(i)
        }
    }
}