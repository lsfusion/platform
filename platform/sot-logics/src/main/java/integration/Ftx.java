package integration;

import java.util.Date;

public class Ftx {
    public String unpLegalEntity1;
    public String unpLegalEntity2;
    public Integer monthNumber;
    public String storeID;
    public String numberDocument;
    public Date dateDocument;
    public Double sumDocument;
    public Double sumWareDocument;
    public Integer entryNumber;
    public String operationCode;
    public String correspondingAccountCode;
    public String analyticalAccountCode;
    public Double sumEntry;
    public Double sumWareEntry;
    public Integer mainEntryAccountingInformationNumber;
    public String debitAnalytics;
    public String creditAnalytics;
    public Double financialSum;

    public Ftx(String unpLegalEntity1, String unpLegalEntity2, Integer monthNumber, String storeID, String numberDocument,
               Date dateDocument, Double sumDocument, Double sumWareDocument, Integer entryNumber, String operationCode,
               String correspondingAccountCode, String analyticalAccountCode, Double sumEntry, Double sumWareEntry,
               Integer mainEntryAccountingInformationNumber, String debitAnalytics, String creditAnalytics, Double financialSum) {
        this.unpLegalEntity1 = unpLegalEntity1;
        this.unpLegalEntity2 = unpLegalEntity2;
        this.monthNumber = monthNumber;
        this.storeID = storeID;
        this.numberDocument = numberDocument;
        this.dateDocument = dateDocument;
        this.sumDocument = sumDocument;
        this.sumWareDocument = sumWareDocument;
        this.entryNumber = entryNumber;
        this.operationCode = operationCode;
        this.correspondingAccountCode = correspondingAccountCode;
        this.analyticalAccountCode = analyticalAccountCode;
        this.sumEntry = sumEntry;
        this.sumWareEntry = sumWareEntry;
        this.mainEntryAccountingInformationNumber = mainEntryAccountingInformationNumber;
        this.debitAnalytics = debitAnalytics;
        this.creditAnalytics = creditAnalytics;
        this.financialSum = financialSum;
    }
}
