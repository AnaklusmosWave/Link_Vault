package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.sqrt

/**
 * Dialog for setting up a folder lock. (either PIN or PATTERN)
 */
@Composable
fun LockSetupDialog(
    onDismiss: () -> Unit,
    onLockConfigured: (type: String, value: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("PIN") } // "PIN" or "PATTERN"
    var pinValue by remember { mutableStateOf("") }
    var patternValue by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "設定資料夾上鎖",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    val tabs = listOf("PIN" to "數字鎖", "PATTERN" to "9點圖形鎖")
                    tabs.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    selectedType = type
                                    pinValue = ""
                                    patternValue = emptyList()
                                    errorMessage = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (selectedType == "PIN") {
                    // PIN Recorder Interface
                    Text(
                        text = "請輸入 4 位密碼：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dots row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        for (i in 0 until 4) {
                            val active = i < pinValue.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    // Compact PIN Keyboard
                    PinKeyboard(
                        onKeyPressed = { digit ->
                            if (pinValue.length < 4) {
                                pinValue += digit
                            }
                        },
                        onBackspace = {
                            if (pinValue.isNotEmpty()) {
                                pinValue = pinValue.dropLast(1)
                            }
                        }
                    )

                } else {
                    // Pattern Recorder Interface
                    Text(
                        text = if (patternValue.isEmpty()) "請在下方繪製圖形鎖軌跡：" else "已記錄圖形軌跡",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    if (patternValue.isNotEmpty()) {
                        Text(
                            text = "連線順序點: ${patternValue.joinToString(" ➔ ")}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        PatternLockCanvas(
                            modifier = Modifier.fillMaxSize(),
                            onPatternComplete = { path ->
                                if (path.size < 3) {
                                    errorMessage = "圖形鎖長度必須至少連接 3 個點"
                                } else {
                                    errorMessage = ""
                                    patternValue = path
                                }
                            }
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            if (selectedType == "PIN") {
                                if (pinValue.length < 4) {
                                    errorMessage = "請輸入完整的 4 位數字密碼"
                                } else {
                                    onLockConfigured("PIN", pinValue)
                                    onDismiss()
                                }
                            } else {
                                if (patternValue.isEmpty()) {
                                    errorMessage = "請繪製圖形密碼"
                                } else {
                                    // Store series of indices as string "0,2,4,8"
                                    onLockConfigured("PATTERN", patternValue.joinToString(","))
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("確認儲存")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for unlocking a locked folder.
 */
@Composable
fun LockUnlockDialog(
    folderName: String,
    lockType: String,
    lockValue: String,
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var tries by remember { mutableStateOf(0) }
    var feedbackMessage by remember { mutableStateOf("請輸入此資料夾的密碼以解鎖") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Unlock",
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (lockType == "PIN") "數位密碼防護" else "圖形安全鎖防護",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = feedbackMessage,
                    fontSize = 14.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (lockType == "PIN") {
                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        for (i in 0 until 4) {
                            val active = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isError) MaterialTheme.colorScheme.error.copy(alpha = if (active) 1f else 0.2f)
                                        else if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    PinKeyboard(
                        onKeyPressed = { digit ->
                            if (enteredPin.length < 4) {
                                isError = false
                                feedbackMessage = "輸入密碼中..."
                                val nextPin = enteredPin + digit
                                enteredPin = nextPin

                                if (nextPin.length == 4) {
                                    if (nextPin == lockValue) {
                                        feedbackMessage = "密碼正確，正在解鎖！"
                                        scope.launch {
                                            kotlinx.coroutines.delay(250)
                                            onUnlockSuccess()
                                            onDismiss()
                                        }
                                    } else {
                                        scope.launch {
                                            kotlinx.coroutines.delay(250)
                                            tries++
                                            isError = true
                                            enteredPin = ""
                                            feedbackMessage = "密碼錯誤！請再試一次"
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        }
                    )
                } else {
                    // Pattern Canvas
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        PatternLockCanvas(
                            modifier = Modifier.fillMaxSize(),
                            onPatternComplete = { path ->
                                val pathStr = path.joinToString(",")
                                if (pathStr == lockValue) {
                                    isError = false
                                    feedbackMessage = "圖形解鎖成功！"
                                    onUnlockSuccess()
                                    onDismiss()
                                } else {
                                    tries++
                                    isError = true
                                    feedbackMessage = "解鎖圖形不正確，請重新繪製"
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("退出不存取", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

/**
 * A beautiful dial pad for PIN codes
 */
@Composable
fun PinKeyboard(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "back")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { rowKeys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                rowKeys.forEach { key ->
                    when (key) {
                        "" -> {
                            Spacer(modifier = Modifier.size(56.dp))
                        }
                        "back" -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .clickable { onBackspace() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { onKeyPressed(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pure Jetpack Compose custom Canvas-based 9-dot Pattern Lock View.
 */
@Composable
fun PatternLockCanvas(
    modifier: Modifier = Modifier,
    onPatternComplete: (List<Int>) -> Unit
) {
    val density = LocalDensity.current
    val strokeColor = MaterialTheme.colorScheme.primary
    val activeDotColor = MaterialTheme.colorScheme.primary
    val inactiveDotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    
    // Convert 40dp threshold to raw pixels
    val thresholdPx = with(density) { 36.dp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        // Compute 3x3 dot coordinates
        val dots = remember(width, height) {
            List(9) { i ->
                val row = i / 3
                val col = i % 3
                val x = (col * 2 + 1) * width / 6f
                val y = (row * 2 + 1) * height / 6f
                Offset(x, y)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(width, height) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            selectedIndices.clear()
                            val idx = findClosestDot(startOffset, dots, thresholdPx)
                            if (idx != -1) {
                                selectedIndices.add(idx)
                            }
                            currentTouchPoint = startOffset
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val currentPos = (currentTouchPoint ?: change.position) + dragAmount
                            currentTouchPoint = currentPos
                            val idx = findClosestDot(currentPos, dots, thresholdPx)
                            if (idx != -1 && !selectedIndices.contains(idx)) {
                                selectedIndices.add(idx)
                            }
                        },
                        onDragEnd = {
                            if (selectedIndices.isNotEmpty()) {
                                onPatternComplete(selectedIndices.toList())
                            }
                            selectedIndices.clear()
                            currentTouchPoint = null
                        },
                        onDragCancel = {
                            selectedIndices.clear()
                            currentTouchPoint = null
                        }
                    )
                }
        ) {
            // 1. Draw connecting lines between mapped dots
            if (selectedIndices.size > 1) {
                for (k in 0 until selectedIndices.size - 1) {
                    val p1 = dots[selectedIndices[k]]
                    val p2 = dots[selectedIndices[k + 1]]
                    drawLine(
                        color = strokeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Draw line to current touch point
            if (selectedIndices.isNotEmpty() && currentTouchPoint != null) {
                val lastDot = dots[selectedIndices.last()]
                drawLine(
                    color = strokeColor.copy(alpha = 0.5f),
                    start = lastDot,
                    end = currentTouchPoint!!,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw dots
            dots.forEachIndexed { idx, pos ->
                val isSelected = selectedIndices.contains(idx)
                
                // Outer ring for selected
                if (isSelected) {
                    drawCircle(
                        color = strokeColor.copy(alpha = 0.2f),
                        radius = 24.dp.toPx(),
                        center = pos
                    )
                }
                
                // Main dots
                drawCircle(
                    color = if (isSelected) activeDotColor else inactiveDotColor,
                    radius = if (isSelected) 10.dp.toPx() else 7.dp.toPx(),
                    center = pos
                )
            }
        }
    }
}

/**
 * Calculates if search touch offset inside target density radius around any 3x3 point
 */
private fun findClosestDot(touch: Offset, dots: List<Offset>, threshold: Float): Int {
    for (i in dots.indices) {
        val dist = getDistance(touch, dots[i])
        if (dist <= threshold) {
            return i
        }
    }
    return -1
}

private fun getDistance(p1: Offset, p2: Offset): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
}
