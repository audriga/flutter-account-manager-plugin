import 'dart:async';

import 'package:flutter/services.dart';

class Account {
  String name;
  String accountType;

  Account({
    required this.name,
    required this.accountType,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Account &&
          name == other.name &&
          accountType == other.accountType;

  @override
  int get hashCode => name.hashCode ^ accountType.hashCode;
}

class AccessToken {
  String tokenType;
  String token;

  AccessToken({
    required this.tokenType,
    required this.token
  });
}

/// Represents some APIs from Android account manager and emulates it on iOS
/// platform
class AccountManager {
  static const String ContactsAuthority = "com.android.contacts";
  static const String CalendarAuthority = "com.android.calendar";
  
  static const MethodChannel _channel = const MethodChannel('accountManager');

  static const String _KeyAccountName = 'account_name';
  static const String _KeyAccountType = 'account_type';
  static const String _KeyAccountPassword = "account_password";
  static const String _KeyAccountUserdata = "account_userdata";
  static const String _KeyAuthTokenType = 'auth_token_type';
  static const String _KeyAccessToken = 'access_token';
  static const String _KeyKey = 'key';
  static const String _Sync_Content_Authority = 'sync_content_authority';
  static const String _KeyBoolValue = 'bool_value';
  static const String _Sync_Content_Authorities = 'sync_content_authorities';


  /// Set the callback to be executed when the app gets opened (by settings app) with an AddAccount Intent
  static void setAddAccountCallback(Future<dynamic> Function(MethodCall call)? handler){
    const platform = MethodChannel('accountManager/addAccountCallback');
    platform.setMethodCallHandler(handler);
  }

  /// Adds the [account] to the account manager on Android and user preferences
  /// on iOS
  /// The following options are android-only:
  /// [password] if given, saves the password to the user account. It can later be retrieved using [getPassword]
  /// [userdata] arbitrary string map to associate with the account
  /// If the account should be synced add the [authorities] of the corresponding content providers
  /// For example [ContactsAuthority] or [CalendarAuthority].
  static Future<bool> addAccount(Account account, {
    String? password,
    Map<String, String>? userdata,
    List<String>? authorities,
  }) async {
    return await _channel.invokeMethod('addAccount', {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType,
      _KeyAccountPassword: password,
      _KeyAccountUserdata: userdata,
      _Sync_Content_Authorities: authorities,
    });
  }

  /// Gets the password of [account]. Note: only works for accounts the app "owns"
  static Future<String> getPassword(Account account) async {
    final ret = await _channel.invokeMethod<String>('getPassword', {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType
    });
    if (ret != null) {
      return ret;
    } else {
      return Future.error('Password is null');
    }
  }

  /// Gets a userdata string of [account] to the given [key]
  /// (Userdata is a key-value String map that can be queried but it is not possible to get the entire map)
  /// Note: only works for accounts the app "owns"
  static Future<String?> getUserData(Account account, String key) {
    return _channel.invokeMethod<String>('getUserData', {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType,
      _KeyKey: key
    });
  }

  /// Returns an access token by [account] and [authTokenType]
  static Future<AccessToken?> getAccessToken(Account account,
      String authTokenType) async {
    AccessToken? accessToken;
    final res = await _channel.invokeMethod('getAccessToken', {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType,
      _KeyAuthTokenType: authTokenType
    });
    if (res != null) {
      accessToken = AccessToken(
        tokenType: res[_KeyAuthTokenType],
        token: res[_KeyAccessToken],
      );
    }
    return accessToken;
  }

  /// Returns a list of accounts
  static Future<List<Account>> getAccounts() async {
    List<Account> accounts = [];
    final result = await _channel.invokeMethod('getAccounts');
    for (var item in result) {
      accounts.add(
          new Account(
              name: item[_KeyAccountName],
              accountType: item[_KeyAccountType]
          )
      );
    }
    return accounts;
  }

  /// Deletes the [account]
  static Future<bool> removeAccount(Account account) async {
    return await _channel.invokeMethod('removeAccount', {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType
    });
  }

  /// Saves the access [token] for the passed [account]
  static Future<bool> setAccessToken(Account account, AccessToken? token
      ) async {
    final data = {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType
    };
    if (token != null) {
      data[_KeyAuthTokenType] = token.tokenType;
      data[_KeyAccessToken] = token.token;
    }
    return await _channel.invokeMethod('setAccessToken', data);
  }

  /// Set whether [account] is syncable for the given [authority].
  /// [account] – the account whose setting we are querying
  /// [authority] – the provider whose behavior is being controlled
  /// return true if successful
  static Future<bool> setIsSyncable(Account account, String authority, bool syncable) async {
    final data = {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType,
      _Sync_Content_Authority: authority,
      _KeyBoolValue: syncable
    };
    return await _channel.invokeMethod('setIsSyncable', data);
  }

  /// Set whether the provider is synced when it received a network tickle.
  /// [account] – the account whose setting we are querying
  /// [authority] – the provider whose behavior is being controlled
  /// return true if successful
  static Future<bool> setSyncAutomatically(Account account, String authority, bool sync) async {
    final data = {
      _KeyAccountName: account.name,
      _KeyAccountType: account.accountType,
      _Sync_Content_Authority: authority,
      _KeyBoolValue: sync
    };
    return await _channel.invokeMethod('setSyncAutomatically', data);
  }
}
