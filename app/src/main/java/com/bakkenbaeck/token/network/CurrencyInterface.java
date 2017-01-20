package com.bakkenbaeck.token.network;

import com.bakkenbaeck.token.model.network.Balance;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Single;

public interface CurrencyInterface {

    @GET("/v2/exchange-rates")
    Single<Balance> getBalance(@Query("currency") String currency);
}
