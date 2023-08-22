package com.aleksaxe.presonalassistent.presonalassistent.services;

import com.aleksaxe.presonalassistent.presonalassistent.model.User;
import com.aleksaxe.presonalassistent.presonalassistent.repositories.UserRepository;
import com.aleksaxe.presonalassistent.presonalassistent.services.intefaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public User createUser(String name, Long chatId) {
        return userRepository.save(new com.aleksaxe.presonalassistent.presonalassistent.model.User(name, chatId));
    }

    @Override
    public void setTimeZoneOffset(int timeZoneOffset, long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElse(createUser("UserName", chatId));
        user.setTimeZoneOffset(timeZoneOffset);
        userRepository.save(user);
    }
}
