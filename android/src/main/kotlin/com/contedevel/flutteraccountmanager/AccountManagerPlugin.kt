package com.contedevel.flutteraccountmanager

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Handler
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.concurrent.TimeUnit


class AccountManagerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "accountManager")
        channel.setMethodCallHandler(this)
    }

    private fun addAccount(call: MethodCall, result: Result) {
        activity?.let {
            val accountName = call.argument<String>(ACCOUNT_NAME)
            val accountType = call.argument<String>(ACCOUNT_TYPE)
            val password = call.argument<String>(PASSWORD)
            val accountManager = AccountManager.get(it)
            val account = Account(accountName, accountType)
            val wasAdded = accountManager.addAccountExplicitly(account, password, null)
            result.success(wasAdded)
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

    private fun peekAccounts(result: Result) {
        if (activity != null) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AccountManager.newChooseAccountIntent(null, null, null, null, null, null, null)
            } else {
                AccountManager.newChooseAccountIntent(null, null, null, false, null, null, null, null)
            }
            ActivityCompat.startActivityForResult(activity!!, intent, REQUEST_CODE, null)
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun removeAccount(call: MethodCall, result: Result) {
        activity?.let {
            val accountName = call.argument<String>(ACCOUNT_NAME)
            val accountType = call.argument<String>(ACCOUNT_TYPE)
            val accountManager = AccountManager.get(it)
            val account = Account(accountName, accountType)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                val future = accountManager.removeAccount(account, {}, Handler())
                result.success(future.getResult(2, TimeUnit.SECONDS))
            } else {
                val wasRemoved = accountManager.removeAccountExplicitly(account)
                result.success(wasRemoved)
            }
        }
        result.success(false)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "addAccount" -> addAccount(call, result)
            "getAccounts" -> getAccounts(result)
            "removeAccount" -> removeAccount(call, result)
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE) {
            val account = if (resultCode == Activity.RESULT_OK && data != null) {
                hashMapOf<String, String>(
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

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        private const val REQUEST_CODE = 23
        private const val ACCOUNT_NAME = "account_name"
        private const val ACCOUNT_TYPE = "account_type"
        private const val PASSWORD = "password"

        @Suppress("UNUSED")
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "accountManager")
            channel.setMethodCallHandler(AccountManagerPlugin())
        }
    }
}