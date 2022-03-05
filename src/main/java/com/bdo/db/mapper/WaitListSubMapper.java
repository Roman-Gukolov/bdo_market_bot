package com.bdo.db.mapper;

import com.bdo.db.dto.WaitListSub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class WaitListSubMapper implements RowMapper<WaitListSub> {

    @Override
    public WaitListSub mapRow(ResultSet rs, int rowNum) throws SQLException {
        WaitListSub entity = new WaitListSub(
                rs.getLong("item_id"),
                rs.getInt("enhancement"),
                rs.getLong("chat_id")
                );
        return entity;
    }
}
