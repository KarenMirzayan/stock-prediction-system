package kz.kbtu.webapi.dto;

public record TelegramCallbackRequest(String token, Long chatId, String telegramUsername) {}
