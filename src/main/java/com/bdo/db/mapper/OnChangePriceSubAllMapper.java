package com.bdo.db.mapper;

import com.bdo.db.dto.OnChangePriceSub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class OnChangePriceSubAllMapper implements RowMapper<OnChangePriceSub> {

    @Override
    public OnChangePriceSub mapRow(ResultSet rs, int rowNum) throws SQLException {
        OnChangePriceSub entity = new OnChangePriceSub();
        entity.setItemId(rs.getLong("item_id"));
        entity.setEnhancement(rs.getInt("enhancement"));
        entity.setMaxPrice(rs.getLong("max_price"));
        return entity;
    }
}
