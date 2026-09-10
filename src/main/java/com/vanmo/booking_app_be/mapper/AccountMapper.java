package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.AccountCreateRequest;
import com.vanmo.booking_app_be.dto.request.AccountUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AccountResponse;
import com.vanmo.booking_app_be.entity.Account;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "onCreate", ignore = true)
    @Mapping(target = "onUpdate", ignore = true)
    Account toEntity(AccountCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "onCreate", ignore = true)
    @Mapping(target = "onUpdate", ignore = true)
    void updateEntity(@MappingTarget Account entity, AccountUpdateRequest req);

    AccountResponse toResponse(Account entity);
}
