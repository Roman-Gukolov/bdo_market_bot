package com.bdo.market.client;

import com.bdo.market.Constants;
import com.bdo.model.market.DetailListItem;
import com.bdo.model.market.Parameters;
import com.bdo.model.market.ServiceError;
import com.bdo.model.market.MarketResponse;
import com.bdo.model.market.MarketApiResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Slf4j
public class MarketApi {

    private CloseableHttpClient httpClient;
    private final String marketURL;
    private final String rqToken;
    private final String cookie;

    private static final String COMMON_ERROR = "Error receiving data from the Central auction.";
    private static final String GET_TRADE_MARKET_WAIT_LIST = "Trademarket/GetWorldMarketWaitList";
    private static final String GET_HOME_MARKET_SUB_LIST = "Home/GetWorldMarketSubList";
    private static final String GET_ITEM_SELL_BUY_INFO = "Home/GetItemSellBuyInfo";
    private static final String HOME_MARKET_SEARCH = "Home/GetWorldMarketSearchList";

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";

    public MarketApi(@Value("${market.url}") String marketURL,
                     @Value("${market.request_token}") String rqToken,
                     @Value("${market.cookie}") String cookie) {
        this.marketURL = marketURL;
        this.rqToken = rqToken;
        this.cookie = cookie;
    }

    @PostConstruct
    private void postConstruct() {
        httpClient = HttpClientBuilder.create().build();
    }

    public MarketResponse<MarketApiResponse> getWaitList() {
        String getWaitListURL = marketURL + GET_TRADE_MARKET_WAIT_LIST;
        log.info("getting wait list: {}", getWaitListURL);
        MarketResponse<MarketApiResponse> marketResponse = new MarketResponse<>();
        try {
            URI uri = new URIBuilder(getWaitListURL).build();
            HttpPost post = new HttpPost(uri);
            StringEntity entity = new StringEntity(StringUtils.EMPTY);
            post.setEntity(entity);
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            this.post(marketResponse, post);
        } catch (Exception e) {
            log.error(COMMON_ERROR, e);
            return marketResponse.setServiceError(new ServiceError(Constants.UNEXPECTED_ERROR_OCCURRED));
        }
        return marketResponse;
    }

    public MarketResponse<MarketApiResponse> find(String name) {
        String getSearchURL = marketURL + HOME_MARKET_SEARCH;
        log.info("getting item info [{}]: {}", name, getSearchURL);
        MarketResponse<MarketApiResponse> marketResponse = new MarketResponse<>();
        try {
            URI uri = new URIBuilder(getSearchURL).build();
            HttpPost post = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(Parameters.TOKEN_PARAM.getName(), rqToken));
            params.add(new BasicNameValuePair(Parameters.SEARCH_TEXT_PARAM.getName(), name));
            post.setEntity(new UrlEncodedFormEntity(params, UTF_8));

            this.prepareHeaders(post);
            this.post(marketResponse, post);
        } catch (Exception e) {
            log.error(COMMON_ERROR, e);
            return marketResponse.setServiceError(new ServiceError(Constants.UNEXPECTED_ERROR_OCCURRED));
        }
        return marketResponse;
    }

    public MarketResponse<MarketApiResponse> getPriceRestrictions(DetailListItem item) {
        String getSellInfoURL = marketURL + GET_ITEM_SELL_BUY_INFO;
        log.info("getting item info [id:{}]: {}", item.getMainKey(), getSellInfoURL);
        MarketResponse<MarketApiResponse> marketResponse = new MarketResponse<>();
        try {
            URI uri = new URIBuilder(getSellInfoURL).build();
            HttpPost post = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(Parameters.TOKEN_PARAM.getName(), rqToken));
            params.add(new BasicNameValuePair(Parameters.MAIN_KEY_PARAM.getName(), String.valueOf(item.getMainKey())));
            params.add(new BasicNameValuePair(Parameters.IS_UP_PARAM.getName(), Boolean.TRUE.toString()));
            params.add(new BasicNameValuePair(Parameters.SUB_KEY_PARAM.getName(), String.valueOf(item.getSubKey())));
            params.add(new BasicNameValuePair(Parameters.SUB_CATEGORY_PARAM.getName(), String.valueOf(item.getSubCategory())));
            params.add(new BasicNameValuePair(Parameters.MAIN_CATEGORY_PARAM.getName(), String.valueOf(item.getMainCategory())));
            params.add(new BasicNameValuePair(Parameters.KEY_TYPE_PARAM.getName(), String.valueOf(item.getKeyType())));
            post.setEntity(new UrlEncodedFormEntity(params));

            this.prepareHeaders(post);
            this.post(marketResponse, post);
        } catch (Exception e) {
            log.error(COMMON_ERROR, e);
            return marketResponse.setServiceError(new ServiceError(Constants.UNEXPECTED_ERROR_OCCURRED));
        }
        return marketResponse;
    }

    @Cacheable(value = "items")
    public MarketResponse<MarketApiResponse> getDetailedInfo(long itemId) {
        String getSubListURL = marketURL + GET_HOME_MARKET_SUB_LIST;
        log.info("getting item info [id:{}]: {}", itemId, getSubListURL);
        MarketResponse<MarketApiResponse> marketResponse = new MarketResponse<>();
        try {
            URI uri = new URIBuilder(getSubListURL).build();
            HttpPost post = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(Parameters.TOKEN_PARAM.getName(), rqToken));
            params.add(new BasicNameValuePair(Parameters.MAIN_KEY_PARAM.getName(), String.valueOf(itemId)));
            post.setEntity(new UrlEncodedFormEntity(params));

            this.prepareHeaders(post);
            this.post(marketResponse, post);
        } catch (Exception e) {
            log.error(COMMON_ERROR, e);
            return marketResponse.setServiceError(new ServiceError(Constants.UNEXPECTED_ERROR_OCCURRED));
        }
        return marketResponse;
    }

    private void post(MarketResponse<MarketApiResponse> marketResponse, HttpPost post) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CloseableHttpResponse response = httpClient.execute(post);
        if (response != null) {
            String respString = EntityUtils.toString(response.getEntity());
            if (response.getCode() == 200) {
                marketResponse.setData(mapper.readValue(respString, MarketApiResponse.class));
            } else {
                marketResponse.setServiceError(new ServiceError(response.toString()));
                log.info("error post request: {}", respString);
            }
        }
    }

    private void prepareHeaders(HttpPost post) {
        post.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        post.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        post.setHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
        post.setHeader(HttpHeaders.COOKIE, cookie);
    }
}
