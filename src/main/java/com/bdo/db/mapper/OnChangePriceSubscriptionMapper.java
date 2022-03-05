package com.bdo.db.mapper;

import com.bdo.db.dto.OnChangePriceSub;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OnChangePriceSubscriptionMapper implements RowMapper<OnChangePriceSub> {
    @Override
    public OnChangePriceSub mapRow(ResultSet rs, int rowNum) throws SQLException {
        OnChangePriceSub entity = new OnChangePriceSub();
        entity.setItemId(rs.getLong("item_id"));
        entity.setEnhancement(rs.getInt("enhancement"));
        return entity;
    }
}
