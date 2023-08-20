package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatus;
import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.ChatStatusRepository;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.EventRepository;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramBotEventService implements EventService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH mm");
    private static final DateTimeFormatter TODAY_TOMORROW_FORMAT = DateTimeFormatter.ofPattern("'в' HH mm");
    DateTimeFormatter DAY_MONTH_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd MM")
            .parseDefaulting(ChronoField.YEAR, Year.now().getValue())
            .toFormatter();
    private static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MM yy HH mm");
    private static String closeEventsKey = "closeEventsKey";

    private final ChatStatusRepository chatStatusRepository;
    private final EventRepository eventRepository;
    private final CacheManager cacheManager;
    //todo прикрутить редис с временем жизни в 5 - 10мин
    Map<Long, Event> eventsWithoutTime = new HashMap<>();

    public TelegramBotEventService(ChatStatusRepository chatStatusRepository, EventRepository eventRepository, CacheManager cacheManager) {
        this.chatStatusRepository = chatStatusRepository;
        this.eventRepository = eventRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public String createEvent(ChatStatusEnum chatStatusEnum, long chatId, String text) {
        switch (chatStatusEnum) {
            case AWAITS_EVENT_NAME -> {
                eventsWithoutTime.put(chatId, new Event(text, chatId));
                //todo апдейтить запись вместо удаления
                chatStatusRepository.deleteByChatId(chatId);
                chatStatusRepository.save(new ChatStatus(chatId, ChatStatusEnum.AWAITS_EVENT_DATE));
                return """
                            Введите дату в одном из следующих видов:
                                Сегодня в HH mm
                                Завтра в HH mm
                                dd MM HH mm?
                                dd MM yy HH mm?
                        """;

            }
            case AWAITS_EVENT_DATE -> {
                LocalDateTime localDateTime = parseDateTime(text);
                if (localDateTime.isBefore(LocalDateTime.now())) {
                    return "Дата уже наступила, попробуйте ввести актуальную дату";
                }
                chatStatusRepository.deleteByChatId(chatId);
                Event event = eventsWithoutTime.get(chatId);
                eventsWithoutTime.remove(chatId);
                if (event == null) {
                    return "Событие не найдено, попробуйте заново.";
                }
                event.setEventDate(localDateTime);
                eventRepository.save(event);
                if (Duration.between(LocalDateTime.now(), event.getEventDate()).toMinutes() <= 120) {
                    //update cache if
                    updateCloseEventsCache(chatId, event);
                }
                return "Событие забронировано на " + localDateTime;
            }
            default -> throw new IllegalArgumentException("Неизвестный статус чата");
        }
    }

    private void updateCloseEventsCache(long chatId, Event event) {
        Cache closeEventsCache = cacheManager.getCache("closeEventsCache");
        if (closeEventsCache != null) {
            Map<Long, List<Event>> closeEvents = getCloseEvents();
            List<Event> events = closeEvents.computeIfAbsent(chatId, k -> new ArrayList<>());
            events.add(event);
            closeEvents.put(chatId, events);
            closeEventsCache.put(closeEventsKey, closeEvents);
            log.debug("updateCloseEventsCache: {}", closeEvents);
        }
    }

    private LocalDateTime parseDateTime(String date) {
        String lowerCaseText = date.toLowerCase();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        if (lowerCaseText.startsWith("сегодня")) {
            LocalTime time = LocalTime.parse(date.substring(8), TODAY_TOMORROW_FORMAT);
            return LocalDateTime.of(now.toLocalDate(), time);
        } else if (lowerCaseText.startsWith("завтра")) {
            LocalTime time = LocalTime.parse(date.substring(7), TODAY_TOMORROW_FORMAT);
            return LocalDateTime.of(now.plusDays(1).toLocalDate(), time);
        } else if (date.length() == 11) {
            LocalDate datePart = LocalDate.parse(date.substring(0, 5), DAY_MONTH_FORMAT);
            LocalTime timePart = LocalTime.parse(date.substring(6), TIME_FORMAT);
            return LocalDateTime.of(datePart.withYear(Year.now().getValue()), timePart);
        } else if (date.length() == 17) {
            return LocalDateTime.parse(date, FULL_DATE_FORMAT);
        } else {
            throw new IllegalArgumentException("Не удалось распознать дату");
        }
    }

    private List<Event> findTodayEvents(Long chatId, LocalDate now) {
        return eventRepository.findAllByChatIdAndEventDateBetweenOrderByEventDateAsc(
                chatId,
                now.atStartOfDay(),
                now.atTime(LocalTime.MAX)
        );
    }

    @Override
    public Map<Long, List<Event>> getCloseEvents() {
        Cache closeEventsCache = cacheManager.getCache("closeEventsCache");
        Cache.ValueWrapper closeEventsValue = null;
        if (closeEventsCache != null) {
            closeEventsValue = closeEventsCache.get(closeEventsKey);
        }
        if (closeEventsValue != null) {
            log.debug("getCloseEvents from cache");
            return (Map<Long, List<Event>>) closeEventsValue.get();
        }

        List<Event> events = eventRepository.findAllByEventDateBetween(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
                LocalDateTime.now().plusHours(2)
                        .plusMinutes(2)
                        .truncatedTo(ChronoUnit.HOURS)
        );

        Map<Long, List<Event>> eventsMap = events.stream()
                .collect(Collectors.groupingBy(Event::getChatId));
        if (closeEventsCache != null) closeEventsCache.put(closeEventsKey, eventsMap);
        log.debug("getCloseEvents from db");
        return eventsMap;
    }

    @Override
    public SendMessage todayEvents(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        InlineKeyboardMarkup daily = createInlineKeyboardForEvents(findTodayEvents(chatId, LocalDate.now()));
        if (daily.getKeyboard().isEmpty()) message.setText("Нет дел на сегодня =D");
        else message.setText("Грядущие дела:");
        message.setReplyMarkup(daily);
        return message;
    }

    @Override
    public InlineKeyboardMarkup createInlineKeyboardForEvents(List<Event> events) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Event e : events) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            String buttonText = e.getName();
            String callbackData = "event_info_" + e.getId();  // Предполагая, что у Event есть уникальный ID
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData(callbackData);
            row.add(button);
            keyboard.add(row);
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }
}
