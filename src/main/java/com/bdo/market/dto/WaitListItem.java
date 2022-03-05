package com.bdo.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WaitListItem {

    @EqualsAndHashCode.Include
    private long itemId;

    private String itemName;

    @EqualsAndHashCode.Include
    private int enhancement;

    @EqualsAndHashCode.Include
    private long price;

    @EqualsAndHashCode.Include
    private long time;
}
