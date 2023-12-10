@file:Suppress("DEPRECATION")

package com.example.flashcardapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.flashcardapplication.databinding.ActivityMainBinding
import com.example.flashcardapplication.fragments.HomePageFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(HomePageFragment(), "Trang chủ")


        binding.viewPager.adapter = adapter


        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.i_trang_chu -> binding.viewPager.currentItem = 0
                R.id.i_loi_giai -> binding.viewPager.currentItem = 1
                R.id.i_add -> {
                    val bottomSheetFragment = CustomBottomSheetDialogFragment()
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }

                R.id.i_thu_vien -> binding.viewPager.currentItem = 3
                R.id.i_ho_so -> binding.viewPager.currentItem = 4
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

class CustomBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var tvCourse: TextView? = null
    private var tvFolder: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_dialog, container, false)

        tvCourse = view.findViewById(R.id.tv_course)
        tvFolder = view.findViewById(R.id.tv_folder)

        tvCourse?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvCourse?.setOnClickListener {
            startActivity(Intent(activity, CreateLessonActivity::class.java))
        }

        tvFolder?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvFolder?.setOnClickListener {
            AlertDialog.Builder(MainActivity())
                .setTitle("Tạo thư mục")
                .setView(R.layout.custom_view_dialog)
                .setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        return view
    }
}
