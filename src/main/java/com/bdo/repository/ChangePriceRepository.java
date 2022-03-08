package com.bdo.repository;

import com.bdo.model.db.ChangePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChangePriceRepository extends JpaRepository<ChangePrice, Long> {

    @Query("select price from ChangePrice price group by price.itemId")
    List<ChangePrice> findWithGroupBy();

    @Query("select price from ChangePrice price where price.itemId=:itemId and price.enhancement=:enhancement")
    List<ChangePrice> findByItemIdAndEnhancement(long itemId, int enhancement);

    @Query("select price from ChangePrice price where price.chatId=:chatId")
    List<ChangePrice> findByChatId(long chatId);

    @Transactional
    @Modifying
    @Query("update ChangePrice price set price.maxPrice=:maxPrice where price.itemId=:itemId and price.enhancement=:enhancement and price.chatId=:chatId")
    void updateMaxPrice(long maxPrice, long itemId, int enhancement, long chatId);

    @Transactional
    @Modifying
    void deleteByItemIdAndEnhancementAndChatId(long itemId, int enhancement, long chatId);

    @Transactional
    @Modifying
    void deleteAllByChatId(long chatId);
}
