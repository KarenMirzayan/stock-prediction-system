package kz.kbtu.notificationservice.listener;

import kz.kbtu.common.dto.ArticleNotificationEvent;
import kz.kbtu.common.entity.Company;
import kz.kbtu.common.entity.User;
import kz.kbtu.notificationservice.repository.UserRepository;
import kz.kbtu.notificationservice.service.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleNotificationListener {

    private final UserRepository userRepository;
    private final TelegramSender telegramSender;

    @KafkaListener(topics = "article-notifications", groupId = "notification-service")
    public void onArticlePublished(ArticleNotificationEvent event) {
        log.info("Received notification event: '{}' tickers={}", event.getTitle(), event.getCompanyTickers());

        Set<String> tickers = event.getCompanyTickers();
        if (tickers == null || tickers.isEmpty()) return;

        List<User> users = userRepository.findTelegramUsersSubscribedToTickers(tickers);
        if (users.isEmpty()) {
            log.info("No subscribers to notify for tickers {}", tickers);
            return;
        }

        for (User user : users) {
            String matchedCompanies = user.getSubscribedCompanies().stream()
                    .filter(c -> tickers.contains(c.getTicker()))
                    .map(c -> c.getName() + " (" + c.getTicker() + ")")
                    .collect(Collectors.joining(", "));

            String message = formatMessage(event, matchedCompanies);
            telegramSender.sendMessage(user.getTelegramChatId(), message);
            log.info("Notified @{} for {}", user.getTelegramUsername(), matchedCompanies);
        }
    }

    private String formatMessage(ArticleNotificationEvent event, String companies) {
        var sb = new StringBuilder();
        sb.append("\uD83D\uDCF0 <b>").append(escapeHtml(event.getTitle())).append("</b>\n\n");
        if (event.getSummary() != null && !event.getSummary().isBlank()) {
            sb.append(escapeHtml(event.getSummary())).append("\n\n");
        }
        if (event.getUrl() != null && !event.getUrl().isBlank()) {
            sb.append("\uD83D\uDD17 <a href=\"").append(event.getUrl()).append("\">Read full article</a>\n\n");
        }
        sb.append("\uD83D\uDCCC You received this because you subscribed to <b>")
          .append(escapeHtml(companies)).append("</b>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
