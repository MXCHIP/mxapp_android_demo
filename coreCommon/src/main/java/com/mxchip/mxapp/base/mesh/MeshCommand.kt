package com.mxchip.mxapp.base.mesh

import com.mxchip.mesh.rule.IMeshCommand
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.bean.MeshState

class MeshCommand: IMeshCommand {
    override fun setMsg(
        uuid: String,
        hex: String,
        timeout: Boolean,
        retry: Boolean,
        timeoutMillis: Long,
        meshResult: (resultState: Boolean, resultMap: Map<String, Any>?) -> Unit
    ) {
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET, hex, { meshState, resultMap ->
                meshResult.invoke(meshState == MeshState.SUCCESS, resultMap)
            }, timeout, retry, timeoutMillis
        )
    }

    override fun getMsg(
        uuid: String,
        hex: String,
        timeout: Boolean,
        retry: Boolean,
        timeoutMillis: Long,
        meshResult: (resultState: Boolean, resultMap: Map<String, Any>?) -> Unit
    ) {
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_GET, hex, { meshState, resultMap ->
                meshResult.invoke(meshState == MeshState.SUCCESS, resultMap)
            }, timeout, retry, timeoutMillis
        )
    }

    override fun syncMsg(
        uuid: String,
        hex: String,
        timeout: Boolean,
        retry: Boolean,
        timeoutMillis: Long,
        meshResult: (resultState: Boolean, resultMap: Map<String, Any>?) -> Unit
    ) {
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_SYNC, hex, { meshState, resultMap ->
                meshResult.invoke(meshState == MeshState.SUCCESS, resultMap)
            }, timeout, retry, timeoutMillis
        )
    }
}