package app.palavrascruzadas

import android.app.Application

class CruzadasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
