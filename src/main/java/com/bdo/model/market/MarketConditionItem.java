package com.bdo.model.market;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketConditionItem {
    private long sellCount;
    private long buyCount;
    private long pricePerOne;
}
