package com.gohan.newsfeed.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NewsFeedManager {
    private val _readCount = MutableStateFlow(0)
    
    // StateFlow read-only untuk diekspos ke UI/Main
    val readCount: StateFlow<Int> = _readCount.asStateFlow()

    fun markAsRead() {
        _readCount.value += 1
    }
}
