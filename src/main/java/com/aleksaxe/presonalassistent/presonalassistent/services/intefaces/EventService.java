package com.aleksaxe.presonalassistent.presonalassistent.services.intefaces;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;

import java.util.List;
import java.util.Map;

public interface EventService {
    String createEvent(ChatStatusEnum chatStatusEnum, long chatId, String text);
    String todayEvents(long chatId);

    Map<Long, List<Event>> getCloseEvents();
}
