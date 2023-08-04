package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatus;
import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.ChatStatusRepository;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.EventRepository;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.EventService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TelegramBotEventService implements EventService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH mm");
    private static final DateTimeFormatter TODAY_TOMORROW_FORMAT = DateTimeFormatter.ofPattern("'в' HH mm");
    DateTimeFormatter DAY_MONTH_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd MM")
            .parseDefaulting(ChronoField.YEAR, Year.now().getValue())
            .toFormatter();
    private static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MM yy HH mm");

    private final ChatStatusRepository chatStatusRepository;
    private final EventRepository eventRepository;
    //todo прикрутить редис с временем жизни в 5 - 10мин
    Map<Long, ChatStatusEnum> chatStatusMap = new HashMap<>();
    Map<Long, Event> events = new HashMap<>();

    public TelegramBotEventService(ChatStatusRepository chatStatusRepository, EventRepository eventRepository) {
        this.chatStatusRepository = chatStatusRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public String createEvent(ChatStatusEnum chatStatusEnum, long chatId, String text) {
        switch (chatStatusEnum) {
            case AWAITS_EVENT_NAME -> {
                events.put(chatId, new Event(text, chatId));
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
                Event event = events.get(chatId);
                events.remove(chatId);
                if (event == null) {
                    return "Событие не найдено, попробуйте заново.";
                }
                event.setEventDate(localDateTime);
                eventRepository.save(event);
                return "Событие забронировано на " + localDateTime;
            }
            default -> throw new IllegalArgumentException("Неизвестный статус чата");
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


    @Override
    public String todayEvents(long chatId) {
        return beautifyEventList(findTodayEvents(chatId, LocalDate.now()));
    }

    private String beautifyEventList(List<Event> event) {
        StringBuilder eventAsString = new StringBuilder();
        event.forEach(
                e -> eventAsString.append(
                                String.format("\n*Name:* %s\n*Date:* %s",
                                        e.getName(),
                                        e.getEventDate()
                                )
                        )
                        .append("\n\n"));
        return eventAsString.toString();
    }

    private List<Event> findTodayEvents(Long chatId, LocalDate now) {
        return eventRepository.findAllByChatIdAndEventDateBetweenOrderByEventDateAsc(
                chatId,
                now.atStartOfDay(),
                now.atTime(LocalTime.MAX)
        );
    }

    //todo сгрупиировать в строку для отправки ?
    @Override
    public Map<Long, List<Event>> getCloseEvents() {
        List<Event> events = eventRepository.findAllByEventDateBetween(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
                LocalDateTime.now().plusHours(2)
                        .plusMinutes(2)
                        .truncatedTo(ChronoUnit.HOURS)
        );
        return events.stream()
                .collect(Collectors.groupingBy(Event::getChatId));
    }

}
