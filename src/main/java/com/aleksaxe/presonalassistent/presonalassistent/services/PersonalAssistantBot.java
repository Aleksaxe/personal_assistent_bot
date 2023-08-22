package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatus;
import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.ChatStatusRepository;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.EventRepository;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.EventService;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.UserService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
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
    private final UserService userService;

    @Autowired
    public PersonalAssistantBot(
            @Value("${bot.token}") String botToken,
            ExchangeCBRService exchangeCBRService,
            EventRepository eventRepository, ChatStatusRepository chatStatusRepository, EventService eventService, UserService userService) {
        super(botToken);
        this.exchangeCBRService = exchangeCBRService;
        this.eventRepository = eventRepository;
        this.chatStatusRepository = chatStatusRepository;
        this.eventService = eventService;
        this.userService = userService;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Optional<ChatStatus> chatStatus = chatStatusRepository.findByChatId(chatId);
        // уже запущен какой-то процесс
        if (chatStatus.isEmpty()) newQuery(chatId, message);
        else processQuery(chatStatus.get().getChatStatusEnum(), chatId, message);
    }

    private void newQuery(Long chatId, Message message) throws TelegramApiException {
        String text = message.getText();
        switch (text) {
            case "/start" -> sendMessage(chatId, "Привет! Я твой личный помощник. Я могу показать тебе курсы валют, " +
                    "погоду и т.д. Для этого просто напиши мне что ты хочешь узнать.");
            case "/event" -> {
                chatStatusRepository.save(new ChatStatus(chatId, ChatStatusEnum.AWAITS_EVENT_NAME));
                sendMessage(chatId, "Как назовем событие?");
            }
            case "/today_event" -> {
                sendMessage(eventService.todayEvents(chatId));
            }
            case "/set_time_zone_offset" -> {
                chatStatusRepository.save(new ChatStatus(chatId, ChatStatusEnum.AWAITS_TIME_ZONE_OFFSET));
                sendMessage(chatId, "Укажите часовой пояс для отправки напоминаний\n" +
                        "Например -6 или +8" );
            }
            case "/exchange" -> {
                sendMessage(createMarkupMessage(chatId, "Курсы валют:", exchangeCBRService.createInlineKeyboardForRates()));
            }
            default -> sendMessage(chatId, "Я не знаю такой команды. Попробуй написать /help");
        }
    }

    private void processQuery(ChatStatusEnum chatStatusEnum, Long chatId, Message message) {
        chatStatusRepository.deleteByChatId(chatId);
        switch (chatStatusEnum) {
            case AWAITS_EVENT_NAME, AWAITS_EVENT_DATE -> {
                sendMessage(chatId,
                        eventService.createEvent(chatStatusEnum, chatId, message)
                );
            }
            case AWAITS_TIME_ZONE_OFFSET -> {
                String text = message.getText();
                userService.setTimeZoneOffset(Integer.parseInt(text), chatId);
                sendMessage(chatId, "Часовой пояс " + text + " установлен" );
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

    private void sendMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error while sending message to chatId: " + sendMessage.getChatId(), e);
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
                LocalDateTime eventDate = event.getEventDate();
                LocalDateTime now = LocalDateTime.now();

                // Вычислить разницу во времени между текущим временем и временем события
                Duration duration = Duration.between(now, eventDate);

                // Проверить, что событие произойдет в течение следующих 2 часов
                if (duration.toMinutes() == 120) {
                    sendMessage(createMarkupMessage(
                            chatId,
                            "Напоминаю, что через 2 часа у вас запланировано событие: " + event.getName(),
                            eventService.createInlineKeyboardForEvents(Collections.singletonList(event))
                    ));
                } else if (duration.toMinutes() == 60) {
                    sendMessage(createMarkupMessage(
                            chatId,
                            "Напоминаю, что через 1 час у вас запланировано событие: " + event.getName(),
                            eventService.createInlineKeyboardForEvents(Collections.singletonList(event))
                    ));
                } else if (duration.toMinutes() == 30) {
                    sendMessage(createMarkupMessage(
                            chatId,
                            "Напоминаю, что через 30 минут у вас запланировано событие: " + event.getName(),
                            eventService.createInlineKeyboardForEvents(Collections.singletonList(event))
                    ));
                } else if (duration.toMinutes() == 10) {
                    sendMessage(createMarkupMessage(
                            chatId,
                            "Напоминаю, что через 10 минут у вас запланировано событие: " + event.getName(),
                            eventService.createInlineKeyboardForEvents(Collections.singletonList(event))
                    ));
                }
            });
        });
    }

    public void clearEvents() {
        eventRepository.deleteByEventDateBefore(LocalDateTime.now());
    }

    private SendMessage createMarkupMessage(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(markup);
        return message;
    }
}
