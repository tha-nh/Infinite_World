package com.infinite.user.controller.rest;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.LoginRequest;
import com.infinite.user.dto.request.UserRequest;
import com.infinite.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "v1/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "authen", description = "Chức năng login, logout")
public class AuthController {
    UserService userService;

    @PostMapping("/login")
    public ApiResponse<Object> login(@RequestBody LoginRequest request){
        return userService.login(request);
    }

    @PostMapping("/register")
    public ApiResponse<Object> register(@RequestBody UserRequest request){
        return userService.create(request);
    }

    @GetMapping("/get-token")
    public ApiResponse<Object> getToken(){
        return userService.getToken();
    }
}
