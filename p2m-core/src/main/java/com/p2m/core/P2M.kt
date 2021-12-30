package com.p2m.core

import android.app.Activity
import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import com.p2m.core.config.P2MConfigManager
import com.p2m.core.internal._P2M
import com.p2m.core.module.*

object P2M : ModuleApiProvider {

    /**
     * Start a config.
     */
    fun config(block: P2MConfigManager.() -> Unit) {
        block(_P2M.configManager)
    }

    /**
     * Initialization.
     */
    @MainThread
    fun init(context: Context, externalModuleClassLoader: ClassLoader = context.classLoader, vararg externalPublicModuleClassName: String) {
        check(Looper.getMainLooper() === Looper.myLooper()) { "`P2M.init()` must be called on the main thread." }
        _P2M.init(context, externalModuleClassLoader, externalPublicModuleClassName)
    }

    /**
     * Get instance of `api` by [clazz] of module.
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     *
     * @see Module
     * @see ModuleApi
     */
    override fun <MODULE_API : ModuleApi<*, *, *>> apiOf(clazz: Class<out Module<MODULE_API>>): MODULE_API {
        return _P2M.apiOf(clazz)
    }

    fun findRecoverableChannel(activity: Activity) =
        _P2M.recoverableChannelHelper.findRecoverableChannel(activity)
}