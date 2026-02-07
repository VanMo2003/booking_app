package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.CustomerCreateRequest;
import com.vanmo.booking_app_be.dto.request.CustomerUpdateRequest;
import com.vanmo.booking_app_be.dto.response.CustomerResponse;
import com.vanmo.booking_app_be.entity.Customer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "account", ignore = true)
    Customer toEntity(CustomerCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "account", ignore = true)
    void updateEntity(@MappingTarget Customer entity, CustomerUpdateRequest req);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.username", target = "username")
    CustomerResponse toResponse(Customer entity);
}
