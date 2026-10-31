package nl.wdhertog.takenlijst.ui

import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.staticCompositionLocalOf
import nl.wdhertog.takenlijst.ui.theme.fg
import nl.wdhertog.takenlijst.ui.theme.bg
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.ui.theme.App
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val Dutch: Locale = Locale.forLanguageTag("nl-NL")
private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", Dutch)
private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Dutch)

fun LocalDate.longLabel(): String = format(longDate).replaceFirstChar { it.uppercase() }

/** "Vandaag", "Morgen", "Vr 30 okt" of "Over 3 wk". */
fun LocalDate.relativeLabel(today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, this)
    return when {
        days == 0L -> "Vandaag"
        days == 1L -> "Morgen"
        days == -1L -> "Gisteren"
        days in 2..13 -> format(shortDate).replace(".", "").replaceFirstChar { it.uppercase() }
        days in 14..60 -> "Over ${days / 7} wk"
        days > 60 -> format(DateTimeFormatter.ofPattern("MMM yyyy", Dutch)).replace(".", "").replaceFirstChar { it.uppercase() }
        else -> format(shortDate).replace(".", "").replaceFirstChar { it.uppercase() }
    }
}

fun lateLabel(due: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(due, today)
    return if (days == 1L) "1 dag te laat" else "$days dagen te laat"
}

fun minutesLabel(m: Int) = "%02d:%02d".format(m / 60, m % 60)

@DrawableRes
fun TaskGroup.icon(): Int = TaskIcon.byKey(icon)?.drawable ?: R.drawable.ic_home

/** De groepen van de gebruiker, op volgorde; wordt bovenaan de app gevuld vanuit de database. */
val LocalGroups = staticCompositionLocalOf { TaskGroup.DEFAULTS }

/** De groep met deze id, of een neutrale "Overig" als hij (nog) niet bestaat. */
@Composable
fun groupOf(id: String): TaskGroup = LocalGroups.current.firstOrNull { it.id == id } ?: TaskGroup.FALLBACK

@Composable
fun GroupTag(group: TaskGroup) {
    Text(
        group.label,
        style = MaterialTheme.typography.labelMedium,
        color = group.fg,
        modifier = Modifier
            .clip(CircleShape)
            .background(group.bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, color: Color = App.colors.muted, trailing: String? = null) {
    Row(
        modifier.padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text.uppercase(Dutch), style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = App.colors.muted)
    }
}

/**
 * Ronde afvinkknop met een raakvlak van 48dp. [progress] (0..1) vult de cirkel als taartpunt, voor taken
 * die meerdere keren per dag gebeuren: 2 van 4 gedaan is een halve cirkel.
 */
@Composable
fun CheckCircle(checked: Boolean, label: String, onClick: () -> Unit, ringColor: Color = MaterialTheme.colorScheme.outline, progress: Float = 0f) {
    val fill by animateColorAsState(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent, label = "vink")
    val partial by animateFloatAsState(if (checked) 0f else progress.coerceIn(0f, 1f), label = "deels")
    val pie = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(fill)
                .then(if (checked) Modifier else Modifier.border(2.dp, ringColor, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            else if (partial > 0f) Canvas(Modifier.size(24.dp)) {
                val inset = 4.dp.toPx()
                drawArc(
                    pie, startAngle = -90f, sweepAngle = 360f * partial, useCenter = true,
                    topLeft = Offset(inset, inset), size = Size(size.width - 2 * inset, size.height - 2 * inset),
                )
            }
        }
    }
}

/** Eén taakregel zoals op het Vandaag-scherm; lang drukken roept [onLongClick] aan (uitstellen). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    title: String,
    group: TaskGroup,
    detail: String,
    checked: Boolean,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    late: String? = null,
    progress: Float = 0f,
    @DrawableRes icon: Int? = null,
    trailing: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = App.colors
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .then(if (late != null) Modifier.border(2.dp, colors.lateRing, RoundedCornerShape(18.dp)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = onLongClick?.let { "Uitstellen" })
            .heightIn(min = 64.dp)
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(
            checked, if (checked) "Maak ongedaan: $title" else "Afvinken: $title", onCheck,
            ringColor = if (late != null) colors.lateFg else MaterialTheme.colorScheme.outline,
            progress = progress,
        )
        Spacer(Modifier.width(4.dp))
        if (icon != null) {
            Box(
                Modifier.alpha(if (checked) 0.5f else 1f).size(36.dp).clip(RoundedCornerShape(12.dp)).background(group.bg),
                contentAlignment = Alignment.Center,
            ) { DrawableIcon(icon, Modifier.size(20.dp), tint = group.fg) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (checked) colors.muted else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 3.dp)) {
                GroupTag(group)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (late != null) Text(late, style = MaterialTheme.typography.bodySmall, color = colors.lateFg, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun DrawableIcon(@DrawableRes id: Int, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface, description: String? = null) {
    Icon(painterResource(id), description, modifier = modifier, tint = tint)
}
