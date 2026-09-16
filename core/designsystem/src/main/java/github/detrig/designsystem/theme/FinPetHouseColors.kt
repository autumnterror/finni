package github.detrig.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Материалы рисованного дома; заменяются вместе с брендбуком. */
@Immutable
data class FinPetHouseColors(
    val playroomWall: Color = Color(0xFFD4EFF1),
    val bedroomWall: Color = Color(0xFFD2E79F),
    val hallWall: Color = Color(0xFFFFF2BC),
    val kitchenWall: Color = Color(0xFFE5EBC2),
    val wallPattern: Color = Color(0xFFB7DFDF),
    val hallStripe: Color = Color(0xFFF6E5A0),
    val floor: Color = Color(0xFFF2DE80),
    val skirting: Color = Color(0xFFB5A24F),
    val partition: Color = Color(0xFFFFF5CA),
    val outline: Color = Color(0xFF806F4A),
    val wood: Color = Color(0xFFD6AC67),
    val woodLight: Color = Color(0xFFE9CB8F),
    val woodShade: Color = Color(0xFFBA8C50),
    val fabric: Color = Color(0xFF66B8D3),
    val fabricLight: Color = Color(0xFFACE4EC),
    val warmAccent: Color = Color(0xFFEC8459),
    val sunnyAccent: Color = Color(0xFFF5CE54),
    val leaf: Color = Color(0xFF8DBA69),
    val leafShade: Color = Color(0xFF64956A),
    val porcelain: Color = Color(0xFFFFFCED),
    val metal: Color = Color(0xFFB8C4C9),
    val blush: Color = Color(0xFFF0A1B2),
    val fog: Color = Color(0xFFE8F2ED),
    val fogShade: Color = Color(0xFFD3E1DD),
)
