package com.aleksaxe.presonalassistent.presonalassistent.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class Event {
    @Id
    private String id;
    private String name;
    private LocalDateTime eventDate;
    private Long chatId;

    public Event(String name, Long chatId) {
        this.name = name;
        this.chatId = chatId;
    }

    @Override
    public String toString() {
        return "Event{" +
                "name='" + name + '\'' +
                ", date=" + eventDate +
                '}';
    }
}
