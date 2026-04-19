package com.infinite.user.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRequest {
    Long id;
    String username;
    String password;
    String name;
    String email;
    String imageUrl;
    String nguoithuchien;
    List<Long> roleIds;
    Integer active;
}
