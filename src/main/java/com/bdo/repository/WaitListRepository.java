package com.bdo.repository;

import com.bdo.model.db.WaitList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WaitListRepository extends JpaRepository<WaitList, Long> {

    List<WaitList> findByItemIdAndEnhancement(long itemId, int enhancement);

    List<WaitList> findByChatId(long chatId);

    @Transactional
    @Modifying
    void deleteByItemIdAndEnhancementAndChatId(long itemId, int enhancement, long chatId);

    @Transactional
    @Modifying
    void deleteAllByChatId(long chatId);

}
