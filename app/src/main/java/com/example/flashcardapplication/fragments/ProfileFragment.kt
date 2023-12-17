package com.example.flashcardapplication.fragments

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.flashcardapplication.SettingActivity
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class ProfileFragment : Fragment(), NetworkListener {
    private var auth : FirebaseAuth? = null
    private var database : FirebaseFirestore? = null
    private lateinit var dataSyncHelper: DataSyncHelper
    private val networkReceiver = NetworkReceiver(listener = this)
    private var context: Context? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataSyncHelper = DataSyncHelper(
            firebaseDb = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = context
        )
        this.context = context
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        Picasso.get().load(auth?.currentUser?.photoUrl).into(binding.civAvatar)
        binding.tvName.text = auth?.currentUser?.displayName
        binding.tvEmail.text = auth?.currentUser?.email

        // handle with course

        binding.layoutSettings.setOnClickListener {
            val intent = android.content.Intent(requireContext(),SettingActivity::class.java)
            startActivity(intent)
            (context as Activity).finish()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireActivity().registerReceiver(networkReceiver, intentFilter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        GlobalScope.launch {
            if(!dataSyncHelper.getIsSyncDelete())
                dataSyncHelper.serverDelete()
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // Do nothing
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(networkReceiver)
    }
}