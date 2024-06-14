# Account manager

Account manager for Flutter. Fork of https://github.com/Klein-Stein/flutter-account-manager-plugin

We recommend familiarizing yourself with the corresponding Android concept: https://developer.android.com/reference/android/accounts/AccountManager

## Features

### Android

- Add/ remove accounts to the Android System
- Set and get access token for an account the app owns
- Set and get secret userdata (Map) for an account the app owns (Set only at account creation time)
- Set and get password for an account the app owns (Set only at account creation time)
- Hook for adding an account via the settings (setAddAccountCallback)
- Sync features [Requires Additional Setup]
  - Can set whether an account is syncable for a given autority (=  Content Provider Authorities, such as for contact or calender sync)
  - Can also set whether the synced is performed automatically
  - User can set callback function that will be called periodically by android system

### iOS

Unchanged from original project, currently limited featureset.

**WARNING:** iOS doesn't provide AccountManager entity, the plugin emulates it using `UserDefaults.standard` to store all data.



## Supported platforms:

- Android 8.1+ (API 27+)
- iOS 12+



[TOC]



## Getting Started

### Installation

Add this package to your dependencies in **pubspec.yaml**. On Android and iOS devices you also need to request permissions at the runtime. We advice to 
use [permission_handler](https://pub.dev/packages/permission_handler).

```yaml
dependencies:
  accountmanager:
    git:
      url: https://github.com/audriga/flutter-account-manager-plugin.git
      ref: master
  permission_handler: ^10.2.0
```

And call `flutter pub get` to download new dependencies

### Usage

To import module add `import 'package:accountmanager/accountmanager.dart';` at the 
import block in your code.

The Class `Account` represents one Account. On Android the combination of Account Name and Account Type uniquely identifies an account.

See the Functions in lib/accountmanager.dart for documentation on the individual API functions.

### Required Setup

To allow the plugin to manage accounts you will need to add at least one custom account type, by implementing a corresponding authenticator. You will also need to set the corresponding permissions in AndroidManifest.xml.

 You can do so by adding the following boilerplate native code for Android:  

1. Create **xml/authenticator.xml** resource in your Android project folder with next content:  
```xml
<?xml version="1.0" encoding="utf-8"?>
<account-authenticator
     xmlns:android="http://schemas.android.com/apk/res/android"
     android:accountType="<YOUR_ACCOUNT_TYPE>"
     android:icon="<YOUR_ICON_48DP>"
     android:smallIcon="<YOUR_ICON_24DP>"
     android:label="<YOUR_ACCOUNT_LABEL>"/>
```
2. Implement `AbstractAccountAuthenticator` stub (see also https://developer.android.com/training/sync-adapters/creating-authenticator):  

**Authenticator.kt**
```kotlin
class Authenticator(private val mContext: Context) // Simple constructor
    : AbstractAccountAuthenticator(mContext) {

    // Editing properties is not supported
    override fun editProperties(r: AccountAuthenticatorResponse, s: String): Bundle {
        throw UnsupportedOperationException()
    }

    /**
    // Use this if you don't want to support adding accounts from Settings
    @Throws(NetworkErrorException::class)
    override fun addAccount(
        r: AccountAuthenticatorResponse,
        s: String,
        s2: String,
        strings: Array<String>,
        bundle: Bundle
    ): Bundle?  = null
    */


    // If you want to support adding accounts from the settings menu, actually implement this method, like so
    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle
    ): Bundle {
        
        val b = Bundle()
        val intent = Intent(mContext, MainActivity::class.java)
        intent.putExtra("accountType", accountType)
        intent.putExtra("authTokenType", authTokenType)
        intent.putExtra("isAddingNewAccount", true)
        b.putParcelable(AccountManager.KEY_INTENT, intent)
        return b
    }

    // Ignore attempts to confirm credentials
    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        options: Bundle
    ): Bundle?  = null

    // Getting an authentication token is not supported
    @Throws(NetworkErrorException::class)
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle
    ): Bundle {
        throw UnsupportedOperationException()
    }

    // Getting a label for the auth token is not supported
    override fun getAuthTokenLabel(authTokenType: String): String {
        // return "$authTokenType (Label)";
        throw UnsupportedOperationException()
    }

    // Updating user credentials is not supported
    @Throws(NetworkErrorException::class)
    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle
    ): Bundle {
        throw UnsupportedOperationException()
    }

    // Checking features for the account is not supported
    @Throws(NetworkErrorException::class)
    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account,
        features: Array<String>
    ): Bundle {
        throw UnsupportedOperationException()
    }
}
```

**AuthenticatorService.kt**
```kotlin
/**
 * A bound Service that instantiates the authenticator
 * when started.
 */
class AuthenticatorService : Service() {

    // Instance field that stores the authenticator object
    private lateinit var mAuthenticator: Authenticator

    override fun onCreate() {
        // Create a new authenticator object
        mAuthenticator = Authenticator(this)
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    override fun onBind(intent: Intent?): IBinder = mAuthenticator.iBinder
}
```

3. Update **AndroidManifest.xml**:

Add required permissions, i.e.:  
```xml
<uses-permission android:name="android.permission.AUTHENTICATE_ACCOUNTS"/>
<uses-permission android:name="android.permission.GET_ACCOUNTS"/>
<uses-permission android:name="android.permission.MANAGE_ACCOUNTS"/>
<uses-permission android:name="android.permission.READ_SYNC_SETTINGS"/>
<uses-permission android:name="android.permission.WRITE_SYNC_SETTINGS" />
```
And register `AuthenticatorService`:
```xml
<service android:name=".AuthenticatorService" android:exported="false">
    <intent-filter>
        <action android:name="android.accounts.AccountAuthenticator"/>
    </intent-filter>
    <meta-data
        android:name="android.accounts.AccountAuthenticator"
        android:resource="@xml/authenticator"/>
</service>
```

More details about Android authentication system you can find on 
[Android Developers resource](https://developer.android.com/training/id-auth/custom_auth).

### Optional Steps

#### Enable Sync for the account
This follows https://developer.android.com/training/sync-adapters/
You have already created a (stub) authenticator and if you plan on syncing contacts or calendars, use their corresponding content providers and skip the (stub) content provider step.
The following assumes you want to sync contacts, swap in the calendar or your custom content provider where necessary if you want to sync those things instead.
(To sync multiple content types it is possible to create multiple sync adapters, see https://stackoverflow.com/a/31161106).

Create **SyncAdapter.kt**

```kotlin
package com.example.yourapp

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.plugin.common.MethodChannel

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
class SyncAdapter @JvmOverloads constructor(
    context: Context,
    autoInitialize: Boolean,
    /**
     * Using a default argument along with @JvmOverloads
     * generates constructor for both method signatures to maintain compatibility
     * with Android 3.0 and later platform versions
     */
    allowParallelSyncs: Boolean = false,
    /*
     * If your app uses a content resolver, get an instance of it
     * from the incoming Context
     */
    val mContentResolver: ContentResolver = context.contentResolver
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {

    /*
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    override fun onPerformSync(
        account: Account,
        extras: Bundle,
        authority: String,
        provider: ContentProviderClient, // This is the actual content provider, this code currently doesn't use it, but rather the flutter code performing the sync will then get a new one.
        syncResult: SyncResult
    ) {
        /*
         * Put the data transfer code here.
         */

        val accountManager = AccountManager.get(context)
        // Gets the password of the account to be synced, you could also use a token instead
        val password = accountManager.getPassword(account)
        // TODO Customize this to get the userData (String key/ Value Store per account) you need
        val userData = accountManager.getUserData(account, "key2")

        // From https://stackoverflow.com/a/76153521
        // Get a handler that can be used to post to the main thread
        val mainHandler = Handler(Looper.getMainLooper())
        val myRunnable = Runnable() {
            run() {
                val engine = FlutterEngine(context)
                val flutterLoader: FlutterLoader = FlutterInjector.instance().flutterLoader()
                if (!flutterLoader.initialized()) {
                    flutterLoader.startInitialization(context)
                }
                flutterLoader.ensureInitializationCompleteAsync(context,null,Handler(Looper.getMainLooper())) {
                    val entryPoint = DartExecutor.DartEntrypoint(flutterLoader.findAppBundlePath(), "backgroundServiceCallback")
                    engine.dartExecutor.executeDartEntrypoint(entryPoint)
                    // TODO Change the method channel name to the channel name you want to use for your sync
                    val localMethodChannel = MethodChannel(engine.dartExecutor.binaryMessenger, "com.example.yourapp/bgsync")
                    // TODO Customize the data you send to flutter
                    val dartArgs = hashMapOf(
                        "NAME" to account.name,
                        "TYPE" to account.type,
                        "EXTRAS" to extras.toString(),
                        "AUTHORITY" to authority,
                        "PROVIDER" to provider.toString(),
                        "SYNCRESULT" to syncResult.toString(),
                        "PASSWORD" to password,
                        "USERDATA_KEY2" to userData,
                    )
                    localMethodChannel.invokeMethod("performSync", dartArgs)
                }
            }
        }
        mainHandler.post(myRunnable)
        return
    }

}
```

create **SyncService.kt** 
```kotlin
package com.example.yourapp

import android.app.Service
import android.content.Intent
import android.os.IBinder

// This binds the SyncAdapter to the framework
/**
 * Define a Service that returns an [android.os.IBinder] for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
class SyncService : Service() {
    /*
     * Instantiate the sync adapter object.
     */
    override fun onCreate() {
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized(sSyncAdapterLock) {
            sSyncAdapter = sSyncAdapter ?: SyncAdapter(applicationContext, true)
        }
    }

    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     *
     */
    override fun onBind(intent: Intent): IBinder {
        /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         *
         * We should never be in a position where this is called before
         * onCreate() so the exception should never be thrown
         */
        return sSyncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        // Storage for an instance of the sync adapter
        private var sSyncAdapter: SyncAdapter? = null
        // Object to use as a thread-safe lock
        private val sSyncAdapterLock = Any()
    }
}

```
Sync Adapter Metadata file **xml/syncadapter.xml**
```xml
<sync-adapter xmlns:android="http://schemas.android.com/apk/res/android"
    android:contentAuthority="com.android.contacts"
    android:accountType="<YOUR_ACCOUNT_TYPE>"
    android:userVisible="true"
    android:allowParallelSyncs="true"
    android:supportsUploading="true" />

    <!-- TODO Swap the content provider string to what you need, like "com.android.calendar" or your custom authority, so it would read android:contentAuthority="com.domain.yourapp.provider"-->
```
Declare sync adapter in **AndroidManifest.xml**
```xml
<service 
    android:name=".SyncService"
    android:exported="true"
    android:process=":sync">
    <intent-filter>
        <action android:name="android.content.SyncAdapter"/>
    </intent-filter>
    <meta-data android:name="android.content.SyncAdapter"
        android:resource="@xml/syncadapter" />
    <!-- TODO if you sync contacts you have to create and link a structure for see https://developer.android.com/guide/topics/providers/contacts-provider#ContactsFile and https://github.com/bitfireAT/davx5-ose/blob/dev-ose/app/src/main/res/xml/contacts.xml-->
    <meta-data
        android:name="android.provider.CONTACTS_STRUCTURE"
        android:resource="@xml/contacts"/>
</service>
```

and if you use an existing content provider like contacts or calendar add permission for that content provider
```xml
<uses-permission android:name="android.permission.READ_CONTACTS"/>
<uses-permission android:name="android.permission.WRITE_CONTACTS"/>
```

When adding the account via `AccountManager.addAccount` don't forget to specify the authority of the content provider you are syncing.

And below your main function add

```dart

@pragma('vm:entry-point')
void backgroundServiceCallback() async {
  WidgetsFlutterBinding.ensureInitialized();
  const platform = MethodChannel('com.example.yourapp/bgsync');
  platform.setMethodCallHandler((call) async {
    // TODO Place the code that should be called on sync here
  });
}
```





#### Add Account from Settings
In order to enable adding an account from settings, add the following code snippet to your **MainActivity.kt** in the `configureFlutterEngine` function
```kotlin
override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    if (intent.extras?.containsKey("isAddingNewAccount") == true) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "accountManager/addAccountCallback").invokeMethod("addAccount", intent.extras.toString())
    }
}
```
And early in your **main.dart** (for example in an overridden `initState` of your homepage) add a callback of what is supposed to happen when the app gets opened by an "add Account" context.
```dart
AccountManager.setAddAccountCallback((call) {
  // Add your custom callback code here, for example
  return Navigator.push(context, MaterialPageRoute(builder: (context) {
    return const AccountService();
  }),);
  // end example
});
```

#### Preference Screen
If you want users to be able to get to your app (Ideally the account settings route) you can add the line `android:accountPreferences="@xml/sync_prefs"` to `account-authenticator` in **xml/authenticator.xml**
And create **xml/sync_prefs.xml** with the following content:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.preference.PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
    <androidx.preference.PreferenceScreen android:title="<Manage_My_Account_Cutstom string>">
        <intent
            android:targetPackage="<Your_App_Base_Package>"
            android:targetClass="<Your_App_Base_Package>.MainActivity"
            android:action="ACTION_VIEW" />
    </androidx.preference.PreferenceScreen>
</androidx.preference.PreferenceScreen>
```
you will also need to add `implementation "androidx.preference:preference-ktx:1.2.1"` to the dependencies of your **android/app/build.gradle**.