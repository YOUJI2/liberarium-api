## 📚 Liberarium API (도서 검색 서비스)

Liberarium API는 **도서 검색 및 상세 조회 서비스**를 제공하는 백엔드 애플리케이션입니다.  
Elasticsearch 기반의 전문 검색 기능을 활용하여 빠르고 정확한 검색 품질을 보장하며, 
Redis를 활용한 캐싱과 분산락(Distributed Lock)을 통해 멀티 인스턴스 환경에서도 안정적인 동시성 제어를 보장합니다.
또한, **인기 검색어 집계**, **도서 상세 정보 조회**, **ES 인덱스 자동화** 등 운영 환경에서 필수적인 기능들을 포함하고 있으며,
 로그 수집(Filebeat → Elasticsearch → Kibana) 기반으로 시스템 운영/모니터링도 추가하였습니다.

### 📎 기술 스택
- JDK 17
- Spring Boot 3.x
- DB: MySQL
- JPA (Hibernate)
- Redis (Redisson ZSET 기반 캐싱 & 분산락)
- Elasticsearch / Kibana
- Gradle
- Docker Compose
- Swagger (Springdoc OpenAPI)

### 📌 구현 범위

**1. 고객용 API**
- 도서 검색 API (단일 / 복합)
- 도서 상세 조회 API
- 인기 검색 키워드 조회 API

**2. 시스템 관리**
- ES 인덱스 자동 생성 및 보호
- Seed 데이터 자동 로딩
- 도서 상세 조회시 캐싱 적용
- 캐시 스탬피드시 분산락 기반 동시성 제어 적용

### 🗂️ 패키지 구조

``` plain
com.liberarium.api
 ├── LiberariumApiApplication.java   # Spring Boot 메인 클래스
 │
 ├── common                          # 공통 모듈
 │   ├── aop                         # AOP 관련 (분산락 등)
 │   ├── cache                       # Redisson/Cache 유틸
 │   ├── exception                   # 예외 처리 및 ErrorCode 정의
 │   └── model                       # 공통 응답/에러 객체
 │
 ├── config                          # 설정 관련
 │   ├── ElasticsearchConfig.java
 │   ├── RedisConfig.java
 │   └── BooksIndexInitializer.java  # ES 인덱스 초기화
 │
 ├── domain                          # 도메인 계층
 │   ├── entity                      # JPA 엔티티
 │   │   ├── Book.java
 │   │   ├── Catalog.java
 │   │   └── ...
 │   ├── repository                  # JPA Repository
 │   └── service                     # 서비스 계층
 │       ├── BookService.java
 │       └── BookServiceImpl.java
 │
 ├── search                          # 검색 관련 기능
 │   ├── controller                  # API Controller
 │   │   └── BookSearchController.java
 │   ├── service                     # 검색 서비스
 │   │   └── BookSearchService.java
 │   └── support                     # ES 관련 유틸
 │       └── EsAwaiterUtil.java
 │
 ├── logging                         # 로깅 및 Correlation ID 처리
 │   └── CorrelationFilter.java
 │
 └── support                         # 테스트/유틸 관련
     └── testcontainers              

```

</br>

### 🌿 브랜치 전략
본 프로젝트는 **Feature → Develop → Main** 흐름의 간소화 Git Flow 전략을 사용합니다.

- **main** : 메인 브랜치  
- **develop** : 개발 통합 브랜치  
- **feature/LB-{이슈번호}** : 기능 개발 브랜치  
<img width="400" height="300" alt="image" src="https://github.com/user-attachments/assets/c4f2063d-5b30-4c08-8d9f-22319c735d73" />

### 📌 commit (TYPE)
- **FEAT**: 새로운 기능 추가
- **FIX**: 버그 수정
- **CHORE**: 빌드/환경설정 등 기타 작업
- **REFACTOR**: 코드 리팩토링
- **DOCS**: 문서 작성/수정

</br>

### 💻 설계
 **1. 아키텍처 설계**

<img width="1751" height="593" alt="image" src="https://github.com/user-attachments/assets/60bdb3b8-8328-4603-b027-6f807a0b3665" />
 

 **2. 테이블 구조**
- [book]과 [catalog]를 기준으로, [book]은 [book_detail]과 1:1 관계를 가지고 다수의 [catalog]를 가질 수 있도록 구성
    ```
    [book] 1 --- * [book_catalog] * --- 1 [catalog]
      1 |
    [book_detail]
    ```
  - 검색에 활용되는 도서의 경우 상세정보를 제외한 기본적인 데이터를 기반으로 하기 때문에 book과 book_detail로 구분하여 필요한 부분만 로드될 수 있도록 나눴습니다.
  - 카탈로그는 책 마다 가지고 있는 시스템적 구분으로 '어느 분야'의 도서인지를 구분하기 위해 구성하였습니다. 도서마다 여러개의 카탈로그를 가질 수 있습니다.
    - 추 후 카탈로그로 구분하여 도서를 검색할 수 있는 필터링 기능을 생각하며 위 구조를 생각했습니다.
- 애플리케이션 시작시 01_schema.sql, 02_seed.sql 호출
- sql 스크립트로 테이블, 인덱스 형성 및 초기 데이터 로드

**3. Redis Cache**
- 도서 상세 조회 시 존재하지 않는 ISBN 요청에 대해 DB 호출을 일시 차단하는 방어 로직 구현  
- 존재하는 도서의 경우 ISBN 기준으로 Redis에 저장하여 조회 성능 개선  

**4. 분산락 AOP (DistributedLockAop)**
- 멀티 인스턴스 환경에서 캐시 스탬피드 방지 및 데이터 정합성 보장  
- Redisson 기반 `RLock` 사용  
- `@DistributedLock` 어노테이션으로 손쉽게 적용 가능  

**5. Elastic Search**
  - 기존 RDB 기반 LIKE 검색은 성능/검색 품질에 한계가 있어 ES선택
  - 검색 품질 → `title`, `author` 필드만 검색, 최신 등록 -> 출판일 순으로 정렬
  - 검색 전략
    - 단일 키워드 검색
    - 복합 키워드 (OR, NOT 조건) 지원
  - **인덱스 자동화**
    - `BooksIndexInitializer`로 애플리케이션 기동 시 인덱스 자동 생성
    - `BookSearchSeeder`로 seed 데이터 자동 적재 → 개발/테스트 환경에서 바로 검색 가능

**6. 애플리케이션 로그(Logback) → JSON 포맷 출력**
  - Logback → JSON 포맷 출력
  - Filebeat로 로그 수집 → Elasticsearch 전송
  - Kibana 대시보드에서
    - 검색 API 호출 현황
    - 에러 로그

**7. 예외 처리 모듈화**
  - `BusinessException` 기반으로 발생 가능한 예외를 `ErrorCode` Enum으로 정의
  - 클라이언트에는 **커스텀 에러 코드 + 메시지** 전달
  - 상세 내용은 내부 문서화 (보안 고려)

**8. 응답 객체 모듈화**
  - 별도의 `ResponseObject`를 활용해 에러와 반환 데이터를 공통 구조로 처리


</br>

### 🛠️ 빌드 및 실행

**Docker Compose 실행**
```bash
# 인프라 실행
docker compose --profile infra up -d

# 전체 실행
docker compose --profile app up -d
```
</br>

### 💡 Swagger 문서화 & 테스트코드 실행 방법

> 프로젝트는 **springdoc-openapi**를 사용하여 자동으로 Swagger UI를 제공


**애플리케이션 실행 후 확인**
- Swagger UI : [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)  

</br>

**테스트 실행 (환경변수 설정 필수, M3기준 Colima로 docker 실행, )**
> `docker compose --profile app up`으로 먼저 모든 application 실행 후 가능

```bash
  DOCKER_HOST=unix:///Users/{사용자폴더}/.colima/default/docker.sock \
  TESTCONTAINERS_CHECKS_DISABLE=true \
  TESTCONTAINERS_RYUK_DISABLED=true \
  ./gradlew test
```

</br>

### 🧐 고민했던 부분

1. **분산락 AOP**
   - **문제**: 캐시 미스가 동시에 발생하면 다수의 요청이 동시에 DB로 떨어져 부하 발생 (Cache Stampede 문제)
   - **대안**
     1. 애플리케이션 단일 인스턴스 환경에서는 `synchronized`로 처리가 가능
     2.  하지만, 멀티 인스턴스 환경을 고려하여 분산락을 추가적으로 개발 
   - **결정**: `Redisson` 기반 `RLock`을 AOP로 추상화하여 `@DistributedLock` 어노테이션만 붙이면 쉽게 적용 가능  
   - **효과**: 다중 서버 환경에서도 데이터 정합성과 캐시 일관성 보장


2. **Elasticsearch 연결 안정성**
   - **문제**: Docker / Testcontainers 환경에서 ES가 기동되기 전에 Spring Boot가 먼저 뜨면서 `Connection refused` 오류 발생
   - **고민**
     - (1) 애플리케이션에서 단순 retry 로직 구현
     - (2) 기동 스크립트(docker-compose depends_on)로 제어 클러스터 상채 점검
   - **결정**: `EsAwaiterUtil`을 구현하여 클러스터 상태가 `yellow/green` 이 될 때까지 최대 60초간 대기 → 애플리케이션이 안정적으로 실행되도록 보장

3. **검색 품질**
   - **문제**: 단순 Full-text 검색 시 불필요한 매칭 결과 과다 발생 (예: 설명, 출판사명 등 불필요한 필드에서 히트)
   - **대안**
     - (1) 모든 필드 검색 (정확도 ↓, recall ↑)
     - (2) 특정 주요 필드 검색 (정확도 ↑, recall ↓)
   - **결정**: `title`, `author` 필드만 검색 대상으로 제한 → 실제 사용자가 기대하는 검색 품질 확보  
   - **추가**: 정렬 정책을 `최근 등록 → 출판일` 순으로 설정하여 최근 등록한 도서를 우선적으로 검색

4. **Redis ZSET 인기 검색어**
   - **문제**: 인기 검색어 집계를 RDB에서 처리하면 `COUNT + GROUP BY` 성능 저하
   - **대안**
     1. RDB 통계 테이블 구축하여 저장
     2.  Redis Sorted Set(ZSET)으로 실시간 집계
   - **결정**: Redis ZSET을 활용 → 검색어 입력 시 score 점수를 증가 시킨다
   - **효과**: O(logN) 성능으로 실시간 순위 집계 가능, API 호출 시 Top N 조회 성능 개선

5. **예외 처리**
   - **문제**: 각 서비스마다 예외 처리 로직이 달라지면 클라이언트 응답 포맷 불일치 및 유지보수 어려움
   - **대안**
     1. Controller마다 개별 예외 처리를 하기엔 부담이 생김
     2. 공통 `BusinessException` + `ErrorCode` Enum 정의
   - **결정**: `BusinessException` 상속 구조로 표준화, 에러코드/메시지를 Enum(`ErrorCode`)으로 관리  
   - **효과**
     - 클라이언트에는 **에러 코드 + 간단 메시지** 전달
       - 서버 내부 로그에는 상세 스택/메시지 기록 → 보안/운영 분리


### 📑 API 명세

#### 1. 도서 상세 조회 API
| Method | Endpoint | 요청 파라미터 | 응답 필드 | 설명 |
|--------|----------|--------------|-----------|------|
| GET | `/api/books/{isbn}` | Path Variable: `isbn` | `isbn`, `title`, `subtitle`, `author`, `catalogName`, `imageUrl`, `publishedDate`, `publisher`, `soldOut`, `freeShipping`, `listPrice`, `salePrice`, `authorBio`, `bookDescription` | 특정 도서의 상세 정보를 조회 |

**요청 예시**
```http
GET /api/books/9791125961376
```
```json
{
  "data": {
    "isbn": "9791125961376",
    "title": "수능기초 10일 격파 사탐영역 한국사 (2021년) - 수능 final 기초 course",
    "subtitle": "수능기초10일격파사탐영역한국사2021년수능final기초course",
    "author": "천재교육 편집부 (엮은이)",
    "catalogName": "소설",
    "imageUrl": "https://image.aladin.co.kr/product/27108/95/cover/k592731149_1.jpg",
    "publishedDate": "2021-05-07",
    "publisher": "천재교육",
    "soldOut": false,
    "freeShipping": false,
    "listPrice": 14000,
    "salePrice": 14000,
    "authorBio": "천재교육 편집부 (엮은이)에 대한 간단 소개입니다.",
    "bookDescription": "수능 문제의 감각을 기를 수 있는 수능 입문 교재로 수능에 반드시 출제될 문제를 예상하여 핵심 개념과 기출 문제, 유사 문제로 구성하였다. 또 수능에 출제되었던 빈출 자료 44개를 정리한 수능 기초 체크 44선을 권두 부록으로 제공한다."
  }
}

```

#### 2. 도서 단일 키워드 검색 API
| Method | Endpoint | 요청 파라미터 | 응답 필드 | 설명 |
|--------|----------|--------------|-----------|------|
| GET | `/api/books/search` | `title`, `page`, `size` | `searchQuery`, `pageInfo`, `books[]`, `metadata` | 단일 키워드 기반 도서 검색 API |

**요청 예시**
```http
GET /api/books?keyword=영어&page=1&size=10
```

**응답 예시**
```json
{
  "data": {
    "searchQuery": "영어",
    "pageInfo": {
      "currentPage": 1,
      "pageSize": 20,
      "totalPages": 3,
      "totalElements": 50
    },
    "books": [
      {
        "id": "9791195969494",
        "title": "직독직해로 영어읽기 완성하기(기본)(8)",
        "subtitle": "직독직해로영어읽기완성하기기본8",
        "image": "https://bookthumb-phinf.pstatic.net/cover/115/157/11515711.jpg?type=m1&udate=20170802",
        "author": "이상익",
        "isbn": "9791195969494",
        "published": "2017-01-01"
      },
     // ... 생략 (총 20권 반환)
    ],
    "metadata": {
      "executionTime": 69,
      "strategy": "SINGLE"
    }
  }
}

```


#### 3. 도서 복합 키워드 검색 API (NOT 연산)
| Method | Endpoint | 요청 파라미터 | 응답 필드 | 설명 |
|--------|----------|--------------|-----------|------|
| GET | `/api/books/search/books` | `q`, `page`, `size` | `searchQuery`, `pageInfo`, `books[]`, `metadata` | 복합 키워드 NOT 조건으로 검색 (예: `수능 NOT 영어`)  |

**요청 예시**
```http
GET /api/books/search/books?q=수능-영어&page=1&size=10
```

```json
{
  "data": {
    "searchQuery": "수능-영어",
    "pageInfo": {
      "currentPage": 1,
      "pageSize": 20,
      "totalPages": 1,
      "totalElements": 6
    },
    "books": [
      {
        "id": "9791125961376",
        "title": "수능기초 10일 격파 사탐영역 한국사 (2021년) - 수능 final 기초 course",
        "subtitle": "수능기초10일격파사탐영역한국사2021년수능final기초course",
        "image": "https://image.aladin.co.kr/product/27108/95/cover/k592731149_1.jpg",
        "author": "천재교육 편집부 (엮은이)",
        "isbn": "9791125961376",
        "published": "2021-05-07"
      },
     // ... 생략 (총 6권 반환)
    ],
    "metadata": {
      "executionTime": 275,
      "strategy": "NOT_OPERATION"
    }
  }
}

```


#### 4. 도서 복합 키워드 검색 API (OR 연산)
| Method | Endpoint | 요청 파라미터 | 응답 필드 | 설명 |
|--------|----------|--------------|-----------|------|
| GET | `/api/books/search/books` | `q`, `page`, `size` | `searchQuery`, `pageInfo`, `books[]`, `metadata` | 복수 키워드를 OR 조건으로 검색 (예: `수능 OR 영어`) |

**요청 예시**
```http
GET /api/books/search/books?q=수능|영어&page=1&size=10
```
```json
{
  "data": {
    "searchQuery": "수능|영어",
    "pageInfo": {
      "currentPage": 1,
      "pageSize": 20,
      "totalPages": 3,
      "totalElements": 56
    },
    "books": [
      {
        "id": "9791195969494",
        "title": "직독직해로 영어읽기 완성하기(기본)(8)",
        "subtitle": "직독직해로영어읽기완성하기기본8",
        "image": "https://bookthumb-phinf.pstatic.net/cover/115/157/11515711.jpg?type=m1&udate=20170802",
        "author": "이상익",
        "isbn": "9791195969494",
        "published": "2017-01-01"
      },
      // ... 생략 (총 56권 중 일부)
    ],
    "metadata": {
      "executionTime": 181,
      "strategy": "OR_OPERATION"
    }
  }
}
```


#### 5. 인기 검색 키워드 조회 API
| Method | Endpoint | 요청 파라미터 | 응답 필드 | 설명 |
|--------|----------|--------------|-----------|------|
| GET | `/api/search/popular` | `size` (조회할 키워드 개수, 기본값 10) | `keyword`, `count` | Redis ZSET 기반으로 인기 검색어 순위를 조회 |

**요청 예시**
```http
GET /api/search/popular?size=10
```

```json
{
  "data": [
    {
      "keyword": "영어",
      "count": 9
    },
    {
      "keyword": "수능",
      "count": 4
    },
    {
      "keyword": "윤공",
      "count": 2
    }
  ]
}

```




