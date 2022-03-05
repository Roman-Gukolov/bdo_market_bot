package com.bdo.db.mapper;

import com.bdo.db.dto.OnChangePriceSub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class OnChangePriceSubscribersMapper implements RowMapper<OnChangePriceSub> {

    @Override
    public OnChangePriceSub mapRow(ResultSet rs, int rowNum) throws SQLException {
        OnChangePriceSub entity = new OnChangePriceSub();
        entity.setChatId(rs.getLong("chat_id"));
        return entity;
    }
}
