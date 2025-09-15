# MSA E-Commerce Product Service Implementation Plan

## 프로젝트 구조
```
src/
├── products/
│   ├── domain/
│   │   ├── product.entity.ts
│   │   └── product.repository.interface.ts
│   ├── application/
│   │   ├── product.service.ts
│   │   └── product.service.spec.ts
│   ├── infrastructure/
│   │   └── persistence/
│   │       └── in-memory-product.repository.ts
│   ├── presentation/
│   │   ├── dto/
│   │   │   ├── create-product.dto.ts
│   │   │   └── update-product.dto.ts
│   │   └── controllers/
│   │       ├── product.controller.ts
│   │       └── product.controller.spec.ts
│   └── product.module.ts
├── app.module.ts
└── main.ts
```

## 구현 작업 목록

### 1. Domain Layer (도메인 계층)
- [x] Product Entity 구현
  - 불변 객체로 설계
  - 비즈니스 규칙 검증 로직 포함
  - 재고 관리 메서드 구현
- [x] Product Repository Interface 정의
  - CRUD 작업을 위한 인터페이스
  - SKU 중복 체크 메서드 포함

### 2. Application Layer (애플리케이션 계층)
- [x] Product Service 구현
  - 비즈니스 로직 처리
  - 트랜잭션 관리
  - 예외 처리 (NotFoundException, BadRequestException)
- [x] Service 단위 테스트 작성
  - 모든 메서드에 대한 테스트
  - 예외 상황 테스트 포함

### 3. Infrastructure Layer (인프라 계층)
- [x] In-Memory Repository 구현
  - Map을 사용한 메모리 저장소
  - Repository Interface 구현
  - 데이터 영속성 시뮬레이션

### 4. Presentation Layer (프레젠테이션 계층)
- [x] Product DTOs 구현
  - CreateProductDto: 상품 생성용 DTO
  - UpdateProductDto: 상품 수정용 DTO
  - class-validator를 사용한 유효성 검증
- [x] Product Controller 구현
  - RESTful API 엔드포인트
  - 요청/응답 처리
  - 유효성 검증 파이프 적용
- [x] Controller 단위 테스트 작성
  - 모든 엔드포인트 테스트
  - 예외 처리 테스트

### 5. Module Configuration (모듈 설정)
- [x] Product Module 구현
  - 의존성 주입 설정
  - Provider 등록
- [x] App Module 구현
  - Product Module 임포트
- [x] Main Application 설정
  - NestJS 애플리케이션 부트스트랩
  - 전역 파이프 설정
  - CORS 설정

## 10. REST API Controller

### 10.1 Product API ✅ **완료**

#### 구현된 엔드포인트:

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/products | 새 상품 생성 | ✅ |
| GET | /api/products | 모든 상품 조회 | ✅ |
| GET | /api/products?category={category} | 카테고리별 상품 조회 | ✅ |
| GET | /api/products/:id | ID로 상품 조회 | ✅ |
| GET | /api/products/sku/:sku | SKU로 상품 조회 | ✅ |
| PATCH | /api/products/:id | 상품 정보 수정 | ✅ |
| DELETE | /api/products/:id | 상품 삭제 | ✅ |

#### 구현 세부사항:

1. **Product Entity (Domain)**
   - 불변 객체 패턴 적용
   - 비즈니스 규칙 검증 (가격, 재고, 이름, SKU)
   - 재고 관리 메서드 (증가/감소)
   - 구매 가능 여부 확인 메서드

2. **Product Service (Application)**
   - CRUD 작업 구현
   - SKU 중복 검사
   - 예외 처리 (NotFoundException, BadRequestException)
   - 도메인 로직과 인프라 계층 분리

3. **Product Controller (Presentation)**
   - RESTful API 설계 원칙 준수
   - ValidationPipe를 통한 입력 검증
   - 적절한 HTTP 상태 코드 반환
   - DTO를 통한 데이터 전송

4. **DTOs**
   - CreateProductDto: 상품 생성 시 필요한 필드
   - UpdateProductDto: 부분 업데이트를 위한 선택적 필드
   - class-validator 데코레이터를 통한 유효성 검증

5. **Repository**
   - IProductRepository 인터페이스 정의
   - InMemoryProductRepository 구현 (테스트/개발용)
   - 의존성 역전 원칙 적용

6. **테스트**
   - Controller 단위 테스트 작성
   - Service 단위 테스트 작성
   - 모든 성공/실패 케이스 커버
   - Mock 객체를 사용한 격리된 테스트

## 기술 스택
- NestJS Framework
- TypeScript
- class-validator & class-transformer
- Jest (테스트 프레임워크)
- SOLID 원칙 적용
- Clean Architecture 패턴
- TDD 방법론

## 다음 단계 제안
1. Database 통합 (TypeORM 또는 Prisma)
2. Swagger/OpenAPI 문서화
3. 인증/인가 미들웨어 추가
4. 로깅 시스템 구현
5. 이벤트 기반 통신 (Message Queue)
6. Docker 컨테이너화
7. CI/CD 파이프라인 설정