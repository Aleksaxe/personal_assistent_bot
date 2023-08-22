package com.aleksaxe.presonalassistent.presonalassistent.services.intefaces;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatusEnum;
import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;
import java.util.Map;

public interface UserService {
    void setTimeZoneOffset(int timeZoneOffset, long chatId);
}
