package github.detrig.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Материалы рисованного дома; заменяются вместе с брендбуком. */
@Immutable
data class FinPetHouseColors(
    val playroomWall: Color = Color(0xFFB8EDF7),
    val bedroomWall: Color = Color(0xFFB5DD78),
    val hallWall: Color = Color(0xFFFFFBCD),
    val kitchenWall: Color = Color(0xFFDCF0B4),
    val wallPattern: Color = Color(0xFF82D7E8),
    val hallStripe: Color = Color(0xFFFFE996),
    val floor: Color = Color(0xFFFFE471),
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
    val floorHighlight: Color = Color(0xFFFFF098),
    val floorShade: Color = Color(0xFFFFDE62),
    val wallHighlight: Color = Color(0xFFFFFFE2),
    val tileGrout: Color = Color(0xFFF3FADB),
    val rug: Color = Color(0xFF79BF56),
    val rugHighlight: Color = Color(0xFF97D36B),
    val rugBorder: Color = Color(0xFF2B8043),
)
