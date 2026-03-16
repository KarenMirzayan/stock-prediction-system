package kz.kbtu.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class TelegramSender {

    private final RestClient restClient;

    public TelegramSender(@Value("${telegram.bot-token}") String botToken) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .build();
    }

    public void sendMessage(Long chatId, String text) {
        try {
            restClient.post()
                    .uri("/sendMessage")
                    .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send Telegram message to chatId={}: {}", chatId, e.getMessage());
        }
    }
}
