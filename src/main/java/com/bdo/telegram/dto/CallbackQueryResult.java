package com.bdo.telegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

@Data
@AllArgsConstructor
public class CallbackQueryResult {
    private EditMessageText message;
    private AnswerCallbackQuery callbackQuery;
}
