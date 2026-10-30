package com.histar.be.billing.controller;

import com.histar.be.billing.dto.*;
import com.histar.be.billing.service.PaymentHistoryService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing/history")
@RequiredArgsConstructor
public class PaymentHistoryController {
    private final PaymentHistoryService history;
    private final CurrentUserAccessor currentUser;

    @GetMapping
    public ApiResponse<PaymentHistoryPage> list(@RequestParam(defaultValue="ALL") String type,
            @RequestParam(defaultValue="ALL") String status, @RequestParam(defaultValue="") String q,
            @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        return ApiResponse.ok(history.list(user(),type,status,q,from,to,page,size));
    }
    @GetMapping("/{kind}/{id}")
    public ApiResponse<PaymentHistoryItem> detail(@PathVariable String kind,@PathVariable UUID id) {
        return ApiResponse.ok(history.detail(user(),kind,id));
    }
    @PostMapping("/{kind}/{id}/refresh")
    public ApiResponse<PaymentHistoryItem> refresh(@PathVariable String kind,@PathVariable UUID id) {
        return ApiResponse.ok(history.refresh(user(),kind,id));
    }
    private UUID user(){return currentUser.getUserId().orElseThrow(()->new AuthException("Unauthorized"));}
}
