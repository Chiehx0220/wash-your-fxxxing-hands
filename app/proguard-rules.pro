# JS bridge: methods are looked up by name from the page
-keepclassmembers class io.github.chiehx0220.wash_your_fxxxing_hands.NativeStore {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
