package com.bdo.db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnChangePriceSub {
    private int id;
    private long itemId;
    private int enhancement;
    private Long maxPrice;
    private long chatId;

    public OnChangePriceSub(long itemId, int enhancement, long maxPrice, long chatId) {
        this.itemId = itemId;
        this.enhancement = enhancement;
        this.maxPrice = maxPrice;
        this.chatId = chatId;
    }

    public OnChangePriceSub(Long itemId, Integer enhancement, long chatId) {
        this.itemId = itemId;
        this.enhancement = enhancement;
        this.chatId = chatId;
    }

    public OnChangePriceSub(Long itemId, Integer enhancement) {
        this.itemId = itemId;
        this.enhancement = enhancement;
    }

    public OnChangePriceSub(Long chatId) {
        this.chatId = chatId;
    }
}
