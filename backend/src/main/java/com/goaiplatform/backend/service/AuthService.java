package com.goaiplatform.backend.service;

import com.goaiplatform.backend.domain.UserEntity;
import com.goaiplatform.backend.dto.AuthDtos;
import com.goaiplatform.backend.repository.UserRepository;
import com.goaiplatform.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<AuthDtos.AuthResponse> register(AuthDtos.RegisterRequest request) {
        log.info("User registration attempt: username={}", request.username());
        return userRepository.findByUsername(request.username())
                .flatMap(existing -> {
                    log.warn("Registration failed: username already exists: {}", request.username());
                    return Mono.<AuthDtos.AuthResponse>error(new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在"));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity user = new UserEntity(
                            null,
                            request.username(),
                            passwordEncoder.encode(request.password()),
                            "USER",
                            LocalDateTime.now()
                    );
                    return userRepository.save(user)
                            .map(saved -> {
                                log.info("User registered successfully: username={}, id={}", saved.username(), saved.id());
                                return new AuthDtos.AuthResponse(
                                        jwtService.generateToken(saved.id(), saved.username(), saved.role()),
                                        saved.username(),
                                        saved.role()
                                );
                            });
                }));
    }

    public Mono<AuthDtos.AuthResponse> login(AuthDtos.LoginRequest request) {
        log.info("User login attempt: username={}", request.username());
        return userRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                        log.warn("Login failed: invalid password for username={}", request.username());
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
                    }
                    log.info("User login successful: username={}, id={}", user.username(), user.id());
                    return Mono.just(new AuthDtos.AuthResponse(
                            jwtService.generateToken(user.id(), user.username(), user.role()),
                            user.username(),
                            user.role()
                    ));
                });
    }
}
