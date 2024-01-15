package be.kuleuven.gbiomed.exporl.playassetdeliverytest

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.model.AssetPackStatus.COMPLETED
import com.google.android.play.core.ktx.requestFetch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun getStatusString(status: Int): String {
    return when (status) {
        1 -> "PENDING"
        2 -> "DOWNLOADING"
        3 -> "TRANSFERRING"
        4 -> "COMPLETED"
        5 -> "FAILED"
        6 -> "CANCELED"
        7 -> "WAITING_FOR_WIFI"
        8 -> "NOT_INSTALLED"
        0 -> "UNKNOWN"
        else -> "UNKNOWN"
    }
}

fun stringifyAssetPackState(assetPackState: AssetPackState): String {
    val stringBuilder = StringBuilder()
    stringBuilder.appendLine("name=${assetPackState.name()}")
    stringBuilder.appendLine("status=${getStatusString(assetPackState.status())}")
    stringBuilder.appendLine("errorCode=${assetPackState.errorCode()}")
    stringBuilder.appendLine("bytesDownloaded=${assetPackState.bytesDownloaded()}")
    stringBuilder.appendLine("totalBytesToDownload=${assetPackState.totalBytesToDownload()}")
    stringBuilder.appendLine("transferProgressPercentage=${assetPackState.transferProgressPercentage()}")
    return stringBuilder.toString()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetPackManager = AssetPackManagerFactory.getInstance(this.applicationContext)

        var assetPackStateString by mutableStateOf("(fetch not started)")
        var downloadProgress by mutableFloatStateOf(0.0f)
        var downloadComplete by mutableStateOf(false)
        var packBLocation by mutableStateOf(assetPackManager.getPackLocation("pack_b"))
        var showImage by mutableStateOf<String?>(null)

        assetPackManager.registerListener { assetPackState ->
            assetPackStateString = stringifyAssetPackState(assetPackState)

            downloadProgress =
                assetPackState.bytesDownloaded().toFloat() / assetPackState.totalBytesToDownload()

            if (assetPackState.status() == COMPLETED) {
                downloadComplete = true

                packBLocation = assetPackManager.getPackLocation("pack_b")
            }
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = assetPackStateString)

                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.Default).launch {
                                assetPackManager.requestFetch(arrayListOf("pack_b"))
                            }
                        }
                    ) {
                        Text("Fetch asset pack B")
                    }

                    Button(
                        onClick = { showImage = "A" }
                    ) {
                        Text("Show image A")
                    }

                    Button(
                        onClick = { showImage = "B" },
                        enabled = downloadComplete
                    ) {
                        Text("Show image B")
                    }

                    LinearProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (showImage == "A") {
                        AssetImage(path = "files/a.png")
                    } else if (showImage == "B") {
                        LocalPathImage(path = "${packBLocation!!.assetsPath()}/files/b.png")
                    }
                }
            }
        }
    }
}

@Composable
fun AssetImage(path: String) {
    val context = LocalContext.current
    val assetStream = context.assets.open(path)
    val bitmap = BitmapFactory.decodeStream(assetStream)
    val imageBitmap = bitmap!!.asImageBitmap()

    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LocalPathImage(path: String) {
    val assetStream = File(path).inputStream()
    val bitmap = BitmapFactory.decodeStream(assetStream)
    val imageBitmap = bitmap!!.asImageBitmap()

    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}