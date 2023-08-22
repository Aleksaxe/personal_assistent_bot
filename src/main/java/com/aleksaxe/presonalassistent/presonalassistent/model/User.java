package com.aleksaxe.presonalassistent.presonalassistent.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.ZonedDateTime;

@Getter @Setter @NoArgsConstructor
public class User {
    @Id
    private String id;
    private Long chatId;
    private String name;
    private int timeZoneOffset;

    public User(String name, Long chatId) {
        this.name = name;
        this.chatId = chatId;
        this.timeZoneOffset = 0;
    }

}
