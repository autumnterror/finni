package github.detrig.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme

/** Настройка количества колонок без утечки foundation-типа в feature API. */
sealed interface FinPetGridColumns {

    @Immutable
    data class Fixed(val count: Int) : FinPetGridColumns {
        init {
            require(count > 0) { "Grid column count must be positive" }
        }
    }

    @Immutable
    data class Adaptive(val minimumItemWidth: Dp) : FinPetGridColumns {
        init {
            require(minimumItemWidth > 0.dp) { "Minimum grid item width must be positive" }
        }
    }
}

/**
 * Типонезависимая вертикальная сетка. Она отвечает только за раскладку, а не за
 * карточки или модель конкретного магазина.
 */
@Composable
fun <T> FinPetLazyGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: FinPetGridColumns = FinPetGridColumns.Adaptive(120.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalSpacing: Dp = AppTheme.spacing.md,
    verticalSpacing: Dp = AppTheme.spacing.md,
    userScrollEnabled: Boolean = true,
    key: ((item: T) -> Any)? = null,
    contentType: ((item: T) -> Any?)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns.toGridCells(),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        userScrollEnabled = userScrollEnabled,
    ) {
        items(
            items = items,
            key = key,
            contentType = contentType ?: { null },
        ) { item ->
            itemContent(item)
        }
    }
}

/** Универсальный ленивый горизонтальный список для chips и других элементов. */
@Composable
fun <T> FinPetLazyRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemSpacing: Dp = AppTheme.spacing.sm,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    userScrollEnabled: Boolean = true,
    key: ((item: T) -> Any)? = null,
    contentType: ((item: T) -> Any?)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = verticalAlignment,
        userScrollEnabled = userScrollEnabled,
    ) {
        items(
            items = items,
            key = key,
            contentType = contentType ?: { null },
        ) { item ->
            itemContent(item)
        }
    }
}

private fun FinPetGridColumns.toGridCells(): GridCells {
    return when (this) {
        is FinPetGridColumns.Fixed -> GridCells.Fixed(count)
        is FinPetGridColumns.Adaptive -> GridCells.Adaptive(minimumItemWidth)
    }
}
