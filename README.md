# BWLOVERS-BE

BWLOVERS-BE는 임신/건강 정보, 보험 정보, AI 추천/분석, OCR 기능을 제공하는 Spring Boot 기반 백엔드 프로젝트입니다.

이 README는 과제 제출 기준에 맞춰, 저장소를 `git clone` 한 뒤 필요한 환경을 준비하고 프로젝트를 다시 빌드, 실행, 테스트할 수 있도록 작성했습니다.

## 재현 범위

이 저장소는 아래 절차까지 재현하는 것을 목표로 합니다.

1. 저장소 clone
2. PostgreSQL / Redis 준비
3. 환경 변수 설정
4. 백엔드 빌드
5. 백엔드 실행
6. 자동 테스트 실행

주의 사항:

- 전체 기능을 100% 재현하려면 외부 서비스 계정이 필요합니다.
  - Naver OAuth
  - CLOVA OCR
  - OpenAI API
  - AWS S3
- 저장소에는 샘플 DB 덤프나 초기 데이터 삽입 스크립트가 포함되어 있지 않습니다.
- 따라서 데이터는 애플리케이션 실행 후 API 호출 또는 직접 SQL 입력으로 준비해야 합니다.

## 1. 프로젝트 설명

- 프로젝트명: BWLOVERS-BE
- 프레임워크: Spring Boot 3.5.3
- 언어: Java 17
- 빌드 도구: Gradle Wrapper
- 실행 엔트리포인트:
  - `bwlovers/src/main/java/com/capstone/bwlovers/BwloversApplication.java`

### 시스템 구성도
<img src="https://github.com/user-attachments/assets/114e752c-ccde-420e-9cf8-210f619ac16a" width="70%">

### 주요 기능

- Naver OAuth2 로그인 및 JWT 기반 인증
- 사용자 건강 정보 / 임신 정보 저장, 조회, 수정
- 보험 상품 조회, 메모 수정, 선택 상품 저장
- 시뮬레이션 저장 및 조회
- AI 추천 / AI 분석 결과 조회 및 콜백 처리
- OCR 이미지 업로드, OCR 결과 요약

## 2. Source code 설명

실제 Spring Boot 애플리케이션 코드는 저장소 루트가 아니라 `bwlovers/` 디렉터리 아래에 있습니다.

```text
BWLOVERS-BE/
├── README.md
└── bwlovers/
    ├── build.gradle
    ├── settings.gradle
    ├── gradlew
    ├── Dockerfile
    ├── docker-compose.yml
    └── src/
        ├── main/
        │   ├── java/com/capstone/bwlovers/
        │   └── resources/application.yml
        └── test/
            ├── java/com/capstone/bwlovers/
            └── resources/application-test.yml
```

### 주요 패키지 구성

| 패키지 | 설명 |
| --- | --- |
| `auth` | Naver 로그인, JWT 토큰 발급/재발급, 사용자 정보 관리 |
| `health` | 건강 상태 저장/조회/수정 |
| `pregnancy` | 임신 정보 저장/조회/수정 |
| `insurance` | 보험 상품 조회, 선택 상품 저장, 메모 수정 |
| `simulation` | 시뮬레이션 저장/조회/삭제 |
| `ai/analysis` | AI 분석 요청, 결과 조회, 콜백 처리 |
| `ai/recommendation` | AI 추천 요청, 결과 조회, 상세 조회, 콜백 처리 |
| `ai/ocr` | OCR 업로드, 상태 조회, OCR 후처리/요약 |
| `global` | 보안, 예외 처리, 공통 설정, S3, 유틸리티 |

### 대표 API

- 인증
  - `GET /auth/redirect/naver`
  - `POST /auth/login`
  - `POST /auth/refresh`
- 사용자
  - `GET /users/me`
  - `PATCH /users/me/profile-image`
  - `PATCH /users/me/username`
- 건강 / 임신
  - `POST|GET|PATCH /users/me/health-status`
  - `POST|GET|PATCH /users/me/pregnancy-info`
- 보험 / 시뮬레이션
  - `GET /insurances`
  - `GET /insurances/{insuranceId}`
  - `POST /insurances/selected`
  - `GET /simulations`
  - `POST /simulations/save`
- AI / OCR
  - `POST /ai/recommend`
  - `POST /ai/simulation`
  - `POST /ocr/jobs`
  - `GET /ocr/jobs/{jobId}`

기본적으로 `/auth/**`, `/oauth2/**`, `/ai/callback/**` 를 제외한 대부분의 API는 JWT 인증이 필요합니다.

## 3. How to build

### 3-1. 사전 요구 사항

- Git
- JDK 17
- 인터넷 연결
  - Gradle 의존성 다운로드 필요

Gradle은 별도 설치가 필요하지 않습니다. 저장소에 `gradlew` 가 포함되어 있습니다.

### 3-2. 빌드 명령

```bash
git clone <repository-url>
cd BWLOVERS-BE/bwlovers
chmod +x gradlew
./gradlew clean build
```

빌드가 성공하면 JAR 파일이 아래 경로에 생성됩니다.

```text
bwlovers/build/libs/
```

## 4. How to install

이 프로젝트는 별도의 설치 마법사가 있는 형태가 아니라, 의존 서비스와 환경 변수를 준비한 뒤 애플리케이션을 실행하는 방식입니다.

### 4-1. PostgreSQL 준비

로컬에 PostgreSQL이 없다면 Docker로 준비할 수 있습니다.

```bash
docker run -d \
  --name bwlovers-postgres \
  -e POSTGRES_DB=bwlovers \
  -e POSTGRES_USER=bwlovers \
  -e POSTGRES_PASSWORD=bwlovers \
  -p 5432:5432 \
  postgres:16
```

### 4-2. Redis 준비

로컬에 Redis가 없다면 Docker로 준비할 수 있습니다.

```bash
docker run -d \
  --name bwlovers-redis \
  -p 6379:6379 \
  redis:7-alpine
```

### 4-3. 환경 변수 설정

애플리케이션은 `bwlovers/src/main/resources/application.yml` 에 정의된 아래 환경 변수를 사용합니다.

| 환경 변수 | 필수 여부 | 설명 |
| --- | --- | --- |
| `DB_URL` | 필수 | PostgreSQL JDBC URL |
| `DB_USERNAME` | 필수 | PostgreSQL 사용자명 |
| `DB_PASSWORD` | 필수 | PostgreSQL 비밀번호 |
| `JWT_SECRET` | 필수 | JWT 서명 비밀키 |
| `CLIENT_ID` | 필수 | Naver OAuth Client ID |
| `CLIENT_SECRET` | 필수 | Naver OAuth Client Secret |
| `CLOVA_OCR_INVOKE_URL` | 필수 | CLOVA OCR Invoke URL |
| `CLOVA_OCR_SECRET` | 필수 | CLOVA OCR Secret |
| `OCR_TEMP_BUCKET` | 필수 | OCR 임시 파일 업로드용 S3 버킷 이름 |
| `OPENAI_API_KEY` | 필수 | OpenAI API Key |
| `REDIS_HOST` | 선택 | 기본값 `localhost` |
| `REDIS_PORT` | 선택 | 기본값 `6379` |
| `REDIS_PASSWORD` | 선택 | 기본값 빈 문자열 |
| `AWS_REGION` | 선택 | 기본값 `ap-northeast-2` |

S3 기능까지 사용하려면 AWS 기본 자격 증명도 필요합니다.

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- 또는 IAM Role

아래 스크립트 예시는 로컬 개발용 환경 변수 설정 예시입니다.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/bwlovers
export DB_USERNAME=bwlovers
export DB_PASSWORD=bwlovers
export JWT_SECRET=change-this-to-a-long-random-secret

export CLIENT_ID=dummy-client-id
export CLIENT_SECRET=dummy-client-secret
export CLOVA_OCR_INVOKE_URL=http://localhost:9000/mock-clova
export CLOVA_OCR_SECRET=dummy-clova-secret
export OCR_TEMP_BUCKET=dummy-bucket
export OPENAI_API_KEY=dummy-openai-key

export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export AWS_REGION=ap-northeast-2
```

참고:

- 앱이 단순 부팅되는지만 확인하려면 외부 API 키 자리에 더미 문자열을 넣어도 됩니다.
- 하지만 Naver 로그인, OCR, OpenAI 요약, S3 업로드, AI 추천/분석 기능은 실제 키가 없으면 정상 동작하지 않습니다.
- `ai.server.url` 은 현재 `application.yml` 에 고정값으로 들어 있으므로, 자체 AI 서버를 사용하려면 해당 파일을 직접 수정해야 합니다.

### 4-4. 애플리케이션 실행

Gradle로 실행:

```bash
cd bwlovers
./gradlew bootRun
```

빌드된 JAR로 실행:

```bash
cd bwlovers
java -jar build/libs/*.jar
```

기본 실행 포트는 `8080` 입니다.

### 4-5. 설치 후 간단 확인

아래 API는 인증 없이 호출 가능하므로 서버 기동 확인용으로 사용할 수 있습니다.

```bash
curl http://localhost:8080/auth/redirect/naver
```

## 5. How to test

### 5-1. 자동 테스트

```bash
cd bwlovers
./gradlew test
```

테스트 관련 설명:

- 테스트 설정 파일: `bwlovers/src/test/resources/application-test.yml`
- 테스트 DB: H2 인메모리 DB
- 테스트 프레임워크: JUnit 5, Spring Boot Test

2026년 6월 19일 기준으로 `./gradlew test` 실행이 성공했습니다.

### 5-2. 수동 테스트

수동 테스트는 아래 순서로 진행할 수 있습니다.

1. PostgreSQL / Redis 실행
2. 환경 변수 설정
3. `./gradlew bootRun` 으로 서버 실행
4. Postman 또는 curl로 API 호출

주의:

- 대부분의 API는 JWT 인증이 필요합니다.
- 로그인 흐름을 실제로 검증하려면 Naver OAuth 애플리케이션 설정이 필요합니다.

## 6. Description of sample data

### Database

- 운영 DB: PostgreSQL
- 캐시 / OCR 작업 상태 저장: Redis
- 테스트 DB: H2

애플리케이션은 `spring.jpa.hibernate.ddl-auto=update` 로 설정되어 있어, 첫 실행 시 엔티티 기준으로 테이블이 생성 또는 갱신됩니다.

대표적으로 아래와 같은 데이터가 저장됩니다.

- 사용자 정보
- 건강 상태 정보
- 임신 정보
- 보험 상품 및 특약 정보
- 시뮬레이션 정보
- OCR 작업 상태

### Sample data

- PostgreSQL용 샘플 데이터 파일이 포함되어 있습니다.
  - `docs/sample-data.sql`
- 이 파일은 재실행 가능하도록 작성되어 있으며, 같은 샘플 데이터를 다시 넣을 수 있습니다.
- 자동 실행되지는 않으며, 애플리케이션이 테이블을 생성한 뒤 수동 import 해야 합니다.

### 외부 입력 데이터

- OCR 기능은 사용자가 업로드한 이미지 파일을 입력 데이터로 사용합니다.
- 업로드된 OCR 임시 파일은 S3 버킷에 저장되도록 구현되어 있습니다.

## 7. Description of used open source

이 프로젝트에서 사용하는 주요 오픈소스 및 라이브러리는 아래와 같습니다.

| 오픈소스 | 용도 |
| --- | --- |
| Spring Boot Starter Web | REST API 서버 |
| Spring Boot Starter WebFlux | 외부 API 호출용 WebClient |
| Spring Boot Starter Security | 인증/인가 |
| Spring Boot Starter OAuth2 Client | Naver OAuth2 로그인 |
| Spring Boot Starter Data JPA | ORM 및 DB 접근 |
| PostgreSQL JDBC Driver | PostgreSQL 연결 |
| Spring Boot Starter Validation | 요청값 검증 |
| Spring Boot Starter Data Redis | Redis 연동 |
| JJWT | JWT 생성 및 검증 |
| Jackson Databind | JSON 직렬화/역직렬화 |
| AWS SDK for Java S3 | S3 업로드/다운로드 |
| Lombok | 반복 코드 감소 |
| H2 Database | 테스트용 인메모리 DB |
| Spring Boot Starter Test | 통합 테스트 |
| Spring Security Test | 보안 테스트 |

## 8. 기타 참고 사항

- 저장소에 포함된 `bwlovers/docker-compose.yml` 은 로컬 전체 개발환경 구성용이라기보다 배포용 앱 컨테이너 실행에 가깝습니다.
- 현재 저장소에는 로컬 전체 환경을 한 번에 구성하는 통합 설치 스크립트는 없습니다.
- 따라서 과제 재현 시에는 README의 명령어 순서대로 직접 실행하면 됩니다.
- 샘플 데이터가 필요하면 `docs/sample-data.sql` 을 import 하면 됩니다.
