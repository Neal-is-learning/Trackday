package com.example.trackday.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

// Maps prototype icon keys → Material icons (closest visual match).
fun tagIcon(key: String): ImageVector = when (key) {
    "book"     -> Icons.AutoMirrored.Rounded.MenuBook
    "sun"      -> Icons.Rounded.WbSunny
    "bag"      -> Icons.Rounded.Work
    "globe"    -> Icons.Rounded.Public
    "heart"    -> Icons.Rounded.Favorite
    "star"     -> Icons.Rounded.Star
    "run"      -> Icons.Rounded.DirectionsRun
    "cup"      -> Icons.Rounded.LocalCafe
    "pen"      -> Icons.Rounded.Edit
    "music"    -> Icons.Rounded.MusicNote
    "cart"     -> Icons.Rounded.ShoppingCart
    "dumbbell" -> Icons.Rounded.FitnessCenter
    else       -> Icons.Rounded.Star
}
