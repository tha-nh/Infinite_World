package com.infinite.user.controller.rest;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.UserRequest;
import com.infinite.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "v1/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "user", description = "User")
public class UserController {
    UserService userService;

    @PostMapping("/search")
    public ApiResponse<Object> search(@RequestBody SearchRequest request, Pageable pageable){
        return userService.searchUsers(request, pageable);
    }

    @PostMapping("/create")
    public ApiResponse<Object> create(@RequestBody UserRequest request){
        return userService.create(request);
    }

    @PostMapping("/update")
    public ApiResponse<Object> update(@RequestBody UserRequest request){
        return userService.update(request);
    }

}
