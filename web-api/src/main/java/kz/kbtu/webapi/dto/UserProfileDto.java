package kz.kbtu.webapi.dto;

import kz.kbtu.common.entity.Role;
import kz.kbtu.common.entity.User;

import java.time.LocalDateTime;

public record UserProfileDto(
    Long id,
    String username,
    String firstName,
    String lastName,
    String avatarUrl,
    Role role,
    LocalDateTime createdAt
) {
    public static UserProfileDto from(User user) {
        return new UserProfileDto(
            user.getId(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getAvatarUrl(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
