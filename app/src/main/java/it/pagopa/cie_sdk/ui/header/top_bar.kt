package it.pagopa.cie_sdk.ui.header

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.pagopa.cie_sdk.ui.UserInteraction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    titleResId: Int,
    imageLeft: HeaderImage?,
    imageRight: HeaderImage? = null,
    modifier: Modifier = Modifier
) {
    val whatIs = stringResource(id = titleResId)
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
    ) {
        CenterAlignedTopAppBar(
            title = {
                BaseHeaderContour {
                    DrawContouredBox(MaterialTheme.colorScheme.onBackground)
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = whatIs,
                        color = primary
                    )
                }
            },
            navigationIcon = {
                if (imageLeft == null) return@CenterAlignedTopAppBar
                imageLeft.ToHeaderIcon()
            },
            actions = {
                if (imageRight == null) return@CenterAlignedTopAppBar
                imageRight.ToHeaderIcon()
            }
        )
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .semantics { disabled() }) {
            drawRect(color = primary)
        }
    }
}

@Composable
private fun BaseHeaderContour(
    onClick: UserInteraction? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val modifier = onClick?.let {
        Modifier
            .clickable {
                it.action()
            }
            .semantics(mergeDescendants = true) {}
    } ?: run {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Box(modifier = modifier) {
        content()
    }
}

@Composable
private fun HeaderImage.ToHeaderIcon() {
    val contourColor = MaterialTheme.colorScheme.onBackground
    BaseHeaderContour(this.onIconClick) {
        DrawContouredBox(contourColor, 48)
        Icon(
            imageVector = this@ToHeaderIcon.icon,
            contentDescription = this@ToHeaderIcon.contentDescription,
            modifier = Modifier
                .align(Alignment.Center)
                .clickable {
                    this@ToHeaderIcon.onIconClick.action()
                },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DrawContouredBox(
    contourColor: androidx.compose.ui.graphics.Color,
    useSize: Int? = null
) {
    val strokeWidth = 2.dp
    val modifier = useSize?.let {
        Modifier
            .fillMaxHeight()
            .width(it.dp)
            .semantics { disabled() }
    } ?: run {
        Modifier
            .fillMaxSize()
            .semantics { disabled() }
    }
    Canvas(modifier = modifier.padding(top = 8.dp, bottom = 8.dp)) {
        val cornerRadius = CornerRadius(8.dp.toPx())
        drawRoundRect(
            color = contourColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth.toPx())
        )
    }
}
