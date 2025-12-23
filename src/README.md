# KnowlearnMAP Backend

확장 가능한 엔터프라이즈급 백엔드 시스템

## 기술 스택

- **Java 17**
- **Spring Boot 3.2.1**
- **PostgreSQL** (knowlearn_map)
- **JPA** + **MyBatis** 혼용
- **HikariCP** 커넥션 풀
- **Maven**

## 빌드 및 실행

### 1. 데이터베이스 설정

`src/main/resources/application.yml` 파일에서 데이터베이스 설정 확인:

```yaml
spring:
  profiles:
    active: local  # 또는 dev
```

- **local** 프로파일: `localhost:5432/knowlearn_map`
- **dev** 프로파일: `172.30.1.57:15433/knowlearn_map`

### 2. 빌드

```bash
cd D:\Projects\knowlearnMAP\src
mvn clean install
```

### 3. 실행

```bash
mvn spring-boot:run
```

또는 프로파일 지정:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 서버 확인

- 서버 주소: `http://localhost:8080`
- Health Check: `http://localhost:8080/actuator/health`

## API 엔드포인트

### Workspace Management

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/workspaces` | 워크스페이스 목록 조회 |
| GET | `/api/workspaces/{id}` | 워크스페이스 단건 조회 |
| POST | `/api/workspaces` | 워크스페이스 생성 |
| PUT | `/api/workspaces/{id}` | 워크스페이스 수정 |
| DELETE | `/api/workspaces/{id}` | 워크스페이스 삭제 |

### 요청 예시

**POST /api/workspaces**
```json
{
  "name": "My Workspace",
  "description": "테스트 워크스페이스",
  "icon": "📄",
  "color": "blue"
}
```

**응답 예시**
```json
{
  "success": true,
  "message": "워크스페이스 생성 성공",
  "data": {
    "id": 1,
    "title": "My Workspace",
    "name": "My Workspace",
    "description": "테스트 워크스페이스",
    "icon": "📄",
    "color": "blue",
    "source": "소스 0개",
    "date": "2025. 12. 18.",
    "role": "Owner"
  }
}
```

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/knowlearnmap/
│   │   ├── KnowlearnMapApplication.java
│   │   ├── common/
│   │   │   ├── annotation/
│   │   │   ├── config/
│   │   │   └── dto/
│   │   └── workspace/
│   │       ├── domain/
│   │       ├── dto/
│   │       ├── repository/
│   │       ├── mapper/
│   │       ├── service/
│   │       └── controller/
│   └── resources/
│       ├── application.yml
│       └── mybatis-mapper/
└── test/
```

## 향후 확장 계획

- [ ] Document 도메인 구현
- [ ] RAG 모듈
- [ ] LLM 연동
- [ ] Ontology 처리
- [ ] ArangoDB 연동
- [ ] 사용자 인증/권한
