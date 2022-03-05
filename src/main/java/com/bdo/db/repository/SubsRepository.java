package com.bdo.db.repository;

import com.bdo.db.dto.OnChangePriceSub;
import com.bdo.db.dto.WaitListSub;
import com.bdo.db.mapper.*;
import com.bdo.exception.DbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Slf4j
@Repository
public class SubsRepository implements ISubsRepository {

    protected final JdbcTemplate template;
    protected final static WaitListSubMapper WAIT_LIST_MAPPER = new WaitListSubMapper();
    protected final static WaitListSubscriptionMapper WAIT_LIST_SUBSCRIPTION_MAPPER = new WaitListSubscriptionMapper();
    protected final static OnChangePriceSubAllMapper ON_CHANGE_PRICE_SUB_ALL_MAPPER = new OnChangePriceSubAllMapper();
    protected final static OnChangePriceSubscribersMapper ON_CHANGE_PRICE_SUBSCRIBERS_MAPPER = new OnChangePriceSubscribersMapper();
    protected final static OnChangePriceSubscriptionMapper ON_CHANGE_PRICE_SUBSCRIPTION_MAPPER = new OnChangePriceSubscriptionMapper();

    private final static String LOG_MSG = "Execute sql [{}]"
                                        + "Data [{}]"
                                        + "Result [{}]";

    private static final String SELECT_WAIT_LIST_SQL =
            "SELECT item_id, enhancement, chat_id FROM wait_list_subs WHERE item_id = ? AND enhancement = ?";
    private static final String SELECT_WAIT_LIST_SUBSCRIPTIONS_SQL =
            "SELECT item_id, enhancement FROM wait_list_subs WHERE chat_id = ?";
    private static final String INSERT_WAIT_LIST_SQL =
            "INSERT INTO wait_list_subs (item_id, enhancement, chat_id) VALUES (?, ?, ?)";
    private static final String DELETE_WAIT_LIST_SQL =
            "DELETE FROM wait_list_subs WHERE item_id = ? AND enhancement = ? AND chat_id = ?";

    private static final String SELECT_ALL_ON_CHANGE_PRICE_SQL =
            "SELECT item_id, enhancement, max_price FROM change_price_subs group by item_id";
    private static final String SELECT_SUBSCRIBERS_ON_CHANGE_PRICE_SQL =
            "SELECT chat_id FROM change_price_subs where item_id = ? AND enhancement = ?";
    private static final String SELECT_SUBSCRIPTIONS_ON_CHANGE_PRICE_SQL =
            "SELECT item_id, enhancement FROM change_price_subs where chat_id = ?";
    private static final String UPDATE_ON_CHANGE_PRICE_SQL =
            "UPDATE change_price_subs SET max_price = ? WHERE item_id = ? AND enhancement = ? AND chat_id = ?";
    private static final String INSERT_ON_CHANGE_PRICE_SQL =
            "INSERT INTO change_price_subs (item_id, enhancement, max_price, chat_id) VALUES (?, ?, ?, ?)";
    private static final String DELETE_ON_CHANGE_PRICE_SQL =
            "DELETE FROM change_price_subs WHERE item_id = ? AND enhancement = ? AND chat_id = ?";

    private static final String UNSUBSCRIBE_ALL_WAIT_LIST_SQL =
            "DELETE FROM wait_list_subs WHERE chat_id = ?";
    private static final String UNSUBSCRIBE_ALL_ON_CHANGE_PRICE_SQL =
            "DELETE FROM change_price_subs WHERE chat_id = ?";

    private static final String INSERT_USER_SQL =
            "INSERT INTO users (user_id, chat_id, user_name) VALUES (?, ?, ?)";

    private static final String INSERT_LOG_SQL =
            "INSERT INTO log (chat_id, user_name, message, date_time, user_id) VALUES (?, ?, ?, ?, ?)";

    public SubsRepository(@Qualifier("bot-db") JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public List<WaitListSub> getSubscribersWaitList(WaitListSub entity) {
        try {
            List<WaitListSub> result = template.query(SELECT_WAIT_LIST_SQL, WAIT_LIST_MAPPER,
                    entity.getItemId(), entity.getEnhancement());
            log.info(LOG_MSG, SELECT_WAIT_LIST_SQL, entity, result);
            return result;
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public List<WaitListSub> getSubscriptionsWaitList(WaitListSub entity) {
        try {
            List<WaitListSub> result = template.query(SELECT_WAIT_LIST_SUBSCRIPTIONS_SQL, WAIT_LIST_SUBSCRIPTION_MAPPER,
                    entity.getChatId());
            log.info(LOG_MSG, SELECT_WAIT_LIST_SUBSCRIPTIONS_SQL, entity, result);
            return result;
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void subscribeWaitList(WaitListSub entity) throws DbException {
        try {
            int result = template.update(INSERT_WAIT_LIST_SQL,
                    entity.getItemId(), entity.getEnhancement(), entity.getChatId());
            log.info(LOG_MSG, INSERT_WAIT_LIST_SQL, entity, result);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void unsubscribeWaitList(WaitListSub entity) {
        try {
            int result = template.update(DELETE_WAIT_LIST_SQL,
                    entity.getItemId(), entity.getEnhancement(), entity.getChatId());
            log.info(LOG_MSG, DELETE_WAIT_LIST_SQL, entity, result);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public List<OnChangePriceSub> getSubscriptionsOnChangePrice() {
        try {
            List<OnChangePriceSub> result = template.query(SELECT_ALL_ON_CHANGE_PRICE_SQL, ON_CHANGE_PRICE_SUB_ALL_MAPPER);
            log.info(LOG_MSG, SELECT_ALL_ON_CHANGE_PRICE_SQL, null, result);
            return result;
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public List<OnChangePriceSub> getSubscribersOnChangePrice(OnChangePriceSub entity) {
        try {
            List<OnChangePriceSub> result = template.query(SELECT_SUBSCRIBERS_ON_CHANGE_PRICE_SQL,
                    ON_CHANGE_PRICE_SUBSCRIBERS_MAPPER, entity.getItemId(), entity.getEnhancement());
            log.info(LOG_MSG, SELECT_SUBSCRIBERS_ON_CHANGE_PRICE_SQL, entity, result);
            return result;
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public List<OnChangePriceSub> getSubscriptionsOnChangePrice(OnChangePriceSub entity) {
        try {
            List<OnChangePriceSub> result = template.query(SELECT_SUBSCRIPTIONS_ON_CHANGE_PRICE_SQL,
                    ON_CHANGE_PRICE_SUBSCRIPTION_MAPPER, entity.getChatId());
            log.info(LOG_MSG, SELECT_SUBSCRIPTIONS_ON_CHANGE_PRICE_SQL, entity, result);
            return result;
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void updatePrice(OnChangePriceSub entity) {
        try {
            int result = template.update(UPDATE_ON_CHANGE_PRICE_SQL,
                    entity.getMaxPrice(), entity.getItemId(), entity.getEnhancement(), entity.getChatId());
            log.info(LOG_MSG, UPDATE_ON_CHANGE_PRICE_SQL, entity, result);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void subscribeOnChangePrice(OnChangePriceSub entity) {
        try {
            int result = template.update(INSERT_ON_CHANGE_PRICE_SQL,
                    entity.getItemId(), entity.getEnhancement(), entity.getMaxPrice(), entity.getChatId());
            log.info(LOG_MSG, INSERT_ON_CHANGE_PRICE_SQL, entity, result);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void unsubscribeOnChangePrice(OnChangePriceSub entity) {
        try {
            int result = template.update(DELETE_ON_CHANGE_PRICE_SQL,
                    entity.getItemId(), entity.getEnhancement(), entity.getChatId());
            log.info(LOG_MSG, DELETE_ON_CHANGE_PRICE_SQL, entity, result);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void unsubscribeAll(long chatId) {
        try {
            int resultWaitList = template.update(UNSUBSCRIBE_ALL_WAIT_LIST_SQL, chatId);
            log.info(LOG_MSG, UNSUBSCRIBE_ALL_WAIT_LIST_SQL, null, resultWaitList);
            int resultOnChangePrice = template.update(UNSUBSCRIBE_ALL_ON_CHANGE_PRICE_SQL, chatId);
            log.info(LOG_MSG, UNSUBSCRIBE_ALL_ON_CHANGE_PRICE_SQL, null, resultOnChangePrice);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void addUser(long userId, long chatId, String userName) {
        try {
            int result = template.update(INSERT_USER_SQL, userId, chatId, userName);
            String data = String.format("userId [%s], chatId [%s], userName [%s]", userId, chatId, userName);
            log.info(LOG_MSG, INSERT_USER_SQL, data, result);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }

    @Override
    public void logUserMessage(long chatId, String userName, String message, long userId) {
        try {
            int result = template.update(INSERT_LOG_SQL, chatId, userName, message, new Date(), userId);
            String data = String.format("chatId [%s], userName [%s], message [%s]", chatId, userName, message);
            log.info(LOG_MSG, INSERT_LOG_SQL, data, result);
        } catch (Exception e) {
            throw new DbException(e);
        }
    }
}
