package com.aleksaxe.presonalassistent.presonalassistent.repositories;

import com.aleksaxe.presonalassistent.presonalassistent.model.ChatStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ChatStatusRepository extends MongoRepository<ChatStatus, Long> {
    Optional<ChatStatus> findByChatId(Long chatId);
    void deleteByChatId(Long chatId);
}
