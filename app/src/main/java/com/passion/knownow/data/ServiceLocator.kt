package com.passion.knownow.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import kotlinx.coroutines.flow.first
import com.passion.knownow.data.db.KnowNowDatabase
import com.passion.knownow.data.network.GitHubApi
import com.passion.knownow.data.network.RawApi
import com.passion.knownow.data.repository.DocRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

private val Context.dataStore by preferencesDataStore(name = "knownow_settings")

object ServiceLocator {
    private lateinit var appContext: Context

    val database: KnowNowDatabase by lazy {
        Room.databaseBuilder(appContext, KnowNowDatabase::class.java, "knownow.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private val okHttp: OkHttpClient by lazy { OkHttpClient.Builder().build() }

    val githubApi: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttp)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }

    val rawApi: RawApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttp)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(RawApi::class.java)
    }

    val docRepository: DocRepository by lazy {
        DocRepository(githubApi, rawApi, database)
    }

    val context: Context get() = appContext

    suspend fun allowMeteredDownloads(): Boolean {
        val key = stringPreferencesKey("allow_metered_downloads")
        return appContext.dataStore.data.first()[key] == "true"
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}