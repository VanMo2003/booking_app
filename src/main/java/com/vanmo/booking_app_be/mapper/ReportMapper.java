package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ReportResponse;
import com.vanmo.booking_app_be.entity.Report;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReportMapper {

     Report toEntity(ReportCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
     void updateEntity(@MappingTarget Report entity, ReportUpdateRequest req);

    ReportResponse toResponse(Report entity);
}
