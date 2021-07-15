package com.tuya.smart.sample

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.tuya.smart.raster.Raster
import com.tuya.smart.raster.RasterLogger
import com.tuya.smart.raster.RasterOptions
import me.weishu.reflection.Reflection

class APP: Application(), Handler.Callback {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)
    }

    override fun onCreate() {
        super.onCreate()
        val options = RasterOptions()
        options.logLevel = RasterLogger.LogLevel.All
        options.enableFindSlowMethod = true
        Raster.init(this, options)

        val h = Handler(Looper.getMainLooper(), this)
        Thread {
            while(true) {
                Thread.sleep(3)
                h.sendEmptyMessage(2000)
            }
        }
//            .start()
    }

    override fun handleMessage(msg: Message): Boolean {
        Thread.sleep(3)
        return true
    }
}