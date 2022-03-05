package com.bdo.market.service.parser;

import com.bdo.market.dto.MarketApiResponse;
import com.bdo.market.dto.WaitListItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class MarketApiParser {
    private static final String ROW_DELIMITER = "|";
    private static final String PARAM_DELIMITER = "-";

    public static List<WaitListItem> parseWaitListResponse(MarketApiResponse data) {
        if (isEmpty(data.getResultMsg())) {
            return Collections.emptyList();
        }
        List<WaitListItem> items = new ArrayList<>();
        parseDataToItems(items, data.getResultMsg(), ROW_DELIMITER);

        return items;
    }

    private static void parseDataToItems(List<WaitListItem> items, String data, String delim) {
        StringTokenizer tokenizer = new StringTokenizer(data, delim);
        while (tokenizer.hasMoreTokens()) {
            if (delim.equals("-")) {
                List<String> collection = new ArrayList<>();
                while (tokenizer.hasMoreTokens()) {
                    collection.add(tokenizer.nextToken());
                }
                items.add(mapToWaitListItem(collection));
            } else {
                parseDataToItems(items, tokenizer.nextToken(), PARAM_DELIMITER);
            }
        }
    }

    private static WaitListItem mapToWaitListItem(List<String> stringToMapping) {
        if (isEmpty(stringToMapping)) {
            return new WaitListItem();
        }
        return new WaitListItem(
                Long.parseLong(stringToMapping.get(0)),
                null,
                Integer.parseInt(stringToMapping.get(1)),
                Long.parseLong(stringToMapping.get(2)),
                Long.parseLong(stringToMapping.get(3)));
    }

    private static boolean isEmpty(List<String> stringToMapping) {
        return CollectionUtils.isEmpty(stringToMapping)
                || (stringToMapping.size() == 1 && stringToMapping.get(0).equals("0"));
    }

    private static boolean isEmpty(String stringToParse) {
        return StringUtils.isBlank(stringToParse) || stringToParse.equals("0");
    }
}
