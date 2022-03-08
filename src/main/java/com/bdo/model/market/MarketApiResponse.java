package com.bdo.model.market;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketApiResponse {
    private SearchItem[] list;
    private DetailListItem[] detailList;
    private long[] priceList;
    private MarketConditionItem[] marketConditionList;
    private long basePrice;
    private int enchantGroup;
    private int enchantMaxGroup;
    private int enchantMaterialKey;
    private int enchantMaterialPrice;
    private int enchantNeedCount;
    private int maxRegisterForWorldMarket;
    private int countValue;
    private int sellMaxCount;
    private int buyMaxCount;
    private boolean isWaitItem;
    private int resultCode;
    private String resultMsg;
}
