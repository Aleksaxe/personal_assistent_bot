package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.EventService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TelegramBotEventService implements EventService {

    //todo прикрутить редис с временем жизни в 5 - 10мин
    Map<Long, ChatStatusEnum> chatStatusMap = new HashMap<>();
    Map<Long, Event> events = new HashMap<>();

    @Override
    public String createEvent(ChatStatusEnum chatStatusEnum, long chatId, String text) {

//        switch (chatStatusEnum) {
//            case AWAITS_EVENT_NAME -> {
//                events.put(chatId, new Event(text, chatId));
//                chatStatusMap.put(chatId, ChatStatusEnum.AWAITS_EVENT_DATE);
//                return """
//                            Введите дату в одном из следующих видов:
//                                Сегодня в HH mm
//                                Завтра в HH mm
//                                dd MM HH mm?
//                                dd MM yy HH mm?
//                        """;
//
//            }
//            case AWAITS_EVENT_DATE -> {
//                LocalDateTime localDateTime = parseDateTime(text);
//                if (localDateTime.isBefore(LocalDateTime.now())) {
//                    sendMessage(chatId, "Дата уже наступила, попробуйте ввести актуальную дату");
//                    return;
//                }
//                chatStatusMap.put(chatId, ChatStatusEnum.READY);
//                Event event = events.get(chatId);
//                events.remove(chatId);
//                if (event == null) {
//                    sendMessage(chatId, "Событие не найдено, попробуйте заново.");
//                    return;
//                }
//                event.setDate(localDateTime);
//                eventRepository.save(event);
//                sendMessage(chatId, "Событие забронировано на " + localDateTime);
//            }
//        }
        return "";
    }
}
