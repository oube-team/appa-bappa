package studio.oube.appabappa.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.status.Reason
import studio.oube.appabappa.ui.component.AppHeader
import studio.oube.appabappa.ui.component.ButtonKind
import studio.oube.appabappa.ui.component.EdgeLight
import studio.oube.appabappa.ui.component.PleosButton
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.balanced

private const val COPY_FADE_MILLIS = 600

// 서비스를 껐으면 바쁜 순간을 판단하지 않고 차속 권한이 없으면 후진만 판단하므로, FREE일 때 상태 문구 대신 이 안내를
// 보여 준다.
enum class Notice(val title: List<String>, val body: String) {
    TURNED_OFF(
        listOf("바쁜 순간 알리기가", "꺼져 있어요"),
        "설정에서 다시 켜면 운전에 집중해야 할 때 알려 드려요"
    ),

    SPEED_NEEDED(
        listOf("차속 권한이 없어서", "후진할 때만 알려 드려요"),
        "차속 권한은 위치 정보 권한으로 표시돼요. 허용하면 차로 변경과 급제동 같은 바쁜 순간을 알려 드려요"
    ),

    SPEED_BLOCKED(
        listOf("차속 권한이 없어서", "후진할 때만 알려 드려요"),
        "위치 정보 권한을 두 번 거절해서 다시 물어볼 수 없어요. 앱을 삭제한 뒤 새로 설치하면 다시 물어봐요"
    )
}

@Composable
internal fun StatusScreen(
    mood: Mood,
    reason: Reason?,
    who: String,
    lightEnabled: Boolean,
    parked: Boolean,
    canPlayScenes: Boolean,
    notice: Notice?,
    layout: AppLayout,
    onOpenScenes: () -> Unit,
    onOpenSettings: () -> Unit,
    onAskSpeed: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        EdgeLight(mood)
        // 주행 중에는 화면을 조작하지 않도록 시연과 설정 버튼을 숨긴다. 시연은 서비스가 재생하므로, 바쁜 순간 알리기를 꺼서
        // 서비스가 실행 중이 아니면 시연 버튼도 숨긴다.
        AppHeader("$who 바빠") {
            if (parked) {
                if (canPlayScenes) PleosButton("시연", onOpenScenes)
                PleosButton("설정", onOpenSettings)
            }
        }
        if (notice == null || mood != Mood.FREE) {
            StatusCopyBlock(statusCopy(mood, reason, who, lightEnabled), layout.statusCopy)
        } else {
            NoticeBlock(notice, layout, onAskSpeed.takeIf { parked })
        }
    }
}

// 상태가 한 번에 바뀌어도 동승자가 놀라지 않도록, 이전 문구와 새 문구를 COPY_FADE_MILLIS 동안 겹쳐서 교체한다.
@Composable
internal fun StatusCopyBlock(copy: StatusCopy, box: CopyBox) {
    Crossfade(
        targetState = copy,
        animationSpec = tween(COPY_FADE_MILLIS),
        label = "status copy",
        modifier = Modifier.offset(box.left, box.top).width(box.width)
    ) { shown ->
        Column {
            for (line in shown.title) {
                PleosText(
                    line,
                    box.style.title,
                    Pleos.colors.text.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(Modifier.height(box.gap))
            shown.reason?.let { line ->
                PleosText(line, box.style.reason, Pleos.colors.text.primary)
                Spacer(Modifier.height(8.dp))
            }
            PleosText(shown.sub, box.style.sub.balanced(), Pleos.colors.text.secondary)
        }
    }
}

// 주행 중에는 화면을 조작하지 않도록 권한을 허용하는 버튼을 숨긴다(onAskSpeed가 null).
@Composable
private fun NoticeBlock(notice: Notice, layout: AppLayout, onAskSpeed: (() -> Unit)?) {
    val box = layout.statusCopy
    Column(Modifier.offset(box.left, box.top).width(box.width)) {
        for (line in notice.title) {
            PleosText(
                line,
                layout.noticeTitle,
                Pleos.colors.text.primary,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(Modifier.height(box.gap))
        PleosText(notice.body, box.style.sub.balanced(), Pleos.colors.text.secondary)
        if (notice == Notice.SPEED_NEEDED && onAskSpeed != null) {
            Spacer(Modifier.height(40.dp))
            PleosButton("위치 정보 허용하기", onAskSpeed, kind = ButtonKind.FILLED)
        }
    }
}
