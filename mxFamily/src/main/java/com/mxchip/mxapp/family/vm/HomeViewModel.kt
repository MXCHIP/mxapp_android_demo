package com.mxchip.mxapp.family.vm

import androidx.paging.PagingData
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import com.mxchip.mxapp.base.base.BaseDefaultDataSource
import com.mxchip.mxapp.family.api.HomeRepository
import com.mxchip.mxapp.family.bean.HomeBean
import kotlinx.coroutines.flow.Flow

/**
 *
 */
class HomeViewModel : BaseViewModel() {

    private val homeRepository by lazy {
        HomeRepository()
    }

    fun findHomes(): Flow<NetStateResponse> {
        return launchFlow { homeRepository.findHome(mapOf("page_no" to 1, "page_size" to 199)) }
    }

    fun getHomeInfo(homeId: Int): Flow<NetStateResponse> {
        return launchFlow { homeRepository.getHomeInfo(homeId) }
    }

    fun setCurrentHome(homeId: Int): Flow<NetStateResponse> {
        return launchFlow { homeRepository.setCurrentHome(mapOf("home_id" to homeId)) }
    }

    fun findHomesPaging(): Flow<PagingData<HomeBean>> {
        return paging {
            BaseDefaultDataSource(HomeBean::class.java) {
                homeRepository.findHome(mapOf("page_no" to it, "page_size" to 20))
            }
        }
    }

    fun createFamilyFlow(name: String, rooms: List<String>): Flow<NetStateResponse> {
        return launchFlow {
            homeRepository.createFamily(
                mapOf(
                    "name" to name,
                    "room_names" to rooms
                )
            )
        }
    }

    fun createHomeSetDefaultRoom(
        name: String,
        rooms: List<Map<String, Any>>
    ): Flow<NetStateResponse> {
        return launchFlow {
            homeRepository.createHomeSetDefaultRoom(
                mapOf(
                    "name" to name,
                    "rooms" to rooms
                )
            )
        }
    }

    fun deleteFamily(homeId: Int): Flow<NetStateResponse> {
        return launchFlow { homeRepository.deleteFamily(mapOf("home_id" to homeId)) }
    }

    fun updateHomeName(homeId: Int, name: String): Flow<NetStateResponse> {
        return launchFlow {
            val param = mapOf("home_id" to homeId, "name" to name)
            homeRepository.updateHome(param)
        }
    }

    fun updateHomeLocation(
        homeId: Int,
        country_name: String,
        province_name: String,
        city_name: String,
        locationKey: String
    ): Flow<NetStateResponse> {
        val param = mapOf(
            "home_id" to homeId,
            "country_name" to country_name,
            "province_name" to province_name,
            "city_name" to city_name,
            "location_key" to locationKey
        )
        return launchFlow { homeRepository.updateHome(param) }
    }

    fun memberList(homeId: Int): Flow<NetStateResponse> {
        return launchFlow {
            homeRepository.memberList(
                mapOf(
                    "home_id" to homeId,
                    "page_size" to 199,
                    "page_no" to 1
                )
            )
        }
    }

    fun removeMember(homeId: Int, memberId: Int): Flow<NetStateResponse> {
        return launchFlow {
            homeRepository.removeFamilyMember(
                mapOf(
                    "home_id" to homeId,
                    "member_id" to memberId
                )
            )
        }
    }

    fun updateMember(
        home_id: Int,
        member_id: Int,
        name: String?,
        role: Int?,
        roomList: List<Int>?
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["home_id"] = home_id
        param["member_id"] = member_id
        if (!name.isNullOrBlank()) {
            param["name"] = name
        }
        role?.apply {
            param["role"] = this
        }
        if (!roomList.isNullOrEmpty()) {
            param["room_ids"] = roomList
        }
        return launchFlow {
            homeRepository.updateMember(param)
        }
    }

    fun bindShareFamilyByCode(type: Int, code: String): Flow<NetStateResponse> {
        return launchFlow {
            homeRepository.bindShareFamilyByCode(
                mapOf(
                    "type" to type,
                    "code" to code
                )
            )
        }
    }

    fun fetchLocationByKeyword(keyword: String): Flow<NetStateResponse> {
        return launchFlow { homeRepository.fetchLocationByKeyword(mapOf("keyword" to keyword)) }
    }

    fun geoToLocation(lat: String, lon: String): Flow<NetStateResponse> {
        return launchFlow { homeRepository.geoToLocation(mapOf("lat" to lat, "lon" to lon)) }
    }

    fun getWeatherOneHour(locationKey: String): Flow<NetStateResponse> {
        return launchFlow { homeRepository.getWeatherOneHour(mapOf("location_key" to locationKey)) }
    }

    fun handOverCode(home_id: Int, member_id: Int): Flow<NetStateResponse> {
        val map = mapOf(
            "home_id" to home_id,
            "member_id" to member_id
        )
        return launchFlow { homeRepository.handOverCode(map) }
    }

    fun handOver(home_id: Int, member_id: Int, code: String): Flow<NetStateResponse> {
        val map = mapOf(
            "home_id" to home_id,
            "member_id" to member_id,
            "code" to code
        )
        return launchFlow { homeRepository.handOver(map) }
    }

    fun handOverByAccount(homeId: Int, username: String): Flow<NetStateResponse> {
        val map = mapOf(
            "home_id" to homeId,
            "username" to username,
        )
        return launchFlow { homeRepository.handOverByAccount(map) }
    }

    fun shareInProgress(homeId: Int): Flow<NetStateResponse> {
        return launchFlow {
            homeRepository.shareInProgress(
                mapOf(
                    "home_id" to homeId,
                    "page_size" to 199
                )
            )
        }
    }

    fun shareCancel(id: Int): Flow<NetStateResponse> {
        val param = mapOf("share_id" to id)
        return launchFlow { homeRepository.shareCancel(param) }
    }

    fun checkAccountExist(
        type: Int,
        account: String,
        area: String?
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["account_type"] = type
        param["account"] = account
        if (!area.isNullOrBlank()) param["area"] = area
        return launchFlow { homeRepository.checkAccountExist(param) }
    }
}