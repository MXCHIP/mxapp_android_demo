# MX App Demo 模块说明与客户定制指南

本文面向需要基于本 Demo 做品牌换肤、文案调整、功能裁剪和环境切换的客户。Demo 以「智家精灵 / Smart Plus」为模板，业务模块可独立改界面，云端地址和功能开关集中在 `app` 模块初始化。

当前工程信息：

| 项 | 值 | 位置 |
| --- | --- | --- |
| 工程名 | `mxapp_android_demo` | `settings.gradle` |
| applicationId | `com.mxchip.mxapp` | `app/build.gradle` |
| 代码包名 | `com.mxchip.mxapp` | 各模块 `namespace` |
| 版本 | versionName `1.0.6`，versionCode `186` | `app/build.gradle` |
| minSdk / targetSdk | 26 / 36 | `gradle/libs.versions.toml` |
| 页面跳转 | 注解路由 `@Router` + `RouterManager` | `coreCommon/.../router/RouterConstants.kt` |

模块之间不直接 `startActivity`，统一走路由常量。改某个页面的布局不会影响跳转地址；改路由字符串才会。

---

## 1. 模块总览

`settings.gradle` 实际参与编译的模块：

```
app
 ├── coreCommon          公共基座（路由、主题色、文案、Mesh、网络）
 ├── mxAccount           登录注册、验证码、国家区号
 ├── mxFamily            家庭、成员、邀请、位置
 ├── mxRoom              房间管理
 ├── mxDevice            设备列表、详情、分享、H5 面板、一键替换选设备
 └── mxPair              配网（添加设备）
```

在`EnvConfigManager`改云环境、AppId、AppSecret 。

依赖的 MXCHIP 二进制 SDK（源码不在本仓库，客户一般不改）：

| 坐标 | 作用 |
| --- | --- |
| `com.mxchip.android:link` | 配网协议实现（飞燕 / 天猫 / Mesh / Fog） |
| `com.mxchip.android:mesh`、`mesh-rule` | 蓝牙 Mesh 连接与规则下发 |
| `com.mxchip.android:http` | HTTP 客户端 |
| `com.mxchip.android:scanner` | 蓝牙扫描 |
| `com.mxchip.android:log`、`utils` | 日志与工具 |
| `com.mxchip.android:router-plugin` | 编译期路由注册 |

---

## 2. 各模块做什么

### 2.1 `app`：壳工程，客户最先改这里

负责进程启动、环境注入、首页壳和登录入口。业务页大多在子模块。

| 类 | 作用 |
| --- | --- |
| `MXApplication` | `initAppConfig()` 注入环境和功能开关，再初始化 HTTP。Token 失效（10403 / 10401）时清登录态并回登录引导页 |
| `StartActivity` | 启动页。已登录进首页，未登录进登录引导 |
| `LoginGuideActivity` | 登录 / 注册入口 |
| `UserNameLoginActivity`、`RegisterActivity` | 账号密码登录、注册（页面在 app，接口在 `mxAccount`） |
| `MainActivity` | 首页壳：家庭切换、房间设备、右上角添加设备。Debug 包右上角可点环境名切换 dev / test / preview / release |
| `fragment/RoomFragment`、`RoomInfoFragment` | 首页房间与设备卡片 |
| `fragment/SmartHomeFragment` | 全屋传感器等首页区块 |
| `RoomDeviceManagerActivity` | 房间内设备管理，可进入替换配网引导 |
| `MasterGatewayManageActivity` | 主网关管理 |
| `SmartHomeSensorSettingActivity` | 传感器设置，可进入添加设备 |
| `utils/EnvConfigManager` | **环境、AppId、功能开关的唯一入口** |

首页「+」添加设备：`MainActivity` 直接打开配网产品页 `PRODUCT_ACTIVITY`。

房间里「添加设备」：`RoomInfoFragment` 先看 `PairManager.isSupportProductCategory()`。打开时进产品列表；关掉时跳过品类页，直接进扫描页。

### 2.2 `coreCommon`：公共基座

所有业务模块都依赖它。客户换应用名、品牌色、通用弹窗、通用文案，主要改这里。

| 内容 | 路径 |
| --- | --- |
| 应用名、全量文案（中 / 英） | `coreCommon/src/main/res/values/strings.xml`、`values-zh-rCN/strings.xml`、`values-en/strings.xml` |
| 品牌色与通用色 | `coreCommon/src/main/res/values/colors.xml`，夜间覆盖 `values-night/colors.xml` |
| 路由表 | `base/router/RouterConstants.kt` |
| 当前家庭、Host、备案号 | `base/utils/AppConfigManager.kt` |
| 登录态 | `base/utils/LoginManager.kt` |
| 设备能力开关 | `base/utils/DeviceManage.kt` |
| Mesh 连接 | `base/mesh/MeshSDKManage.kt` |
| 通用顶栏、列表项、弹窗 | `base/widget/TopBarView.kt`、`CommonItemView.kt`、`base/dialog/MXDialog.kt` |
| 扫码页 | `base/activity/QrScanActivity.kt` |
| H5 WebView 与 JS Bridge | `base/activity/MXWebViewActivity.kt`、`base/h5bridge/` |
| 深色模式 | `base/utils/NightModeManager.kt` |

应用名资源是 `mx_app_name`：

- 默认 / 英文：`Smart Plus`
- 简体中文：`智家精灵`

Manifest 的 `android:label` 也引用这个字符串。

### 2.3 `mxAccount`：账号

登录、注册、验证码、设置密码、国家 / 区号选择。

主要页面：`VerificationCodeActivity`、`PasswordActivity`、`SetPasswordActivity`、`SelectCodeActivity`、`AccountRegionSearchActivity`、协议弹窗 `AgreementDialog`。

接口在 `account/api/AccountApiService.kt`，例如 `/app/v1/auth/login`、`/app/v1/auth/register`、`/app/v1/auth/sendVerifyCode`。

是否展示选国家由 `AccountManager.setSupportSelectCountry()` 控制，在 `EnvConfigManager.initAppConfig()` 里调用。当前代码写的是 `false`。同文件里的常量 `SELECT_COUNTRY = true` **没有被引用**，改它不会生效。

### 2.4 `mxFamily`：家庭

家庭列表、创建家庭引导、家庭设置、成员、邀请码加入、移交家庭、家庭位置。

主要页面：`HomeManageActivity`、`FamilySettingActivity`、`CreateHomeGuideActivity`、`CreateRoomGuideActivity`、`InviteMemberActivity`、`JoinHomeByInviteCodeActivity`、`HandOverVerificationActivity`、`LocationActivity`、`HomeMemberDetailActivity`。

开关在 `HomeManager`：

- `setSupportHomeShare(true)`：成员邀请
- `setSupportHomeLocation(true)`：家庭位置

接口示例：`/app/v3/home/list`、`/app/v3/home/create`、`/app/v3/home/member/list`。

### 2.5 `mxRoom`：房间

房间列表、创建、设置、成员房间权限、选择房间、房间二维码。

主要页面：`RoomManageActivity`、`RoomCreateActivity`、`RoomSettingActivity`、`RoomSelectActivity`、`RoomPermissionActivity`。

接口在 `room/api/RoomApi.kt`：`/app/v4/room/list`、`/app/v3/room/create`、`/app/v3/room/update`。

### 2.6 `mxDevice`：设备使用与分享

配网成功之后的设备能力在这里，配网过程本身在 `mxPair`。

| 能力 | 页面 |
| --- | --- |
| 设备详情、快捷详情、分享来的设备详情 | `DeviceDetailActivity`、`ShortcutDeviceDetailActivity`、`SharedDeviceDetailActivity` |
| 设备控制面板（云端 H5） | `DeviceDevicePanelActivity` + `ui/bridge/DeviceBridgeJsApi.kt` |
| 产品说明书 | `ProductUseManualActivity` |
| 设备分享 | `DeviceShareListActivity`、`DeviceShareAccountActivity`、`MineDeviceDetailActivity` |
| 恢复出厂列表、同步失败 | `DeviceRecoveryListActivity`、`DeviceSyncDataFailedActivity` |
| Matter 同步 | `DeviceSyncMatterListActivity`、`SyncMatterHelpActivity` |
| 一键替换时选择被替换设备 | `SelectReplaceDeviceActivity` |
| 中控智能列表 | `CenterControlSmartListActivity` |

设备控制页的按钮和布局多数来自云端下发的 H5 面板（`/app/v1/panel/h5/upgradeInfo`），改原生 layout **不会**改变面板里的开关样式。原生能改的是详情页外壳、分享、恢复、同步这些 Activity。

`DeviceManage` 开关（在 `EnvConfigManager.initAppConfig()` 设置）：

| 方法 | 当前值 | 含义 |
| --- | --- | --- |
| `setSupportAddDevice` | `true` | 允许添加 / 删除家庭设备。关掉后首页添加入口和房间「添加」文案会变 |
| `setSupportSetCommonly` | `true` | 设备详情允许设为常用 |
| `setSupportDeviceShare` | `true` | 单设备分享 |
| `setSupportDeviceFeedback` | `false` | 设备反馈。个人中心模块已移除，保持关闭 |

### 2.7 请求签名 AAR

对外只提供 `libs/apiCore-release.aar`，由 `coreCommon` 引用。里面两个类：

`MXApiConfigUtil`：

| 方法 | 作用 |
| --- | --- |
| `setMxAppInfo(appId, appSecret)` | 写入当前环境的 AppId、AppSecret |
| `getMXAppId()` / `getAppSecret()` | 读取上面写入的值 |
| `setToken(token)` / `getToken()` | 写入、读取登录 Token |
| `setLanguage(language)` / `getLanguage()` | 写入、读取请求语言，默认 `zh-Hans-CN` |

`HeaderInterceptor(appVersion, otherHeaders)`：OkHttp 拦截器。创建时传入应用版本号，以及可选的额外请求头。把它加进 `HttpManager.init` 的拦截器列表即可，Demo 在 `MXApplication.initHttp()` 里这样做。AppId、AppSecret、Token、语言用上面的 `MXApiConfigUtil` 设置，不需要改这个类。

---

## 3. 配网模块 `mxPair`（重点）

配网负责「发现设备 → 下发 Wi-Fi / Mesh 参数 → 绑定到当前家庭」。

### 3.1 用户能看到的页面

| 页面 | 路由 | 布局 | 做什么 |
| --- | --- | --- | --- |
| `ProductActivity` | `/page_pair/ProductActivity` | `pair_ac_product.xml` | 添加设备首页。两个 Tab：自动发现、手动添加 |
| `AutoFindFragment` | 嵌在产品页 | `pair_fr_auto_find.xml` | 扫描附近待配网设备，多选后添加 |
| `CategoryFragment` | 嵌在产品页 | `pair_fr_category.xml` | 按云端品类手动选产品 |
| `CategorySearchActivity` | `/page_pair/CategorySearchActivity` | `pair_ac_category_search.xml` | 搜索品类 / 产品 |
| `PairGuideActivity` | `/page_pair/PairGuideActivity` | `pair_ac_pair_guide.xml` | 配网引导。图、说明、按钮文案来自云端 |
| `PairGuideHelpActivity` | `/page_pair/PairGuideHelpActivity` | `pair_ac_pair_guide_help.xml` | 引导帮助大图 |
| `PairScanActivity` | `/page_pair/PairScanActivity` | `pair_ac_pair_scan.xml` | 按指定产品扫描；也用于一键替换发现新设备 |
| `PairWIFIActivity` | `/page_pair/PairWIFIActivity` | `pair_ac_pair_wifi.xml` | 填写 SSID / 密码。SSID 以 `_5G` / `-5G` 结尾会提示 |
| `PairingActivity` | `/page_pair/PairingActivity` | `pair_ac_pairing.xml` | 配网进度 |
| `PairSuccessActivity` | `/page_pair/PairSuccessActivity` | `pair_ac_pair_success.xml` | 成功后改名、选房间、设常用，然后回首页 |
| `PairFailedActivity` | `/page_pair/PairFailedActivity` | `pair_ac_pair_failed.xml` | 失败说明 |
| `ReplacePairingActivity` | `/page_pair/ReplacePairingActivity` | `pair_ac_one_key_replace.xml` | 一键替换进度 |

配网文案不在 `mxPair` 自己的 strings 里，用的是 `coreCommon` 的 `mx_provision_*`、`mx_init_device` 等。改配网文案去 `coreCommon` 三份 `strings.xml`。

配网按钮、Tab 选中色在代码里写死读取 `com.mxchip.mxapp.base.R.color.core_color_13b7f9`，例如 `ProductActivity`、`PairGuideActivity`。换品牌色改这个颜色，不要只改 `app` 模块的 `app_main_color`。

### 3.2 两条入口

```
首页 MainActivity 右上角 +
        │
        ▼
ProductActivity
  ├─ Tab「自动发现」AutoFindFragment
  │     选中设备
  │       ├─ 含 Wi-Fi 设备 → PairWIFIActivity → PairingActivity
  │       └─ 纯 Mesh      → PairingActivity
  └─ Tab「手动添加」CategoryFragment / 搜索
        → PairGuideActivity（云端引导图文）
            ├─ 普通产品 → PairScanActivity →（可选 Wi-Fi）→ PairingActivity
            └─ 扫码配网 link_type = 13
                  → 先扫网关二维码（12 位）
                  → 调恢复网关接口
                  → PairScanActivity

房间内添加（RoomInfoFragment）
  ├─ PairManager.isSupportProductCategory() == true → 同上 ProductActivity
  └─ false → 直接 PairScanActivity（不展示品类列表）

房间设备管理里的替换
  → PairGuideActivity（带 replaced.iotId）
  → PairScanActivity 单选
  →（可选 Wi-Fi）→ ReplacePairingActivity
  → 替换成功路由（默认回设备相关页）
```

`ProductActivity` 启动时如果当前没有家庭（`getUseHomeId() <= 0`）会直接关闭。配网前必须已经选中家庭。进入配网会把 `MeshSDKManage.isConnectProxy` 设为 `false`，避免和扫描抢蓝牙；回到首页 `MainActivity.onStart()` 会重新打开 proxy 连接。

### 3.3 配网类型

产品的 `link_type_id` 由云端产品信息决定，客户端按类型选 SDK。定义在 `mxPair/.../consts/PairConstants.kt` 的 `MxLinkType`。

| code | 枚举 | 行为 | 是否要 Wi-Fi |
| --- | --- | --- | --- |
| 2 | `ALI_BROADCAST` | 一键配网（枚举已定义） | 必须 |
| 3 | `ALI_TMALL` | 天猫精灵蓝牙辅助，`TMPair` | 必须 |
| 4 | `ALI_WIFI_BATCH` | Wi-Fi 批量（枚举已定义） | 必须 |
| 5 | `ALI_BLE` | 飞燕蓝牙辅助，`FYPair` | 必须 |
| 7 | `MXCHIP_MESH` | 蓝牙 Mesh，`MeshPair` | 否 |
| 10 | `MXCHIP_MESH_WIFI` | Mesh + Wi-Fi，SSID 必填 | 必须 |
| 11 | `MXCHIP_MESH_WIFI_2` | Mesh + Wi-Fi 可选，密码可空 | 展示 Wi-Fi 页，允许跳过 |
| 12 | `FOG_BLE_WIFI` | Fog 蓝牙辅助，`FogPair`，会带上 Fog API / MQTT | 必须（密码可空字符串） |
| 13 | `MX_BLE_MESH_SCAN` | 先扫码恢复网关，再走 Mesh | 否 |

`PairViewModel.starPair()` 里真正发起连接的是 3、5、7、10、11、12、13。2 和 4 只参与「要不要进 Wi-Fi 页」的判断，进入 `starPair()` 的 `else` 会记日志并失败。新增配网协议需要改 `PairViewModel` 并依赖 `link` SDK，不属于换肤范围。

云平台字段 `cloud_platform`：`1` 飞燕，`2` Fog。Mesh 配网用它选择 `LinkPlatform.PLATFORM_ALI` 或 `PLATFORM_FOG`。

Fog 设备下发的云地址不是写死在配网页里的，来自 `PairManager.setGatewayConfig()`，而这个值由 `EnvConfigManager.initHostConfig()` 根据当前环境填入。换环境后，新的 Fog 设备会连到对应的 Fog API / MQTT。

### 3.4 配网过程中客户端做的事

进度状态在 `PairState`：开始连接 → 取四元组 → 下发 Wi-Fi → 同步 → 绑定 → 成功 / 失败。

业务接口封装在 `AppPairBusiness`（实现 `IPairBusiness`），页面通过 `PairManager.setPairBusiness()` 注入。未设置时各页面会自己 new 一个 `AppPairBusiness`。客户如果要换成自己的绑定后端，实现 `IPairBusiness` 并在进配网前 `setPairBusiness()`，比改每个 Activity 干净。默认实现已经覆盖 Demo 云接口：

| 步骤 | 接口 |
| --- | --- |
| 拉品类和产品 | `GET /app/v3/category/product/info` |
| 配网引导图文 | `GET /app/v1/product/bindGuide?product_key=` |
| 分配 Mesh 地址 | `POST /app/v3/mesh/address` |
| 绑定设备 | `POST /app/v3/device/bind` |
| 配网成功改房间 / 名称 | `POST /app/v3/device/batchSet`、`GET /app/v4/room/list`、`POST /app/v3/room/create` |
| Fog 随机数 / BleKey / 连云状态 | `/app/v3/fog/device/setNet/getRandom`、`getBleKey`、`bleConfigNetworking` |
| 飞燕 cipher | `POST /awss/cipher/get` |
| 扫码配网恢复网关 | `POST /app/v3/device/restore` |

引导页上的图片、说明、确认按钮文字全部来自 `bindGuide`，后台改产品引导即可，不必发版。`hide = true` 的产品不会出现在手动品类列表，但仍会写入本地产品缓存。

扫描距离可调：`PairManager.setScanRssiThreshold(rssi)`，范围 -100 到 0。不设置则不过滤。适合客户觉得「扫到太远的设备」时收紧。

配网成功页「设为常用」由 `PairManager.setSupportSetCommonly(true)` 控制，和设备详情里的常用开关是两个开关。

### 3.5 客户改配网 UI 时改哪些文件

只换外观、文案、显隐，按下面做，不要改 `PairViewModel` 的连接分支。

| 想改什么 | 改哪里 |
| --- | --- |
| 配网各页布局、间距、图标 | `mxPair/src/main/res/layout/pair_*.xml` |
| 配网文案 | `coreCommon` 的 `values` / `values-zh-rCN` / `values-en` 里 `mx_provision_*` |
| 配网按钮、Tab 主题色 | `coreCommon/.../colors.xml` 的 `core_color_13b7f9` |
| 引导插画和步骤说明 | 云端产品 `bindGuide`，接口 `/app/v1/product/bindGuide` |
| 手动品类里展示哪些产品 | 云端品类接口；`hide=true` 的产品客户端会过滤 |
| 是否显示「手动添加」品类 | `EnvConfigManager.initAppConfig()` 里 `PairManager.setSupportProductCategory()` |
| 成功页是否显示「设为常用」 | 同文件 `PairManager.setSupportSetCommonly()` |
| 扫描范围 | 启动时调用 `PairManager.setScanRssiThreshold()` |
| 绑定接口、成功后跳哪一页 | 实现 `IPairBusiness`，替换 `AppPairBusiness` |

`setSupportProductCategory(false)` 只影响**房间内**添加设备：房间按钮会跳过产品页直接扫描。首页右上角「+」在 `MainActivity` 里仍固定打开 `ProductActivity`。如果客户希望首页也不出品类页，要同时改 `MainActivity.initEvent()` 里 `ivAddSmall` 的跳转。

---

## 4. 客户改 UI：从哪里下手

建议只动资源和应用壳，不动配网状态机和 Mesh 连接。

### 4.1 应用名、包名、图标、版本

| 想改什么 | 文件 |
| --- | --- |
| 桌面名称、应用内「智家精灵 / Smart Plus」 | `coreCommon/src/main/res/values-zh-rCN/strings.xml`、`values/strings.xml`、`values-en/strings.xml` 的 `mx_app_name`。三处一起改 |
| 商店包名 | `app/build.gradle` 的 `applicationId`（当前 `com.mxchip.mxapp`）。改完需要云端把该包名配进对应 App |
| 版本号 | 同文件 `versionName`、`versionCode` |
| Release 桌面图标 | `app/src/main/res/mipmap-*/ic_launcher_app.png`，Manifest 占位符在 `app/build.gradle` 的 release `manifestPlaceholders` |
| Debug 桌面图标 | `mipmap-*/ic_launcher_test.png` |
| 启动页 Logo | `app/src/main/res/mipmap-*/ic_splash.png`，拼进 `drawable/bg_splash.xml` |
| 登录引导 Logo | `mipmap-*/ic_login_guide_logo.png` |
| 启动页窗口背景 | `app/src/main/res/drawable/bg_splash.xml`，主题 `Theme.Launcher` 在 `app/src/main/res/values/themes.xml` |
| 登录页背景 | `app/src/main/res/drawable/bg_login.xml`。文件里写死了 `#13B7F9`，换品牌色时要一起改 |
| 空设备 / 空场景图 | `app/src/main/res/mipmap-*/ic_empty_device.png`、`ic_empty_scene.png`，夜间图在 `mipmap-night-*` |

图标目前主要在 `mipmap-xhdpi`。正式换图标时按 xhdpi / xxhdpi / xxxhdpi 补齐，避免放大模糊。

### 4.2 品牌色

品牌色现在有两处，颜色都是 `#13B7F9`，职责不同：

| 资源 | 文件 | 谁在用 |
| --- | --- | --- |
| `core_color_13b7f9` | `coreCommon/src/main/res/values/colors.xml` | 配网、账号、家庭等模块按钮和 Tab。**换肤以这个为准** |
| `app_main_color` | `app/src/main/res/values/colors.xml` | 首页壳里仍有页面直接引用，例如房间「添加」按钮 |

两处改成同一个色值。另外检查：

- `app/src/main/res/drawable/bg_login.xml` 里的 `#13B7F9`
- `app` 与 `coreCommon` 的 `values-night/colors.xml`（夜间没有单独覆盖 `core_color_13b7f9`，日间改完夜间会跟着用新色）
- 各模块 `values-night/colors.xml` 里带 `13b7f9` 后缀的颜色名，那些是「浅色某种颜色、深色变成品牌蓝」的语义色，不一定要改成新品牌色

通用背景、标题字色、分割线在 `coreCommon/.../values/colors.xml` 的 `core_color_background_*`、`core_color_text_*`。深色模式覆盖在同目录 `values-night/colors.xml`。跟随系统还是强制夜间，由 `NightModeManager` 决定，默认跟随系统。

### 4.3 页面布局按模块改

| 用户看到的界面 | 布局目录 |
| --- | --- |
| 启动、登录引导、注册、首页、房间卡片 | `app/src/main/res/layout/` |
| 验证码、密码、选国家 | `mxAccount/src/main/res/layout/account_*.xml` |
| 家庭、邀请、移交 | `mxFamily/src/main/res/layout/family_*.xml` |
| 房间管理 | `mxRoom/src/main/res/layout/room_*.xml` |
| 设备详情、分享、同步 | `mxDevice/src/main/res/layout/device_*.xml` |
| 配网 | `mxPair/src/main/res/layout/pair_*.xml` |
| 通用顶栏、通用弹窗、扫码框 | `coreCommon/src/main/res/layout/core_*.xml` |

改布局时保持 view id 不变，对应 Kotlin 里的 ViewBinding 才不用跟着改。要删控件，先搜这个 id 的点击和显隐。

设备控制面板是 H5，不在以上目录。面板皮肤在云端面板包里改。

### 4.4 文案与多语言

几乎全部业务文案在 `coreCommon`：

- `values/strings.xml`：默认（当前是英文）
- `values-zh-rCN/strings.xml`：简体中文
- `values-en/strings.xml`：英文

`app` 模块没有自己的 `strings.xml`。新增一句用户可见文案，三份文件一起加，避免某语言回落到默认英文。

请求头语言在 `MXApiConfigUtil.setLanguage()`，默认 `zh-Hans-CN`。客户端界面语言跟随系统资源，和这个请求头是两套配置。

---

## 5. 客户改环境：只改 `EnvConfigManager`

文件：`app/src/main/java/com/mxchip/mxapp/utils/EnvConfigManager.kt`。

`MXApplication.onCreate()` 调用 `initAppConfig()`，其中按已选地区调用 `initHostConfig()`，写入：

1. `AppConfigManager.setHttpHost()`：业务 API Base URL，随后 `HttpManager.init` 使用
2. `PairManager.setGatewayConfig()`：配网时下发给网关 / Fog 设备的云地址
3. `MXApiConfigUtil.setMxAppInfo()`：请求签名用的 AppId、AppSecret

### 5.1 环境与地址

Debug 包（`BuildConfig.BUILD_TYPE == "debug"`）按 MMKV 里记住的环境切换，默认测试环境。Release 包固定走生产，不受 Debug 切换影响。

中国区（`serverStation` 不是 `america` / `Singapore` 时）：

| 环境 | 常量 | 业务 HOST | Fog API | Fog MQTT |
| --- | --- | --- | --- | --- |
| 开发 | `ENV_DEV` | `https://dev-app-api-cloud.dev.mxchip.com.cn` | `api-dev.fogcloud.io` | `mqtt-dev.fogcloud.io` |
| 测试（Debug 默认） | 其他 / 未设置 | `https://sit-app-api-cloud.dev.mxchip.com.cn` | `api-dev.fogcloud.io` | `mqtt-dev.fogcloud.io` |
| 预发 | `ENV_PREV` | `https://stg-app-api-cloud.dev.mxchip.com.cn` | `app.api.fogcloud.io` | `app.mqtt.fogcloud.io` |
| 生产 | `ENV_RELEASE`，以及所有 Release 包 | `https://app.api.cloud.mxchip.com:2443` | `app.api.fogcloud.io` | `app.mqtt.fogcloud.io` |

海外：

| 地区 | 业务 HOST | Fog API | Fog MQTT |
| --- | --- | --- | --- |
| `america` | `https://api-us.mxchip.com` | `api-us.fogcloud.io` | `mqtt-us.fogcloud.io` |
| `Singapore` | `https://api-sg.mxchip.com:443` | `api-sg.fogcloud.io` | `mqtt.sg.gispacehub.com` |

地区来源：登录后保存的 ISO3。`CHN` 走中国区；其他国家读 `assets` 里的 `countrys.json`，用其中的 `server_station` 字段匹配上表。客户自有云时，改 `getHostConfig()` 里对应分支的三个 key：`HOST`、`FOG_API`、`FOG_MQTT`。三个要成套，只改 HOST 会导致设备连到另一套 Fog。

Debug 包首页会显示「当前开发 / 测试 / 预发 / 生产环境」，点击弹出切换，调用 `EnvConfigManager.switchEnv()`。这是开发便利，Release 包该控件隐藏（`MainActivity.checkViewState()`）。

### 5.2 AppId、AppSecret、备案号

同文件顶部：

| 字段 | 作用 |
| --- | --- |
| `mAppId`、`mAppSecret` | 每个环境分支里赋值，经 `MXApiConfigUtil.setMxAppInfo` 进入签名头。必须和该环境云端应用一致，否则接口验签失败 |
| `mAppRecord` | 备案号，`AppConfigManager.setAppRecord()`。当前为 ICP 文案，关于页等位置读取 `getAppRecord()` |

测试环境使用的 AppId 与开发 / 预发 / 生产不同，改地址时对照同一 `when` 分支里的 AppId，不要只改 URL。

### 5.3 功能开关（也在这个文件的 `initAppConfig`）

| 开关 | 当前值 | 效果 |
| --- | --- | --- |
| `AccountManager.setSupportSelectCountry` | `false` | 关闭选国家。不要改未被引用的 `SELECT_COUNTRY` 常量 |
| `DeviceManage.setSupportAddDevice` | `true` | 添加 / 删除家庭设备 |
| `DeviceManage.setSupportSetCommonly` | `true` | 设备详情「常用」 |
| `DeviceManage.setSupportDeviceShare` | `true` | 设备分享 |
| `DeviceManage.setSupportDeviceFeedback` | `false` | 保持关闭 |
| `PairManager.setSupportProductCategory` | `true` | 房间内添加设备是否先进入品类页 |
| `PairManager.setSupportSetCommonly` | `true` | 配网成功页「常用」 |
| `HomeManager.setSupportHomeShare` | `true` | 家庭成员邀请 |
| `HomeManager.setSupportHomeLocation` | `true` | 家庭位置 |
| `Home_Scan` | `true` | 和「允许添加设备」一起决定首页右上角「+」是否显示 |
| `Home_CreateGroup`、`Home_CreateScene` | `false` | **当前工程没有引用这两处常量**，改 true 不会出现创建群组 / 场景入口 |

`Home_Scan` 为 true 时，即使用户关掉添加设备，首页「+」仍会显示，点进去还是配网页。若要彻底隐藏添加入口，`setSupportAddDevice(false)` 且 `Home_Scan = false`，并确认 `MainActivity` 里点击逻辑符合预期。

---

## 6. 建议的客户改动顺序

1. 云端先建好应用，拿到对应环境的 AppId、AppSecret、Host、Fog API、Fog MQTT。
2. 只改 `EnvConfigManager.getHostConfig()` 和同分支的 `mAppId` / `mAppSecret`，用 Debug 包在首页切换环境验证登录和拉家庭。
3. 改 `coreCommon` 的 `mx_app_name`（三份 strings）和 `core_color_13b7f9`，同步 `app_main_color` 与 `bg_login.xml`。
4. 替换 `ic_launcher_app`、`ic_splash`、`ic_login_guide_logo`、空状态图。
5. 按模块改 layout。配网只改 `mxPair` 的 `pair_*.xml` 和 `coreCommon` 文案；引导图优先走云端 `bindGuide`。
6. 用 `initAppConfig()` 关掉不需要的分享、选国家、品类列表、家庭位置。
7. 确认 Release 包不会走 Debug 环境切换，且生产 AppId 与生产 Host 是同一套。
8. 设备面板皮肤到云端 H5 面板改，不在本仓库 layout 里。
