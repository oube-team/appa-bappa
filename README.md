# 아빠 바빠 (Appa Bappa)

차로 변경이나 회전처럼 운전에 집중해야 하는 순간이 오면, 실내 조명 색을 변경해 동승자에게 운전자가 바쁘다는 것을 알려 주는 Pleos Connect 앱입니다.

## 테스트 환경

- Pleos Connect v2.1.0 에뮬레이터

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
