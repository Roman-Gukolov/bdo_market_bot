package com.bdo.model.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "wait_list_subs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WaitList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "item_id")
    private long itemId;
    @Column(name = "enhancement")
    private int enhancement;
    @Column(name = "chat_id")
    private long chatId;

    public WaitList(long itemId, int enhancement, long chatId) {
        this.itemId = itemId;
        this.enhancement = enhancement;
        this.chatId = chatId;
    }

}
