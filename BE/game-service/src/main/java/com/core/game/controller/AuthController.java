package com.core.game.controller;

import com.core.game.model.Player;
import com.core.game.repository.PlayerRepository;
import com.core.game.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("v1/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication APIs: register, login, logout")
public class AuthController {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Register a new player")
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Player player) {
        if (playerRepository.existsByUsername(player.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        player.setPassword(passwordEncoder.encode(player.getPassword()));
        playerRepository.save(player);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Registration successful");
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
    public Map<String, Object> logout() {
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Logout successful");
        return res;
    }
}