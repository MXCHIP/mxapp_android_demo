package com.mxchip.mxapp.account.ui

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.account.databinding.AccountDgAgreementBinding
import com.mxchip.mxapp.base.utils.AgreementManager
import com.mxchip.mxapp.base.utils.generateShape

/**
 * app协议说明展示弹框
 */
class AgreementDialog: DialogFragment() {

    private var policyInvoke: ((Int) -> Unit)? = null
    private var agreementBlock: (() -> Unit)? = null
    private var disagreeBlock: (() -> Unit)? = null

    fun setPolicyClick(block: (Int) -> Unit) {
        this.policyInvoke = block
    }

    fun setAgreementClick(block: () -> Unit) {
        this.agreementBlock = block
    }

    fun setDisagreeClick(block: () -> Unit) {
        this.disagreeBlock = block
    }


    private lateinit var binding: AccountDgAgreementBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AccountDgAgreementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //必须设置否则ClickableSpan 点击事件无效
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        binding.root.background =
            generateShape(requireContext().getColor(com.mxchip.mxapp.base.R.color.core_color_background_dialog), 20)
        binding.tvContent.text = textSpan()
        binding.btnQuit.setOnClickListener {
            dismiss()
            disagreeBlock?.invoke()
        }
        binding.btnAgree.setBtnBackgroundColor(ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        binding.btnAgree.setOnClickListener {
            dismiss()
            AgreementManager.agree()
            agreementBlock?.invoke()
        }
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }

    private fun textSpan(): SpannableString {
        val protocol = getString(com.mxchip.mxapp.base.R.string.mx_user_agreement)
        val policy = getString(com.mxchip.mxapp.base.R.string.mx_privacy_policy)
        val agreementText =
            String.format(getString(com.mxchip.mxapp.base.R.string.mx_agreement_dialog_txt), protocol, policy)
        val spanString = SpannableString(agreementText)
        val protocolIndex = spanString.indexOf(protocol)
        val policyIndex = spanString.indexOf(policy)

        val protocolClickSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                policyInvoke?.invoke(2)
            }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9)
//                ds.isUnderlineText = false
            }
        }

        val policyClickSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                policyInvoke?.invoke(1)
            }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9)
//                ds.isUnderlineText = false
            }
        }
        spanString.setSpan(
            protocolClickSpan,
            protocolIndex,
            protocolIndex + protocol.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spanString.setSpan(
            policyClickSpan,
            policyIndex,
            policyIndex + policy.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spanString
    }
}