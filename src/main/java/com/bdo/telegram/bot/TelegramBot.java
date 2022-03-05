package com.bdo.telegram.bot;

import com.bdo.telegram.dto.CallbackQueryResult;
import com.bdo.telegram.service.BotService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final String UNSUBSCRIBE_ALL_BTN = "Отписаться от всех уведомлений";
    private static final String GET_ALL_SUBSCRIPTIONS_BTN = "Мои уведомления";
    private static final String MENU = "/menu";
    private static final String START = "/start";
    private static final String DONATE = "Поддержать разработчика";
    private static final String UNSUBSCRIBE_ALL_MSG = "Вы отписались от всех уведомлений";
    private static final String START_BTN_MSG = "Введите название предмета для поиска на центральном аукционе.";
    private static final String MENU_BTN_TEXT = "Меню";
    private static final String CHAT_NOT_FOUND_ERROR = "Bad Request: chat not found";

    private final BotService botService;

    private final String botUsername;
    private final String botToken;
    private final String yooMoneyUrl;

    public TelegramBot(TelegramBotsApi telegramBotsApi,
                       @Value("${bot.name}") String botUsername,
                       @Value("${bot.token}") String botToken,
                       @Value("${yooMoneyUrl}") String yooMoneyUrl,
                       BotService botService) throws TelegramApiException {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.botService = botService;
        this.yooMoneyUrl = yooMoneyUrl;

        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update request) {
        if (request.hasMessage() && request.getMessage().hasText()) {
            Message requestMessage = request.getMessage();
            log.info("new message: text[{}], user[{}]",
                    requestMessage.getText(), requestMessage.getFrom().toString());

            switch (requestMessage.getText()) {
                case START: {
                    start(requestMessage.getChatId());
                    botService.addUser(requestMessage.getFrom().getId(), requestMessage.getChatId(),
                            getUserName(requestMessage.getFrom()));
                    break;
                }
                case MENU: {
                    getMenu(requestMessage.getChatId());
                    break;
                }
                case DONATE: {
                    donate(requestMessage.getChatId(), requestMessage.getMessageId());
                    break;
                }
                case UNSUBSCRIBE_ALL_BTN: {
                    botService.unsubscribeAll(requestMessage.getChatId());
                    delete(requestMessage.getChatId(), requestMessage.getMessageId());
                    notify(requestMessage.getChatId(), UNSUBSCRIBE_ALL_MSG);
                    break;
                }
                case GET_ALL_SUBSCRIPTIONS_BTN: {
                    for (SendMessage message : botService.getAllSubscriptions(requestMessage.getChatId())) {
                        execute(message);
                    }
                    delete(requestMessage.getChatId(), requestMessage.getMessageId());
                    break;
                }
                default: {
                    search(requestMessage.getChatId(), requestMessage.getText());
                    break;
                }
            }

            String user = getUserName(requestMessage.getFrom());
            botService.logUserMessage(requestMessage.getChatId(), user,
                    requestMessage.getText(), requestMessage.getFrom().getId());
        } else if (request.hasCallbackQuery()) {
            log.info("new button action: data[{}], callback user[{}]",
                    request.getCallbackQuery().getData(), request.getCallbackQuery().getFrom().toString());

            CallbackQueryResult result = botService.processCallbackQuery(request);
            execute(result.getMessage());
            if (result.getCallbackQuery() != null) {
                sendAlert(result.getCallbackQuery());
            }
        }
    }

    public void notify(long chatId, String text) {
        final SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));
        response.setText(text);

        execute(response);
    }

    public void sendAlert(AnswerCallbackQuery callbackQuery) {
        try {
            callbackQuery.setShowAlert(true);
            execute(callbackQuery);
        } catch (Exception e) {
            String error = String.format("Error execute answerCallbackQuery [callback_id:%s, text:%s]",
                    callbackQuery.getCallbackQueryId(), callbackQuery.getText());
            log.error(error, e);
        }
    }

    private void start(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(START_BTN_MSG);
        addMenu(message);
        execute(message);
    }

    private void getMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(MENU_BTN_TEXT);
        addMenu(message);
        execute(message);
    }

    private void donate(long chatId, int messageId) {
        delete(chatId, messageId);

        String text = String.format("yooMoney: %s", yooMoneyUrl);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.disableWebPagePreview();
        execute(message);
    }

    private void search(long chatId, String name) {
        execute(botService.findItem(chatId, name));
    }

    private void execute(SendMessage message) {
        try {
            message.setProtectContent(true);
            super.execute(message);
        } catch (TelegramApiRequestException e) {
            if (e.getApiResponse().equals(CHAT_NOT_FOUND_ERROR)) {
                botService.unsubscribeAll(Long.parseLong(message.getChatId()));
            }
        } catch (Exception e) {
            String error = String.format("Error execute message [chat_id: %s, text: %s]",
                    message.getChatId(), message.getText());
            log.error(error, e);
        }
    }

    private void execute(EditMessageText message) {
        try {
            super.execute(message);
        } catch (Exception e) {
            String error = String.format("Error execute message [chat_id: %s, text: %s]",
                    message.getChatId(), message.getText());
            log.error(error, e);
        }
    }

    private void delete(long chatId, int messageId) {
        try {
            DeleteMessage delete = new DeleteMessage(String.valueOf(chatId), messageId);
            execute(delete);
        } catch (Exception e) {
            String error = String.format("Error deleting message [chat_id:%s, message_id:%s]",
                    chatId, messageId);
            log.error(error, e);
        }
    }

    private void addMenu(SendMessage message) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        message.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> btnRowList = new ArrayList<>();
        KeyboardRow btnRow = new KeyboardRow();

        btnRow.add(new KeyboardButton(UNSUBSCRIBE_ALL_BTN));
        btnRow.add(new KeyboardButton(GET_ALL_SUBSCRIPTIONS_BTN));
        btnRow.add(new KeyboardButton(DONATE));
        btnRowList.add(btnRow);
        replyKeyboardMarkup.setKeyboard(btnRowList);
    }

    private String getUserName(User user) {
        StringBuilder userBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(user.getUserName())) {
            userBuilder.append(user.getUserName()).append("/");
        }
        if (StringUtils.isNotBlank(user.getFirstName())) {
            userBuilder.append(user.getFirstName()).append("/");
        }
        if (StringUtils.isNotBlank(user.getLastName())) {
            userBuilder.append(user.getLastName()).append("/");
        }
        return userBuilder.substring(0, userBuilder.toString().length() - 1);
    }
}
