package com.vibeapply

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

private enum class Page { HOME, PDF, COMPRESS, PHOTO, CHECKLIST, OCR, PDF_EXTRACT, PDF_MERGE }

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
    var ocrImage by remember { mutableStateOf<Uri?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var pdfSource by remember { mutableStateOf<Uri?>(null) }
    var pdfPageCount by remember { mutableStateOf<Int?>(null) }
    var pdfPageRange by remember { mutableStateOf("") }
    var selectedPdfPageNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    val mergePdfSources = remember { mutableStateListOf<Uri>() }
    var selectedPhotoPreset by remember { mutableStateOf(0) }
    var photoPresetMenu by remember { mutableStateOf(false) }
    val localPrefs = remember { context.getSharedPreferences("vibeapply_local", android.content.Context.MODE_PRIVATE) }
    var checkedItems by remember {
        mutableStateOf<Set<String>>(
            localPrefs.getStringSet("checked_items", emptySet<String>())?.toSet() ?: emptySet<String>()
        )
    }
    var customItems by remember {
        mutableStateOf<List<String>>(
            localPrefs.getStringSet("custom_items", emptySet<String>())?.toList()?.sorted() ?: emptyList<String>()
        )
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
    val pickOcrImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        ocrImage = selected
        ocrText = ""
        status = if (selected == null) "No image selected." else "Image ready for on-device text extraction."
    }
    val pickPdfSource = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        pdfSource = selected
        pdfPageCount = null
        pdfPageRange = ""
        if (selected == null) {
            status = "No PDF selected."
        } else {
            busy = true
            status = "Reading PDF page count…"
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { getPdfPageCount(context, selected) } }
                result.fold(
                    { count ->
                        pdfPageCount = count
                        pdfPageRange = if (count > 0) "1-" + count else ""
                        status = "PDF ready • " + count + " page(s)."
                    },
                    { status = "Could not open PDF: " + (it.localizedMessage ?: "Unknown error") }
                )
                busy = false
            }
        }
    }
    val pickMergePdfs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { selected ->
        mergePdfSources.clear()
        mergePdfSources.addAll(selected)
        status = if (selected.isEmpty()) "No PDFs selected." else selected.size.toString() + " PDF file(s) ready to merge."
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

    val createExtractedPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
        val source = pdfSource
        val pages = selectedPdfPageNumbers
        if (destination != null && source != null && pages.isNotEmpty()) {
            scope.launch {
                busy = true
                status = "Extracting selected pages offline…"
                val result = withContext(Dispatchers.IO) {
                    runCatching { writeRenderedPdfPages(context, listOf(source), mapOf(source.toString() to pages), destination) }
                }
                status = result.fold(
                    { "New PDF saved • " + pages.size + " page(s)." },
                    { "Could not extract pages: " + (it.localizedMessage ?: "Unknown error") }
                )
                busy = false
            }
        }
    }
    val createMergedPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
        val sources = mergePdfSources.toList()
        if (destination != null && sources.size >= 2) {
            scope.launch {
                busy = true
                status = "Merging PDFs offline…"
                val result = withContext(Dispatchers.IO) {
                    runCatching { writeRenderedPdfPages(context, sources, null, destination) }
                }
                status = result.fold(
                    { "Merged PDF saved • " + sources.size + " source file(s)." },
                    { "Could not merge PDFs: " + (it.localizedMessage ?: "Unknown error") }
                )
                busy = false
                mergePdfSources.clear()
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
                    Spacer(Modifier.height(12.dp))
                    ToolCard("OCR", "Image Text Extractor", "Read printed Latin-script text from a photo without uploading it.") {
                        page = Page.OCR
                        status = "Choose an image to extract text on this device."
                    }
                    Spacer(Modifier.height(12.dp))
                    ToolCard("PDF", "PDF Page Extractor", "Save selected pages from a PDF as a separate document.") {
                        page = Page.PDF_EXTRACT
                        status = "Choose a PDF to inspect its pages."
                    }
                    Spacer(Modifier.height(12.dp))
                    ToolCard("+", "Merge PDFs", "Combine several PDF files into one document.") {
                        page = Page.PDF_MERGE
                        status = "Choose two or more PDF files to combine."
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
                Page.PDF_EXTRACT -> {
                    BackHeader("PDF Page Extractor") { page = Page.HOME }
                    Text("Choose which pages to keep in a new PDF.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { pickPdfSource.launch(arrayOf("application/pdf")) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()
                    ) { Text(if (pdfSource == null) "Choose a PDF" else "Change PDF") }
                    Spacer(Modifier.height(10.dp))
                    InfoPanel(
                        if (pdfPageCount == null) "No PDF selected" else "PDF loaded • " + pdfPageCount + " page(s)",
                        "Enter pages like 1-3,5,8-10. Output pages are rendered as images, so selectable text and interactive form fields are not retained. Up to 30 pages per output for memory safety."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pdfPageRange,
                        onValueChange = { pdfPageRange = it.take(100) },
                        label = { Text("Pages to keep") },
                        placeholder = { Text("Example: 1-3,5") },
                        singleLine = true,
                        enabled = pdfPageCount != null && !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Extract and save PDF", pdfSource != null && pdfPageCount != null && pdfPageRange.isNotBlank() && !busy) {
                        val count = pdfPageCount ?: 0
                        val parsed = runCatching { parsePdfPageSelection(pdfPageRange, count) }
                        parsed.fold(
                            { pages ->
                                selectedPdfPageNumbers = pages
                                createExtractedPdf.launch("VibeApply-extracted-pages.pdf")
                            },
                            { status = it.localizedMessage ?: "Invalid page selection." }
                        )
                    }
                }
                Page.PDF_MERGE -> {
                    BackHeader("Merge PDFs") { page = Page.HOME }
                    Text("Combine two or more PDF files into one new document.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { pickMergePdfs.launch(arrayOf("application/pdf")) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose PDF files") }
                    Spacer(Modifier.height(10.dp))
                    InfoPanel(
                        mergePdfSources.size.toString() + " PDF file(s) selected",
                        "All pages are combined in selection order. Pages are rendered as images in the output, so selectable text and interactive form fields are not retained. Up to 30 pages total per output."
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Merge and save PDF", mergePdfSources.size >= 2 && !busy) {
                        createMergedPdf.launch("VibeApply-merged.pdf")
                    }
                }
                Page.OCR -> {
                    BackHeader("Image Text Extractor") { page = Page.HOME }
                    Text("Extract printed text from an image with on-device recognition.", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { pickOcrImage.launch(arrayOf("image/*")) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()
                    ) { Text(if (ocrImage == null) "Choose an image" else "Change image") }
                    Spacer(Modifier.height(10.dp))
                    InfoPanel(
                        if (ocrImage == null) "No image selected" else "Image ready",
                        "This build recognizes Latin-script text (such as English and numbers). Bangla text recognition is not included yet. Processing stays on this device."
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Extract text", ocrImage != null && !busy) {
                        val source = ocrImage
                        if (source != null) {
                            busy = true
                            status = "Recognizing text on this device…"
                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            try {
                                val image = InputImage.fromFilePath(context, source)
                                recognizer.process(image)
                                    .addOnSuccessListener { result ->
                                        ocrText = result.text
                                        status = if (result.text.isBlank()) "No readable text was found. Try a sharper, well-lit image." else "Text extracted. You can edit it or copy it below."
                                        busy = false
                                        recognizer.close()
                                    }
                                    .addOnFailureListener { error ->
                                        status = "Text extraction failed: " + (error.localizedMessage ?: "Unknown error")
                                        busy = false
                                        recognizer.close()
                                    }
                            } catch (error: Exception) {
                                recognizer.close()
                                status = "Could not read this image: " + (error.localizedMessage ?: "Unknown error")
                                busy = false
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = ocrText,
                        onValueChange = { ocrText = it },
                        label = { Text("Extracted text") },
                        placeholder = { Text("The recognized text will appear here.") },
                        minLines = 6,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("VibeApply extracted text", ocrText))
                            status = "Text copied to clipboard."
                        },
                        enabled = ocrText.isNotBlank() && !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Copy extracted text") }
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


private fun getPdfPageCount(context: android.content.Context, source: Uri): Int {
    val descriptor = context.contentResolver.openFileDescriptor(source, "r")
        ?: throw IOException("Could not open the selected PDF")
    return try {
        PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
    } finally {
        descriptor.close()
    }
}

private fun parsePdfPageSelection(input: String, pageCount: Int): List<Int> {
    if (pageCount < 1) throw IllegalArgumentException("The PDF has no readable pages.")
    val selected = sortedSetOf<Int>()
    val tokens = input.split(",").map { it.trim() }
    if (tokens.isEmpty() || tokens.any { it.isEmpty() }) {
        throw IllegalArgumentException("Enter page numbers like 1-3,5.")
    }
    tokens.forEach { token ->
        val parts = token.split("-").map { it.trim() }
        val start: Int
        val end: Int
        if (parts.size == 1) {
            start = parts[0].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid page number: " + token)
            end = start
        } else if (parts.size == 2) {
            start = parts[0].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid page range: " + token)
            end = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid page range: " + token)
        } else {
            throw IllegalArgumentException("Invalid page range: " + token)
        }
        if (start < 1 || end < start || end > pageCount) {
            throw IllegalArgumentException("Page range " + token + " is outside 1-" + pageCount + ".")
        }
        (start..end).forEach { selected.add(it) }
    }
    if (selected.isEmpty()) throw IllegalArgumentException("Select at least one page.")
    if (selected.size > 30) throw IllegalArgumentException("Export up to 30 pages at a time.")
    return selected.toList()
}

private fun writeRenderedPdfPages(
    context: android.content.Context,
    sources: List<Uri>,
    selectedPages: Map<String, List<Int>>?,
    destination: Uri
) {
    if (sources.isEmpty()) throw IllegalArgumentException("Select at least one PDF.")
    val document = PdfDocument()
    var outputPageNumber = 1
    try {
        sources.forEach { source ->
            val descriptor = context.contentResolver.openFileDescriptor(source, "r")
                ?: throw IOException("Could not open a selected PDF")
            try {
                PdfRenderer(descriptor).use { renderer ->
                    val pageNumbers = selectedPages?.get(source.toString())
                        ?: (1..renderer.pageCount).toList()
                    pageNumbers.forEach { pageNumber ->
                        if (pageNumber !in 1..renderer.pageCount) {
                            throw IllegalArgumentException("A selected page is outside the PDF.")
                        }
                        if (outputPageNumber > 30) {
                            throw IllegalArgumentException("Export up to 30 pages at a time.")
                        }
                        val page = renderer.openPage(pageNumber - 1)
                        try {
                            val scale = min(1.8f, 1800f / max(page.width, page.height).toFloat())
                            val bitmapWidth = (page.width * scale).toInt().coerceAtLeast(1)
                            val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                            try {
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                val matrix = Matrix().apply { setScale(scale, scale) }
                                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                val pageInfo = PdfDocument.PageInfo.Builder(
                                    page.width, page.height, outputPageNumber
                                ).create()
                                val outputPage = document.startPage(pageInfo)
                                outputPage.canvas.drawBitmap(
                                    bitmap, null,
                                    RectF(0f, 0f, page.width.toFloat(), page.height.toFloat()),
                                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                                )
                                document.finishPage(outputPage)
                            } finally {
                                bitmap.recycle()
                            }
                        } finally {
                            page.close()
                        }
                        outputPageNumber += 1
                    }
                }
            } finally {
                descriptor.close()
            }
        }
        if (outputPageNumber == 1) throw IOException("No pages were available to export.")
        context.contentResolver.openOutputStream(destination, "w")?.use { document.writeTo(it) }
            ?: throw IOException("Could not open the output file")
    } finally {
        document.close()
    }
}
