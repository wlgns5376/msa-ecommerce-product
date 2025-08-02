# MSA E-Commerce Product Service

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?style=flat-square&logo=spring)
![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20DDD-purple?style=flat-square)
![Build](https://img.shields.io/badge/Build-Gradle%208.5-blue?style=flat-square&logo=gradle)

## 📋 프로젝트 개요

이 프로젝트는 마이크로서비스 아키텍처 기반의 이커머스 플랫폼에서 **상품 관리를 담당하는 서비스**입니다. 헥사고날 아키텍처와 DDD(Domain-Driven Design)를 기반으로 하는 4계층 아키텍처로 설계되었습니다.

## 🏗 아키텍처

### 4계층 아키텍처

```
├── bootstrap/              # 애플리케이션 진입점 (API 서버)
│   └── product-api/        # 상품 REST API
├── core/                   # 도메인 핵심 로직
│   └── product-domain/     # 상품 도메인 모델 & 비즈니스 로직
├── infrastructure/         # 외부 의존성 구현
│   ├── product-persistence/ # 상품 데이터 영속성 계층
│   └── product-kafka/      # 상품 이벤트 메시징 계층
└── common/                 # 공통 모듈
```

### 헥사고날 아키텍처 특징

- **포트와 어댑터 패턴**: 도메인 로직과 외부 의존성 분리
- **의존성 역전**: 도메인이 인프라에 의존하지 않음
- **테스트 용이성**: 각 계층별 독립적인 테스트 가능
- **확장성**: 새로운 어댑터 추가로 쉬운 확장

## 🛠 기술 스택

- **언어**: Java 17
- **프레임워크**: Spring Boot 3.2.0, Spring Cloud 2023.0.0
- **빌드 도구**: Gradle 8.5
- **테스트**: JUnit 5, Mockito, AssertJ
- **보안**: Spring Security, JWT
- **데이터베이스**: H2 (개발), MariaDB (운영)
- **캐싱**: Redis
- **메시징**: Kafka
- **서비스 디스커버리**: Eureka
- **API 문서**: Swagger/OpenAPI 3.0

## 🚀 시작하기

### 1. 프로젝트 복사

```bash
# 프로젝트 클론
git clone https://github.com/wlgns5376/msa-ecommerce-product.git
cd msa-ecommerce-product
```

### 2. 설정 변경

```bash
# gradle.properties 설정 확인
# systemProp.file.encoding=UTF-8
# org.gradle.daemon=true
```

### 3. 인프라 실행

```bash
# Docker로 필요한 인프라 실행
cd docker
docker-compose up -d

# 실행 확인
docker-compose ps
```

### 4. 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# 상품 API 서버 실행
./gradlew :product-api:bootRun
```

### 5. API 확인

```bash
# 개발 환경
http://localhost:8080/api/v1

# Swagger UI
http://localhost:8080/swagger-ui.html

# Health Check
http://localhost:8080/api/v1/actuator/health
```

## 📂 템플릿 구조

### Common 모듈
- `DomainEvent`: 도메인 이벤트 인터페이스
- `DomainEventPublisher`: 이벤트 발행 인터페이스  
- `BusinessException`: 비즈니스 예외 기본 클래스
- `Assert`: 도메인 검증 유틸리티

### Domain 템플릿
- `AggregateRoot`: 애그리게이트 루트 기본 클래스
- `BaseEntity`: 엔티티 기본 클래스
- `ValueObject`: 값 객체 마커 인터페이스
- `Repository`: 레포지토리 인터페이스
- `UseCase`: 유스케이스 인터페이스
- `ApplicationService`: 애플리케이션 서비스 기본 클래스

### Infrastructure 템플릿
- **Persistence**: JPA 설정, 레포지토리 어댑터, Redis 설정
- **Kafka**: 이벤트 발행 어댑터, Kafka 설정

### Bootstrap 템플릿
- **Security**: JWT 인증, 보안 설정
- **Exception**: 전역 예외 처리
- **Config**: Swagger 설정
- **Application**: 메인 애플리케이션 클래스

## 🔧 사용 방법

### 1. 프로젝트 구조

상품 서비스는 다음의 모듈로 구성되어 있습니다:
- `product-domain`: 상품 도메인 핵심 로직
- `product-api`: REST API 엔드포인트
- `product-persistence`: JPA/Redis 영속성
- `product-kafka`: 이벤트 발행/구독

### 2. 도메인 모델 구현

```java
// 상품 도메인 엔티티 예시
@Getter
public class Product extends AggregateRoot<ProductId> {
    private final ProductId id;
    private final ProductName name;
    private final ProductPrice price;
    private final ProductStock stock;
    
    public Product(ProductId id, ProductName name, ProductPrice price, ProductStock stock) {
        this.id = id;
        this.name = name; 
        this.price = price;
        this.stock = stock;
        
        // 도메인 이벤트 발행
        addDomainEvent(new ProductCreatedEvent(id, name, price));
    }
    
    @Override
    public ProductId getId() {
        return id;
    }
}
```

### 3. 레포지토리 구현

```java
// 상품 도메인 레포지토리 인터페이스
public interface ProductRepository extends Repository<Product, ProductId> {
    Optional<Product> findByName(ProductName name);
    List<Product> findByPriceRange(ProductPrice minPrice, ProductPrice maxPrice);
}

// 인프라 레포지토리 어댑터
@Repository
public class ProductRepositoryAdapter extends GenericRepositoryAdapter<Product, ProductEntity, ProductId> 
        implements ProductRepository {
    
    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository, ProductMapper mapper) {
        super(jpaRepository, mapper::toDomain, mapper::toEntity);
    }
    
    @Override
    public Optional<Product> findByName(ProductName name) {
        return jpaRepository.findByName(name.getValue())
                .map(toDomainMapper);
    }
}
```

### 4. API 컨트롤러 구현

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    
    @PostMapping
    public ResponseEntity<CreateProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        
        CreateProductResponse response = createProductUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<GetProductResponse> getProduct(@PathVariable Long id) {
        GetProductResponse response = getProductUseCase.execute(id);
        return ResponseEntity.ok(response);
    }
}
```

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :product-domain:test
./gradlew :product-api:test

# 테스트 커버리지 확인
./gradlew jacocoTestReport
```

## 📋 개발 가이드

### 의존성 규칙
- ✅ Bootstrap → Core, Infrastructure, Common
- ✅ Infrastructure → Core, Common  
- ✅ Core → Common
- ❌ Core 모듈 간 직접 의존 금지
- ❌ Common → 다른 모듈 의존 금지

### 코딩 규칙
1. **불변 객체**: 값 객체는 불변으로 설계
2. **유효성 검증**: 생성자에서 비즈니스 규칙 검증
3. **도메인 이벤트**: 중요한 상태 변경 시 이벤트 발행
4. **패키지 구조**: domain, application, usecase로 명확히 분리

### 테스트 규칙
- Given-When-Then 패턴 적용
- 예외 상황 테스트 포함
- `@DisplayName` 사용하여 가독성 향상
- 최소 70% 테스트 커버리지 유지

## 🐳 Docker 지원

```bash
# 개발 환경 인프라 실행
docker-compose up -d

# 특정 서비스만 실행
docker-compose up -d mariadb redis

# 로그 확인
docker-compose logs -f kafka
```

## 📄 환경 설정

### 환경 변수
```bash
# 데이터베이스
DB_URL=jdbc:mariadb://localhost:3306/product_db
DB_USERNAME=product_user
DB_PASSWORD=product_pass

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Eureka
EUREKA_URL=http://localhost:8761/eureka/
```

### 프로필별 설정
- `application.yml`: 기본 설정
- `application-dev.yml`: 개발 환경 (H2, 디버그 로깅)
- `application-prod.yml`: 운영 환경 (MariaDB, 최적화)

## 🤝 기여 가이드

1. 이슈 등록 후 작업 시작
2. 기능별 브랜치 생성
3. 테스트 코드 작성 필수
4. PR 생성 및 리뷰 요청

## 📄 라이선스

MIT License

## 📞 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해 주세요.

---

이 boilerplate를 사용하여 빠르게 마이크로서비스를 개발하고, 헥사고날 아키텍처의 장점을 경험해보세요! 🚀