/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.home

// import androidx.compose.ui.tooling.preview.Preview
// import com.google.ai.edge.gallery.ui.theme.GalleryTheme
// import com.google.ai.edge.gallery.ui.preview.PreviewModelManagerViewModel
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.google.ai.edge.gallery.GalleryTopAppBar
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.AppBarAction
import com.google.ai.edge.gallery.data.AppBarActionType
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.CategoryInfo
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.firebaseAnalytics
import com.google.ai.edge.gallery.proto.ImportedModel
import com.google.ai.edge.gallery.ui.common.RevealingText
import com.google.ai.edge.gallery.ui.common.SwipingText
import com.google.ai.edge.gallery.ui.common.TaskIcon
import com.google.ai.edge.gallery.ui.common.rememberDelayedAnimationProgress
import com.google.ai.edge.gallery.ui.common.tos.TosDialog
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.customColors
import com.google.ai.edge.gallery.ui.theme.homePageTitleStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "AGHomeScreen"
private const val TASK_COUNT_ANIMATION_DURATION = 250
private const val ANIMATION_INIT_DELAY = 100L
private const val TOP_APP_BAR_ANIMATION_DURATION = 600
private const val TITLE_FIRST_LINE_ANIMATION_DURATION = 600
private const val TITLE_SECOND_LINE_ANIMATION_DURATION = 600
private const val TITLE_SECOND_LINE_ANIMATION_DURATION2 = 800
private const val TITLE_SECOND_LINE_ANIMATION_START =
  ANIMATION_INIT_DELAY + (TITLE_FIRST_LINE_ANIMATION_DURATION * 0.5).toInt()
private const val TASK_LIST_ANIMATION_START = TITLE_SECOND_LINE_ANIMATION_START + 110
private const val TASK_CARD_ANIMATION_DELAY_OFFSET = 100
private const val TASK_CARD_ANIMATION_DURATION = 600
private const val CONTENT_COMPOSABLES_ANIMATION_DURATION = 1200
private const val CONTENT_COMPOSABLES_OFFSET_Y = 16

/** Navigation destination data */
object HomeScreenDestination {
  @StringRes val titleRes = R.string.app_name
}

private val PREDEFINED_CATEGORY_ORDER = listOf(Category.LLM.id, Category.EXPERIMENTAL.id)
private val PREDEFINED_LLM_TASK_ORDER =
  listOf(
    BuiltInTaskId.LLM_ASK_IMAGE,
    BuiltInTaskId.LLM_ASK_AUDIO,
    BuiltInTaskId.LLM_PROMPT_LAB,
    BuiltInTaskId.LLM_CHAT,
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  tosViewModel: TosViewModel,
  navigateToTaskScreen: (Task) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }

  if (showTosDialog) {
    TosDialog(
      onTosAccepted = {
        showTosDialog = false
        tosViewModel.acceptTos()
      }
    )
  } else {
    // Show splash screen and navigate after delay
    SplashScreen()

    // Navigate to chat task after a delay
    LaunchedEffect(uiState.tasks) {
      if (uiState.tasks.isNotEmpty()) {
        delay(3000)
        val chatTask = uiState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
        if (chatTask != null) {
          navigateToTaskScreen(chatTask)
        } else {
          // Handle case where chat task is not found, maybe log an error.
          Log.e(TAG, "AI Chat task not found.")
        }
      }
    }
  }
}

@Composable
private fun SplashScreen() {
    val firstLineText = stringResource(R.string.app_name_first_part)
    val secondLineText = stringResource(R.string.app_name_second_part)

    // Animation for the splash screen
    val progress = rememberDelayedAnimationProgress(
        initialDelay = 200L,
        animationDurationMs = 1500,
        animationLabel = "splash screen animation",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress
                translationY = (16.dp * (1 - progress)).toPx()
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = firstLineText,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = secondLineText,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AppTitle() {
  val firstLineText = stringResource(R.string.app_name_first_part)
  val secondLineText = stringResource(R.string.app_name_second_part)
  val titleColor = MaterialTheme.customColors.appTitleGradientColors[1]
  val screenWidthInDp = LocalConfiguration.current.screenWidthDp.dp
  val fontSize = with(LocalDensity.current) { (screenWidthInDp.toPx() * 0.12f).toSp() }
  val titleStyle = homePageTitleStyle.copy(fontSize = fontSize, lineHeight = fontSize)

  // First line text "Google AI" and its animation.
  //
  // The animation starts with the first line of text swiping in from left to right, progressively
  // revealing itself in the title color (blue). Then, after a brief delay, the exact same text, but
  // in the onSurface color (which is black in light mode), begins its own left-to-right swiping
  // animation. This second animation is positioned directly on top of the first, appearing just as
  // the initial reveal is finishing or has just completed, creating a layered and dynamic visual
  // effect.
  Box {
    var delay = ANIMATION_INIT_DELAY
    SwipingText(
      text = firstLineText,
      style = titleStyle,
      color = titleColor,
      animationDelay = delay,
      animationDurationMs = TITLE_FIRST_LINE_ANIMATION_DURATION,
    )
    delay += (TITLE_FIRST_LINE_ANIMATION_DURATION * 0.3).toLong()
    SwipingText(
      text = firstLineText,
      style = titleStyle,
      color = MaterialTheme.colorScheme.onSurface,
      animationDelay = delay,
      animationDurationMs = TITLE_FIRST_LINE_ANIMATION_DURATION,
    )
  }
  // Second line text "Edge Gallery" and its animation.
  //
  // The initial animation is the same as the first line text. Right before it is done, the final
  // text with a gradient is revealed.
  Box {
    var delay = TITLE_SECOND_LINE_ANIMATION_START
    SwipingText(
      text = secondLineText,
      style = titleStyle,
      color = titleColor,
      modifier = Modifier.offset(y = (-16).dp),
      animationDelay = delay,
      animationDurationMs = TITLE_SECOND_LINE_ANIMATION_DURATION,
    )
    delay += (TITLE_SECOND_LINE_ANIMATION_DURATION * 0.3).toInt()
    SwipingText(
      text = secondLineText,
      style = titleStyle,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.offset(y = (-16).dp),
      animationDelay = delay,
      animationDurationMs = TITLE_SECOND_LINE_ANIMATION_DURATION,
    )
    delay += (TITLE_SECOND_LINE_ANIMATION_DURATION * 0.6).toInt()
    RevealingText(
      text = secondLineText,
      style =
        titleStyle.copy(
          brush = linearGradient(colors = MaterialTheme.customColors.appTitleGradientColors)
        ),
      modifier = Modifier.offset(x = (-16).dp, y = (-16).dp),
      animationDelay = delay,
      animationDurationMs = TITLE_SECOND_LINE_ANIMATION_DURATION2,
    )
  }
}

@Composable
private fun IntroText() {
  val url = "https://huggingface.co/litert-community"
  val linkColor = MaterialTheme.customColors.linkColor
  val uriHandler = LocalUriHandler.current

  // Intro text animation:
  //
  // fade in + slide up.
  val progress =
    rememberDelayedAnimationProgress(
      initialDelay = TITLE_SECOND_LINE_ANIMATION_START,
      animationDurationMs = CONTENT_COMPOSABLES_ANIMATION_DURATION,
      animationLabel = "intro text animation",
    )

  val introText = buildAnnotatedString {
    append("${stringResource(R.string.app_intro)} ")
    // TODO: Consolidate the link clicking logic into ui/common/ClickableLink.kt.
    withLink(
      link =
        LinkAnnotation.Url(
          url = url,
          styles =
            TextLinkStyles(
              style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
            ),
          linkInteractionListener = { _ ->
            firebaseAnalytics?.logEvent("resource_link_click", bundleOf("link_destination" to url))
            uriHandler.openUri(url)
          },
        )
    ) {
      append(stringResource(R.string.litert_community_label))
    }
  }
  Text(
    introText,
    style = MaterialTheme.typography.bodyMedium,
    modifier =
      Modifier.graphicsLayer {
        alpha = progress
        translationY = (CONTENT_COMPOSABLES_OFFSET_Y.dp * (1 - progress)).toPx()
      },
  )
}

@Composable
private fun CategoryTabHeader(
  sortedCategories: List<CategoryInfo>,
  selectedIndex: Int,
  onCategorySelected: (Int) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  val progress =
    rememberDelayedAnimationProgress(
      initialDelay = TASK_LIST_ANIMATION_START,
      animationDurationMs = CONTENT_COMPOSABLES_ANIMATION_DURATION,
      animationLabel = "task card animation",
    )

  LazyRow(
    state = listState,
    modifier =
      Modifier.fillMaxWidth().padding(bottom = 32.dp).graphicsLayer {
        alpha = progress
        translationY = (CONTENT_COMPOSABLES_OFFSET_Y.dp * (1 - progress)).toPx()
      },
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(key = "spacer_start") { Spacer(modifier = Modifier.width(8.dp)) }
    itemsIndexed(items = sortedCategories) { index, category ->
      Row(
        modifier =
          Modifier.height(40.dp)
            .clip(CircleShape)
            .background(
              color =
                if (selectedIndex == index) MaterialTheme.customColors.tabHeaderBgColor
                else Color.Transparent
            )
            .clickable {
              onCategorySelected(index)

              // Scroll to clicked item when the item is not fully inside view.
              scope.launch {
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                val targetItem =
                  visibleItems.find {
                    // +1 because the first item is the item keyed at spacer_start.
                    it.index == index + 1
                  }
                if (
                  targetItem == null ||
                    targetItem.offset < 0 ||
                    targetItem.offset + targetItem.size > listState.layoutInfo.viewportSize.width
                ) {
                  listState.animateScrollToItem(index = index)
                }
              }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          getCategoryLabel(context = context, category = category),
          modifier = Modifier.padding(horizontal = 16.dp),
          style = MaterialTheme.typography.labelLarge,
          color =
            if (selectedIndex == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    item(key = "spacer_end") { Spacer(modifier = Modifier.width(8.dp)) }
  }
}

@Composable
private fun TaskList(
  pagerState: PagerState,
  sortedCategories: List<CategoryInfo>,
  tasksByCategories: Map<String, List<Task>>,
  navigateToTaskScreen: (Task) -> Unit,
) {
  // Model list animation:
  //
  // 1.  Slide Up: The entire column of task cards translates upwards,
  // 2.  Fade in one by one: The task card fade in one by one. See TaskCard for details.
  val progress =
    rememberDelayedAnimationProgress(
      initialDelay = TASK_LIST_ANIMATION_START,
      animationDurationMs = CONTENT_COMPOSABLES_ANIMATION_DURATION,
      animationLabel = "task card animation",
    )

  HorizontalPager(
    state = pagerState,
    verticalAlignment = Alignment.Top,
    contentPadding = PaddingValues(horizontal = 20.dp),
  ) { pageIndex ->
    val tasks = tasksByCategories[sortedCategories[pageIndex].id]!!
    Column(
      modifier =
        Modifier.fillMaxWidth().padding(4.dp).graphicsLayer {
          translationY = (CONTENT_COMPOSABLES_OFFSET_Y.dp * (1 - progress)).toPx()
        },
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      var index = 0
      for (task in tasks) {
        TaskCard(
          task = task,
          index = index,
          onClick = { navigateToTaskScreen(task) },
          modifier = Modifier.fillMaxWidth(),
        )
        index++
      }
    }
  }
}

@Composable
private fun TaskCard(task: Task, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  // Observes the model count and updates the model count label with a fade-in/fade-out animation
  // whenever the count changes.
  val modelCount by remember {
    derivedStateOf {
      val trigger = task.updateTrigger.value
      if (trigger >= 0) {
        task.models.size
      } else {
        0
      }
    }
  }
  val modelCountLabel by remember {
    derivedStateOf {
      when (modelCount) {
        1 -> context.getString(R.string.one_model)
        else -> context.getString(R.string.n_models, modelCount)
      }
    }
  }
  var curModelCountLabel by remember { mutableStateOf("") }
  var modelCountLabelVisible by remember { mutableStateOf(true) }

  LaunchedEffect(modelCountLabel) {
    if (curModelCountLabel.isEmpty()) {
      curModelCountLabel = modelCountLabel
    } else {
      modelCountLabelVisible = false
      delay(TASK_COUNT_ANIMATION_DURATION.toLong())
      curModelCountLabel = modelCountLabel
      modelCountLabelVisible = true
    }
  }

  // Task card animation:
  //
  // This animation makes the task cards appear with a delayed fade-in effect. Each card will become
  // visible sequentially, starting after an initial delay and then with an additional offset for
  // subsequent cards.
  val progress =
    rememberDelayedAnimationProgress(
      initialDelay = TASK_LIST_ANIMATION_START + index * TASK_CARD_ANIMATION_DELAY_OFFSET,
      animationDurationMs = TASK_CARD_ANIMATION_DURATION,
      animationLabel = "task card animation",
    )

  Card(
    modifier =
      modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick).graphicsLayer {
        alpha = progress
      },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.customColors.taskCardBgColor),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Title and model count
      Column {
        Text(
          task.label,
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          curModelCountLabel,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      // Icon.
      TaskIcon(task = task, width = 40.dp)
    }
  }
}

// Helper function to get the file name from a URI
fun getFileName(context: Context, uri: Uri): String? {
  if (uri.scheme == "content") {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
          return cursor.getString(nameIndex)
        }
      }
    }
  } else if (uri.scheme == "file") {
    return uri.lastPathSegment
  }
  return null
}

private fun getCategoryLabel(context: Context, category: CategoryInfo): String {
  val stringRes = category.labelStringRes
  val label = category.label
  if (stringRes != null) {
    return context.getString(stringRes)
  } else if (label != null) {
    return label
  }
  return context.getString(R.string.category_unlabeled)
}

// @Preview
// @Composable
// fun HomeScreenPreview() {
//   GalleryTheme {
//     HomeScreen(
//       modelManagerViewModel = PreviewModelManagerViewModel(context = LocalContext.current),
//       navigateToTaskScreen = {},
//     )
//   }
// }
