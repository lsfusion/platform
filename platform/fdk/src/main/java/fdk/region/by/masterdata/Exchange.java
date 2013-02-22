package fdk.region.by.masterdata;

import java.sql.Date;

public class Exchange {
    String currencyID;
    String homeCurrencyID;
    Date date;
    Double exchangeRate;

    public Exchange(String currencyID, String homeCurrencyID, Date date, Double exchangeRate) {
        this.currencyID = currencyID;
        this.homeCurrencyID = homeCurrencyID;
        this.date = date;
        this.exchangeRate = exchangeRate;
    }
}
