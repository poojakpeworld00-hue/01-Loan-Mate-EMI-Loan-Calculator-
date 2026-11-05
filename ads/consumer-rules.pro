# Custom-ad cards are stored as JSON (Gson) in the vault: keep field names and the TypeToken signature.
-keep class com.fffskintools.skin.tools.ads.entity.InhouseSpotSlab { *; }
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
