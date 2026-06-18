package com.gohan.newsfeed.service

import com.gohan.newsfeed.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class NewsService {
    private val _articles = MutableStateFlow<List<NewsArticle>>(listOf(
        NewsArticle(1, "Inovasi AI 2024", Category.TECHNOLOGY, "Konten AI...", System.currentTimeMillis()),
        NewsArticle(2, "Final Liga Champions", Category.SPORTS, "Hasil pertandingan...", System.currentTimeMillis()),
        NewsArticle(3, "Pemilu Serentak", Category.POLITICS, "Berita politik...", System.currentTimeMillis()),
        NewsArticle(4, "Tips Hidup Sehat", Category.HEALTH, "Cara menjaga kesehatan...", System.currentTimeMillis()),
        NewsArticle(5, "Robotika Masa Depan", Category.TECHNOLOGY, "Kemajuan robot...", System.currentTimeMillis()),
        NewsArticle(6, "Bursa Transfer Pemain", Category.SPORTS, "Update transfer...", System.currentTimeMillis()),
        NewsArticle(7, "Kebijakan Ekonomi Baru", Category.POLITICS, "Analisis ekonomi...", System.currentTimeMillis()),
        NewsArticle(8, "Vaksin Generasi Baru", Category.HEALTH, "Info medis...", System.currentTimeMillis()),
        NewsArticle(9, "Gadget Terbaru Diluncurkan", Category.TECHNOLOGY, "Review gadget...", System.currentTimeMillis()),
        NewsArticle(10, "Turnamen Badminton", Category.SPORTS, "Jadwal tanding...", System.currentTimeMillis())
    ))

    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()

    private val _newArticlesFlow = MutableSharedFlow<NewsArticle>()

    /**
     * Memancarkan artikel baru setiap 2 detik secara bertahap, 
     * dan juga memancarkan artikel yang baru ditambahkan secara real-time.
     */
    fun newsFeedFlow(): Flow<NewsArticle> = flow {
        // Emit existing articles as a simulation
        val currentList = _articles.value
        currentList.forEach { article ->
            delay(2000)
            emit(article)
        }
        // Then keep the flow open for new articles added via addArticle
        emitAll(_newArticlesFlow)
    }.catch { e ->
        println("Error pada Flow: ${e.message}")
    }

    /**
     * Menambahkan artikel baru ke dalam sistem.
     */
    suspend fun addArticle(title: String, category: Category, content: String) {
        val newId = (_articles.value.maxOfOrNull { it.id } ?: 0) + 1
        val newArticle = NewsArticle(newId, title, category, content, System.currentTimeMillis())
        _articles.value = _articles.value + newArticle
        _newArticlesFlow.emit(newArticle)
    }

    /**
     * Memfilter artikel berdasarkan kategori dan mentransformasinya menjadi String terformat.
     * (Requirement 2 & 3)
     */
    fun filterByCategory(flow: Flow<NewsArticle>, category: Category): Flow<NewsArticle> {
        return flow
            .filter { it.category == category }
            .onEach { println("[LOG] Processing: ${it.title}") }
    }

    /**
     * Transformasi data artikel menjadi string untuk logging/display sederhana.
     * (Requirement 3)
     */
    fun transformToDisplay(article: NewsArticle): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return "[${article.category}] ${article.title} (${sdf.format(Date(article.publishedAt))})"
    }

    /**
     * Mengambil detail artikel dengan simulasi delay network.
     */
    suspend fun fetchArticleDetail(id: Int): NewsResult<NewsArticle> {
        return try {
            delay(500)
            val article = _articles.value.find { it.id == id }
            if (article != null) NewsResult.Success(article) 
            else NewsResult.Error("Artikel ID $id tidak ditemukan")
        } catch (e: Exception) {
            NewsResult.Error("Terjadi kesalahan sistem: ${e.message}")
        }
    }
}
