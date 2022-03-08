package com.bdo.model.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "change_price_subs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "item_id")
    private long itemId;
    @Column(name = "enhancement")
    private int enhancement;
    @Column(name = "max_price")
    private Long maxPrice;
    @Column(name = "chat_id")
    private long chatId;

    public ChangePrice(long itemId, int enhancement, long maxPrice, long chatId) {
        this.itemId = itemId;
        this.enhancement = enhancement;
        this.maxPrice = maxPrice;
        this.chatId = chatId;
    }

    public ChangePrice(long itemId, int enhancement, long maxPrice) {
        this.itemId = itemId;
        this.enhancement = enhancement;
        this.maxPrice = maxPrice;
    }

}
