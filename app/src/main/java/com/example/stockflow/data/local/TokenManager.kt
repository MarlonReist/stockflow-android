package com.example.stockflow.data.local

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "auth_preferences"
)

class TokenManager(context: Context) {

    private val appContext = context.applicationContext

    val token: Flow<String?> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[TOKEN_KEY]
        }

    suspend fun saveToken(token: String) {
        appContext.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun clearToken() {
        appContext.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }
}
