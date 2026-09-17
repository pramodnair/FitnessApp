package com.example.fitnessapp.ui.foodsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import com.example.fitnessapp.data.nutrition.SavedMealCombo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.data.model.MealType
import com.example.fitnessapp.data.nutrition.FoodDatabase
import com.example.fitnessapp.data.nutrition.FoodItemDefinition
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBarcodeScan: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FoodSearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val plateItems by viewModel.plateItems.collectAsStateWithLifecycle()
    val selectedMealType by viewModel.selectedMealType.collectAsStateWithLifecycle()
    val plateTotals by viewModel.plateNutritionTotals.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val recentFoods by viewModel.recentFoods.collectAsStateWithLifecycle()
    val savedCombos by viewModel.savedCombos.collectAsStateWithLifecycle()
    val yesterdayMeals by viewModel.yesterdayMealsForSelectedType.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showMealTypeMenu by remember { mutableStateOf(false) }
    var isPlateExpanded by remember { mutableStateOf(true) }
    var showSentenceParserDialog by remember { mutableStateOf(false) }
    var itemForCustomQty by remember { mutableStateOf<PlateItem?>(null) }
    var showSaveComboDialog by remember { mutableStateOf(false) }
    var comboNameInput by remember { mutableStateOf("") }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Plate Builder & Search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search dishes & adjust portion macros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Meal Type selector
                    Box {
                        OutlinedButton(
                            onClick = { showMealTypeMenu = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(selectedMealType.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showMealTypeMenu,
                            onDismissRequest = { showMealTypeMenu = false }
                        ) {
                            MealType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label, fontWeight = if (type == selectedMealType) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        viewModel.onMealTypeSelected(type)
                                        showMealTypeMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & AI actions
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search dishes (e.g. Roti, Dal, Paneer)") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Barcode Scanner Button
                        IconButton(
                            onClick = onNavigateToBarcodeScan,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        // AI Sentence Parser Button
                        IconButton(
                            onClick = { showSentenceParserDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Meal Parser",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (isAiLoading) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    // Copy Yesterday's Meal Banner (if available for selected meal type)
                    if (yesterdayMeals.isNotEmpty()) {
                        val yesterdayItemCount = yesterdayMeals.sumOf { it.items.size }
                        val yesterdayCalories = yesterdayMeals.sumOf { it.calories }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Yesterday's ${selectedMealType.label}: $yesterdayItemCount items ($yesterdayCalories kcal)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = { viewModel.copyYesterdayMealsToPlate() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Copy to Plate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories Horizontal Row
                    val scrollState = rememberScrollState()
                    val allCategories = remember {
                        listOf("All", "⚡ Recent", "🍱 My Combos") + FoodDatabase.categories.filter { it != "All" }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allCategories.forEach { category ->
                            val isSelected = selectedCategory.equals(category, ignoreCase = true)
                            val countBadge = when (category) {
                                "⚡ Recent" -> if (recentFoods.isNotEmpty()) " (${recentFoods.size})" else ""
                                "🍱 My Combos" -> if (savedCombos.isNotEmpty()) " (${savedCombos.size})" else ""
                                else -> ""
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onCategorySelected(category) },
                                label = { Text("$category$countBadge", fontSize = 12.sp) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Plate summary card (Sticky at top if items exist)
            AnimatedVisibility(
                visible = plateItems.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { isPlateExpanded = !isPlateExpanded }
                            ) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Your Plate (${plateTotals.itemCount} items)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isPlateExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        comboNameInput = ""
                                        showSaveComboDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkAdd,
                                        contentDescription = "Save as Combo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(
                                    onClick = { viewModel.clearPlate() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Expanded Plate Items List
                        AnimatedVisibility(visible = isPlateExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                plateItems.forEach { plateItem ->
                                    PlateItemRow(
                                        plateItem = plateItem,
                                        onIncrement = { viewModel.incrementPlateItem(plateItem.food.id) },
                                        onDecrement = { viewModel.decrementPlateItem(plateItem.food.id) },
                                        onDelete = { viewModel.removeFromPlate(plateItem.food.id) },
                                        onCustomQtyClick = { itemForCustomQty = plateItem }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Nutrition Metrics Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${plateTotals.calories}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("kcal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            NutritionMiniStat(label = "Protein", value = "${plateTotals.proteinG}g", color = Color(0xFF10B981))
                            NutritionMiniStat(label = "Carbs", value = "${plateTotals.carbsG}g", color = Color(0xFFF59E0B))
                            NutritionMiniStat(label = "Fat", value = "${plateTotals.fatG}g", color = Color(0xFFEF4444))
                            NutritionMiniStat(label = "Fiber", value = "${plateTotals.fiberG}g", color = Color(0xFF8B5CF6))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Log Meal CTA Button
                        Button(
                            onClick = {
                                viewModel.logPlateAsMeal { count, cal ->
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log to ${selectedMealType.label} (${plateTotals.calories} kcal)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Food Search Results Header & List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val countText = when (selectedCategory) {
                        "🍱 My Combos" -> "${savedCombos.size} combos"
                        "⚡ Recent" -> "${searchResults.size} items"
                        else -> "${searchResults.size} items"
                    }
                    val titleText = when (selectedCategory) {
                        "🍱 My Combos" -> "My Saved Combos"
                        "⚡ Recent" -> "Recently Logged Foods"
                        else -> if (searchQuery.isBlank()) "Popular $selectedCategory Foods" else "Results for \"$searchQuery\""
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = countText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedCategory == "🍱 My Combos") {
                    if (savedCombos.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No Saved Combos Yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Build a plate with multiple foods (e.g. 2 Rotis + Dal + Salad) and tap the bookmark icon on your plate to save it as a 1-tap reusable combo!",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(savedCombos, key = { it.id }) { combo ->
                            SavedMealComboCard(
                                combo = combo,
                                onAdd = { viewModel.addComboToPlate(combo) },
                                onDelete = { viewModel.deleteCombo(combo.id) }
                            )
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Fastfood,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (selectedCategory == "⚡ Recent") "No recent foods yet" else "No local matches for \"$searchQuery\"",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (selectedCategory == "⚡ Recent") "Foods you log will automatically appear here for fast 1-tap re-logging."
                                    else "Search online using Gemini AI to fetch nutrition info and save it to your database.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (selectedCategory != "⚡ Recent") {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { viewModel.searchWithGeminiAi() },
                                            enabled = !isAiLoading && searchQuery.isNotBlank(),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Search with AI", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.parseNaturalLanguageMeal(searchQuery)
                                                } else {
                                                    showSentenceParserDialog = true
                                                }
                                            },
                                            enabled = !isAiLoading,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Parse to Plate", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(searchResults, key = { it.id }) { food ->
                        val plateItem = plateItems.firstOrNull { it.food.id == food.id || it.food.name.equals(food.name, ignoreCase = true) }
                        FoodSearchResultCard(
                            food = food,
                            plateItem = plateItem,
                            onAdd = { viewModel.addToPlate(food, 1.0f) },
                            onIncrement = { viewModel.incrementPlateItem(food.id) },
                            onDecrement = { viewModel.decrementPlateItem(food.id) }
                        )
                    }

                    // Online AI Fallback Card at bottom of list
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Looking for something else?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Query Gemini AI for custom or regional dishes.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.searchWithGeminiAi() },
                                    enabled = !isAiLoading && searchQuery.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Search", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // AI Natural Language Meal Sentence Parser Dialog
    if (showSentenceParserDialog) {
        var sentenceText by remember { mutableStateOf(searchQuery) }
        AlertDialog(
            onDismissRequest = { showSentenceParserDialog = false },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Smart Meal Parser", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Type your whole meal naturally and AI will break it down into items and portions on your plate.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sentenceText,
                        onValueChange = { sentenceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 2 rotis, 1 bowl dal tadka and 1 cup dahi") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSentenceParserDialog = false
                        viewModel.parseNaturalLanguageMeal(sentenceText)
                    },
                    enabled = sentenceText.isNotBlank()
                ) {
                    Text("Parse & Add to Plate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSentenceParserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save Plate as Combo Dialog
    if (showSaveComboDialog) {
        AlertDialog(
            onDismissRequest = { showSaveComboDialog = false },
            icon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Save as Meal Combo", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Save this plate (${plateItems.size} items • ${plateTotals.calories} kcal) as a reusable combo for 1-tap logging.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = comboNameInput,
                        onValueChange = { comboNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Daily Lunch Thali, Post-Workout Shake") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentPlateAsCombo(comboNameInput)
                        showSaveComboDialog = false
                    },
                    enabled = comboNameInput.isNotBlank()
                ) {
                    Text("Save Combo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveComboDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom Quantity Stepper / Direct Input Dialog
    itemForCustomQty?.let { item ->
        var qtyText by remember {
            mutableStateOf(
                if (item.quantity % 1f == 0f) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
            )
        }
        AlertDialog(
            onDismissRequest = { itemForCustomQty = null },
            title = { Text("Adjust Quantity", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "${item.food.name} (${item.food.servingSizeDescription})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Quantity in ${item.food.servingUnit}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = qtyText.toFloatOrNull()
                        if (parsed != null && parsed > 0f) {
                            viewModel.updatePlateItemQuantity(item.food.id, parsed)
                        }
                        itemForCustomQty = null
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForCustomQty = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FoodSearchResultCard(
    food: FoodItemDefinition,
    plateItem: PlateItem?,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plateItem != null)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = food.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (food.isCustomOrAi) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${food.servingSizeDescription} • ${food.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MacroChip(label = "${food.calories} kcal", isPrimary = true)
                    MacroChip(label = "P: ${food.proteinG}g", color = Color(0xFF10B981))
                    MacroChip(label = "C: ${food.carbsG}g", color = Color(0xFFF59E0B))
                    MacroChip(label = "F: ${food.fatG}g", color = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action: Add button or Quantity stepper
            if (plateItem != null) {
                val qtyStr = if (plateItem.quantity % 1f == 0f) plateItem.quantity.toInt().toString() else "%.1f".format(plateItem.quantity)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = qtyStr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onAdd,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlateItemRow(
    plateItem: PlateItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
    onCustomQtyClick: () -> Unit
) {
    val scaled = plateItem.scaledFoodItem
    val qtyStr = if (plateItem.quantity % 1f == 0f) plateItem.quantity.toInt().toString() else "%.1f".format(plateItem.quantity)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plateItem.food.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${scaled.calories} kcal • ${scaled.proteinG}g P • ${scaled.carbsG}g C • ${scaled.fatG}g F",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Stepper with clickable quantity label for direct typing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 2.dp)
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(14.dp))
            }

            Text(
                text = "$qtyStr ${plateItem.food.servingUnit}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onCustomQtyClick() }
                    .padding(horizontal = 6.dp)
            )

            IconButton(onClick = onIncrement, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(14.dp))
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NutritionMiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MacroChip(label: String, isPrimary: Boolean = false, color: Color? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isPrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else (color ?: MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.15f)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else (color ?: MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun SavedMealComboCard(
    combo: SavedMealCombo,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = combo.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${combo.items.size} items • ${combo.totalCalories} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Combo",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onAdd,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val itemsSummary = combo.items.joinToString(", ") {
                val qtyStr = if (it.quantity % 1f == 0f) it.quantity.toInt().toString() else "%.1f".format(it.quantity)
                "$qtyStr ${it.foodName}"
            }
            Text(
                text = itemsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroChip("P: ${combo.totalProtein}g", color = Color(0xFF64B5F6))
                MacroChip("C: ${combo.totalCarbs}g", color = Color(0xFF81C784))
                MacroChip("F: ${combo.totalFat}g", color = Color(0xFFFFB74D))
            }
        }
    }
}
