package com.restaurant.api.conventions.templates;

import com.example.dto.TemplateDto;
import com.example.entity.Template;
import com.example.exception.ApiException;
import com.example.exception.ErrorCode;
import com.example.mapper.TemplateMapper;
import com.example.annotation.Facade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Facade 템플릿
 *
 * 역할:
 * - 트랜잭션 경계 정의
 * - 여러 Service 조합
 * - DTO ↔ Entity 변환 (Mapper 사용)
 * - 비즈니스 흐름 조율
 *
 * 사용법:
 * 1. Template을 도메인명으로 변경 (예: Product)
 * 2. 필요한 Service 의존성 추가
 * 3. Mapper 구현
 */
@Facade  // @Component 대신 @Facade 사용 (역할 명시)
@RequiredArgsConstructor
public class FacadeTemplate {

    // ===== Service 의존성 =====
    private final ServiceTemplate templateService;
    // private final UserService userService;
    // private final FileAttachmentService fileAttachmentService;

    // ===== Mapper 의존성 =====
    private final TemplateMapper templateMapper;

    // ===== 조회 =====

    /**
     * 검색 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<TemplateDto.SummaryResponse> search(TemplateDto.SearchRequest searchRequest, Pageable pageable) {
        return templateService.search(searchRequest, pageable)
                .map(templateMapper::toSummaryResponse);
    }

    /**
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public TemplateDto.Response getById(String id) {
        return templateMapper.toResponse(
                templateService.getById(id)
        );
    }

    // ===== 생성 =====

    /**
     * 생성
     */
    @Transactional
    public TemplateDto.Response create(TemplateDto.PostRequest request) {
        // 1. Entity 생성
        Template template = new Template(
                request.name(),
                request.description()
        );

        // 2. 저장
        Template savedTemplate = templateService.save(template);

        // 3. DTO 변환 및 반환
        return templateMapper.toResponse(savedTemplate);
    }

    /**
     * 생성 (연관관계 포함)
     */
    // @Transactional
    // public TemplateDto.Response create(TemplateDto.PostRequest request) {
    //     // 1. 연관 Entity 조회
    //     User user = userService.getById(request.userId());
    //
    //     // 2. Entity 생성
    //     Template template = new Template(request.name(), user);
    //
    //     // 3. 저장
    //     Template savedTemplate = templateService.save(template);
    //
    //     // 4. DTO 변환 및 반환
    //     return templateMapper.toResponse(savedTemplate);
    // }

    // ===== 수정 =====

    /**
     * 수정
     */
    @Transactional
    public TemplateDto.Response update(String id, TemplateDto.PatchRequest request) {
        // 1. 조회
        Template template = templateService.getById(id);

        // 2. 수정
        Template updatedTemplate = templateService.update(
                template,
                request.name(),
                request.description()
        );

        // 3. DTO 변환 및 반환
        return templateMapper.toResponse(updatedTemplate);
    }

    /**
     * 복잡한 수정 (여러 Service 조합)
     */
    // @Transactional
    // public TemplateDto.Response update(String id, TemplateDto.PatchRequest request) {
    //     Template template = templateService.getById(id);
    //
    //     // 1. 기본 정보 수정 (값이 있는 경우에만)
    //     if (StringUtils.hasText(request.name())) {
    //         templateService.updateName(template, request.name());
    //     }
    //
    //     // 2. 연관 Entity 수정 (null이 아닌 경우에만)
    //     if (Objects.nonNull(request.child())) {
    //         Child child = childService.getById(template.getChild().getId());
    //         childService.update(child, request.child().name());
    //     }
    //
    //     // 3. 이미지 처리 (전달된 경우에만)
    //     if (Objects.nonNull(request.imageList())) {
    //         template.clearImageList();
    //         for (ImageDto.PostRequest imageRequest : request.imageList()) {
    //             FileAttachment file = fileAttachmentService.getById(imageRequest.fileAttachmentId());
    //             template.addImage(new TemplateImage(template, file));
    //         }
    //     }
    //
    //     return templateMapper.toResponse(template);
    // }

    // ===== 삭제 =====

    /**
     * 삭제
     */
    @Transactional
    public void delete(String id) {
        Template template = templateService.getById(id);
        templateService.delete(template);
    }

    // ===== 외부 서비스 연동 예시 =====

    /**
     * 외부 검증 후 처리
     */
    // @Transactional
    // public TemplateDto.Response createWithVerification(TemplateDto.PostRequest request) {
    //     // 1. 외부 서비스 검증
    //     smsVerificationService.validateAndMarkAsUsed(request.smsVerificationId());
    //
    //     // 2. Entity 생성
    //     Template template = new Template(request.name(), request.description());
    //
    //     // 3. 저장
    //     return templateMapper.toResponse(templateService.save(template));
    // }

    // ===== 권한 검사 예시 =====

    /**
     * 권한 검사 후 조회
     */
    // @Transactional(readOnly = true)
    // public TemplateDto.Response getByIdWithPermission(String id, String currentUserId) {
    //     Template template = templateService.getById(id);
    //
    //     // 권한 검사
    //     if (!template.getUser().getId().equals(currentUserId)) {
    //         throw new ApiException(ErrorCode.AUTHENTICATION_INVALID);
    //     }
    //
    //     return templateMapper.toResponse(template);
    // }
}
