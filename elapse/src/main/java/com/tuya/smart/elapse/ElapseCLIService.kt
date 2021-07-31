package com.tuya.smart.elapse

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ElapseCLIService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Elapse.dump()
        stopSelf()
        return START_NOT_STICKY
    }

}