package com.restaurant.api.conventions.templates;

import com.example.dto.TemplateDto;
import com.example.service.FacadeTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controller 템플릿
 *
 * 역할:
 * - HTTP 요청/응답 처리
 * - 파라미터 바인딩 및 Validation
 * - Facade만 의존
 *
 * 사용법:
 * 1. Template을 도메인명으로 변경 (예: Product)
 * 2. URL 경로를 리소스명으로 변경 (예: /v1/products)
 * 3. API 문서화 주석 추가
 */
@RestController
@RequestMapping("/v1/templates")  // TODO: 리소스명 변경
@RequiredArgsConstructor
public class ControllerTemplate {

    private final FacadeTemplate templateFacade;

    // ===== 검색 =====

    /**
     * 템플릿 검색 #{이슈번호}
     */
    @GetMapping("/search")
    public Page<TemplateDto.SummaryResponse> search(
            @ModelAttribute TemplateDto.SearchRequest searchRequest,
            Pageable pageable) {
        return templateFacade.search(searchRequest, pageable);
    }

    // ===== 단건 조회 =====

    /**
     * 템플릿 조회 #{이슈번호}
     */
    @GetMapping("/{templateId}")
    public TemplateDto.Response getById(@PathVariable String templateId) {
        return templateFacade.getById(templateId);
    }

    // ===== 생성 =====

    /**
     * 템플릿 생성 #{이슈번호}
     */
    @PostMapping
    public TemplateDto.Response create(
            @RequestBody @Valid TemplateDto.PostRequest request) {
        return templateFacade.create(request);
    }

    // ===== 수정 =====

    /**
     * 템플릿 수정 #{이슈번호}
     */
    @PatchMapping("/{templateId}")
    public TemplateDto.Response update(
            @PathVariable String templateId,
            @RequestBody @Valid TemplateDto.PatchRequest request) {
        return templateFacade.update(templateId, request);
    }

    // ===== 삭제 =====

    /**
     * 템플릿 삭제 #{이슈번호}
     */
    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String templateId) {
        templateFacade.delete(templateId);
    }

    // ===== 추가 엔드포인트 예시 =====

    // 현재 사용자 정보 조회
    // /**
    //  * 내 정보 조회 #{이슈번호}
    //  */
    // @GetMapping("/me")
    // public TemplateDto.Response getMyInfo() {
    //     return templateFacade.getMyInfo();
    // }

    // 특정 필드 수정
    // /**
    //  * 비밀번호 변경 #{이슈번호}
    //  */
    // @PatchMapping("/{templateId}/password")
    // public TemplateDto.Response changePassword(
    //         @PathVariable String templateId,
    //         @RequestBody @Valid TemplateDto.PasswordPatchRequest request) {
    //     return templateFacade.updatePassword(templateId, request);
    // }

    // 하위 리소스 조회
    // /**
    //  * 템플릿의 아이템 목록 조회 #{이슈번호}
    //  */
    // @GetMapping("/{templateId}/items")
    // public Page<ItemDto.SummaryResponse> getItems(
    //         @PathVariable String templateId,
    //         Pageable pageable) {
    //     return templateFacade.getItems(templateId, pageable);
    // }

    // 복합 요청 (쿼리 파라미터로 DTO 바인딩)
    // /**
    //  * 템플릿 사용자 조회 #{이슈번호}
    //  */
    // @GetMapping("/users")
    // public TemplateDto.Response getByUser(
    //         @Valid @ModelAttribute TemplateDto.UserRequest request) {
    //     return templateFacade.getByUser(request);
    // }
}
