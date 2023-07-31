package com.aleksaxe.presonalassistent.presonalassistent.configurations;

import com.aleksaxe.presonalassistent.presonalassistent.services.PersonalAssistantBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class PersonalAssistantBotConfiguration {

    @Bean
    public TelegramBotsApi telegramBotsApi(PersonalAssistantBot personalAssistantBot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(personalAssistantBot);
        return api;
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
