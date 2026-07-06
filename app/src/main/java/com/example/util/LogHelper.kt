package com.planca.app.util

import android.util.Log
import com.planca.app.BuildConfig

object LogHelper {
    private const val TAG = "Planca"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }
}
