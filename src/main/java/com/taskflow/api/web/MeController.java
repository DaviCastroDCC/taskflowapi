package com.taskflow.api.web;

import com.taskflow.api.dto.response.UserResponse;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.UserRepository;
import com.taskflow.api.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/v1/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal caller) {
        return userRepository
                .findById(caller.getId())
                .map(UserResponse::of)
                .orElseThrow(() -> ResourceNotFoundException.of("User", caller.getId()));
    }
}
