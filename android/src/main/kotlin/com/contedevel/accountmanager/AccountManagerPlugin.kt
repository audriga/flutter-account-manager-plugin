package com.contedevel.accountmanager

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.os.*
import android.provider.ContactsContract
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry


class AccountManagerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "accountManager")
        channel.setMethodCallHandler(this)
    }


    private fun addAccount(call: MethodCall, result: Result) {
        activity?.let {
            val accountName = call.argument<String>(ACCOUNT_NAME)
            val accountType = call.argument<String>(ACCOUNT_TYPE)
            val accountPassword = call.argument<String?>(ACCOUNT_PASSWORD)
            val accountUserData = call.argument<HashMap<String,String>?>(ACCOUNT_USERDATA)
            val contentAuthorities = call.argument<List<String>?>(SYNC_CONTENT_AUTHORITIES)
            val accountManager = AccountManager.get(it)
            val account = Account(accountName, accountType)

            var accountUserDataBundle: Bundle? = null
            if (accountUserData != null) {
                accountUserDataBundle = Bundle()
                for (entry in accountUserData) {
                    accountUserDataBundle.putString(entry.key, entry.value)
                }
            }
            /**
             * From the AccountManager Doc:
             * Params:
             * account The {@link Account} to add
             * password The password to associate with the account, null for none
             * userdata String values to use for the account's userdata, null for
             *            none
             * @return True if the account was successfully added, false if the account
             *         already exists, the account is null, the user is locked, or another error occurs.
             */
            val wasAdded = accountManager.addAccountExplicitly(account, accountPassword, accountUserDataBundle)

            if (wasAdded && contentAuthorities != null) {
                for (authority in contentAuthorities) {
                    ContentResolver.setIsSyncable(account, authority, 1)
                    ContentResolver.setSyncAutomatically(account, authority, true)
                }
            }

            result.success(wasAdded)
        }
    }

    private fun setIsSyncable(call: MethodCall, result: Result) {
        val accountName = call.argument<String>(ACCOUNT_NAME)
        val accountType = call.argument<String>(ACCOUNT_TYPE)
        val contentAuthority = call.argument<String>(SYNC_CONTENT_AUTHORITY)
        val value = if  (call.argument<Boolean>(BOOL_VALUE) == true) 1 else 0
        val account = Account(accountName, accountType)
        ContentResolver.setIsSyncable(account, contentAuthority, value)
        result.success(ContentResolver.getIsSyncable(account, contentAuthority) == value)
    }
    private fun setSyncAutomatically(call: MethodCall, result: Result) {
        val accountName = call.argument<String>(ACCOUNT_NAME)
        val accountType = call.argument<String>(ACCOUNT_TYPE)
        val contentAuthority = call.argument<String>(SYNC_CONTENT_AUTHORITY)
        val value = call.argument<Boolean>(BOOL_VALUE) == true
        val account = Account(accountName, accountType)
        ContentResolver.setSyncAutomatically(account, contentAuthority, value)
        result.success(ContentResolver.getSyncAutomatically(account, contentAuthority) == value)
    }

    private fun getPassword(call: MethodCall, result: Result) {
        val accountName = call.argument<String>(ACCOUNT_NAME)
        val accountType = call.argument<String>(ACCOUNT_TYPE)
        activity?.let {
            val accountManager = AccountManager.get(activity)
            val account =
                accountManager.accounts.firstOrNull { account -> (account.type == accountType && account.name == accountName) }
            if (account != null) {
                val password = accountManager.getPassword(account)
                if (password != null) {
                    result.success(password)
                } else {
                    result.error("no_password", "Could not get password for given account", null)
                }
            } else {
                result.error("no_match", "Found no accounts matching the arguments", null)
            }
        }
    }
    private fun getUserData(call: MethodCall, result: Result) {
        val accountName = call.argument<String>(ACCOUNT_NAME)
        val accountType = call.argument<String>(ACCOUNT_TYPE)
        val key = call.argument<String>(KEY)
        if (key == null) {
            result.error("no_key", "Key given was null", null)
            return;
        }
        activity?.let {
            val accountManager = AccountManager.get(activity)
            val account =
                accountManager.accounts.firstOrNull { account -> (account.type == accountType && account.name == accountName) }
            if (account != null) {
                val userData = accountManager.getUserData(account, key)
                if (userData != null) {
                    result.success(userData)
                } else {
                    result.error("no_data", "Could not get data for given account and key", null)
                }
            } else {
                result.error("no_match", "Found no accounts matching the arguments", null)
            }
        }
    }

    private fun setAccessToken(call: MethodCall, result: Result) {
        activity?.let {
            val accountName = call.argument<String>(ACCOUNT_NAME)
            val accountType = call.argument<String>(ACCOUNT_TYPE)
            val authTokenType = call.argument<String>(AUTH_TOKEN_TYPE)
            val accessToken = call.argument<String>(ACCESS_TOKEN)
            val account = Account(accountName, accountType)
            val accountManager = AccountManager.get(it)
            accountManager.setAuthToken(account, authTokenType, accessToken)
            result.success(true)
        }
    }

    private fun getAccounts(result: Result) {
        activity?.let {
            val accounts = AccountManager.get(activity).accounts
            val list = mutableListOf<HashMap<String, String>>()
            for (account in accounts) {
                list.add(hashMapOf(
                    ACCOUNT_NAME to account.name,
                    ACCOUNT_TYPE to account.type
                ))
            }
            result.success(list)
        }
    }

    private fun getAccessToken(call: MethodCall, result: Result) {
        activity?.let {
            val accountName = call.argument<String>(ACCOUNT_NAME)
            val accountType = call.argument<String>(ACCOUNT_TYPE)
            val authTokenType = call.argument<String>(AUTH_TOKEN_TYPE)
            val account = Account(accountName, accountType)
            AccountManager.get(activity).getAuthToken(account, authTokenType, null, false,
                { data ->
                    val bundle: Bundle = data.result
                    bundle.getString(AccountManager.KEY_AUTHTOKEN)?.let { token ->
                        result.success(hashMapOf(
                            AUTH_TOKEN_TYPE to authTokenType,
                            ACCESS_TOKEN to token
                        ))
                    }
                },
                Handler(Looper.getMainLooper()) {
                    result.success(null)
                    true
                }
            )
        }
    }

//    private fun peekAccounts(result: Result) {
//        if (activity != null) {
//            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                AccountManager.newChooseAccountIntent(null, null, null, null, null, null, null)
//            } else {
//                AccountManager.newChooseAccountIntent(null, null, null, false, null, null, null, null)
//            }
//            ActivityCompat.startActivityForResult(activity!!, intent, REQUEST_CODE, null)
//            result.success(true)
//        } else {
//            result.success(false)
//        }
//    }

    private fun removeAccount(call: MethodCall, result: Result) {
        if (activity != null) {
            val accountName = call.argument<String>(ACCOUNT_NAME)
            val accountType = call.argument<String>(ACCOUNT_TYPE)
            val accountManager = AccountManager.get(activity)
            val account = Account(accountName, accountType)
            val wasRemoved = accountManager.removeAccountExplicitly(account)
            result.success(wasRemoved)
        } else {
            result.success(false)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "addAccount" -> addAccount(call, result)
            "getAccounts" -> getAccounts(result)
            "getAccessToken" -> getAccessToken(call, result)
            "removeAccount" -> removeAccount(call, result)
            "setAccessToken" -> setAccessToken(call, result)
            "getPassword" -> getPassword(call, result)
            "getUserData" -> getUserData(call, result)
            "setIsSyncable" -> setIsSyncable(call, result)
            "setSyncAutomatically" -> setSyncAutomatically(call, result)
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE) {
            val account = if (resultCode == Activity.RESULT_OK && data != null) {
                hashMapOf(
                    "NAME" to data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),
                    "TYPE" to data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
                )
            } else null
            channel.invokeMethod("onAccountPicked", account)
            return true
        }
        return false
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    companion object {
        private const val REQUEST_CODE = 23
        private const val ACCOUNT_NAME = "account_name"
        private const val ACCOUNT_TYPE = "account_type"
        private const val ACCOUNT_PASSWORD = "account_password"
        private const val ACCOUNT_USERDATA = "account_userdata"
        private const val AUTH_TOKEN_TYPE = "auth_token_type"
        private const val ACCESS_TOKEN = "access_token"
        private const val KEY = "key"
        private const val SYNC_CONTENT_AUTHORITY = "sync_content_authority"
        private const val SYNC_CONTENT_AUTHORITIES = "sync_content_authorities"
        private const val BOOL_VALUE = "bool_value";
    }
}
