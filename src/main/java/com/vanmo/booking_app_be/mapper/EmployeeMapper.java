package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.EmployeeCreateRequest;
import com.vanmo.booking_app_be.dto.request.EmployeeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.EmployeeResponse;
import com.vanmo.booking_app_be.entity.Employee;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    Employee toEntity(EmployeeCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Employee entity, EmployeeUpdateRequest req);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "position.name", target = "positionName")
    EmployeeResponse toResponse(Employee entity);
}
