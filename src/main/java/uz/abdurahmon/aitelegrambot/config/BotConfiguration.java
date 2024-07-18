package uz.abdurahmon.aitelegrambot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:bot.properties")
public class BotConfiguration {
    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.name}")
    private String botName;
}