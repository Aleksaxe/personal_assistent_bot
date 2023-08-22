package com.aleksaxe.presonalassistent.presonalassistent.scheduler;

import com.aleksaxe.presonalassistent.presonalassistent.services.PersonalAssistantBot;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class EventScheduler {
    private final PersonalAssistantBot personalAssistantBot;

    public EventScheduler(PersonalAssistantBot personalAssistantBot) {
        this.personalAssistantBot = personalAssistantBot;
    }

    @Scheduled(fixedRate = 60000)
    public void sendNotificationsTask() {
        personalAssistantBot.sendEventNotification();
    }

    @Scheduled(cron = "0 0 5 * * ?")
    public void clearEventsTask() {
        personalAssistantBot.clearEvents();
    }

    @CacheEvict(value = "closeEventsCache", allEntries = true)
    @Scheduled(fixedRateString = "${caching.spring.closeEventsCache.evictRate}")
    public void evictCloseEventsCache() {
    }
}
