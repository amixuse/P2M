package com.p2m.example.app

import android.app.Application
import android.util.Log
import com.p2m.core.P2M
import com.p2m.core.log.ILogger
import com.p2m.core.log.Level

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()

        // 主进程
        if (packageName == getProcessName()) {
            initP2M()
        }
    }

    private fun initP2M() {
        // 配置
        P2M.config {
            logger = object : ILogger {
                override fun log(level: Level, msg: String, throwable: Throwable?) {
                    when(level) {
                        Level.INFO -> Log.i("P2M", msg, throwable)
                        Level.DEBUG -> Log.d("P2M", msg, throwable)
                        Level.WARNING -> Log.w("P2M", msg, throwable)
                        Level.ERROR -> Log.e("P2M", msg, throwable)
                    }
                }
            }
        }

        // P2M.init()将阻塞主线程，直到所有模块初始化完毕。
        P2M.init(
            context = this,
            externalModuleClassLoader = classLoader,
            "com.p2m.example.external.im.p2m.api.IM"
        )
    }
}