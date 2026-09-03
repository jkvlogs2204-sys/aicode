package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Co2
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Feature keys for EcoMind application iconography mapping
 */
enum class EcoMindFeature {
    DASHBOARD,
    CARBON_TRACKER,
    WATER_METRICS,
    ENERGY_METRICS,
    STREAKS,
    ACHIEVEMENTS,
    COMMUNITY,
    FEED,
    HARDWARE,
    RFID_ZONES,
    CATALOG,
    AI_GUIDE,
    SETTINGS,
    ACCOUNT
}

/**
 * Utility mapper providing Material Symbols / Icons (Filled & Outlined)
 * based on selected state and feature domain.
 */
object EcoMindIconMapper {

    fun getIcon(feature: EcoMindFeature, isSelected: Boolean = true): ImageVector {
        return when (feature) {
            EcoMindFeature.DASHBOARD -> if (isSelected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
            EcoMindFeature.CARBON_TRACKER -> if (isSelected) Icons.Filled.Co2 else Icons.Outlined.Co2
            EcoMindFeature.WATER_METRICS -> if (isSelected) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop
            EcoMindFeature.ENERGY_METRICS -> if (isSelected) Icons.Filled.Bolt else Icons.Outlined.Bolt
            EcoMindFeature.STREAKS -> if (isSelected) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment
            EcoMindFeature.ACHIEVEMENTS -> if (isSelected) Icons.Filled.MilitaryTech else Icons.Outlined.MilitaryTech
            EcoMindFeature.COMMUNITY -> if (isSelected) Icons.Filled.Groups else Icons.Outlined.Groups
            EcoMindFeature.FEED -> if (isSelected) Icons.Filled.Forum else Icons.Outlined.Forum
            EcoMindFeature.HARDWARE -> if (isSelected) Icons.Filled.Memory else Icons.Outlined.Memory
            EcoMindFeature.RFID_ZONES -> if (isSelected) Icons.Filled.Sensors else Icons.Outlined.Sensors
            EcoMindFeature.CATALOG -> if (isSelected) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
            EcoMindFeature.AI_GUIDE -> if (isSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
            EcoMindFeature.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
            EcoMindFeature.ACCOUNT -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
        }
    }
}
