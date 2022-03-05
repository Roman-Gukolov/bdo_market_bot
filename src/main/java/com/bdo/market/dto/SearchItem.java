package com.bdo.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchItem {
    @JsonProperty(value = "mainKey")
    private long id;
    private int sumCount;
    private long totalSumCount;
    private String name;
}
