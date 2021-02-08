package net.theluckycoder.stundenplan.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.theluckycoder.stundenplan.TimetableType

class AppPreferences(context: Context) {

    private val userTeamDataStore = context.createDataStore(name = DATA_STORE_NAME)

    val darkThemeFlow: Flow<Boolean> = userTeamDataStore.data
        .map { preferences ->
            preferences[DARK_THEME] ?: false
        }.distinctUntilChanged()

    suspend fun updateUseDarkTheme(useDarkTheme: Boolean) = userTeamDataStore.edit { preferences ->
        preferences[DARK_THEME] = useDarkTheme
    }

    val timetableTypeFlow: Flow<TimetableType> = userTeamDataStore.data
        .map { preferences ->
            if (preferences[TIMETABLE_TYPE] == false)
                TimetableType.MIDDLE_SCHOOL
            else
                TimetableType.HIGH_SCHOOL
        }.distinctUntilChanged()

    suspend fun updateTimetableType(timetableType: TimetableType) = userTeamDataStore.edit { preferences ->
        preferences[TIMETABLE_TYPE] = timetableType == TimetableType.HIGH_SCHOOL
    }

    companion object {
        private const val DATA_STORE_NAME = "user_prefs"

        private val DARK_THEME = booleanPreferencesKey("dark_theme")
        private val TIMETABLE_TYPE = booleanPreferencesKey("timetable_type")
    }
}
