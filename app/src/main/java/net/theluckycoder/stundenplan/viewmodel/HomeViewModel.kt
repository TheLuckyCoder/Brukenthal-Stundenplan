package net.theluckycoder.stundenplan.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.theluckycoder.stundenplan.BuildConfig
import net.theluckycoder.stundenplan.extensions.app
import net.theluckycoder.stundenplan.extensions.isNetworkAvailable
import net.theluckycoder.stundenplan.model.TimetableType
import net.theluckycoder.stundenplan.repository.MainRepository
import net.theluckycoder.stundenplan.utils.AppPreferences
import net.theluckycoder.stundenplan.utils.FirebaseConstants
import net.theluckycoder.stundenplan.utils.NetworkResult
import java.io.FileNotFoundException
import kotlin.math.roundToInt

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MainRepository(app)
    private val preferences = AppPreferences(app)

    private var lastPdfRenderer: PdfRenderer? = null

    private val timetableStateFlow = MutableStateFlow(TimetableType.HIGH_SCHOOL)
    val timetableFlow = timetableStateFlow.asStateFlow()

    private val networkStateFlow = MutableStateFlow<NetworkResult?>(null)
    val networkFlow = networkStateFlow.asStateFlow()

    val darkThemeFlow = preferences.darkThemeFlow

    // region Mutexes

    private val pdfRendererMutex = Mutex()
    private val refreshMutex = Mutex()

    // endregion Mutexes

    init {
        viewModelScope.launch {
            launch {
                subscribeToFirebaseTopics()
            }

            launch {
                val lastTimetable = preferences.timetableTypeFlow.first()
                timetableStateFlow.value = lastTimetable
                refresh()
            }
        }
    }

    fun refresh() = viewModelScope.launch(Dispatchers.Default) {
        refreshMutex.withLock {
            val isNetworkAvailable = app.isNetworkAvailable()

            if (isNetworkAvailable) {
                downloadTimetable()
            } else {
                // Let the user know that we can't download a newer timetable
                networkStateFlow.value =
                    NetworkResult.Fail(NetworkResult.Fail.Reason.MissingNetworkConnection)
            }
        }
    }

    fun switchTheme(useDarkTheme: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        preferences.updateUseDarkTheme(useDarkTheme)
    }

    fun switchTimetableType(newTimetableType: TimetableType) {
        timetableStateFlow.value = newTimetableType
        refresh()

        viewModelScope.launch(Dispatchers.Default) {
            // Remove the old pdfRenderer
            pdfRendererMutex.withLock {
                lastPdfRenderer?.close()
                lastPdfRenderer = null
            }

            preferences.updateTimetableType(newTimetableType)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(FileNotFoundException::class)
    suspend fun renderPdf(
        width: Int,
        zoom: Float = 1.0f,
        darkMode: Boolean = false
    ): Bitmap = withContext(Dispatchers.Default) {
        val pdfRenderer = pdfRendererMutex.withLock {
            if (lastPdfRenderer == null) {
                val pfd = ParcelFileDescriptor
                    .open(repository.getLastFile(timetableStateFlow.value), MODE_READ_ONLY)
                lastPdfRenderer = PdfRenderer(pfd)
            }

            lastPdfRenderer!!
        }

        ensureActive()

        val scaledWidth = (width * zoom).roundToInt()
//            val scaledHeight = (height * zoom).roundToInt()

        val bitmap = pdfRenderer.openPage(0).use { page ->
            val bitmap = Bitmap.createBitmap(
                scaledWidth, (scaledWidth.toFloat() / page.width * page.height).toInt(),
                Bitmap.Config.ARGB_8888
            )

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            bitmap
        }
        
        if (darkMode) {
          val length = bitmap.width * bitmap.height
          val pixels = IntArray(length)
          bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

          for (i in 0 until length) {
            val c = pixels[i]
            if (Color.alpha(c) != 0) {
              pixels[i] = Color.argb(Color.alpha(c), 255 - Color.red(c), 255 - Color.green(c), 255 - Color.blue(c))
            }
          }
                      
          bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
        
        bitmap
    }

    private suspend fun downloadTimetable() = coroutineScope {
        val type = timetableFlow.value

        // Let the user know that we are starting the download
        networkStateFlow.value = NetworkResult.Loading()

        try {
            val timetable = repository.getTimetable(type)
            check(timetable.url.isNotBlank()) { "No PDF url link found" }

            Log.i(PDF_TAG, "Url: ${timetable.url}")

            if (!repository.doesFileExist(timetable)) {
                repository.downloadPdf(timetable)
                    .flowOn(Dispatchers.IO)
                    .collect { networkResult ->
                        ensureActive()
                        networkStateFlow.value = networkResult
                    }
            } else {
                networkStateFlow.value = NetworkResult.Success()
            }

            Log.i(PDF_TAG, "Finished downloading")
        } catch (e: Exception) {
            Log.e(PDF_TAG, "Failed to download for $type", e)

            networkStateFlow.value =
                NetworkResult.Fail(NetworkResult.Fail.Reason.DownloadFailed)
        }
    }

    private fun subscribeToFirebaseTopics() {
        with(Firebase.messaging) {
            subscribeToTopic(FirebaseConstants.TOPIC_ALL)

            if (BuildConfig.DEBUG)
                subscribeToTopic(FirebaseConstants.TOPIC_TEST)
        }
    }

    private companion object {
        private const val PDF_TAG = "PDF Load"
    }
}
