package com.aleksaxe.presonalassistent.presonalassistent.repositories;

import com.aleksaxe.presonalassistent.presonalassistent.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, Long> {
    List<Event> findAllByChatIdAndEventDateBetweenOrderByEventDateAsc(Long chatId, LocalDateTime atStartOfDay, LocalDateTime atTime);
    List<Event> findAllByEventDateBetween(LocalDateTime from, LocalDateTime to);
    void deleteByEventDateBefore(LocalDateTime date);

}
