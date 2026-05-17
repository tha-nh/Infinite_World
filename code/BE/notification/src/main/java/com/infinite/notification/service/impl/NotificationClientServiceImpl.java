package com.infinite.notification.service.impl;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.PageResponse;
import com.infinite.common.exception.BadRequestException;
import com.infinite.common.util.I18n;
import com.infinite.notification.application.command.ClaimNotificationCommand;
import com.infinite.notification.application.command.MarkNotificationAsReadCommand;
import com.infinite.notification.application.query.GetUserInboxQuery;
import com.infinite.notification.dto.response.UserNotificationDto;
import com.infinite.notification.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClientServiceImpl implements NotificationClientService {

    private final GetUserInboxQuery getUserInboxQuery;
    private final MarkNotificationAsReadCommand markNotificationAsReadCommand;
    private final ClaimNotificationCommand claimNotificationCommand;

    @Override
    public ApiResponse<Object> getInbox(String userIdHeader, int page, int size) {
        Long userId = parseUserId(userIdHeader);
        log.info("Get inbox for user: userId={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<UserNotificationDto> result = getUserInboxQuery.execute(userId, pageable);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.inbox.retrieved"))
                .result(PageResponse.success(result))
                .build();
    }

    @Override
    public ApiResponse<Object> getUnreadCount(String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        log.info("Get unread count for user: userId={}", userId);

        long count = getUserInboxQuery.getUnreadCount(userId);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.unread.count"))
                .result(Map.of("unreadCount", count))
                .build();
    }

    @Override
    public ApiResponse<Object> markAsRead(String userIdHeader, Long id) {
        Long userId = parseUserId(userIdHeader);
        log.info("Mark notification as read: userId={}, notificationId={}", userId, id);

        markNotificationAsReadCommand.execute(userId, id);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.mark.read.success"))
                .build();
    }

    @Override
    public ApiResponse<Object> markAllAsRead(String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        log.info("Mark all notifications as read: userId={}", userId);

        int count = markNotificationAsReadCommand.executeAll(userId);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.mark.all.read.success"))
                .result(Map.of("markedCount", count))
                .build();
    }

    @Override
    public ApiResponse<Object> delete(String userIdHeader, Long id) {
        Long userId = parseUserId(userIdHeader);
        log.info("Delete notification: userId={}, notificationId={}", userId, id);

        markNotificationAsReadCommand.delete(userId, id);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.delete.success"))
                .build();
    }

    @Override
    public ApiResponse<Object> claim(String userIdHeader, Long id) {
        Long userId = parseUserId(userIdHeader);
        log.info("Claim reward: userId={}, notificationId={}", userId, id);

        Map<String, Object> result = claimNotificationCommand.execute(userId, id);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.claim.success"))
                .result(result)
                .build();
    }

    private Long parseUserId(String userIdHeader) {
        try {
            return Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(I18n.msg("notification.user.invalid"));
        }
    }
}
