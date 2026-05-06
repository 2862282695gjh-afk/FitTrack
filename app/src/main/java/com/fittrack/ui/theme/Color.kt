package com.fittrack.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// FitTrack 2.0 — Lovable 风格清新配色系统
// 基于 oklch 色彩空间，对齐 ai-fitness-coach-98 设计稿
// ═══════════════════════════════════════════════════════════════

// ── 主色：清新绿（Lovable primary: oklch(0.72 0.19 145)）────
val FitGreen = Color(0xFF34A870)
val FitGreenLight = Color(0xFF5EC99A)
val FitGreenDark = Color(0xFF268A58)
val FitGreenContainer = Color(0xFFD4F0E2)

// ── 主色发光态（Lovable primary-glow: oklch(0.82 0.17 150)）──
val FitGreenGlow = Color(0xFF6ED8A4)

// ── 辅色：柔和蓝（保留用于次要信息）─────────────────────────
val FitBlue = Color(0xFF4A9BD9)
val FitBlueLight = Color(0xFF7BB8E8)
val FitBlueContainer = Color(0xFFD6EBFF)

// ── 强调色：暖橙（强度标签、警告、CTA）──────────────────────
val FitOrange = Color(0xFFE8864A)
val FitOrangeLight = Color(0xFFF0A878)
val FitOrangeContainer = Color(0xFFFFE8DE)

// ── 强调色：亮黄（成就、金币、经验值）──────────────────────────
val FitYellow = Color(0xFFE8C840)
val FitYellowDark = Color(0xFFC4A620)
val FitYellowContainer = Color(0xFFFFF5D6)

// ── 强调色：淡紫（AI 功能入口）───────────────────────────────
val FitPurple = Color(0xFF7B5FCC)
val FitPurpleLight = Color(0xFFA88AE0)
val FitPurpleContainer = Color(0xFFEDE5FF)

// ── 错误/危险色 ───────────────────────────────────────────────
val FitRed = Color(0xFFE85454)
val FitRedContainer = Color(0xFFFFE2E2)

// ── 中性色（Lovable 风格：暖灰调）────────────────────────────
val FitBackground = Color(0xFFFAFBF9)
val FitSurface = Color(0xFFFFFFFF)
val FitOnSurface = Color(0xFF2A3230)
val FitOnSurfaceVariant = Color(0xFF5E6E68)
val FitOutline = Color(0xFFE0E8E4)
val FitOutlineVariant = Color(0xFFD0DAD5)

// ── 渐变预设（Lovable 风格：单色系 135° 渐变）────────────────
val GradientPrimary = listOf(FitGreen, FitGreenGlow)
val GradientHero = listOf(FitGreen, FitBlue)
val GradientBlue = listOf(FitBlue, FitBlueLight)
val GradientOrange = listOf(FitOrange, FitOrangeLight)
val GradientYellow = listOf(FitYellow, Color(0xFFF0DC70))
val GradientPurple = listOf(FitPurple, FitPurpleLight)

// ── 阴影预设（Lovable 风格：柔和扩散阴影）────────────────────
val ShadowGlow = Color(0x6634A870)    // primary 40% — 用于 AI 按钮、FAB 的发光
val ShadowCard = Color(0x142A3230)    // onSurface 8% — 用于普通卡片的浮起
val ShadowCardHover = Color(0x332A3230) // onSurface 20% — 用于卡片 hover

// ── 深色模式适配 ──────────────────────────────────────────────
val FitDarkBackground = Color(0xFF121816)
val FitDarkSurface = Color(0xFF1C2422)
val FitDarkOnSurface = Color(0xFFE4EAE8)
val FitDarkOutline = Color(0xFF343E3A)
