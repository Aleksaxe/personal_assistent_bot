package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatus;
import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.ChatStatusRepository;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.EventRepository;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class PersonalAssistantBot extends TelegramLongPollingBot {

    private final ExchangeCBRService exchangeCBRService;
    private final EventRepository eventRepository;
    private final ChatStatusRepository chatStatusRepository;
    private final EventService eventService;

    @Autowired
    public PersonalAssistantBot(
            @Value("${bot.token}") String botToken,
            ExchangeCBRService exchangeCBRService,
            EventRepository eventRepository, ChatStatusRepository chatStatusRepository, EventService eventService) {
        super(botToken);
        this.exchangeCBRService = exchangeCBRService;
        this.eventRepository = eventRepository;
        this.chatStatusRepository = chatStatusRepository;
        this.eventService = eventService;
    }

    Map<Long, Event> events = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();
        Optional<ChatStatus> chatStatus = chatStatusRepository.findByChatId(chatId);
        // уже запущен какой-то процесс
        if (chatStatus.isEmpty()) newQuery(chatId, text);
        else processQuery(chatStatus.get().getChatStatusEnum(), chatId, text);
    }

    private void newQuery(Long chatId, String text) {
        switch (text) {
            case "/start" -> sendMessage(chatId, "Привет! Я твой личный помощник. Я могу показать тебе курсы валют, " +
                    "погоду и т.д. Для этого просто напиши мне что ты хочешь узнать.");
            case "/event" -> {
                chatStatusRepository.save(new ChatStatus(chatId, ChatStatusEnum.AWAITS_EVENT_NAME));
                sendMessage(chatId, "Как назовем событие?");
            }
            case "/today_event" -> sendMessage(chatId, eventService.todayEvents(chatId));
            case "/exchange" -> sendMessage(chatId, exchangeCBRService.getUserRates());
            default -> sendMessage(chatId, "Я не знаю такой команды. Попробуй написать /help");
        }
    }

    private void processQuery(ChatStatusEnum chatStatusEnum, Long chatId, String text) {
        switch (chatStatusEnum) {
            case AWAITS_EVENT_NAME, AWAITS_EVENT_DATE -> {
                sendMessage(chatId,
                        eventService.createEvent(chatStatusEnum, chatId, text)
                );
            }
        }
    }


    private void sendMessage(Long chatId, String s) {
        try {
            execute(new SendMessage(chatId.toString(), s));
        } catch (TelegramApiException e) {
            log.error("Error while sending message to chatId: " + chatId, e);
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public String getBotUsername() {
        return "YourPersonalAssistantBot";
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    public void sendEventNotification() {
        // Получить все ближайшие события для всех пользователей
        Map<Long, List<Event>> closeEvents = eventService.getCloseEvents();

        closeEvents.keySet().forEach(chatId -> {
            List<Event> events = closeEvents.get(chatId);
            events.forEach(event -> {
                LocalDateTime eventDate = event.getDate();
                LocalDateTime now = LocalDateTime.now();

                // Вычислить разницу во времени между текущим временем и временем события
                Duration duration = Duration.between(now, eventDate);

                // Проверить, что событие произойдет в течение следующих 2 часов
                if (duration.toMinutes() == 120) {
                    sendMessage(event.getChatId(), "Напоминаю, что через 2 часа у вас запланировано событие: " + event.getName());
                } else if (duration.toMinutes() == 60) {
                    sendMessage(event.getChatId(), "Напоминаю, что через 1 час у вас запланировано событие: " + event.getName());
                } else if (duration.toMinutes() == 30) {
                    sendMessage(event.getChatId(), "Напоминаю, что через 30 минут у вас запланировано событие: " + event.getName());
                } else if (duration.toMinutes() == 10) {
                    sendMessage(event.getChatId(), "Напоминаю, что через 10 минут у вас запланировано событие: " + event.getName());
                }
            });
        });
    }
}
