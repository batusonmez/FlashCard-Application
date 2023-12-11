@file:Suppress("DEPRECATION")

package com.example.flashcardapplication.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.flashcardapplication.databinding.FragmentLibraryBinding

class LibraryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLibraryBinding.inflate(inflater, container, false)

        val adapter = ViewPagerLibraryAdapter(childFragmentManager)
        adapter.addFragment(LibraryCourseFragment(), "Học phần")
        adapter.addFragment(LibraryFolderFragment(), "Thư mục")


        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.viewPager.adapter = adapter

        return binding.root
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