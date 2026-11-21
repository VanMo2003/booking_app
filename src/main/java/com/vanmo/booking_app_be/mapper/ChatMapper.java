package com.vanmo.booking_app_be.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vanmo.booking_app_be.dto.response.ChatMessageResponse;
import com.vanmo.booking_app_be.dto.response.ConversationResponse;
import com.vanmo.booking_app_be.entity.ChatMessage;
import com.vanmo.booking_app_be.entity.Conversation;
import com.vanmo.booking_app_be.entity.enums.ChatSide;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(source = "conversation.id", target = "conversationId")
    ChatMessageResponse toMessageResponse(ChatMessage message);

    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(source = "hotel.name", target = "hotelName")
    @Mapping(source = "hotel.address", target = "hotelAddress")
    @Mapping(source = "hotel.phone", target = "hotelPhone")
    @Mapping(source = "hotel.pathImage", target = "hotelPathImage")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.fullName", target = "customerName")
    @Mapping(source = "customer.phoneNumber", target = "customerPhone")
    @Mapping(source = "customer.pathImage", target = "customerPathImage")
    @Mapping(target = "mySide", ignore = true)
    @Mapping(target = "unread", ignore = true)
    ConversationResponse toConversationResponse(Conversation conversation);

    /** The conversation as {@code side} sees it - its own unread counter; the hand-off note stays internal. */
    default ConversationResponse forSide(Conversation conversation, ChatSide side) {
        boolean customer = side == ChatSide.CUSTOMER;
        return toConversationResponse(conversation).toBuilder()
                .mySide(side)
                .unread(customer ? conversation.getCustomerUnread() : conversation.getHotelUnread())
                .needsStaff(customer ? null : conversation.getNeedsStaff())
                .handoffReason(customer ? null : conversation.getHandoffReason())
                .build();
    }
}
