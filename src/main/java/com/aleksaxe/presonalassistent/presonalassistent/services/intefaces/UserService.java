package com.aleksaxe.presonalassistent.presonalassistent.services.intefaces;

import com.aleksaxe.presonalassistent.presonalassistent.model.User;

import java.util.Optional;

public interface UserService {
    void setTimeZoneOffset(int timeZoneOffset, long chatId);

    Optional<User> getUserByChatId(Long chatId);
}
