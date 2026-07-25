package com.pickupdrop.common;

import org.modelmapper.ModelMapper;

/**
 * Entity ↔ DTO conversion base. Every domain Mapper extends this;
 * TypeMaps are pre-registered so mapping errors surface at startup.
 */
public abstract class BaseMapper<E, D> {

    protected final ModelMapper modelMapper;
    private final Class<E> entityClass;

    protected BaseMapper(ModelMapper modelMapper, Class<E> entityClass) {
        this.modelMapper = modelMapper;
        this.entityClass = entityClass;
    }

    protected <T> void registerDtoMapping(Class<T> dtoClass) {
        if (modelMapper.getTypeMap(entityClass, dtoClass) == null) {
            modelMapper.createTypeMap(entityClass, dtoClass);
        }
    }

    protected <T> T toDto(E entity, Class<T> dtoClass) {
        return modelMapper.map(entity, dtoClass);
    }
}
