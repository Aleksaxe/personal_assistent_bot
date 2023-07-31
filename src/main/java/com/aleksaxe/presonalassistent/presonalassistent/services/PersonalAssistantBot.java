package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatus;
import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.ChatStatusRepository;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
import java.util.Optional;

@Slf4j
@Component
public class PersonalAssistantBot extends TelegramLongPollingBot {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH mm");
    private static final DateTimeFormatter TODAY_TOMORROW_FORMAT = DateTimeFormatter.ofPattern("'в' HH mm");
    DateTimeFormatter DAY_MONTH_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("dd MM")
            .parseDefaulting(ChronoField.YEAR, Year.now().getValue())
            .toFormatter();
    private static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MM yy HH mm");

    private final ExchangeCBRService exchangeCBRService;
    private final EventRepository eventRepository;
    private final ChatStatusRepository chatStatusRepository;

    @Autowired
    public PersonalAssistantBot(
            @Value("${bot.token}") String botToken,
            ExchangeCBRService exchangeCBRService,
            EventRepository eventRepository, ChatStatusRepository chatStatusRepository) {
        super(botToken);
        this.exchangeCBRService = exchangeCBRService;
        this.eventRepository = eventRepository;
        this.chatStatusRepository = chatStatusRepository;
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
            case "/today_event" -> {
                sendMessage(chatId, beautifyEventList(
                        findEvent(chatId, LocalDate.now()))
                );
            }
            case "/exchange" -> sendMessage(chatId, exchangeCBRService.getUserRates());
            default -> sendMessage(chatId, "Я не знаю такой команды. Попробуй написать /help");
        }
    }

    private String beautifyEventList(List<Event> event) {
        StringBuilder eventAsString = new StringBuilder();
        event.forEach(
                e -> eventAsString.append(
                                String.format("\n*Name:* %s\n*Date:* %s",
                                        e.getName(),
                                        e.getDate()
                                )
                        )
                        .append("\n\n"));
        return eventAsString.toString();
    }

    private List<Event> findEvent(Long chatId, LocalDate now) {
        return eventRepository.findAllByChatIdAndDateBetweenOrderByDateAsc(
                chatId,
                now.atStartOfDay(),
                now.atTime(LocalTime.MAX)
        );
    }

    private void processQuery(ChatStatusEnum chatStatusEnum, Long chatId, String text) {
        switch (chatStatusEnum) {
            case AWAITS_EVENT_NAME -> {
                events.put(chatId, new Event(text, chatId));
                //todo апдейтить запись вместо удаления
                chatStatusRepository.deleteByChatId(chatId);
                chatStatusRepository.save(new ChatStatus(chatId, ChatStatusEnum.AWAITS_EVENT_DATE));
                sendMessage(chatId, """
                            Введите дату в одном из следующих видов:
                                Сегодня в HH mm
                                Завтра в HH mm
                                dd MM HH mm?
                                dd MM yy HH mm?
                        """);

            }
            case AWAITS_EVENT_DATE -> {
                LocalDateTime localDateTime = parseDateTime(text);
                if (localDateTime.isBefore(LocalDateTime.now())) {
                    sendMessage(chatId, "Дата уже наступила, попробуйте ввести актуальную дату");
                    return;
                }
                chatStatusRepository.deleteByChatId(chatId);
                Event event = events.get(chatId);
                events.remove(chatId);
                if (event == null) {
                    sendMessage(chatId, "Событие не найдено, попробуйте заново.");
                    return;
                }
                event.setDate(localDateTime);
                eventRepository.save(event);
                sendMessage(chatId, "Событие забронировано на " + localDateTime);
            }
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
}
