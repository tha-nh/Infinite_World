package com.infinite.user.controller;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.util.MessageUtils;
import com.infinite.user.model.Player;
import com.infinite.user.repository.PlayerRepository;
import com.infinite.user.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.infinite.common.constant.StatusCode.*;
import static com.infinite.common.dto.response.Response.*;

@RestController
@RequestMapping("v1/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Auth", description = "Authentication APIs: register, login, logout")
public class AuthController {

    PlayerRepository playerRepository;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;
    Logger log = LoggerFactory.getLogger(AuthController.class);
    MessageUtils messageUtils;

    @Operation(summary = "Register a new player")
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Player player) {
        log.info(">>> inside controller");
        player.setPassword(passwordEncoder.encode(player.getPassword()));
        playerRepository.save(player);
        Map<String, Object> res = new HashMap<>();
        res.put("message", messageUtils.getMessage("auth.register.success"));
        return res;
    }

    @Operation(summary = "Login and get JWT token")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Player player) {
        Player dbPlayer = playerRepository.findByUsername(player.getUsername())
                .orElseThrow(() -> new RuntimeException("Username not found"));
        if (!passwordEncoder.matches(player.getPassword(), dbPlayer.getPassword())) {
            throw new RuntimeException("Wrong password");
        }
        String token = jwtUtil.generateToken(dbPlayer.getUsername());
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        return res;
    }

    @Operation(summary = "Logout (FE discards JWT)")
    @PostMapping("/logout")
    public ApiResponse<Object> logout() {

        log.info(">>> inside controller");

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .build();

    }
}