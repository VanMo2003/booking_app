package com.vanmo.booking_app_be.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.dto.request.ChatMessageRequest;
import com.vanmo.booking_app_be.dto.request.ConversationCreateRequest;
import com.vanmo.booking_app_be.dto.response.ChatMessageResponse;
import com.vanmo.booking_app_be.dto.response.ChatUnreadResponse;
import com.vanmo.booking_app_be.dto.response.ConversationResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.ChatMessage;
import com.vanmo.booking_app_be.entity.Conversation;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.event.ChatUpdatedEvent;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.ChatMapper;
import com.vanmo.booking_app_be.repository.ChatMessageRepository;
import com.vanmo.booking_app_be.repository.ConversationRepository;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.ChatService;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

/**
 * Guest ↔ branch chat. A conversation belongs to one customer and one branch; on the branch side the chain owner, the
 * branch manager and every staff member of that branch share it. Admins have no access. Every change publishes a
 * {@link ChatUpdatedEvent}, which {@code ChatEventListener} fans out over WebSocket after commit (and pushes to
 * whoever of the other side is offline).
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_PAGE_SIZE = 100;
    public static final String ASSISTANT_NAME = "Trợ lý AI";
    private static final Sort INBOX_ORDER =
            Sort.by(Sort.Direction.DESC, "lastMessageAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomerRepository customerRepository;
    private final HotelRepository hotelRepository;
    private final EmployeeRepository employeeRepository;
    private final HotelChainRepository hotelChainRepository;
    private final ChatMapper chatMapper;
    private final AccessGuard accessGuard;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ConversationResponse open(ConversationCreateRequest req) {
        Customer customer = requireCustomerProfile(accessGuard.currentAccount());
        Hotel hotel = hotelRepository
                .findById(req.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + req.getHotelId()));
        Conversation conversation = conversationRepository
                .findByCustomer_IdAndHotel_Id(customer.getId(), hotel.getId())
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder().customer(customer).hotel(hotel).build()));
        return chatMapper.forSide(conversation, ChatSide.CUSTOMER);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getMine(int page, int size) {
        Customer customer = requireCustomerProfile(accessGuard.currentAccount());
        Page<Conversation> conversations = conversationRepository.findAllByCustomer_IdAndLastMessageAtIsNotNull(
                customer.getId(), inboxPage(page, size));
        return PageResponse.from(conversations.map(c -> chatMapper.forSide(c, ChatSide.CUSTOMER)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getInbox(Long hotelId, int page, int size) {
        List<Long> hotelIds = servedHotelIds(hotelId);
        Pageable pageable = inboxPage(page, size);
        Page<Conversation> conversations = hotelIds.isEmpty()
                ? Page.empty(pageable)
                : conversationRepository.findAllByHotel_IdInAndLastMessageAtIsNotNull(hotelIds, pageable);
        return PageResponse.from(conversations.map(c -> chatMapper.forSide(c, ChatSide.HOTEL)));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getById(Long id) {
        Conversation conversation = findConversation(id);
        return chatMapper.forSide(conversation, sideOf(accessGuard.currentAccount(), conversation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long conversationId, Long beforeId, int size) {
        Conversation conversation = findConversation(conversationId);
        sideOf(accessGuard.currentAccount(), conversation);
        Pageable limit = PageRequest.of(0, Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
        List<ChatMessage> messages = beforeId == null
                ? chatMessageRepository.findByConversation_IdOrderByIdDesc(conversationId, limit)
                : chatMessageRepository.findByConversation_IdAndIdLessThanOrderByIdDesc(
                        conversationId, beforeId, limit);
        return messages.stream().map(chatMapper::toMessageResponse).toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse send(Long conversationId, ChatMessageRequest req) {
        Account caller = accessGuard.currentAccount();
        Conversation conversation = findConversation(conversationId);
        ChatSide side = sideOf(caller, conversation);
        String content = req.getContent().strip();
        if (content.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "content: must not be blank");
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .senderSide(side)
                .senderAccountId(caller.getId())
                .senderName(senderName(caller, conversation))
                .senderRole(caller.getRole())
                .content(content)
                .build());

        conversation.setLastMessage(NotificationMessages.preview(content));
        conversation.setLastSenderSide(side);
        conversation.setLastMessageAt(message.getOnCreate() == null ? new Date() : message.getOnCreate());
        // Writing implies you've read the thread; the other side gets one more unread.
        if (side == ChatSide.CUSTOMER) {
            conversation.setCustomerUnread(0);
            conversation.setHotelUnread(conversation.getHotelUnread() + 1);
        } else {
            conversation.setHotelUnread(0);
            conversation.setCustomerUnread(conversation.getCustomerUnread() + 1);
            // A person on the team took over from the assistant.
            conversation.setNeedsStaff(null);
            conversation.setHandoffReason(null);
        }
        conversationRepository.save(conversation);

        ChatMessageResponse response = chatMapper.toMessageResponse(message);
        eventPublisher.publishEvent(event(ChatUpdatedEvent.Type.MESSAGE, conversation, response, caller.getId()));
        return response;
    }

    @Override
    @Transactional
    public ChatMessageResponse postAssistantReply(
            Long conversationId, Long replyToMessageId, String content, boolean handoff, String handoffReason) {
        Conversation conversation = findConversation(conversationId);
        // Someone answered (or the guest wrote again) while the model was thinking: that newer turn wins.
        List<ChatMessage> latest =
                chatMessageRepository.findByConversation_IdOrderByIdDesc(conversationId, PageRequest.of(0, 1));
        if (latest.isEmpty() || !latest.getFirst().getId().equals(replyToMessageId)) {
            return null;
        }
        String text = content.strip();
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .senderSide(ChatSide.HOTEL)
                .senderName(ASSISTANT_NAME)
                .aiGenerated(true)
                .content(text)
                .build());

        conversation.setLastMessage(NotificationMessages.preview(text));
        conversation.setLastSenderSide(ChatSide.HOTEL);
        conversation.setLastMessageAt(message.getOnCreate() == null ? new Date() : message.getOnCreate());
        // The guest has a new answer; the team still has the guest's message to look at.
        conversation.setCustomerUnread(conversation.getCustomerUnread() + 1);
        if (handoff) {
            conversation.setNeedsStaff(true);
            conversation.setHandoffReason(handoffReason == null ? null : truncate(handoffReason.strip(), 255));
        }
        conversationRepository.save(conversation);

        ChatMessageResponse response = chatMapper.toMessageResponse(message);
        eventPublisher.publishEvent(event(ChatUpdatedEvent.Type.MESSAGE, conversation, response, null));
        return response;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    @Override
    @Transactional
    public ConversationResponse markRead(Long conversationId) {
        Account caller = accessGuard.currentAccount();
        Conversation conversation = findConversation(conversationId);
        ChatSide side = sideOf(caller, conversation);
        boolean changed =
                side == ChatSide.CUSTOMER ? conversation.getCustomerUnread() > 0 : conversation.getHotelUnread() > 0;
        if (changed) {
            if (side == ChatSide.CUSTOMER) {
                conversation.setCustomerUnread(0);
            } else {
                conversation.setHotelUnread(0);
            }
            conversationRepository.save(conversation);
            eventPublisher.publishEvent(event(ChatUpdatedEvent.Type.READ, conversation, null, caller.getId()));
        }
        return chatMapper.forSide(conversation, side);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatUnreadResponse countUnread(Long hotelId) {
        Account caller = accessGuard.currentAccount();
        long unread;
        if (caller.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findByAccount_Id(caller.getId());
            unread = customer == null ? 0 : conversationRepository.sumCustomerUnread(customer.getId());
        } else {
            List<Long> hotelIds = servedHotelIds(hotelId);
            unread = hotelIds.isEmpty() ? 0 : conversationRepository.sumHotelUnread(hotelIds);
        }
        return ChatUnreadResponse.builder().unread(unread).build();
    }

    private Customer requireCustomerProfile(Account caller) {
        if (caller.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        Customer customer = customerRepository.findByAccount_Id(caller.getId());
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_PROFILE_REQUIRED);
        }
        return customer;
    }

    private List<Long> servedHotelIds(Long hotelId) {
        if (hotelId != null) {
            return List.of(accessGuard.requireBranchTeam(hotelId).getId());
        }
        return accessGuard.branchIdsServedBy(accessGuard.currentAccount());
    }

    /** Which side the caller speaks for in this conversation; anyone else (admins included) is refused. */
    private ChatSide sideOf(Account caller, Conversation conversation) {
        if (caller.getRole() == Role.CUSTOMER) {
            Account owner = conversation.getCustomer().getAccount();
            if (owner == null || !owner.getId().equals(caller.getId())) {
                throw new AppException(ErrorCode.UN_AUTHORIZED);
            }
            return ChatSide.CUSTOMER;
        }
        accessGuard.requireBranchTeam(conversation.getHotel().getId());
        return ChatSide.HOTEL;
    }

    private String senderName(Account caller, Conversation conversation) {
        String name =
                switch (caller.getRole()) {
                    case CUSTOMER -> conversation.getCustomer().getFullName();
                    case STAFF -> {
                        Employee employee = employeeRepository.findByAccount_Id(caller.getId());
                        yield employee == null ? null : employee.getFullName();
                    }
                    case HOTEL_OWNER -> {
                        HotelChain chain = hotelChainRepository.findByAccount_Id(caller.getId());
                        yield chain == null ? null : chain.getOwnerName();
                    }
                    default -> null;
                };
        return name == null || name.isBlank() ? caller.getUsername() : name;
    }

    /** Owner of the chain, manager of the branch and its staff - everyone who answers this branch's chat. */
    private List<String> teamAccountIds(Hotel hotel) {
        return accessGuard.branchTeam(hotel).stream().map(Account::getId).toList();
    }

    /** @param senderAccountId null for the assistant */
    private ChatUpdatedEvent event(
            ChatUpdatedEvent.Type type,
            Conversation conversation,
            ChatMessageResponse message,
            String senderAccountId) {
        Account customerAccount = conversation.getCustomer().getAccount();
        return new ChatUpdatedEvent(
                type,
                message,
                chatMapper.forSide(conversation, ChatSide.CUSTOMER),
                chatMapper.forSide(conversation, ChatSide.HOTEL),
                customerAccount == null ? null : customerAccount.getId(),
                teamAccountIds(conversation.getHotel()),
                senderAccountId);
    }

    private Conversation findConversation(Long id) {
        return conversationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found id=" + id));
    }

    private static Pageable inboxPage(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)), INBOX_ORDER);
    }
}
