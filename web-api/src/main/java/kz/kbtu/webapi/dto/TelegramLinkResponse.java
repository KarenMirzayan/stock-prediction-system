package kz.kbtu.webapi.dto;

import java.time.LocalDateTime;

public record TelegramLinkResponse(String linkUrl, LocalDateTime expiresAt) {}
