package com.gohan.newsfeed

import com.gohan.newsfeed.manager.NewsFeedManager
import com.gohan.newsfeed.model.*
import com.gohan.newsfeed.service.NewsService
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    println("==============================================")
    println("        NEWS FEED SIMULATOR KOTLIN/JVM        ")
    println("==============================================")
    println("Nama : Gohan Tua Jeremia Ambarita")
    println("NIM  : 123140160")
    println("==============================================\n")

    val newsService = NewsService()
    val newsManager = NewsFeedManager()

    // 1 & 2. Simulasi Flow & Operators (Berjalan di background)
    val feedJob = launch {
        println("[UI] Memulai News Feed (Kategori: TECHNOLOGY)...\n")
        newsService.filterByCategory(newsService.newsFeedFlow(), Category.TECHNOLOGY)
            .collect { displayString ->
                println(">>> UI Update: $displayString")
                newsManager.markAsRead()
                println(">>> Total Artikel Dibaca: ${newsManager.readCount.value}")
            }
    }

    // Tunggu agar flow berjalan sebentar
    delay(5000)

    // 4. Coroutines Paralel (Async/Await)
    println("\n--- Menjalankan Fetch Detail Secara Paralel ---")
    val ids = listOf(1, 2, 5)
    val time = measureTimeMillis {
        val jobs = ids.map { id ->
            async { newsService.fetchArticleDetail(id) }
        }
        
        val results = jobs.awaitAll()
        results.forEach { res ->
            if (res is NewsResult.Success) println("Berhasil Fetch: ${res.data.title}")
        }
    }
    println("Total Waktu Fetch Paralel: $time ms")

    delay(2000)
    feedJob.cancel()
    println("\nSimulasi Selesai.")
}
