package com.relab.budge_it.identity.controller;

import com.relab.budge_it.identity.dto.IdentityDtos.UserProfileResponse;
import com.relab.budge_it.identity.service.UserService;
import com.relab.budge_it.shared.response.ApiResponse;
import com.relab.budge_it.shared.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getProfile(userProvider.getCurrentUserId())));
    }
}