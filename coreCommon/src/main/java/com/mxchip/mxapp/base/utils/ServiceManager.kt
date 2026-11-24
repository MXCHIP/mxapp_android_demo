package com.mxchip.mxapp.base.utils

//todo 改使用MxRouter注解方式：MxRouter.get<ISmartService>()?.executeScene(sceneId)
//object ServiceManager {
//    private val serviceMap = ConcurrentHashMap<Class<*>, Any>()
//
//    fun <T> register(interfaceClass: Class<T>, implementation: T) {
//        serviceMap[interfaceClass] = implementation as Any
//    }
//
//    fun <T> getService(interfaceClass: Class<T>): T {
//        @Suppress("UNCHECKED_CAST")
//        return serviceMap[interfaceClass] as? T
//            ?: throw IllegalStateException("Service not registered: $interfaceClass")
//    }
//}

interface IDeviceService {

}

interface ISmartService {
    fun executeScene(sceneId: Int)
}

interface IHomeService {

}

interface IRoomService {

}