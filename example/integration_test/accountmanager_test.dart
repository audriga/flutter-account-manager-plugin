import 'dart:io';

import 'package:accountmanager/accountmanager.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  const testAccountType = 'com.contedevel.account';

  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Add account and subsequently remove it', (tester) async {
    var account = Account(name: 'testAccount', accountType:  testAccountType);
    var success = await AccountManager.addAccount(account);
    expect(success, true);

    success = await AccountManager.removeAccount(account);
    expect(success, true);
  });

  testWidgets('Account with secret properties', (widgetTester) async {
    var account = Account(name: 'testAccount', accountType:  testAccountType);
    var testPassword = 'this is a password';
    var success = await AccountManager.addAccount(
      account,
      password: testPassword,
      userdata: {
        'key1': 'value1',
        'key2': 'value2',
      },
    );
    expect(success, true);

    expect(await AccountManager.getPassword(account), testPassword);
    expect(await AccountManager.getUserData(account, 'key1'), 'value1');
    expect(await AccountManager.getUserData(account, 'key2'), 'value2');

    success = await AccountManager.removeAccount(account);
    expect(success, true);
  }, skip: !Platform.isAndroid);
  
  group('Can add and read properties', () {
    setUp(() async {
      var account = Account(name: 'testAccount', accountType:  testAccountType);
      await AccountManager.addAccount(account);
    });

    tearDown(() async {
      var account = Account(name: 'testAccount', accountType:  testAccountType);
      await AccountManager.removeAccount(account);
    });

    testWidgets('Set and get token', (widgetTester) async {
      var testTokenString = 'test token';
      AccessToken testToken = AccessToken(
          tokenType: 'Bearer', token: testTokenString);
      var account = Account(name: 'testAccount', accountType:  testAccountType);
      AccountManager.setAccessToken(account, testToken);

      var retrievedToken = await AccountManager.getAccessToken(account, 'Bearer');
      expect(retrievedToken!.token, testTokenString );
    });

    testWidgets('Add multiple accounts', (widgetTester) async {
      // Already added by setUp
      var account = Account(name: 'testAccount', accountType:  testAccountType);
      var account2 = Account(name: 'testAccount2', accountType:  testAccountType);
      await AccountManager.addAccount(account2);

      var accountList = await AccountManager.getAccounts();

      expect(accountList, containsAll([account, account2]));
    });
  });
}
