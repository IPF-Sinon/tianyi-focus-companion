package top.funcun.companion.shell.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** 底栏 Tab 数据 */
data class BarTab(
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector,
)

/**
 * 悬浮胶囊底栏（对标 FolkPatch）。
 *
 * @param floating true = 悬浮胶囊（含滑动指示器）；false = 常规通栏 NavigationBar
 * @param compact true = 胶囊（percent 50）；false = 大圆角
 */
@Composable
fun FloatingBottomBar(
    tabs: List<BarTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    floating: Boolean = true,
    compact: Boolean = true,
) {
    if (!floating) {
        StandardBar(tabs, selectedIndex, onSelect, modifier)
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        val screenWidth = maxWidth
        val hPad = when {
            screenWidth > 600.dp -> 32.dp
            screenWidth > 400.dp -> 24.dp
            else -> 16.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPad, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            val barShape = if (compact) RoundedCornerShape(percent = 50)
            else MaterialTheme.shapes.large
            Surface(
                modifier = Modifier.wrapContentWidth(),
                shape = barShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                BarContent(tabs, selectedIndex, onSelect)
            }
        }
    }
}

@Composable
private fun BarContent(
    tabs: List<BarTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val itemSize = 52.dp
    val itemSpacing = 6.dp
    val containerPadding = 8.dp
    val barHeight = 68.dp
    val itemShape = CircleShape

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "navIndicator",
    )

    val barWidth = (itemSize * tabs.size) +
        (itemSpacing * (tabs.size - 1)) +
        (containerPadding * 2)

    Box(modifier = Modifier.width(barWidth).height(barHeight)) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = containerPadding)) {
            // 滑动指示器
            if (tabs.isNotEmpty()) {
                val density = LocalDensity.current
                val itemSizePx = with(density) { itemSize.toPx() }
                val spacingPx = with(density) { itemSpacing.toPx() }
                val offset = (itemSizePx + spacingPx) * animatedIndex

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .offset { IntOffset(offset.toInt(), 0) }
                        .width(itemSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(itemSize)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = itemShape,
                            ),
                    )
                }
            }

            // 图标项
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .size(itemSize)
                            .clip(itemShape)
                            .clickable { if (!selected) onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) tab.filled else tab.outlined,
                            contentDescription = tab.label,
                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardBar(
    tabs: List<BarTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = if (index == selectedIndex) tab.filled else tab.outlined,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}