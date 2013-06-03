package lsfusion.erp.region.by.masterdata;

import java.math.BigDecimal;
import java.sql.Date;

public class Exchange {
    String currencyID;
    String homeCurrencyID;
    Date date;
    BigDecimal exchangeRate;

    public Exchange(String currencyID, String homeCurrencyID, Date date, BigDecimal exchangeRate) {
        this.currencyID = currencyID;
        this.homeCurrencyID = homeCurrencyID;
        this.date = date;
        this.exchangeRate = exchangeRate;
    }
}
