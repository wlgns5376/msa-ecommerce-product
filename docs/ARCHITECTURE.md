# 상품 관리 시스템 아키텍처 설계 문서

## 1. 시스템 개요

### 1.1 아키텍처 원칙
- **마이크로서비스 아키텍처**: 독립적으로 배포 가능한 서비스 단위
- **도메인 주도 설계 (DDD)**: Bounded Context 기반 서비스 분리
- **이벤트 드리븐 아키텍처**: 서비스 간 비동기 통신
- **API 우선 설계**: 명확한 인터페이스 정의

### 1.2 기술 스택
- **언어/프레임워크**: Node.js, TypeScript, NestJS
- **데이터베이스**: PostgreSQL (각 서비스별 독립 DB)
- **메시지 브로커**: Apache Kafka
- **캐시**: Redis
- **API Gateway**: Kong
- **컨테이너**: Docker, Kubernetes

## 2. 서비스 아키텍처

### 2.1 서비스 구성

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                           │
│                          (Kong)                              │
└─────────────────┬───────────────────────┬───────────────────┘
                  │                       │
        ┌─────────▼──────────┐  ┌────────▼──────────┐
        │  Product Service   │  │ Inventory Service │
        │                    │  │                   │
        │  - Products        │  │  - SKUs           │
        │  - Options         │  │  - Stock          │
        │  - Categories      │  │  - Reservations   │
        │                    │  │  - Movements      │
        └─────────┬──────────┘  └────────┬──────────┘
                  │                       │
        ┌─────────▼──────────┐  ┌────────▼──────────┐
        │   PostgreSQL       │  │   PostgreSQL      │
        │   (Product DB)     │  │  (Inventory DB)   │
        └────────────────────┘  └───────────────────┘
                  │                       │
                  └───────────┬───────────┘
                              │
                    ┌─────────▼──────────┐
                    │   Message Broker   │
                    │      (Kafka)       │
                    └────────────────────┘
```

### 2.2 서비스 책임

#### Product Service
- 상품 정보 관리 (생성, 수정, 조회, 삭제)
- 상품 옵션 관리
- 카테고리 관리 및 상품 분류
- 상품 검색 및 필터링

#### Inventory Service
- SKU 관리
- 재고 수량 관리
- 재고 입출고 처리
- 재고 선점/해제
- 재고 이동 이력 관리

### 2.3 서비스 간 통신

#### 동기 통신 (REST/gRPC)
```yaml
Product → Inventory:
  - GET /api/inventory/stock-status
  - POST /api/inventory/check-availability
  
Inventory → Product:
  - GET /api/products/sku-mappings
```

#### 비동기 통신 (Event)
```yaml
Events:
  - inventory.stock.updated
  - inventory.stock.reserved
  - inventory.stock.released
  - product.created
  - product.updated
  - product.deleted
```

## 3. 도메인 모델

### 3.1 Product Service 도메인

```typescript
// Aggregate Root
class Product {
  id: string
  name: string
  description: string
  type: ProductType // NORMAL | BUNDLE
  status: ProductStatus
  images: ProductImage[]
  options: ProductOption[]
  categories: Category[]
  createdAt: Date
  updatedAt: Date
}

// Value Objects
class ProductOption {
  id: string
  productId: string
  name: string
  price: Money
  status: OptionStatus
  skuMappings: SkuMapping[]
}

class SkuMapping {
  skuId: string
  quantity: number
}

// Entity
class Category {
  id: string
  name: string
  parentId?: string
  level: number
  sortOrder: number
  isActive: boolean
}
```

### 3.2 Inventory Service 도메인

```typescript
// Aggregate Root
class SKU {
  id: string
  code: string
  name: string
  description?: string
  weight?: number
  volume?: number
  createdAt: Date
}

// Entity
class Inventory {
  skuId: string
  totalQuantity: number
  reservedQuantity: number
  availableQuantity: number // calculated
  lastUpdated: Date
}

// Value Object
class StockMovement {
  id: string
  skuId: string
  type: MovementType
  quantity: number
  reference: string
  createdAt: Date
}

// Entity
class Reservation {
  id: string
  skuId: string
  quantity: number
  orderId: string
  expiresAt: Date
  status: ReservationStatus
}
```

## 4. 데이터베이스 설계

### 4.1 Product Service Schema

```sql
-- Products Table
CREATE TABLE products (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  type VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- Product Options Table
CREATE TABLE product_options (
  id UUID PRIMARY KEY,
  product_id UUID REFERENCES products(id),
  name VARCHAR(255) NOT NULL,
  price DECIMAL(10,2),
  status VARCHAR(50),
  created_at TIMESTAMP
);

-- SKU Mappings Table
CREATE TABLE sku_mappings (
  id UUID PRIMARY KEY,
  option_id UUID REFERENCES product_options(id),
  sku_id VARCHAR(100) NOT NULL,
  quantity INTEGER NOT NULL
);

-- Categories Table
CREATE TABLE categories (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  parent_id UUID REFERENCES categories(id),
  level INTEGER NOT NULL,
  sort_order INTEGER,
  is_active BOOLEAN DEFAULT true
);

-- Product Categories (N:M)
CREATE TABLE product_categories (
  product_id UUID REFERENCES products(id),
  category_id UUID REFERENCES categories(id),
  is_primary BOOLEAN DEFAULT false,
  PRIMARY KEY (product_id, category_id)
);
```

### 4.2 Inventory Service Schema

```sql
-- SKUs Table
CREATE TABLE skus (
  id VARCHAR(100) PRIMARY KEY,
  code VARCHAR(100) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  weight DECIMAL(10,3),
  volume DECIMAL(10,3),
  created_at TIMESTAMP
);

-- Inventory Table
CREATE TABLE inventory (
  sku_id VARCHAR(100) PRIMARY KEY REFERENCES skus(id),
  total_quantity INTEGER NOT NULL DEFAULT 0,
  reserved_quantity INTEGER NOT NULL DEFAULT 0,
  last_updated TIMESTAMP
);

-- Stock Movements Table
CREATE TABLE stock_movements (
  id UUID PRIMARY KEY,
  sku_id VARCHAR(100) REFERENCES skus(id),
  type VARCHAR(50) NOT NULL,
  quantity INTEGER NOT NULL,
  reference VARCHAR(255),
  created_at TIMESTAMP
);

-- Reservations Table
CREATE TABLE reservations (
  id UUID PRIMARY KEY,
  sku_id VARCHAR(100) REFERENCES skus(id),
  quantity INTEGER NOT NULL,
  order_id VARCHAR(100),
  expires_at TIMESTAMP,
  status VARCHAR(50),
  created_at TIMESTAMP
);
```

## 5. API 설계

### 5.1 Product Service API

#### 상품 관리
```yaml
# 상품 생성
POST /api/products
Request:
  name: string
  description: string
  type: NORMAL | BUNDLE
  categoryIds: string[]

# 상품 조회
GET /api/products/{productId}
Response:
  product: Product
  options: ProductOption[]
  stockStatus: StockStatus[]

# 상품 목록
GET /api/products
Query:
  categoryId?: string
  page: number
  size: number
  sort: string
```

#### 카테고리 관리
```yaml
# 카테고리 트리 조회
GET /api/categories/tree
Response:
  categories: CategoryTree[]

# 카테고리별 상품
GET /api/categories/{categoryId}/products
Query:
  page: number
  size: number
```

### 5.2 Inventory Service API

#### SKU 관리
```yaml
# SKU 생성
POST /api/skus
Request:
  code: string
  name: string
  initialStock: number

# 재고 조회
GET /api/inventory/{skuId}
Response:
  skuId: string
  totalQuantity: number
  reservedQuantity: number
  availableQuantity: number
```

#### 재고 작업
```yaml
# 재고 입고
POST /api/inventory/{skuId}/receive
Request:
  quantity: number
  reference: string

# 재고 선점
POST /api/inventory/reservations
Request:
  skuId: string
  quantity: number
  orderId: string
  ttl: number
```

## 6. 이벤트 설계

### 6.1 이벤트 스키마

```typescript
interface DomainEvent {
  eventId: string
  eventType: string
  aggregateId: string
  timestamp: Date
  payload: any
}

// 재고 업데이트 이벤트
interface StockUpdatedEvent extends DomainEvent {
  eventType: 'inventory.stock.updated'
  payload: {
    skuId: string
    previousQuantity: number
    currentQuantity: number
    movementType: string
  }
}

// 재고 선점 이벤트
interface StockReservedEvent extends DomainEvent {
  eventType: 'inventory.stock.reserved'
  payload: {
    reservationId: string
    skuId: string
    quantity: number
    orderId: string
  }
}
```

### 6.2 이벤트 흐름

```
주문 생성 → 재고 확인 → 재고 선점 → 선점 이벤트 발행
         ↓
    재고 부족 시
         ↓
    품절 처리
```

## 7. 성능 최적화 전략

### 7.1 캐싱 전략
- **Redis 캐싱**: 자주 조회되는 상품 정보
- **캐시 무효화**: 이벤트 기반 캐시 갱신
- **TTL 설정**: 카테고리(1시간), 상품(10분), 재고(30초)

### 7.2 데이터베이스 최적화
- **인덱싱**: 검색 필드 복합 인덱스
- **파티셔닝**: 재고 이동 이력 테이블 월별 파티션
- **읽기 전용 복제본**: 조회 트래픽 분산

### 7.3 API 최적화
- **GraphQL**: 필요한 필드만 선택적 조회
- **배치 API**: 대량 재고 조회
- **페이지네이션**: 커서 기반 페이징

## 8. 보안 설계

### 8.1 인증/인가
- **JWT 토큰**: API Gateway 레벨 인증
- **역할 기반 접근 제어**: Admin, Manager, User
- **서비스 간 인증**: mTLS

### 8.2 데이터 보안
- **전송 암호화**: TLS 1.3
- **민감 정보 암호화**: AES-256
- **감사 로그**: 모든 변경 작업 기록

## 9. 모니터링 및 로깅

### 9.1 메트릭
- **비즈니스 메트릭**: 상품 조회수, 재고 정확도
- **기술 메트릭**: API 응답시간, 에러율
- **인프라 메트릭**: CPU, 메모리, 디스크

### 9.2 로깅
- **구조화 로깅**: JSON 형식
- **분산 추적**: OpenTelemetry
- **로그 집계**: ELK Stack

## 10. 배포 전략

### 10.1 CI/CD
- **빌드**: GitHub Actions
- **테스트**: 단위/통합/E2E 테스트
- **배포**: Kubernetes Rolling Update

### 10.2 환경 구성
- **개발**: Minikube
- **스테이징**: EKS 클러스터
- **운영**: Multi-AZ EKS

## 11. 재해 복구

### 11.1 백업 전략
- **데이터베이스**: 일일 전체 백업, 시간별 증분 백업
- **설정 파일**: Git 버전 관리
- **복구 시간 목표(RTO)**: 1시간

### 11.2 고가용성
- **서비스**: 최소 3개 레플리카
- **데이터베이스**: Master-Slave 구성
- **로드 밸런서**: 다중 AZ 분산