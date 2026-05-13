# OkHttp Dependency Setup Guide

## ✅ Current Status

The OkHttp dependency **IS ALREADY ADDED** to your `app/build.gradle` file:

**File:** `app/build.gradle`  
**Line 62:**
```gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

---

## 🔧 If You're Getting Import Errors

If Android Studio shows "Cannot resolve symbol 'okhttp3'", follow these steps:

### **Step 1: Sync Gradle**
1. Open Android Studio
2. Click **File → Sync Project with Gradle Files**
3. Wait for sync to complete (check bottom status bar)

### **Step 2: Invalidate Caches (if sync doesn't work)**
1. Click **File → Invalidate Caches / Restart**
2. Select **Invalidate and Restart**
3. Wait for Android Studio to restart and re-index

### **Step 3: Clean and Rebuild**
1. Click **Build → Clean Project**
2. Wait for clean to finish
3. Click **Build → Rebuild Project**

---

## 📦 Alternative Dependency Formats

If the current dependency doesn't work, try these alternatives:

### **Option 1: Use Latest Stable Version**
```gradle
dependencies {
    // OkHttp for HTTP requests (WhatsApp webhook)
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

### **Option 2: Use Version Catalog (Recommended for modern projects)**

**Step 1:** Add to `gradle/libs.versions.toml`:
```toml
[versions]
okhttp = "4.12.0"

[libraries]
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
```

**Step 2:** Use in `app/build.gradle`:
```gradle
dependencies {
    implementation libs.okhttp
}
```

### **Option 3: Use Older Stable Version (if 4.12.0 has issues)**
```gradle
dependencies {
    // OkHttp for HTTP requests (WhatsApp webhook)
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
}
```

---

## 🔍 Verify Dependency is Downloaded

### **Method 1: Check External Libraries**
1. In Android Studio, expand **External Libraries** in Project view
2. Look for **okhttp-4.12.0**
3. If you see it, the dependency is properly downloaded

### **Method 2: Check Gradle Console**
1. Click **Build → Make Project**
2. Check the Build output window
3. Look for any errors related to OkHttp

### **Method 3: Check build folder**
Navigate to:
```
app/build/intermediates/
```
If build succeeds, dependency is working.

---

## 📝 Complete app/build.gradle File

Here's what your complete dependencies block should look like:

```gradle
dependencies {
    // Core AndroidX
    implementation libs.appcompat
    implementation libs.material
    implementation libs.activity
    implementation libs.constraintlayout

    // RecyclerView + CardView
    implementation libs.recyclerview
    implementation libs.cardview

    // ✅ Room Database
    def room_version = "2.6.1"
    implementation libs.room.runtime
    annotationProcessor "androidx.room:room-compiler:$room_version"

    // ✅ Lifecycle components
    def lifecycle_version = "2.7.0"
    implementation libs.lifecycle.viewmodel
    implementation libs.lifecycle.livedata

    // ✅ Firebase Dependencies (latest stable versions)
    implementation platform('com.google.firebase:firebase-bom:33.5.1')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-database'
    implementation 'com.google.firebase:firebase-analytics'

    // ✅ OkHttp for HTTP requests (WhatsApp webhook)
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'

    // ✅ Unit & Instrumentation Tests
    testImplementation libs.junit
    androidTestImplementation libs.ext.junit
    androidTestImplementation libs.espresso.core
}
```

---

## 🚨 Common Issues & Solutions

### **Issue 1: "Cannot resolve symbol 'okhttp3'"**
**Solution:**
1. Sync Gradle (File → Sync Project with Gradle Files)
2. If still not working, check internet connection
3. Try invalidating caches (File → Invalidate Caches / Restart)

### **Issue 2: "Failed to resolve: okhttp3"**
**Solution:**
1. Check your internet connection
2. Verify `mavenCentral()` is in repositories (settings.gradle line 18)
3. Try using a VPN if you're in a region with restricted access

### **Issue 3: Build fails with dependency conflict**
**Solution:**
Add this to your `app/build.gradle` inside `android {}` block:
```gradle
android {
    // ... other config ...
    
    packagingOptions {
        resources {
            excludes += ['META-INF/DEPENDENCIES']
        }
    }
}
```

### **Issue 4: "Duplicate class" error**
**Solution:**
Make sure you don't have multiple OkHttp dependencies. Check for:
```gradle
// Remove if you have these:
implementation 'com.squareup.okhttp:okhttp:2.x.x'  // Old version
implementation 'com.squareup.okhttp3:okhttp:3.x.x' // Old version
```

---

## ✅ Verification Checklist

After adding the dependency, verify:

- [ ] `app/build.gradle` has `implementation 'com.squareup.okhttp3:okhttp:4.12.0'`
- [ ] Gradle sync completed successfully
- [ ] No red underlines in `WhatsAppUtils.java` on `import okhttp3.*`
- [ ] Project builds without errors
- [ ] External Libraries shows `okhttp-4.12.0`

---

## 🔗 Import Statements Required

In `WhatsAppUtils.java`, you should have these imports:

```java
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
```

If these imports show errors after Gradle sync, there's a dependency issue.

---

## 📞 Still Having Issues?

If you're still getting errors after following all steps:

1. **Share the exact error message** from Build output
2. **Check Gradle version**: File → Project Structure → Project
   - Gradle Version should be 8.0 or higher
   - Android Gradle Plugin should be 8.0 or higher

3. **Try offline mode OFF**: 
   - File → Settings → Build, Execution, Deployment → Gradle
   - Uncheck "Offline work"

4. **Clear Gradle cache**:
   ```
   Delete folder: C:\Users\YourUsername\.gradle\caches
   Then sync again
   ```

---

## 🎯 Quick Fix Command

If you prefer command line, run this in project root:

```bash
# Windows PowerShell
./gradlew clean
./gradlew build --refresh-dependencies
```

This will force download all dependencies fresh.
