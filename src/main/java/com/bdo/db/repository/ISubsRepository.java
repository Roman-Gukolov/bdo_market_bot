package com.bdo.db.repository;

import com.bdo.db.dto.OnChangePriceSub;
import com.bdo.db.dto.WaitListSub;

import java.util.List;

public interface ISubsRepository {

    /**
     * Получение списка пользователей, подписанных на уведомления.
     * @param entity данные предмета.
     */
    List<WaitListSub> getSubscribersWaitList(WaitListSub entity);

    /**
     * Получение подписок пользователя.
     * @param entity данные предмета.
     */
    List<WaitListSub> getSubscriptionsWaitList(WaitListSub entity);

    /**
     * Подписаться на уведомления.
     * @param entity данные предмета.
     */
    void subscribeWaitList(WaitListSub entity);

    /**
     * Отписаться от уведомлений.
     * @param entity данные предмета.
     */
    void unsubscribeWaitList(WaitListSub entity);

    /**
     * Получение списка пользователей, подписанных на уведомления.
     */
    List<OnChangePriceSub> getSubscriptionsOnChangePrice();

    /**
     * Получение списка пользователей, подписанных на уведомления.
     * @param entity данные предмета.
     */
    List<OnChangePriceSub> getSubscribersOnChangePrice(OnChangePriceSub entity);

    /**
     * Получение подписок пользователя.
     * @param entity данные предмета.
     */
    List<OnChangePriceSub> getSubscriptionsOnChangePrice(OnChangePriceSub entity);

    /**
     * Обновить максимальную стоимость предмета.
     * @param entity данные предмета.
     */
    void updatePrice(OnChangePriceSub entity);

    /**
     * Подписаться на уведомления.
     * @param entity данные предмета.
     */
    void subscribeOnChangePrice(OnChangePriceSub entity);

    /**
     * Отписаться от уведомлений.
     * @param entity данные предмета.
     */
    void unsubscribeOnChangePrice(OnChangePriceSub entity);

    /**
     * Отписаться от всех уведомлений.
     * @param chatId ID чата
     */
    void unsubscribeAll(long chatId);

    void addUser(long userId, long chatId, String userName);

    void logUserMessage(long chatId, String userName, String message, long userId);
}
