package net.theluckycoder.stundenplan

import android.app.Application
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import net.theluckycoder.stundenplan.utils.UpdateChecker

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 20 * 60 // 20 minutes
        }

        Firebase.remoteConfig.also {
            it.setConfigSettingsAsync(configSettings)
            it.setDefaultsAsync(mapOf(
                UpdateChecker.KEY_CURRENT_VERSION to BuildConfig.VERSION_CODE,
            ))

            it.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("RemoteConfig", "Remote Config Fetched Successfully")
                }
            }
        }
    }
}
