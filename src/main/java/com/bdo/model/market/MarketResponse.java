package com.bdo.model.market;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MarketResponse<T> {
    private T data;
    private ServiceError serviceError;
}
