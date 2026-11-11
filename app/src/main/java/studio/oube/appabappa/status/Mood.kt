package studio.oube.appabappa.status

// 앱 화면이 동승자에게 보여 주는 상태다. 엔진의 BUSY와 VERY_BUSY는 동승자에게 같은 뜻이라 BUSY 하나로 보여 주고,
// 바쁜 상황이 끝난 뒤 조명이 초록색인 동안은 THANKS를 보여 준다.
enum class Mood {
    FREE,
    SOON,
    BUSY,
    RECOVERING,
    THANKS
}
