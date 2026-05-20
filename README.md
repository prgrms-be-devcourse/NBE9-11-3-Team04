# NBE9-11-2-Team04
백엔드 9기 11회차 2차 프로젝트 4팀(404 에러 없조)

## 📌 DevC (Dev Community) [http://43.200.4.180.nip.io/]

개발자를 위한 정보 공유 및 커뮤니티 플랫폼입니다.

게시글, 댓글, 좋아요, 북마크, 알림, 신고 기능을 통해 효율적인 정보 탐색과 사용자 간 소통을 지원합니다.

----------------------------------------------------------------------------------

## 🧩 프로젝트 소개

DevC는 기존 개발 커뮤니티의 비효율적인 정보 탐색과 사용자 활동 관리 문제를 개선하기 위해 제작된 서비스입니다.

카테고리 기반 탐색과 검색 기능을 통해 원하는 정보를 빠르게 찾을 수 있으며 좋아요·북마크·마이페이지 기능을 통해 사용자 활동을 체계적으로 관리할 수 있도록 설계했습니다.

또한 댓글 및 알림 기능을 통해 사용자 간 실시간 소통을 강화하고 관리자 기능을 통해 안정적인 커뮤니티 운영이 가능하도록 구현했습니다.

----------------------------------------------------------------------------------
## 📊 통합 시스템 구성도

<img width="1441" height="871" alt="통합_시스템_구성도" src="https://github.com/user-attachments/assets/26688ed3-037c-44bd-8aef-5f617303c2bd" />

----------------------------------------------------------------------------------

## ⚙️ 기술 스택

### Frontend

<img src="https://img.shields.io/badge/Next.js 16-000000?style=for-the-badge&logo=nextdotjs&logoColor=white"> <img src="https://img.shields.io/badge/React 19-61DAFB?style=for-the-badge&logo=react&logoColor=black">
<img src="https://img.shields.io/badge/TypeScript 5-3178C6?style=for-the-badge&logo=typescript&logoColor=white">
<img src="https://img.shields.io/badge/Tailwind CSS 4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white">

### Backend

<img src="https://img.shields.io/badge/Kotlin 2.2.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"> <img src="https://img.shields.io/badge/Java 24-007396?style=for-the-badge&logo=openjdk&logoColor=white">
<img src="https://img.shields.io/badge/Spring Boot 4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
<img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
<img src="https://img.shields.io/badge/Spring Data JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/OAuth2 Client-4285F4?style=for-the-badge&logo=google&logoColor=white">
<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white">
<img src="https://img.shields.io/badge/Springdoc OpenAPI-85EA2D?style=for-the-badge&logo=openapiinitiative&logoColor=black">

### Database & Infra

<img src="https://img.shields.io/badge/MySQL 8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/H2-09476B?style=for-the-badge&logoColor=white">
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
<img src="https://img.shields.io/badge/Docker Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white">
<img src="https://img.shields.io/badge/AWS EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white">
<img src="https://img.shields.io/badge/GitHub Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">

### Test

<img src="https://img.shields.io/badge/JUnit 5-25A162?style=for-the-badge&logo=junit5&logoColor=white"> <img src="https://img.shields.io/badge/Mockito Kotlin-78A641?style=for-the-badge&logoColor=white">
<img src="https://img.shields.io/badge/Spring Security Test-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">


----------------------------------------------------------------------------------

## 📡 API 문서

- Notion API Docs: https://www.notion.so/2-33c15a0120548052a70cd10c147ef3db?p=5f415a01205482dea53c0198a5534ba1&pm=s

------------------------------------------------------------------------------

## 🚀 실행 방법

git clone https://github.com/your-repo.git

cd your-repo

./gradlew bootRun

----------------------------------------------------------------------------------

## 📁 프로젝트 구조

```text
├── back/DevC
│   └── src/main/kotlin/com/back/devc
│       ├── domain
│       │   ├── admin
│       │   ├── auth
│       │   ├── interaction
│       │   ├── member
│       │   └── post
│       └── global
│           ├── config
│           ├── entity
│           ├── exception
│           ├── initData
│           ├── response
│           └── security
├── frontend
│   ├── app
│   ├── components
│   └── lib
├── docker-compose.yml
└── .env.example
```

----------------------------------------------------------------------------------

## 주요 API

### 인증

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/api/auth/signup` | 이메일 회원가입 |
| POST | `/api/auth/login` | 이메일 로그인 |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/api/auth/oauth2/me` | OAuth2 로그인 상태 조회 |
| POST | `/api/auth/oauth2/exchange` | OAuth 로그인 코드 교환 |
| POST | `/api/auth/oauth2/signup/complete` | OAuth 추가 회원가입 완료 |

### 게시글

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/api/posts` | 게시글 목록 조회 |
| GET | `/api/posts/{postId}` | 게시글 상세 조회 |
| POST | `/api/posts` | 게시글 작성 |
| PUT | `/api/posts/{postId}` | 게시글 수정 |
| DELETE | `/api/posts/{postId}` | 게시글 삭제 |
| GET | `/api/admin/posts` | 관리자 게시글 목록 조회 |
| GET | `/api/admin/posts/{postId}` | 관리자 게시글 상세 조회 |

### 댓글 / 첨부파일

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/api/posts/{postId}/comments` | 댓글 목록 조회 |
| POST | `/api/posts/{postId}/comments` | 댓글 작성 |
| POST | `/api/comments/{commentId}/replies` | 대댓글 작성 |
| PATCH | `/api/comments/{commentId}` | 댓글 수정 |
| DELETE | `/api/comments/{commentId}` | 댓글 삭제 |
| POST | `/api/comments/{commentId}/attachments` | 댓글 첨부파일 업로드 |
| GET | `/api/comments/{commentId}/attachments` | 댓글 첨부파일 조회 |
| DELETE | `/api/comments/{commentId}/attachments/{attachmentId}` | 댓글 첨부파일 삭제 |

### 사용자 / 마이페이지

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/api/users/me` | 내 정보 조회 |
| GET | `/api/users/{userId}/profile` | 공개 프로필 조회 |
| DELETE | `/api/users/me` | 회원 탈퇴 |
| GET | `/api/mypage` | 마이페이지 요약 조회 |
| GET | `/api/mypage/posts` | 내가 작성한 게시글 조회 |
| GET | `/api/mypage/comments` | 내가 작성한 댓글 조회 |
| GET | `/api/mypage/likes` | 좋아요한 게시글 조회 |
| GET | `/api/mypage/bookmarks` | 북마크한 게시글 조회 |
| PATCH | `/api/mypage` | 마이페이지 정보 수정 |

### 상호작용 / 알림 / 검색

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/api/posts/{postId}/likes` | 게시글 좋아요 |
| DELETE | `/api/posts/{postId}/likes` | 게시글 좋아요 취소 |
| GET | `/api/users/me/likes` | 내가 좋아요한 게시글 조회 |
| POST | `/api/posts/{postId}/bookmarks` | 게시글 북마크 |
| DELETE | `/api/posts/{postId}/bookmarks` | 게시글 북마크 취소 |
| GET | `/api/notifications` | 알림 목록 조회 |
| PATCH | `/api/notifications/{notificationId}/read` | 알림 읽음 처리 |
| POST | `/api/search-logs` | 검색 로그 저장 |
| GET | `/api/users/me/search-logs` | 내 검색 로그 조회 |
| GET | `/api/search-logs/popular` | 인기 검색어 조회 |

### 신고 / 관리자

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/api/report/post` | 게시글 신고 |
| POST | `/api/report/comment` | 댓글 신고 |
| GET | `/api/admin/reports/raw` | 관리자 신고 원본 조회 |
| GET | `/api/admin/reports/groups` | 관리자 신고 그룹 조회 |
| POST | `/api/admin/reports/groups/approve` | 신고 그룹 승인 |
| POST | `/api/admin/reports/groups/reject` | 신고 그룹 반려 |
| GET | `/api/admin/members` | 관리자 회원 목록 조회 |
| GET | `/api/admin/members/{userId}` | 관리자 회원 상세 조회 |
| PATCH | `/api/admin/members/{userId}/status` | 회원 상태 변경 |
| GET | `/api/admin/dashboard` | 관리자 대시보드 조회 |

### AI 토론 관리

| Method | URL | 설명 |
| --- | --- | --- |
| POST | `/api/admin/ai-discussions/pending` | AI 토론 주제 생성 |
| GET | `/api/admin/ai-discussions/pending` | 승인 대기 AI 토론 조회 |
| GET | `/api/admin/ai-discussions` | AI 토론 목록 조회 |
| GET | `/api/admin/ai-discussions/{aiDiscussionPostId}` | AI 토론 상세 조회 |
| PATCH | `/api/admin/ai-discussions/{aiDiscussionPostId}/approve` | AI 토론 승인 |
| PATCH | `/api/admin/ai-discussions/{aiDiscussionPostId}/reject` | AI 토론 거절 |

## Docker 배포

### 환경변수 파일 생성

```bash
cp .env.example .env
```

배포 환경에서는 `.env`에 MySQL, Spring profile, Datasource, JWT, OAuth, Frontend/Backend URL 값을 설정합니다.

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/devc?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false
FRONTEND_BASE_URL=http://43.200.4.180.nip.io
BACKEND_BASE_URL=http://43.200.4.180.nip.io:8080
NEXT_PUBLIC_API_BASE_URL=http://43.200.4.180.nip.io:8080
```

### 전체 실행

```bash
docker compose up -d --build
docker compose ps
```

### 로그 확인

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

### 컨테이너 구성

| 서비스 | 설명 | 포트 |
| --- | --- | --- |
| `devc-mysql` | MySQL 8.4 데이터베이스 | `3306:3306` |
| `devc-backend` | Spring Boot REST API | `8080:8080` |
| `devc-frontend` | Next.js Frontend | `3000:3000` |

## 로컬 실행

### Backend

```bash
cd back/DevC
./gradlew bootRun
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## 테스트

```bash
cd back/DevC
./gradlew test
```

GitHub Actions는 `main`, `dev` 브랜치의 `push` 및 `pull_request` 시 백엔드 테스트를 실행합니다.

## Kotlin 마이그레이션

본 프로젝트는 Java/Spring Boot 기반 REST API를 Kotlin 기반으로 전환했습니다.

- DTO, Enum, Controller, Repository, Entity, Service, Security, Global 응답/예외 계층을 단계적으로 전환
- Kotlin `data class`, `val`, nullable type, Elvis operator, property 접근 방식 적용
- JPA Entity는 `data class`를 사용하지 않고 `kotlin("plugin.jpa")`, `allOpen` 설정 기반으로 처리
- 기존 API path, HTTP method, 응답 구조, 예외 응답 형식 유지
- Docker 배포 환경에서는 `prod` profile과 `.env` 기반 환경변수 주입 방식 적용

## GitHub Flow 전략

본 프로젝트는 `main` 브랜치를 기준으로 기능 단위 브랜치를 생성하고 Pull Request를 통해 코드 리뷰 후 병합하는 GitHub Flow 방식으로 관리했습니다.

| 브랜치 | 설명 |
| --- | --- |
| `main` | 배포 가능한 안정 버전 브랜치 |
| `dev` | 통합 개발 브랜치 |
| `feat/*` | 신규 기능 개발 |
| `fix/*` | 버그 수정 |
| `refactor/*` | 리팩토링 |
| `docs/*` | 문서 수정 |
| `chore/*` | 설정 및 기타 작업 |
| `migration/*` | Kotlin 마이그레이션 작업 |

### 작업 프로세스

1. 최신 브랜치 기준으로 작업 브랜치 생성
2. 기능 개발 및 테스트
3. Pull Request 생성
4. CI 테스트 확인
5. 팀원 리뷰 반영
6. 승인 후 병합
