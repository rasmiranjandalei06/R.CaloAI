package com.example.ui.screens.meals

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainActivity
import com.example.data.entities.FoodLog
import com.example.gemini.FoodAnalysisResponse
import com.example.gemini.CameraScanResponse
import com.example.gemini.RecipeAnalysisResponse
import java.io.InputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MealsScreen(
    viewModel: MealsViewModel = viewModel()
) {
    val context = LocalContext.current
    val foodLogs by viewModel.foodLogs.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val cameraScanState by viewModel.cameraScanState.collectAsState()
    val recipeState by viewModel.recipeState.collectAsState()

    var activeMealTypeForAdd by remember { mutableStateOf<String?>(null) }
    var showAddDialogType by remember { mutableStateOf<String?>(null) } // "text", "camera", "manual", "recipe"

    val scrollState = rememberScrollState()

    // Permissions logic
    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Intake Log", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { viewModel.duplicateYesterdayMeals() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Yesterday's Meals")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary banner of logged meals
            MealsSummaryBanner(foodLogs)

            // Collapsible sections
            val mealCategories = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACKS")
            mealCategories.forEach { category ->
                val logs = foodLogs.filter { it.mealType == category }
                MealCategoryCard(
                    categoryName = category,
                    items = logs,
                    onAddFood = {
                        activeMealTypeForAdd = category
                        // Show bottom selection sheets
                        showAddDialogType = "select_option"
                    },
                    onDelete = { viewModel.deleteFood(it) },
                    onToggleFav = { viewModel.toggleFavoriteFood(it) }
                )
            }
        }

        // --- Bottom Sheet / Dialog for logging food ---
        if (activeMealTypeForAdd != null && showAddDialogType != null) {
            val mealType = activeMealTypeForAdd!!
            
            when (showAddDialogType) {
                "select_option" -> {
                    ModalBottomSheet(
                        onDismissRequest = {
                            activeMealTypeForAdd = null
                            showAddDialogType = null
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Add Food to $mealType",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Divider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddDialogType = "text" }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Ask AI nutrition scanner", fontWeight = FontWeight.Bold)
                                    Text("Type e.g. '1 plate biryani and salad' to estimate", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (hasCameraPermission) {
                                            showAddDialogType = "camera"
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Scan Food with Camera", fontWeight = FontWeight.Bold)
                                    Text("Capture meal photo to auto extract nutrition", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddDialogType = "recipe" }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Paste Recipe Ingredients Block", fontWeight = FontWeight.Bold)
                                    Text("Get multi-serving breakdown matching calories", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAddDialogType = "manual" }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Enter Manually", fontWeight = FontWeight.Bold)
                                    Text("Add custom macros weight inputs directly", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                "text" -> {
                    DialogTextSearch(
                        mealType = mealType,
                        uiState = searchState,
                        onSearch = { query -> viewModel.searchFoodItemWithAI(query) },
                        onConfirmAdd = { res ->
                            viewModel.addFoodLog(
                                mealType = mealType,
                                name = res.food_name,
                                qty = res.quantity,
                                cals = res.calories,
                                prot = res.protein_g,
                                carbs = res.carbs_g,
                                fat = res.fat_g
                            )
                            viewModel.clearSearchState()
                            activeMealTypeForAdd = null
                            showAddDialogType = null
                        },
                        onDismiss = {
                            viewModel.clearSearchState()
                            showAddDialogType = "select_option"
                        }
                    )
                }
                "camera" -> {
                    DialogCameraScan(
                        mealType = mealType,
                        uiState = cameraScanState,
                        onScanImage = { bmp -> viewModel.scanCameraImageWithAI(bmp) },
                        onConfirmAdd = { res, selectedIndices ->
                            res.items.forEachIndexed { idx, item ->
                                if (idx in selectedIndices) {
                                    viewModel.addFoodLog(
                                        mealType = mealType,
                                        name = item.food_name,
                                        qty = item.estimated_quantity,
                                        cals = item.calories,
                                        prot = item.protein_g,
                                        carbs = item.carbs_g,
                                        fat = item.fat_g
                                    )
                                }
                            }
                            viewModel.clearCameraScanState()
                            activeMealTypeForAdd = null
                            showAddDialogType = null
                        },
                        onDismiss = {
                            viewModel.clearCameraScanState()
                            showAddDialogType = "select_option"
                        }
                    )
                }
                "recipe" -> {
                    DialogRecipeAnalyze(
                        mealType = mealType,
                        uiState = recipeState,
                        onAnalyze = { text -> viewModel.analyzeRecipeTextWithAI(text) },
                        onConfirmAdd = { res ->
                            res.recipes.forEach { item ->
                                viewModel.addFoodLog(
                                    mealType = mealType,
                                    name = item.food_name,
                                    qty = "1 serving",
                                    cals = item.calories,
                                    prot = item.protein_g,
                                    carbs = item.carbs_g,
                                    fat = item.fat_g
                                )
                            }
                            viewModel.clearRecipeState()
                            activeMealTypeForAdd = null
                            showAddDialogType = null
                        },
                        onDismiss = {
                            viewModel.clearRecipeState()
                            showAddDialogType = "select_option"
                        }
                    )
                }
                "manual" -> {
                    DialogManualEntry(
                        mealType = mealType,
                        onDismiss = { showAddDialogType = "select_option" },
                        onSave = { name, cals, prot, carbs, fat, qty ->
                            viewModel.addFoodLog(
                                mealType = mealType,
                                name = name,
                                qty = qty.ifBlank { "1 portion" },
                                cals = cals,
                                prot = prot,
                                carbs = carbs,
                                fat = fat
                            )
                            activeMealTypeForAdd = null
                            showAddDialogType = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MealsSummaryBanner(logs: List<FoodLog>) {
    val totalCalories = logs.sumOf { it.calories }
    val totalProtein = logs.sumOf { it.proteinG }
    val totalCarbs = logs.sumOf { it.carbsG }
    val totalFats = logs.sumOf { it.fatG }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("TODAY'S ACCUMULATED INTAKE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${totalCalories.roundToInt()} kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroPill(label = "P", value = "${totalProtein.roundToInt()}g", color = Color(0xFFFF5722))
                    MacroPill(label = "C", value = "${totalCarbs.roundToInt()}g", color = Color(0xFF4CAF50))
                    MacroPill(label = "F", value = "${totalFats.roundToInt()}g", color = Color(0xFFFFEB3B))
                }
            }
        }
    }
}

@Composable
fun MacroPill(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
fun MealCategoryCard(
    categoryName: String,
    items: List<FoodLog>,
    onAddFood: () -> Unit,
    onDelete: (FoodLog) -> Unit,
    onToggleFav: (FoodLog) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val sectionTotalCalories = items.sumOf { it.calories }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = categoryName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Text(
                    text = "${sectionTotalCalories.roundToInt()} kcal",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (items.isEmpty()) {
                        Text(
                            "No foods logged yet. Click add below! 🌿",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items.forEach { food ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(food.foodName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onToggleFav(food) },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (food.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = null,
                                                    tint = if (food.isFavorite) Color.Red else Color.LightGray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "${food.quantity} • P: ${food.proteinG.roundToInt()}g • C: ${food.carbsG.roundToInt()}g • F: ${food.fatG.roundToInt()}g",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${food.calories.roundToInt()} kcal",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(onClick = { onDelete(food) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onAddFood,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Food to $categoryName")
                    }
                }
            }
        }
    }
}

// --- Text Search Dialog ---
@Composable
fun DialogTextSearch(
    mealType: String,
    uiState: UiState<FoodAnalysisResponse>,
    onSearch: (String) -> Unit,
    onConfirmAdd: (FoodAnalysisResponse) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini Nutri-Analyze") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("e.g., 2 scrambled eggs and 1 bread slice") },
                    label = { Text("What did you eat?") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onSearch(query) },
                    enabled = query.isNotBlank() && uiState !is UiState.Loading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Analyze with AI 🧠")
                }

                when (uiState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    is UiState.Error -> {
                        Text(uiState.message, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                    is UiState.Success -> {
                        val response = uiState.data
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Extracted Food Metrics:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Food Name: ${response.food_name}", fontWeight = FontWeight.Bold)
                                Text("Quantity: ${response.quantity}")
                                Text("Calories: ${response.calories.roundToInt()} kcal", fontWeight = FontWeight.Bold)
                                Text("Protein: ${response.protein_g.roundToInt()}g | Carbs: ${response.carbs_g.roundToInt()}g | Fats: ${response.fat_g.roundToInt()}g")
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (uiState is UiState.Success) {
                Button(onClick = { onConfirmAdd(uiState.data) }) {
                    Text("Add to Log")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Recipe Analyzer Dialog ---
@Composable
fun DialogRecipeAnalyze(
    mealType: String,
    uiState: UiState<RecipeAnalysisResponse>,
    onAnalyze: (String) -> Unit,
    onConfirmAdd: (RecipeAnalysisResponse) -> Unit,
    onDismiss: () -> Unit
) {
    var textBlock by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Recipe Ingredients Analyzer") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = textBlock,
                    onValueChange = { textBlock = it },
                    placeholder = { Text("Paste ingredients list line-by-line\ne.g.,\n2 cups Rice\n500g Chicken breast\n1 tbsp Olive oil") },
                    label = { Text("Recipe Ingredients block") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )

                Button(
                    onClick = { onAnalyze(textBlock) },
                    enabled = textBlock.isNotBlank() && uiState !is UiState.Loading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Analyze Recipe")
                }

                when (uiState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    is UiState.Error -> {
                        Text(uiState.message, color = Color.Red)
                    }
                    is UiState.Success -> {
                        val response = uiState.data
                        Card {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Recipe Values:", fontWeight = FontWeight.Bold)
                                Text("Calories: ${response.total_calories.roundToInt()} kcal")
                                Text("P: ${response.total_protein_g.roundToInt()}g | C: ${response.total_carbs_g.roundToInt()}g | F: ${response.total_fat_g.roundToInt()}g")
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (uiState is UiState.Success) {
                Button(onClick = { onConfirmAdd(uiState.data) }) {
                    Text("Add All Ingredients")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Camera Scan Dialog (CameraX integration) ---
@Composable
fun DialogCameraScan(
    mealType: String,
    uiState: UiState<CameraScanResponse>,
    onScanImage: (Bitmap) -> Unit,
    onConfirmAdd: (CameraScanResponse, Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isCapturing by remember { mutableStateOf(true) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    LaunchedEffect(isCapturing) {
        if (isCapturing) {
            capturedBitmap = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera AI Food Scanner") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCapturing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    imageCapture = ImageCapture.Builder().build()
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (e: Exception) {
                                        Log.e("CameraView", "Failed to bind camera use cases", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Capture Trigger button
                        IconButton(
                            onClick = {
                                val imageCap = imageCapture ?: return@IconButton
                                imageCap.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.capacity())
                                            buffer.get(bytes)
                                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            
                                            // Handle correct rotation (simple fallback scaling)
                                            val matrix = android.graphics.Matrix()
                                            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                                            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                            
                                            capturedBitmap = rotated
                                            isCapturing = false
                                            
                                            image.close()
                                            onScanImage(rotated)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            super.onError(exception)
                                            Log.e("CameraX", "Image capture failed", exception)
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .size(56.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = "Capture picture", tint = Color.Black)
                        }
                    }
                } else {
                    // Scanning state UI
                    when (uiState) {
                        is UiState.Loading -> {
                            CircularProgressIndicator()
                            Text("Gemini is cooking with Vision AI...")
                        }
                        is UiState.Error -> {
                            Text(uiState.message, color = Color.Red)
                            Button(onClick = { isCapturing = true }) {
                                Text("Retake Photo")
                            }
                        }
                        is UiState.Success -> {
                            val response = uiState.data
                            Text("Detected items in photo:", fontWeight = FontWeight.Bold)
                            
                            response.items.forEachIndexed { index, item ->
                                val isChecked = index in selectedIndices
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIndices = if (isChecked) selectedIndices - index else selectedIndices + index
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            selectedIndices = if (isChecked) selectedIndices - index else selectedIndices + index
                                        }
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("${item.food_name} (${item.estimated_quantity})", fontWeight = FontWeight.Bold)
                                        Text("${item.calories.roundToInt()} kcal • P:${item.protein_g.roundToInt()}g • C:${item.carbs_g.roundToInt()}g • F:${item.fat_g.roundToInt()}g")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Coach notes: ${response.notes}", fontSize = 11.sp, color = Color.Gray)
                        }
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            if (uiState is UiState.Success) {
                Button(onClick = { onConfirmAdd(uiState.data, selectedIndices) }) {
                    Text("Log Selected Items")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Manual Entry Dialog ---
@Composable
fun DialogManualEntry(
    mealType: String,
    onDismiss: () -> Unit,
    onSave: (name: String, cals: Double, prot: Double, carbs: Double, fat: Double, qty: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calsStr by remember { mutableStateOf("") }
    var protStr by remember { mutableStateOf("") }
    var carbsStr by remember { mutableStateOf("") }
    var fatStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Food Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it }, label = { Text("Quantity (e.g., 1 bowl)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = calsStr, onValueChange = { calsStr = it }, label = { Text("Calories (kcal)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protStr, onValueChange = { protStr = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbsStr, onValueChange = { carbsStr = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fatStr, onValueChange = { fatStr = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.ifBlank { "Logged Item" },
                        calsStr.toDoubleOrNull() ?: 0.0,
                        protStr.toDoubleOrNull() ?: 0.0,
                        carbsStr.toDoubleOrNull() ?: 0.0,
                        fatStr.toDoubleOrNull() ?: 0.0,
                        qtyStr
                    )
                },
                enabled = name.isNotBlank() && calsStr.isNotBlank()
            ) {
                Text("Confirm & Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
