package com.example.bejam.ui.home

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bejam.data.model.DailySelection
import com.example.bejam.databinding.FragmentShareBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShareFragment : Fragment() {
    private var _binding: FragmentShareBinding? = null
    private val binding get() = _binding!!
    private val args: ShareFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentShareBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.trackName.text = args.trackName
        binding.artistNames.text = args.artistNames
        Glide.with(this)
            .load(args.imageUrl)
            .into(binding.albumImage)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val docId = "${firebaseUser.uid}_$today"
            // Check if already posted today
            Firebase.firestore.collection("daily_selections")
                .document(docId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        // Already posted today, disable post button
                        binding.postButton.isEnabled = false
                        binding.postButton.text = "Du hast heute schon einen Song gepostet!"
                        Toast.makeText(requireContext(), "Du hast heute schon einen Song gepostet!", Toast.LENGTH_LONG).show()
                    } else {
                        // Enable post button
                        binding.postButton.isEnabled = true
                        binding.postButton.text = "Post"
                        binding.postButton.setOnClickListener {
                            postSong(docId)
                        }
                    }
                }
                .addOnFailureListener {
                    // In case of error, allow posting
                    binding.postButton.isEnabled = true
                    binding.postButton.text = "Post"
                    binding.postButton.setOnClickListener {
                        postSong(docId)
                    }
                }
        }
    }

    private fun postSong(docId: String) {
        val comment = binding.commentEditText.text.toString().trim()
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val selection = DailySelection(
                id = docId,
                userId = firebaseUser.uid,
                songId = args.trackId,
                songName = args.trackName,
                artist = args.artistNames,
                imageUrl = args.imageUrl,
                comment = comment,
                likes = emptyList(),
                timestamp = System.currentTimeMillis()
            )

            Firebase.firestore.collection("daily_selections")
                .document(docId)
                .set(selection)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Posted “${args.trackName}”!", Toast.LENGTH_SHORT).show()
                    binding.postButton.isEnabled = false
                    binding.postButton.text = "Du hast heute schon einen Song gepostet!"
                    findNavController().popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to post selection.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
