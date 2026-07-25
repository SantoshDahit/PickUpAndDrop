package com.restaurant.api.conventions.templates;

import com.example.dto.TemplateDto;
import com.example.entity.Template;
import com.example.exception.ApiException;
import com.example.exception.ErrorCode;
import com.example.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service 템플릿
 *
 * 역할:
 * - 단일 도메인 CRUD
 * - Repository만 의존
 * - Entity 반환 (DTO가 아님)
 *
 * 사용법:
 * 1. Template을 도메인명으로 변경 (예: Product)
 * 2. ErrorCode에 해당 도메인 Not Found 에러 추가
 * 3. 필요한 메서드 추가
 */
@Service
@RequiredArgsConstructor
public class ServiceTemplate {

    private final TemplateRepository templateRepository;
    // 외부 의존성 (인코더 등)
    // private final PasswordEncoder passwordEncoder;

    // ===== 조회 메서드 =====

    /**
     * ID로 조회 (필수)
     * - Not Found 시 예외 발생
     */
    @Transactional(readOnly = true)
    public Template getById(String id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
                // TODO: ErrorCode.TEMPLATE_IS_NOT_FOUND로 변경
    }

    /**
     * ID로 조회 (선택)
     * - Not Found 시 null 반환
     */
    @Transactional(readOnly = true)
    public Template getNullableById(String id) {
        return templateRepository.findById(id).orElse(null);
    }

    /**
     * 조건으로 조회 (Optional 반환)
     */
    // @Transactional(readOnly = true)
    // public Optional<Template> getNullableByName(String name) {
    //     return templateRepository.findByName(name);
    // }

    /**
     * 검색 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<Template> search(TemplateDto.SearchRequest searchRequest, Pageable pageable) {
        return templateRepository.search(searchRequest, pageable);
    }

    // ===== 생성 메서드 =====

    /**
     * 생성 (파라미터 → Entity 생성 + 저장)
     * - Entity 생성 로직을 Service에 캡슐화
     * - 연관관계 설정, 유니크 코드 생성 등 부가 로직 포함 가능
     */
    @Transactional
    public Template create(String name, String description) {
        Template template = new Template(name, description);
        return templateRepository.save(template);
    }

    // 연관 Entity가 있는 생성 예시
    // @Transactional
    // public Template create(User user, String name, String description) {
    //     Template template = new Template(user, name, description);
    //     user.updateTemplate(template);  // 양방향 연관관계 설정
    //     return templateRepository.save(template);
    // }

    // ===== 저장 메서드 =====

    /**
     * 저장/수정 (이미 생성된 Entity를 저장)
     */
    @Transactional
    public Template save(Template template) {
        return templateRepository.save(template);
    }

    // ===== 삭제 메서드 =====

    /**
     * Soft Delete
     */
    @Transactional
    public void delete(Template template) {
        template.softDelete();
        templateRepository.save(template);
    }

    /**
     * Hard Delete (필요한 경우)
     */
    // @Transactional
    // public void hardDelete(Template template) {
    //     templateRepository.delete(template);
    // }

    // ===== 수정 메서드 =====

    /**
     * 이름 수정
     */
    @Transactional
    public Template updateName(Template template, String name) {
        template.updateName(name);
        return templateRepository.save(template);
    }

    /**
     * 전체 수정
     */
    @Transactional
    public Template update(Template template, String name, String description) {
        template.update(name, description);
        return templateRepository.save(template);
    }

    /**
     * 중복 검사 후 수정 (예: 이름 변경)
     */
    // @Transactional
    // public void updateName(Template template, String newName) {
    //     // 변경 없으면 스킵
    //     if (template.getName().equals(newName)) {
    //         return;
    //     }
    //
    //     // 중복 검사
    //     templateRepository.findByName(newName)
    //         .ifPresent(existing -> {
    //             throw new ApiException(ErrorCode.TEMPLATE_DUPLICATE_NAME);
    //         });
    //
    //     template.updateName(newName);
    //     templateRepository.save(template);
    // }

    /**
     * 비밀번호 수정 (인코딩 포함)
     */
    // @Transactional
    // public Template updatePassword(Template template, String password) {
    //     template.updatePassword(passwordEncoder.encode(password));
    //     return templateRepository.save(template);
    // }
}
