package com.example.trackday.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide UI signals that need to cross the NavHost boundary.
 * The check-in popup is hosted at the app root (so it can cover the bottom
 * nav like a real alarm), but is triggered from inside the timeline screen.
 */
object AppUiState {
    var checkInVisible by mutableStateOf(false)
        private set

    fun openCheckIn() { checkInVisible = true }
    fun closeCheckIn() { checkInVisible = false }
}
