package cn.lemwood.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardRadius = 12.dp
val ChipRadius = 4.dp
val DialogRadius = 16.dp
val ButtonRadius = 20.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(ChipRadius),
    small = RoundedCornerShape(ChipRadius),
    medium = RoundedCornerShape(CardRadius),
    large = RoundedCornerShape(DialogRadius),
    extraLarge = RoundedCornerShape(ButtonRadius),
)
