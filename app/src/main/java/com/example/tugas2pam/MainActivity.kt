package com.example.tugas2pam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tugas2pam.ui.theme.Tugas2pamTheme
import com.gohan.newsfeed.manager.NewsFeedManager
import com.gohan.newsfeed.model.Category
import com.gohan.newsfeed.model.NewsArticle
import com.gohan.newsfeed.model.NewsResult
import com.gohan.newsfeed.service.NewsService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val newsService = NewsService()
    private val newsManager = NewsFeedManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Tugas2pamTheme {
                NewsFeedApp(newsService, newsManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedApp(newsService: NewsService, newsManager: NewsFeedManager) {
    val scope = rememberCoroutineScope()
    val articles = remember { mutableStateListOf<NewsArticle>() }
    val readCount by newsManager.readCount.collectAsState()
    
    var selectedCategory by remember { mutableStateOf(Category.TECHNOLOGY) }
    var isFetchingDetail by remember { mutableStateOf(false) }
    var detailMessage by remember { mutableStateOf<String?>(null) }
    var showDetailDialog by remember { mutableStateOf<NewsArticle?>(null) }
    var showAddArticleDialog by remember { mutableStateOf(false) }

    // Re-start feed when category changes
    LaunchedEffect(selectedCategory) {
        articles.clear()
        // Menggunakan filterByCategory dari NewsService (Requirement 2)
        newsService.filterByCategory(newsService.newsFeedFlow(), selectedCategory)
            .onEach { article ->
                articles.add(0, article)
                newsManager.markAsRead() // Update StateFlow (Requirement 4)
            }
            .collect()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("News Feed Simulator", style = MaterialTheme.typography.titleLarge)
                        Text("Reactive & Async Demo", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isFetchingDetail = true
                            val ids = listOf(1, 2, 5)
                            val results = ids.map { async { newsService.fetchArticleDetail(it) } }.awaitAll()
                            val successCount = results.count { it is NewsResult.Success }
                            detailMessage = "Parallel Fetch Complete: $successCount items retrieved."
                            isFetchingDetail = false
                        }
                    }) {
                        if (isFetchingDetail) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Parallel Fetch")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddArticleDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Article")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            DeveloperHeader()

            SummarySection(readCount = readCount)

            CategorySelector(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            AnimatedVisibility(visible = detailMessage != null) {
                detailMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth().clickable { detailMessage = null }
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                text = "Live ${selectedCategory.name} Feed",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp), // Extra bottom padding for FAB
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(articles, key = { it.id }) { article ->
                    NewsCard(
                        article = article,
                        displayFormat = newsService.transformToDisplay(article), // Requirement 3
                        onClick = {
                            scope.launch {
                                // Mengambil detail asinkron (Requirement 5)
                                val result = newsService.fetchArticleDetail(article.id)
                                if (result is NewsResult.Success) {
                                    showDetailDialog = result.data
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Article Dialog
    if (showAddArticleDialog) {
        AddArticleDialog(
            onDismiss = { showAddArticleDialog = false },
            onConfirm = { title, category, content ->
                scope.launch {
                    newsService.addArticle(title, category, content)
                    showAddArticleDialog = false
                }
            }
        )
    }

    // Detail Dialog
    if (showDetailDialog != null) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text(showDetailDialog?.title ?: "") },
            text = { Text(showDetailDialog?.content ?: "") },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DeveloperHeader() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pengembang:", style = MaterialTheme.typography.labelLarge)
            Text("Gohan Tua Jeremia Ambarita", fontWeight = FontWeight.Bold)
            Text("NIM: 123140160", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SummarySection(readCount: Int) {
    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Artikel Dibaca", color = Color.White)
            Text(
                text = readCount.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun CategorySelector(selectedCategory: Category, onCategorySelected: (Category) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedCategory.ordinal,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {}
    ) {
        Category.entries.forEach { category ->
            Tab(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                text = { Text(category.name) }
            )
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle, displayFormat: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = displayFormat, // Menggunakan hasil transformasi (Requirement 3)
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddArticleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Category, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.TECHNOLOGY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Artikel Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Kategori:", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Category.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Konten") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, selectedCategory, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Tugas2pamTheme {
        NewsFeedApp(NewsService(), NewsFeedManager())
    }
}