package com.histar.be.ar;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ar/cards/email")
public class ArCardMailController {
    private final ArCardMailService service;
    private final CurrentUserAccessor user;
    public ArCardMailController(ArCardMailService service, CurrentUserAccessor user) { this.service=service;this.user=user; }
    private UUID userId() { return user.getUserId().orElseThrow(()->new ArCardMailException(401,"AUTH_REQUIRED","Vui lòng đăng nhập để nhận ảnh AR.")); }
    @GetMapping("/status")
    public ApiResponse<ArCardMailService.Status> status(HttpServletResponse response) {
        response.setHeader("Cache-Control","private, no-store");return ApiResponse.ok(service.status(userId()));
    }
    @PostMapping
    public ApiResponse<ArCardMailService.Accepted> send(@RequestBody Map<String,Object> body, HttpServletResponse response) {
        response.setHeader("Cache-Control","private, no-store");var id=userId();UUID requestId;String artifactId;
        try {
            if(!body.keySet().equals(Set.of("requestId","artifactId")) || !(body.get("requestId") instanceof String value) || !(body.get("artifactId") instanceof String selected)) throw new IllegalArgumentException();
            artifactId=selected;
            requestId=UUID.fromString(value);if(!requestId.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
        } catch(RuntimeException error) { throw new ArCardMailException(422,"INVALID_REQUEST","Chỉ nhận mã yêu cầu và mã hiện vật hợp lệ; email người nhận do máy chủ xác định."); }
        return ApiResponse.ok(service.send(id,artifactId,requestId));
    }
    @ExceptionHandler(ArCardMailException.class)
    public ResponseEntity<Map<String,Object>> error(ArCardMailException error) {
        var body=new LinkedHashMap<String,Object>();body.put("success",false);body.put("code",error.code());body.put("message",error.getMessage());
        var headers=new HttpHeaders();headers.setCacheControl("private, no-store");
        if(error.retryAfterSeconds()!=null) { headers.set("Retry-After",error.retryAfterSeconds().toString());body.put("retryAfterSeconds",error.retryAfterSeconds()); }
        return new ResponseEntity<>(body,headers,HttpStatusCode.valueOf(error.status()));
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String,Object>> databaseUnavailable() { return error(new ArCardMailException(503,"MAIL_UNAVAILABLE","Gửi ảnh AR qua email tạm thời chưa sẵn sàng.")); }
}
