package com.bdo.market.service;

import com.bdo.market.Constants;
import com.bdo.market.client.MarketApi;
import com.bdo.market.service.parser.MarketApiParser;
import com.bdo.model.market.*;
import com.google.common.collect.Lists;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@NoArgsConstructor
@Slf4j
public class MarketService {

    private MarketApi marketApi;

    private static final String EMPTY_WAIT_LIST_HAS_BEEN_RETURNED = "Empty wait list has been returned";
    private static final String TRADE_MARKET_NOT_FOUND_ERROR_OCCURRED = "Не удалось получить данные о предмете " +
            "с Центрального аукциона.";
    private static final List<String> matchingNames = Lists.newArrayList("Меч Черной звезды",
            "Лук Черной звезды", "Клинок Черной звезды");

    @Autowired
    public MarketService(MarketApi marketApi) {
        this.marketApi = marketApi;
    }

    public List<WaitListItem> getWaitList() {
        MarketResponse<MarketApiResponse> waitList = marketApi.getWaitList();
        if (waitList.getServiceError() != null) {
            log.error(waitList.getServiceError().toString());
            return Collections.emptyList();
        }
        List<WaitListItem> waitListItems =  MarketApiParser.parseWaitListResponse(waitList.getData());
        if (CollectionUtils.isEmpty(waitListItems)) {
            log.error(EMPTY_WAIT_LIST_HAS_BEEN_RETURNED);
            return Collections.emptyList();
        }
        mapItemNames(waitListItems);
        return waitListItems;
    }

    public PriceRestriction getPriceRestrictions(long itemId, int enhancement) {
        MarketResponse<MarketApiResponse> itemInfo = marketApi.getDetailedInfo(itemId);
        if (itemInfo.getServiceError() != null) {
            log.error(itemInfo.getServiceError().toString());
            return null;
        }
        if (itemInfo.getData() == null || ArrayUtils.isEmpty(itemInfo.getData().getDetailList())) {
            log.error(TRADE_MARKET_NOT_FOUND_ERROR_OCCURRED);
            return null;
        }

        DetailListItem item = Arrays
                .stream(itemInfo.getData().getDetailList())
                .filter(Objects::nonNull).filter(info -> info.getSubKey() == enhancement)
                .findFirst().orElse(null);

        if (item == null) {
            log.error(Constants.EMPTY_SEARCH_RESULT);
            return null;
        }
        MarketResponse<MarketApiResponse> priceRestrictionsResponse = marketApi.getPriceRestrictions(item);
        if (priceRestrictionsResponse.getServiceError() != null) {
            log.error(priceRestrictionsResponse.getServiceError().toString());
            return null;
        }
        if (priceRestrictionsResponse.getData() == null
                || ArrayUtils.isEmpty(priceRestrictionsResponse.getData().getMarketConditionList())) {
            log.error(TRADE_MARKET_NOT_FOUND_ERROR_OCCURRED);
            return null;
        }
        return getPriceRestrictions(Arrays.asList(priceRestrictionsResponse.getData().getMarketConditionList()));
    }

    public SearchItemInfo find(String name) {
        MarketResponse<MarketApiResponse> searchResponse = marketApi.find(name);
        if (searchResponse.getServiceError() != null) {
            log.error(searchResponse.getServiceError().toString());
            return null;
        }
        if (searchResponse.getData() == null || ArrayUtils.isEmpty(searchResponse.getData().getList())) {
            log.error(Constants.EMPTY_SEARCH_RESULT);
            return null;
        }

        SearchItem[] data = searchResponse.getData().getList();
        if (data.length > 1) {
            if (matchingNames.stream().anyMatch(i -> i.equalsIgnoreCase(name))) {
                for (SearchItem item : data) {
                    if (item.getName().equalsIgnoreCase(name)) {
                        return getItemInfo(item);
                    }
                }
            } else {
                SearchItemInfo result = new SearchItemInfo();
                result.setFullResponse(false);

                List<DetailListItem> items = Arrays.stream(data)
                        .map(item -> new DetailListItem(item.getName()))
                        .collect(Collectors.toList());
                result.setDetailListItem(items);
                return result;
            }
        } else {
            return getItemInfo(data[0]);
        }
        return null;
    }

    public String getItemNameById(long id) {
        List<DetailListItem> items = getDetailListItem(id);
        return items.stream().findFirst().map(DetailListItem::getName).orElse(null);
    }

    private List<DetailListItem> getDetailListItem(long itemId) {
        MarketResponse<MarketApiResponse> apiResponse = marketApi.getDetailedInfo(itemId);
        if (apiResponse.getServiceError() != null) {
            log.error(apiResponse.getServiceError().toString());
            return Collections.emptyList();
        }
        if (ArrayUtils.isEmpty(apiResponse.getData().getDetailList())) {
            log.error(TRADE_MARKET_NOT_FOUND_ERROR_OCCURRED);
            return Collections.emptyList();
        }
        return Arrays.asList(apiResponse.getData().getDetailList());
    }

    private SearchItemInfo getItemInfo(SearchItem item) {
        SearchItemInfo result = new SearchItemInfo();
        result.setFullResponse(true);

        List<DetailListItem> items = getDetailListItem(item.getId());
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }
        result.setDetailListItem(items);
        return result;
    }

    private PriceRestriction getPriceRestrictions(List<MarketConditionItem> itemPriceDetails) {
        PriceRestriction priceRestriction = new PriceRestriction();
        try {
            long minPrice = itemPriceDetails.stream()
                    .min(Comparator.comparing(MarketConditionItem::getPricePerOne))
                    .map(MarketConditionItem::getPricePerOne)
                    .orElse(0L);
            long maxPrice = itemPriceDetails.stream()
                    .max(Comparator.comparing(MarketConditionItem::getPricePerOne))
                    .map(MarketConditionItem::getPricePerOne)
                    .orElse(0L);

            priceRestriction.setMinPrice(minPrice);
            priceRestriction.setMaxPrice(maxPrice);
            return priceRestriction;
        } catch (Exception e) {
            log.error("Ошибка получения цен.", e);
            return null;
        }
    }

    private void mapItemNames(List<WaitListItem> list) {
        for (WaitListItem item : list) {
            item.setItemName(getItemNameById(item.getItemId()));
        }
    }
}
