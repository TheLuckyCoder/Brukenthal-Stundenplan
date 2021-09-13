package net.theluckycoder.stundenplan.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import net.theluckycoder.stundenplan.BuildConfig
import net.theluckycoder.stundenplan.R
import net.theluckycoder.stundenplan.model.TimetableType
import net.theluckycoder.stundenplan.utils.Analytics
import net.theluckycoder.stundenplan.viewmodel.HomeViewModel
import kotlin.math.roundToInt

class HomeActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = ComposeView(this)
        view.setContent {
            val isDark by viewModel.darkThemeFlow.collectAsState(initial = true)
            AppTheme(isDark = isDark) {
                val systemUiController = rememberSystemUiController()

                val primaryVariantColor = MaterialTheme.colors.primaryVariant
                SideEffect {
                    systemUiController.setSystemBarsColor(primaryVariantColor, darkIcons = true)
                }

                HomeScreen(viewModel = viewModel)
            }
        }

        setContentView(view)

        // For Firebase Analytics
        if (intent.getBooleanExtra(ARG_OPENED_FROM_NOTIFICATION, false))
            Analytics.openNotificationEvent()
    }

    companion object {
        const val ARG_OPENED_FROM_NOTIFICATION = "opened_from_notification"
        private const val APP_STORE_URL =
            "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
    }
}

@Composable
private fun HomeScreen(
    viewModel: HomeViewModel,
) {
    val timetableType = viewModel.timetableFlow.collectAsState()
    Scaffold(
        topBar = {
            TopBar(viewModel)
        },
        bottomBar = {
            BottomBar(
                timetableType = timetableType.value,
                onTimetableChange = { newTimetableType ->
                    viewModel.switchTimetableType(newTimetableType)
                }
            )
        }
    ) {
        HomeContent(viewModel)
    }
}

@Composable
private fun TopBar(
    viewModel: HomeViewModel
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.primary,
        title = {
            Text(text = stringResource(id = R.string.activity_title))
        },
        actions = {
            val darkThemeFlow = viewModel.darkThemeFlow.collectAsState(initial = true)
            IconButton(onClick = {
                viewModel.switchTheme(!darkThemeFlow.value)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_switch_theme),
                    contentDescription = null
                )
            }
            IconButton(onClick = {
                viewModel.refresh()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = null,
                )
            }

        }
    )
}

@Composable
private fun HomeContent(
    viewModel: HomeViewModel
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }

        val timetableFlow by viewModel.timetableFlow.collectAsState()
        val networkFlow by viewModel.networkFlow.collectAsState()

        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        val roundedScale = scale.roundToInt()
        LaunchedEffect(width, timetableFlow, networkFlow, roundedScale) {
            try {
                bitmap = viewModel.renderPdf(width.toInt(), zoom = roundedScale.toFloat())
            } catch (e: Exception) {
                // Many things can go wrong, but oh well, we'll just do nothing
                e.printStackTrace()
            }
        }

        if (bitmap != null) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            offset += pan
                            scale *= zoom
                        }
                    },
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun BottomBar(
    timetableType: TimetableType,
    onTimetableChange: (newTimetable: TimetableType) -> Unit
) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        val highSchoolSelected = timetableType == TimetableType.HIGH_SCHOOL
        val middleSchoolSelected = highSchoolSelected.not()

        BottomNavigationItem(
            selected = highSchoolSelected,
            onClick = { onTimetableChange(TimetableType.HIGH_SCHOOL) },
            icon = {
                Text(
                    text = stringResource(id = R.string.high_school),
                    color = if (highSchoolSelected) MaterialTheme.colors.secondaryVariant else Color.Unspecified,
                )
            }
        )
        BottomNavigationItem(
            selected = middleSchoolSelected,
            onClick = { onTimetableChange(TimetableType.MIDDLE_SCHOOL) },
            icon = {
                Text(
                    text = stringResource(id = R.string.middle_school),
                    color = if (middleSchoolSelected) MaterialTheme.colors.secondaryVariant else Color.Unspecified,
                )
            }
        )
    }
}

@Preview
@Composable
fun BottomBarPreview() {
    var timetableType by remember { mutableStateOf(TimetableType.MIDDLE_SCHOOL) }

    AppTheme(isDark = true) {
        BottomBar(timetableType = timetableType) { newTimetableType: TimetableType ->
            timetableType = newTimetableType
        }
    }
}
