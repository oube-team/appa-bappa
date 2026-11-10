package studio.oube.appabappa.ui

import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.status.Reason

private const val HANGUL_FIRST = '가'
private const val HANGUL_LAST = '힣'
private const val FINALS_PER_SYLLABLE = 28

// 받침이 있으면 앞의 조사를, 없으면 뒤의 조사를 붙인다(아빠가, 삼촌이).
internal fun withJosa(word: String, withFinal: String, withoutFinal: String): String {
    val last = word.lastOrNull()
    val hasFinal =
        last != null &&
            last in HANGUL_FIRST..HANGUL_LAST &&
            (last - HANGUL_FIRST) % FINALS_PER_SYLLABLE != 0
    return word + if (hasFinal) withFinal else withoutFinal
}

// 지금 상태 화면의 문구다. 제목은 두 줄로 나눠 보여 주고, 문구가 같으면 다시 그리지 않도록 값으로 비교한다.
internal data class StatusCopy(val title: List<String>, val reason: String?, val sub: String)

// 실내 조명으로 알리기를 끄면 조명 색이 바뀌지 않으므로, FREE 문구에서 조명 색 대신 화면으로 알린다고 안내한다.
// approaching은 바쁜 이유로 알리는 안내 지점에 아직 닿기 전이면 true다(Reason.isApproaching). 이때는 BUSY에서도 SOON
// 문장으로 알려서, 아직 시작하지 않은 회전이나 합류를 하고 있다고 알리지 않는다.
internal fun statusCopy(
    mood: Mood,
    reason: Reason?,
    approaching: Boolean,
    who: String,
    lightEnabled: Boolean
): StatusCopy {
    val subject = withJosa(who, "이", "가")
    return when (mood) {
        Mood.FREE -> StatusCopy(
            listOf("지금은 ${withJosa(who, "이랑", "랑")}", "이야기해도 돼요"),
            null,
            if (lightEnabled) {
                "운전에 집중해야 할 때는 조명 색으로 알려 드려요"
            } else {
                "운전에 집중해야 할 때는 화면으로 알려 드려요"
            }
        )

        Mood.SOON -> StatusCopy(
            listOf("$subject 곧 운전에", "집중해야 해요"),
            reason?.lines()?.soon,
            "하던 이야기를 짧게 마무리해 보세요"
        )

        Mood.BUSY -> StatusCopy(
            listOf("$subject 운전에", "집중하고 있어요"),
            reason?.lines()?.let { lines -> lines.soon.takeIf { approaching } ?: lines.busy },
            "끝나면 초록으로 알려 드릴게요"
        )

        Mood.RECOVERING -> StatusCopy(
            listOf("$subject 운전에", "집중하고 있어요"),
            reason?.lines()?.recovering,
            "다시 바빠질 수 있어서 잠시 주황을 유지해요"
        )

        Mood.THANKS -> StatusCopy(
            listOf("기다려 줘서", "고마워요"),
            null,
            "이제 다시 이야기해도 돼요"
        )
    }
}

// 동승자가 읽을 바쁜 이유 한 줄이다. 엔진은 안내 지점이 가까울 때만 SOON으로 판단하므로, 안내 종류에만 soon 문장이 있다.
// soon 문장은 SOON 상태일 때와, 바쁜 상태인데 그 안내 지점에 아직 닿기 전일 때 사용한다.
internal class ReasonLines(val soon: String?, val busy: String, val recovering: String)

// 이유마다 문장 세 개를 나열한 표라서 길지만, when으로 두면 이유를 추가할 때 문장을 빠뜨리지 않는다.
@Suppress("LongMethod")
internal fun Reason.lines(): ReasonLines = when (this) {
    // NaviHelper javadoc의 TURN_DEVIDE_MERGE 설명은 "Split and merge road"뿐이라 고속도로인지, 합류인지 분기인지 알 수
    // 없으므로, 도로 종류는 빼고 합류와 분기를 함께 말한다.
    Reason.MERGE -> ReasonLines(
        "합류나 분기 지점이 가까워요",
        "합류나 분기 지점을 지나고 있어요",
        "방금 합류나 분기 지점을 지났어요"
    )

    Reason.HIGHWAY_ENTRY -> ReasonLines(
        "고속도로 진입로가 가까워요",
        "고속도로로 들어가고 있어요",
        "방금 고속도로로 들어갔어요"
    )

    Reason.HIGHWAY_EXIT -> ReasonLines(
        "나들목 출구가 가까워요",
        "나들목으로 나가고 있어요",
        "방금 나들목으로 나갔어요"
    )

    // 안내 없이 켠 방향지시등이다. 앱은 방향지시등만 알고 차로를 실제로 변경했는지는 알 수 없어서, 방향지시등을 켜고
    // 껐다고 알린다. 엔진은 안내 지점이 가까울 때만 SOON으로 판단하므로 SOON 문장이 없다.
    Reason.TURN_SIGNAL -> ReasonLines(
        null,
        "방향지시등을 켜고 있어요",
        "방금 방향지시등을 껐어요"
    )

    Reason.LEFT_TURN -> ReasonLines(
        "좌회전할 지점이 가까워요",
        "좌회전하고 있어요",
        "방금 좌회전했어요"
    )

    Reason.RIGHT_TURN -> ReasonLines(
        "우회전할 지점이 가까워요",
        "우회전하고 있어요",
        "방금 우회전했어요"
    )

    Reason.U_TURN -> ReasonLines(
        "유턴할 지점이 가까워요",
        "유턴하고 있어요",
        "방금 유턴했어요"
    )

    // 회전 안내 지점을 회전하지 않고 지나간 경우다. 지점을 지난 뒤(RECOVERING)에만 이 이유를 사용한다.
    Reason.PASSED_LEFT_TURN -> ReasonLines(
        null,
        "좌회전할 지점을 지나고 있어요",
        "방금 좌회전할 지점을 지났어요"
    )

    Reason.PASSED_RIGHT_TURN -> ReasonLines(
        null,
        "우회전할 지점을 지나고 있어요",
        "방금 우회전할 지점을 지났어요"
    )

    Reason.PASSED_U_TURN -> ReasonLines(
        null,
        "유턴할 지점을 지나고 있어요",
        "방금 유턴할 지점을 지났어요"
    )

    Reason.ROUNDABOUT -> ReasonLines(
        "회전교차로가 가까워요",
        "회전교차로를 지나고 있어요",
        "방금 회전교차로를 지났어요"
    )

    Reason.JUNCTION -> ReasonLines(
        "갈림길이 가까워요",
        "갈림길을 지나고 있어요",
        "방금 갈림길을 지났어요"
    )

    Reason.TOLLGATE -> ReasonLines(
        "요금소가 가까워요",
        "요금소를 지나고 있어요",
        "방금 요금소를 지났어요"
    )

    Reason.REST_AREA -> ReasonLines(
        "휴게소 진입로가 가까워요",
        "휴게소로 들어가고 있어요",
        "방금 휴게소로 들어갔어요"
    )

    Reason.DROWSY_REST_AREA -> ReasonLines(
        "졸음쉼터 진입로가 가까워요",
        "졸음쉼터로 들어가고 있어요",
        "방금 졸음쉼터로 들어갔어요"
    )

    Reason.TURNING -> ReasonLines(
        null,
        "회전하고 있어요",
        "방금 회전을 마쳤어요"
    )

    Reason.REVERSING -> ReasonLines(
        null,
        "후진하고 있어요",
        "방금 후진을 마쳤어요"
    )

    Reason.HARD_BRAKING -> ReasonLines(
        null,
        "급하게 감속하고 있어요",
        "방금 급하게 감속했어요"
    )

    Reason.FORWARD_COLLISION_WARNING -> ReasonLines(
        null,
        "전방 충돌 경고가 켜졌어요",
        "방금 전방 충돌 경고가 꺼졌어요"
    )

    Reason.BLIND_SPOT_WARNING -> ReasonLines(
        null,
        "후측방 경고가 켜졌어요",
        "방금 후측방 경고가 꺼졌어요"
    )
}
