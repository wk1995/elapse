package com.tuya.smart.sample

import android.app.Application
import android.content.Context
import com.tuya.smart.raster.Raster
import com.tuya.smart.raster.RasterLogger
import com.tuya.smart.raster.RasterOptions
import me.weishu.reflection.Reflection

class APP: Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Reflection.unseal(base)
    }

    override fun onCreate() {
        super.onCreate()
        val options = RasterOptions()
        options.logLevel = RasterLogger.LogLevel.All
        Raster.init(this, options)
    }
}