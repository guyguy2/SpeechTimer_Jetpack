package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Option model for [ExpressiveToggleGroup]. */
@Immutable
data class ExpressiveToggleOption<T>(
    val value: T,
    val label: String
)

/**
 * Selective Material 3 Expressive wrapper around [ButtonGroup] and [FilledTonalToggleButton].
 * - Isolates `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` in one place.
 * - Respects system reduced motion ([isReducedMotionEnabled]) by disabling width expansion.
 * - Falls back to wrapping [FlowRow] on narrow widths or large font scales (such as 2.0x)
 *   so labels never clip, ellipsize, or collapse into an overflow menu.
 * - Enforces >= 48.dp touch targets on every toggle button for ATF compliance.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun <T> ExpressiveToggleGroup(
    options: List<ExpressiveToggleOption<T>>,
    selectedValue: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useSingleRowGroup =
            fontScale < 1.25f && maxWidth >= 380.dp && options.size in 2..4

        if (useSingleRowGroup) {
            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                modifier = Modifier.fillMaxWidth(),
                expandedRatio = if (reducedMotion) 0f else ButtonGroupDefaults.ExpandedRatio
            ) {
                options.forEachIndexed { index, option ->
                    customItem(
                        buttonGroupContent = {
                            val shapes = when {
                                options.size == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            val interactionSource = remember { MutableInteractionSource() }
                            val baseModifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                            val itemModifier = if (reducedMotion) {
                                baseModifier
                            } else {
                                baseModifier.animateWidth(interactionSource)
                            }
                            FilledTonalToggleButton(
                                checked = option.value == selectedValue,
                                onCheckedChange = { onSelect(option.value) },
                                modifier = itemModifier,
                                shapes = shapes,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                interactionSource = interactionSource
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSelect(option.value)
                                    menuState.dismiss()
                                }
                            )
                        }
                    )
                }
            }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    FilledTonalToggleButton(
                        checked = option.value == selectedValue,
                        onCheckedChange = { onSelect(option.value) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Accessible Material 3 Expressive toggle button wrapper enforcing a >= 48.dp touch target.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveTonalToggleAction(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        content = content
    )
}
