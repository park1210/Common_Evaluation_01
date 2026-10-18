# Django 이미지 블로그 & Android PhotoViewer

> Mobile/Web Service Project

기존 Django 이미지 블로그 서버와 Android 클라이언트를 연동하여  
이미지 게시물 조회, 업로드, 검색 및 정렬 등의 기능을 구현한 프로젝트입니다.

본 프로젝트는 **경희대학교 모바일/웹서비스 프로그래밍 수업에서 제공된  
Django 이미지 블로그 예제 코드를 기반으로 수정 및 확장하여 구현하였습니다.**

- Original example code: **kiokahn@khu.ac.kr**
- Modified & Extended for Mobile/Web Service Project

---

## 프로젝트 구조

```text
ROOT/
├── PhotoBlogServer/     # Django Server
├── PhotoViewer/         # Android Client
├── README.md
└── ...
```

### PhotoBlogServer

Django 및 Django REST Framework를 이용한 이미지 블로그 서버입니다.

주요 기능:

- 이미지 게시물 생성
- 이미지 게시물 조회
- 게시물 수정
- REST API 제공
- 제목 및 본문 검색
- 최신순 / 오래된순 / 제목순 정렬
- 반복 POST 요청 감지 및 중복 게시 방지
- PythonAnywhere Cloud Server 배포

### PhotoViewer

Java 기반 Android Client입니다.

주요 기능:

- Django REST API를 통한 게시물 동기화
- 이미지 / 제목 / 본문 / 게시 날짜 표시
- 새로운 게시물 업로드
- 게시물 검색
- 최신순 / 오래된순 / 제목순 / 제목 역순 정렬
- 작성 중 게시물 자동 임시저장 및 복원
- 게시물 목록 위치 기억 및 이어보기
- PythonAnywhere Cloud Server 연동

---

# Django Server 실행 방법

## 1. 가상환경 생성

`PhotoBlogServer` 폴더로 이동합니다.

```bash
cd PhotoBlogServer
```

가상환경을 생성합니다.

### Windows

```powershell
python -m venv .venv
```

가상환경 활성화:

```powershell
.\.venv\Scripts\Activate.ps1
```

### Linux / macOS

```bash
python3 -m venv venv
source venv/bin/activate
```

정상적으로 실행되면 터미널에 가상환경 이름이 표시됩니다.

```text
(.venv)
```

또는

```text
(venv)
```

---

## 2. 라이브러리 설치

```bash
pip install -r requirements.txt
```

---

## 3. DB 마이그레이션

```bash
python manage.py migrate
```

---

## 4. 관리자 계정 생성

```bash
python manage.py createsuperuser
```

안내에 따라 관리자 ID, 이메일, 비밀번호를 설정합니다.

---

## 5. Django 서버 실행

```bash
python manage.py runserver
```

정상 실행 시 기본 주소는 다음과 같습니다.

```text
http://127.0.0.1:8000/
```

---

# 주요 접속 주소

## 사용자 페이지

```text
http://127.0.0.1:8000/
```

## 관리자 페이지

```text
http://127.0.0.1:8000/admin/
```

## REST API

```text
http://127.0.0.1:8000/api_root/Post/
```

---

# REST API 검색 및 정렬

REST API에서는 게시물의 제목과 본문을 검색할 수 있습니다.

## 검색

```text
/api_root/Post/?search=검색어
```

예:

```text
/api_root/Post/?search=제주
```

제목 또는 본문에 `제주`가 포함된 게시물만 반환합니다.

---

## 최신순 정렬

```text
/api_root/Post/?ordering=-created_date
```

## 오래된순 정렬

```text
/api_root/Post/?ordering=created_date
```

## 제목순 정렬

```text
/api_root/Post/?ordering=title
```

## 제목 역순 정렬

```text
/api_root/Post/?ordering=-title
```

검색과 정렬을 동시에 사용할 수도 있습니다.

```text
/api_root/Post/?search=제주&ordering=title
```

---

# 중복 게시 방지

동일한 게시물이 짧은 시간 안에 반복적으로 POST되는 것을 방지하기 위해  
서버에서 중복 게시물 검사를 수행합니다.

최근 10초 이내에 다음 값이 모두 동일한 게시물이 존재하는 경우:

```text
작성자
제목
본문
```

새로운 게시물을 생성하지 않고 다음 상태를 반환합니다.

```text
HTTP 409 Conflict
```

이를 통해 네트워크 지연이나 반복 입력으로 인한 중복 게시물 생성을 방지합니다.

---

# Android PhotoViewer

Android Client 프로젝트는 다음 위치에 있습니다.

```text
PhotoViewer/
```

Android Studio에서 `PhotoViewer` 폴더를 열어 실행할 수 있습니다.

---

## Server 주소

Android Client는 PythonAnywhere에 배포된 Django Server와 연결됩니다.

`MainActivity.java`와 `UploadActivity.java`에서 다음 Server URL을 사용합니다.

```java
private static final String SITE_URL =
        "https://jaewoo1210.pythonanywhere.com";
```

---

# PythonAnywhere Cloud Server

Django Server는 PythonAnywhere에서도 실행할 수 있도록 구성하였습니다.

Cloud Server:

```text
https://jaewoo1210.pythonanywhere.com
```

REST API:

```text
https://jaewoo1210.pythonanywhere.com/api_root/Post/
```

검색 예시:

```text
https://jaewoo1210.pythonanywhere.com/api_root/Post/?search=검색어
```

---

# 테스트

Django 테스트 실행:

```bash
python manage.py test blog
```

Code Coverage 측정:

```bash
coverage erase
coverage run manage.py test blog
coverage report
```

HTML Coverage Report 생성:

```bash
coverage html
```

생성된 결과:

```text
htmlcov/index.html
```

---

# 구현된 추가 편의 기능

### 1. 게시물 작성 내용 자동 임시저장 및 복원

게시물 작성 중 입력한 제목, 본문 및 선택 이미지를 자동으로 저장합니다.  
작성 화면을 벗어난 뒤 다시 진입하면 기존 작성 내용을 이어서 작성할 수 있습니다.

### 2. 게시물 목록 위치 기억 및 이어보기

게시물 목록을 탐색하다 다른 화면으로 이동한 경우 이전 스크롤 위치를 저장합니다.  
목록으로 돌아오면 이전에 보던 위치에서 탐색을 이어갈 수 있습니다.

### 3. Server 게시물 검색 및 정렬

Django REST API에서 제목과 본문을 기준으로 검색할 수 있으며  
작성일, 게시일 및 제목을 기준으로 결과를 정렬할 수 있습니다.

### 4. Client 검색 및 정렬 UI

Android Client에서 검색어를 입력하고 서버 검색 API를 이용할 수 있습니다.  
최신순, 오래된순, 제목순, 제목 역순 정렬을 지원합니다.

### 5. Server 반복 요청 감지 및 중복 게시 방지

최근 10초 이내 동일한 작성자, 제목, 본문으로 게시 요청이 반복되는 경우  
서버에서 해당 요청을 중복 요청으로 판단하여 새로운 게시물이 생성되지 않도록 합니다.

---

# 참고 및 출처

본 프로젝트의 Django 이미지 블로그 기본 코드는  
**경희대학교 모바일/웹서비스 프로그래밍 수업에서 교수자가 제공한 예제 코드**를 기반으로 합니다.

```text
Original Django Image Blog Example
by kiokahn@khu.ac.kr
```

해당 예제 코드를 기반으로 프로젝트 요구사항에 맞추어  
Android Client 연동, REST API 활용, Cloud Server 배포 및 추가 편의 기능 등을 수정·확장하였습니다.