package com.contedevel.accountmanager_example


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
 * This is only needed if you use the sync functionality
 *
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
        // Customize this to get the userData (String key/ Value Store per account) you need
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
                    // Change the method channel name to the channel name you want to use for your sync
                    val localMethodChannel = MethodChannel(engine.dartExecutor.binaryMessenger, "com.contedevel.accountmanager_example/bgsync")
                    // Customize the data you send to flutter
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