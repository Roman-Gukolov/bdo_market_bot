package com.bdo.db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WaitListSub {
    private int id;
    private long itemId;
    private int enhancement;
    private long chatId;

    public WaitListSub(long itemId, int enhancement, long chatId) {
        this.itemId = itemId;
        this.enhancement = enhancement;
        this.chatId = chatId;
    }

    public WaitListSub(long itemId, int enhancement) {
        this.itemId = itemId;
        this.enhancement = enhancement;
    }

    public WaitListSub(long chatId) {
        this.chatId = chatId;
    }
}
