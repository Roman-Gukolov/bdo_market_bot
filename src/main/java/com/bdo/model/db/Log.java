package com.bdo.model.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "log")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "chat_id")
    private long chatId;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "message")
    private String message;
    @Column(name = "date_time")
    private Date date;

    public Log(long chatId, String userName, String message) {
        this.chatId = chatId;
        this.userName = userName;
        this.message = message;
        this.date = new Date();
    }
}
