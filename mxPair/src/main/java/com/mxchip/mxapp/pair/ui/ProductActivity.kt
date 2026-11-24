package com.mxchip.mxapp.pair.ui

import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.pair.bean.FirstCategory
import com.mxchip.mxapp.pair.bean.SecondCategory
import com.mxchip.mxapp.pair.consts.PairConstants
import com.mxchip.mxapp.pair.databinding.PairAcProductBinding
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.viewmodel.PairProductViewModel
import com.mxchip.router.annotation.Router


/**
 *添加设备/产品列表
 */
@Router(RouterConstants.PRODUCT_ACTIVITY)
class ProductActivity : MXBusinessActivity<PairAcProductBinding>() {

    private val vm by viewModels<PairProductViewModel>()
    private var manualAddFragment: CategoryFragment? = null
    private val categoryList = arrayListOf<FirstCategory>()

    override fun getLayoutBinding(): PairAcProductBinding =
        PairAcProductBinding.inflate(layoutInflater)

    override fun onInit() {
        if (AppConfigManager.getUseHomeId() <= 0) {
            finish()
            return
        }
        binding.layoutToolbar.setPadding(0,AppUtil.getStatusBarHeight(),0,0)
        //配网,禁止proxy连接
        MeshSDKManage.isConnectProxy = false
        //移除暂存Wi-Fi信息
        PairDataCenter.setCacheWifi(null, null)
        //每次启动app清除记录的位置信息
        PairDataCenter.removeLocationCurrent()
        PairDataCenter.clearReplaceInfo()//清除一键替换信息
        initFragment()
        inject()
        initData()
    }

    private fun initFragment() {
        val fragments = mutableListOf<Fragment>()
        fragments.add(AutoFindFragment())
        manualAddFragment = CategoryFragment()
        fragments.add(manualAddFragment!!)
        //禁止滑动切换
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.offscreenPageLimit = 1
        binding.viewPager2.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragments[position]
            }
        }

        val tabLayoutMediator = TabLayoutMediator(
            binding.tabLayout, binding.viewPager2, true
        ) { tab, position ->
            if (position == 0) {
                tab.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_auto)
            } else tab.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_manual)
        }
        tabLayoutMediator.attach()

        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tabLayout.setSelectedTabIndicatorColor(mainColor)
        binding.tabLayout.setTabTextColors(
            ContextCompat.getColor(
                this,
                com.mxchip.mxapp.base.R.color.core_color_text_list_content
            ), mainColor
        )

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
//                tabTextBold(tab)
                //操作更新类别列表
                if (tab?.position == 1) manualAddFragment?.categoryData(categoryList)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
//                tab?.apply {
//                    val sp = SpannableString(tab.text)
//                    val span = sp.getSpans(0, sp.length, StyleSpan::class.java)
//                    if (span.isNotEmpty()) {
//                        sp.removeSpan(span[0])
//                        tab.text = sp
//                    }
//                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })
//        binding.tabLayout.getTabAt(0)?.select()
    }

//    private fun tabTextBold(tab: TabLayout.Tab?) {
//        tab?.apply {
//            val sp = SpannableString(tab.text)
//            sp.setSpan(
//                StyleSpan(Typeface.BOLD), 0, sp.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
//            )
//            tab.text = sp
//        }
//    }

    private fun inject() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.ivSearch.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.CATEGORY_SEARCH_ACTIVITY, mapOf(
                    PairConstants.KEY_CATEGORY_LIST to categoryList
                )
            )
        }
    }

    private fun initData() {
        vm.findProducts().launchAndCollectIn(
            this,
            Lifecycle.State.CREATED,
            loadFlowList<FirstCategory>(success = {
                categoryList.clear()
                val newCategory = mutableListOf<FirstCategory>()
                it?.forEach { first ->
                    val newSecondCategory = mutableListOf<SecondCategory>()
                    for (category in first.categorys) {
                        val newProduct = mutableListOf<ProductInfo>()
                        for (product in category.products) {
                            //缓存产品信息
                            ProductInfoManager.saveProductInfo(
                                product.product_key,
                                JsonUtil.toJson(product)
                            )
                            if (product.cloud_platform == PairConstants.CLOUD_PLATFORM_FEIYAN) {
                                //设备广播数据fog为pk，飞燕设备为pid，飞燕设备存储pid-pk对应关系
                                ProductInfoManager.saveProductKeyPid(
                                    product.product_key,
                                    product.feiyan_product_id
                                )
                            }
                            //过滤隐藏的产品，不展示在手动产品列表
                            if (!product.hide) {
                                newProduct.add(product)
                            }
                        }
                        newSecondCategory.add(
                            SecondCategory(
                                category.category_id,
                                category.name,
                                newProduct
                            )
                        )
                    }
                    newCategory.add(FirstCategory(first.category_id, first.name, newSecondCategory))
                }
                //将隐藏的产品过滤掉
                categoryList.addAll(newCategory)
            }, error = { code, msg, _ ->
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            })
        )
    }
}