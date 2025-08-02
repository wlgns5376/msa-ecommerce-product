# 사용 가이드

이 문서는 MSA E-commerce Boilerplate를 사용하여 새로운 마이크로서비스를 개발하는 방법을 설명합니다.

## 🚀 빠른 시작

### 1. 프로젝트 설정

```bash
# 1. Boilerplate 복사
cp -r msa-ecommerce-boilerplate my-service
cd my-service

# 2. 패키지명 변경 (선택사항)
# com.commerce.boilerplate → com.mycompany.myservice
find . -name "*.java" -exec sed -i 's/com.commerce.boilerplate/com.mycompany.myservice/g' {} \;

# 3. 프로젝트명 변경
sed -i 's/msa-ecommerce-boilerplate/my-service/g' settings.gradle
```

### 2. 도메인 모듈 생성

```bash
# 예: User 도메인 생성
mkdir -p core/user-core/src/main/java/com/mycompany/myservice/user
mkdir -p bootstrap/user-api/src/main/java/com/mycompany/myservice/user/api
mkdir -p infrastructure/user-persistence/src/main/java/com/mycompany/myservice/user/infrastructure
```

### 3. settings.gradle 업데이트

```gradle
// 기존 템플릿 제거 또는 유지
include 'domain-template'
project(':domain-template').projectDir = file('core/domain-template')

// 새 도메인 추가
include 'user-core'
project(':user-core').projectDir = file('core/user-core')

include 'user-api'
project(':user-api').projectDir = file('bootstrap/user-api')

include 'user-persistence'
project(':user-persistence').projectDir = file('infrastructure/user-persistence')
```

## 📋 단계별 개발 가이드

### Step 1: 도메인 모델 설계

#### 1.1 값 객체 생성

```java
// UserId.java
@Getter
@EqualsAndHashCode
public class UserId implements ValueObject {
    private final String value;
    
    public UserId(String value) {
        Assert.hasText(value, "사용자 ID는 필수입니다.");
        this.value = value;
    }
    
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
}

// Email.java
@Getter
@EqualsAndHashCode
public class Email implements ValueObject {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    private final String value;
    
    public Email(String value) {
        Assert.hasText(value, "이메일은 필수입니다.");
        Assert.isTrue(EMAIL_PATTERN.matcher(value).matches(), 
                     "올바른 이메일 형식이 아닙니다.");
        this.value = value;
    }
}
```

#### 1.2 도메인 엔티티 생성

```java
// User.java
@Getter
public class User extends AggregateRoot<UserId> {
    private final UserId id;
    private final Email email;
    private final String name;
    private UserStatus status;
    private final LocalDateTime createdAt;
    
    public User(UserId id, Email email, String name) {
        Assert.notNull(id, "사용자 ID는 필수입니다.");
        Assert.notNull(email, "이메일은 필수입니다.");
        Assert.hasText(name, "이름은 필수입니다.");
        
        this.id = id;
        this.email = email;
        this.name = name;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        
        // 도메인 이벤트 발행
        addDomainEvent(new UserCreatedEvent(id, email, name));
    }
    
    public void deactivate() {
        if (this.status == UserStatus.INACTIVE) {
            throw new UserAlreadyDeactivatedException(this.id);
        }
        
        this.status = UserStatus.INACTIVE;
        markAsUpdated();
        
        addDomainEvent(new UserDeactivatedEvent(this.id));
    }
    
    @Override
    public UserId getId() {
        return id;
    }
}
```

#### 1.3 도메인 이벤트 생성

```java
// UserCreatedEvent.java
@Getter
@AllArgsConstructor
public class UserCreatedEvent implements DomainEvent {
    private final UserId userId;
    private final Email email;
    private final String name;
    private final LocalDateTime occurredAt;
    
    public UserCreatedEvent(UserId userId, Email email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.occurredAt = LocalDateTime.now();
    }
    
    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
    
    @Override
    public String eventType() {
        return "UserCreatedEvent";
    }
}
```

### Step 2: 애플리케이션 계층 구현

#### 2.1 레포지토리 인터페이스 정의

```java
// UserRepository.java
public interface UserRepository extends Repository<User, UserId> {
    Optional<User> findByEmail(Email email);
    List<User> findByStatus(UserStatus status);
    boolean existsByEmail(Email email);
}
```

#### 2.2 유스케이스 정의

```java
// CreateUserUseCase.java
public interface CreateUserUseCase extends UseCase<CreateUserRequest, CreateUserResponse> {
}

// CreateUserRequest.java
@Getter
@AllArgsConstructor
public class CreateUserRequest {
    private final String email;
    private final String name;
}

// CreateUserResponse.java
@Getter
@AllArgsConstructor
public class CreateUserResponse {
    private final String userId;
    private final String email;
    private final String name;
    private final String status;
    
    public static CreateUserResponse from(User user) {
        return new CreateUserResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            user.getName(),
            user.getStatus().name()
        );
    }
}
```

#### 2.3 애플리케이션 서비스 구현

```java
// UserApplicationService.java
@Service
@RequiredArgsConstructor
public class UserApplicationService extends ApplicationService 
        implements CreateUserUseCase, DeactivateUserUseCase {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public CreateUserResponse execute(CreateUserRequest request) {
        // 1. 중복 검증
        Email email = new Email(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        
        // 2. 도메인 객체 생성
        UserId userId = UserId.generate();
        User user = new User(userId, email, request.getName());
        
        // 3. 저장
        User savedUser = userRepository.save(user);
        
        // 4. 도메인 이벤트 발행
        publishDomainEvents(savedUser);
        
        return CreateUserResponse.from(savedUser);
    }
}
```

### Step 3: 인프라스트럭처 계층 구현

#### 3.1 JPA 엔티티 생성

```java
// UserEntity.java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseJpaEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;
    
    public UserEntity(String id, String email, String name, UserStatus status) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.status = status;
    }
}
```

#### 3.2 JPA 레포지토리 생성

```java
// UserJpaRepository.java
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
    List<UserEntity> findByStatus(UserStatus status);
    boolean existsByEmail(String email);
}
```

#### 3.3 도메인-엔티티 매퍼 생성

```java
// UserMapper.java
@Component
public class UserMapper {
    
    public User toDomain(UserEntity entity) {
        return User.restore(
            new UserId(entity.getId()),
            new Email(entity.getEmail()),
            entity.getName(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    public UserEntity toEntity(User domain) {
        return new UserEntity(
            domain.getId().getValue(),
            domain.getEmail().getValue(),
            domain.getName(),
            domain.getStatus()
        );
    }
}
```

#### 3.4 레포지토리 어댑터 구현

```java
// UserRepositoryAdapter.java
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter extends GenericRepositoryAdapter<User, UserEntity, UserId> 
        implements UserRepository {
    
    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;
    
    public UserRepositoryAdapter(UserJpaRepository jpaRepository, UserMapper mapper) {
        super(jpaRepository, mapper::toDomain, mapper::toEntity);
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue())
                .map(mapper::toDomain);
    }
    
    @Override
    public List<User> findByStatus(UserStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }
}
```

### Step 4: API 계층 구현

#### 4.1 DTO 생성

```java
// CreateUserApiRequest.java
@Getter
@NoArgsConstructor
public class CreateUserApiRequest {
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;
    
    public CreateUserRequest toServiceRequest() {
        return new CreateUserRequest(email, name);
    }
}
```

#### 4.2 컨트롤러 구현

```java
// UserController.java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    
    @PostMapping
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다.")
    public ResponseEntity<CreateUserResponse> createUser(
            @Valid @RequestBody CreateUserApiRequest request) {
        
        CreateUserResponse response = createUserUseCase.execute(request.toServiceRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 조회", description = "사용자 정보를 조회합니다.")
    public ResponseEntity<GetUserResponse> getUser(
            @PathVariable String userId) {
        
        GetUserResponse response = getUserUseCase.execute(new GetUserRequest(userId));
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 비활성화", description = "사용자를 비활성화합니다.")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable String userId) {
        
        deactivateUserUseCase.execute(new DeactivateUserRequest(userId));
        return ResponseEntity.noContent().build();
    }
}
```

### Step 5: 테스트 작성

#### 5.1 도메인 테스트

```java
// UserTest.java
@DisplayName("사용자 도메인 테스트")
class UserTest {
    
    @Test
    @DisplayName("사용자 생성 시 도메인 이벤트가 발행된다")
    void createUser_ShouldPublishDomainEvent() {
        // Given
        UserId userId = UserId.generate();
        Email email = new Email("test@example.com");
        String name = "테스트 사용자";
        
        // When
        User user = new User(userId, email, name);
        
        // Then
        assertThat(user.getDomainEvents()).hasSize(1);
        assertThat(user.getDomainEvents().get(0))
                .isInstanceOf(UserCreatedEvent.class);
    }
    
    @Test
    @DisplayName("비활성화된 사용자를 다시 비활성화하면 예외가 발생한다")
    void deactivateAlreadyInactiveUser_ShouldThrowException() {
        // Given
        User user = createUser();
        user.deactivate();
        
        // When & Then
        assertThatThrownBy(user::deactivate)
                .isInstanceOf(UserAlreadyDeactivatedException.class);
    }
}
```

#### 5.2 애플리케이션 서비스 테스트

```java
// UserApplicationServiceTest.java
@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 애플리케이션 서비스 테스트")  
class UserApplicationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DomainEventPublisher domainEventPublisher;
    
    @InjectMocks
    private UserApplicationService userApplicationService;
    
    @Test
    @DisplayName("중복된 이메일로 사용자 생성 시 예외가 발생한다")
    void createUserWithDuplicateEmail_ShouldThrowException() {
        // Given
        CreateUserRequest request = new CreateUserRequest("test@example.com", "테스트");
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userApplicationService.execute(request))
                .isInstanceOf(DuplicateEmailException.class);
    }
}
```

#### 5.3 통합 테스트

```java
// UserControllerIntegrationTest.java
@SpringBootTest
@AutoConfigureTestDatabase
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("사용자 컨트롤러 통합 테스트")
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("사용자 생성 API 통합 테스트")
    void createUser_IntegrationTest() {
        // Given
        CreateUserApiRequest request = new CreateUserApiRequest();
        request.setEmail("test@example.com");
        request.setName("테스트 사용자");
        
        // When
        ResponseEntity<CreateUserResponse> response = restTemplate.postForEntity(
                "/api/v1/users", request, CreateUserResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
    }
}
```

## 🔧 고급 기능

### 1. 이벤트 소싱 구현

```java
// EventStore.java
public interface EventStore {
    void save(String aggregateId, List<DomainEvent> events, long expectedVersion);
    List<DomainEvent> getEventsForAggregate(String aggregateId);
}

// EventSourcedAggregateRoot.java
public abstract class EventSourcedAggregateRoot<ID> extends AggregateRoot<ID> {
    private long version = 0;
    
    protected void apply(DomainEvent event) {
        applyEvent(event);
        addDomainEvent(event);
    }
    
    protected abstract void applyEvent(DomainEvent event);
}
```

### 2. CQRS 패턴 구현

```java
// Query 모델
@Entity
@Table(name = "user_read_model")
public class UserReadModel {
    @Id
    private String id;
    private String email;
    private String name;
    private String status;
    // 조회 최적화된 필드들
}

// Query Handler
@Component
public class UserQueryHandler {
    
    private final UserReadModelRepository repository;
    
    public UserDetailView getUserDetail(String userId) {
        return repository.findById(userId)
                .map(UserDetailView::from)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
```

### 3. 사가 패턴 구현

```java
// OrderSaga.java
@Component
public class OrderSaga {
    
    @EventHandler
    public void handle(OrderCreatedEvent event) {
        // 1. 재고 확인
        // 2. 결제 처리
        // 3. 배송 준비
    }
    
    @EventHandler
    public void handle(PaymentFailedEvent event) {
        // 보상 트랜잭션 실행
        // 주문 취소 처리
    }
}
```

## 📚 참고 자료

- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS](https://martinfowler.com/bliki/CQRS.html)