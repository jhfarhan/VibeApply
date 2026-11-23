package com.vibeapply

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

private val Background = Color(0xFF09090B)
private val SurfaceDark = Color(0xFF171719)
private val SurfaceRaised = Color(0xFF202024)
private val AccentRed = Color(0xFFE53945)
private val AccentSoft = Color(0xFFFF737B)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextMuted = Color(0xFFA1A1AA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 9, 11)
        window.navigationBarColor = android.graphics.Color.rgb(9, 9, 11)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = AccentRed, secondary = AccentSoft, background = Background,
                surface = SurfaceDark, onPrimary = Color.White,
                onBackground = TextPrimary, onSurface = TextPrimary
            )) { VibeApplyApp() }
        }
    }
}

private enum class Page { HOME, PDF, COMPRESS, PHOTO, CHECKLIST }

private data class PhotoPreset(val label: String, val width: Int, val height: Int)

private val PhotoPresets = listOf(
    PhotoPreset("35 × 45 mm • 413 × 531 px", 413, 531),
    PhotoPreset("2 × 2 inch • 600 × 600 px", 600, 600),
    PhotoPreset("4 × 6 inch • 1200 × 1800 px", 1200, 1800)
)

private val BaseChecklistItems = listOf(
    "Completed application form",
    "Recent photograph",
    "Resume or CV",
    "Education certificates",
    "Transcript or marksheet",
    "Identification document (if required)",
    "Recommendation letter (if required)",
    "Payment receipt (if required)"
)

@Composable
private fun VibeApplyApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(Page.HOME) }
    var status by remember { mutableStateOf("Your files stay on your device. No account or cloud upload.") }
    var busy by remember { mutableStateOf(false) }
    val pdfImages = remember { mutableStateListOf<Uri>() }
    var compressImage by remember { mutableStateOf<Uri?>(null) }
    var targetKb by remember { mutableStateOf("200") }
    var photoImage by remember { mutableStateOf<Uri?>(null) }
    var selectedPhotoPreset by remember { mutableStateOf(0) }
    var photoPresetMenu by remember { mutableStateOf(false) }
    val localPrefs = remember { context.getSharedPreferences("vibeapply_local", android.content.Context.MODE_PRIVATE) }
    var checkedItems by remember {
        mutableStateOf(localPrefs.getStringSet("checked_items", emptySet())?.toSet() ?: emptySet())
    }
    var customItems by remember {
        mutableStateOf(localPrefs.getStringSet("custom_items", emptySet())?.toList()?.sorted() ?: emptyList())
    }
    var newChecklistItem by remember { mutableStateOf("") }

    val pickPdfImages = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { selected ->
        pdfImages.clear()
        pdfImages.addAll(selected)
        status = if (selected.isEmpty()) "No images selected." else selected.size.toString() + " image(s) ready for your PDF."
    }
    val pickCompressImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        compressImage = selected
        status = if (selected == null) "No image selected." else "Image selected. Choose a target size."
    }
    val pickPhotoImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        photoImage = selected
        status = if (selected == null) "No image selected." else "Photo selected. Choose an output size."
    }
    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
        if (destination != null) {
            val images = pdfImages.toList()
            scope.launch {
                busy = true
                status = "Creating your PDF on this device…"
                val result = withContext(Dispatchers.IO) { runCatching { createPdfFromImages(context, images, destination) } }
                status = result.fold(
                    { "PDF saved successfully • " + images.size + " page(s)." },
                    { "Could not create PDF: " + (it.message ?: "Unknown error") }
                )
                busy = false
                if (result.isSuccess) pdfImages.clear()
            }
        }
    }
    val createJpeg = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { destination ->
        val source = compressImage
        if (destination != null && source != null) {
            val parsed = targetKb.toIntOrNull()
            val limit = if (parsed == null) 200 else parsed.coerceIn(10, 20_000)
            scope.launch {
                busy = true
                status = "Compressing image offline…"
                val result = withContext(Dispatchers.IO) { runCatching { compressImageToTarget(context, source, destination, limit) } }
                status = result.fold(
                    { actual ->
                        val actualKb = (actual + 1023) / 1024
                        if (actualKb <= limit) "Image saved • " + actualKb + " KB (target " + limit + " KB)."
                        else "Image saved • " + actualKb + " KB. The selected target was very small for this image."
                    },
                    { "Could not compress image: " + (it.message ?: "Unknown error") }
                )
                busy = false
                compressImage = null
            }
        }
    }

    val createPhoto = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { destination ->
        val source = photoImage
        if (destination != null && source != null) {
            val preset = PhotoPresets[selectedPhotoPreset]
            scope.launch {
                busy = true
                status = "Resizing photo offline…"
                val result = withContext(Dispatchers.IO) {
                    runCatching { resizePhotoToPreset(context, source, destination, preset) }
                }
                status = result.fold(
                    { bytes -> "Photo saved • " + ((bytes + 1023) / 1024) + " KB • " + preset.width + " × " + preset.height + " px." },
                    { "Could not resize photo: " + (it.message ?: "Unknown error") }
                )
                busy = false
                photoImage = null
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(AccentRed, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Text("V", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("VibeApply", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Your offline document toolkit", fontSize = 12.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(26.dp))
            when (page) {
                Page.HOME -> {
                    Text("Get things application-ready.", fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Prepare documents, resize images and create PDFs right on your phone.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(22.dp))
                    ToolCard("PDF", "Images to PDF", "Turn multiple photos or scanned pages into one PDF.") {
                        page = Page.PDF
                        status = "Select the images you want to combine."
                    }
                    Spacer(Modifier.height(12.dp))
                    ToolCard("KB", "Image Compressor", "Reduce a photo to a target file size for online forms.") {
                        page = Page.COMPRESS
                        status = "Choose an image and set its target size."
                    }
                    Spacer(Modifier.height(12.dp))
                    ToolCard("ID", "Photo Resizer", "Prepare ID, passport-style or print photos at exact pixel dimensions.") {
                        page = Page.PHOTO
                        status = "Choose a photo and an output size."
                    }
                    Spacer(Modifier.height(12.dp))
                    ToolCard("✓", "Application Checklist", "Keep track of documents before submitting an application.") {
                        page = Page.CHECKLIST
                        status = "Your checklist is saved only on this device."
                    }
                    Spacer(Modifier.height(20.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("PRIVATE BY DESIGN", color = AccentSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(7.dp))
                            Text("No account. No cloud upload. Your documents stay on your device.", color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
                Page.PDF -> {
                    BackHeader("Images to PDF") { page = Page.HOME }
                    Text("Combine photos into a clean, shareable PDF.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(onClick = { pickPdfImages.launch(arrayOf("image/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose images")
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoPanel(
                        if (pdfImages.isEmpty()) "No images selected" else pdfImages.size.toString() + " image(s) selected",
                        "Each selected image becomes one A4 PDF page. You choose where to save the result."
                    )
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton("Create PDF", pdfImages.isNotEmpty() && !busy) { createPdf.launch("VibeApply-document.pdf") }
                }
                Page.COMPRESS -> {
                    BackHeader("Image Compressor") { page = Page.HOME }
                    Text("Make an image smaller for forms and online applications.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { pickCompressImage.launch(arrayOf("image/*")) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()
                    ) { Text(if (compressImage == null) "Choose an image" else "Change image") }
                    Spacer(Modifier.height(12.dp))
                    InfoPanel(
                        if (compressImage == null) "No image selected" else "Image ready",
                        "The output is JPEG. Original file is never changed."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetKb,
                        onValueChange = { targetKb = it.filter(Char::isDigit).take(5) },
                        label = { Text("Target size (KB)") },
                        supportingText = { Text("Choose between 10 KB and 20,000 KB.") },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    val validTarget = targetKb.toIntOrNull()?.let { it in 10..20_000 } == true
                    PrimaryButton("Compress and save", compressImage != null && validTarget && !busy) {
                        createJpeg.launch("VibeApply-compressed.jpg")
                    }
                }
                Page.PHOTO -> {
                    BackHeader("Photo Resizer") { page = Page.HOME }
                    Text("Create a precisely sized JPEG for forms or photo prints.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { pickPhotoImage.launch(arrayOf("image/*")) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()
                    ) { Text(if (photoImage == null) "Choose a photo" else "Change photo") }
                    Spacer(Modifier.height(14.dp))
                    Text("Output size", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedButton(onClick = { photoPresetMenu = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(PhotoPresets[selectedPhotoPreset].label)
                        }
                        DropdownMenu(expanded = photoPresetMenu, onDismissRequest = { photoPresetMenu = false }) {
                            PhotoPresets.forEachIndexed { index, preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.label) },
                                    onClick = { selectedPhotoPreset = index; photoPresetMenu = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    InfoPanel(
                        if (photoImage == null) "No photo selected" else "Photo ready",
                        "The image is center-cropped to the selected aspect ratio and resized to the exact pixel dimensions. Check that the crop keeps the subject correctly framed."
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Resize and save JPEG", photoImage != null && !busy) {
                        createPhoto.launch("VibeApply-photo.jpg")
                    }
                }
                Page.CHECKLIST -> {
                    BackHeader("Application Checklist") { page = Page.HOME }
                    val allItems = BaseChecklistItems + customItems
                    val completed = allItems.count { checkedItems.contains(it) }
                    Text("Track what you have prepared. Progress is stored locally on this phone.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(completed.toString() + " of " + allItems.size + " completed", color = AccentSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (allItems.isEmpty()) 0f else completed.toFloat() / allItems.size.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                                color = AccentRed,
                                trackColor = SurfaceRaised
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    allItems.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checkedItems.contains(item),
                                onCheckedChange = { isChecked ->
                                    val updated = if (isChecked) checkedItems + item else checkedItems - item
                                    checkedItems = updated
                                    localPrefs.edit().putStringSet("checked_items", updated).apply()
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AccentRed)
                            )
                            Text(item, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newChecklistItem,
                        onValueChange = { newChecklistItem = it.take(80) },
                        label = { Text("Add your own item") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Add checklist item", newChecklistItem.trim().isNotEmpty() && !busy) {
                        val item = newChecklistItem.trim()
                        if (item.isNotEmpty() && !(BaseChecklistItems + customItems).contains(item)) {
                            val updated = customItems + item
                            customItems = updated
                            localPrefs.edit().putStringSet("custom_items", updated.toSet()).apply()
                        }
                        newChecklistItem = ""
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            checkedItems = emptySet()
                            localPrefs.edit().remove("checked_items").apply()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Reset completed items", color = AccentSoft) }
                }
            }
            Spacer(Modifier.height(20.dp))
            if (busy) {
                Text("Working locally…", color = AccentSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
            }
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
                Text(status, Modifier.fillMaxWidth().padding(14.dp), color = TextMuted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(30.dp))
            Text("VibeApply • Offline-first • Free", Modifier.align(Alignment.CenterHorizontally), color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ToolCard(symbol: String, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(Color(0xFF32171B), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text(symbol, color = AccentSoft, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(description, color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Text("›", color = AccentSoft, fontSize = 30.sp)
        }
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = AccentSoft, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
        Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun InfoPanel(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(detail, color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentRed, disabledContainerColor = SurfaceRaised),
        shape = RoundedCornerShape(15.dp)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private fun decodeScaledBitmap(context: android.content.Context, uri: Uri): Bitmap {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IOException("Could not open selected image")
    var sample = 1
    while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IOException("Could not decode selected image")
}

private fun createPdfFromImages(context: android.content.Context, images: List<Uri>, destination: Uri) {
    if (images.isEmpty()) throw IllegalArgumentException("Select at least one image")
    val document = PdfDocument()
    try {
        images.forEachIndexed { index, uri ->
            val bitmap = decodeScaledBitmap(context, uri)
            try {
                val pageWidth = 595
                val pageHeight = 842
                val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                page.canvas.drawColor(android.graphics.Color.WHITE)
                val scale = min(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
                val width = bitmap.width * scale
                val height = bitmap.height * scale
                val left = (pageWidth - width) / 2f
                val top = (pageHeight - height) / 2f
                page.canvas.drawBitmap(
                    bitmap, null, RectF(left, top, left + width, top + height),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
                document.finishPage(page)
            } finally {
                bitmap.recycle()
            }
        }
        context.contentResolver.openOutputStream(destination, "w")?.use { document.writeTo(it) }
            ?: throw IOException("Could not open destination file")
    } finally {
        document.close()
    }
}

private fun compressImageToTarget(
    context: android.content.Context, source: Uri, destination: Uri, targetKb: Int
): Long {
    val targetBytes = targetKb.toLong() * 1024L
    var bitmap = decodeScaledBitmap(context, source)
    var quality = 90
    var encoded = ByteArray(0)
    try {
        for (attempt in 0 until 32) {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            encoded = output.toByteArray()
            if (encoded.size <= targetBytes) break
            if (quality > 35) {
                quality -= 10
            } else {
                val newWidth = max(240, (bitmap.width * 0.85f).toInt())
                val newHeight = max(240, (bitmap.height * 0.85f).toInt())
                if (newWidth >= bitmap.width || newHeight >= bitmap.height) break
                val smaller = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                bitmap.recycle()
                bitmap = smaller
                quality = 85
            }
        }
        context.contentResolver.openOutputStream(destination, "w")?.use { it.write(encoded) }
            ?: throw IOException("Could not open destination file")
        return encoded.size.toLong()
    } finally {
        bitmap.recycle()
    }
}


private fun resizePhotoToPreset(
    context: android.content.Context,
    source: Uri,
    destination: Uri,
    preset: PhotoPreset
): Long {
    val input = decodeScaledBitmap(context, source)
    val output = Bitmap.createBitmap(preset.width, preset.height, Bitmap.Config.ARGB_8888)
    try {
        val sourceRatio = input.width.toFloat() / input.height.toFloat()
        val targetRatio = preset.width.toFloat() / preset.height.toFloat()
        val crop = if (sourceRatio > targetRatio) {
            val cropWidth = (input.height * targetRatio).toInt().coerceIn(1, input.width)
            val left = (input.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, input.height)
        } else {
            val cropHeight = (input.width / targetRatio).toInt().coerceIn(1, input.height)
            val top = (input.height - cropHeight) / 2
            Rect(0, top, input.width, top + cropHeight)
        }
        val canvas = Canvas(output)
        canvas.drawBitmap(
            input, crop, Rect(0, 0, preset.width, preset.height),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        val bytes = ByteArrayOutputStream().use { stream ->
            if (!output.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                throw IOException("Could not encode resized photo")
            }
            stream.toByteArray()
        }
        context.contentResolver.openOutputStream(destination, "w")?.use { it.write(bytes) }
            ?: throw IOException("Could not open destination file")
        return bytes.size.toLong()
    } finally {
        input.recycle()
        output.recycle()
    }
}
