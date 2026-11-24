package com.mxchip.mxapp.base.base

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.mxchip.http.internal.HttpError
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.dialog.LoadingDialog
import com.mxchip.mxapp.base.utils.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created with Android Studio
 * Talk is Cheap
 * @author: chenxukun
 * @date: 2021/8/11
 * @desc:
 */
abstract class MXBusinessFragment<Binding : ViewBinding> : Fragment() {
    companion object {
        private const val TAG = "MXBusinessFragment"
    }

    private val loadingDialog by lazy {
        LoadingDialog(requireContext())
    }

    protected lateinit var binding: Binding

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        return requireActivity().layoutInflater
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = getLayoutBinding(inflater, container, savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onInit(view, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(
            TAG,
            "FragmentLifecycle--->onFragmentAttach-->${javaClass.simpleName}-->activity: ${activity?.javaClass?.simpleName}"
        )
    }

    override fun onDetach() {
        Log.d(
            TAG,
            "FragmentLifecycle--->onFragmentDetach-->${javaClass.simpleName}-->activity: ${activity?.javaClass?.simpleName}"
        )
        super.onDetach()
    }

    override fun onDestroyView() {
        dismissLoading()
        super.onDestroyView()
    }

    fun showLoading() {
        loadingDialog.show()
    }

    fun dismissLoading() {
        loadingDialog.dismiss()
    }

    abstract fun getLayoutBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): Binding

    abstract fun onInit(view: View, savedInstanceState: Bundle?)

    private var toastTime = 0L

    protected fun onException(code: Int, showNetErrToast: Boolean = true): Boolean {
        if (code == HttpError.HTTP_STATUS_ERROR
            || code == HttpError.DNS_ERROR
            || code == HttpError.TIMEOUT_ERROR
            || code == HttpError.IO_ERROR
            || code == HttpError.JSON_PARSE_ERROR
        ) {
            if (showNetErrToast) {
                val curt = System.currentTimeMillis()
                if (curt - toastTime > 2000) {
                    toastTime = curt
                    showNetErrToast()
                }
            }
            return true
        }else if (code == HttpError.UNKNOWN_ERROR){
            MXLog.d(TAG,"http unknown error")
            return true
        }
        return false
    }

    fun showToast(msg: String) {
        ToastUtil.showToast(requireContext(), msg)
    }

    private fun showNetErrToast() {
        lifecycleScope.launch(Dispatchers.Main) {
            ToastUtil.showToast(
                requireContext(),
                MXBusinessApplication.mxBusinessApp.getString(com.mxchip.mxapp.base.R.string.mx_network_error)
            )
        }
    }
}