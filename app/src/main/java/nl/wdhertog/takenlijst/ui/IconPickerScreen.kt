package nl.wdhertog.takenlijst.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.ui.theme.App

/** Icoon kiezen voor een taak, met zoeken en indeling per categorie. Geeft "" terug voor "automatisch". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerScreen(current: String, title: String, group: TaskGroup, onPick: (String) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = TaskIcon.search(query)
    val auto = TaskIcon.guess(title)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Icoon kiezen", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Zoek, bijv. hond, was of tuin") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "Wissen") } },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = App.colors.card, unfocusedContainerColor = App.colors.card,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 76.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Automatisch") }
                    item {
                        IconTile(auto?.drawable ?: group.icon(), if (auto != null) "Auto: ${auto.label}" else "Auto", current.isEmpty(), group) { onPick("") }
                    }
                    TaskIcon.CATEGORIES.forEach { category ->
                        item(span = { GridItemSpan(maxLineSpan) }, key = "h$category") { SectionHeader(category) }
                        items(TaskIcon.ALL.filter { it.category == category }, key = { it.key }) { icon ->
                            IconTile(icon.drawable, icon.label, icon.key == current, group) { onPick(icon.key) }
                        }
                    }
                } else if (results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Geen icoon gevonden voor “$query”.",
                            style = MaterialTheme.typography.bodyMedium, color = App.colors.muted,
                            modifier = Modifier.padding(top = 24.dp, start = 4.dp),
                        )
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("${results.size} gevonden") }
                    items(results, key = { it.key }) { icon ->
                        IconTile(icon.drawable, icon.label, icon.key == current, group) { onPick(icon.key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(drawable: Int, label: String, selected: Boolean, group: TaskGroup, onClick: () -> Unit) {
    val fg = App.colors.groupFg.getValue(group)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = if (selected) App.colors.groupBg.getValue(group) else App.colors.card,
            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) fg else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(56.dp).semantics { this.selected = selected; contentDescription = label },
        ) {
            Box(contentAlignment = Alignment.Center) { DrawableIcon(drawable, Modifier.size(26.dp), tint = fg) }
        }
        Text(
            label, style = MaterialTheme.typography.labelMedium, color = App.colors.muted,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).height(32.dp),
        )
    }
}
