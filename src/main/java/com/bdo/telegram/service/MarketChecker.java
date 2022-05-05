package com.bdo.telegram.service;

import com.bdo.model.db.ChangePrice;
import com.bdo.model.db.WaitList;
import com.bdo.repository.ChangePriceRepository;
import com.bdo.repository.WaitListRepository;
import com.bdo.model.market.PriceRestriction;
import com.bdo.model.market.WaitListItem;
import com.bdo.market.service.MarketService;
import com.bdo.telegram.bot.TelegramBot;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
@Slf4j
public class MarketChecker {

    private WaitListRepository waitListRepository;
    private ChangePriceRepository changePriceRepository;
    private MarketService marketService;
    private TelegramBot tgBot;

    private Set<WaitListItem> notifiedItems = new HashSet<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String WAIT_LIST_INFO = "ПРЕДМЕТ ЗАРЕГИСТРИРОВАН НА ЦЕНТРАЛЬНОМ АУКЦИОНЕ!%n"
                                               + "Название предмета: %s;%n"
                                               + "Уровень усиления: %s;%n"
                                               + "Дата регистрации: %s;";
    private static final String ON_CHANGE_PRICE_INFO = "ЦЕНА ПРЕДМЕТА ИЗМЕНИЛАСЬ!%n"
                                                     + "Название предмета: %s;%n"
                                                     + "Уровень усиления: %s;%n"
                                                     + "Старая цена: %s;%n"
                                                     + "Новая цена: %s;%n";

    @Autowired
    public MarketChecker(WaitListRepository waitListRepository,
                         ChangePriceRepository changePriceRepository,
                         MarketService marketService,
                         TelegramBot tgBot) {
        this.waitListRepository = waitListRepository;
        this.changePriceRepository = changePriceRepository;
        this.marketService = marketService;
        this.tgBot = tgBot;
    }

    @Scheduled(fixedRate = 60000)
    @Async
    public void notifyWaitList() {
        Thread.currentThread().setName("WaitListTask");
        List<WaitListItem> waitList = marketService.getWaitList();
        if (CollectionUtils.isEmpty(waitList)) {
            return;
        }

        for (WaitListItem item : waitList) {
            if (notifiedItems.contains(item)) {
                continue;
            }

            List<WaitList> subscribers = waitListRepository.findByItemIdAndEnhancement(
                    item.getItemId(), item.getEnhancement());
            if (CollectionUtils.isEmpty(subscribers)) {
                continue;
            }

            for (WaitList subscriber : subscribers) {
                String text = String.format(WAIT_LIST_INFO, item.getItemName(), item.getEnhancement(),
                        dateFormat.format(new Date(item.getTime() * 1000)));
                tgBot.notify(subscriber.getChatId(), text);
            }
            notifiedItems.add(item);
        }
    }

    @Scheduled(fixedRate = 1200000)
    public void filterOutdatedItems() {
        notifiedItems = notifiedItems.stream()
                .filter(item -> new Date(item.getTime() * 1000).after(new Date()))
                .collect(Collectors.toSet());
    }

    @Scheduled(fixedRate = 15000)
    @Async
    public void notifyOnChangePrice() {
        Thread.currentThread().setName("ChangePriceTask");

        List<ChangePrice> subscriptions = changePriceRepository.findDistinct();
        if (CollectionUtils.isEmpty(subscriptions)) {
            return;
        }

        for (ChangePrice subscription : subscriptions) {
            PriceRestriction priceRestriction =
                    marketService.getPriceRestrictions(subscription.getItemId(), subscription.getEnhancement());
            if (priceRestriction == null) {
                continue;
            }

            if (priceRestriction.getMaxPrice() != subscription.getMaxPrice()) {
                List<ChangePrice> subscribers = changePriceRepository
                        .findByItemIdAndEnhancement(subscription.getItemId(), subscription.getEnhancement());
                if (CollectionUtils.isEmpty(subscribers)) {
                    return;
                }

                for (ChangePrice sub : subscribers) {
                    String text = String.format(ON_CHANGE_PRICE_INFO,
                            marketService.getItemNameById(subscription.getItemId()),
                            subscription.getEnhancement(), subscription.getMaxPrice(), priceRestriction.getMaxPrice());

                    tgBot.notify(sub.getChatId(), text);
                    changePriceRepository.updateMaxPrice(priceRestriction.getMaxPrice(), subscription.getItemId(),
                            subscription.getEnhancement(), sub.getChatId());
                }
            }
        }
    }
}
