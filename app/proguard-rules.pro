# WebView + JS app: keep the activity and webkit; nothing custom to obfuscate-protect.
-keep class com.cricc.pro.MainActivity { *; }
-dontwarn android.webkit.**
