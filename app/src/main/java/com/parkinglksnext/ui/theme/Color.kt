package com.parkinglksnext.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary brand (Parkly orange) ──────────────────────────
val ParklyOrange      = Color(0xFFFF6B2B)
val ParklyOrangeLight = Color(0xFFFFF0EA)
val ParklyOrangeDark  = Color(0xFFD95315)

// ── Backgrounds ───────────────────────────────────────────
val ParklyBackground  = Color(0xFFF7F8FC)
val ParklySurface     = Color(0xFFFFFFFF)

// ── Text ──────────────────────────────────────────────────
val ParklyTextPrimary   = Color(0xFF1A1A2E)
val ParklyTextSecondary = Color(0xFF8B8FA8)

// ── Status ────────────────────────────────────────────────
val ParklyGreen      = Color(0xFF22C55E)
val ParklyGreenLight = Color(0xFFDCFCE7)
val ParklyRed        = Color(0xFFEF4444)
val ParklyRedLight   = Color(0xFFFEE2E2)
val ParklyGrayLight  = Color(0xFFF3F4F6)

// ── Parking spot ──────────────────────────────────────────
val SpotAvailable = Color(0xFF22C55E)
val SpotTaken     = Color(0xFF9CA3AF)
val SpotSelected  = Color(0xFFFF6B2B)

// ── Legacy aliases (keeps existing code compiling) ────────
val LksOrange        = ParklyOrange
val LksWhite         = Color(0xFFFFFFFF)
val LksBackground    = ParklyBackground
val LksSurface       = ParklySurface
val LksTextPrimary   = ParklyTextPrimary
val LksTextSecondary = ParklyTextSecondary
val LksError         = ParklyRed