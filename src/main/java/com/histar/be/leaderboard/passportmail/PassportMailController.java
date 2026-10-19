package com.histar.be.leaderboard.passportmail;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

@RestController
@RequestMapping("/api/leaderboard/passport/email")
public class PassportMailController {
    private final PassportMailService service;
    private final CurrentUserAccessor currentUser;
    public PassportMailController(PassportMailService service,CurrentUserAccessor currentUser){this.service=service;this.currentUser=currentUser;}
    @GetMapping("/status")
    public ApiResponse<PassportMailService.Status> status(HttpServletResponse response){
        noStore(response);return ApiResponse.ok(service.status(userId()));
    }
    @PostMapping
    public ApiResponse<PassportMailService.Accepted> send(@RequestParam(required=false) MultipartFile image,
            @RequestParam(required=false) String requestId,HttpServletRequest request,HttpServletResponse response) {
        noStore(response);UUID userId=userId();
        if(request.getParameterMap().keySet().stream().anyMatch(key -> !"requestId".equals(key))
                || (request instanceof MultipartHttpServletRequest multipart && (multipart.getMultiFileMap().keySet().stream().anyMatch(key -> !"image".equals(key)) || multipart.getFiles("image").size()!=1)))
            throw new PassportMailException(422,"INVALID_REQUEST","Yêu cầu chỉ nhận ảnh hộ chiếu và mã định danh.");
        UUID key;
        try {key=UUID.fromString(requestId);if(!key.toString().equalsIgnoreCase(requestId)) throw new IllegalArgumentException();}
        catch(RuntimeException ex){throw new PassportMailException(422,"INVALID_REQUEST_ID","Yêu cầu gửi cần mã định danh hợp lệ.");}
        return ApiResponse.ok(service.send(userId,image,key));
    }
    private UUID userId(){return currentUser.getUserId().orElseThrow(()->new PassportMailException(401,"AUTH_REQUIRED","Vui lòng đăng nhập để gửi hộ chiếu qua email."));}
    @ExceptionHandler(PassportMailException.class)
    public ResponseEntity<Map<String,Object>> error(PassportMailException error){
        Map<String,Object> body=new LinkedHashMap<>();body.put("success",false);body.put("code",error.getCode());body.put("message",error.getMessage());
        var headers=new HttpHeaders();headers.setCacheControl("private, no-store");
        if(error.getRetryAfterSeconds()!=null){body.put("retryAfterSeconds",error.getRetryAfterSeconds());headers.set("Retry-After",error.getRetryAfterSeconds().toString());}
        return new ResponseEntity<>(body,headers,HttpStatusCode.valueOf(error.getStatus()));
    }
    @ExceptionHandler({MultipartException.class,org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<Map<String,Object>> invalid(Exception ignored){return error(new PassportMailException(422,"INVALID_REQUEST","Vui lòng gửi ảnh PNG cùng mã yêu cầu hợp lệ."));}
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String,Object>> unavailable(Exception ignored){return error(new PassportMailException(503,"MAIL_UNAVAILABLE","Tính năng gửi hộ chiếu qua email tạm thời không khả dụng."));}
    private void noStore(HttpServletResponse response){response.setHeader("Cache-Control","private, no-store");}
}



