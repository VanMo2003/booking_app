package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.PayrollCreateRequest;
import com.vanmo.booking_app_be.dto.request.PayrollUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PayrollResponse;
import com.vanmo.booking_app_be.entity.Payroll;

@Mapper(componentModel = "spring")
public interface PayrollMapper {

    @Mapping(target = "employee", ignore = true)
    Payroll toEntity(PayrollCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Payroll entity, PayrollUpdateRequest req);

    PayrollResponse toResponse(Payroll entity);
}
