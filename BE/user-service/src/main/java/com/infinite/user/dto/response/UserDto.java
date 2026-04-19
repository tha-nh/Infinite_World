package com.infinite.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phoneNumber;
    private String imageUrl;
    private Integer active;
    private List<String> roles;
    
    // Constructor for JPA query (without roles)
    public UserDto(Long id, String username, String name, String email, String phoneNumber, String imageUrl, Integer active) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
        this.active = active;
    }
}
