package com.restaurant.api.conventions.templates;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO 템플릿
 *
 * 사용법:
 * 1. 클래스명을 {Domain}Dto로 변경 (예: ProductDto)
 * 2. 각 Inner Class의 필드를 도메인에 맞게 정의
 */
public class DtoTemplate {

    // ===== 생성 요청 =====
    public record PostRequest(
            @NotBlank String name,          // 필수 문자열
            String description              // 선택적 필드
            // @NotNull LocalDate date,     // 필수 날짜
            // @NotNull @Valid ChildDto.PostRequest child  // 중첩 DTO
    ) {}

    // ===== 수정 요청 =====
    public record PatchRequest(
            String name,                    // null이면 수정하지 않음
            String description
            // LocalDate date,
            // Status status
    ) {}

    // ===== 상세 응답 =====
    @Getter
    @NoArgsConstructor
    public static class Response {
        // 1. 식별자
        private String id;

        // 2. 핵심 비즈니스 필드
        private String name;
        private String description;

        // 3. 연관 Entity (해당 DTO로)
        // private UserDto.Response user;
        // private List<ItemDto.Response> itemList;

        // 4. 시간 필드
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    // ===== 목록 응답 (요약) =====
    @Getter
    @NoArgsConstructor
    public static class SummaryResponse {
        private String id;
        private String name;
        private LocalDateTime createdAt;

        // Entity로부터 직접 생성하는 생성자 (선택적)
        // public SummaryResponse(Template template) {
        //     this.id = template.getId();
        //     this.name = template.getName();
        //     this.createdAt = template.getCreatedAt();
        // }
    }

    // ===== 검색 조건 =====
    public record SearchRequest(
            String name,                           // 단일 조건 (like 검색)
            // List<Status> statusList,            // 목록 조건 (IN)
            LocalDateTime minCreatedAt,            // 범위 조건 (시작)
            LocalDateTime maxCreatedAt             // 범위 조건 (끝)
    ) {}

    // ===== 특수 목적 DTO 예시 =====

    // 비밀번호 변경 전용
    // public record PasswordPatchRequest(
    //     @NotBlank String password
    // ) {}

    // 복합 요청 (다른 DTO 포함)
    // public record CompositePatchRequest(
    //     String name,
    //     ChildDto.PatchRequest child
    // ) {}

    // 통계/집계 응답
    // @Getter
    // @NoArgsConstructor
    // public static class CountResponse {
    //     private Long totalCount;
    //     private Long activeCount;
    //
    //     public CountResponse(Long totalCount, Long activeCount) {
    //         this.totalCount = totalCount;
    //         this.activeCount = activeCount;
    //     }
    // }
}
