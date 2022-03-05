package com.bdo.telegram.service;

import com.bdo.db.dto.OnChangePriceSub;
import com.bdo.db.dto.WaitListSub;
import com.bdo.db.repository.ISubsRepository;
import com.bdo.db.repository.SubsRepository;
import com.bdo.market.dto.PriceRestriction;
import com.bdo.market.dto.WaitListItem;
import com.bdo.market.service.MarketService;
import com.bdo.telegram.bot.TelegramBot;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private ISubsRepository repository;
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
    public MarketChecker(SubsRepository repository,
                         MarketService marketService,
                         TelegramBot tgBot) {
        this.repository = repository;
        this.marketService = marketService;
        this.tgBot = tgBot;
    }

    @Scheduled(fixedRate = 60000)
    @Async
    public void notifyWaitList() {
        List<WaitListItem> waitList = marketService.getWaitList();
        if (CollectionUtils.isEmpty(waitList)) {
            return;
        }

        for (WaitListItem item : waitList) {
            if (notifiedItems.contains(item)) {
                continue;
            }

            List<WaitListSub> subscribers = repository.getSubscribersWaitList(
                    new WaitListSub(item.getItemId(), item.getEnhancement()));
            if (CollectionUtils.isEmpty(subscribers)) {
                continue;
            }

            for (WaitListSub subscriber : subscribers) {
                String text = String.format(WAIT_LIST_INFO, item.getItemName(), item.getEnhancement(),
                        dateFormat.format(new Date(item.getTime() * 1000)));
                tgBot.notify(subscriber.getChatId(), text);
            }
            notifiedItems.add(item);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void filterOutdatedItems() {
        notifiedItems = notifiedItems.stream().filter(item -> new Date(item.getTime() * 1000).after(new Date()))
                .collect(Collectors.toSet());
    }

    @Scheduled(fixedRate = 15000)
    @Async
    public void notifyOnChangePrice() {
        List<OnChangePriceSub> subscriptions = repository.getSubscriptionsOnChangePrice();
        if (CollectionUtils.isEmpty(subscriptions)) {
            return;
        }

        for (OnChangePriceSub subscription : subscriptions) {
            PriceRestriction priceRestriction =
                    marketService.getPriceRestrictions(subscription.getItemId(), subscription.getEnhancement());
            if (priceRestriction == null) {
                continue;
            }

            if (priceRestriction.getMaxPrice() != subscription.getMaxPrice()) {
                List<OnChangePriceSub> subscribers = repository.getSubscribersOnChangePrice(
                        new OnChangePriceSub(subscription.getItemId(), subscription.getEnhancement()));
                if (CollectionUtils.isEmpty(subscribers)) {
                    return;
                }

                for (OnChangePriceSub sub : subscribers) {
                    String text = String.format(ON_CHANGE_PRICE_INFO,
                            marketService.getItemNameById(subscription.getItemId()),
                            subscription.getEnhancement(), subscription.getMaxPrice(), priceRestriction.getMaxPrice());

                    tgBot.notify(sub.getChatId(), text);
                    repository.updatePrice(new OnChangePriceSub(subscription.getItemId(), subscription.getEnhancement(),
                            priceRestriction.getMaxPrice(), sub.getChatId()));
                }
            }
        }
    }
}
