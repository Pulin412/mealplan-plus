package com.mealplanplus.ui.screens.foods

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Barcode → product flow. Live ML Kit scan (or manual code entry) → Open Food Facts lookup → product
 * card with "Add to my foods". Drives off [FoodsUiState.barcodePhase]; all logic lives in the VM.
 */
@Composable
fun BarcodeScanSheet(state: FoodsUiState, viewModel: FoodViewModel) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Scan barcode", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(16.dp))

        when (state.barcodePhase) {
            BarcodePhase.SCANNING -> ScanningView(hasCamera, onGrant = { permLauncher.launch(Manifest.permission.CAMERA) }, onCode = viewModel::onBarcodeScanned)
            BarcodePhase.LOOKING_UP -> Centered { CircularProgressIndicator(color = Teal); Spacer(Modifier.height(12.dp)); Text("Looking up product…", fontSize = 13.sp, color = Muted) }
            BarcodePhase.RESULT -> state.barcodeResult?.let { ProductResult(it, onAdd = viewModel::addScannedFood, onRescan = viewModel::rescanBarcode) }
            BarcodePhase.NOT_FOUND -> Centered {
                Text(state.barcodeMessage ?: "Product not found", fontSize = 13.5.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("Try again, or add it manually from the ＋ menu.", fontSize = 12.sp, color = MutedLight)
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = viewModel::rescanBarcode) { Text("Scan again", color = Teal) }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = viewModel::closeSheet, modifier = Modifier.align(Alignment.End)) { Text("Close", color = Teal) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ScanningView(hasCamera: Boolean, onGrant: () -> Unit, onCode: (String) -> Unit) {
    var manual by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(Ink),
    ) {
        if (hasCamera) {
            BarcodeCameraPreview(onBarcode = onCode, modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)))
            // Framing guide
            Box(Modifier.fillMaxWidth(0.7f).height(96.dp).border(2.dp, Teal, RoundedCornerShape(10.dp)))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.QrCodeScanner, null, tint = Teal, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(10.dp))
                Text("Camera permission needed", fontSize = 13.sp, color = Muted)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onGrant) { Text("Allow camera", color = Teal) }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Text(
        if (hasCamera) "Point the camera at a product barcode" else "Grant camera access to scan",
        fontSize = 12.sp, color = MutedLight, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )

    Spacer(Modifier.height(6.dp))
    if (!manual) {
        TextButton(onClick = { manual = true }, modifier = Modifier.fillMaxWidth()) { Text("Enter barcode manually", color = Teal, fontSize = 13.sp) }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = code, onValueChange = { code = it.filter(Char::isDigit) },
                placeholder = { Text("Barcode number", fontSize = 13.sp) }, singleLine = true, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { if (code.isNotBlank()) onCode(code.trim()) }, enabled = code.isNotBlank()) { Text("Look up", color = Teal) }
        }
    }
}

@Composable
private fun ProductResult(dto: FoodDto, onAdd: () -> Unit, onRescan: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp)).padding(16.dp),
    ) {
        Text(dto.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        dto.brand?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(2.dp)); Text(it, fontSize = 12.sp, color = MutedLight)
        }
        Spacer(Modifier.height(12.dp))
        Text("Per 100 g", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MutedLight)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Macro("kcal", dto.caloriesPer100)
            Macro("P", dto.proteinPer100)
            Macro("C", dto.carbsPer100)
            Macro("F", dto.fatPer100)
        }
    }
    Spacer(Modifier.height(14.dp))
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Teal)
            .clickable(onClick = onAdd).padding(vertical = 13.dp),
        Alignment.Center,
    ) { Text("Add to my foods", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnAccent) }
    Spacer(Modifier.height(6.dp))
    TextButton(onClick = onRescan, modifier = Modifier.fillMaxWidth()) { Text("Scan another", color = Teal) }
}

@Composable
private fun Macro(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(label, fontSize = 10.5.sp, color = MutedLight)
    }
}

@Composable
private fun Centered(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(300.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

/** CameraX preview + ML Kit barcode analysis. Fires [onBarcode] once, on the first EAN/UPC read. */
@Composable
private fun BarcodeCameraPreview(onBarcode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val handled = remember { AtomicBoolean(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                        .build(),
                )
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy -> analyze(scanner, proxy, handled, onBarcode) }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

@OptIn(ExperimentalGetImage::class)
private fun analyze(scanner: BarcodeScanner, proxy: ImageProxy, handled: AtomicBoolean, onBarcode: (String) -> Unit) {
    val media = proxy.image
    if (media == null || handled.get()) { proxy.close(); return }
    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let { code ->
                if (handled.compareAndSet(false, true)) onBarcode(code)
            }
        }
        .addOnCompleteListener { proxy.close() }
}
