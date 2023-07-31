package com.aleksaxe.presonalassistent.presonalassistent.services.intefaces;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;

public interface EventService {
    String createEvent(ChatStatusEnum chatStatusEnum, long chatId, String text);
}
