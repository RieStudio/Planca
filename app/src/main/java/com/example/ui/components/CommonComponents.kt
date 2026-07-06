package com.planca.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppText(
                text = label,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            )
        }
    }
}

@Composable
fun PlancaIcon(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val stemWidth = w * 0.055f
        val stemX = w * 0.23f
        val stemTop = h * 0.15f
        val stemBottom = h * 0.95f
        
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(stemX, stemTop),
            size = Size(stemWidth, stemBottom - stemTop),
            cornerRadius = CornerRadius(stemWidth / 2f, stemWidth / 2f)
        )
        
        val bowlRadius = w * 0.28f
        val bowlCenterX = stemX + (stemWidth / 2f) + bowlRadius
        val bowlCenterY = stemTop + bowlRadius
        
        drawCircle(
            color = primaryColor,
            radius = bowlRadius,
            center = Offset(bowlCenterX, bowlCenterY)
        )
        
        val innerRadius = bowlRadius - (w * 0.055f)
        drawCircle(
            color = backgroundColor,
            radius = innerRadius,
            center = Offset(bowlCenterX, bowlCenterY)
        )
        
        val path = Path().apply {
            val startX = bowlCenterX - (bowlRadius * 0.60f)
            val startY = bowlCenterY
            
            val cornerX = bowlCenterX - (bowlRadius * 0.15f)
            val cornerY = bowlCenterY + (bowlRadius * 0.45f)
            
            val endX = bowlCenterX + (bowlRadius * 1.20f)
            val endY = bowlCenterY - (bowlRadius * 0.90f)
            
            moveTo(startX, startY)
            lineTo(cornerX, cornerY)
            lineTo(endX, endY)
        }
        
        val strokeWidth = w * 0.055f
        val maskStrokeWidth = strokeWidth * 1.8f
        
        drawPath(
            path = path,
            color = backgroundColor,
            style = Stroke(
                width = maskStrokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    textDecoration: androidx.compose.ui.text.style.TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val themeFont = MaterialTheme.typography.bodyMedium.fontFamily
    val mergedStyle = if (style.fontFamily == null) {
        style.copy(fontFamily = themeFont)
    } else {
        style
    }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily ?: themeFont,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = mergedStyle
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = @Composable { it() }
) {
    val themeFont = MaterialTheme.typography.bodyMedium.fontFamily
    val mergedStyle = if (textStyle.fontFamily == null) {
        textStyle.copy(fontFamily = themeFont)
    } else {
        textStyle
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        cursorBrush = cursorBrush,
        decorationBox = decorationBox
    )
}
