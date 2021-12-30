package com.p2m.core.internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.WorkerThread
import com.p2m.core.app.App
import com.p2m.core.channel.InterceptorServiceDefault
import com.p2m.core.channel.RecoverableChannel
import com.p2m.core.config.P2MConfigManager
import com.p2m.core.internal.channel.RecoverableChannelHelper
import com.p2m.core.internal.config.InternalP2MConfigManager
import com.p2m.core.internal.execution.Executor
import com.p2m.core.internal.execution.InternalExecutor
import com.p2m.core.internal.log.logE
import com.p2m.core.internal.log.logW
import com.p2m.core.internal.module.*
import com.p2m.core.internal.module.DefaultModuleFactory
import com.p2m.core.internal.module.DefaultModuleNameCollectorFactory
import com.p2m.core.internal.module.ExternalModuleInfoFinder
import com.p2m.core.internal.module.ManifestModuleInfoFinder
import com.p2m.core.internal.module.ModuleContainerDefault
import com.p2m.core.internal.module.deriver.InternalDriver
import com.p2m.core.module.*
import kotlin.collections.ArrayList

@SuppressLint("StaticFieldLeak")
internal object _P2M : ModuleApiProvider {
    internal lateinit var internalContext : Context
    internal val executor: Executor by lazy { InternalExecutor() }
    internal val interceptorService = InterceptorServiceDefault()
    internal val configManager: P2MConfigManager = InternalP2MConfigManager()
    internal val recoverableChannelHelper by lazy { RecoverableChannelHelper() }
    private val moduleContainer = ModuleContainerDefault()
    private lateinit var driver: InternalDriver

    fun init(
        context: Context,
        externalModuleClassLoader: ClassLoader = context.classLoader,
        externalPublicModuleClassName: Array<out String>,
        @WorkerThread onIdea: (() -> Unit)? = null
    ) {
        check(!_P2M::internalContext.isInitialized) { "`can only be called once." }

        val applicationContext = context.applicationContext
        this.internalContext = applicationContext

        var ideaStartTime = 0L
        val app = App()
            .onEvaluate {
                recoverableChannelHelper.init(context, executor)
                ideaStartTime = SystemClock.uptimeMillis()
                onIdea?.invoke()
            }
            .onEvaluateTooLongStart {
                logE("running `onIdea` too long, it is recommended to shorten to ${(SystemClock.uptimeMillis() - ideaStartTime).also { ideaStartTime+=it }} ms.")
            }
            .onEvaluateTooLongEnd {
                logE("`onIdea` was ran for too long, timeout: ${SystemClock.uptimeMillis() - ideaStartTime} ms.")
            }


        val externalModules = externalPublicModuleClassName.mapTo(ArrayList(externalPublicModuleClassName.size)) { className ->
                ModuleInfo.fromExternal(
                    classLoader = externalModuleClassLoader,
                    publicClassName = className
                )
            }
        val moduleNameCollector: ModuleNameCollector = DefaultModuleNameCollectorFactory()
            .newInstance("${applicationContext.packageName}.GeneratedModuleNameCollector")
            .apply { collectExternal(externalModules) }
        val moduleInfoFinder: ModuleInfoFinder = GlobalModuleInfoFinder(
            ExternalModuleInfoFinder(externalModules),
            ManifestModuleInfoFinder(applicationContext)
        )
        val moduleFactory: ModuleFactory = DefaultModuleFactory()
        moduleContainer.register(app, moduleNameCollector, moduleInfoFinder, moduleFactory)

        this.driver = InternalDriver(applicationContext, app, this.moduleContainer)
        this.driver.considerOpenAwait()
    }

    override fun <MODULE_API : ModuleApi<*, *, *>> apiOf(
        clazz: Class<out Module<MODULE_API>>
    ): MODULE_API {
        check(::internalContext.isInitialized) { "Please call `init()` before." }

        val driver = this.driver
        check(driver.isEvaluating?.get() != true) { "Don not call `P2M.apiOf()` in `onEvaluate()`." }
        driver.safeModuleApiProvider?.get()?.let { moduleProvider ->
            return moduleProvider.apiOf(clazz)
        }

        val module = moduleContainer.find(clazz)
        check(module != null) { "The ${clazz.moduleName} is not exist for ${clazz.name}" }
        driver.considerOpenAwait()
        @Suppress("UNCHECKED_CAST")
        return module.api as MODULE_API
    }

    internal fun saveRecoverableChannel(intent: Intent, recoverableChannel: RecoverableChannel){
        recoverableChannelHelper.saveRecoverableChannel(intent, recoverableChannel)
    }

}