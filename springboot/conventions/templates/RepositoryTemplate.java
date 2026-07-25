package com.restaurant.api.conventions.templates;

import com.example.dto.TemplateDto;
import com.example.entity.Template;
// import com.example.common.QueryDslUtil;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// import static com.example.entity.QTemplate.template;

/**
 * Repository 템플릿
 *
 * 4개의 파일로 구성됩니다:
 * 1. {Domain}Repository - 인터페이스 (추상화)
 * 2. {Domain}JpaRepository - Spring Data JPA
 * 3. {Domain}QueryRepository - QueryDSL
 * 4. {Domain}RepositoryImpl - 구현체 (조합)
 *
 * 사용법:
 * 1. Template을 도메인명으로 변경 (예: Product)
 * 2. 각 클래스를 별도 파일로 분리
 * 3. 필요한 메서드 추가
 */

// ============================================================
// 1. Repository 인터페이스 (별도 파일: TemplateRepository.java)
// ============================================================
interface TemplateRepository {

    // 기본 CRUD
    Optional<Template> findById(String id);
    Template save(Template template);
    void delete(Template template);

    // 검색
    Page<Template> search(TemplateDto.SearchRequest searchRequest, Pageable pageable);

    // 추가 메서드 예시
    // Optional<Template> findByName(String name);
    // List<Template> findAllByUserId(String userId);
}


// ============================================================
// 2. JPA Repository (별도 파일: TemplateJpaRepository.java)
// ============================================================
interface TemplateJpaRepository extends JpaRepository<Template, String> {

    // 쿼리 메서드 예시
    // Optional<Template> findByName(String name);
    // Optional<Template> findByNameAndUserId(String name, String userId);
    // List<Template> findAllByUserIdOrderByCreatedAtDesc(String userId);
}


// ============================================================
// 3. Query Repository (별도 파일: TemplateQueryRepository.java)
// ============================================================
@Repository
@RequiredArgsConstructor
class TemplateQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 검색 (페이징)
     */
    public Page<Template> search(TemplateDto.SearchRequest searchRequest, Pageable pageable) {

        // 1. 데이터 조회
        // List<Template> content = queryFactory.selectFrom(template)
        //     .where(
        //         likeNameIfExists(searchRequest.name()),
        //         createdAtBetween(searchRequest.minCreatedAt(), searchRequest.maxCreatedAt()),
        //         template.deletedAt.isNull()  // Soft Delete 고려
        //     )
        //     .offset(pageable.getOffset())
        //     .orderBy(QueryDslUtil.getOrderSpecifiers(pageable.getSort(), template))
        //     .limit(pageable.getPageSize())
        //     .fetch();

        // 2. 전체 카운트 조회
        // Long totalCount = Optional.ofNullable(
        //     queryFactory.select(Wildcard.count)
        //         .from(template)
        //         .where(
        //             likeNameIfExists(searchRequest.name()),
        //             createdAtBetween(searchRequest.minCreatedAt(), searchRequest.maxCreatedAt()),
        //             template.deletedAt.isNull()
        //         )
        //         .fetchOne()
        // ).orElse(0L);

        // return new PageImpl<>(content, pageable, totalCount);

        // 임시 반환 (실제 구현 시 위 코드 사용)
        return Page.empty(pageable);
    }

    // ===== 동적 조건 메서드 =====

    // 단일 값 동등 비교
    // private BooleanExpression eqUserIdIfExists(String userId) {
    //     if (Objects.isNull(userId)) {
    //         return null;
    //     }
    //     return template.user.id.eq(userId);
    // }

    // 문자열 부분 검색 (LIKE)
    // private BooleanExpression likeNameIfExists(String name) {
    //     if (Objects.isNull(name) || name.isBlank()) {
    //         return null;
    //     }
    //     return template.name.contains(name);
    // }

    // 목록 포함 여부 (IN)
    // private BooleanExpression inStatusListIfExists(List<Status> statusList) {
    //     if (Objects.isNull(statusList) || statusList.isEmpty()) {
    //         return null;
    //     }
    //     return template.status.in(statusList);
    // }

    // 범위 조건
    // private BooleanExpression createdAtBetween(LocalDateTime minCreatedAt, LocalDateTime maxCreatedAt) {
    //     if (minCreatedAt != null && maxCreatedAt != null) {
    //         return template.createdAt.between(minCreatedAt, maxCreatedAt);
    //     } else if (minCreatedAt != null) {
    //         return template.createdAt.goe(minCreatedAt);
    //     } else if (maxCreatedAt != null) {
    //         return template.createdAt.loe(maxCreatedAt);
    //     }
    //     return null;
    // }
}


// ============================================================
// 4. Repository 구현체 (별도 파일: TemplateRepositoryImpl.java)
// ============================================================
@Repository
@RequiredArgsConstructor
class TemplateRepositoryImpl implements TemplateRepository {

    private final TemplateJpaRepository templateJpaRepository;
    private final TemplateQueryRepository templateQueryRepository;

    @Override
    public Page<Template> search(TemplateDto.SearchRequest searchRequest, Pageable pageable) {
        return templateQueryRepository.search(searchRequest, pageable);
    }

    @Override
    public Optional<Template> findById(String id) {
        return templateJpaRepository.findById(id);
    }

    @Override
    public Template save(Template template) {
        return templateJpaRepository.save(template);
    }

    @Override
    public void delete(Template template) {
        templateJpaRepository.delete(template);
    }

    // 추가 메서드 구현
    // @Override
    // public Optional<Template> findByName(String name) {
    //     return templateJpaRepository.findByName(name);
    // }
}
