package com.bdo.telegram.service;

import com.bdo.db.dto.OnChangePriceSub;
import com.bdo.db.dto.WaitListSub;
import com.bdo.db.repository.ISubsRepository;
import com.bdo.db.repository.SubsRepository;
import com.bdo.market.Constants;
import com.bdo.market.dto.*;
import com.bdo.market.service.MarketService;
import com.bdo.telegram.dto.AllowedNotifyItem;
import com.bdo.telegram.dto.ButtonAction;
import com.bdo.telegram.dto.ButtonCallbackData;
import com.bdo.telegram.dto.CallbackQueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@NoArgsConstructor
@Slf4j
public class BotService {

    private ISubsRepository repository;
    private MarketService marketService;
    private AllowedNotifyItem[] dictionary;
    private ObjectMapper mapper;

    private static final int MAX_TEXT_LENGTH = 4096;
    private static final String WAIT_LIST_ALLOWED_PATH = "src/main/resources/permanent_allowed_notification_items.json";

    private static final String SUBSCRIBE_SIGN = "\u2796 ";
    private static final String UNSUBSCRIBE_SIGN = "\u2795 ";
    private static final String NEW_LINE = "\r\n";
    private static final String SUBSCRIPTION_FORMAT = "\u2022 %s +%s";
    private static final String FIND_ITEM_MSG = "Результат поиска:";
    private static final String FIND_ITEM_TOO_LONG_ITEMS = "Ничего не найдено. Введите точное название предмета.";
    private static final String SUBSCRIBE_MSG_ALERT = "Вы подписались на уведомления.";
    private static final String UNSUBSCRIBE_MSG_ALERT = "Вы отписались от уведомлений.";
    private static final String UNABLE_TO_SUBSCRIBE = "Невозможно подписаться на уведомления: ";
    private static final String ZERO_SUBSCRIPTIONS_FOUND = "Вы не подписаны на уведомления.";

    private static final String MULTIPLE_ITEMS_FOUND = "Найдено несколько предметов. Для поиска конкретного предмета "
                                                     + "введите полное название.";
    private static final String NOTIFICATION_FORMAT = "Тип уведомлений: %s";
    private final static String SEARCH_FORMAT = "Название предмета: %s%n%n"
                                              + "Уведомления:";


    @Autowired
    public BotService(SubsRepository repository,
                      MarketService marketService) {
        this.repository = repository;
        this.marketService = marketService;
    }

    @PostConstruct
    private void postConstruct() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            dictionary = mapper.readValue(Paths.get(WAIT_LIST_ALLOWED_PATH).toFile(), AllowedNotifyItem[].class);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public SendMessage findItem(long chatId, String name) {
        SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));

        SearchItemInfo itemInfo = marketService.find(name);
        if (itemInfo == null) {
            response.setText(Constants.EMPTY_SEARCH_RESULT);
            return response;
        }

        if (itemInfo.isFullResponse()) {
            DetailListItem item = itemInfo.getDetailListItem().iterator().next();
            response.setText(String.format(SEARCH_FORMAT, item.getName()));

            setInitialButtons(response, item.getMainKey());
            return response;
        }

        StringBuilder resultBuilder = new StringBuilder(FIND_ITEM_MSG).append(NEW_LINE);
        for (DetailListItem item : itemInfo.getDetailListItem()) {
            resultBuilder.append(item.getName()).append(NEW_LINE);
        }
        resultBuilder.append(NEW_LINE).append(MULTIPLE_ITEMS_FOUND);

        if (resultBuilder.toString().length() > MAX_TEXT_LENGTH) {
            response.setText(FIND_ITEM_TOO_LONG_ITEMS);
        } else {
            response.setText(resultBuilder.toString());
        }
        return response;
    }

    public CallbackQueryResult processCallbackQuery(Update query) {
        ButtonCallbackData data;
        try {
            data = mapper.readValue(query.getCallbackQuery().getData(), ButtonCallbackData.class);
        } catch (JsonProcessingException e) {
            log.error("Could not read data: ", e);
            return null;
        }
        EditMessageText editMessage = new EditMessageText();
        long chatId = query.getCallbackQuery().getMessage().getChatId();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setChatId(String.valueOf(query.getCallbackQuery().getMessage().getChatId()));
        editMessage.setMessageId(query.getCallbackQuery().getMessage().getMessageId());
        editMessage.setText(query.getCallbackQuery().getMessage().getText());

        AnswerCallbackQuery callbackQuery = new AnswerCallbackQuery();
        callbackQuery.setCallbackQueryId(String.valueOf(query.getCallbackQuery().getId()));
        executeCallback(data, callbackQuery, chatId);
        updateMessage(data, editMessage);
        return new CallbackQueryResult(editMessage, callbackQuery);
    }

    public List<SendMessage> getAllSubscriptions(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        List<WaitListSub> waitListSubs = repository.getSubscriptionsWaitList(new WaitListSub(chatId));
        List<OnChangePriceSub> onChangePriceSubs = repository.getSubscriptionsOnChangePrice(new OnChangePriceSub(chatId));

        StringBuilder responseBuilder = new StringBuilder();
        if (CollectionUtils.isEmpty(waitListSubs) && CollectionUtils.isEmpty(onChangePriceSubs)) {
            responseBuilder.append(ZERO_SUBSCRIPTIONS_FOUND);
        } else {
            if (!CollectionUtils.isEmpty(waitListSubs)) {
                responseBuilder.append(String.format(NOTIFICATION_FORMAT, ButtonAction.S_WAIT_LIST.getValue()));
                responseBuilder.append(NEW_LINE);
                for (WaitListSub waitListSub : waitListSubs) {
                    String itemInfo = String.format(SUBSCRIPTION_FORMAT,
                            marketService.getItemNameById(waitListSub.getItemId()), waitListSub.getEnhancement());
                    responseBuilder.append(itemInfo);
                    responseBuilder.append(NEW_LINE);
                }
                responseBuilder.append(NEW_LINE);
            }
            if (!CollectionUtils.isEmpty(onChangePriceSubs)) {
                responseBuilder.append(String.format(NOTIFICATION_FORMAT, ButtonAction.S_CHANGE_PRICE.getValue()));
                responseBuilder.append(NEW_LINE);
                for (OnChangePriceSub onChangePriceSub : onChangePriceSubs) {
                    String itemInfo = String.format(SUBSCRIPTION_FORMAT,
                            marketService.getItemNameById(onChangePriceSub.getItemId()), onChangePriceSub.getEnhancement());
                    responseBuilder.append(itemInfo);
                    responseBuilder.append(NEW_LINE);
                }
            }
        }
        List<SendMessage> response = new ArrayList<>();
        splitMessage(String.valueOf(chatId), responseBuilder.toString(), response);
        return response;
    }

    private void setInitialButtons(SendMessage message, long itemId) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        rowInline.add(getSubscribeWaitListButton(itemId));
        rowInline.add(getSubscribeOnChangePriceButton(itemId));

        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
    }

    private void executeCallback(ButtonCallbackData data, AnswerCallbackQuery callback, long chatId) {
        switch (data.getAction()) {
            case S_WAIT_LIST_ITEM: {
                boolean subscribed = isAlreadySubscribedWaitList(data.getItemId(), data.getEnhancement(), chatId);
                if (!subscribed) {
                    subscribe(new WaitListSub(data.getItemId(), data.getEnhancement(), chatId));
                    callback.setText(SUBSCRIBE_MSG_ALERT);
                } else {
                    unSubscribe(new WaitListSub(data.getItemId(), data.getEnhancement(), chatId));
                    callback.setText(UNSUBSCRIBE_MSG_ALERT);
                }
                break;
            }
            case S_CHANGE_PRICE_ITEM: {
                boolean subscribed = isAlreadySubscribedOnChangePrice(data.getItemId(), data.getEnhancement(), chatId);
                if (!subscribed) {
                    PriceRestriction priceRestriction =
                            marketService.getPriceRestrictions(data.getItemId(), data.getEnhancement());
                    if (priceRestriction == null) {
                        log.error(UNABLE_TO_SUBSCRIBE + Constants.UNEXPECTED_ERROR_OCCURRED);
                    } else {
                        subscribe(new OnChangePriceSub(data.getItemId(), data.getEnhancement(),
                                priceRestriction.getMaxPrice(), chatId));
                        callback.setText(SUBSCRIBE_MSG_ALERT);
                    }
                } else {
                    unSubscribe(new OnChangePriceSub(data.getItemId(), data.getEnhancement(), chatId));
                    callback.setText(UNSUBSCRIBE_MSG_ALERT);
                }
                break;
            }
        }
    }

    private void updateMessage(ButtonCallbackData data, EditMessageText editMessage) {
        final int BUTTONS_IN_LINE = 6;
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        if (data.getAction() == ButtonAction.BACK) {
            rowsInline.add(Collections.singletonList(getSubscribeWaitListButton(data.getItemId())));
            rowsInline.add(Collections.singletonList(getSubscribeOnChangePriceButton(data.getItemId())));
        } else {
            List<InlineKeyboardButton> buttons =
                    getSubscriptionOptionsRowButton(data, Long.parseLong(editMessage.getChatId()));
            rowsInline.addAll(ListUtils.partition(buttons, BUTTONS_IN_LINE));
            rowsInline.add(getBackRowButton(data));
        }

        markupInline.setKeyboard(rowsInline);
        editMessage.setReplyMarkup(markupInline);
    }

    private boolean isNotAllowedWaitListSubscription(long itemId, int enhancement) {
        if (dictionary == null) {
            postConstruct();
        }
        if (dictionary != null) {
            return Arrays.stream(dictionary)
                    .noneMatch(item -> item.getId() == itemId && item.getEnhancement() == enhancement);
        }
        return true;
    }

    private boolean isAlreadySubscribedWaitList(long itemId, int enhancement, long chatId) {
        List<WaitListSub> subs = repository.getSubscribersWaitList(new WaitListSub(itemId, enhancement));
        return subs.stream().anyMatch(sub -> sub.getChatId() == chatId);
    }

    private boolean isAlreadySubscribedOnChangePrice(long itemId, int enhancement, long chatId) {
        List<OnChangePriceSub> subs = repository.getSubscribersOnChangePrice(new OnChangePriceSub(itemId, enhancement));
        return subs.stream().anyMatch(sub -> sub.getChatId() == chatId);
    }

    private List<InlineKeyboardButton> getSubscriptionOptionsRowButton(ButtonCallbackData callback, long chatId) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        String itemName = marketService.getItemNameById(callback.getItemId());
        SearchItemInfo itemInfo = marketService.find(itemName);
        if (itemInfo == null) {
            return Collections.emptyList();
        }

        List<DetailListItem> items = itemInfo.getDetailListItem();
        switch (callback.getAction()) {
            case S_WAIT_LIST:
            case S_WAIT_LIST_ITEM: {
                for (DetailListItem item : items) {
                    if (isNotAllowedWaitListSubscription(item.getMainKey(), (int) item.getSubKey())) {
                        continue;
                    }
                    boolean subscribed = isAlreadySubscribedWaitList(item.getMainKey(), (int) item.getSubKey(), chatId);
                    row.add(getSubscriptionOptionWaitList(item.getMainKey(), (int) item.getSubKey(), subscribed));
                }
                break;
            }
            case S_CHANGE_PRICE:
            case S_CHANGE_PRICE_ITEM: {
                for (DetailListItem item : items) {
                    boolean subscribed = isAlreadySubscribedOnChangePrice(item.getMainKey(), (int) item.getSubKey(), chatId);
                    row.add(getSubscriptionOptionOnChangePrice(item.getMainKey(), (int) item.getSubKey(), subscribed));
                }
                break;
            }
        }

        return row;
    }

    @SneakyThrows
    private InlineKeyboardButton getSubscriptionOptionWaitList(long itemId, int enhancement, boolean subscribed) {
        ButtonCallbackData data = new ButtonCallbackData(itemId, enhancement, ButtonAction.S_WAIT_LIST_ITEM);

        InlineKeyboardButton btn = new InlineKeyboardButton();
        String sign = subscribed ? SUBSCRIBE_SIGN : UNSUBSCRIBE_SIGN;
        btn.setText(sign + enhancement);
        btn.setCallbackData(mapper.writeValueAsString(data));
        return btn;
    }

    @SneakyThrows
    private InlineKeyboardButton getSubscriptionOptionOnChangePrice(long itemId, int enhancement, boolean subscribed) {
        ButtonCallbackData data = new ButtonCallbackData(itemId, enhancement, ButtonAction.S_CHANGE_PRICE_ITEM);

        InlineKeyboardButton btn = new InlineKeyboardButton();
        String sign = subscribed ? SUBSCRIBE_SIGN + " " : UNSUBSCRIBE_SIGN;
        btn.setText(sign + enhancement);
        btn.setCallbackData(mapper.writeValueAsString(data));
        return btn;
    }

    @SneakyThrows
    private List<InlineKeyboardButton> getBackRowButton(ButtonCallbackData data) {
        ButtonCallbackData newButtonData = new ButtonCallbackData(data.getItemId(), ButtonAction.BACK);
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(String.valueOf(newButtonData.getAction().getValue()));
        btn.setCallbackData(mapper.writeValueAsString(newButtonData));

        return Collections.singletonList(btn);
    }

    @SneakyThrows
    private InlineKeyboardButton getSubscribeWaitListButton(long itemId) {
        ButtonCallbackData data = new ButtonCallbackData(itemId, ButtonAction.S_WAIT_LIST);

        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(data.getAction().getValue());
        btn.setCallbackData(mapper.writeValueAsString(data));

        return btn;
    }

    @SneakyThrows
    private InlineKeyboardButton getSubscribeOnChangePriceButton(long itemId) {
        ButtonCallbackData data = new ButtonCallbackData(itemId, ButtonAction.S_CHANGE_PRICE);

        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(data.getAction().getValue());
        btn.setCallbackData(mapper.writeValueAsString(data));

        return btn;
    }

    private void splitMessage(String chatId, String msgText, List<SendMessage> response) {
        if (msgText.length() > MAX_TEXT_LENGTH) {
            String text = msgText.substring(0, MAX_TEXT_LENGTH);
            String newMessage = text.substring(0, text.lastIndexOf(NEW_LINE));
            response.add(new SendMessage(chatId, newMessage));
            splitMessage(chatId, msgText.substring(newMessage.length() + NEW_LINE.length()), response);
        } else {
            response.add(new SendMessage(chatId, msgText));
        }
    }

    public void subscribe(WaitListSub item) {
        repository.subscribeWaitList(item);
    }

    public void unSubscribe(WaitListSub item) {
        repository.unsubscribeWaitList(item);
    }

    public void subscribe(OnChangePriceSub item) {
        repository.subscribeOnChangePrice(item);
    }

    public void unSubscribe(OnChangePriceSub item) {
        repository.unsubscribeOnChangePrice(item);
    }

    public void unsubscribeAll(long chatId) {
        repository.unsubscribeAll(chatId);
    }

    public void addUser(long userId, long chatId, String userName) {
        repository.addUser(userId, chatId, userName);
    }

    public void logUserMessage(long chatId, String userName, String message, long userId) {
        repository.logUserMessage(chatId, userName, message, userId);
    }
}
