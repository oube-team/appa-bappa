package studio.oube.appabappa.light

// 앱이 종료됐다가 다시 시작해도 조명의 원래 색을 알 수 있도록 저장해 두는 값이다.
// original은 앱이 조명 색을 변경하기 전의 색이다.
// changeFrom과 changeTo는 앱이 마지막으로 요청한 변경의 출발 색과 도착 색이다.
internal data class LightRecord(
    val original: Int? = null,
    val changeFrom: Int? = null,
    val changeTo: Int? = null
) {
    // 시작할 때 조회한 색(current)으로 원래 색을 정한 기록을 반환한다. 색을 조회하지 못했거나 앱이 켠 색이 남아 있으면
    // 저장해 둔 원래 색을 그대로 사용한다.
    fun atStart(current: Int?): LightRecord =
        if (current == null || leftByApp(current)) this else withUserColor(current)

    // 조명에 사용자가 고른 색이 켜져 있으면 앱이 켠 색이 남아 있지 않으므로, 그 색을 원래 색으로 삼고 마지막 변경을 삭제한다.
    // 마지막 변경을 남겨 두면, 사용자가 나중에 조명을 그 변경의 출발 색이나 도착 색으로 되돌렸을 때 앱이 켠 색으로
    // 잘못 판단해 조명을 예전에 저장한 원래 색으로 변경한다.
    fun withUserColor(color: Int) = copy(original = color, changeFrom = null, changeTo = null)

    fun withChange(from: Int, to: Int) = copy(changeFrom = from, changeTo = to)

    // 앱이 조명 색을 변경한 채로 종료됐을 수 있어서, 조회한 색이 마지막 변경에서 켜질 수 있었던 색이면 앱이 켠 색이
    // 남아 있는 것으로 판단한다. 서서히 변경했는지는 저장하지 않아서, 중간색도 늘 켜질 수 있었던 색으로 판단한다.
    private fun leftByApp(current: Int): Boolean {
        if (changeFrom == null || changeTo == null) return false
        return fadePath(changeFrom, changeTo).any { LightPlan.sameColor(it, current) }
    }
}
