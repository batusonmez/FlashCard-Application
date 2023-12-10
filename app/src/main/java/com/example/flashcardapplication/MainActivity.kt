@file:Suppress("DEPRECATION")

package com.example.flashcardapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.flashcardapplication.fragments.HomePageFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private var viewPage: ViewPager? = null
    private var bottomNavigation : BottomNavigationView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPage = findViewById(R.id.view_page)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(HomePageFragment(), "Trang chủ")


        viewPage?.adapter = adapter


        bottomNavigation?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.i_trang_chu -> viewPage?.currentItem = 0
                R.id.i_loi_giai -> viewPage?.currentItem = 1
                R.id.i_add -> viewPage?.currentItem = 2
                R.id.i_thu_vien -> viewPage?.currentItem = 3
                R.id.i_ho_so -> viewPage?.currentItem = 4
            }
            true
        }
    }

}

class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
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
