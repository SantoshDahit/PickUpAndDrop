# 백엔드 컨벤션 문서

Spring Boot 백엔드 프로젝트의 구조와 컨벤션을 정리한 가이드입니다.
새 프로젝트에서도 동일한 아키텍처를 적용할 수 있도록 작성되었습니다.

---

## 문서 목록

| 번호 | 문서 | 설명 |
|------|------|------|
| 1 | [프로젝트 개요](./01-project-overview.md) | 기술 스택, 의존성, 패키지 구조 |
| 2 | [아키텍처](./02-architecture.md) | 4계층 구조 (Controller → Facade → Service → Repository) |
| 3 | [Entity 컨벤션](./03-entity-convention.md) | Base Entity, UUID ID, update 메서드 패턴 |
| 4 | [DTO 컨벤션](./04-dto-convention.md) | Inner Class 패턴 (PostRequest, Response 등) |
| 5 | [Mapper 컨벤션](./05-mapper-convention.md) | BaseMapper + ModelMapper 변환 패턴 |
| 6 | [Repository 컨벤션](./06-repository-convention.md) | 3계층 Repository 패턴 (Interface, Jpa, Query) |
| 7 | [Service/Facade 컨벤션](./07-service-facade-convention.md) | 트랜잭션 패턴, 역할 분리, @Facade 어노테이션 |
| 8 | [Controller 컨벤션](./08-controller-convention.md) | RESTful URL 설계, HTTP 메서드 매핑 |
| 9 | [예외 처리 컨벤션](./09-exception-convention.md) | ErrorCode, ApiException, ErrorResponse |
| 10 | [API 로깅 컨벤션](./10-api-logging-convention.md) | AOP 기반 로깅, @NoApiLogging, 민감정보 마스킹 |
| 11 | [Soft Delete 컨벤션](./11-soft-delete-convention.md) | deletedAt 필터링, QueryDSL 수동 필터 패턴 |
| 12 | [네이밍 규칙](./12-naming-convention.md) | 클래스명, 메서드명, 변수명 규칙 |
| 13 | [OAuth 소셜 로그인 컨벤션](./13-oauth-convention.md) | Strategy + Factory OAuth 패턴, Provider 추가 가이드 |
| 14 | [JWT & Security 컨벤션](./14-jwt-security-convention.md) | JWT 인증, Security Filter Chain, Whitelist 패턴 |
| 15 | [AOP 동적 응답값 Enrichment 컨벤션](./15-aop-enrichment-convention.md) | AOP 동적 응답값 주입, 재귀 엔티티 추출 |
| 16 | [FCM 푸시 알림 컨벤션](./16-fcm-push-convention.md) | Firebase 푸시, 배치 전송, PushEvent |
| 17 | [SMS 인증 컨벤션](./17-sms-verification-convention.md) | SMS 인증 코드, PIN 생성, 상태 전이 |
| 18 | [배포 컨벤션](./18-deployment-convention.md) | CI/CD, Docker, Blue-Green 배포 |
| 19 | [설정 관리 컨벤션](./19-configuration-convention.md) | 프로필 구조, ConfigurationProperties |
| 20 | [Flyway 마이그레이션 컨벤션](./20-flyway-convention.md) | DB 마이그레이션 네이밍, 작성 규칙 |
| 21 | [이미지 업로드 컨벤션](./21-image-upload-convention.md) | 이미지 업로드 처리 |
| 22 | [RESTful API 설계 컨벤션](./22-restful-api-convention.md) | RESTful 설계 원칙, 리소스 중심 설계, 상태 코드 |
| 23 | [테스트 컨벤션](./23-test-convention.md) | Testcontainers, 통합 테스트 구조, DB 격리 |
| 24 | [Git 브랜치 컨벤션](./24-git-branch-convention.md) | 한글 브랜치명, PR target, prefix 자유 |
| 25 | [AI 어시스턴트 협업 컨벤션](./25-ai-collaboration.md) | push/PR/머지는 사용자 명시 허락 후 진행 |
| 26 | [Prometheus 모니터링 컨벤션](./26-prometheus-convention.md) | actuator + micrometer-registry-prometheus, 화이트리스트 최소화, 커스텀 메트릭 패턴 |

---

## 코드 템플릿

[templates/](./templates/) 폴더에서 각 레이어별 Java 템플릿을 확인할 수 있습니다.

| 템플릿 | 설명 |
|--------|------|
| [BaseMapperTemplate.java](./templates/BaseMapperTemplate.java) | BaseMapper 추상 클래스 (프로젝트 내 포함) |
| [EntityTemplate.java](./templates/EntityTemplate.java) | Entity 클래스 템플릿 |
| [DtoTemplate.java](./templates/DtoTemplate.java) | DTO Inner Class 패턴 템플릿 |
| [MapperTemplate.java](./templates/MapperTemplate.java) | BaseMapper 상속 Mapper 템플릿 |
| [RepositoryTemplate.java](./templates/RepositoryTemplate.java) | 3계층 Repository 템플릿 |
| [ServiceTemplate.java](./templates/ServiceTemplate.java) | Service 클래스 템플릿 |
| [FacadeTemplate.java](./templates/FacadeTemplate.java) | Facade 클래스 템플릿 |
| [ControllerTemplate.java](./templates/ControllerTemplate.java) | Controller 클래스 템플릿 |

---

## 퀵스타트: 새 도메인 추가하기

새로운 도메인(예: `Product`)을 추가할 때 아래 순서로 작업합니다.

### 1. Entity 생성
```java
// src/main/java/.../entity/Product.java
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseFullTimeEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    // 필드 정의...

    public Product(/* 생성 파라미터 */) {
        this.id = UUID.randomUUID().toString();
        // 초기화...
    }

    public void update(/* 수정 파라미터 */) {
        // 수정 로직...
    }
}
```

### 2. DTO 생성
```java
// src/main/java/.../dto/ProductDto.java
public class ProductDto {
    public record PostRequest(/* 필드 */) {}
    public record PatchRequest(/* 필드 */) {}

    @Getter
    public static class Response { /* 필드 */ }

    @Getter
    @NoArgsConstructor
    public static class SummaryResponse { /* 필드 */ }

    public record SearchRequest(/* 검색 조건 */) {}
}
```

### 3. Repository 생성 (4개 파일)
```
ProductRepository.java       // 인터페이스
ProductJpaRepository.java    // Spring Data JPA
ProductQueryRepository.java  // QueryDSL
ProductRepositoryImpl.java   // 구현체
```

### 4. Mapper 생성
```java
// src/main/java/.../mapper/ProductMapper.java
@Component
public class ProductMapper extends BaseMapper<Product, ProductDto> {

    protected ProductMapper(ModelMapper modelMapper) {
        super(modelMapper, Product.class);
        this.registerDtoMapping(ProductDto.class);
        this.registerDtoMapping(ProductDto.Response.class);
        this.registerDtoMapping(ProductDto.SummaryResponse.class);
    }

    public ProductDto.Response toResponse(Product entity) {
        return super.toDto(entity, ProductDto.Response.class);
    }

    public ProductDto.SummaryResponse toSummaryResponse(Product entity) {
        return super.toDto(entity, ProductDto.SummaryResponse.class);
    }
}
```

### 5. Service 생성
```java
// src/main/java/.../service/ProductService.java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    // getById, search, save, delete 메서드
}
```

### 6. Facade 생성
```java
// src/main/java/.../service/ProductFacade.java
@Component
@RequiredArgsConstructor
public class ProductFacade {
    private final ProductService productService;
    private final ProductMapper productMapper;
    // DTO 변환 및 여러 Service 조합
}
```

### 7. Controller 생성
```java
// src/main/java/.../controller/ProductController.java
@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductFacade productFacade;
    // CRUD 엔드포인트
}
```

### 8. ErrorCode 추가
```java
// ErrorCode.java에 추가
PRODUCT_IS_NOT_FOUND(HttpStatus.NOT_FOUND, "제품을 찾을 수 없습니다.", ""),
```

---

## 핵심 원칙

1. **BCNF 정규화**: 모든 테이블은 BCNF까지 정규화 (반정규화 시 사유 기록 필수)
2. **레이어 분리**: 각 레이어는 명확한 책임을 가짐
3. **단방향 의존성**: Controller → Facade → Service → Repository
4. **트랜잭션 경계**: Facade에서 `@Transactional` 정의
5. **DTO 변환**: Facade에서 Entity ↔ DTO 변환 (Mapper 사용)
6. **Soft Delete**: 대부분의 Entity에서 `deletedAt`을 통한 논리 삭제 사용

---

## 참고 자료

- QueryDSL (Jakarta 분류자)을 사용합니다.
- Spring Boot 기반입니다.
