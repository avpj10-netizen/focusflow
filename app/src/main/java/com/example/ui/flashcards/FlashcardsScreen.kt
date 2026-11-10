package com.example.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlashcardEntity
import com.example.data.model.SubjectEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SubjectBadge
import com.example.ui.components.parseHexColor
import com.example.ui.theme.BreakGreen
import com.example.ui.theme.FocusGold
import com.example.ui.theme.IosFlipCard
import com.example.ui.theme.iosBounceClick

@Composable
fun FlashcardsScreen(
    flashcards: List<FlashcardEntity>,
    subjects: List<SubjectEntity>,
    onAddFlashcard: (Long, String, String) -> Unit,
    onToggleMastered: (Long, Boolean) -> Unit,
    onDeleteFlashcard: (FlashcardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCards = flashcards.filter {
        selectedSubjectId == null || it.subjectId == selectedSubjectId
    }

    val activeCard = if (filteredCards.isNotEmpty()) {
        filteredCards[currentCardIndex.coerceIn(0, filteredCards.lastIndex)]
    } else null

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .iosBounceClick { showAddDialog = true }
                    .testTag("add_flashcard_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Flashcard")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "Smart Flashcards",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active recall & spaced repetition deck",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (filteredCards.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${currentCardIndex + 1}/${filteredCards.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Subject Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                item {
                    val isAll = selectedSubjectId == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.iosBounceClick {
                            selectedSubjectId = null
                            currentCardIndex = 0
                            isFlipped = false
                        }
                    ) {
                        Text(
                            text = "All Subjects",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAll) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                items(subjects) { subj ->
                    val isSelected = selectedSubjectId == subj.id
                    val color = parseHexColor(subj.colorHex)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                        modifier = Modifier.iosBounceClick {
                            selectedSubjectId = subj.id
                            currentCardIndex = 0
                            isFlipped = false
                        }
                    ) {
                        Text(
                            text = subj.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeCard == null) {
                EmptyStateView(
                    icon = Icons.Default.Style,
                    title = "No Flashcards in this Deck",
                    message = "Create question and answer flashcards to test your knowledge with active recall.",
                    actionLabel = "Create Flashcard",
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                val cardSubject = subjects.find { it.id == activeCard.subjectId }

                // 3D Flip Card
                IosFlipCard(
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped },
                    frontContent = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (cardSubject != null) {
                                    SubjectBadge(name = cardSubject.name, colorHex = cardSubject.colorHex)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "QUESTION (Tap to flip)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = activeCard.frontText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "💡 Tap card to reveal answer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    backContent = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (cardSubject != null) {
                                    SubjectBadge(name = cardSubject.name, colorHex = cardSubject.colorHex)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BreakGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "ANSWER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BreakGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = activeCard.backText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "Tap again to flip back",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Mastered vs Need Review
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            onToggleMastered(activeCard.id, false)
                            if (currentCardIndex < filteredCards.lastIndex) {
                                currentCardIndex++
                                isFlipped = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .iosBounceClick {
                                onToggleMastered(activeCard.id, false)
                                if (currentCardIndex < filteredCards.lastIndex) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = FocusGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Need Review", color = FocusGold)
                    }

                    Button(
                        onClick = {
                            onToggleMastered(activeCard.id, true)
                            if (currentCardIndex < filteredCards.lastIndex) {
                                currentCardIndex++
                                isFlipped = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BreakGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .iosBounceClick {
                                onToggleMastered(activeCard.id, true)
                                if (currentCardIndex < filteredCards.lastIndex) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mastered", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Controls (Previous, Next)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (currentCardIndex > 0) {
                                currentCardIndex--
                                isFlipped = false
                            }
                        },
                        enabled = currentCardIndex > 0
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous")
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = "Card ${currentCardIndex + 1} of ${filteredCards.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    IconButton(
                        onClick = {
                            if (currentCardIndex < filteredCards.lastIndex) {
                                currentCardIndex++
                                isFlipped = false
                            }
                        },
                        enabled = currentCardIndex < filteredCards.lastIndex
                    ) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFlashcardDialog(
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onSave = { subjId, front, back ->
                onAddFlashcard(subjId, front, back)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddFlashcardDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var frontText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Study Flashcard", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Subject", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subj ->
                        val isSelected = selectedSubjectId == subj.id
                        val color = parseHexColor(subj.colorHex)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier.iosBounceClick { selectedSubjectId = subj.id }
                        ) {
                            Text(
                                text = subj.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = frontText,
                    onValueChange = { frontText = it },
                    label = { Text("Front (Question or Concept) *") },
                    placeholder = { Text("e.g. What is the Heisenberg Uncertainty Principle?") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_front_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = backText,
                    onValueChange = { backText = it },
                    label = { Text("Back (Answer or Definition) *") },
                    placeholder = { Text("e.g. It is impossible to simultaneously measure position and momentum with arbitrary precision.") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("flashcard_back_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (frontText.isNotBlank() && backText.isNotBlank()) {
                        onSave(selectedSubjectId, frontText.trim(), backText.trim())
                    }
                },
                enabled = frontText.isNotBlank() && backText.isNotBlank(),
                modifier = Modifier.testTag("save_flashcard_button")
            ) {
                Text("Add Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
