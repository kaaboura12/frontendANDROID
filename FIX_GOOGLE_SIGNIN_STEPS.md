# 🔧 How to Fix Google Sign-In - Step by Step

## Step 1: Clean the Project

### Option A: Using Android Studio (Easiest)
1. Open Android Studio
2. Click on **Build** menu at the top
3. Click **Clean Project**
4. Wait for it to finish (you'll see "BUILD SUCCESSFUL" in the Build output)

### Option B: Using Terminal/Command Line
1. Open terminal in Android Studio (View → Tool Windows → Terminal)
2. Type this command:
   ```
   .\gradlew clean
   ```
3. Press Enter and wait for it to finish

---

## Step 2: Rebuild the Project

### Option A: Using Android Studio
1. Click on **Build** menu at the top
2. Click **Rebuild Project**
3. Wait for it to finish (this may take 2-5 minutes)
4. You'll see "BUILD SUCCESSFUL" when done

### Option B: Using Terminal
1. In the terminal, type:
   ```
   .\gradlew build
   ```
2. Press Enter and wait for it to finish

---

## Step 3: Uninstall the App from Your Device

### On Emulator:
1. Long press the app icon
2. Drag it to "Uninstall" or click the X
3. Confirm uninstall

### On Physical Device:
1. Go to Settings → Apps
2. Find your app (DAM_android)
3. Tap on it
4. Tap "Uninstall"
5. Confirm

---

## Step 4: Run the App Again

1. In Android Studio, click the green **Run** button (▶️)
2. Select your device/emulator
3. Wait for the app to install and launch

---

## Step 5: Check Logcat (To Verify It's Working)

1. In Android Studio, open **Logcat** (View → Tool Windows → Logcat)
2. In the search box, type: `GoogleSignInHelper`
3. Look for these messages:

### ✅ If it's working, you'll see:
```
✅ Web Client ID found in strings.xml: 874691787165-j3qrph4i6dcgk...
✅ Google Sign-In configured with Web Client ID: 874691787165...
✅ Full Web Client ID length: 72 characters
```

### ❌ If it's NOT working, you'll see:
```
❌ GOOGLE_CLIENT_ID is EMPTY in strings.xml!
❌ Web Client ID NOT CONFIGURED
```

---

## Step 6: Test Google Sign-In

1. Open the app
2. Click "Continuer avec Google" button
3. Select your Google account
4. It should work now! ✅

---

## 🆘 If It Still Doesn't Work

### Check 1: Verify strings.xml
- Open `app/src/main/res/values/strings.xml`
- Make sure line 31 has:
  ```xml
  <string name="GOOGLE_CLIENT_ID">874691787165-j3qrph4i6dcgk744f92t0pfsrqp0iqg0.apps.googleusercontent.com</string>
  ```

### Check 2: Invalidate Caches
1. Click **File** menu
2. Click **Invalidate Caches...**
3. Check "Invalidate and Restart"
4. Wait for Android Studio to restart
5. Then do Clean → Rebuild again

### Check 3: Check Logcat
- Share the Logcat messages with `GoogleSignInHelper` filter
- This will tell us exactly what's wrong

---

## 📝 Quick Summary

1. **Build → Clean Project**
2. **Build → Rebuild Project**
3. **Uninstall app** from device
4. **Run app** again
5. **Check Logcat** for messages
6. **Test Google Sign-In**

That's it! 🎉

