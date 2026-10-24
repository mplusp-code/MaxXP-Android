package com.relay.app.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Dark Minimalist Palette (Concept v2)
val BackgroundDark = Color(0xFF121214)
val SurfaceDark = Color(0xFF16161A)
val CardDark = Color(0xFF1C1C20)
val CardElevated = Color(0xFF24242A)
val BorderDark = Color(0xFF2E2E36)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9E9EA8)
val TextMuted = Color(0xFF6C6C76)

// Concept v2 Neon / Vibrant Accents
val AccentLimeGreen = Color(0xFF70E000)
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
val DarkGlossHeaderTop = SurfaceDark
val DarkGlossHeaderBottom = BackgroundDark
val AccentGreenHighlight = AccentLimeGreen
val GlossWhiteAlpha35 = Color(0x33FFFFFF)
val GlossWhiteAlpha20 = Color(0x1AFFFFFF)
val BevelShadowAlpha60 = Color(0x66000000)

val SoftkeyBackgroundTop = SurfaceDark
val SoftkeyBackgroundBottom = BackgroundDark

val AlertWarningOrange = AmberAttention
val AlertWarningDark = Color(0xFFB45309)

val ItemHighlightGreen = Color(0xFF1F3818)
val ItemBackgroundNormal = CardDark

object RelayColors {
    val Background = BackgroundDark
    val Surface = SurfaceDark
    val Card = CardDark
    val CardElevatedBg = CardElevated
    val Border = BorderDark

    val TextHigh = TextPrimary
    val TextMedium = TextSecondary
    val TextLow = TextMuted

    val AccentPrimary = AccentLimeGreen
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
