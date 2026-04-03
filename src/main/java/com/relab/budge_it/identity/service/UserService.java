package com.relab.budge_it.identity.service;

import com.relab.budge_it.identity.domain.User;
import com.relab.budge_it.identity.dto.IdentityDtos.UserProfileResponse;
import com.relab.budge_it.identity.repository.UserRepository;
import com.relab.budge_it.shared.response.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User"));
        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAccountNumber(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}