package com.contedevel.accountmanager_example

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        if (intent.extras?.containsKey("isAddingNewAccount") == true) {
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "accountManager/addAccountCallback").invokeMethod("addAccount", intent.extras.toString())
        }
    }
}
