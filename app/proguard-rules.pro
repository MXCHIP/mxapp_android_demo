# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# 设置混淆的压缩比率 0 ~ 7
-optimizationpasses 5
# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames
# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共库的成员
-dontskipnonpubliclibraryclassmembers
# 混淆时不记录日志
-verbose
# 忽略警告
-ignorewarnings
# 保留注解不混淆
-keepattributes *Annotation*,InnerClasses
# 避免混淆泛型
-keepattributes Signature
# 保留代码行号，方便异常信息的追踪
-keepattributes SourceFile,LineNumberTable
# 混淆采用的算法
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# dump.txt文件列出apk包内所有class的内部结构
-dump class_files.txt
# seeds.txt文件列出未混淆的类和成员
#-printseeds seeds.txt
# usage.txt文件列出从apk中删除的代码
#-printusage unused.txt
# mapping.txt文件列出混淆前后的映射
-printmapping mapping.txt

# ----保留R文件----
-keep class **.R
-keep class **.R$* {
    <fields>;
}

#=========================
# android system
#=========================
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.preference.Preference
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService
-keep class android.support.** {*;}
-dontwarn android.support.**
-keep interface android.support.** { *; }

-keep class androidx.** {*;}
-keep interface androidx.** {*;}
-keep public class * extends androidx.**
-dontwarn androidx.**

-keep class com.google.android.material.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

#关闭 Log日志
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-keep class **.R$* {*;}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep public class * implements java.io.Serializable {*;}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
-keep public class android.net.http.SslError
-keep public class android.webkit.WebViewClient

-dontwarn android.webkit.WebView
-dontwarn android.net.http.SslError
-dontwarn android.webkit.WebViewClient

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep - Library. Keep all public and protected classes, fields, and methods.
-keep public class * {
    public protected <fields>;
    public protected <methods>;
}

# Keep names - Native method names. Keep all native class/method names.
-keepclasseswithmembers,allowshrinking class * {
    native <methods>;
}

# 不做预校验
-dontpreverify

### 忽略警告
-ignorewarnings

#如果引用了v4或者v7包
-dontwarn android.support.**
-dontwarn androidx.**

-keepattributes EnclosingMethod

## 注解支持
-keepclassmembers class *{
   void *(android.view.View);
   void *(org.json.**);
}

-keepclassmembers class * extends android.app.Activity { # 保持自定义控件类不被混淆
   public void *(android.view.View);
}

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

# 不混淆使用了 @Keep 注解相关的类
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
   @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
   @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
   @androidx.annotation.Keep <init>(...);
}


#=========================
# 各 SDK 的配置
#=========================

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-dontwarn android.databinding.**
-keep class android.databinding.** { *; }


# OkHttp3
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *;}
-dontwarn okio.**

#v4包不混淆
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }

# GCM/FCM通道
-keep class com.google.firebase.**{*;}
-dontwarn com.google.firebase.**

-dontwarn dalvik.**
-dontwarn com.tencent.smtt.**

-keep class com.tencent.smtt.** {
    *;
}

-keep class com.tencent.tbs.** {
    *;
}

-keep class me.pqpo.** {*;}

-keep class com.mxchip.log.** {*;}

-keep class com.mxchip.router.** {*;}

-keep class com.mxchip.http.response.** {*;}

-keep class com.mxchip.**.bean.** {*;}
#-keep class com.mxchip.** {*;}#//取消所有类名中包含了“model”的类及其成员的混淆

-keep class no.nordicsemi.android.mesh.**{*;}


# ------------lifecycle--------------------------------------
# Android architecture components: Lifecycle
# Android架构组件：生命周期
# LifecycleObserver's empty constructor is considered to be unused by proguard
# LifecycleObserver的空构造函数被proguard认为是未使用的
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
# ViewModel's empty constructor is considered to be unused by proguard
# ViewModel的空构造函数被proguard视为未使用
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
# keep Lifecycle State and Event enums values
# 保留生命周期状态和事件枚举值
-keepclassmembers class androidx.lifecycle.Lifecycle$State { *; }
-keepclassmembers class androidx.lifecycle.Lifecycle$Event { *; }
# keep methods annotated with @OnLifecycleEvent even if they seem to be unused
# 保持使用@OnLifecycleEvent注释的方法，即使它们似乎未被使用
# (Mostly for LiveData.LifecycleBoundObserver.onStateChange(), but who knows)
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent *;
}
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}

-keep class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class androidx.lifecycle.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# paging 分页
-dontwarn androidx.paging.DataSource
# Room 数据库
-dontwarn androidx.room.**
-keep class androidx.room.**{ *;}

# ------------mob--------------------------------------
-ignorewarnings

#关闭某个运营商功能时编译通不过可以加上下面的代码
#-dontpreverify

# for SecVerify
-keep class com.mob.**{*;}
-dontwarn com.mob.**
# for CTCC
-keep class cn.com.chinatelecom.account.**{*;}
-keep class cn.com.chinatelecom.account.api.ClientUtils{*;}
# for CUCC
-keep class com.sdk.**{*;}
# for CMCC
-keep class com.cmic.sso.sdk.**{*;}
-keep class com.unicom.xiaowo.account.shield.**{*;}

#-keep class com.alibaba.**{*;}
#-keep class com.aliyun.**{*;}
#-keep class com.taobao.**{*;}
#-keep interface com.alibaba.**{*;}
#-keep interface com.aliyun.**{*;}
#-dontwarn com.alibaba.**
#-dontwarn com.aliyun.**

#=========================
# 各 SDK 的配置
#=========================

# account.begin
-keep class com.aliyun.iot.aep.sdk.login.**{*;}
# account.end


# account-oa.begin
-keep class com.aliyun.iot.aep.sdk.login.oa.**{*;}
# account-oa.end

# open-account.begin
-keep class com.alibaba.sdk.android.*
# open-account.end

# Wireless-Security.begin
-keep class com.taobao.securityjni.**{*;}
-keep class com.taobao.wireless.security.**{*;}
-keep class com.ut.secbody.**{*;}
-keep class com.taobao.dp.**{*;}
-keep class com.alibaba.wireless.security.**{*;}
# Wireless-Security.end

# API-Client.begin
-keep public class com.aliyun.iot.aep.sdk.apiclient.** {
    public <methods>;
    public <fields>;
}
-keep public class com.aliyun.iot.aep.sdk.apiclient.request.IoTRequestBuilder {
    private static <fields>;
}
-dontwarn com.aliyun.iot.aep.sdk.base.delegate.APIGatewaySDKDelegate

-dontwarn okhttp3.OkHttpClient$Builder

# API-Client.end

# api-client-biz
-keep public class com.aliyun.alink.apiclient.biz.** {
    public <methods>;
    public <fields>;
}

# bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}


#厂商的混淆规则
-keep class android.os.SystemProperties
-dontwarn android.os.SystemProperties
-keep class com.huawei.**{*;}
-keep class com.meizu.**{*;}
-keep class com.xiaomi.**{*;}

-dontwarn com.huawei.**
-dontwarn com.meizu.**
-dontwarn com.xiaomi.**
-keep class com.mob.**{*;}
-dontwarn com.mob.**
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep class com.huawei.hms.**{*;}
-keep class com.meizu.cloud.**{*;}
-keep class com.xiaomi.mipush.sdk.**{*;}
-keep class org.apache.thrift.**{*;}
-keep class com.google.** {*;}
-keep class com.coloros.** {*;}
-dontwarn com.huawei.hms.**
-dontwarn com.meizu.cloud.**
-dontwarn com.xiaomi.mipush.sdk.**
-dontwarn org.apache.thrift.**
-dontwarn com.google.**
-dontwarn com.coloros.**

-dontwarn com.vivo.push.**
-keep class com.vivo.push.**{*; }
-keep class com.vivo.vms.**{*; }
-keep class com.mob.pushsdk.plugins.vivo.PushVivoReceiver{*;}

-keep class com.meizu.cloud.pushsdk.MzPushMessageReceiver{
public *;
}

-keep class com.mob.pushsdk.plugins.xiaomi.PushXiaoMiRevicer {*;}
-dontwarn com.xiaomi.push.**

#这是oppo的混淆规则
-keep public class * extends android.app.Service
-keep class com.heytap.msp.** { *;}
-keep class com.mob.pushsdk.plugins.oppo.** { *;}
-keep class com.shockwave.**