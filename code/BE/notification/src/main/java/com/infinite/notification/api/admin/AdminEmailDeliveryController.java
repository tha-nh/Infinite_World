package com.infinite.notification.api.admin;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.notification.service.EmailDeliveryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/email-deliveries")
@RequiredArgsConstructor
public class AdminEmailDeliveryController {

    private final EmailDeliveryAdminService emailDeliveryAdminService;

    @GetMapping
    public ApiResponse<Object> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return emailDeliveryAdminService.search(page, size, status);
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> findById(@PathVariable Long id) {
        return emailDeliveryAdminService.findById(id);
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Object> retry(@PathVariable Long id) {
        return emailDeliveryAdminService.retry(id);
    }
}
