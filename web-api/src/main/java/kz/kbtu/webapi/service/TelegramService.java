package kz.kbtu.webapi.service;

import kz.kbtu.webapi.dto.TelegramLinkResponse;
import kz.kbtu.webapi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TelegramService {

    private final UserRepository userRepository;
    private final RestClient telegramClient;

    @Value("${telegram.bot-name}")
    private String botName;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.link-token-expiry-minutes:15}")
    private int linkTokenExpiryMinutes;

    public TelegramService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.telegramClient = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    @Transactional
    public TelegramLinkResponse generateLinkToken(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var token = UUID.randomUUID().toString().replace("-", "");
        var expiry = LocalDateTime.now().plusMinutes(linkTokenExpiryMinutes);

        user.setTelegramLinkToken(token);
        user.setTelegramLinkTokenExpiry(expiry);
        userRepository.save(user);

        var linkUrl = "https://t.me/" + botName + "?start=" + token;
        return new TelegramLinkResponse(linkUrl, expiry);
    }

    @Transactional
    public void linkAccount(String token, Long chatId, String telegramUsername) {
        var user = userRepository.findByTelegramLinkToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired link token"));

        if (user.getTelegramLinkTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setTelegramLinkToken(null);
            user.setTelegramLinkTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Link token has expired");
        }

        user.setTelegramChatId(chatId);
        user.setTelegramUsername(telegramUsername);
        user.setTelegramLinkToken(null);
        user.setTelegramLinkTokenExpiry(null);
        userRepository.save(user);

        log.info("Linked Telegram @{} (chatId={}) to user '{}'", telegramUsername, chatId, user.getUsername());
    }

    @Transactional
    public void unlinkAccount(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        user.setTelegramChatId(null);
        user.setTelegramUsername(null);
        user.setTelegramLinkToken(null);
        user.setTelegramLinkTokenExpiry(null);
        userRepository.save(user);

        log.info("Unlinked Telegram from user '{}'", username);
    }

    public void sendMessage(Long chatId, String text) {
        try {
            telegramClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send Telegram message to chatId={}: {}", chatId, e.getMessage());
        }
    }
}
