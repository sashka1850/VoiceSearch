package com.voicesearch.app.ui.onboarding

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicesearch.core.ui.components.PrimaryButton
import kotlinx.coroutines.launch

/**
 * Three-slide intro shown on the very first launch. Each slide carries one
 * core idea (impressions are short — keep the copy tight). Skip + Next on
 * non-last slides, "Начать" on the last slide.
 *
 * Marks the user as "seen" through [OnboardingViewModel] regardless of
 * which exit path triggers it (Skip or Начать), so the user never sees
 * this twice.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val slides = remember { defaultSlides() }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    fun finish() {
        viewModel.markSeen()
        onFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Top bar: Skip on every slide except the last.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (pagerState.currentPage < slides.lastIndex) {
                    TextButton(onClick = ::finish) { Text("Пропустить") }
                } else {
                    Spacer(Modifier.height(40.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                SlideContent(slide = slides[page])
            }

            PageDots(
                count = slides.size,
                current = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = if (pagerState.currentPage == slides.lastIndex) "Начать" else "Дальше",
                onClick = {
                    if (pagerState.currentPage == slides.lastIndex) {
                        finish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(ICON_BG_SIZE_DP.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(ICON_SIZE_DP.dp),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    LaunchedEffect(current) { /* relay for recomposition */ }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val isActive = i == current
            Box(
                modifier = Modifier
                    .size(if (isActive) ACTIVE_DOT_DP.dp else INACTIVE_DOT_DP.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private fun defaultSlides() = listOf(
    OnboardingSlide(
        icon = Icons.Outlined.TableChart,
        title = "Импортируйте таблицу",
        body = "Добавьте файл XLSX/CSV с устройства или вставьте публичную ссылку с " +
            "Яндекс.Документов или Яндекс.Диска. Приложение разберёт столбцы за вас.",
    ),
    OnboardingSlide(
        icon = Icons.Filled.Tune,
        title = "Настройте поиск",
        body = "Укажите столбец поиска, столбец отметки и наименование. Приложение " +
            "само поймёт общий префикс и подскажет сколько последних символов произносить.",
    ),
    OnboardingSlide(
        icon = Icons.Filled.Mic,
        title = "Ищите голосом или вводом",
        body = "Зажмите микрофон и произнесите код — приложение найдёт строку и " +
            "поставит отметку. Или используйте встроенную клавиатуру.",
    ),
)

private const val ICON_BG_SIZE_DP = 120
private const val ICON_SIZE_DP = 56
private const val ACTIVE_DOT_DP = 10
private const val INACTIVE_DOT_DP = 8
