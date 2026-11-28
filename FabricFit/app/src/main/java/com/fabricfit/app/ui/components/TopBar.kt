package com.fabricfit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R

/**
 * The one top bar every FabricFit screen uses.
 *
 * There used to be no such thing: 28 screens each hand-rolled a `Row` with an arrow,
 * a title and optional actions. They disagreed on the title style
 * (`headlineSmall` / `headlineMedium` / `headlineLarge`), on the horizontal inset
 * (12 / 16 / 20 dp), on the back glyph (`ArrowBack` vs `ArrowBackIosNew`), on the
 * title colour (`onBackground` vs `primary`), and on the accessible label
 * (`cd_back` vs `action_back`). Two screens used the same pattern but marked the
 * title `Bold` differently, so even "shipped" headers drifted.
 *
 * This component fixes all of that in one place, and adds two behaviours the copies
 * lacked:
 *
 *  - **Consistent touch target.** The back affordance is a real 48 dp [IconButton],
 *    not a tap on whatever happened to be beside the title.
 *  - **A heading for accessibility.** The title is marked as a heading so TalkBack
 *    announces structure instead of reading the bar as an undifferentiated run of
 *    text.
 *
 * The bar is transparent and expects the caller to supply the screen background, so
 * it layers correctly over both the cream (light) and deep-forest (dark) surfaces
 * rather than assuming one.
 *
 * @param title already-resolved text, so callers can pass `stringResource(...)` or a
 *   dynamic value such as a collection name.
 * @param onBack when non-null, renders the back affordance. Pass null on a top-level
 *   destination: those are reached from the tab bar and must not imply a back stack.
 * @param subtitle optional second line for context (e.g. an item count).
 * @param actions trailing content, laid out end-aligned.
 * @param tone colour for title and icons; defaults to the screen's `onBackground`.
 */
@Composable
fun FabricFitTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    tone: Color = MaterialTheme.colorScheme.onBackground,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (onBack == null) 20.dp else 4.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = tone,
                )
            }
            Spacer(Modifier.width(2.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tone.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/**
 * A centred, editorial variant for destinations that lead with their own imagery —
 * the security lock and the premium paywall present more like a magazine spread than
 * an app screen. Same type and touch sizes as [FabricFitTopBar]; only the alignment
 * and the absence of a back affordance differ, so the two never contradict each other.
 */
@Composable
fun FabricFitCenteredTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: Color = MaterialTheme.colorScheme.onBackground,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        if (leading != null) {
            Box(Modifier.align(Alignment.CenterStart)) { leading() }
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tone.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}
