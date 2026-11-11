# 아빠 바빠 (Appa Bappa)

차로 변경이나 회전처럼 운전에 집중해야 하는 순간이 오면, 실내 조명 색을 변경해 동승자에게 운전자가 바쁘다는 것을 알려 주는 Pleos Connect 앱입니다.

## 테스트 환경

- 에뮬레이터: Pleos Connect v2.1.0(Android API 34, arm64)
- 화면 규격: 2560 x 1440 px, 160 dpi(에뮬레이터 기본 설정)
- 앱 영역 전체를 사용하는 가로 창과 화면을 나눈 세로 창에서 동작을 확인했습니다.
- 실제 차량에서는 실행하지 않았습니다.

## APK

`apk/Oube_AppaBappa_release.apk`(버전 0.1.0, release 빌드)

## 실행 방법

1. Pleos Connect 에뮬레이터를 실행하고 APK를 설치합니다.

   ```
   adb install apk/Oube_AppaBappa_release.apk
   ```

2. 에뮬레이터의 앱 목록에서 "아빠 바빠"를 실행합니다.
3. 권한 안내 화면에서 "계속"을 누르고, 권한 요청 창 두 개에서 "허용"을 누릅니다. 차속 권한은 권한 요청 창에 위치 정보로 표시됩니다.

## 동작 확인

### 가상 콘솔로 주행

1. 가상 콘솔에서 기어를 D로 변속하고 ACCEL을 길게 눌러 가속합니다.
2. Emulation Controls의 Convenience에서 Turn Signal State를 Blink right로 변경합니다. 앱이 실내 조명을 주황색으로 변경합니다.
3. Turn Signal State를 Off로 변경합니다. 앱이 주황색을 5초(시속 60km 이상에서 방향지시등을 켰으면 10초) 동안 유지한 뒤, 조명을 초록색으로 3초 동안 켜고 원래 색으로 되돌립니다.
4. BRAKE를 눌러 정차한 뒤 기어를 P로 변속합니다. 앱이 도착 뒤 화면에 동승자가 기다려 준 횟수와 시간을 표시합니다.

조명 색은 가상 콘솔의 SYSTEMS 칸 AMBIENT 항목과 BODY 칸의 차 그림에 표시됩니다. 가상 콘솔 탭이 브라우저에서 다른 탭 뒤에 있으면 조명 색이 변경되지 않습니다. 주행하는 동안 가상 콘솔 탭을 앞에 두어 주세요.

### 앱의 시연

기어가 P일 때 지금 상태 화면의 "시연"을 누르면 시연 목록이 표시됩니다.

- 장면 재생: 고속도로 합류, 급제동 같은 장면 10개가 있습니다. 장면을 재생하는 동안 앱이 실내 조명 색도 변경합니다.
- 직접 조작해 보기: 화면의 페달, 스티어링 휠, 방향지시등으로 가상의 차를 운전합니다.

## 빌드

JDK 17과 Android SDK(compileSdk 36)가 필요합니다. Pleos Connect SDK는 빌드할 때 Gradle이 내려받습니다.

```
./gradlew assembleDebug   # debug APK
./gradlew check           # 컴파일, Android Lint, detekt, ktlint, 단위 테스트
```

release 서명 키는 저장소에 포함하지 않았습니다.

## 오픈소스 라이선스

### 라이브러리

| 라이브러리 | 라이선스 |
| --- | --- |
| AndroidX Core KTX, Activity Compose | Apache License 2.0 |
| Jetpack Compose (Foundation, UI, Animation) | Apache License 2.0 |
| Kotlin 표준 라이브러리, kotlinx.coroutines | Apache License 2.0 |

Pleos Connect SDK(Vehicle, NaviHelper)는 저장소에 포함하지 않고 Gradle 의존성으로 내려받아 사용합니다. SDK에는 Pleos Connect SDK 라이선스가 적용됩니다.

### 아이콘

직접 조작 시연 화면의 내비게이션 안내 표시에는 [Lucide](https://github.com/lucide-icons/lucide) 아이콘 arrow-up-left, corner-up-left의 모양을, 시연 목록의 재생 아이콘에는 Lucide 아이콘 play의 모양을 사용했습니다. Lucide는 ISC 라이선스로 배포됩니다. arrow-up-left와 corner-up-left는 Lucide의 라이선스 파일에 Feather에서 가져온 아이콘으로 명시되어 있어서, Feather의 MIT 라이선스 고지도 함께 포함합니다. 급제동 버튼의 경고 아이콘과 뒤로 가기 화살표는 Pleos Connect 디자인 가이드가 제공하는 Figma 파일(design assets)의 아이콘입니다. 계기판의 방향지시등 표시는 직접 그렸습니다.

```
ISC License

Copyright (c) 2026 Lucide Icons and Contributors

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
```

```
The MIT License (MIT)

Copyright (c) 2013-present Cole Bemis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
