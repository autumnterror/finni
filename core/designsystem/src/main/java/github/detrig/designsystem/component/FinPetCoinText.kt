package github.detrig.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import github.detrig.designsystem.R
import github.detrig.designsystem.theme.FinPetTheme

/** Replaces legacy currency signs in UI copy with the shared game coin image. */
@Composable
fun FinPetCoinText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val marked: AnnotatedString = buildAnnotatedString {
        text.forEach { character ->
            if (character == '₽') appendInlineContent("game_coin", "монет") else append(character)
        }
    }
    Text(
        text = marked,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = mapOf(
            "game_coin" to InlineTextContent(
                Placeholder(1.15.em, 1.15.em, PlaceholderVerticalAlign.Center),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_game_coin),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            },
        ),
    )
}

@Preview(name = "Игровая валюта")
@Composable
private fun FinPetCoinTextPreview() {
    FinPetTheme { FinPetCoinText("Нужно 40 ₽, осталось 20 ₽") }
}
