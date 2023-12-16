@file:Suppress("DEPRECATION")

package com.example.flashcardapplication

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
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
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityMainBinding
import com.example.flashcardapplication.databinding.CustomViewDialogBinding
import com.example.flashcardapplication.fragments.HomePageFragment
import com.example.flashcardapplication.fragments.LibraryFragment
import com.example.flashcardapplication.models.Folder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NetworkListener{
    private lateinit var binding: ActivityMainBinding
    private val networkReceiver = NetworkReceiver(listener = this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(HomePageFragment(), "Trang chủ")
        adapter.addFragment(HomePageFragment(), "Lời giải") // change this
        adapter.addFragment(LibraryFragment(), "Thư viện")

        if(intent.hasExtra("viewPager")){
            binding.viewPager.currentItem = intent.getIntExtra("viewPager", 0)
        }

        binding.viewPager.adapter = adapter


        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.i_trang_chu -> binding.viewPager.currentItem = 0
                R.id.i_loi_giai -> binding.viewPager.currentItem = 1
                R.id.i_add -> {
                    val bottomSheetFragment = CustomBottomSheetDialogFragment()
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }

                R.id.i_thu_vien -> binding.viewPager.currentItem = 2
                R.id.i_ho_so -> binding.viewPager.currentItem = 3
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, intentFilter)
    }
    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        val dataSyncHelper = DataSyncHelper(
            firebaseDb = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = this
        )
        if(!dataSyncHelper.getIsSyncDelete()){
            GlobalScope.launch {
                dataSyncHelper.serverDelete()
            }
        }
        dataSyncHelper.setIsSync(false)
        GlobalScope.launch {
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Không có kết nối mạng")
            .setMessage("Vui lòng kiểm tra lại kết nối mạng")
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
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
    private var binding: CustomViewDialogBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CustomViewDialogBinding.inflate(inflater, container, false)
        val view = inflater.inflate(R.layout.bottom_sheet_dialog, container, false)

        tvCourse = view.findViewById(R.id.tv_course)
        tvFolder = view.findViewById(R.id.tv_folder)

        tvCourse?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvCourse?.setOnClickListener {
            startActivity(Intent(activity, CreateLessonActivity::class.java))
        }

        tvFolder?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvFolder?.setOnClickListener {
            val parent = binding?.root?.parent as? ViewGroup
            parent?.removeView(binding?.root)

            val dialog = AlertDialog.Builder(it.context)
                .setTitle("Tạo thư mục")
                .setView(binding?.root?.rootView)
                .setPositiveButton("Ok") { _, _ ->
                    val name = binding!!.edtName.text.toString()
                    val description = binding?.edtDescription?.text.toString()
                    if(name.isEmpty()){
                        binding!!.edtName.error = "Tên thư mục không được để trống"
                        return@setPositiveButton
                    }else{
                        val roomDb = context?.let { it1 -> RoomDb.getDatabase(it1) }
                        val folder = Folder(0, name, description, "public",
                            FirebaseAuth.getInstance().currentUser?.email.toString())
                        val folderId = roomDb?.ApplicationDao()?.insertFolder(folder)?.toInt()
                        DataSyncHelper(
                            firebaseDb = FirebaseFirestore.getInstance(),
                            auth = FirebaseAuth.getInstance(),
                            context = requireContext()
                        ).setIsSync(false)
                        val intent = Intent(activity, FolderActivity::class.java)
                        intent.putExtra("folderId", folderId)
                        startActivity(intent)
                    }
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }

        return view
    }
}
