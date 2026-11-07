package studio.oube.appabappa

// Pleos SDK의 리스너 등록과 해제, NaviHelper 요청은 권한이 없으면 결과 콜백 대신 그 자리에서 SecurityException을
// 던진다(SDK 소스). 실차에서 권한이 없어도 서비스가 종료되지 않도록, 실패를 기록하고 나머지 신호로 계속 판단한다.
// 확인, 조회, 설정 함수는 권한 오류를 onFailed로 알려 주므로 이 함수로 감싸지 않는다.
internal inline fun sdkCall(what: String, call: () -> Unit) {
    runCatching { call() }.onFailure { AppLog.w("$what failed", it) }
}
