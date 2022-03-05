package com.bdo.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchItemInfo {
    private boolean fullResponse;
    private List<DetailListItem> detailListItem;

    public SearchItemInfo(boolean fullResponse) {
        this.fullResponse = fullResponse;
    }
}
