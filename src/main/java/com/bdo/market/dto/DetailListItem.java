package com.bdo.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailListItem {
    private long pricePerOne;
    private long totalTradeCount;
    private int keyType;
    private long mainKey;
    private long subKey;
    private long count;
    private String name;
    private int grade;
    private int mainCategory;
    private int subCategory;
    private int chooseKey;

    public DetailListItem(String name) {
        this.name = name;
    }
}
