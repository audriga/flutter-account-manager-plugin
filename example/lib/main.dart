import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:accountmanager/accountmanager.dart';


class HomeWidget extends StatefulWidget {
  static const kAccountType = 'com.contedevel.account';

  @override
  State<HomeWidget> createState() => _HomeWidgetState();
}

class _HomeWidgetState extends State<HomeWidget> {
  bool openedFromSettings = false;

  @override
  initState(){
    super.initState();

    if (Platform.isAndroid) {
      AccountManager.setAddAccountCallback((call) {
        // Add your custom callback code here, for example
        setState(() {
          openedFromSettings = true;
        });
        return Future.value(null);
      });
    }
  }

  Future<String> fetchName() async {
    String name = '';
    if (await Permission.contacts.request().isGranted) {
      try {
        var accounts = await AccountManager.getAccounts();
        for (Account account in accounts) {
          if (account.accountType == HomeWidget.kAccountType) {
            await AccountManager.removeAccount(account);
          }
        }
        var account = new Account(name: 'User 007', accountType: HomeWidget.kAccountType);
        late bool success;
        if (Platform.isAndroid) {
          success = await AccountManager.addAccount(account, authorities: [AccountManager.ContactsAuthority]);
        } else {
          success =  await AccountManager.addAccount(account);
        }
        if (success) {
          AccessToken? token = new AccessToken(
              tokenType: 'Bearer',
              token: 'Blah-Blah code'
          );
          await AccountManager.setAccessToken(account, token);
          accounts = await AccountManager.getAccounts();
          for (Account account in accounts) {
            if (account.accountType == HomeWidget.kAccountType) {
              token = await AccountManager.getAccessToken(account,
                  token!.tokenType);
              if (token != null) {
                name = account.name + ' - ' + token.token;
              }
              break;
            }
          }
        }
      } catch(e, s) {
        print(e);
        print(s);
      }
    }
    return name;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text('Account manager'),
          backgroundColor: openedFromSettings? Colors.red : null,
        ),
        body: Center(
          child: FutureBuilder<String>(
            future: fetchName(),
            builder: (BuildContext context, AsyncSnapshot<String> snapshot) {
              String text = 'Loading...';
              if (snapshot.connectionState == ConnectionState.done) {
                if (snapshot.hasData && snapshot.data != null) {
                  text = snapshot.data!;
                } else {
                  text = 'Failed';
                }
              }
              return Text(text);
            },
          ),
        )
    );
  }
}

void main() => runApp(MaterialApp(
  title: 'Account manager',
  home: HomeWidget(),
));

@pragma('vm:entry-point')
void backgroundServiceCallback() async {
  WidgetsFlutterBinding.ensureInitialized();
  const platform = MethodChannel('com.contedevel.accountmanager_example/bgsync');
  platform.setMethodCallHandler((call) async {
    // Place the code that should be called on sync here
  });
}