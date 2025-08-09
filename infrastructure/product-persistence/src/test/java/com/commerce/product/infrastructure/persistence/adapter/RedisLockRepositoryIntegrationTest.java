package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.model.DistributedLock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Redis Testcontainer required")
class RedisLockRepositoryIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private RedisLockRepositoryAdapter lockRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
    
    @Test
    @DisplayName("락을 성공적으로 획득하고 해제할 수 있다")
    void acquireAndReleaseLock() {
        // Given
        String key = "test-lock";
        Duration leaseDuration = Duration.ofSeconds(5);
        Duration waitTimeout = Duration.ofSeconds(1);
        
        // When
        Optional<DistributedLock> lockOpt = lockRepository.acquireLock(key, leaseDuration, waitTimeout);
        
        // Then
        assertThat(lockOpt).isPresent();
        assertThat(lockRepository.isLocked(key)).isTrue();
        
        // When - 락 해제
        boolean released = lockRepository.releaseLock(lockOpt.get());
        
        // Then
        assertThat(released).isTrue();
        assertThat(lockRepository.isLocked(key)).isFalse();
    }
    
    @Test
    @DisplayName("동일한 키에 대해 중복 락 획득은 실패한다")
    void duplicateLockAcquisitionFails() {
        // Given
        String key = "test-lock";
        Duration leaseDuration = Duration.ofSeconds(5);
        Duration waitTimeout = Duration.ofMillis(100);
        
        // When - 첫 번째 락 획득
        Optional<DistributedLock> firstLock = lockRepository.acquireLock(key, leaseDuration, waitTimeout);
        
        // When - 두 번째 락 획득 시도
        Optional<DistributedLock> secondLock = lockRepository.acquireLock(key, leaseDuration, waitTimeout);
        
        // Then
        assertThat(firstLock).isPresent();
        assertThat(secondLock).isEmpty();
        
        // Cleanup
        lockRepository.releaseLock(firstLock.get());
    }
    
    @Test
    @DisplayName("락 연장이 정상적으로 동작한다")
    void extendLock() {
        // Given
        String key = "test-lock";
        Duration leaseDuration = Duration.ofSeconds(2);
        Duration waitTimeout = Duration.ofSeconds(1);
        Optional<DistributedLock> lockOpt = lockRepository.acquireLock(key, leaseDuration, waitTimeout);
        
        assertThat(lockOpt).isPresent();
        DistributedLock lock = lockOpt.get();
        
        // When
        boolean extended = lockRepository.extendLock(lock, Duration.ofSeconds(3));
        
        // Then
        assertThat(extended).isTrue();
        assertThat(lockRepository.isLocked(key)).isTrue();
        
        // 원래 만료 시간 이후에도 락이 유지되는지 확인
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThat(lockRepository.isLocked(key)).isTrue();
        
        // Cleanup
        lockRepository.releaseLock(lock);
    }
    
    @Test
    @DisplayName("동시에 여러 스레드가 락을 획득하려 할 때 하나만 성공한다")
    void concurrentLockAcquisition() throws InterruptedException {
        // Given
        String key = "concurrent-lock";
        int threadCount = 10;
        Duration leaseDuration = Duration.ofSeconds(1);
        Duration waitTimeout = Duration.ofMillis(50);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<DistributedLock> acquiredLocks = new CopyOnWriteArrayList<>();
        
        // When
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Optional<DistributedLock> lockOpt = lockRepository.acquireLock(key, leaseDuration, waitTimeout);
                    if (lockOpt.isPresent()) {
                        successCount.incrementAndGet();
                        acquiredLocks.add(lockOpt.get());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(acquiredLocks).hasSize(1);
        
        // Cleanup
        lockRepository.releaseLock(acquiredLocks.get(0));
        executor.shutdown();
    }
    
    @Test
    @DisplayName("락 대기 시간 동안 재시도하여 락을 획득할 수 있다")
    void lockAcquisitionWithRetry() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        String key = "retry-lock";
        Duration leaseDuration = Duration.ofMillis(500);
        Duration waitTimeout = Duration.ofSeconds(2);
        
        // 첫 번째 락 획득
        Optional<DistributedLock> firstLock = lockRepository.acquireLock(key, leaseDuration, Duration.ZERO);
        assertThat(firstLock).isPresent();
        
        // When - 다른 스레드에서 대기하며 락 획득 시도
        CompletableFuture<Optional<DistributedLock>> futureLock = CompletableFuture.supplyAsync(() ->
            lockRepository.acquireLock(key, leaseDuration, waitTimeout)
        );
        
        // 첫 번째 락 해제
        Thread.sleep(300);
        lockRepository.releaseLock(firstLock.get());
        
        // Then
        Optional<DistributedLock> secondLock = futureLock.get(3, TimeUnit.SECONDS);
        assertThat(secondLock).isPresent();
        
        // Cleanup
        lockRepository.releaseLock(secondLock.get());
    }
    
    @Test
    @DisplayName("서로 다른 키에 대한 락은 독립적으로 동작한다")
    void independentLocks() {
        // Given
        String key1 = "lock-1";
        String key2 = "lock-2";
        Duration leaseDuration = Duration.ofSeconds(5);
        Duration waitTimeout = Duration.ofSeconds(1);
        
        // When
        Optional<DistributedLock> lock1 = lockRepository.acquireLock(key1, leaseDuration, waitTimeout);
        Optional<DistributedLock> lock2 = lockRepository.acquireLock(key2, leaseDuration, waitTimeout);
        
        // Then
        assertThat(lock1).isPresent();
        assertThat(lock2).isPresent();
        assertThat(lock1.get().key()).isNotEqualTo(lock2.get().key());
        
        // Cleanup
        lockRepository.releaseLock(lock1.get());
        lockRepository.releaseLock(lock2.get());
    }
}