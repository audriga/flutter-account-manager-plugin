package com.contedevel.accountmanager_example

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.os.Bundle
import android.content.Intent

// From https://developer.android.com/training/sync-adapters/creating-authenticator
/*
 * Implement AbstractAccountAuthenticator and stub out all
 * of its methods
 */
class Authenticator(private val mContext: Context) // Simple constructor
    : AbstractAccountAuthenticator(mContext) {

    // Editing properties is not supported
    override fun editProperties(r: AccountAuthenticatorResponse, s: String): Bundle {
        throw UnsupportedOperationException()
    }


    /**
    // Don't add additional accounts
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
