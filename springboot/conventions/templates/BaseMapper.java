package com.restaurant.api.conventions.templates;

import org.modelmapper.ModelMapper;

/**
 * Entity ↔ DTO 변환을 위한 기본 Mapper 추상 클래스.
 *
 * 모든 도메인 Mapper가 이 클래스를 상속합니다.
 * ModelMapper를 감싸서 TypeMap 등록과 변환 메서드를 표준화합니다.
 *
 * 사용법:
 * 1. 프로젝트의 common/ 패키지에 이 파일을 복사
 * 2. 패키지명을 프로젝트에 맞게 변경
 * 3. 도메인 Mapper에서 extends BaseMapper<Entity, Dto>로 상속
 *
 * @param <E> Entity 타입
 * @param <D> DTO 타입
 */
public abstract class BaseMapper<E, D> {

    protected final ModelMapper modelMapper;
    private final Class<E> entityClass;

    protected BaseMapper(ModelMapper modelMapper, Class<E> entityClass) {
        this.modelMapper = modelMapper;
        this.entityClass = entityClass;
    }

    /**
     * DTO 클래스를 ModelMapper에 TypeMap으로 등록합니다.
     * 생성자에서 사용할 DTO 클래스마다 호출해야 합니다.
     *
     * TypeMap을 사전 등록하면:
     * - 애플리케이션 시작 시점에 매핑 오류를 조기 감지
     * - 커스텀 매핑 설정 (addMappings)의 기반이 됨
     *
     * @param dtoClass 등록할 DTO 클래스
     */
    protected <T> void registerDtoMapping(Class<T> dtoClass) {
        if (modelMapper.getTypeMap(entityClass, dtoClass) == null) {
            modelMapper.createTypeMap(entityClass, dtoClass);
        }
    }

    /**
     * Entity를 지정 DTO 클래스로 변환합니다.
     *
     * @param entity 변환할 Entity
     * @param dtoClass 대상 DTO 클래스
     * @return 변환된 DTO
     */
    protected <T> T toDto(E entity, Class<T> dtoClass) {
        return modelMapper.map(entity, dtoClass);
    }
}
