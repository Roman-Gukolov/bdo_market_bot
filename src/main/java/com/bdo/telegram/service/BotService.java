package com.bdo.telegram.service;

import com.bdo.model.db.ChangePrice;
import com.bdo.model.db.Log;
import com.bdo.model.db.User;
import com.bdo.model.db.WaitList;
import com.bdo.market.Constants;
import com.bdo.market.service.MarketService;
import com.bdo.model.market.DetailListItem;
import com.bdo.model.market.PriceRestriction;
import com.bdo.model.market.SearchItemInfo;
import com.bdo.repository.ChangePriceRepository;
import com.bdo.repository.LogRepository;
import com.bdo.repository.UserRepository;
import com.bdo.repository.WaitListRepository;
import com.bdo.model.telegram.ButtonAction;
import com.bdo.model.telegram.ButtonCallbackData;
import com.bdo.model.telegram.CallbackQueryResult;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@NoArgsConstructor
@Slf4j
public class BotService {

    private static final long MIN_PRICE_FOR_WAIT_LIST = 20_000_000_000L;
    private WaitListRepository waitListRepository;
    private ChangePriceRepository changePriceRepository;
    private LogRepository logRepository;
    private UserRepository userRepository;
    private MarketService marketService;
    private ObjectMapper mapper;

    private static final int MAX_TEXT_LENGTH = 4096;

    private static final String SUBSCRIBE_SIGN = "\u2796 ";
    private static final String UNSUBSCRIBE_SIGN = "\u2795 ";
    private static final String NEW_LINE = "\r\n";
    private static final String SUBSCRIPTION_FORMAT = "\u2022 %s +%s";
    private static final String FIND_ITEM_MSG = "Результат поиска:";
    private static final String FIND_ITEM_TOO_LONG_ITEMS = "Ничего не найдено. Введите точное название предмета.";
    private static final String SUBSCRIBE_MSG_ALERT = "Вы подписались на уведомления.";
    private static final String UNSUBSCRIBE_MSG_ALERT = "Вы отписались от уведомлений.";
    private static final String UNABLE_TO_SUBSCRIBE = "Невозможно подписаться на уведомления: "
                                                    + "Не удалось получить данные с Центрального аукциона.";
    private static final String ZERO_SUBSCRIPTIONS_FOUND = "Вы не подписаны на уведомления.";

    private static final String MULTIPLE_ITEMS_FOUND = "Найдено несколько предметов. Для поиска конкретного предмета "
                                                     + "введите полное название.";
    private static final String NOTIFICATION_FORMAT = "Тип уведомлений: %s";
    private final static String SEARCH_FORMAT = "Название предмета: %s%n%n"
                                              + "Уведомления:";

    @Autowired
    public BotService(WaitListRepository waitListRepository,
                      ChangePriceRepository changePriceRepository,
                      LogRepository logRepository,
                      UserRepository userRepository,
                      MarketService marketService) {
        this.waitListRepository = waitListRepository;
        this.changePriceRepository = changePriceRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.marketService = marketService;
    }

    @PostConstruct
    private void postConstruct() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        List<WaitList> waitLists = waitListRepository.findByChatId(chatId);
        List<ChangePrice> changePrices = changePriceRepository.findByChatId(chatId);

        StringBuilder responseBuilder = new StringBuilder();
        if (CollectionUtils.isEmpty(waitLists) && CollectionUtils.isEmpty(changePrices)) {
            responseBuilder.append(ZERO_SUBSCRIPTIONS_FOUND);
        } else {
            if (!CollectionUtils.isEmpty(waitLists)) {
                responseBuilder.append(String.format(NOTIFICATION_FORMAT, ButtonAction.S_WAIT_LIST.getValue()));
                responseBuilder.append(NEW_LINE);
                for (WaitList waitList : waitLists) {
                    String itemInfo = String.format(SUBSCRIPTION_FORMAT,
                            marketService.getItemNameById(waitList.getItemId()), waitList.getEnhancement());
                    responseBuilder.append(itemInfo);
                    responseBuilder.append(NEW_LINE);
                }
                responseBuilder.append(NEW_LINE);
            }
            if (!CollectionUtils.isEmpty(changePrices)) {
                responseBuilder.append(String.format(NOTIFICATION_FORMAT, ButtonAction.S_CHANGE_PRICE.getValue()));
                responseBuilder.append(NEW_LINE);
                for (ChangePrice changePrice : changePrices) {
                    String itemInfo = String.format(SUBSCRIPTION_FORMAT,
                            marketService.getItemNameById(changePrice.getItemId()), changePrice.getEnhancement());
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
                    subscribe(new WaitList(data.getItemId(), data.getEnhancement(), chatId));
                    callback.setText(SUBSCRIBE_MSG_ALERT);
                } else {
                    unSubscribeWaitList(data.getItemId(), data.getEnhancement(), chatId);
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
                        log.error(UNABLE_TO_SUBSCRIBE);
                    } else {
                        subscribe(new ChangePrice(data.getItemId(), data.getEnhancement(),
                                priceRestriction.getMaxPrice(), chatId));
                        callback.setText(SUBSCRIBE_MSG_ALERT);
                    }
                } else {
                    unSubscribeChangePrice(data.getItemId(), data.getEnhancement(), chatId);
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

    private boolean isAlreadySubscribedWaitList(long itemId, int enhancement, long chatId) {
        List<WaitList> subs = waitListRepository.findByItemIdAndEnhancement(itemId, enhancement);
        return subs.stream().anyMatch(sub -> sub.getChatId() == chatId);
    }

    private boolean isAlreadySubscribedOnChangePrice(long itemId, int enhancement, long chatId) {
        List<ChangePrice> subs = changePriceRepository.findByItemIdAndEnhancement(itemId, enhancement);
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
                    if (item.getPricePerOne() < MIN_PRICE_FOR_WAIT_LIST) {
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

    public void subscribe(WaitList item) {
        waitListRepository.save(item);
    }

    public void unSubscribeWaitList(long itemId, int enhancement, long chatId) {
        waitListRepository.deleteByItemIdAndEnhancementAndChatId(itemId, enhancement, chatId);
    }

    public void subscribe(ChangePrice item) {
        changePriceRepository.save(item);
    }

    public void unSubscribeChangePrice(long itemId, int enhancement, long chatId) {
        changePriceRepository.deleteByItemIdAndEnhancementAndChatId(itemId, enhancement, chatId);
    }

    public void unsubscribeAll(long chatId) {
        waitListRepository.deleteAllByChatId(chatId);
        changePriceRepository.deleteAllByChatId(chatId);
    }

    public void addUser(long chatId, String userName) {
        userRepository.save(new User(chatId, userName));
    }

    public void logUserMessage(long chatId, String userName, String message) {
        logRepository.save(new Log(chatId, userName, message));
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
}
