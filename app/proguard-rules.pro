# Keep JavaScript interface methods
-keepclassmembers class net.tawkit.mobile.MobileJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
