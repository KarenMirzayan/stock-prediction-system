package kz.kbtu.webapi.controller;

import kz.kbtu.webapi.dto.TelegramCallbackRequest;
import kz.kbtu.webapi.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives a callback from the Telegram bot after the user sends /start <token>.
 * Secured by a shared secret passed in the X-Bot-Secret header — not by JWT,
 * because the caller is the bot server, not the user's browser.
 */
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramCallbackController {

    private final TelegramService telegramService;

    @Value("${telegram.bot-secret}")
    private String botSecret;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(
        @RequestHeader("X-Bot-Secret") String secret,
        @RequestBody TelegramCallbackRequest request
    ) {
        if (!botSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        telegramService.linkAccount(request.token(), request.chatId(), request.telegramUsername());
        return ResponseEntity.ok().build();
    }
}
