@file:Suppress("DEPRECATION")

package com.example.flashcardapplication.fragments

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.databinding.FragmentLibraryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LibraryFragment : Fragment(), NetworkListener {
    private lateinit var dataSyncHelper: DataSyncHelper
    private val networkReceiver = NetworkReceiver(listener = this)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataSyncHelper = DataSyncHelper(
            firebaseDb = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = context
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLibraryBinding.inflate(inflater, container, false)

        val adapter = ViewPagerLibraryAdapter(childFragmentManager)
        adapter.addFragment(LibraryCourseFragment(), " Học phần ")
        adapter.addFragment(LibraryFolderFragment(), " Thư mục ")


        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.viewPager.adapter = adapter

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireActivity().registerReceiver(networkReceiver, intentFilter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        if(!dataSyncHelper.getIsSyncDelete()){
            GlobalScope.launch {
                dataSyncHelper.serverDelete()
            }
        }
        GlobalScope.launch {
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // nothing
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(networkReceiver)
    }
}

class ViewPagerLibraryAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
    private val fragmentList = ArrayList<Fragment>()
    private val fragmentTitleList = ArrayList<String>()
    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }
    override fun getCount(): Int {
        return fragmentList.size
    }
    fun addFragment(fragment: Fragment, title: String) {
        fragmentList.add(fragment)
        fragmentTitleList.add(title)
    }
    override fun getPageTitle(position: Int): CharSequence {
        return fragmentTitleList[position]
    }
}