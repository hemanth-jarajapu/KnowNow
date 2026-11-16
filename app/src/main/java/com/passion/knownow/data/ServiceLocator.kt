package com.passion.knownow.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.WorkManager
import com.passion.knownow.data.db.KnowNowDatabase
import com.passion.knownow.data.network.GitHubApi
import com.passion.knownow.data.network.HttpFetcher
import com.passion.knownow.data.network.RawApi
import com.passion.knownow.data.repository.DocRepository
import com.passion.knownow.data.repository.DownloadRepository
import kotlinx.coroutines.flow.first
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

private val Context.settingsDataStore by preferencesDataStore(name = "knownow_settings")

object ServiceLocator {
    private lateinit var appContext: Context

    @Volatile
    var githubToken: String? = null

    val database: KnowNowDatabase by lazy {
        Room.databaseBuilder(appContext, KnowNowDatabase::class.java, "knownow.db")
            .addMigrations(KnowNowDatabase.MIGRATION_6_7)
            .build()
    }

    private val authInterceptor = Interceptor { chain ->
        val token = githubToken
        val host = chain.request().url.host
        val request = if (
            !token.isNullOrBlank() &&
            (host == "api.github.com" || host == "raw.githubusercontent.com")
        ) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val retryInterceptor = Interceptor { chain ->
        var attempt = 0
        var response = chain.proceed(chain.request())
        while (!response.isSuccessful && (response.code == 429 || response.code in 500..599) && attempt < 2) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull()
            val delayMs = retryAfter?.times(1000) ?: (400L shl attempt)
            response.close()
            Thread.sleep(delayMs.coerceAtMost(4_000L))
            attempt++
            response = chain.proceed(chain.request())
        }
        response
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(Cache(File(appContext.cacheDir, "http_cache"), 64L * 1024 * 1024))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(retryInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    val fetcher: HttpFetcher by lazy { HttpFetcher(httpClient) }

    val githubApi: GitHubApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(httpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }

    val rawApi: RawApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(httpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(RawApi::class.java)
    }

    val docRepository: DocRepository by lazy {
        DocRepository(githubApi, rawApi, database)
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(
            context = appContext,
            db = database,
            workManager = WorkManager.getInstance(appContext),
            fetcher = fetcher,
        )
    }

    val context: Context get() = appContext
    val dataStore get() = appContext.settingsDataStore

    suspend fun allowMeteredDownloads(): Boolean {
        val key = stringPreferencesKey("allow_metered_downloads")
        return dataStore.data.first()[key] == "true"
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
