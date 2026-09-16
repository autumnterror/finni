package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.gamesession.R
import github.detrig.feature.gamesession.presentation.GameSessionViewEvent

@Composable
internal fun HomeActionBar(
    onEvent: (GameSessionViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        HomeActionItem(
            drawableRes = R.drawable.btn_finpet_action_work,
            contentDescription = "Работа",
            event = GameSessionViewEvent.WorkClicked,
        ),
        HomeActionItem(
            drawableRes = R.drawable.btn_finpet_action_tasks,
            contentDescription = "Задания",
            event = GameSessionViewEvent.TasksClicked,
        ),
        HomeActionItem(
            drawableRes = R.drawable.btn_finpet_action_shop,
            contentDescription = "Магазин",
            event = GameSessionViewEvent.ShopClicked,
        ),
        HomeActionItem(
            drawableRes = R.drawable.btn_finpet_action_bank,
            contentDescription = "Банк",
            event = GameSessionViewEvent.BankClicked,
        ),
        HomeActionItem(
            drawableRes = R.drawable.btn_finpet_action_care,
            contentDescription = "Уход",
            event = GameSessionViewEvent.CareClicked,
        ),
        HomeActionItem(
            drawableRes = R.drawable.btn_finpet_action_home,
            contentDescription = "Дом",
            event = GameSessionViewEvent.HomeClicked,
        ),
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            HomeActionButton(
                item = item,
                onClick = { onEvent(item.event) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    item: HomeActionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PixelImage(
            drawableRes = item.drawableRes,
            contentDescription = item.contentDescription,
            modifier = Modifier.fillMaxSize(),
            size = null,
            contentScale = ContentScale.Fit,
        )
    }
}

private data class HomeActionItem(
    val drawableRes: Int,
    val contentDescription: String,
    val event: GameSessionViewEvent,
)
