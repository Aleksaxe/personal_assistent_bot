package com.aleksaxe.presonalassistent.presonalassistent.repositories;

import com.aleksaxe.presonalassistent.presonalassistent.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
}
