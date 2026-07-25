package com.restaurant.api.conventions.templates;

import com.example.common.BaseMapper;
import com.example.dto.TemplateDto;
import com.example.entity.Template;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Mapper 템플릿
 *
 * 역할:
 * - Entity ↔ DTO 변환
 * - BaseMapper 상속으로 ModelMapper 기반 자동 매핑
 * - 연관 Entity는 커스텀 매핑으로 처리
 *
 * 사용법:
 * 1. Template을 도메인명으로 변경 (예: Product)
 * 2. 사용할 DTO 클래스를 registerDtoMapping()으로 등록
 * 3. 변환 메서드 정의 (toDto, toResponse, toSummaryResponse)
 * 4. 연관 Entity가 있는 경우 커스텀 매핑 추가
 */
@Component
public class MapperTemplate extends BaseMapper<Template, TemplateDto> {

    // ===== 연관 Entity Mapper (커스텀 매핑 시 사용) =====
    // private final UserMapper userMapper;

    // ===== 기본 생성자 =====
    protected MapperTemplate(ModelMapper modelMapper) {
        super(modelMapper, Template.class);
        this.registerDtoMapping(TemplateDto.class);
        this.registerDtoMapping(TemplateDto.Response.class);
        this.registerDtoMapping(TemplateDto.SummaryResponse.class);
    }

    // 연관 Entity가 있는 경우의 생성자 예시
    // protected MapperTemplate(ModelMapper modelMapper, UserMapper userMapper) {
    //     super(modelMapper, Template.class);
    //     this.userMapper = userMapper;
    //     this.registerDtoMapping(TemplateDto.class);
    //     this.registerDtoMapping(TemplateDto.Response.class);
    //     this.registerDtoMapping(TemplateDto.SummaryResponse.class);
    //
    //     // 연관 Entity 필드 자동 매핑 무시 (수동 처리)
    //     modelMapper.typeMap(Template.class, TemplateDto.Response.class)
    //             .addMappings(mapper -> mapper
    //                     .using(ctx -> null)
    //                     .map(Template::getUser, TemplateDto.Response::setUser));
    // }

    // ===== 변환 메서드 =====

    /**
     * Entity → 전체 DTO
     */
    public TemplateDto toDto(Template entity) {
        return super.toDto(entity, TemplateDto.class);
    }

    /**
     * Entity → 상세 응답 DTO
     */
    public TemplateDto.Response toResponse(Template entity) {
        return super.toDto(entity, TemplateDto.Response.class);
    }

    // 연관 Entity가 있는 경우의 toResponse 예시
    // public TemplateDto.Response toResponse(Template entity) {
    //     TemplateDto.Response response = super.toDto(entity, TemplateDto.Response.class);
    //     if (entity.getUser() != null) {
    //         response.setUser(userMapper.toSummaryResponse(entity.getUser()));
    //     }
    //     return response;
    // }

    /**
     * Entity → 목록 응답 DTO
     */
    public TemplateDto.SummaryResponse toSummaryResponse(Template entity) {
        return super.toDto(entity, TemplateDto.SummaryResponse.class);
    }
}
