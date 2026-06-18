package com.gohan.newsfeed

import com.gohan.newsfeed.manager.NewsFeedManager
import com.gohan.newsfeed.model.Category
import com.gohan.newsfeed.service.NewsService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewsFeedTest {
    private val service = NewsService()
    private val manager = NewsFeedManager()

    @Test
    fun `test flow emits correct count`() = runTest {
        val result = service.newsFeedFlow().take(3).toList()
        assertEquals(3, result.size)
    }

    @Test
    fun `test filter category logic`() = runTest {
        val techFlow = service.filterByCategory(service.newsFeedFlow().take(5), Category.TECHNOLOGY)
        val result = techFlow.toList()
        result.forEach { assert(it.contains("[TECHNOLOGY]")) }
    }

    @Test
    fun `test StateFlow increment`() = runTest {
        assertEquals(0, manager.readCount.value)
        manager.markAsRead()
        assertEquals(1, manager.readCount.value)
    }
}
