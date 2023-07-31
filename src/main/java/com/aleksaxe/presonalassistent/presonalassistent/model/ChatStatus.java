package com.aleksaxe.presonalassistent.presonalassistent.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class ChatStatus {
    @Id
    private String id;
    private Long chatId;
    private ChatStatusEnum chatStatusEnum;

    public ChatStatus(Long chatId, ChatStatusEnum chatStatusEnum) {
        this.chatId = chatId;
        this.chatStatusEnum = chatStatusEnum;
    }
}
