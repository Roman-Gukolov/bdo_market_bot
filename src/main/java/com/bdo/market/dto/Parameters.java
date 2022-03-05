package com.bdo.market.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Parameters {
    TOKEN_PARAM("__RequestVerificationToken")
    , MAIN_KEY_PARAM("mainKey")
    , IS_UP_PARAM("isUp")
    , SUB_KEY_PARAM("subKey")
    , MAIN_CATEGORY_PARAM("mainCategory")
    , SUB_CATEGORY_PARAM("mainCategory")
    , KEY_TYPE_PARAM("keyType")
    , SEARCH_TEXT_PARAM("searchText")
    ;

    @Getter
    private final String name;
}
