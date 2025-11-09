# Google Sign-In Setup Guide

## 📋 Overview

This app uses Google Sign-In for authentication. To enable Google Sign-In with backend authentication, you need to configure a Web Client ID from Google Cloud Console.

## 🚀 Quick Setup

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Google Sign-In API** (also called **Google+ API**)

### Step 2: Create OAuth 2.0 Credentials

1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth client ID**
3. If prompted, configure the OAuth consent screen first
4. Select **Web application** as the application type
5. Give it a name (e.g., "WeldiWin Web Client")
6. Click **Create**
7. **Copy the Client ID** (this is your Web Client ID)

### Step 3: Configure in App

1. Open `app/src/main/res/values/strings.xml`
2. Find the `google_web_client_id` string resource (around line 33)
3. Replace the empty string with your Web Client ID:
   ```xml
   <string name="google_web_client_id">YOUR_ACTUAL_WEB_CLIENT_ID_HERE.apps.googleusercontent.com</string>
   ```
4. Save the file and rebuild the app

**Example:**
```xml
<string name="google_web_client_id">123456789-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com</string>
```

### Step 4: Verify Configuration

The app will automatically read the Web Client ID from `strings.xml`. You don't need to modify any Kotlin code.

## ⚠️ Important Notes

- **Web Client ID is required** to get the `idToken` needed for backend authentication
- Without Web Client ID, Google Sign-In will work but `idToken` will be `null`
- The Web Client ID is different from the Android Client ID
- Make sure you use the **Web application** type, not Android

## 🧪 Testing

1. After configuring the Web Client ID, rebuild the app
2. Click the "Continuer avec Google" button
3. Select a Google account
4. The app should successfully authenticate with your backend

## 🔍 Troubleshooting

### Error: "Developer error. Please check Web Client ID configuration" (Status Code 10)

This is the most common error. It means the Web Client ID is missing or incorrectly configured.

**Solutions:**
1. **Check strings.xml**: Make sure you've added your Web Client ID to `app/src/main/res/values/strings.xml`
   ```xml
   <string name="google_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```

2. **Verify the Web Client ID format**: It should end with `.apps.googleusercontent.com`
   - ✅ Correct: `123456789-abc...xyz.apps.googleusercontent.com`
   - ❌ Wrong: Android Client ID (doesn't end with .apps.googleusercontent.com)

3. **Check OAuth consent screen**: Make sure the OAuth consent screen is properly configured in Google Cloud Console

4. **Verify it's a Web application type**: 
   - Go to Google Cloud Console > APIs & Services > Credentials
   - Make sure you're using the **Web application** type, not Android
   - The Web Client ID should be different from the Android Client ID

5. **Rebuild the app**: After adding the Web Client ID, make sure to rebuild the app completely

6. **Check Logcat**: Look for log messages starting with `GoogleSignInHelper` to see what's happening

### Error: "ID token is missing"
- Make sure you've configured the Web Client ID correctly in `strings.xml`
- Check that the Web Client ID is from a **Web application** type credential
- Verify the Web Client ID format: `xxxxx.apps.googleusercontent.com`
- Rebuild the app after making changes

### Error: "Network error" (Status Code 7)
- Check your internet connection
- Make sure Google Play Services is up to date on your device

## 📚 Additional Resources

- [Google Sign-In for Android Documentation](https://developers.google.com/identity/sign-in/android/start-integrating)
- [OAuth 2.0 for Mobile & Desktop Apps](https://developers.google.com/identity/protocols/oauth2/native-app)
- [Google Cloud Console](https://console.cloud.google.com/)

