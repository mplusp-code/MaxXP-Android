package com.relay.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Default Dark Palette (Concept v2)
val DefaultBackgroundDark = Color(0xFF121214)
val DefaultSurfaceDark = Color(0xFF16161A)
val DefaultCardDark = Color(0xFF1C1C20)
val DefaultCardElevatedDark = Color(0xFF24242A)
val DefaultBorderDark = Color(0xFF2E2E36)

val DefaultTextPrimaryDark = Color(0xFFFFFFFF)
val DefaultTextSecondaryDark = Color(0xFF9E9EA8)
val DefaultTextMutedDark = Color(0xFF6C6C76)

// Default Light Palette
val DefaultBackgroundLight = Color(0xFFF4F4F6)
val DefaultSurfaceLight = Color(0xFFFFFFFF)
val DefaultCardLight = Color(0xFFEAEAEE)
val DefaultCardElevatedLight = Color(0xFFE2E2E8)
val DefaultBorderLight = Color(0xFFD4D4D8)

val DefaultTextPrimaryLight = Color(0xFF18181B)
val DefaultTextSecondaryLight = Color(0xFF71717A)
val DefaultTextMutedLight = Color(0xFFA1A1AA)

// Dynamic Color Scheme for CompositionLocal
data class RelayColorScheme(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color
)

val LocalRelayColors = staticCompositionLocalOf {
    RelayColorScheme(
        background = DefaultBackgroundDark,
        surface = DefaultSurfaceDark,
        card = DefaultCardDark,
        cardElevated = DefaultCardElevatedDark,
        border = DefaultBorderDark,
        textPrimary = DefaultTextPrimaryDark,
        textSecondary = DefaultTextSecondaryDark,
        textMuted = DefaultTextMutedDark,
        accent = Color(0xFF70E000)
    )
}

// Reactive Composable Getters reflecting active theme mode and accent color
val BackgroundDark: Color
    @Composable
    get() = LocalRelayColors.current.background

val SurfaceDark: Color
    @Composable
    get() = LocalRelayColors.current.surface

val CardDark: Color
    @Composable
    get() = LocalRelayColors.current.card

val CardElevated: Color
    @Composable
    get() = LocalRelayColors.current.cardElevated

val BorderDark: Color
    @Composable
    get() = LocalRelayColors.current.border

val TextPrimary: Color
    @Composable
    get() = LocalRelayColors.current.textPrimary

val TextSecondary: Color
    @Composable
    get() = LocalRelayColors.current.textSecondary

val TextMuted: Color
    @Composable
    get() = LocalRelayColors.current.textMuted

val AccentLimeGreen: Color
    @Composable
    get() = LocalRelayColors.current.accent

// Concept v2 Neon / Vibrant Accents
val AccentGreen = Color(0xFF4ADE80)
val AccentTeal = Color(0xFF2DD4BF)
val AccentBlue = Color(0xFF38BDF8)
val AccentOrange = Color(0xFFFB923C)

// Attention / Warning Colors
val AmberAttention = Color(0xFFF59E0B)
val AmberAttentionBg = Color(0xFF2A2211)
val AmberAttentionBorder = Color(0xFF78350F)
val AmberAttentionText = Color(0xFFFBBF24)

// Service Badge & Card Tint Colors (Matching PDF Concept v2)
val WhatsAppGreen = Color(0xFF16A34A)
val WhatsAppCardBg = Color(0xFF103823)

val TelegramBlue = Color(0xFF0284C7)
val TelegramCardBg = Color(0xFF0E2F4A)

val InstagramMagenta = Color(0xFFD946EF)
val InstagramCardBg = Color(0xFF3B122C)

val FacebookBlue = Color(0xFF2563EB)
val FacebookCardBg = Color(0xFF132747)

val ViberPurple = Color(0xFF7C3AED)
val ViberCardBg = Color(0xFF25143A)

val GoogleAmber = Color(0xFFEA580C)
val GoogleCardBg = Color(0xFF3A1E0B)

// Legacy / Compatibility Constants
val DarkGlossHeaderTop: Color @Composable get() = SurfaceDark
val DarkGlossHeaderBottom: Color @Composable get() = BackgroundDark
val AccentGreenHighlight: Color @Composable get() = AccentLimeGreen
val GlossWhiteAlpha35 = Color(0x33FFFFFF)
val GlossWhiteAlpha20 = Color(0x1AFFFFFF)
val BevelShadowAlpha60 = Color(0x66000000)

val SoftkeyBackgroundTop: Color @Composable get() = SurfaceDark
val SoftkeyBackgroundBottom: Color @Composable get() = BackgroundDark

val AlertWarningOrange = AmberAttention
val AlertWarningDark = Color(0xFFB45309)

val ItemHighlightGreen = Color(0xFF1F3818)
val ItemBackgroundNormal: Color @Composable get() = CardDark

object RelayColors {
    val Background: Color @Composable get() = BackgroundDark
    val Surface: Color @Composable get() = SurfaceDark
    val Card: Color @Composable get() = CardDark
    val CardElevatedBg: Color @Composable get() = CardElevated
    val Border: Color @Composable get() = BorderDark

    val TextHigh: Color @Composable get() = TextPrimary
    val TextMedium: Color @Composable get() = TextSecondary
    val TextLow: Color @Composable get() = TextMuted

    val AccentPrimary: Color @Composable get() = AccentLimeGreen
    val AccentAltGreen = AccentGreen
    val AccentTealColor = AccentTeal
    val AccentBlueColor = AccentBlue
    val AccentOrangeColor = AccentOrange

    val AttentionAmber = AmberAttention
    val AttentionBg = AmberAttentionBg
    val AttentionBorder = AmberAttentionBorder
    val AttentionText = AmberAttentionText

    val BadgeWhatsApp = WhatsAppGreen
    val BadgeTelegram = TelegramBlue
    val BadgeInstagram = InstagramMagenta
    val BadgeFacebook = FacebookBlue
    val BadgeViber = ViberPurple
    val BadgeGoogleMessages = GoogleAmber
    val StatusWarningAmber = AmberAttention

    val WhatsAppBg = WhatsAppCardBg
    val TelegramBg = TelegramCardBg
    val InstagramBg = InstagramCardBg
    val FacebookBg = FacebookCardBg
    val ViberBg = ViberCardBg
    val GoogleBg = GoogleCardBg
}
