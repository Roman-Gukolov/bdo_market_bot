package com.bdo.model.telegram;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ButtonAction {
    S_WAIT_LIST("Регистрация предмета")
    , S_WAIT_LIST_ITEM("Регистрация предмета")
    , S_CHANGE_PRICE("Изменение макс. цены")
    , S_CHANGE_PRICE_ITEM("Изменение макс. цены")
    , BACK("Назад")
    ;

    @Getter
    private final String value;
}
