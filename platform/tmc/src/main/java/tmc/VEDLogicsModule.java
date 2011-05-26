package tmc;

import jxl.Sheet;
import jxl.Workbook;
import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.*;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.OrderType;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.filter.CompareFilterInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.*;
import platform.server.integration.*;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import tmc.integration.exp.AbstractSaleExportTask;
import tmc.integration.exp.CashRegController;
import tmc.integration.exp.SaleExportTask;
import tmc.integration.imp.CustomerCheckRetailImportActionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * User: DAle
 * Date: 20.05.11
 * Time: 16:50
 */

public class VEDLogicsModule extends LogicsModule {
    public BaseLogicsModule<VEDBusinessLogics> LM;
    private VEDBusinessLogics VEDBL;
    private Logger logger;

    public VEDLogicsModule(BaseLogicsModule<VEDBusinessLogics> LM, VEDBusinessLogics VEDBL, Logger logger) {
        this.LM = LM;
        this.VEDBL = VEDBL;
        this.logger = logger;
    }

    private LP sumAddManfrOrderArticle;
    private LP addManfrOrderArticle;
    private LP sumAddManfrOrder;
    private LP addManfrOrder;
    private LP sumManfrOrder;
    private CustomClass storeLegalEntity;
    private CustomClass contract;
    private LP permissionOrder;
    private LP contractLegalEntityLegalEntity;
    private CustomClass contractSupplier;
    private CustomClass contractStore;
    private CustomClass contractCustomer;
    private LP legalOutContract;
    private LP legalIncContract;
    private CustomClass contractSale;
    private CustomClass contractDelivery;
    private LP nameLegalOutContract;
    private LP nameLegalIncContract;
    private LP purposeOrder;
    private AbstractGroup documentPrintGroup;
    private LP quantityOrder;
    private LP coeffTransArticle;
    private LP addArticleBarcode;
    private LP weightArticle;
    private LP weightOrderArticle;
    private LP transOrderArticle;
    private LP weightOrder;
    private LP transOrder;
    private LP isNegativeAddvOrderArticle;
    private LP unnLegalEntity;
    private LP legalEntityUnn;
    private LP invoiceDocumentNumber;
    private LP addressLegalEntity;
    private LP importDocs;
    private LP documentIncSklFreeQuantity;
    private LP downToZero;
    private LP freeIncOrderArticle;
    private CustomClass commitReturnShopOut;
    private LP sumWithDiscountCouponOrder;
    private LP sumDiscountPayCouponOrder;
    private LP sumRetailIncBetweenDate;
    private LP sumRetailOutBetweenDate;
    private LP sumDiscountPayCouponIncBetweenDate;
    private LP sumDiscountPayCouponOutBetweenDate;
    private LP sumWithDiscountCouponIncBetweenDate;
    private LP sumWithDiscountCouponOutBetweenDate;
    private LP sumPriceChangeBetweenDate;
    private LP genderArticle;
    private LP nameToGender;
    private CustomClass document;
    private ConcreteCustomClass revalueAct;
    private LP documentRevalueAct;
    private AbstractGroup documentRetailGroup;
    private AbstractGroup documentManfrGroup;
    private AbstractGroup documentObligationGroup;
    private AbstractGroup documentPayGroup;
    private AbstractGroup documentDiscountGroup;
    private AbstractGroup documentNDSGroup;
    private AbstractGroup documentSumGroup;
    private LP sumOrder;
    private AbstractGroup documentPrintRetailGroup;
    private CreateArticleFormEntity createArticleForm;
    private LP orderMinute;
    private LP sumWithDiscountCouponOrderArticle;
    private LP quantityCommitIncArticle;
    private LP quantityCommitOutArticle;
    private LP quantityIncSubjectArticleBetweenDate;
    private LP quantityOutSubjectArticleBetweenDate;
    private LP exclActionStore;
    private LP inclActionStore;
    private LP actionOutArticle;
    private LP articleDocQuantity;
    private LP contragentOrder;
    private LP betweenDate2;
    private LP quantitySupplierArticleBetweenDates;
    private LP sumNoNDSSupplierArticleBetweenDates;
    private LP sumNoNDSRetailSupplierArticleBetweenDates;
    private LP markUpSupplierShopArticleBetweenDates;
    private LP markUpPercentSupplierShopArticleBetweenDates;
    private LP formatAssortment;
    private LP nameFormatAssortment;
    private LP contractSpecification;
    private LP nameContractSpecification;
    private LP articleToSpecification;
    private LP specificationDateFrom;
    private LP specificationDateTo;
    private LP assortmentDateFrom;
    private LP assortmentDateTo;
    private LP articleToDocument;
    private LP contractOrder;

    private LP obligationIssued;
    private LP dateIssued;
    private LP shopOutName;
    private LP shopIncName;
    private LP clientIncName;
    private LP clientOutName;
    private LP certToSaled;
    private LP obligToIssued;
    private LP betweenDate;
    private LP betweenObligationIssuedDateFromDateTo;
    private LP betweenObligationToIssuedDateFromDateTo;
    private LP sumWithDiscountObligation;
    private LP discountSumObligation;
    private LP articleIncDocumentQuantity;
    private LP articleOutDocumentQuantity;
    private LP articleIncDocumentPrice;
    private LP articleOutDocumentPrice;
    private AbstractCustomClass orderWhole;

    // конкретные классы
    // реализация по безналу (в опт)
    CustomClass orderSaleWhole;
    CustomClass invoiceSaleWhole;
    CustomClass commitSaleWhole;

    // реализация в розницу
    CustomClass orderSaleArticleRetail;
    CustomClass orderSaleInvoiceArticleRetail;
    CustomClass commitSaleInvoiceArticleRetail;
    CustomClass commitSaleCheckArticleRetail;
    // инвентаризация
    CustomClass balanceCheck;
    // закупка у местного поставщика
    CustomClass orderDeliveryLocal;
    CustomClass commitDeliveryLocal;
    CustomClass orderDeliveryShopLocal;
    ConcreteCustomClass commitDeliveryShopLocal;
    CustomClass orderDeliveryWarehouseLocal;
    CustomClass commitDeliveryWarehouseLocal;
    // закупка у импортного поставщика
    CustomClass orderDeliveryImport;
    CustomClass commitDeliveryImport;

    // внутреннее перемещение
    CustomClass orderDistribute;
    CustomClass orderDistributeShop;
    CustomClass commitDistributeShop;
    CustomClass orderDistributeWarehouse;
    // возвраты
    // возврат местному поставщику
    CustomClass orderReturnDeliveryLocal, invoiceReturnDeliveryLocal, commitReturnDeliveryLocal;
    // возврат реализации по безналу
    CustomClass returnSaleInvoice;
    CustomClass returnSaleWhole;
    // возврат реализации за наличный расчет
    CustomClass returnSaleInvoiceRetail;
    CustomClass returnSaleCheckRetail;

    CustomClass order, orderInc, orderOut, orderStoreOut;
    CustomClass shipmentDocument;
    CustomClass shipmentDocumentOut;
    CustomClass commitOut, commitInc;
    CustomClass orderDoOut;
    CustomClass orderExtOut;
    CustomClass orderExtOutReturn;
    CustomClass orderExtInc;
    CustomClass orderExtIncReturn;
    CustomClass orderSale;
    CustomClass orderSaleReturn;

    CustomClass orderDo, orderReturn;

    CustomClass orderInner;
    CustomClass orderDelivery;
    CustomClass commitDelivery;

    CustomClass move, moveInner, returnOrderInner, returnOuter, orderDoInner, commitInner;

    CustomClass contragent;
    CustomClass contragentWhole;
    CustomClass supplier;
    CustomClass specification;
    CustomClass assortment;
    public ConcreteCustomClass article;
    public ConcreteCustomClass currency;
    public ConcreteCustomClass unitOfMeasure;
    public ConcreteCustomClass gender;
    CustomClass store, importSupplier, orderLocal, format, line;
    ConcreteCustomClass localSupplier;
    ConcreteCustomClass articleGroup, brend;
    CustomClass customer;
    CustomClass customerWhole;
    CustomClass customerInvoiceRetail;
    public ConcreteCustomClass customerCheckRetail;
    //CustomClass customerCheckRetail;
    CustomClass orderExtOutWhole;
    CustomClass orderInvoiceRetail;
    CustomClass transactionInvoiceRetail;
    CustomClass orderRetail;
    CustomClass orderSaleRetail;

    CustomClass documentRevalue;
    CustomClass commitDoShopInc;
    CustomClass documentNDS;
    CustomClass subject;

    CustomClass shop, warehouse;
    CustomClass legalEntity;

    CustomClass orderShopInc, orderShopOut;
    CustomClass orderWarehouseInc, orderWarehouseOut;

    CustomClass action;
    CustomClass saleAction;
    CustomClass articleAction;
    CustomClass groupArticleAction;

    private final boolean noArticleGroups = true;

    CustomClass obligation;
    CustomClass coupon;
    ConcreteCustomClass giftObligation;

    public LP checkRetailExported;


    AbstractGroup documentGroup;
    AbstractGroup priceGroup;
    AbstractGroup moveGroup;
    AbstractGroup logisticsGroup;
    AbstractGroup documentMoveGroup;
    AbstractGroup documentPriceGroup;
    AbstractGroup documentLogisticsGroup;
    public AbstractGroup cashRegGroup;
    public AbstractGroup cashRegOperGroup, cashRegAdminGroup;
    AbstractGroup couponGroup;
    AbstractGroup artExtraGroup;

    AbstractGroup documentInvoiceSaleGroup, documentShipmentGroup, documentShipmentOutGroup, documentShipmentTransportGroup;

    CustomClass saleCert;
    CustomClass saleInvoiceCert;
    CustomClass saleCheckCert;
    CustomClass documentShopPrice;

    LP balanceSklFreeQuantity;
    LP articleFreeQuantity;
    LP balanceFreeQuantity;
    LP obligationSumFrom;
    LP couponFromIssued;
    LP obligFromIssued;
    LP obligationToIssued;
    LP documentBarcodePrice, documentBarcodePriceOv;
    LP numberInvoiceDocument, seriesInvoiceDocument;
    LP orderBirthDay;
    LP payWithCard;
    LP printOrderCheck;
    // выноски
    LP orderUser;
    //LP discountSumOrderArticle;
    LP orderHour;
    LP orderContragentBarcode;
    LP orderUserBarcode;
    LP orderComputer;
    LP saleExport;
    LP importOrder;
    LP importArticlesRRP;
    LP importArticlesInfo;
    LP actionArticleStore;

    private LP shopFormat;
    private LP nameShopFormat;

    LP articleFormatMin;
    LP articleFormatToSell;

    LP countryArticle;

    LP priceAllOrderDeliveryArticle;
    LP sumNDSRetailOrderArticle;
    LP sumNDSOrderArticle;

    LP sumNoNDSOrderArticle;
    LP sumWithoutNDSRetailOrderArticle;
    LP addvOrderArticle;

    LP sumRetailOrder;
    LP sumNDSRetailOrder;
    LP sumDeliveryOrder;
    LP sumNDSOrder;

    LP sumNoNDSOrder;
    LP sumWithoutNDSRetailOrder;
    LP sumAddvOrder;
    LP sumPriceChangeOrder;
    LP addvOrder;
    LP nameLegalEntitySubject;
    LP nameLegalEntityIncOrder, nameLegalEntityOutOrder;

    LP addressSubject;

    LP removePercent;
    LP addPercent;
    LP addvArticle;
    LP currentRRPPriceStoreArticle;
    LP discountArticleStore;
    LP currentStoreDiscount;

    LP fullNameArticle;
    LP gigienaArticle;
    LP spirtArticle;
    LP statusArticle;
    LP brendArticle;
    LP nameBrendArticle;
    LP nameCountryArticle;

    public LP dateLastImportShop;
    public LP dateLastImport;

    LP padl;
    LP val;
    LP round1;
    LP round0;
    LP calcPercent;


    @Override
    public void initClasses() {
        subject = LM.addAbstractClass("subject", "Субъект", LM.baseClass.named);

        action = LM.addAbstractClass("action", "Акция", LM.baseClass);
        saleAction = LM.addConcreteClass("saleAction", "Распродажа", action);
        articleAction = LM.addConcreteClass("articleAction", "Акции по позициям", action);

        contract = LM.addAbstractClass("contract", "Договор", LM.baseClass.named, LM.transaction);
        contractDelivery = LM.addAbstractClass("contractDelivery", "Договор на закупку", contract);
        contractSale = LM.addAbstractClass("contractSale", "Договор на продажу", contract);
        contractSupplier = LM.addConcreteClass("contractSupplier", "Договор с поставщиком", contractDelivery);
        contractStore = LM.addConcreteClass("contractStore", "Договор между юр. лицами", contractSale, contractDelivery);
        contractCustomer = LM.addConcreteClass("contractcustomer", "Договор с покупателями", contractSale);

        groupArticleAction = LM.addConcreteClass("groupArticleAction", "Группа акций", LM.baseClass.named);

        store = LM.addAbstractClass("store", "Склад", subject);
        shop = LM.addConcreteClass("shop", "Магазин", store);
        warehouse = LM.addConcreteClass("warehouse", "Распред. центр", store);
        article = LM.addConcreteClass("article", "Товар", LM.baseClass.named, LM.barcodeObject);
        articleGroup = LM.addConcreteClass("articleGroup", "Группа товаров", LM.baseClass.named);
        specification = LM.addConcreteClass("specification", "Спецификация", LM.baseClass.named);
        assortment = LM.addConcreteClass("assortment", "Ассортимент", LM.baseClass.named);
        legalEntity = LM.addAbstractClass("legalEntity", "Юр.лицо", LM.baseClass.named);
        storeLegalEntity = LM.addConcreteClass("storeLegalEntity", "Юр.лицо складов", legalEntity);

        currency = LM.addConcreteClass("currency", "Валюта", LM.baseClass.named);
        unitOfMeasure = LM.addConcreteClass("unitOfMeasure", "Единица измерения", LM.baseClass.named);

        // новый классы
        brend = LM.addConcreteClass("brend", "Бренд", LM.baseClass.named);
        line = LM.addConcreteClass("line", "Линия", LM.baseClass.named);
        gender = LM.addConcreteClass("gender", "Пол", LM.baseClass.named);

        contragent = LM.addAbstractClass("contragent", "Контрагент", subject);
        contragentWhole = LM.addAbstractClass("contragentWhole", "Оптовый контрагент", contragent, legalEntity);

        supplier = LM.addAbstractClass("supplier", "Поставщик", contragentWhole);
        localSupplier = LM.addConcreteClass("localSupplier", "Местный поставщик", supplier);
        importSupplier = LM.addConcreteClass("importSupplier", "Импортный поставщик", supplier);

        customer = LM.addAbstractClass("customer", "Покупатель", contragent);
        customerWhole = LM.addConcreteClass("customerWhole", "Оптовый покупатель", customer, contragentWhole);
        customerInvoiceRetail = LM.addConcreteClass("customerInvoiceRetail", "Покупатель по накладным", customer, legalEntity);
        customerCheckRetail = LM.addConcreteClass("customerCheckRetail", "Розничный покупатель", customer, LM.barcodeObject);

        format = LM.addConcreteClass("format", "Формат", LM.baseClass.named);

        documentShopPrice = LM.addAbstractClass("documentShopPrice", "Изменение цены в магазине", LM.transaction);
        documentRevalue = LM.addConcreteClass("documentRevalue", "Переоценка в магазине", documentShopPrice);

        documentNDS = LM.addConcreteClass("documentNDS", "Изменение НДС", LM.transaction);

        // заявки на приход, расход
        order = LM.addAbstractClass("order", "Заявка", LM.transaction);

        orderInc = LM.addAbstractClass("orderInc", "Заявка прихода на склад", order);
        orderShopInc = LM.addAbstractClass("orderShopInc", "Заявка прихода на магазин", orderInc);
        orderWarehouseInc = LM.addAbstractClass("orderWarehouseInc", "Заявка прихода на распред. центр", orderInc);

        orderDo = LM.addAbstractClass("orderDo", "Прямая операция", order);
        orderReturn = LM.addAbstractClass("orderReturn", "Возврат операции", order);

        orderInner = LM.addAbstractClass("orderInner", "Внутренняя операция", order); // то есть с партиями
        returnOrderInner = LM.addAbstractClass("returnOrderInner", "Возврат внутренней операции", orderInner, orderReturn);
        orderDoInner = LM.addAbstractClass("orderDoInner", "Заказ", orderInner, orderDo);

        orderOut = LM.addAbstractClass("orderOut", "Заявка расхода со склада", orderInner);
        orderStoreOut = LM.addAbstractClass("orderStoreOut", "Заявка расхода со склада", orderOut);
        orderShopOut = LM.addAbstractClass("orderShopOut", "Заявка расхода с магазина", orderOut);
        orderWarehouseOut = LM.addAbstractClass("orderWarehouseOut", "Заявка расхода с распред. центра", orderOut);

        shipmentDocument = LM.addAbstractClass("shipmentDocument", "Заявка на перевозку", order);
        shipmentDocumentOut = LM.addAbstractClass("shipmentDocumentOut", "Заявка на перевозку со склада", shipmentDocument, orderOut);

        commitOut = LM.addAbstractClass("commitOut", "Отгруженная заявка", order);
        commitInc = LM.addAbstractClass("commitInc", "Принятая заявка", commitOut);

        commitInner = LM.addAbstractClass("commitInner", "Отгруженный заказ", commitOut, orderDoInner);

        orderDoOut = LM.addAbstractClass("orderDoOut", "Прямая заявка со склада", orderOut, orderDoInner);

        orderExtInc = LM.addAbstractClass("orderExtInc", "Заявка внешн. контрагенту", orderInc, orderDo);
        orderExtIncReturn = LM.addAbstractClass("orderExtIncReturn", "Заявка на возврат внешн. контрагенту", orderOut, returnOrderInner);
        orderExtOut = LM.addAbstractClass("orderExtOut", "Заявка от внешн. контрагента", orderDoOut);
        orderExtOutReturn = LM.addAbstractClass("orderExtOutReturn", "Заявка на возврат от внешн. контрагента", orderInc, returnOrderInner);

        commitDoShopInc = LM.addAbstractClass("commitDoShopInc", "Принятый приход на магазин", documentShopPrice, orderShopInc, commitInc);
        commitReturnShopOut = LM.addAbstractClass("commitReturnShopOut", "Отгруженный возврат с магазина", orderShopOut, commitOut);

        orderWhole = LM.addAbstractClass("orderWhole", "Оптовая операция", order);

        // внутр. и внешние операции
        orderDelivery = LM.addAbstractClass("orderDelivery", "Закупка", orderExtInc); // всегда прих., создает партию - элементарную единицу учета
        commitDelivery = LM.addAbstractClass("commitDelivery", "Приход от пост.", orderDelivery, commitInc, shipmentDocument);

        orderSale = LM.addAbstractClass("orderSale", "Продажа", orderExtOut);
        orderSaleReturn = LM.addAbstractClass("orderSaleReturn", "Возврат продажи", orderExtOutReturn);

        orderExtOutWhole = LM.addAbstractClass("orderExtOutWhole", "Оптовая операция с покупателем", orderWhole);
        orderInvoiceRetail = LM.addAbstractClass("orderInvoiceRetail", "Розничная операция по накладной", order);


        transactionInvoiceRetail = LM.addAbstractClass("transactionInvoiceRetail", "Розничная операция с сертификатом по накладной", LM.transaction);

        orderRetail = LM.addAbstractClass("orderRetail", "Розничная операция", LM.baseClass);

        //orderSaleRetail = addAbstractClass("orderSaleRetail", "Реализация через кассу", order, orderRetail);
        orderSaleRetail = LM.addAbstractClass("orderSaleRetail", "Реализация через кассу", LM.transaction, orderRetail);

        orderSaleWhole = LM.addConcreteClass("orderSaleWhole", "Оптовый заказ", orderWarehouseOut, orderExtOutWhole, orderSale);
        invoiceSaleWhole = LM.addConcreteClass("invoiceSaleWhole", "Выписанный оптовый заказ", orderSaleWhole, shipmentDocumentOut);
        commitSaleWhole = LM.addConcreteClass("commitSaleWhole", "Отгруженный оптовый заказ", invoiceSaleWhole, commitInner);

        orderSaleArticleRetail = LM.addAbstractClass("orderSaleArticleRetail", "Розничный заказ товаров", orderShopOut, orderSale);
        orderSaleInvoiceArticleRetail = LM.addConcreteClass("orderSaleInvoiceArticleRetail", "Розничный заказ товаров по накладной", orderSaleArticleRetail, orderInvoiceRetail);
        commitSaleInvoiceArticleRetail = LM.addConcreteClass("commitSaleInvoiceArticleRetail", "Отгруженный розничный заказ по накладной", commitInner,
                LM.addConcreteClass("writtenOutSaleInvoiceArticleRetail", "Выписанный розничный заказ по накладной", orderSaleInvoiceArticleRetail, shipmentDocumentOut));
        commitSaleCheckArticleRetail = LM.addConcreteClass("commitSaleCheckArticleRetail", "Реализация товаров через кассу", orderSaleArticleRetail, commitInner, orderSaleRetail);

        saleCert = LM.addConcreteClass("saleCert", "Реализация сертификатов", LM.transaction);

        saleInvoiceCert = LM.addConcreteClass("saleInvoiceCert", "Реализация сертификатов по накладной", saleCert, transactionInvoiceRetail);
        saleCheckCert = LM.addConcreteClass("saleCheckCert", "Реализация сертификатов через кассу", saleCert, orderSaleRetail);

        balanceCheck = LM.addConcreteClass("balanceCheck", "Инвентаризация", orderStoreOut, commitOut, orderInner, orderDo);

        orderDistribute = LM.addAbstractClass("orderDistribute", "Внутреннее перемещение", orderOut, orderInc, orderInner);

        orderDistributeShop = LM.addConcreteClass("orderDistributeShop", "Заказ на внутреннее перемещение на магазин", orderWarehouseOut, orderShopInc, orderDistribute, orderDoOut);
        commitDistributeShop = LM.addConcreteClass("commitDistributeShop", "Принятое внутреннее перемещение на магазин", commitDoShopInc,
                LM.addConcreteClass("loadedDistributeShop", "Отгруженное внутреннее перемещение на магазин", commitInner,
                        LM.addConcreteClass("writtenOutDistributeShop", "Выписанное внутреннее перемещение на магазин", orderDistributeShop, shipmentDocumentOut)));

        orderLocal = LM.addAbstractClass("orderLocal", "Операция с местным поставщиком", order, orderWhole);

        orderDeliveryLocal = LM.addAbstractClass("orderDeliveryLocal", "Закупка у местного поставщика", orderDelivery, orderLocal);
        commitDeliveryLocal = LM.addAbstractClass("commitDeliveryLocal", "Приход от местного поставщика", orderDeliveryLocal, commitDelivery);

        orderDeliveryShopLocal = LM.addConcreteClass("orderDeliveryShopLocal", "Закупка у местного поставщика на магазин", orderDeliveryLocal, orderShopInc);
        commitDeliveryShopLocal = LM.addConcreteClass("commitDeliveryShopLocal", "Приход от местного поставщика на магазин", orderDeliveryShopLocal, commitDeliveryLocal, commitDoShopInc);

        orderDeliveryWarehouseLocal = LM.addConcreteClass("orderDeliveryWarehouseLocal", "Закупка у местного поставщика на распред. центр", orderDeliveryLocal, orderWarehouseInc);
        commitDeliveryWarehouseLocal = LM.addConcreteClass("commitDeliveryWarehouseLocal", "Приход от местного поставщика на распред. центр", orderDeliveryWarehouseLocal, commitDeliveryLocal);

        orderDeliveryImport = LM.addConcreteClass("orderDeliveryImport", "Закупка у импортного поставщика", orderDelivery, orderWarehouseInc);
        commitDeliveryImport = LM.addConcreteClass("commitDeliveryImport", "Приход от импортного поставщика", orderDeliveryImport, commitDelivery);

        orderReturnDeliveryLocal = LM.addConcreteClass("orderReturnDeliveryLocal", "Заявка на возврат местному поставщику", orderStoreOut, orderLocal, orderExtIncReturn);
        invoiceReturnDeliveryLocal = LM.addConcreteClass("invoiceReturnDeliveryLocal", "Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal, shipmentDocumentOut);
        commitReturnDeliveryLocal = LM.addConcreteClass("commitReturnDeliveryLocal", "Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleInvoice = LM.addAbstractClass("returnSaleInvoice", "Возврат по накладной", commitInc, shipmentDocument, orderSaleReturn);
        returnSaleWhole = LM.addConcreteClass("returnSaleWhole", "Оптовый возврат", orderWarehouseInc, orderExtOutWhole, returnSaleInvoice);
        returnSaleInvoiceRetail = LM.addConcreteClass("returnSaleInvoiceRetail", "Возврат розничного заказа по накладной", orderShopInc, orderInvoiceRetail, returnSaleInvoice);
        returnSaleCheckRetail = LM.addConcreteClass("returnSaleCheckRetail", "Возврат реализации через кассу", orderShopInc, commitInc, orderRetail, orderSaleReturn);

        orderDistributeWarehouse = LM.addConcreteClass("orderDistributeWarehouse", "Заказ на возврат внутр. перемещ. на распред. центр", orderShopOut, orderWarehouseInc, returnOrderInner, orderDistribute);
        LM.addConcreteClass("commitDistributeWarehouse", "Принятый возврат внутр. перемещ. на распред. центр", commitInc,
                LM.addConcreteClass("loadedDistributeWarehouse", "Отгруженный возвр. внутр. перемещ. на распред. центр", commitReturnShopOut,
                        LM.addConcreteClass("writtenOutDistributeWarehouse", "Выписанный возврат внутр. перемещ. на распред. центр", orderDistributeWarehouse, shipmentDocumentOut)));

        obligation = LM.addAbstractClass("obligation", "Сертификат", LM.baseClass.named, LM.barcodeObject);
        coupon = LM.addConcreteClass("coupon", "Купон", obligation);
        giftObligation = LM.addConcreteClass("giftObligation", "Подарочный сертификат", obligation);

        document = LM.addAbstractClass("document", "Документ", LM.baseClass);
        revalueAct = LM.addConcreteClass("revalueAct", "Акт переоценки", document);
    }

    @Override
    public void initTables() {
        LM.tableFactory.include("article", article);
        LM.tableFactory.include("orders", order);
        LM.tableFactory.include("store", store);
        LM.tableFactory.include("localsupplier", localSupplier);
        LM.tableFactory.include("importsupplier", importSupplier);
        LM.tableFactory.include("customerwhole", customerWhole);
        LM.tableFactory.include("customerretail", customerCheckRetail);
        LM.tableFactory.include("articlestore", article, store);
        LM.tableFactory.include("articleorder", article, order);
        LM.tableFactory.include("articleaction", article, action);
        LM.tableFactory.include("articlespecification", article, specification);
        LM.tableFactory.include("rates", DateClass.instance);
        LM.tableFactory.include("intervals", DoubleClass.instance);
        LM.tableFactory.include("shoprates", DateClass.instance, shop);

        LM.tableFactory.include("obligation", obligation);
        LM.tableFactory.include("obligationorder", obligation, order);
    }

    @Override
    public void initGroups() {
        documentGroup = new AbstractGroup("Параметры документа");
        documentGroup.createContainer = false;
        LM.publicGroup.add(documentGroup);

        moveGroup = new AbstractGroup("Движение товаров");
        LM.publicGroup.add(moveGroup);

        documentMoveGroup = new AbstractGroup("Текущие параметры документа");
        documentGroup.add(documentMoveGroup);

        priceGroup = new AbstractGroup("Ценовые параметры");
        LM.publicGroup.add(priceGroup);

        documentPriceGroup = new AbstractGroup("Расчеты");
        documentPriceGroup.createContainer = false;
        documentGroup.add(documentPriceGroup);

        documentSumGroup = new AbstractGroup("Всего");
        documentPriceGroup.add(documentSumGroup);

        documentDiscountGroup = new AbstractGroup("Скидки");
        documentPriceGroup.add(documentDiscountGroup);

        documentNDSGroup = new AbstractGroup("Налоги");
        documentPriceGroup.add(documentNDSGroup);

        documentRetailGroup = new AbstractGroup("Розн. цены");
        documentPriceGroup.add(documentRetailGroup);

        documentPayGroup = new AbstractGroup("Платежные средства");
        documentPriceGroup.add(documentPayGroup);

        documentObligationGroup = new AbstractGroup("Сертификаты");
        documentPriceGroup.add(documentObligationGroup);

        documentManfrGroup = new AbstractGroup("Цены изготовителя");
        documentPriceGroup.add(documentManfrGroup);

        logisticsGroup = new AbstractGroup("Логистические параметры");
        LM.publicGroup.add(logisticsGroup);

        documentLogisticsGroup = new AbstractGroup("Логистические параметры документа");
        documentGroup.add(documentLogisticsGroup);

        cashRegGroup = new AbstractGroup("Операции с ФР");
        cashRegGroup.createContainer = false;
        LM.baseGroup.add(cashRegGroup);

        cashRegOperGroup = new AbstractGroup("Оперативные операции с ФР");
        cashRegGroup.add(cashRegOperGroup);

        cashRegAdminGroup = new AbstractGroup("Административные операции с ФР");
        cashRegGroup.add(cashRegAdminGroup);

        couponGroup = new AbstractGroup("Параметры купона");
        LM.publicGroup.add(couponGroup);

        artExtraGroup = new AbstractGroup("Доп. атрибуты товара");
        LM.publicGroup.add(artExtraGroup);

        documentPrintGroup = new AbstractGroup("Документы");
        LM.publicGroup.add(documentPrintGroup);

        documentPrintRetailGroup = new AbstractGroup("Розничные документы");
        documentPrintGroup.add(documentPrintRetailGroup);

        documentInvoiceSaleGroup = new AbstractGroup("Основание");
        documentPrintGroup.add(documentInvoiceSaleGroup);

        documentShipmentGroup = new AbstractGroup("Накладная");
        documentPrintGroup.add(documentShipmentGroup);

        documentShipmentOutGroup = new AbstractGroup("Отпуск");
        documentPrintGroup.add(documentShipmentOutGroup);

        documentShipmentTransportGroup = new AbstractGroup("Транспорт");
        documentPrintGroup.add(documentShipmentTransportGroup);
    }

    @Override
    public void initProperties() {

        removePercent = LM.addSFProp("((prm1*(100-prm2))/100)", DoubleClass.instance, 2);
        addPercent = LM.addSFProp("((prm1*(100+prm2))/100)", DoubleClass.instance, 2);
        LP backPercent = LM.addSFProp("prm1*prm2/(100+prm2)", DoubleClass.instance, 2);
        calcPercent = LM.addSFProp("prm1*100/prm2", DoubleClass.instance, 2);
        LP diff = LM.addSFProp("prm1-prm2", DoubleClass.instance, 2);

        round1 = LM.addSFProp("(ROUND(CAST((prm1) as NUMERIC(15,3)),-1))", DoubleClass.instance, 1);
        round0 = LM.addSFProp("(ROUND(CAST((prm1) as NUMERIC(15,3)),0))", DoubleClass.instance, 1);
        padl = LM.addSFProp("lpad(prm1,12,'0')", StringClass.get(12), 1);

        LP multiplyDouble2 = LM.addMFProp(DoubleClass.instance, 2);

        LP onlyPositive = LM.addJProp(LM.and1, 1, LM.positive, 1);
        LP min = LM.addSFProp(FormulaExpr.MIN2, DoubleClass.instance, 2);
        LP abs = LM.addSFProp("ABS(prm1)", DoubleClass.instance, 1);

        addArticleBarcode = LM.addJProp(true, "Ввод товара по штрих-коду", LM.addAAProp(article, LM.barcode), 1);

        LP groupParent = LM.addDProp("groupParent", "Родительская группа", articleGroup, articleGroup);
        LP groupParentName = LM.addJProp(LM.baseGroup, "Родительская группа", LM.name, groupParent, 1);

        articleToGroup = LM.addDProp(LM.idGroup, "articleToGroup", "Группа товаров", articleGroup, article); // принадлежность товара группе
        nameArticleGroupArticle = LM.addJProp(LM.baseGroup, "Группа товаров", LM.name, articleToGroup, 1);

        coeffTransArticle = LM.addDProp("coeffTransArticle", "Коэфф. гр. мест", DoubleClass.instance, article);
        weightArticle = LM.addDProp("weightArticle", "Вес товара", DoubleClass.instance, article);

        payWithCard = LM.addAProp(new PayWithCardActionProperty());
        printOrderCheck = LM.addAProp(new PrintOrderCheckActionProperty());
        saleExport = LM.addAProp(new SaleExportActionProperty());
        importOrder = LM.addAProp(new ImportOrderActionProperty());
        importArticlesRRP = LM.addAProp(new ImportArticlesRRPActionProperty());
        importArticlesInfo = LM.addAProp(new ImportArticlesInfoActionProperty());
        importDocs = LM.addAProp(new ImportDocsActionProperty());
        downToZero = LM.addAProp(new DownToZeroActionProperty());

        computerShop = LM.addDProp("computerShop", "Магазин рабочего места", shop, LM.computer);
        currentShop = LM.addJProp("Текущий магазин", computerShop, LM.currentComputer);

        panelScreenComPort = LM.addDProp(LM.baseGroup, "panelComPort", "COM-порт табло", IntegerClass.instance, LM.computer);
        cashRegComPort = LM.addDProp(LM.baseGroup, "cashRegComPort", "COM-порт фискального регистратора", IntegerClass.instance, LM.computer);
        LM.addJProp(LM.baseGroup, "Магазин рабочего места", LM.name, computerShop, 1);

        LP legalEntitySubject = LM.addCUProp("legalEntitySubject", "Юр. лицо (ИД)", LM.addDProp("legalEntityStore", "Юр. лицо (ИД)", storeLegalEntity, store), LM.object(legalEntity));
        nameLegalEntitySubject = LM.addJProp(LM.baseGroup, "nameLegalEntitySubject", "Юр. лицо", LM.name, legalEntitySubject, 1);

        // новые свойства местного поставщика
        LP[] propsLegalEntity = LM.addDProp(LM.baseGroup, "LegalEntity", new String[]{"unn", "address", "tel", "bankAddress", "account"},
                new String[]{"УНН", "Адрес", "Контактный телефон", "Адрес банка", "Счёт"}, new ValueClass[]{StringClass.get(20), StringClass.get(100), StringClass.get(20), StringClass.get(100), StringClass.get(30)}, legalEntity);
        unnLegalEntity = propsLegalEntity[0]; addressLegalEntity = propsLegalEntity[1];
        legalEntityUnn = LM.addAGProp("legalEntityUnn", "Юр. лицо", unnLegalEntity);
        LP[] propsLegalEntitySubject = LM.addJProp(LM.privateGroup, false, "Subject", propsLegalEntity, legalEntitySubject, 1);

        addressSubject = LM.addCUProp(LM.addDProp("addressStore", "Адрес", StringClass.get(100), store), propsLegalEntity[1]);

        contragentOrder = LM.addCUProp("contragentOrder", true, "Контрагент", // generics
                LM.addDProp("localSupplier", "Местный поставщик", localSupplier, orderLocal),
                LM.addDProp("importSupplier", "Импортный поставщик", importSupplier, orderDeliveryImport),
                LM.addDProp("wholeCustomer", "Оптовый покупатель", customerWhole, orderExtOutWhole),
                LM.addDProp("invoiceRetailCustomer", "Розничный покупатель", customerInvoiceRetail, orderInvoiceRetail),
                LM.addDProp("checkRetailCustomer", "Розничный покупатель", customerCheckRetail, orderSaleRetail));

        subjectOutOrder = LM.addCUProp("subjectOutOrder", true, "От кого (ИД)", // generics
                            LM.addJProp(LM.and1, contragentOrder, 1, LM.is(orderInc), 1),
                            LM.addDProp("outStore", "Склад (расх.)", store, orderStoreOut),
                            LM.addDProp("outShop", "Магазин (расх.)", shop, orderShopOut),
                            LM.addDProp("outWarehouse", "Распред. центр (расх.)", warehouse, orderWarehouseOut),
                            LM.addDProp("certStore", "Магазин (серт.)", shop, saleCert));
        subjectIncOrder = LM.addCUProp("subjectIncOrder", true, "Кому (ИД)", LM.addJProp(LM.and1, contragentOrder, 1, LM.is(orderOut), 1), // generics
                            LM.addJProp(LM.and1, contragentOrder, 1, LM.is(saleCert), 1), // для сертификатов
                            LM.addDProp("incShop", "Магазин (прих.)", shop, orderShopInc),
                            LM.addDProp("incWarehouse", "Распред. центр (прих.)", warehouse, orderWarehouseInc));
        // имена
        nameSubjectIncOrder = LM.addJProp(LM.baseGroup, "nameSubjectIncOrder", "Кому", LM.name, subjectIncOrder, 1);
        nameImplSubjectIncOrder = LM.addJProp(true, "Кому", LM.name, subjectIncOrder, 1);
        nameSubjectOutOrder = LM.addJProp(LM.baseGroup, "nameSubjectOutOrder", "От кого", LM.name, subjectOutOrder, 1);

        nameSubjectIncOrder.setPreferredCharWidth(40); nameSubjectOutOrder.setPreferredCharWidth(40); nameImplSubjectIncOrder.setPreferredCharWidth(40);

        addressSubjectIncOrder = LM.addJProp("addressSubjectIncOrder", "Адрес (кому)", addressSubject, subjectIncOrder, 1);
        addressSubjectOutOrder = LM.addJProp("addressSubjectOutOrder", "Адрес (от кого)", addressSubject, subjectOutOrder, 1);

        propsLegalEntityIncOrder = LM.addJProp(LM.privateGroup, false, "IncOrder", propsLegalEntitySubject, subjectIncOrder, 1);
        propsLegalEntityOutOrder = LM.addJProp(LM.privateGroup, false, "OutOrder", propsLegalEntitySubject, subjectOutOrder, 1);

        propsCustomerCheckRetail = LM.addDProp(LM.baseGroup, "", new String[]{"checkRetailCustomerPhone", "checkRetailCustomerBorn", "checkRetailCustomerAddress", "clientInitialSum"},
                            new String[]{"Телефон", "Дата рождения", "Адрес", "Начальная сумма"}, new ValueClass[] {StringClass.get(20), DateClass.instance, StringClass.get(40), DoubleClass.instance}, customerCheckRetail);
        bornCustomerCheckRetail = propsCustomerCheckRetail[1]; clientInitialSum = propsCustomerCheckRetail[3];
        propsCustomerIncOrder = LM.addJProp(LM.baseGroup, false, "IncOrder", propsCustomerCheckRetail, subjectIncOrder, 1); propsCustomerImplIncOrder = LM.addJProp(LM.baseGroup, true, "ImplIncOrder", propsCustomerCheckRetail, subjectIncOrder, 1);

        LP legalEntityIncOrder = LM.addJProp("legalEntityIncOrder", "Юр. лицо (кому) (ИД)", legalEntitySubject, subjectIncOrder, 1);
        LP legalEntityOutOrder = LM.addJProp("legalEntityOutOrder", "Юр. лицо (от кого) (ИД)", legalEntitySubject, subjectOutOrder, 1);

        nameLegalEntityIncOrder = LM.addJProp("nameLegalEntityIncOrder", "Юр. лицо (кому)", LM.name, legalEntityIncOrder, 1); propsLegalEntityIncOrder = LM.addJProp(LM.privateGroup, false, "LegalIncOrder", propsLegalEntity, "(кому)", legalEntityIncOrder, 1);
        nameLegalEntityOutOrder = LM.addJProp("nameLegalEntityOutOrder", "Юр. лицо (от кого)", LM.name, legalEntityOutOrder, 1); propsLegalEntityOutOrder = LM.addJProp(LM.privateGroup, false, "LegalOutOrder", propsLegalEntity, "(от кого)", legalEntityOutOrder, 1);

        LP diffOutInc = LM.addJProp(LM.diff2, subjectIncOrder, 1, subjectOutOrder, 2);
        LP allowedReturn = LM.addIfElseUProp(diffOutInc, LM.addJProp(diffOutInc, 2, 1), LM.is(orderOut), 1); // потом по идее надо сравнивать что совпадают юрлица

        outerOrderQuantity = LM.addDProp(documentGroup, "extIncOrderQuantity", "Кол-во заяв.", DoubleClass.instance, orderDelivery, article);
        outerCommitedQuantity = LM.addDProp(documentGroup, "extIncCommitedQuantity", "Кол-во принятое", DoubleClass.instance, commitDelivery, article);
//        outerCommitedQuantity.setDerivedChange(outerOrderQuantity, 1, 2, is(commitInc), 1); // пока не будем делать так как идет ручное штрих-кодирование
        LP expiryDate = LM.addDProp(LM.baseGroup, "expiryDate", "Срок годн.", DateClass.instance, commitDelivery, article);

        // для возвратных своего рода generics
        LP returnOuterQuantity = LM.addDProp("returnDeliveryLocalQuantity", "Кол-во возврата", DoubleClass.instance, orderReturnDeliveryLocal, article, commitDeliveryLocal);

        returnInnerCommitQuantity = LM.addCUProp(documentGroup, "returnInnerCommitQuantity", "Кол-во возврата", // generics
                LM.addDProp("returnSaleWholeQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleWhole, article, commitDelivery, commitSaleWhole),
                LM.addDProp("returnSaleInvoiceRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleInvoiceRetail, article, commitDelivery, commitSaleInvoiceArticleRetail),
                LM.addDProp("returnSaleCheckRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleCheckRetail, article, commitDelivery, commitSaleCheckArticleRetail),
                LM.addDProp("returnDistributeShopQuantity", "Кол-во возврата", DoubleClass.instance, orderDistributeWarehouse, article, commitDelivery, commitDistributeShop));

        LP returnSameClasses = LM.addCUProp( // generics - для ограничения что возвращать те же классы операций, повторяет верхнее свойство
                LM.addCProp(LogicalClass.instance, true, returnSaleWhole, commitSaleWhole),
                LM.addCProp(LogicalClass.instance, true, returnSaleInvoiceRetail, commitSaleInvoiceArticleRetail),
                LM.addCProp(LogicalClass.instance, true, returnSaleCheckRetail, commitSaleCheckArticleRetail),
                LM.addCProp(LogicalClass.instance, true, orderDistributeWarehouse, commitDistributeShop));

        LP orderInnerQuantity = LM.addDProp("outOrderQuantity", "Кол-во", DoubleClass.instance, orderDoInner, article, commitDelivery);

        // инвентаризация
        innerBalanceCheck = LM.addDProp(documentGroup, "innerBalanceCheck", "Остаток инв.", DoubleClass.instance, balanceCheck, article, commitDelivery);
        innerBalanceCheckDB = LM.addDProp("innerBalanceCheckDB", "Остаток (по учету)", DoubleClass.instance, balanceCheck, article, commitDelivery);

        LP returnDistrCommitQuantity = LM.addSGProp(LM.privateGroup, "returnDistrCommitQuantity", true, "Возвр. кол-во", returnInnerCommitQuantity, 1, 2, 3);
        innerQuantity = LM.addCUProp(documentGroup, "innerQuantity", true, "Кол-во", returnOuterQuantity, orderInnerQuantity, returnDistrCommitQuantity, LM.addDUProp("balanceCheckQuantity", "Кол-во инв.", innerBalanceCheckDB, innerBalanceCheck));

        quantityCommitIncArticle = LM.addCUProp(LM.addJProp(LM.and1, outerCommitedQuantity, 1, 2, LM.equals2, 1, 3), LM.addJProp(LM.and1, innerQuantity, 1, 2, 3, LM.is(commitInc), 1));
        LP incSklCommitedQuantity = LM.addSGProp(moveGroup, "incSklCommitedQuantity", true, "Кол-во прихода парт. на скл.",
                quantityCommitIncArticle, subjectIncOrder, 1, 2, 3);

        quantityCommitOutArticle = LM.addJProp("Кол-во отгр. парт.", LM.and1, innerQuantity, 1, 2, 3, LM.is(commitOut), 1);
        LP outSklCommitedQuantity = LM.addSGProp(moveGroup, "Кол-во отгр. парт. на скл.", quantityCommitOutArticle, subjectOutOrder, 1, 2, 3);
        LP outSklQuantity = LM.addSGProp(moveGroup, "Кол-во заяв. парт. на скл.", innerQuantity, subjectOutOrder, 1, 2, 3);

        // тут в общем-то должен не LM.and1 идти а разница через формулу и SUProp на 0 для склада (то есть для склада неизвестно это нет, а для контрагента просто неизвестно)
        balanceSklCommitedQuantity = LM.addJProp(moveGroup, "balanceSklCommitedQuantity", true, "Остаток парт. на скл.", LM.and1, LM.addDUProp(incSklCommitedQuantity, outSklCommitedQuantity), 1, 2, 3, LM.is(store), 1);
        balanceSklFreeQuantity = LM.addJProp(moveGroup, "balanceSklFreeQuantity", true, "Свободное кол-во на скл.", LM.and1, LM.addDUProp(incSklCommitedQuantity, outSklQuantity), 1, 2, 3, LM.is(store), 1);
        LM.addConstraint(LM.addJProp("Кол-во резерва должно быть не меньше нуля", LM.greater2, LM.vzero, balanceSklFreeQuantity, 1, 2, 3), false);

        articleFreeQuantity = LM.addSGProp(moveGroup, "articleFreeQuantity", true, "Свободное кол-во на скл.", balanceSklFreeQuantity, 1, 2);

        innerBalanceCheckDB.setDerivedChange(balanceSklCommitedQuantity, subjectOutOrder, 1, 2, 3);

        LM.addJProp(moveGroup, "Остаток парт. прих.", balanceSklCommitedQuantity, subjectIncOrder, 1, 2, 3);
        LM.addJProp(moveGroup, "Остаток парт. расх.", balanceSklCommitedQuantity, subjectOutOrder, 1, 2, 3);

        LP documentOutSklFreeQuantity = LM.addJProp("Дост. парт. расх.", balanceSklFreeQuantity, subjectOutOrder, 1, 2, 3);

        LP returnedInnerQuantity = LM.addSGProp("Кол-во возвр. парт.", returnInnerCommitQuantity, 4, 2, 3);
        confirmedInnerQuantity = LM.addDUProp("Кол-во подтв. парт.", LM.addJProp(LM.and1, orderInnerQuantity, 1, 2, 3, LM.is(commitOut), 1), returnedInnerQuantity);
        LM.addConstraint(LM.addJProp("Кол-во возврата должно быть не меньше кол-ва самой операции", LM.greater2, LM.vzero, confirmedInnerQuantity, 1, 2, 3), false);

        LP returnInnerConfirmedFreeQuantity = LM.addJProp(documentGroup, "returnInnerFreeQuantity", "Дост. кол-во по возврату парт.", LM.andNot1,
                LM.addCUProp(LM.addJProp(LM.and1, LM.addICProp(DoubleClass.instance, orderReturnDeliveryLocal, article, commitDelivery), 1, 2, 3, LM.equals2, 3, 4), // для возврата поставщику - нет ограничений, все равно остаток меньше
                        LM.addJProp(LM.and1, confirmedInnerQuantity, 4, 2, 3, returnSameClasses, 1, 4)), 1, 2, 3, 4, allowedReturn, 1, 4); // для возврата на склад, diff а не same чтобы можно было реализацию через кассы возвращать без контрагента
        returnDistrCommitQuantity.setDG(false, returnInnerConfirmedFreeQuantity, 1, 2, 3, 4, LM.date, 4, 4);

        // создаем свойства ограничения для расчета себестоимости (являются следствием addConstraint)
        LP fullFreeOrderArticleDelivery = LM.addCUProp(documentOutSklFreeQuantity, LM.addICProp(DoubleClass.instance, orderExtOutReturn, article, commitDelivery)); // для расхода не со склада нету ограничений
        documentInnerFreeQuantity = LM.addJProp(documentMoveGroup, "Дост. кол-во по парт.", min, fullFreeOrderArticleDelivery, 1, 2, 3,
                        LM.addCUProp(LM.addSGProp(returnInnerConfirmedFreeQuantity, 1, 2, 3), LM.addICProp(DoubleClass.instance, orderDo, article, commitDelivery)), 1, 2, 3); // для прямой операции нет ограничений
        returnInnerFreeQuantity = LM.addJProp(min, fullFreeOrderArticleDelivery, 1, 2, 3, returnInnerConfirmedFreeQuantity, 1, 2, 3, 4);

        // добавляем свойства по товарам
        articleInnerQuantity = LM.addDGProp(documentGroup, "articleInnerQuantity", true, "Кол-во", 2, false, innerQuantity, 1, 2, documentInnerFreeQuantity, 1, 2, 3, LM.date, 3, 3);
        documentFreeQuantity = LM.addSGProp(documentMoveGroup, "Доступ. кол-во", documentInnerFreeQuantity, 1, 2);

        // для док. \ товара \ парт. \ док. прод.   - кол-во подтв. парт. если совпадают контрагенты
        returnInnerQuantity = LM.addDGProp(documentGroup, "returnInnerQuantity", "Кол-во возврата", 2, false, returnInnerCommitQuantity, 1, 2, 4,
                returnInnerFreeQuantity, 1, 2, 3, 4, LM.date, 3, 3);
        returnFreeQuantity = LM.addSGProp(documentGroup, "Дост. кол-во к возврату", returnInnerFreeQuantity, 1, 2, 4);
        LP returnDocumentQuantity = LM.addCUProp("Кол-во возврата", returnOuterQuantity, returnInnerQuantity); // возвратный документ\прямой документ
        LM.addConstraint(LM.addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", LM.and1, allowedReturn, 1, 3, returnDocumentQuantity, 1, 2, 3), false);

        sumReturnedQuantity = LM.addSGProp(documentGroup, "Кол-во возврата", returnInnerQuantity, 1, 3);
        sumReturnedQuantityFree = LM.addSGProp(documentGroup, "Дост. кол-во к возврату", returnFreeQuantity, 1, 3);

        LP saleCertGiftObligation = LM.addDProp("saleCertGiftObligation", "Выдать", LogicalClass.instance, saleCert, giftObligation);

        articleQuantity = LM.addCUProp("articleQuantity", "Кол-во", outerCommitedQuantity, articleInnerQuantity);
        articleOrderQuantity = LM.addCUProp("Заяв. кол-во", outerOrderQuantity, articleInnerQuantity);
        articleDocQuantity = LM.addCUProp("Кол-во док.", LM.addSUProp(Union.OVERRIDE, outerCommitedQuantity, outerOrderQuantity), articleInnerQuantity);
        LP absQuantity = LM.addSGProp("Всего тов.", LM.addCUProp(LM.addJProp(abs, articleDocQuantity, 1, 2), LM.addJProp(LM.and1, LM.addCProp(DoubleClass.instance, 1), saleCertGiftObligation, 1, 2)), 1);
        LM.addConstraint(LM.addJProp("Нельзя создавать пустые документы", LM.andNot1, LM.is(order), 1, LM.addJProp(LM.greater2, absQuantity, 1, LM.vzero), 1), false);

        // ожидаемый приход на склад
        articleFreeOrderQuantity = LM.addSUProp("articleFreeOrderQuantity", true, "Ожидаемое своб. кол-во", Union.SUM, articleFreeQuantity, LM.addSGProp(moveGroup, "Ожидается приход", LM.addJProp(LM.andNot1, articleOrderQuantity, 1, 2, LM.is(commitInc), 1), subjectIncOrder, 1, 2)); // сумма по еще не пришедшим

        articleBalanceCheck = LM.addDGProp(documentGroup, "articleBalanceCheck", "Остаток инв.", 2, false, innerBalanceCheck, 1, 2, innerBalanceCheckDB, 1, 2, 3, LM.date, 3, 3);

        LP articleBalanceSklCommitedQuantity = LM.addSGProp(moveGroup, "articleBalanceSklCommitedQuantity", "Остаток тов. на скл.", balanceSklCommitedQuantity, 1, 2);
        LM.addJProp(documentMoveGroup, "Остаток тов. прих.", articleBalanceSklCommitedQuantity, subjectIncOrder, 1, 2);
        LM.addJProp(documentMoveGroup, "Остаток тов. расх.", articleBalanceSklCommitedQuantity, subjectOutOrder, 1, 2);

        // цены
        shopFormat = LM.addDProp("shopFormat", "Формат", format, shop);
        nameShopFormat = LM.addJProp(LM.baseGroup, "Формат", LM.name, shopFormat, 1);

        // новые свойства товара
        fullNameArticle = LM.addDProp(artExtraGroup, "fullNameArticle", "Полное наименование", StringClass.get(100), article);
        gigienaArticle = LM.addDProp(artExtraGroup, "gigienaArticle", "Гигиеническое разрешение", StringClass.get(50), article);
        spirtArticle = LM.addDProp(artExtraGroup, "spirtArticle", "Содержание спирта", DoubleClass.instance, article);
        statusArticle = LM.addDProp(artExtraGroup, "statusArticle", "Собственный/несобственный", LogicalClass.instance, article);

        brendArticle = LM.addDProp("brendArticle", "Бренд товара (ИД)", brend, article);
        nameBrendArticle = LM.addJProp(artExtraGroup, "Бренд товара", LM.name, brendArticle, 1);

        countryArticle = LM.addDProp("countryArticle", "Страна товара", LM.country, article);
        nameCountryArticle = LM.addJProp(LM.baseGroup, "Страна товара", LM.name, countryArticle, 1);

        LP articleLine = LM.addDProp("articleLine", "Линия товара", line, article);
        LM.addJProp(artExtraGroup, "Линия товара", LM.name, articleLine, 1);

        genderArticle = LM.addDProp("genderArticle", "Пол", gender, article);
        LM.addJProp(artExtraGroup, "Пол", LM.name, genderArticle, 1);
        //**************************************************************************************************************
        currentRRP = LM.addDProp(priceGroup, "currentRRP", "RRP", DoubleClass.instance, article);
        currencyArticle = LM.addDProp("currencyArticle", "Валюта (ИД)", currency, article);
        nameCurrencyArticle = LM.addJProp(priceGroup, "nameCurrencyArticle", "Валюта", LM.name, currencyArticle, 1);
        unitOfMeasureArticle = LM.addDProp("unitOfMeasureArticle", "Ед. изм.", unitOfMeasure, article);
        nameUnitOfMeasureArticle = LM.addJProp(LM.baseGroup, "nameUnitOfMeasureArticle", "Ед. изм.", LM.name, unitOfMeasureArticle, 1);
        LP currentCurrencyRate = LM.addDProp(LM.baseGroup, "currentCurrencyRate", "Курс", DoubleClass.instance, currency);
        LP currentFormatDiscount = LM.addDProp(priceGroup, "currentFormatDiscount", "Скидка на формат", DoubleClass.instance, format);
        LP currentWarehouseDiscount = LM.addDProp(priceGroup, "currentWarehouseDiscount", "Опт. скидка", DoubleClass.instance);

        LP addvBrend = LM.addDProp(LM.baseGroup, "addvBrend", "Наценка", DoubleClass.instance, brend);
        LP addvSetArticle = LM.addDProp(priceGroup, "addvSetArticle", "Наценка по тов.", DoubleClass.instance, article);
        LP addvBrendArticle = LM.addJProp("addvBrendArticle", "Наценка по бренду", addvBrend, brendArticle, 1);
        addvArticle = LM.addSUProp(priceGroup, "addvArticle", true, "Дейст. наценка", Union.OVERRIDE, addvBrendArticle, addvSetArticle);

        // простые акции
        LP actionFrom = LM.addDProp(LM.baseGroup, "actionFrom", "От", DateClass.instance, action);
        LP actionTo = LM.addDProp(LM.baseGroup, "actionTo", "До", DateClass.instance, action);
        LP actionDiscount = LM.addDProp(LM.baseGroup, "actionDiscount", "Скидка", DoubleClass.instance, action);

        LP customerCheckRetailDiscount = LM.addDProp(LM.baseGroup, "customerCheckRetailDiscount", "Мин. скидка", DoubleClass.instance, customerCheckRetail);

        LP xorActionAll = LM.addDProp(LM.baseGroup, "xorActionAll", "Вкл./искл.", LogicalClass.instance, action);
        LP xorActionArticleGroup = LM.addDProp(LM.baseGroup, "xorActionArticleGroup", "Вкл./искл.", LogicalClass.instance, action, articleGroup);
        xorActionArticle = LM.addDProp("xorArticle", "Вкл./искл.", LogicalClass.instance, action, article); // не включаем в группу, предполагается что реактирование через inAction идет
        inAction = LM.addXorUProp(LM.baseGroup, "inAction", true, "В акции", LM.addJProp(LM.and1, xorActionAll, 1, LM.is(article), 2), LM.addJProp(xorActionArticleGroup, 1, articleToGroup, 2), xorActionArticle);

        LP isStarted = LM.addJProp(LM.baseGroup, "Началась", LM.and(true, true), LM.is(action), 1,
                LM.addJProp(LM.less2, LM.currentDate, actionFrom, 1), 1,  // активация акции, если текущая дата в диапазоне акции
                LM.addJProp(LM.greater2, LM.currentDate, actionTo, 1), 1);

        exclActionStore = LM.addDProp(LM.baseGroup, "exclActionStore", "Искл.", LogicalClass.instance, action, store);
        inclActionStore = LM.addJProp("inclActionStore", "Вкл.", LM.andNot1, LM.addCProp(LogicalClass.instance, true, action, store), 1, 2, exclActionStore, 1, 2);
        actionArticleStore = LM.addAGProp(priceGroup, "actionArticleStore", "Дейст. распродажа", LM.addJProp(LM.and(false, false, false), inAction, 1, 2, isStarted, 1, LM.is(saleAction), 1, inclActionStore, 1, 3), 1);
        discountArticleStore = LM.addSUProp("discountArticleStore", true, "Тек. скидка", Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, article, store), LM.addJProp(priceGroup, "Тек. скидка", actionDiscount, actionArticleStore, 1, 2));
        LP actionNoExtraDiscount = LM.addDProp(LM.baseGroup, "actionNoExtraDiscount", "Без доп. скидок", LogicalClass.instance, saleAction);

        actionOutArticle = LM.addJProp("actionOutArticle", "Дейст. распродажа", actionArticleStore, 2, subjectOutOrder, 1);

        LP articleActionToGroup = LM.addDProp("articleActionToGroup", "Группа акций", groupArticleAction, articleAction);
        LM.addJProp(LM.baseGroup, "Группа акций", LM.name, articleActionToGroup, 1);

        LP articleActionHourFrom = LM.addDProp(LM.baseGroup, "articleActionHourFrom", "Час от", DoubleClass.instance, articleAction);
        LP articleActionHourTo = LM.addDProp(LM.baseGroup, "articleActionHourTo", "Час до", DoubleClass.instance, articleAction);
        LP articleActionClientSum = LM.addDProp(LM.baseGroup, "articleActionClientSum", "Нак. сумма от", DoubleClass.instance, articleAction);
        LP articleActionQuantity = LM.addDProp(LM.baseGroup, "articleActionQuantity", "Кол-во от", DoubleClass.instance, articleAction);
        LP articleActionBirthDay = LM.addDProp(LM.baseGroup, "articleActionBirthDay", "День рожд.", LogicalClass.instance, articleAction);
        LP articleActionWithCheck = LM.addDProp(LM.baseGroup, "articleActionWithCheck", "Нак. с тек. чеком", LogicalClass.instance, articleAction);

        // продажа облигаций
        //**************************************************************************************************************
        // новые свойства для подарочных сертификатов
        certToSaled = LM.addDProp(LM.baseGroup, "certToSaled", "Продан заранее", LogicalClass.instance, obligation);
        LP sverka = LM.addJProp(LM.equals2, 1, LM.addJProp(LM.and1, 1, certToSaled, 1), 2);
        LP issueCoupon = LM.addDProp("orderSaleCoupon", "Выдать купон", LogicalClass.instance, commitSaleCheckArticleRetail, coupon);
        issueObligation = LM.addCUProp(documentGroup, "Выдать", sverka, saleCertGiftObligation, issueCoupon);

        obligationIssued = LM.addAGProp("obligationIssued", true, "Выд. документ", issueObligation, 1);

        obligationSum = LM.addDProp(LM.baseGroup, "obligationSum", "Сумма", DoubleClass.instance, obligation);
        LM.setNotNull(obligationSum);
        obligationSumFrom = LM.addDProp(LM.baseGroup, "obligationSumFrom", "Сумма покупки", DoubleClass.instance, obligation);

        LP couponMaxPercent = LM.addDProp(LM.baseGroup, "couponMaxPercent", "Макс. процент по купонам", DoubleClass.instance);

        NDS = LM.addDProp(documentGroup, "NDS", "НДС", DoubleClass.instance, documentNDS, article);
        LP[] maxNDSProps = LM.addMGProp((AbstractGroup) null, true, new String[]{"currentNDSDate", "currentNDSDoc"}, new String[]{"Дата посл. НДС", "Посл. док. НДС"}, 1,
                LM.addJProp(LM.and1, LM.date, 1, NDS, 1, 2), 1, 2);
        currentNDSDate = maxNDSProps[0];
        currentNDSDoc = maxNDSProps[1];
        LM.addPersistent(currentNDSDate);
        LM.addPersistent(currentNDSDoc);
        currentNDS = LM.addJProp("currentNDS", true, "Тек. НДС", NDS, currentNDSDoc, 1, 1);

        LP ndsOrderDoArticle = LM.addDCProp("ndsOrderDoArticle", "НДС", currentNDS, 2, articleQuantity, 1, 2, orderDo);
        LP ndsOrderReturnArticle = LM.addMGProp(LM.privateGroup, "ndsOrderReturnArticle", "НДС возвр.", LM.addJProp(LM.and1, ndsOrderDoArticle, 3, 2, returnDocumentQuantity, 1, 2, 3), 1, 2);
        ndsOrderArticle = LM.addCUProp(LM.baseGroup, "ndsOrderArticle", "НДС", ndsOrderDoArticle, ndsOrderReturnArticle);

        currentStoreDiscount = LM.addCUProp(priceGroup, "Скидка на складе",
                LM.addJProp(LM.and1, currentWarehouseDiscount, LM.is(warehouse), 1),
                LM.addJProp(currentFormatDiscount, shopFormat, 1));    // берётся скидка формата, если её нет - оптовая скидка

        balanceFormatFreeQuantity = LM.addSGProp(moveGroup, "Своб. кол-во по форм.", articleFreeQuantity, shopFormat, 1, 2);

        balanceFreeQuantity = LM.addSGProp(moveGroup, "Своб. кол-во по форм.", articleFreeQuantity, 2);

        freeIncOrderArticle = LM.addJProp("freeIncOrderArticle", "Своб. кол-во (прих.)", articleFreeQuantity, subjectIncOrder, 1, 2);

        // текущая схема
        articleSupplier = LM.addDProp("articleSupplier", "Поставщик товара", supplier, article);
        nameArticleSupplier = LM.addJProp(logisticsGroup, "nameArticleSupplier", "Поставщик товара", LM.name, articleSupplier, 1);
        LP shopWarehouse = LM.addDProp("storeWarehouse", "Распред. центр", warehouse, shop); // магазин может числиться не более чем в одном распределяющем центре
        LM.addJProp(logisticsGroup, "Распред. центр", LM.name, shopWarehouse, 1);
        LP articleSupplierPrice = LM.addDProp(logisticsGroup, "articleSupplierPrice", "Цена поставок", DoubleClass.instance, article);

        LP revalueShop = LM.addDProp("revalueShop", "Магазин", shop, documentRevalue);
        LM.addJProp(LM.baseGroup, "Магазин", LM.name, revalueShop, 1);

        documentRevalued = LM.addDProp(documentGroup, "isRevalued", "Переоц.", LogicalClass.instance, documentRevalue, article);

        // ЦЕНЫ

        LP commitArticleQuantity = LM.addJProp(LM.and1, LM.is(commitDoShopInc), 1, articleQuantity, 1, 2);

        LP[] maxCommitIncProps = LM.addMGProp((AbstractGroup) null, true, new String[]{"currentCommitIncDate", "currentCommitIncDoc"}, new String[]{"Дата посл. прих. в маг.", "Посл. док. прих. в маг."}, 1,
                LM.addJProp(LM.and1, LM.date, 1, commitArticleQuantity, 1, 2), 1, subjectIncOrder, 1, 2);
        LP currentCommitIncDate = maxCommitIncProps[0]; LM.addPersistent(currentCommitIncDate);
        LP currentCommitIncDoc = maxCommitIncProps[1]; LM.addPersistent(currentCommitIncDoc);

        LP[] maxRevaluePriceProps = LM.addMGProp((AbstractGroup) null, true, new String[]{"currentRevalueDate", "currentRevalueDoc"}, new String[]{"Дата посл. переоц. в маг.", "Посл. док. переоц. в маг."}, 1,
                LM.addJProp(LM.and1, LM.date, 1, documentRevalued, 1, 2), 1, revalueShop, 1, 2);
        LP currentRevalueDate = maxRevaluePriceProps[0]; LM.addPersistent(currentRevalueDate);
        LP currentRevalueDoc = maxRevaluePriceProps[1]; LM.addPersistent(currentRevalueDoc);

        LP[] maxShopPriceProps = LM.addMUProp((AbstractGroup) null, new String[]{"currentShopPriceDate", "currentShopPriceDoc"}, new String[]{"Дата посл. цены в маг.", "Посл. док. цены в маг."}, 1,
                currentCommitIncDate, currentRevalueDate, currentCommitIncDoc, currentRevalueDoc);
        currentShopPriceDate = maxShopPriceProps[0]; LM.addPersistent(currentShopPriceDate);
        currentShopPriceDoc = maxShopPriceProps[1]; LM.addPersistent(currentShopPriceDoc);

        LP currentRRPPriceArticle = LM.addJProp(priceGroup, "Необх. цена RRP", multiplyDouble2, currentRRP, 1, LM.addJProp(currentCurrencyRate, currencyArticle, 1), 1);
        currentRRPPriceStoreArticle = LM.addJProp(LM.and1, currentRRPPriceArticle, 2, LM.is(store), 1);

        LP revalueShopPrice = LM.addDProp("revalueShopPrice", "Цена (прих.)", DoubleClass.instance, documentRevalue, article);
        LP returnShopOutPrice = LM.addDProp("returnShopOutPrice", "Цена (возвр.)", DoubleClass.instance, commitReturnShopOut, article);
        LP incomeShopPrice = LM.addDProp("shopPrice", "Цена (прих.)", DoubleClass.instance, commitDoShopInc, article);
        shopPrice = LM.addCUProp(documentRetailGroup, "priceDocument", "Цена (маг.)", incomeShopPrice, returnShopOutPrice, revalueShopPrice);

        currentShopPrice = LM.addJProp(priceGroup, "currentShopPrice", true, "Цена на маг. (тек.)", shopPrice, currentShopPriceDoc, 1, 2, 2);

        // цены в документах
        LP priceOrderDeliveryArticle = LM.addDCProp("priceOrderDeliveryArticle", "Цена пост. с НДС", true, articleSupplierPrice, 2, articleQuantity, 1, 2, orderDelivery);
        priceAllOrderDeliveryArticle = LM.addSUProp(documentSumGroup, "priceAllOrderDeliveryArticle", "Цена пост. с НДС", Union.OVERRIDE, LM.addJProp(LM.and1, articleSupplierPrice, 2, LM.is(orderDelivery), 1), priceOrderDeliveryArticle);
        LP orderSalePrice = LM.addDProp("orderSalePrice", "Цена прод.", DoubleClass.instance, orderDoOut, article);
        LP priceOrderDoArticle = LM.addCUProp(priceOrderDeliveryArticle, orderSalePrice);
        priceOrderArticle = LM.addCUProp(documentSumGroup, "priceOrderArticle", "Цена", priceOrderDoArticle, LM.addMGProp(LM.privateGroup, "priceOrderReturnArticle", "Цена возвр.", LM.addJProp(LM.and1, priceOrderDoArticle, 3, 2, returnDocumentQuantity, 1, 2, 3), 1, 2));
        LP priceNDSOrderArticle = LM.addJProp(round0, LM.addJProp(backPercent, priceOrderArticle, 1, 2, ndsOrderArticle, 1, 2), 1, 2);
        priceNoNDSOrderArticle = LM.addDUProp("Цена без НДС", priceOrderArticle, priceNDSOrderArticle);
        LP priceNDSRetailArticle = LM.addJProp(round1, LM.addJProp(backPercent, shopPrice, 1, 2, ndsOrderArticle , 1, 2), 1, 2);
        priceNoNDSRetailArticle = LM.addDUProp("Цена продажи без НДС", shopPrice, priceNDSRetailArticle);

        LP priceManfrOrderDeliveryArticle = LM.addDProp("priceManfrOrderDeliveryArticle", "Цена изг.", DoubleClass.instance, orderDelivery, article);
        priceManfrOrderDeliveryArticle.setDerivedForcedChange(true, priceNoNDSOrderArticle, 1, 2, articleQuantity, 1, 2);
        priceManfrOrderArticle = LM.addCUProp(documentManfrGroup, "priceManfrOrderArticle", "Цена изг.", priceManfrOrderDeliveryArticle, LM.addMGProp(LM.privateGroup, "maxPriceManfrInnerArticle", "Макс. цена закуп.", LM.addJProp(LM.and1, priceManfrOrderDeliveryArticle, 3, 2, innerQuantity, 1, 2, 3), 1, 2));

        LP priceExtOrderIncArticle = LM.addCUProp(LM.addJProp(LM.and1, priceNoNDSOrderArticle, 1, 2, LM.is(orderDelivery), 1),
                LM.addMGProp(LM.privateGroup, "maxPriceInnerArticle", "Макс. цена закуп.", LM.addJProp(LM.and1, priceNoNDSOrderArticle, 3, 2, innerQuantity, 1, 2, 3), 1, 2));

        // с дублированием initRequiredStorePrice, чтобы не делать defaultChanged true
        LP requiredStorePrice = initRequiredStorePrice(priceGroup, "requiredStorePrice", true, "Необх. цена",
                LM.addJProp(priceGroup, "lastPriceIncStoreArticle", true, "Посл. цена прихода", priceExtOrderIncArticle, currentCommitIncDoc, 1, 2, 2), LM.object(store));
        revalueShopPrice.setDerivedForcedChange(requiredStorePrice, revalueShop, 1, 2, documentRevalued, 1, 2);
        returnShopOutPrice.setDerivedForcedChange(currentShopPrice, subjectOutOrder, 1, 2, articleQuantity, 1, 2);
        incomeShopPrice.setDerivedForcedChange(true, initRequiredStorePrice(LM.privateGroup, LM.genSID(), false, "Необх. цена (прих.)", priceExtOrderIncArticle, subjectIncOrder), 1, 2, commitArticleQuantity, 1, 2);

        LP saleStorePrice = LM.addCUProp(priceGroup, "Цена прод.", LM.addJProp(LM.and1, requiredStorePrice, 1, 2, LM.is(warehouse), 1), currentShopPrice);
        orderSalePrice.setDerivedForcedChange(saleStorePrice, subjectOutOrder, 1, 2, articleQuantity, 1, 2);
        priceAllOrderSaleArticle = LM.addSUProp(documentSumGroup, "Цена прод.", Union.OVERRIDE, LM.addJProp(LM.and1, LM.addJProp(saleStorePrice, subjectOutOrder, 1, 2), 1, 2, LM.is(orderDoOut), 1), priceOrderDoArticle);

        LP outOfDatePrice = LM.addJProp(LM.and(false, false), LM.vtrue, articleBalanceSklCommitedQuantity, 1, 2, LM.addJProp(LM.diff2, requiredStorePrice, 1, 2, currentShopPrice, 1, 2), 1, 2);
        documentRevalued.setDerivedChange(outOfDatePrice, revalueShop, 1, 2);

        priceStore = LM.addCUProp("priceStore", true, "Склад (цены)", subjectIncOrder, revalueShop);
        inDocumentPrice = LM.addCUProp("inDocumentPrice", true, "Изм. цены", documentRevalued, commitArticleQuantity);
        prevPrice = LM.addDCProp(documentRetailGroup, "prevPrice", "Цена пред.", true, currentShopPrice, priceStore, 1, 2, inDocumentPrice, 1, 2);
        revalBalance = LM.addDCProp(documentRetailGroup, "revalBalance", "Остаток переоц.", true, articleBalanceSklCommitedQuantity, priceStore, 1, 2, inDocumentPrice, 1, 2);
        isRevalued = LM.addJProp(LM.diff2, shopPrice, 1, 2, prevPrice, 1, 2); // для акта переоценки
        isNewPrice = LM.addJProp(LM.andNot1, inDocumentPrice, 1, 2, LM.addJProp(LM.equals2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2); // для ценников
        ndsShopOrderPriceArticle = LM.addDCProp(documentRetailGroup, "ndsShopOrderPriceArticle", "НДС (маг.)", currentNDS, 2, inDocumentPrice, 1, 2);

        LP supplierCycle = LM.addDProp(logisticsGroup, "supplierCycle", "Цикл поставок", DoubleClass.instance, supplier);
        LP shopCycle = LM.addDProp(logisticsGroup, "shopCycle", "Цикл распределения", DoubleClass.instance, shop);

        LP supplierToWarehouse = LM.addDProp(logisticsGroup, "supplierToWarehouse", "Пост. на распред. центр", LogicalClass.instance, supplier);

        // абстрактный товар \ склад - поставщик
        LP articleSuppliedOnWarehouse = LM.addJProp(supplierToWarehouse, articleSupplier, 1);
        articleStoreSupplier = LM.addSUProp("articleStoreSupplier", true, "Пост. товара на склад", Union.OVERRIDE, LM.addJProp(LM.and1, articleSupplier, 2, LM.is(store), 1),
                LM.addJProp(LM.and1, shopWarehouse, 1, articleSuppliedOnWarehouse, 2));

        LP storeSupplierCycle = LM.addCUProp(LM.addJProp(LM.and1, supplierCycle, 2, LM.is(store), 1), LM.addJProp(LM.and1, shopCycle, 1, LM.is(warehouse), 2));
        // цикл распределения, если от распределяющего центра или цикл поставок, если от поставщика

        articleStorePeriod = LM.addJProp("articleStorePeriod", true, "Цикл поставок на склад", storeSupplierCycle, 1, articleStoreSupplier, 1, 2);

        articleFormatToSell = LM.addDProp(logisticsGroup, "articleFormatToSell", "В ассортименте", LogicalClass.instance, assortment, article);
        articleFormatMin = LM.addDProp(logisticsGroup, "articleFormatMin", "Страх. запас", DoubleClass.instance, format, article);

        LP articleStoreToSell = LM.addCUProp(logisticsGroup, "articleStoreToSell", "В ассортименте", LM.addJProp(articleFormatToSell, shopFormat, 1, 2),
                LM.addDProp("articleWarehouseToSell", "В ассортименте", LogicalClass.instance, warehouse, article));

        articleStoreMin = LM.addJProp("articleStoreMin", true, "Страх. запас", LM.and1, LM.addCUProp(logisticsGroup, "Страх. запас", LM.addJProp(articleFormatMin, shopFormat, 1, 2),
                LM.addDProp("articleWarehouseMin", "Страх. запас", DoubleClass.instance, warehouse, article)), 1, 2, articleStoreToSell, 1, 2);
        LP articleStoreForecast = LM.addJProp(LM.and1, LM.addDProp(logisticsGroup, "articleStoreForecast", "Прогноз прод. (в день)", DoubleClass.instance, store, article), 1, 2, articleStoreToSell, 1, 2);

        // MAX((страховой запас+прогноз расхода до следующего цикла поставки)-остаток,0) (по внутренним складам)
        articleFullStoreDemand = LM.addSUProp("articleFullStoreDemand", true, "Общ. необходимость", Union.SUM, LM.addJProp(multiplyDouble2, addSupplierProperty(articleStoreForecast), 1, 2, articleStorePeriod, 1, 2), addSupplierProperty(articleStoreMin));
        LP articleStoreRequired = LM.addJProp(onlyPositive, LM.addDUProp(articleFullStoreDemand, addSupplierProperty(articleFreeOrderQuantity)), 1, 2);

        documentLogisticsRequired = LM.addJProp(documentLogisticsGroup, "Необходимо", articleStoreRequired, subjectIncOrder, 1, 2);
        documentLogisticsSupplied = LM.addJProp(documentLogisticsGroup, "Поставляется", LM.equals2, subjectOutOrder, 1, LM.addJProp(articleStoreSupplier, subjectIncOrder, 1, 2), 1, 2);
        documentLogisticsRecommended = LM.addJProp(documentLogisticsGroup, "Рекомендовано", min, documentLogisticsRequired, 1, 2, documentFreeQuantity, 1, 2);

        LP orderClientSaleSum = LM.addDProp("orderClientSaleSum", "Нак. сумма", DoubleClass.instance, orderSaleArticleRetail);
        LP orderClientInitialSum = LM.addDCProp("orderClientInitialSum", "Нак. сумма", clientInitialSum, true, subjectIncOrder, 1);
        orderClientSum = LM.addSUProp(LM.baseGroup, "Нак. сумма", Union.SUM, LM.addCProp(DoubleClass.instance, 0, orderSaleArticleRetail), orderClientSaleSum, orderClientInitialSum);
        orderHour = LM.addDCProp(LM.baseGroup, "orderHour", "Час", LM.currentHour, LM.is(orderSale), 1, orderSaleArticleRetail);
        orderMinute = LM.addDCProp(LM.baseGroup, "orderMinute", "Минута", LM.currentMinute, LM.is(orderSale), 1, orderSaleArticleRetail);

        changeQuantityTime = LM.addTCProp(Time.EPOCH, "changeQuantityTime", "Время выбора", articleInnerQuantity, orderSaleArticleRetail);
        changeQuantityOrder = LM.addOProp(documentGroup, "Номер", OrderType.SUM, LM.addJProp(LM.and1, LM.addCProp(IntegerClass.instance, 1), articleInnerQuantity, 1, 2), true, true, 1, 1, changeQuantityTime, 1, 2);

        LP monthDay = LM.addSFProp("EXTRACT(MONTH FROM prm1) * 40 + EXTRACT(DAY FROM prm1)", IntegerClass.instance, 1);
        orderBirthDay = LM.addDCProp("orderBirthDay", "День рожд.", LM.addJProp(LM.equals2, monthDay, 1, LM.addJProp(monthDay, bornCustomerCheckRetail, 1), 2), true, LM.date, 1, subjectIncOrder, 1);

        sumManfrOrderArticle = LM.addJProp(documentManfrGroup, "sumManfrOrderArticle", "Сумма изг.", multiplyDouble2, articleQuantity, 1, 2, priceManfrOrderArticle, 1, 2);
        sumManfrOrder = LM.addSGProp(documentManfrGroup, "sumManfrOrder", "Сумма изг.", sumManfrOrderArticle, 1);

        sumOrderArticle = LM.addJProp(documentSumGroup, "sumOrderArticle", "Сумма", multiplyDouble2, articleQuantity, 1, 2, priceOrderArticle, 1, 2);
        sumNoNDSOrderArticleExtInc = LM.addJProp(documentSumGroup, "sumNoNDSOrderArticleExtInc", "Сумма прихода без НДС", multiplyDouble2, innerQuantity, 1, 2, 3,  priceNoNDSOrderArticle, 3, 2);
        sumNoNDSRetailArticleExtInc = LM.addJProp(documentSumGroup, "sumNoNDSRetailArticleExtInc", "Сумма продажи без НДС", multiplyDouble2, innerQuantity, 1, 2, 3, priceNoNDSRetailArticle, 3, 2);

        sumOrder = LM.addSGProp(documentSumGroup, "sumOrder", "Сумма", sumOrderArticle, 1);
        weightOrderArticle = LM.addJProp("weightOrderArticle", "Вес поз.", multiplyDouble2, articleQuantity, 1, 2, weightArticle, 2);
        transOrderArticle = LM.addJProp("transOrderArticle", "Кол-во гр. мест", multiplyDouble2, articleQuantity, 1, 2, coeffTransArticle, 2);

        LP orderActionClientSum = LM.addSUProp(Union.SUM, LM.addJProp(LM.and1, orderClientSum, 1, LM.is(articleAction), 2), LM.addJProp(LM.and1, LM.addSGProp(sumOrderArticle, 1), 1, articleActionWithCheck, 2));
        LP articleActionActive = LM.addJProp(LM.and(false, false, false, false, true, true, true, true, true), articleQuantity, 1, 2, LM.is(orderSaleArticleRetail), 1, LM.is(articleAction), 3, inAction, 3, 2, isStarted, 3,
                LM.addJProp(LM.less2, articleQuantity, 1, 2, articleActionQuantity, 3), 1, 2, 3,
                LM.addJProp(LM.and(false, true), articleActionBirthDay, 2, LM.is(orderSaleArticleRetail), 1, orderBirthDay, 1), 1, 3,
                LM.addJProp(LM.less2, orderActionClientSum, 1, 2, articleActionClientSum, 2), 1, 3,
                LM.addJProp(LM.less2, orderHour, 1, articleActionHourFrom, 2), 1, 3,
                LM.addJProp(LM.greater2, orderHour, 1, articleActionHourTo, 2), 1, 3);

        orderNoDiscount = LM.addDProp(LM.baseGroup, "orderNoDiscount", "Без. скидок", LogicalClass.instance, orderSaleArticleRetail);
        LP orderArticleSaleDiscount = LM.addDCProp("orderArticleSaleDiscount", "Скидка", true, LM.addJProp(LM.and(true, true),
                LM.addSUProp(Union.MAX,
                        LM.addSGProp(LM.addMGProp(LM.addJProp(LM.and1, actionDiscount, 3, articleActionActive, 1, 2, 3), 1, 2, articleActionToGroup, 3), 1, 2),
                        LM.addJProp(LM.and1, LM.addJProp(customerCheckRetailDiscount, subjectIncOrder, 1), 1, LM.is(article), 2)), 1, 2,
                LM.addJProp(actionNoExtraDiscount, actionOutArticle, 1, 2), 1, 2, orderNoDiscount, 1),
                true, 1, 2, articleQuantity, 1, 2);
        LP discountOrderReturnArticle = LM.addMGProp(LM.privateGroup, "discountOrderReturnArticle", "Скидка возвр.", LM.addJProp(LM.and1, orderArticleSaleDiscount, 3, 2, returnDocumentQuantity, 1, 2, 3), 1, 2);
        discountOrderArticle = LM.addCUProp(LM.baseGroup, "discountOrderArticle", "Скидка", orderArticleSaleDiscount, discountOrderReturnArticle); // возвращаем ту же скидку при возврате

        discountSumOrderArticle = LM.addJProp(documentDiscountGroup, "discountSumOrderArticle", "Сумма скидки", round1, LM.addJProp(LM.percent, sumOrderArticle, 1, 2, discountOrderArticle, 1, 2), 1, 2);
        sumWithDiscountOrderArticle = LM.addDUProp(documentDiscountGroup, "sumWithDiscountOrderArticle", "Сумма со скидкой", sumOrderArticle, discountSumOrderArticle);

        LP orderSalePayGift = LM.addSGProp(LM.addJProp(LM.and(false, false, false), obligationSum, 2, issueObligation, 1, 2, LM.is(order), 1, LM.is(giftObligation), 2), 1);
        discountSumOrder = LM.addSGProp(documentDiscountGroup, "discountSumOrder", true, "Сумма скидки", discountSumOrderArticle, 1);
        sumWithDiscountOrder = LM.addCUProp(documentDiscountGroup, "sumWithDiscountOrder", true, "Сумма со скидкой", orderSalePayGift, LM.addSGProp(sumWithDiscountOrderArticle, 1));

        sumNDSOrderArticle = LM.addJProp(documentNDSGroup, "sumNDSOrderArticle", "Сумма НДС", round0, LM.addJProp(backPercent, sumWithDiscountOrderArticle, 1, 2, ndsOrderArticle, 1, 2), 1, 2);
        sumNoNDSOrderArticle = LM.addDUProp(documentNDSGroup, "sumNoNDSOrderArticle", "Сумма без НДС", sumWithDiscountOrderArticle, sumNDSOrderArticle);
        // док.
        sumNDSOrder = LM.addSGProp(documentNDSGroup, "sumNDSOrder", "Сумма НДС", sumNDSOrderArticle, 1);
        sumNoNDSOrder = LM.addDUProp(documentNDSGroup, "sumNoNDSOrder", "Сумма без НДС", sumWithDiscountOrder, sumNDSOrder);

        quantityOrder = LM.addSGProp("quantityOrder", "Кол-во", articleQuantity, 1);
        weightOrder = LM.addSGProp("weightOrder", "Общий вес", weightOrderArticle, 1);
        transOrder = LM.addSGProp("transOrder", "Общее кол-во гр. мест", transOrderArticle, 1);

        // для бухгалтерии магазинов
        sumRetailOrderArticle = LM.addJProp(documentRetailGroup, "sumRetailOrderArticle", "Сумма розн.", multiplyDouble2, shopPrice, 1, 2, articleQuantity, 1, 2);
        sumNDSRetailOrderArticle = LM.addJProp(documentRetailGroup, "sumNDSRetailOrderArticle", "Сумма НДС (розн.)", round0, LM.addJProp(backPercent, sumRetailOrderArticle, 1, 2, ndsShopOrderPriceArticle, 1, 2), 1, 2);
        sumWithoutNDSRetailOrderArticle = LM.addJProp(documentRetailGroup, "sumWithoutNDSRetailOrderArticle", "Сумма розн. без НДС", diff, sumRetailOrderArticle, 1, 2, sumNDSRetailOrderArticle, 1, 2);
        sumAddvOrderArticle = LM.addJProp(documentRetailGroup, "sumAddvOrderArticle", "Сумма нацен.", diff, sumWithoutNDSRetailOrderArticle, 1, 2, sumNoNDSOrderArticle, 1, 2);
        addvOrderArticle = LM.addJProp(documentRetailGroup, "addvOrderArticle", "Наценка", round0, LM.addJProp(calcPercent, sumAddvOrderArticle, 1, 2, sumNoNDSOrderArticle, 1, 2), 1, 2);
        isNegativeAddvOrderArticle = LM.addJProp(LM.less2, addvOrderArticle, 1, 2, LM.vzero);

        // изг.
        sumAddManfrOrderArticle = LM.addJProp(documentManfrGroup, "sumAddManfrOrderArticle", "Сумма опт. нац.", diff, sumNoNDSOrderArticle, 1, 2, sumManfrOrderArticle, 1, 2);
        addManfrOrderArticle = LM.addJProp(documentManfrGroup, "addManfrOrderArticle", "Опт. нац.", round0, LM.addJProp(calcPercent, sumAddManfrOrderArticle, 1, 2, sumManfrOrderArticle, 1, 2), 1, 2);

        // док.
        sumRetailOrder = LM.addSGProp(documentRetailGroup, "sumRetailOrder", "Сумма розн.", sumRetailOrderArticle, 1);
        sumNDSRetailOrder = LM.addSGProp(documentRetailGroup, "sumNDSRetailOrder", "Сумма НДС (розн.)", sumNDSRetailOrderArticle, 1);
        sumWithoutNDSRetailOrder = LM.addDUProp(documentRetailGroup, "sumWithoutNDSRetailOrder", "Сумма розн. без НДС", sumRetailOrder, sumNDSRetailOrder);
        sumAddvOrder = LM.addJProp(documentRetailGroup, "sumAddvOrder", "Сумма нацен.", diff, sumWithoutNDSRetailOrder, 1, sumNoNDSOrder, 1);
        addvOrder = LM.addJProp(documentRetailGroup, "addvOrder", "Наценка", round0, LM.addJProp(calcPercent, sumAddvOrder, 1, sumNoNDSOrder, 1), 1);

        // изг.
        sumAddManfrOrder = LM.addJProp(documentManfrGroup, "sumAddManfrOrder", "Сумма опт. нац.", diff, sumNoNDSOrder, 1, sumManfrOrder, 1);
        addManfrOrder = LM.addJProp(documentManfrGroup, "addManfrOrder", "Опт. нац.", round0, LM.addJProp(calcPercent, sumAddManfrOrder, 1, sumManfrOrder, 1), 1);

        // переоценка
        revalChangeBalance = LM.addJProp("revalChangeBalance", "Остаток (изм.)", LM.and1, revalBalance, 1, 2, LM.addJProp(LM.diff2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2);
        sumNewPrevRetailOrderArticle = LM.addJProp("sumNewPrevRetailOrderArticle", "Сумма розн. (нов.)", multiplyDouble2, shopPrice, 1, 2, revalChangeBalance, 1, 2);
        sumPrevRetailOrderArticle = LM.addJProp("sumPrevRetailOrderArticle", "Сумма розн. (пред.)", multiplyDouble2, prevPrice, 1, 2, revalChangeBalance, 1, 2);
        sumPriceChangeOrderArticle = LM.addJProp("sumPriceChangeOrderArticle", "Сумма переоц.", diff, sumNewPrevRetailOrderArticle, 1, 2, sumPrevRetailOrderArticle, 1, 2);
        sumRevalBalance = LM.addSGProp("sumRevalBalance", "Кол-во переоц.", revalChangeBalance, 1);
        sumNewPrevRetailOrder = LM.addSGProp("sumNewPrevRetailOrder", "Сумма розн. (нов.)", sumNewPrevRetailOrderArticle, 1);
        sumPrevRetailOrder = LM.addSGProp("sumPrevRetailOrder", "Сумма розн. (пред.)", sumPrevRetailOrderArticle, 1);
        sumPriceChangeOrder = LM.addJProp("sumPriceChangeOrder", "Сумма переоц.", diff, sumNewPrevRetailOrder, 1, sumPrevRetailOrder, 1);

        // для товарного отчета
/*        documentRevalueAct = LM.addDProp(LM.baseGroup, "documentRevalueAct", "Документ", documentShopPrice, revalueAct);
        LP revalueActDocument = addAGProp(LM.baseGroup, false, "revalueActDocument", false, "Акт переоценки", revalueAct, documentRevalueAct);
        follows(sumPriceChangeOrder, revalueActDocument, 1);
        follows(LM.is(revalueAct), documentRevalueAct, 1);*/

        orderSaleUseObligation = LM.addDProp("orderSaleUseObligation", "Использовать", LogicalClass.instance, commitSaleCheckArticleRetail, obligation);
        LP obligationUseSum = LM.addJProp(LM.and1, obligationSum, 2, orderSaleUseObligation, 1, 2);

        obligationDocument = LM.addAGProp("obligationDocument", "Исп. документ", orderSaleUseObligation, 1);

        LP addDays = LM.addSFProp("prm1+prm2", DateClass.instance, 2);

        couponStart = LM.addDProp(LM.baseGroup, "couponStart", "Дата начала купонов", DateClass.instance);
        LP couponExpiry = LM.addDProp(LM.baseGroup, "couponExpiry", "Дата окончания купонов", DateClass.instance);
        LP certExpiry = LM.addDProp(LM.baseGroup, "certExpiry", "Срок действия серт.", IntegerClass.instance);

        dateIssued = LM.addJProp("Дата выдачи", LM.date, obligationIssued, 1);
        couponFromIssued = LM.addDCProp(LM.baseGroup, "couponFromIssued", "Дата начала", couponStart, dateIssued, 1, coupon);
        LP couponToIssued = LM.addDCProp("couponToIssued", "Дата окончания", couponExpiry, obligationIssued, 1, coupon);
        LP certToIssued = LM.addDCProp("certToIssued", "Дата окончания", LM.addJProp(addDays, 1, certExpiry), dateIssued, 1, giftObligation);

        obligationToIssued = LM.addCUProp(LM.baseGroup, "obligationToIssued", "Дата окончания", couponToIssued, certToIssued);

        LP orderSaleObligationAllowed = LM.addJProp(LM.and(false, true, true, true), LM.is(commitSaleCheckArticleRetail), 1, obligationIssued, 2,
                LM.addJProp(LM.less2, sumWithDiscountOrder, 1, obligationSumFrom, 2), 1, 2,
                LM.addJProp(LM.greater2, LM.date, 1, obligationToIssued, 2), 1, 2,
                LM.addJProp(LM.less2, LM.date, 1, couponFromIssued, 2), 1, 2);
        LM.addConstraint(LM.addJProp("Нельзя использовать выбранный сертификат", LM.andNot1, orderSaleUseObligation, 1, 2, orderSaleObligationAllowed, 1, 2), false);

        LP orderSaleObligationCanBeUsed = LM.addJProp(LM.andNot1, orderSaleObligationAllowed, 1, 2, obligationDocument, 2);
        orderSaleObligationCanNotBeUsed = LM.addJProp(LM.and(false, true), LM.is(commitSaleCheckArticleRetail), 1, LM.is(obligation), 2, orderSaleObligationCanBeUsed, 1, 2);

        LP orderMaxCoupon = LM.addDCProp("orderMaxCoupon", "Макс. процент по купонам", couponMaxPercent, LM.is(orderSaleArticleRetail), 1);

        // сумма без сертификатов
        LP orderSalePayCoupon = LM.addJProp("orderSalePayCoupon", true, "Сумма куп." , min, LM.addSGProp(LM.addJProp(LM.and1, obligationUseSum, 1, 2, LM.is(coupon), 2), 1), 1, LM.addJProp(LM.percent, sumWithDiscountOrder, 1, orderMaxCoupon, 1), 1);
        LP orderSalePayGiftObligation = LM.addSGProp("orderSalePayGiftObligation", true, "Сумма под. серт.", LM.addJProp(LM.and1, obligationUseSum, 1, 2, LM.is(giftObligation), 2), 1);
        orderSalePayObligation = LM.addSUProp(documentObligationGroup, "orderSalePayObligation", true, "Сумма серт.", Union.SUM, orderSalePayGiftObligation, orderSalePayCoupon);
        sumWithDiscountObligationOrder = LM.addJProp(documentObligationGroup, "sumWithDiscountObligationOrder", true, "Сумма к опл.", onlyPositive, LM.addDUProp(sumWithDiscountOrder, orderSalePayObligation), 1);
        sumWithDiscountObligationOrderArticle = LM.addPGProp(LM.privateGroup, "sumWithDiscountObligationOrderArticle", false, -1, true, "Сумма к опл.", sumWithDiscountOrderArticle, sumWithDiscountObligationOrder, 1);

        // пока для товарного отчета
        sumWithDiscountCouponOrder = LM.addJProp(documentObligationGroup, "sumWithDiscountCouponOrder", true, "Сумма без куп.", onlyPositive, LM.addDUProp(sumWithDiscountOrder, orderSalePayCoupon), 1);
        sumWithDiscountCouponOrderArticle = LM.addPGProp(LM.privateGroup, "sumWithDiscountCouponOrderArticle", false, -1, true, "Сумма без куп.", sumWithDiscountOrderArticle, sumWithDiscountCouponOrder, 1);
        sumDiscountPayCouponOrder = LM.addSUProp(documentObligationGroup, "sumDiscountPayCouponOrder", true, "Сумма серт.", Union.SUM, discountSumOrder, orderSalePayCoupon);

        LP clientSaleSum = LM.addSGProp(sumWithDiscountObligationOrder, subjectIncOrder, 1);
        orderClientSaleSum.setDerivedChange(clientSaleSum, subjectIncOrder, 1);
        clientSum = LM.addSUProp(LM.baseGroup, "clientSum", true, "Нак. сумма", Union.SUM, clientSaleSum, clientInitialSum);
        accumulatedClientSum = LM.addJProp("Накопленная сумма", clientSum, subjectIncOrder, 1);

        orderSalePayCash = LM.addDProp(documentPayGroup, "orderSalePayCash", "Наличными", DoubleClass.instance, orderSaleRetail);
        orderSalePayCard = LM.addDProp(documentPayGroup, "orderSalePayCard", "Карточкой", DoubleClass.instance, orderSaleRetail);

        impSumCard = LM.addDProp(LM.baseGroup, "inpSumCard", "Безнал. в кассе (ввод)", DoubleClass.instance, DateClass.instance, shop);
        LP curCard = LM.addJProp(cashRegGroup, true, "Безнал. в кассе (ввод)", impSumCard, LM.currentDate, currentShop);
        impSumCash = LM.addDProp(LM.baseGroup, "inpSumCash", "Наличных в кассе (ввод)", DoubleClass.instance, DateClass.instance, shop);
        LP curCash = LM.addJProp(cashRegGroup, true, "Наличных в кассе (ввод)", impSumCash, LM.currentDate, currentShop);
        impSumBank = LM.addDProp(LM.baseGroup, "inpSumBank", "Отправить в банк", DoubleClass.instance, DateClass.instance, shop);
        LP curBank = LM.addJProp(cashRegGroup, true, "Отправить в банк (ввод)", impSumBank, LM.currentDate, currentShop);

        LP allOrderSalePayCard = LM.addSGProp(LM.baseGroup, "Безнал. в кассе", orderSalePayCard, LM.date, 1, subjectOutOrder, 1);
        LP retailSumOrderRetail = LM.addJProp(LM.and1, sumWithDiscountObligationOrder, 1, LM.is(orderRetail), 1);
        LP allOrderSalePayCash = LM.addDUProp(cashRegGroup, "Наличных в кассе", LM.addDUProp(LM.addSGProp(retailSumOrderRetail, LM.date, 1, subjectOutOrder, 1), LM.addSGProp(retailSumOrderRetail, LM.date, 1, subjectIncOrder, 1)), allOrderSalePayCard);

        LP allOrderSalePayCardCur = LM.addJProp(cashRegGroup, "allOrderSalePayCardCur", "Безнал. в кассе", allOrderSalePayCard, LM.currentDate, currentShop);
        LP allOrderSalePayCashCur = LM.addJProp(cashRegGroup, "Наличных в кассе", allOrderSalePayCash, LM.currentDate, currentShop);

        // сдача/доплата
        LP orderSalePayAll = LM.addSUProp(Union.SUM, orderSalePayCard, orderSalePayCash);
        LP orderSaleDiffSum = LM.addJProp(LM.and1, LM.addDUProp(orderSalePayAll, sumWithDiscountObligationOrder), 1, LM.is(orderSaleRetail), 1);
        LP notEnoughSum = LM.addJProp(LM.negative, orderSaleDiffSum, 1);
        orderSaleToDo = LM.addJProp(documentPayGroup, "Необходимо", LM.and1, LM.addIfElseUProp(LM.addCProp(StringClass.get(5), "Итого", orderSaleRetail),
                LM.addCProp(StringClass.get(5), "Сдача", orderSaleRetail), notEnoughSum, 1), 1, orderSaleDiffSum, 1);
        orderSaleToDoSum = LM.addJProp(documentPayGroup, "Сумма необх.", abs, orderSaleDiffSum, 1);

        LM.addConstraint(LM.addJProp("Сумма наличными меньше сдачи", LM.greater2, orderSalePayCard, 1, sumWithDiscountObligationOrder, 1), false);
        LM.addConstraint(LM.addJProp("Всё оплачено карточкой", LM.and1, LM.addJProp(LM.equals2, orderSalePayCard, 1, sumWithDiscountObligationOrder, 1), 1, orderSalePayCash, 1), false);
        LM.addConstraint(LM.addJProp("Введенной суммы не достаточно", LM.and1, notEnoughSum, 1, orderSalePayAll, 1), false); // если ни карточки ни кэша не задали, значит заплатитли без сдачи

        documentBarcodePrice = LM.addJProp("Цена", priceAllOrderSaleArticle, 1, LM.barcodeToObject, 2);
        documentBarcodePriceOv = LM.addSUProp("Цена", Union.OVERRIDE, documentBarcodePrice, LM.addJProp(LM.and1, LM.addJProp(obligationSum, LM.barcodeToObject, 1), 2, LM.is(order), 1));

        barcodeAddClient = LM.addSDProp("Доб. клиента", LogicalClass.instance);
        barcodeAddClientAction = LM.addJProp(true, "", LM.andNot1, LM.addBAProp(customerCheckRetail, barcodeAddClient), 1, LM.barcodeToObject, 1);

        barcodeAddCert = LM.addSDProp("Доб. серт.", LogicalClass.instance);
        barcodeAddCertAction = LM.addJProp(true, "", LM.andNot1, LM.addBAProp(giftObligation, barcodeAddCert), 1, LM.barcodeToObject, 1);

        barcodeAction2 = LM.addJProp(true, "Ввод штрих-кода 2",
                LM.addCUProp(
                        LM.addSCProp(LM.addIfElseUProp(articleQuantity, articleOrderQuantity, LM.is(commitInc), 1)),
                        LM.addIfElseUProp(orderSaleUseObligation, issueObligation, LM.addJProp(LM.diff2, 1, obligationIssued, 2), 1, 2),
                        LM.addJProp(LM.equals2, subjectIncOrder, 1, 2),
                        xorActionArticle, articleFormatToSell, documentRevalued,
                        LM.addJProp(LM.and1, LM.changeUser, 2, LM.is(LM.baseClass), 1)
                ), 1, LM.barcodeToObject, 2);
        barcodeAction3 = LM.addJProp(true, "Ввод штрих-кода 3",
                LM.addCUProp(
                        LM.addJProp(LM.and(false, false), LM.changeUser, 2, LM.is(LM.baseClass), 1, LM.is(LM.baseClass), 3),
                        LM.addSCProp(returnInnerQuantity)
                ), 1, LM.barcodeToObject, 3, 2);

        LP xorCouponArticleGroup = LM.addDProp(couponGroup, "xorCouponArticleGroup", "Вкл.", LogicalClass.instance, articleGroup);
        xorCouponArticle = LM.addDProp(couponGroup, "xorCouponArticle", "Вкл./искл.", LogicalClass.instance, article);
        inCoupon = LM.addXorUProp(couponGroup, "inCoupon", true, "Выд. купон", xorCouponArticle, LM.addJProp(xorCouponArticleGroup, articleToGroup, 1));

        couponIssueSum = LM.addDProp(couponGroup, "couponIssueSum", "Сумма купона", DoubleClass.instance, DoubleClass.instance);

        LP couponDocToIssueSum = LM.addDCProp("couponDocToIssueSum", "Сумма купона к выд.", LM.addIfProp(LM.addMGProp(LM.addJProp(LM.and1, couponIssueSum, 3, LM.addJProp(LM.greater2, priceOrderArticle, 1, 2, 3), 1, 2, 3), 1, 2), false, inCoupon, 2), true, 1, 2, articleQuantity, 1, 2, commitSaleCheckArticleRetail); // здесь конечно хорошо было бы orderSaleDocPrice вытащить за скобки, но будет висячий ключ поэтому приходится пока немого извращаться

        couponToIssueQuantity = LM.addDUProp("К выдаче", LM.addSGProp(articleQuantity, 1, couponDocToIssueSum, 1, 2),
                LM.addSGProp(LM.addJProp(LM.and1, LM.addCProp(DoubleClass.instance, 1), issueCoupon, 1, 2), 1, obligationSum, 2));
        couponToIssueConstraint = LM.addJProp("Кол-во выданных купонов не соответствует требуемому", LM.diff2, couponToIssueQuantity, 1, 2, LM.vzero);
        LM.addConstraint(couponToIssueConstraint, false);

        orderUser = LM.addDCProp("orderUser", "Исп-ль заказа", LM.currentUser, true, LM.is(order), 1);
        orderUserName = LM.addJProp("Исп-ль заказа", LM.name, orderUser, 1);
        // вспомогательные свойства
        orderContragentBarcode = LM.addJProp("Штрих-код (кому)", LM.barcode, subjectIncOrder, 1);
        orderUserBarcode = LM.addJProp("Кассир", LM.barcode, orderUser, 1);

        orderComputer = LM.addDCProp("orderComputer", "Компьютер заказа", LM.currentComputer, true, LM.is(order), 1);
        orderComputerName = LM.addJProp("Компьютер заказа", LM.hostname, orderComputer, 1);


        LM.setNotNull(LM.addJProp("Штрих-код покупателя", LM.and1, LM.barcode, 1, LM.is(customerCheckRetail), 1));
        //setNotNull(LM.addJProp("Штрих-код товара", LM.and1, barcode, 1, LM.is(article), 1));
        LM.setNotNull(LM.addJProp("Штрих-код сертификата", LM.and1, LM.barcode, 1, LM.is(obligation), 1));
        //setNotNull(LM.addJProp(andNot1, barcode, 1, LM.is(user), 1));

        checkRetailExported = LM.addDProp("checkRetailExported", "Экспортирован", LogicalClass.instance, orderRetail);

        VEDBL.cashRegController = new CashRegController(this); // бред конечно создавать его здесь, но влом создавать getCashRegController()
        VEDBL.cashRegController.addCashRegProperties();

        LP importCustomerCheckRetail = LM.addProp(LM.baseGroup, new CustomerCheckRetailImportActionProperty(VEDBL, LM.genSID()));

        quantityCheckCommitInnerArticle = LM.addSDProp("quantityCheckCommitInnerArticle", "Кол-во свер.", DoubleClass.instance, commitInner, article);
        barcodeActionCheck = LM.addJProp(true, "Ввод штрих-кода (проверки)",
                LM.addCUProp(
                        LM.addSCProp(LM.addIfElseUProp(quantityCheckCommitInnerArticle, articleOrderQuantity, LM.is(commitInner), 1))
                ), 1, LM.barcodeToObject, 2);

        quantityDiffCommitArticle = LM.addDUProp(articleOrderQuantity, LM.addCUProp("Кол-во свер.", outerCommitedQuantity, quantityCheckCommitInnerArticle));

        // для импорта
        nameToCurrency = LM.addAGProp("nameToCurrency", "Валюта", currency, LM.name);
        nameToArticleGroup = LM.addAGProp("nameToArticleGroup", "Гр. тов.", articleGroup, LM.name);
        nameToUnitOfMeasure = LM.addAGProp("nameToUnitOfMeasure", "Ед. изм.", unitOfMeasure, LM.name);
        nameToBrend = LM.addAGProp("nameToBrend", "Бренд", brend, LM.name);
        nameToGender = LM.addAGProp("nameToGender", "Пол", gender, LM.name);

        dateLastImportShop = LM.addDProp(cashRegGroup, "dateLastImportSh", "Дата прайса", DateClass.instance, shop);
        dateLastImport = LM.addJProp(cashRegGroup, "dateLastImport", "Дата прайса", dateLastImportShop, currentShop);

        padlBarcodeToObject = LM.addJProp(LM.privateGroup, true, "Объект (до 12)", LM.barcodeToObject, padl, 1);

        // для накладной
        seriesInvoiceDocument = LM.addDProp(documentShipmentGroup, "seriesInvoiceDocument", "Серия", StringClass.get(4), shipmentDocument);
        numberInvoiceDocument = LM.addDProp(documentShipmentGroup, "numberInvoiceDocument", "Номер", StringClass.get(15), shipmentDocument);
        invoiceDocumentNumber = LM.addAGProp("invoiceDocumentNumber", "Документ", numberInvoiceDocument);

        legalOutContract = LM.addGDProp(LM.idGroup, "Contract", "legalOut", "Кого (ИД)", new ValueClass[]{supplier, storeLegalEntity}, new CustomClass[]{contractSupplier, contractSale});
        nameLegalOutContract = LM.addJProp(LM.baseGroup, "nameLegalOutContract", "Кого", LM.name, legalOutContract, 1);
        legalIncContract = LM.addGDProp(LM.idGroup, "Contract", "legalInc", "С кем (ИД)", new ValueClass[]{storeLegalEntity, customerWhole}, new CustomClass[]{contractDelivery, contractCustomer});
        nameLegalIncContract = LM.addJProp(LM.baseGroup, "nameLegalIncContract", "С кем", LM.name, legalIncContract, 1);

        LP legalEntityDoIncOrder = LM.addIfElseUProp(LM.privateGroup, "legalEntityDoIncOrder", "Договор", legalEntityIncOrder, legalEntityOutOrder, LM.is(orderDo), 1);
        LP legalEntityDoOutOrder = LM.addIfElseUProp(LM.privateGroup, "legalEntityDoOutOrder", "Договор", legalEntityOutOrder, legalEntityIncOrder, LM.is(orderDo), 1);
        contractOrder = LM.addDProp(LM.idGroup, "contractOrder", "Договор (ИД)", contract, orderWhole);
        LP nameContractOrder = LM.addJProp(LM.baseGroup, "nameContractOrder", "Договор", LM.name, contractOrder, 1);

        LM.addConstraint(LM.addJProp("только между этими юрлицами", LM.diff2, legalEntityDoIncOrder, 1, LM.addJProp(legalIncContract, contractOrder, 1), 1), true);
        LM.addConstraint(LM.addJProp("только между этими юрлицами", LM.diff2, legalEntityDoOutOrder, 1, LM.addJProp(legalOutContract, contractOrder, 1), 1), true);

        formatAssortment = LM.addDProp("formatAssortment", "Формат ассортимента", format, assortment);
        nameFormatAssortment = LM.addJProp(LM.baseGroup, "nameFormatAssortment", "Имя формата", LM.name, formatAssortment, 1);
        contractSpecification = LM.addDProp("contractSpecification", "договор спецификации", contract, specification);
        nameContractSpecification = LM.addJProp(LM.baseGroup, "nameContractSpecification", "Имя договора", LM.name, contractSpecification, 1);

        articleToSpecification = LM.addDProp(LM.baseGroup, "articleToSpecification", "Принадлежит спецификации", LogicalClass.instance, article, specification);
        specificationDateFrom = LM.addDProp("specificationDateFrom", "Дата (С)", DateClass.instance, specification);
        specificationDateTo = LM.addDProp("specificationDateTo", "Дата (По)", DateClass.instance, specification);
        assortmentDateFrom = LM.addDProp("assortmentDateFrom", "Дата (С)", DateClass.instance, assortment);
        assortmentDateTo = LM.addDProp("assortmentDateTo", "Дата (По)", DateClass.instance, assortment);

        //!!!
        articleToDocument = LM.addMGProp(LM.baseGroup, "articleToDocument", "Входит в договор документа", LM.addJProp(LM.and(false, false, false),
                articleToSpecification, 1, 2,
                LM.addJProp(LM.less2, specificationDateFrom, 1, LM.date, 2), 2, 3,
                LM.addJProp(LM.greater2, specificationDateTo, 1, LM.date, 2), 2, 3,
                LM.addJProp(LM.equals2, contractSpecification, 1, contractOrder, 2), 2, 3), 1, 3);
        LM.addConstraint(LM.addJProp("Можно выбирать товары только согласно договору", LM.andNot1, articleInnerQuantity, 2, 1, LM.addJProp(LM.equals2, LM.vtrue, articleToDocument, 1, 2), 1, 2), true);
        LM.addConstraint(LM.addJProp("Можно выбирать товары только согласно договору", LM.andNot1, outerOrderQuantity, 2, 1, LM.addJProp(LM.equals2, LM.vtrue, articleToDocument, 1, 2), 1, 2), true);

        LP invoiceOrderRetail = LM.addDProp("invoiceOrderRetail", "Счет-фактура", StringClass.get(50), orderSaleInvoiceArticleRetail);
        LP purposeOrderRetail = LM.addDProp("purposeOrderRetail", "Цель приобретения", StringClass.get(50), orderSaleInvoiceArticleRetail);
        permissionOrder = LM.addCUProp(documentInvoiceSaleGroup, "permissionOrder", "Основание отпуска", invoiceOrderRetail, nameContractOrder);
        purposeOrder = LM.addCUProp(documentInvoiceSaleGroup, "purposeOrder", "Цель приобретения", purposeOrderRetail);

        LP[] propsInvoiceDocument = LM.addDProp(documentShipmentOutGroup, "InvoiceDocument",
                        new String[]{"personPermission", "personOut", "personWarrant", "warrantBy", "personInc"},
                        new String[]{"Отпуск разрешил", "Отпуск произвел", "Кому выд. ТМЦ (по дов.)", "По доверенности выд.", "Товар получил"},
                        StringClass.getArray(50,60,60,60,70,60), shipmentDocumentOut);
        LP[] propsInvoiceTransportDocument = LM.addDProp(documentShipmentTransportGroup, "InvoiceDocument",
                        new String[]{"personPRR", "typePRR", "codePRR", "timeOut", "timeInc", "timeDelay",
                                "transport", "transportList", "personTransport", "personDriver", "personRespTransport", "typeTransport", "route", "readdress", "trailer", "garageNumber"},
                        new String[]{"Исполнитель ПРР", "Способ ПРР", "Код ПРР", "Убытие", "Прибытие", "Простой",
                                "Автомобиль", "Путевой лист", "Владелец автотранспорта", "Водитель", "Экспедитор", "Вид перевозки", "Маршрут", "Переадресовка", "Прицеп", "Гаражный номер", ""},
                        StringClass.getArray(60,20,10,8,8,8, 20,10,60,60,60,20,20,50,30,15), shipmentDocumentOut);



        shopOutName  = LM.addJProp(LM.baseGroup, "Магазин расхода", nameSubjectOutOrder, obligationIssued, 1);
        shopIncName  = LM.addJProp(LM.baseGroup, "Магазин использования", nameSubjectOutOrder, obligationDocument, 1);

        clientIncName  = LM.addJProp(LM.baseGroup, "clientIncName", "Клиент", nameSubjectIncOrder, obligationIssued, 1);
        clientOutName  = LM.addJProp(LM.baseGroup, "clientOutName", "Клиент", nameSubjectIncOrder, obligationDocument, 1);

        sumWithDiscountObligation = LM.addJProp(LM.baseGroup, "sumWithDiscountObligation", "Сумма со скидкой", sumWithDiscountOrder, obligationDocument, 1);
        discountSumObligation = LM.addJProp(LM.baseGroup, "discountSumObligation", "Сумма скидки", discountSumOrder, obligationDocument, 1);

        obligFromIssued = LM.addJProp(LM.baseGroup, "Дата выдачи", LM.date, obligationIssued,1);
        obligToIssued = LM.addJProp(LM.baseGroup, "Дата использования", LM.date, obligationDocument, 1);


       articleIncDocumentQuantity = LM.addJProp(LM.baseGroup, "articleIncDocumentQuantity", "Кол-во", articleQuantity, obligationDocument, 1, 2);
       articleOutDocumentQuantity = LM.addJProp(LM.baseGroup, "articleOutDocumentQuantity", "Кол-во", articleQuantity, obligationIssued, 1, 2);

       articleIncDocumentPrice = LM.addJProp(LM.baseGroup, "articleIncDocumentPrice", "Цена", orderSalePrice, obligationDocument, 1, 2);
       articleOutDocumentPrice = LM.addJProp(LM.baseGroup, "articleOutDocumentPrice", "Цена", orderSalePrice, obligationIssued, 1, 2);

        initDateProperties();
    }

    void initDateProperties() {

        betweenDate2 = LM.addJProp(LM.between, LM.date, 1, LM.object(DateClass.instance), 2, LM.object(DateClass.instance), 3);

        quantitySupplierArticleBetweenDates = LM.addSGProp("quantitySupplierArticleBetweenDates", "Проданное кол-во",
                LM.addJProp(LM.and(false,false), innerQuantity, 1, 2, 3, LM.is(orderSale), 1, betweenDate2, 1, 4, 5), contragentOrder, 3, 2, 4, 5);
        sumNoNDSSupplierArticleBetweenDates = LM.addSGProp("sumNoNDSSupplierArticleBetweenDates", "Сумма поставщика без НДС",
                LM.addJProp(LM.and(false,false), sumNoNDSOrderArticleExtInc, 1, 2, 3, LM.is(orderSale), 1, betweenDate2, 1, 4, 5), contragentOrder, 3, 2, 4, 5);
        sumNoNDSRetailSupplierArticleBetweenDates = LM.addSGProp("sumNoNDSRetailSupplierArticleBetweenDates", "Сумма продажи без НДС",
                LM.addJProp(LM.and(false,false), sumNoNDSRetailArticleExtInc, 1, 2, 3, LM.is(orderSale), 1, betweenDate2, 1, 4, 5), contragentOrder, 3, 2, 4, 5);
        markUpSupplierShopArticleBetweenDates = LM.addDUProp("markUpSupplierShopArticleBetweenDates","Сумма наценки", sumNoNDSRetailSupplierArticleBetweenDates, sumNoNDSSupplierArticleBetweenDates);

        markUpPercentSupplierShopArticleBetweenDates = LM.addJProp(documentRetailGroup, "markUpPercentSupplierShopArticleBetweenDates", "Наценка", round0, LM.addJProp(calcPercent, markUpSupplierShopArticleBetweenDates, 1, 2, 3, 4, sumNoNDSSupplierArticleBetweenDates, 1, 2, 3, 4), 1, 2, 3, 4);



        betweenObligationIssuedDateFromDateTo = LM.addJProp(LM.between, dateIssued, 1, LM.object(DateClass.instance), 2, LM.object(DateClass.instance), 3);
        betweenObligationToIssuedDateFromDateTo = LM.addJProp(LM.between, obligToIssued, 1, LM.object(DateClass.instance), 2, LM.object(DateClass.instance), 3);




        betweenDate = LM.addJProp(LM.between, LM.date, 1, LM.object(DateClass.instance), 2, LM.object(DateClass.instance), 3);

        sumRetailIncBetweenDate = LM.addSGProp(LM.baseGroup, "sumRetailIncBetweenDate", "Приходная сумма за интервал",
                LM.addJProp(LM.and1, sumRetailOrder, 1, betweenDate, 1, 2, 3), subjectIncOrder, 1, 2, 3);
        sumRetailOutBetweenDate = LM.addSGProp(LM.baseGroup, "sumRetailOutBetweenDate", "Расходная сумма за интервал",
                LM.addJProp(LM.and1, sumRetailOrder, 1, betweenDate, 1, 2, 3), subjectOutOrder, 1, 2, 3);

        sumWithDiscountCouponIncBetweenDate = LM.addSGProp(LM.baseGroup, "sumWithDiscountCouponIncBetweenDate", "Приходная сумма без скидки за интервал",
                LM.addJProp(LM.and1, sumWithDiscountCouponOrder, 1, betweenDate, 1, 2, 3), subjectIncOrder, 1, 2, 3);
        sumWithDiscountCouponOutBetweenDate = LM.addSGProp(LM.baseGroup, "sumWithDiscountCouponOutBetweenDate", "Расходная сумма без скидки за интервал",
                LM.addJProp(LM.and1, sumWithDiscountCouponOrder, 1, betweenDate, 1, 2, 3), subjectOutOrder, 1, 2, 3);

        sumDiscountPayCouponIncBetweenDate = LM.addSGProp(LM.baseGroup, "sumDiscountPayCouponIncBetweenDate", "Приходная сумма скидки за интервал",
                LM.addJProp(LM.and1, sumDiscountPayCouponOrder, 1, betweenDate, 1, 2, 3), subjectIncOrder, 1, 2, 3);
        sumDiscountPayCouponOutBetweenDate = LM.addSGProp(LM.baseGroup, "sumDiscountPayCouponOutBetweenDate", "Расходная сумма скидки за интервал",
                LM.addJProp(LM.and1, sumDiscountPayCouponOrder, 1, betweenDate, 1, 2, 3), subjectOutOrder, 1, 2, 3);

        sumPriceChangeBetweenDate = LM.addSGProp(LM.baseGroup, "sumPriceChangeBetweenDate", "Сумма переоценки за интервал",
                LM.addJProp(LM.and1, sumPriceChangeOrder, 1, betweenDate, 1, 2, 3), subjectIncOrder, 1, 2, 3);

        quantityIncSubjectArticleBetweenDate = LM.addSGProp(LM.baseGroup, "quantityIncSubjectArticleBetweenDate", "Кол-во прих. за интервал",
                LM.addJProp(LM.and1, quantityCommitIncArticle, 1, 2, 3, betweenDate, 1, 4, 5), subjectIncOrder, 1, 2, 4, 5);
        quantityOutSubjectArticleBetweenDate = LM.addSGProp(LM.baseGroup, "quantityOutSubjectArticleBetweenDate", "Кол-во расх. за интервал",
                LM.addJProp(LM.and1, quantityCommitOutArticle, 1, 2, 3, betweenDate, 1, 4, 5), subjectIncOrder, 1, 2, 4, 5);
    }

    LP padlBarcodeToObject;
    LP nameToCurrency;
    LP nameToArticleGroup;
    LP nameToUnitOfMeasure;
    LP nameToBrend;

    private LP initRequiredStorePrice(AbstractGroup group, String sID, boolean persistent, String caption, LP deliveryPriceStoreArticle, LP storeProp) {
        LP currentRRPPriceObjectArticle = LM.addJProp(currentRRPPriceStoreArticle, storeProp, 1, 2);
        LP currentObjectDiscount = LM.addJProp(currentStoreDiscount, storeProp, 1);
        LP discountPriceObjectArticle = LM.addJProp(discountArticleStore, 2, storeProp, 1);

        LP addDeliveryObjectArticle = LM.addJProp("Необх. цена с нац.", addPercent, LM.addJProp(addPercent, deliveryPriceStoreArticle, 1, 2, addvArticle, 2), 1, 2, currentNDS, 2);
        LP currentPriceObjectArticle = LM.addSUProp("Необх. цена", Union.MAX, addDeliveryObjectArticle, currentRRPPriceObjectArticle);
        LP actionPriceObjectArticle = LM.addJProp("Акц. цена", removePercent, currentPriceObjectArticle, 1, 2, discountPriceObjectArticle, 1, 2);
        return LM.addJProp(group, sID, persistent, caption, round1, LM.addJProp(removePercent, actionPriceObjectArticle, 1, 2, currentObjectDiscount, 1), 1, 2);
    }

    LP barcodeActionCheck, quantityCheckCommitInnerArticle, quantityDiffCommitArticle;
    LP impSumCard;
    LP impSumCash;
    LP impSumBank;

    private LP addSupplierProperty(LP property) {
        return LM.addSUProp(Union.SUM, property, LM.addSGProp(property, articleStoreSupplier, 1, 2, 2));
    }

    LP orderUserName;
    LP orderComputerName;
    LP articleToGroup;
    LP couponToIssueConstraint;
    LP couponIssueSum;
    LP couponToIssueQuantity;
    LP xorCouponArticle;
    LP inCoupon;
    public LP issueObligation;
    public LP obligationSum;
    LP barcodeAddClient;
    LP barcodeAddClientAction;
    LP barcodeAddCert;
    LP barcodeAddCertAction;
    LP barcodeAction2;
    LP barcodeAction3;
    LP orderClientSum;
    public LP sumWithDiscountOrderArticle;
    LP priceOrderArticle;
    LP priceNoNDSOrderArticle;
    LP priceNoNDSRetailArticle;
    LP priceManfrOrderArticle;
    LP sumManfrOrderArticle;
    LP sumOrderArticle;
    LP sumNoNDSOrderArticleExtInc;
    LP sumNoNDSRetailArticleExtInc;
    public LP priceAllOrderSaleArticle;
    LP changeQuantityOrder;
    LP computerShop;
    LP currentShop;
    LP panelScreenComPort;
    public LP cashRegComPort;
    LP orderSalePayCash;
    LP orderSalePayCard;
    LP changeQuantityTime;
    LP confirmedInnerQuantity;
    LP couponStart;
    LP obligationDocument;
    LP orderSaleObligationCanNotBeUsed;
    LP orderSaleUseObligation;

    public LP xorActionArticle;
    LP inAction;
    LP orderSalePayObligation;
    LP sumWithDiscountObligationOrder;
    public LP sumWithDiscountObligationOrderArticle;
    public LP clientInitialSum;
    LP clientSum;
    LP accumulatedClientSum;
    LP nameSubjectIncOrder;
    LP nameImplSubjectIncOrder;
    LP nameSubjectOutOrder;

    LP addressSubjectIncOrder;
    LP addressSubjectOutOrder;

    public LP subjectIncOrder;
    public LP subjectOutOrder;

    LP articleFreeOrderQuantity;

    LP nameArticleGroupArticle;

    LP articleSupplier;
    LP nameArticleSupplier;
    LP articleStoreSupplier;
    LP articleStorePeriod;
    LP articleStoreMin;
    LP articleFullStoreDemand;

    LP bornCustomerCheckRetail;
    LP[] propsCustomerCheckRetail;
    LP[] propsCustomerContragentOrder, propsCustomerImplContragentOrder;
    LP[] propsCustomerIncOrder, propsCustomerImplIncOrder;
    LP[] propsLegalEntityIncOrder, propsLegalEntityOutOrder;

//    LP logClientInitialSum;

    LP documentLogisticsSupplied, documentLogisticsRequired, documentLogisticsRecommended;
    LP currentNDSDate, currentNDSDoc, currentNDS, NDS;
    LP ndsOrderArticle;
    LP sumAddvOrderArticle;
    LP sumPriceChangeOrderArticle;
    LP sumRevalBalance;
    LP revalChangeBalance;
    LP sumNewPrevRetailOrderArticle;
    LP sumNewPrevRetailOrder;
    LP sumRetailOrderArticle;
    LP sumPrevRetailOrderArticle;
    LP sumPrevRetailOrder;
    public LP<?> articleQuantity, prevPrice, revalBalance;
    LP articleOrderQuantity;
    LP discountSumOrder, sumWithDiscountOrder, orderSaleDiff;
    LP orderSaleToDo, orderSaleToDoSum;
    public LP discountOrderArticle;
    public LP discountSumOrderArticle;
    LP orderNoDiscount;
    public LP shopPrice;
    LP ndsShopOrderPriceArticle;
    LP priceStore, inDocumentPrice;
    LP isRevalued, isNewPrice, documentRevalued;
    LP balanceFormatFreeQuantity;
    LP currentShopPriceDate;
    LP currentShopPriceDoc;
    LP currentShopPrice;
    LP currentRRP;
    LP currencyArticle;
    LP nameCurrencyArticle;
    LP unitOfMeasureArticle;
    LP nameUnitOfMeasureArticle;
    LP returnInnerFreeQuantity;
    LP sumReturnedQuantity;
    LP sumReturnedQuantityFree;

    public LP documentFreeQuantity, documentInnerFreeQuantity, returnFreeQuantity, innerQuantity, returnInnerCommitQuantity, returnInnerQuantity;
    public LP outerOrderQuantity, outerCommitedQuantity, articleBalanceCheck, articleBalanceCheckDB, innerBalanceCheck, innerBalanceCheckDB, balanceSklCommitedQuantity;
    public LP articleInnerQuantity;


    @Override
    public void initIndexes() {
        LM.addIndex(articleInnerQuantity);
    }

    private NavigatorElement saleRetailCashRegisterElement;
    FormEntity commitSaleForm;
    FormEntity saleCheckCertForm;
    FormEntity cachRegManagementForm;
    FormEntity returnSaleCheckRetailArticleForm;

    public CommitSaleCheckRetailFormEntity commitSaleBrowseForm;
    public SaleCheckCertFormEntity saleCheckCertBrowseForm;
    public ReturnSaleCheckRetailFormEntity returnSaleCheckRetailBrowse;

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement print = new NavigatorElement(LM.baseElement, "print", "Печатные формы");
        FormEntity incomePrice = LM.addFormEntity(new IncomePriceFormEntity(print, "incomePrice"));
        FormEntity revalueAct = LM.addFormEntity(new RevalueActFormEntity(print, "revalueAct"));
        FormEntity pricers = LM.addFormEntity(new PricersFormEntity(print, "pricers"));
        FormEntity stickers = LM.addFormEntity(new StickersFormEntity(print, "stickers"));
        FormEntity invoice = LM.addFormEntity(new InvoiceFormEntity(print, "invoice", "Счет-фактура", true));
        FormEntity ttn1Blank = LM.addFormEntity(new TTNFormEntity(print, "ttn1blank", "ТТН-1 бланк (гориз.)", false));
        FormEntity ttn1BlankV = LM.addFormEntity(new TTNFormEntity(print, "ttn1blank_v", "ТТН-1 бланк", false));
        FormEntity ttn1Attach = LM.addFormEntity(new TTNFormEntity(print, "ttn1attach", "ТТН-1 приложение (гориз.)", true));
        FormEntity ttn1AttachV = LM.addFormEntity(new TTNFormEntity(print, "ttn1attach_v", "ТТН-1 приложение", true));
        FormEntity ttn1SideA = LM.addFormEntity(new TTNFormEntity(print, "ttn1a", "ТТН-1 сторона A (гориз.)", true));
        FormEntity ttn1SideAV = LM.addFormEntity(new TTNFormEntity(print, "ttn1a_v", "ТТН-1 сторона A", true));
        FormEntity ttn1SideB = LM.addFormEntity(new TTNFormEntity(print, "ttn1b", "ТТН-1 сторона B (гориз.)", false));
        FormEntity ttn1SideBV = LM.addFormEntity(new TTNFormEntity(print, "ttn1b_v", "ТТН-1 сторона B", false));
        FormEntity tn2Blank = LM.addFormEntity(new TNFormEntity(print, "tn2blank", "ТН-2 бланк (гориз.)", false));
        FormEntity tn2BlankV = LM.addFormEntity(new TNFormEntity(print, "tn2blank_v", "ТН-2 бланк", false));
        FormEntity tn2Attach = LM.addFormEntity(new TNFormEntity(print, "tn2attach", "ТН-2 приложение (гориз.)", true));
        FormEntity tn2AttachV = LM.addFormEntity(new TNFormEntity(print, "tn2attach_v", "ТН-2 приложение", true));
        FormEntity tn2 = LM.addFormEntity(new TNFormEntity(print, "tn2", "ТН-2 (одн. стр.) (гориз.)", true));
        FormEntity tn2V = LM.addFormEntity(new TNFormEntity(print, "tn2_v", "ТН-2 (одн. стр.)", true));

        LM.addFormEntity(new ArticleReportFormEntity(print, "articleReport"));
        createArticleForm = LM.addFormEntity(new CreateArticleFormEntity(null, "createArticleForm", "Ввод нового товара"));

        NavigatorElement classifier = new NavigatorElement(LM.baseElement, "classifier", "Справочники");
            LM.addFormEntity(new ArticleInfoFormEntity(classifier, "articleInfoForm"));
            LM.addFormEntity(new StoreInfoFormEntity(classifier, "storeInfoForm"));
            LM.addFormEntity(new DocumentArticleFormEntity(classifier, "DocumentArticleForm"));
            LM.addFormEntity(new ArticleSupplierFormEntity(classifier, "ArticleSupplierForm"));
            LM.addFormEntity(new ArticleSpecificationFormEntity(classifier, " ArticleSpecificationForm"));


        NavigatorElement delivery = new NavigatorElement(LM.baseElement, "delivery", "Управление закупками");
            LM.addFormEntity(new ContragentWholeArticleFormEntity(delivery, "ContragentWholeArticleForm1", true));
            LM.addFormEntity(new SpecificationSupplierFormEntity(delivery, "SpecificationSupplierForm"));
            NavigatorElement deliveryLocal = new NavigatorElement(delivery, "deliveryLocal", "Закупки у местных поставщиков");
                NavigatorElement deliveryShopLocal = new NavigatorElement(deliveryLocal, "deliveryShopLocal", "Закупки на магазин");
                    FormEntity deliveryCommitShopLocal = LM.addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, true, "deliveryCommitShopLocal", 1));
                    deliveryCommitShopLocal.caption = "Ввод прихода";
                    FormEntity deliveryOrderShopLocal = LM.addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, true, "deliveryOrderShopLocal", 0));
                    deliveryOrderShopLocal.caption = "Ввод заявки";
                    FormEntity deliveryShopLocalBrowse = LM.addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, false, "deliveryShopLocalBrowse", 0));
                    deliveryShopLocalBrowse.caption = "Список документов";
                NavigatorElement deliveryWarehouseLocal = new NavigatorElement(deliveryLocal, "deliveryWarehouseLocal", "Закупки на распред. центр");
                    FormEntity deliveryWarehouseShopLocal = LM.addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, true, "deliveryWarehouseShopLocal", 1));
                    deliveryWarehouseShopLocal.caption = "Ввод прихода";
                    FormEntity deliveryOrderWarehouseLocal = LM.addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, true, "deliveryOrderWarehouseLocal", 0));
                    deliveryOrderWarehouseLocal.caption = "Ввод заявки";
                    FormEntity deliveryCommitWarehouseLocal = LM.addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, false, "deliveryCommitWarehouseLocal", 0));
                    deliveryCommitWarehouseLocal.caption = "Список документов";
                NavigatorElement returnDeliveryLocal = new NavigatorElement(deliveryLocal, "returnDeliveryLocal", "Возвраты поставщику");
                    FormEntity returnCommitDeliveryLocal = LM.addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, true, "returnCommitDeliveryLocal", 1));
                    returnCommitDeliveryLocal.caption = "Ввод отгрузки";
                    FormEntity returnOrderDeliveryLocal = LM.addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, true, "returnOrderDeliveryLocal", 0));
                    returnOrderDeliveryLocal.caption = "Ввод заявки";
                    FormEntity returnDeliveryLocalBrowse = LM.addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, false, "returnDeliveryLocalBrowse", 0));
                    returnDeliveryLocalBrowse.caption = "Список документов";
            NavigatorElement deliveryImport = new NavigatorElement(delivery, "deliveryImport", "Закупки у импортных поставщиков");
                FormEntity deliveryCommitImport = LM.addFormEntity(new DeliveryImportFormEntity(deliveryImport, true, "deliveryCommitImport", 1));
                deliveryCommitImport.caption = "Ввод прихода";
                FormEntity deliveryOrderImport = LM.addFormEntity(new DeliveryImportFormEntity(deliveryImport, true, "deliveryOrderImport", 0));
                deliveryOrderImport.caption = "Ввод заявки";
                FormEntity deliveryImportBrowse = LM.addFormEntity(new DeliveryImportFormEntity(deliveryImport, false, "deliveryImportBrowse", 0));
                deliveryImportBrowse.caption = "Список документов";

        NavigatorElement sale = new NavigatorElement(LM.baseElement, "sale", "Управление продажами");
            NavigatorElement saleRetailElement = new NavigatorElement(sale, "saleRetailElement", "Управление розничными продажами");
                saleRetailCashRegisterElement = new NavigatorElement(saleRetailElement, "saleRetailCashRegisterElement", "Касса");
                    LM.addFormEntity(new ShopArticleFormEntity(saleRetailCashRegisterElement, "shopArticleForm", "Товары в других магазинах"));
                    commitSaleForm = LM.addFormEntity(new CommitSaleCheckRetailFormEntity(saleRetailCashRegisterElement, "commitSaleForm", true, false));
                        LM.addFormEntity(new CommitSaleCheckRetailFormEntity(commitSaleForm, "commitSaleCheckRetailForm", false, false));
                        commitSaleBrowseForm = LM.addFormEntity(new CommitSaleCheckRetailFormEntity(commitSaleForm, "commitSaleBrowseForm", false, true));
                        LM.addFormEntity(new CommitSaleCheckRetailExcelFormEntity(commitSaleForm, "commitSaleCheckRetailExcelForm", "Выгрузка в Excel"));
                    saleCheckCertForm = LM.addFormEntity(new SaleCheckCertFormEntity(saleRetailCashRegisterElement, "saleCheckCertForm", true, false));
                        LM.addFormEntity(new SaleCheckCertFormEntity(saleCheckCertForm, "saleCheckCertForm2", false, false));
                        saleCheckCertBrowseForm = LM.addFormEntity(new SaleCheckCertFormEntity(saleCheckCertForm, "saleCheckCertBrowseForm", false, true));
                    returnSaleCheckRetailArticleForm = LM.addFormEntity(new ReturnSaleCheckRetailFormEntity(saleRetailCashRegisterElement, true, "returnSaleCheckRetailArticleForm", false));
                        LM.addFormEntity(new ReturnSaleCheckRetailFormEntity(returnSaleCheckRetailArticleForm, false, "returnSaleCheckRetailArticleForm2", false));
                        returnSaleCheckRetailBrowse = LM.addFormEntity(new ReturnSaleCheckRetailFormEntity(returnSaleCheckRetailArticleForm, false, "returnSaleCheckRetailBrowse", true));
                    cachRegManagementForm = LM.addFormEntity(VEDBL.cashRegController.createCashRegManagementFormEntity(saleRetailCashRegisterElement, "cachRegManagementForm"));
                    LM.addFormEntity(new ShopMoneyFormEntity(saleRetailCashRegisterElement, "shopMoneyForm", "Данные из касс"));
                    LM.addFormEntity(new ClientFormEntity(saleRetailCashRegisterElement, "clientForm", "Редактирование клиентов"));
                NavigatorElement saleRetailInvoice = new NavigatorElement(saleRetailElement, "saleRetailInvoice", "Безналичный расчет");
                    FormEntity saleRetailInvoiceForm = LM.addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoice, "saleRetailInvoiceForm", true, false));
                        LM.addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoiceForm, "orderSaleInvoiceRetailForm", false, false));
                    FormEntity saleInvoiceCert = LM.addFormEntity(new SaleInvoiceCertFormEntity(saleRetailInvoice, "saleInvoiceCert", true, false));
                        LM.addFormEntity(new SaleInvoiceCertFormEntity(saleInvoiceCert, "saleInvoiceCert2", false, false));
                    FormEntity returnSaleInvoiceRetailArticle = LM.addFormEntity(new ReturnSaleInvoiceRetailFormEntity(saleRetailInvoice, true, "returnSaleInvoiceRetailArticle", false));
                        LM.addFormEntity(new ReturnSaleInvoiceRetailFormEntity(returnSaleInvoiceRetailArticle, false, "returnSaleInvoiceRetailArticle2", false));
                        LM.addFormEntity(new ReturnSaleInvoiceRetailFormEntity(returnSaleInvoiceRetailArticle, false, "returnSaleInvoiceRetailArticle3", true));
            NavigatorElement saleWhole = new NavigatorElement(sale, "saleWhole", "Управление оптовыми продажами");
                LM.addFormEntity(new CustomerWholeFormEntity(saleWhole, "customerWholeForm"));
                LM.addFormEntity(new ContragentWholeArticleFormEntity(saleWhole, "ContragentWholeArticleForm2", false));
                FormEntity saleWholeForm = LM.addFormEntity(new SaleWholeFormEntity(saleWhole, "saleWholeForm", true));
                    LM.addFormEntity(new SaleWholeFormEntity(saleWholeForm, "saleWholeForm2", false));
                FormEntity returnSaleWholeArticle = LM.addFormEntity(new ReturnSaleWholeFormEntity(saleWhole, "returnSaleWholeArticle", true));
                    LM.addFormEntity(new ReturnSaleWholeFormEntity(returnSaleWholeArticle, "returnSaleWholeArticle2", false));

        NavigatorElement distribute = new NavigatorElement(LM.baseElement, "distribute", "Управление распределением");
            LM.addFormEntity(new StoreLegalEntityFormEntity(distribute, "storeLegalEntityForm"));
            FormEntity distributeShopForm = LM.addFormEntity(new DistributeShopFormEntity(distribute, "distributeShopForm", true));
                FormEntity distributeShopBrowseForm = LM.addFormEntity(new DistributeShopFormEntity(distributeShopForm, "distributeShopBrowseForm", false));
            FormEntity distributeWarehouseForm = LM.addFormEntity(new DistributeWarehouseFormEntity(distribute, "distributeWarehouseForm", true));
                FormEntity distributeWarehouseBrowseForm = LM.addFormEntity(new DistributeWarehouseFormEntity(distributeWarehouseForm, "distributeWarehouseBrowseForm", false));

        NavigatorElement price = new NavigatorElement(LM.baseElement, "price", "Управление ценообразованием");
            FormEntity documentRevalue = LM.addFormEntity(new DocumentRevalueFormEntity(price, true, "documentRevalue"));
                LM.addFormEntity(new DocumentRevalueFormEntity(documentRevalue, false, "documentRevalue2"));

        NavigatorElement toSell = new NavigatorElement(LM.baseElement, "toSell", "Управление ассортиментом");
            //LM.addFormEntity(new ArticleFormatFormEntity(toSell, "articleFormatForm", true));
            //LM.addFormEntity(new ArticleFormatFormEntity(toSell, "articleFormatForm2", false));
            LM.addFormEntity(new AssortmentFormEntity(toSell, "AssortmentFormEntity"));

        NavigatorElement tax = new NavigatorElement(LM.baseElement, "tax", "Управление налогами");
            FormEntity nds = LM.addFormEntity(new DocumentNDSFormEntity(tax, true, "nds"));
                LM.addFormEntity(new DocumentNDSFormEntity(nds, false, "nds2"));

        NavigatorElement actions = new NavigatorElement(LM.baseElement, "actions", "Управление акциями");
            FormEntity saleAction = LM.addFormEntity(new ActionFormEntity(actions, "saleAction"));
            FormEntity couponInterval = LM.addFormEntity(new CouponIntervalFormEntity(actions, "couponInterval"));
            FormEntity couponArticle = LM.addFormEntity(new CouponArticleFormEntity(actions, "couponArticle"));
            FormEntity obligationDocument = LM.addFormEntity(new obligationDocumentFormEntity(actions, "obligationDocument"));

        NavigatorElement balance = new NavigatorElement(LM.baseElement, "balance", "Управление хранением");
            FormEntity balanceCheck = LM.addFormEntity(new BalanceCheckFormEntity(balance, "balanceCheck", true));
                LM.addFormEntity(new BalanceCheckFormEntity(balanceCheck, "balanceCheck2", false));

        NavigatorElement store = new NavigatorElement(LM.baseElement, "store", "Сводная информация");
            LM.addFormEntity(new StoreArticleFormEntity(store, "storeArticleForm"));

        LM.addFormEntity(new GlobalFormEntity(LM.baseElement, "globalForm"));
        FormEntity deliveryShopImport = LM.addFormEntity(new DeliveryShopLocalFormEntity(LM.baseElement, false, "deliveryShopImport", 0, true));
        deliveryShopImport.caption = "Импорт";

//        FormEntity logClient = LM.addFormEntity(new LogClientFormEntity(actions, "logClientForm"));

        commitDoShopInc.addRelevant(incomePrice);
        documentShopPrice.addRelevant(revalueAct);
        documentShopPrice.addRelevant(pricers);
    }


    public static Font FONT_SMALL_BOLD = new Font("Tahoma", Font.BOLD, 12);
    public static Font FONT_SMALL_PLAIN = new Font("Tahoma", Font.PLAIN, 12);
    public static Font FONT_MEDIUM_BOLD = new Font("Tahoma", Font.BOLD, 14);
    public static Font FONT_MEDIUM_PLAIN = new Font("Tahoma", Font.PLAIN, 14);
    public static Font FONT_LARGE_BOLD = new Font("Tahoma", Font.BOLD, 18);
    public static Font FONT_HUGE_BOLD = new Font("Tahoma", Font.BOLD, 23);


    private class GlobalFormEntity extends FormEntity {
        protected GlobalFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Глобальные параметры");
            addPropertyDraw(LM.publicGroup, true);

        }
    }

    private class ClientFormEntity extends FormEntity {

        private ClientFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            GroupObjectEntity gobjFindClient = new GroupObjectEntity(genID());
            ObjectEntity barcodeClient = new ObjectEntity(genID(), StringClass.get(13), "Введите штрих код");
            ObjectEntity nameClient = new ObjectEntity(genID(), StringClass.get(100), "Введите ФИО");

            gobjFindClient.add(barcodeClient);
            gobjFindClient.add(nameClient);

            addGroup(gobjFindClient);

            gobjFindClient.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjFindClient.initClassView = ClassViewType.PANEL;

            addPropertyDraw(barcodeClient, LM.objectValue);
            addPropertyDraw(nameClient, LM.objectValue);

            ObjectEntity objClient = addSingleGroupObject(customerCheckRetail, "Клиент");
            ObjectEntity objDoc = addSingleGroupObject(commitSaleCheckArticleRetail, "Чек");

            addPropertyDraw(objClient, LM.barcode, LM.name, propsCustomerCheckRetail, clientSum);
            addPropertyDraw(objDoc, LM.objectValue, LM.date, orderHour, orderMinute, subjectOutOrder, sumWithDiscountObligationOrder);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.barcode, objClient), Compare.EQUALS, addPropertyObject(orderContragentBarcode, objDoc)));

            //addFixedFilter(new NotNullFilterEntity(addPropertyObject(LM.barcode, objClient)));

            addFixedFilter(new OrFilterEntity(
                    new CompareFilterEntity(addPropertyObject(LM.barcode, objClient), Compare.EQUALS, barcodeClient),
                    new CompareFilterEntity(addPropertyObject(LM.name, objClient), Compare.EQUALS, nameClient)));
        }


        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }
    }

    private class BarcodeFormEntity extends FormEntity<VEDBusinessLogics> {

        ObjectEntity objBarcode;

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", LM.baseGroup, true);
            objBarcode.groupTo.initClassView = ClassViewType.PANEL;
            objBarcode.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            objBarcode.resetOnApply = true;

            addPropertyDraw(LM.reverseBarcode);

            addActionsOnObjectChange(objBarcode,
                                     addPropertyObject(
                                             LM.addJProp(true, LM.andNot1,
                                                     LM.addMFAProp(
                                                             null,
                                                             "Ввод нового товара",
                                                             createArticleForm,
                                                             new ObjectEntity[]{createArticleForm.objBarcode},
                                                             true,
                                                             createArticleForm.addPropertyObject(addArticleBarcode, createArticleForm.objBarcode)
                                                     ), 1,
                                                     LM.barcodeToObject, 1
                                             ), objBarcode));

//            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(LM.objectValue, objBarcode));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(LM.reverseBarcode)));
            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;
            design.addIntersection(barcodeView, design.get(getPropertyDraw(LM.barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(barcodeView, design.get(getPropertyDraw(LM.reverseBarcode)), DoNotIntersectSimplexConstraint.TOTHE_BOTTOM);
            design.addIntersection(design.get(getPropertyDraw(LM.reverseBarcode)), design.get(getPropertyDraw(LM.barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            if (getPropertyDraw(documentBarcodePriceOv) != null) {
                design.addIntersection(design.get(getPropertyDraw(LM.barcodeObjectName)), design.get(getPropertyDraw(documentBarcodePriceOv)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.setFont(barcodeView, FONT_LARGE_BOLD);
            design.setFont(LM.reverseBarcode, FONT_SMALL_BOLD);
            design.setFont(barcodeAddClient, FONT_SMALL_BOLD);
            design.setFont(barcodeAddCert, FONT_SMALL_BOLD);
            design.setFont(LM.barcodeObjectName, FONT_LARGE_BOLD);
            design.setFont(documentBarcodePriceOv, FONT_LARGE_BOLD);
            design.setBackground(LM.barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(LM.reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            design.setFocusable(LM.reverseBarcode, false);
            design.setFocusable(false, objBarcode.groupTo);
            barcodeView.clearText = true;
            return design;
        }
    }

    private abstract class DocumentFormEntity extends BarcodeFormEntity {

        public final ObjectEntity objDoc;

        protected boolean toAdd = false;

        protected static final boolean fixFilters = false;
        protected static final boolean noOuters = true;

        protected Object[] getDocumentProps() {
            return new Object[]{LM.baseGroup, true, documentGroup, true};
        }

        protected boolean isDocumentFocusable() {
            return true;
        }

        protected DocumentFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd) {
            super(parent, sID, (toAdd ? documentClass.caption : "Документы"));

            this.toAdd = toAdd;

            objDoc = addSingleGroupObject(documentClass, getDocumentProps());


            if (toAdd) {
                objDoc.groupTo.initClassView = ClassViewType.PANEL;
                objDoc.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
                objDoc.addOnTransaction = true;
            } else {
                addPropertyDraw(orderUserName, objDoc);
                if (!isReadOnly())
                    LM.addObjectActions(this, objDoc);
            }

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction2, objDoc, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(LM.seekBarcodeAction, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(LM.barcodeNotFoundMessage, objBarcode));

            if (hasExternalScreen()) {
                addPropertyDraw(documentBarcodePriceOv, objDoc, objBarcode).setToDraw(objBarcode.groupTo);
                //getPropertyDraw(documentBarcodePriceOv).setToDraw(objBarcode.groupTo);
            }

            addPropertyDraw(objDoc, documentPrintGroup, true);
        }

        protected abstract boolean isSaleForm();

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd && isSaleForm()) {
                design.setFont(FONT_MEDIUM_BOLD, objDoc.groupTo);

                // устанавливаем дизайн
                design.setFont(documentPriceGroup, FONT_HUGE_BOLD, objDoc.groupTo);
                design.setBackground(documentPriceGroup, new Color(240, 240, 240), objDoc.groupTo);

                // ставим Label сверху
                design.setPanelLabelAbove(documentPriceGroup, true, objDoc.groupTo);

                // привязываем функциональные кнопки
                design.setEditKey(nameSubjectIncOrder, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), objDoc.groupTo);
                design.setEditKey(orderSalePayCard, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
                design.setEditKey(orderSalePayCash, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));

                if (!isDocumentFocusable()) {
                    design.setEnabled(false, objDoc.groupTo);
                } else {
                    design.setEnabled(documentPriceGroup, false, objDoc.groupTo);
                    design.setEnabled(orderSalePayCash, true, objDoc.groupTo);
                    design.setEnabled(orderSalePayCard, true, objDoc.groupTo);
                }

                ContainerView docRightContainer = null;

                // так конечно делать неправильно, но DocumentFormEntity - это первый общий класс у продажи сертификатов и кассы
                ContainerView docDiscountContainer = design.getGroupPropertyContainer(objDoc.groupTo, documentDiscountGroup);

                if (docDiscountContainer != null) {
                    // делаем, чтобы суммы были внизу и как можно правее
                    docDiscountContainer.getContainer().setSID(null); // сделано, чтобы при визуальной настройке она сразу не перетаскивала все свойства в другой контейнер при помощи ContainerMover
                    design.mainContainer.addBack(2, docDiscountContainer);
                    if(docRightContainer!=null)
                        design.addIntersection(docRightContainer, docDiscountContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                    docRightContainer = docDiscountContainer;
                }

                ContainerView docObligationContainer = design.getGroupPropertyContainer(objDoc.groupTo, documentObligationGroup);
                if(docObligationContainer != null) {
//                  docObligationContainer.getContainer().setSID(null); // сделано, чтобы при визуальной настройке она сразу не перетаскивала все свойства в другой контейнер при помощи ContainerMover
                    design.mainContainer.addBack(2, docObligationContainer);
                    if(docRightContainer!=null)
                        design.addIntersection(docRightContainer, docObligationContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                    docRightContainer = docObligationContainer;
                }

//                    PropertyDrawEntity payCash = getPropertyDraw(orderSalePayCash);
//
//      PropertyDrawEntity payCard = getPropertyDraw(orderSalePayCard);

                ContainerView payContainer = design.getGroupPropertyContainer(objDoc.groupTo, documentPayGroup);
                if (payContainer != null) {

                    design.mainContainer.addBack(2, payContainer);
                    payContainer.constraints.directions = new SimplexComponentDirections(0.1, -0.1, 0, 0.1);

//                        if (payCash != null) payContainer.add(design.get(payCash));
//                        if (payCard != null) payContainer.add(design.get(payCard));
                    PropertyDrawEntity toDo = getPropertyDraw(orderSaleToDo);
                    if (toDo != null) {
//                            payContainer.add(design.get(toDo));
                        design.get(toDo).design.background = Color.yellow;
                    }
                    PropertyDrawEntity toDoSum = getPropertyDraw(orderSaleToDoSum);
                    if (toDoSum != null) {
//                            payContainer.add(design.get(toDoSum));
                        design.get(toDoSum).design.background = Color.yellow;
                    }

                    if(docRightContainer!=null)
                        design.addIntersection(docRightContainer, payContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                    docRightContainer = payContainer;
                }

                if (hasExternalScreen()) {
                    PropertyDrawEntity barcodeNavigator = getPropertyDraw(LM.barcodeObjectName);
                    if (barcodeNavigator != null) {
                        design.get(barcodeNavigator).externalScreen = VEDBL.panelScreen;
                        design.get(barcodeNavigator).externalScreenConstraints.order = 1;
                    }

                    PropertyDrawEntity orderSaleNavigator = getPropertyDraw(orderSaleToDo);
                    if (orderSaleNavigator != null) {
                        design.get(orderSaleNavigator).externalScreen = VEDBL.panelScreen;
                        design.get(orderSaleNavigator).externalScreenConstraints.order = 3;
                    }

                    PropertyDrawEntity orderSaleSumNavigator = getPropertyDraw(orderSaleToDoSum);
                    if (orderSaleSumNavigator != null) {
                        design.get(orderSaleSumNavigator).externalScreen = VEDBL.panelScreen;
                        design.get(orderSaleSumNavigator).externalScreenConstraints.order = 4;
                    }

                    PropertyDrawEntity priceNavigator = getPropertyDraw(documentBarcodePriceOv);
                    if (priceNavigator != null) {
                        design.get(priceNavigator).externalScreen = VEDBL.panelScreen;
                        design.get(priceNavigator).externalScreenConstraints.order = 2;
                    }
                }
            }

            ContainerView printCont = design.getGroupPropertyContainer(objDoc.groupTo, documentPrintGroup);
            if(printCont!=null) {
                ContainerView pageContainer = design.createContainer();
                design.getMainContainer().addAfter(pageContainer, design.getGroupObjectContainer(objDoc.groupTo));

                Collection<GroupObjectEntity> documentGroups = getDocumentGroups();
                if(documentGroups.size()==1)
                    pageContainer.add(design.getGroupObjectContainer(BaseUtils.single(documentGroups)));
                else {
                    ContainerView groupContainer = design.createContainer("Спецификация");
                    for(GroupObjectEntity group : documentGroups)
                        groupContainer.add(design.getGroupObjectContainer(group));
                    pageContainer.add(groupContainer);
                }

                pageContainer.add(printCont);
                pageContainer.tabbedPane = true;
            }

            PropertyDrawView objectValueView = design.get(getPropertyDraw(LM.objectValue, objDoc));
            if(objectValueView!=null)
                objectValueView.caption = "Код";
            PropertyDrawView objectClassNameView = design.get(getPropertyDraw(LM.objectClassName, objDoc));
            if(objectClassNameView!=null)
                objectClassNameView.caption = "Статус";

            return design;
        }

        protected Collection<GroupObjectEntity> getDocumentGroups() {
            return new ArrayList<GroupObjectEntity>();
        }

        protected boolean hasExternalScreen() {
            return false;
        }
    }

    private abstract class ArticleFormEntity extends DocumentFormEntity {
        public final ObjectEntity objArt;
        public RegularFilterGroupEntity articleFilterGroup;
        public RegularFilterEntity documentFilter;

        protected abstract PropertyObjectEntity getCommitedQuantity();

        @Override
        protected boolean isSaleForm() {
            return false;
        }

        protected Object[] getArticleProps() {
            return new Object[]{LM.baseGroup, true};
        }

        protected Object[] getDocumentArticleProps() {
            return new Object[]{LM.baseGroup, true, documentGroup, true};
        }

        protected ArticleFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, sID, documentClass, toAdd);

            objArt = addSingleGroupObject(article, getArticleProps());

            addPropertyDraw(objDoc, objArt, getDocumentArticleProps());

            articleFilterGroup = new RegularFilterGroupEntity(genID());
            documentFilter = new RegularFilterEntity(genID(),
                    getDocumentArticleFilter(),
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
            articleFilterGroup.addFilter(documentFilter, !toAdd || filled);
            fillExtraFilters(articleFilterGroup, toAdd && !filled);
            addRegularFilterGroup(articleFilterGroup);

//            addHintsNoUpdate(properties, moveGroup);

            PropertyDrawEntity<?> addvOrderArticleDraw = getPropertyDraw(addvOrderArticle);
            if(addvOrderArticleDraw!=null)
                addvOrderArticleDraw.setPropertyHighlight(addPropertyObject(isNegativeAddvOrderArticle, objDoc, objArt));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.get(objArt.groupTo).grid.constraints.fillVertical = 3;

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            PropertyDrawView addvOrderArticleView = design.get(getPropertyDraw(addvOrderArticle));
            if(addvOrderArticleView!=null)
                addvOrderArticleView.highlightColor = new Color(255,0,0);

            return design;
        }

        @Override
        protected Collection<GroupObjectEntity> getDocumentGroups() {
            return Collections.singleton(objArt.groupTo);
        }

        protected ArticleFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd) {
            this(parent, sID, documentClass, toAdd, false);
        }

        protected abstract FilterEntity getDocumentArticleFilter();

        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
        }

        // такое дебильное множественное наследование
        public void fillExtraLogisticsFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(documentLogisticsSupplied, objDoc, objArt)),
                    "Поставляется",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(documentLogisticsRequired, objDoc, objArt)),
                    "Необходимо",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
        }

        protected void addCheckFilter(RegularFilterGroupEntity filterGroup, boolean setDefault) {
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(quantityDiffCommitArticle, objDoc, objArt), Compare.NOT_EQUALS, 0.0),
                    "Отличается от заказа",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)), setDefault);
        }

        protected void fillExtraCheckFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            if(!toAdd)
                addCheckFilter(filterGroup, false);
        }

    }

    // для те которые не различают заказано и выполнено
    private abstract class ArticleNoCheckFormEntity extends ArticleFormEntity {

        protected FilterEntity getDocumentArticleFilter() {
            return new NotNullFilterEntity(getCommitedQuantity());
        }

        protected ArticleNoCheckFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd) {
            super(parent, sID, documentClass, toAdd);
        }

        protected ArticleNoCheckFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, sID, documentClass, toAdd, filled);
        }
    }

    private abstract class InnerFormEntity extends ArticleNoCheckFormEntity {

        public RegularFilterEntity availableFilter;

        protected PropertyObjectEntity getCommitedQuantity() {
            return addPropertyObject(articleInnerQuantity, objDoc, objArt);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            if (!fixFilters) {
                availableFilter = new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(documentFreeQuantity, objDoc, objArt)),
                        "Дост. кол-во",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));

                 filterGroup.addFilter(availableFilter, toAdd);

                 availableFilter = new RegularFilterEntity(genID(),
                       new NotNullFilterEntity(addPropertyObject(articleToDocument, objArt, objDoc)),
                        "Принадлежащие договору",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));

                  filterGroup.addFilter(availableFilter, toAdd);
            }
        }

        protected InnerFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, sID, documentClass, toAdd, filled);

            if (fixFilters)
                addFixedFilter(new OrFilterEntity(getDocumentArticleFilter(), new NotNullFilterEntity(addPropertyObject(documentFreeQuantity, objDoc, objArt))));
        }
    }

    private abstract class OuterFormEntity extends ArticleFormEntity {

        protected PropertyObjectEntity getCommitedQuantity() {
            return getPropertyObject(outerCommitedQuantity);
        }

        protected PropertyObjectEntity getOrderQuantity() {
            return getPropertyObject(outerOrderQuantity);
        }

        protected FilterEntity getDocumentArticleFilter() {
            return new OrFilterEntity(new NotNullFilterEntity(getOrderQuantity()), new NotNullFilterEntity(getCommitedQuantity()));
        }

        protected OuterFormEntity(NavigatorElement parent, String sID, boolean toAdd, int concrete, CustomClass orderClass, CustomClass commitClass) {
            super(parent, sID, concrete==0?orderClass:commitClass, toAdd);

            if(toAdd && concrete!=0)
            {
                addCheckFilter(articleFilterGroup, true);


            }
            //!!!
            addPropertyDraw(articleToDocument, objArt, objDoc);
            addPropertyDraw(objArt, nameCountryArticle, nameUnitOfMeasureArticle);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);

            fillExtraCheckFilters(filterGroup, toAdd);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{LM.baseGroup, true, documentSumGroup, true, documentNDSGroup, true, documentManfrGroup, true, documentRetailGroup, true, importOrder};
        }

        @Override
        protected Object[] getArticleProps() {
            return new Object[]{LM.name, LM.barcode, currentRRP, nameCurrencyArticle};
        }

        @Override
        protected Object[] getDocumentArticleProps() {
            return new Object[]{outerOrderQuantity, outerCommitedQuantity, priceManfrOrderArticle, priceAllOrderDeliveryArticle, addvOrderArticle, ndsOrderArticle, shopPrice, prevPrice, revalBalance, sumOrderArticle, sumNDSOrderArticle, sumNoNDSOrderArticle, sumAddvOrderArticle, ndsShopOrderPriceArticle, sumWithoutNDSRetailOrderArticle, sumNDSRetailOrderArticle, sumRetailOrderArticle, nameArticleGroupArticle, documentLogisticsGroup, true};
        }
    }

    private class DeliveryShopLocalFormEntity extends OuterFormEntity {
        public DeliveryShopLocalFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            this(parent, toAdd, sID, concrete, false);
        }

        public DeliveryShopLocalFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete, boolean impDocs) {
            super(parent, sID, toAdd, concrete, orderDeliveryShopLocal, commitDeliveryShopLocal);

            if(impDocs) {
                addPropertyDraw(downToZero, objDoc);
                addSingleGroupObject(store, LM.name, importDocs);
            }
        }
    }

    private class DeliveryWarehouseLocalFormEntity extends OuterFormEntity {
        public DeliveryWarehouseLocalFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            super(parent, sID, toAdd, concrete, orderDeliveryWarehouseLocal, commitDeliveryWarehouseLocal);

                 }
    }

    private class DeliveryImportFormEntity extends OuterFormEntity {
        public DeliveryImportFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            super(parent, sID, toAdd, concrete, orderDeliveryImport, commitDeliveryImport);
        }
    }

    private class ArticleOuterFormEntity extends InnerFormEntity {
        ObjectEntity objOuter;

        protected ArticleOuterFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, CustomClass commitClass, boolean filled, boolean showOuters) {
            super(parent, sID, documentClass, toAdd, filled);
            if (showOuters || !noOuters) {
                objOuter = addSingleGroupObject(commitClass, LM.baseGroup, true);
                addPropertyDraw(objOuter, objDoc, LM.baseGroup, true, documentGroup, true);
                addPropertyDraw(objOuter, objDoc, objArt, LM.baseGroup, true, documentGroup, true);
                addPropertyDraw(objOuter, objArt, LM.baseGroup, true, priceAllOrderDeliveryArticle);

                NotNullFilterEntity documentFilter = new NotNullFilterEntity(getPropertyObject(innerQuantity));
                NotNullFilterEntity documentFreeFilter = new NotNullFilterEntity(addPropertyObject(documentInnerFreeQuantity, objDoc, objArt, objOuter));
                if (fixFilters)
                    addFixedFilter(new OrFilterEntity(documentFilter, documentFreeFilter));
                RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        documentFilter,
                        "Документ",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd || filled);
                if (!fixFilters)
                {
                    filterGroup.addFilter(new RegularFilterEntity(genID(),
                            documentFreeFilter,
                            "Дост. кол-во",
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), toAdd && !filled);


                }
                addRegularFilterGroup(filterGroup);


            }
        }

        @Override
        protected Collection<GroupObjectEntity> getDocumentGroups() {
            Collection<GroupObjectEntity> documentGroups = super.getDocumentGroups();
            if(objOuter != null)
                documentGroups = BaseUtils.add(documentGroups, objOuter.groupTo);
            return documentGroups;
        }
    }

    private class ReturnDeliveryLocalFormEntity extends ArticleOuterFormEntity {

        @Override
        protected Object[] getDocumentArticleProps() {
            return new Object[]{articleInnerQuantity, priceManfrOrderArticle, priceOrderArticle, ndsOrderArticle, sumWithDiscountOrderArticle, sumNDSOrderArticle, sumNoNDSOrderArticle};
        }

        public ReturnDeliveryLocalFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            super(parent, sID, concrete==0?orderReturnDeliveryLocal:commitReturnDeliveryLocal, toAdd, commitDeliveryLocal, false, true);
        }

    }

    private class ArticleInnerFormEntity extends ArticleOuterFormEntity {

        protected ArticleInnerFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, sID, documentClass, toAdd, commitDelivery, filled, false);
        }
    }

    private class DocumentInnerFormEntity extends ArticleInnerFormEntity {

        protected DocumentInnerFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, sID, toAdd, documentClass, filled);
        }
    }

    private class SaleWholeFormEntity extends DocumentInnerFormEntity {

        @Override
        protected boolean isSaleForm() {
            return true;
        }

        public SaleWholeFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, orderSaleWhole, false);
        }
    }

    public class ShopArticleFormEntity extends FormEntity {

        private ObjectEntity objShop;
        private ObjectEntity objArticle;
        private ObjectEntity nameArticle;

        public ShopArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            nameArticle = addSingleGroupObject(StringClass.get(100), "Поиск по наименованию", LM.objectValue);
            nameArticle.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticle = addSingleGroupObject(article, LM.barcode, nameArticleGroupArticle, nameBrendArticle, LM.name, nameArticleSupplier, balanceFreeQuantity);
            objShop = addSingleGroupObject(shop, LM.name, addressSubject, nameShopFormat);

            addPropertyDraw(articleFreeQuantity, objShop, objArticle);
            addPropertyDraw(currentShopPrice, objShop, objArticle);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.name, objArticle), Compare.START_WITH, nameArticle));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(balanceFreeQuantity, objArticle)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleFreeQuantity, objShop, objArticle)));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(getPropertyDraw(LM.objectValue, nameArticle)).caption = "Введите первые буквы наименования";
            return design;
        }
    }


    public class ShopMoneyFormEntity extends FormEntity {
        public ShopMoneyFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objShop = addSingleGroupObject(shop, LM.baseGroup);

            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            ObjectEntity objDateFrom = new ObjectEntity(genID(), DateClass.instance, "Выгрузить от");
            ObjectEntity objDateTo = new ObjectEntity(genID(), DateClass.instance, "Выгрузить до");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);
            addGroup(gobjDates);

            gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjDates.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objDateFrom, LM.objectValue);
            addPropertyDraw(objDateTo, LM.objectValue);
            addPropertyDraw(saleExport, objShop, objDateFrom, objDateTo);
        }
    }

    private abstract class SaleRetailFormEntity extends DocumentInnerFormEntity {

        @Override
        protected boolean isSaleForm() {
            return true;
        }

        protected abstract boolean isSubjectImpl();

        @Override
        protected Object[] getDocumentProps() {
            return BaseUtils.add(isSubjectImpl()?new Object[]{nameImplSubjectIncOrder, propsCustomerImplIncOrder}: new Object[]{nameSubjectIncOrder, propsCustomerIncOrder},
                    new Object[]{orderClientSum, sumWithDiscountOrder, discountSumOrder, orderSalePayObligation, sumWithDiscountObligationOrder, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum, orderBirthDay, orderNoDiscount, payWithCard, printOrderCheck});
        }

        @Override
        protected Object[] getArticleProps() {
            return new Object[]{};
        }

        @Override
        protected Object[] getDocumentArticleProps() {
            return new Object[]{};
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleRetailFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean allStores) {
            super(parent, sID, toAdd, documentClass, true);

            if (!toAdd)
                addPropertyDraw(objDoc, LM.date, LM.objectValue, orderHour, orderMinute);

            if (allStores) {
                addPropertyDraw(objDoc, subjectOutOrder, nameSubjectOutOrder);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectOutOrder, objDoc), Compare.EQUALS, shopImplement));
            }

            // чтобы в порядке нужном
            addPropertyDraw(changeQuantityOrder, objDoc, objArt);
            addPropertyDraw(LM.barcode, objArt);

            objArt.groupTo.filterProperty = addPropertyDraw(LM.name, objArt);

            addPropertyDraw(articleQuantity, objDoc, objArt); // для timeChanges иначе можно было бы articleQuantity
            addPropertyDraw(documentFreeQuantity, objDoc, objArt);
            addPropertyDraw(priceAllOrderSaleArticle, objDoc, objArt);
            addPropertyDraw(discountOrderArticle, objDoc, objArt);
            addPropertyDraw(discountSumOrderArticle, objDoc, objArt);
            addPropertyDraw(sumWithDiscountOrderArticle, objDoc, objArt);
            //addPropertyDraw(documentBarcodePriceOv, objDoc, objBarcode);
            if (!toAdd) {
                addPropertyDraw(sumWithDiscountObligationOrderArticle, objDoc, objArt);
            }
            //getPropertyDraw(documentBarcodePriceOv).setToDraw(objBarcode.groupTo);

            objArt.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            addFixedOrder(addPropertyObject(changeQuantityTime, objDoc, objArt), false);

            if(isSubjectImpl())
                addPropertyDraw(barcodeAddClient);
            addActionsOnObjectChange(objBarcode, true,
                                     addPropertyObject(barcodeAddClientAction, objBarcode),
                                     addPropertyObject(barcodeAction2, objDoc, objBarcode),
                                     addPropertyObject(LM.seekBarcodeAction, objBarcode),
                                     addPropertyObject(LM.barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.setEnabled(orderClientSum, false);

            design.get(objDoc.groupTo).grid.constraints.fillVertical = 2;

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);

            design.setEnabled(LM.publicGroup, false, objArt.groupTo);
            design.setEnabled(articleQuantity, true, objArt.groupTo);

            design.get(objArt.groupTo).grid.defaultComponent = true;

            ObjectView objArtView = design.get(objArt);
            objArtView.classChooser.show = false;

            if(isSubjectImpl()) {
                design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddClient)));
                design.addIntersection(design.get(getPropertyDraw(barcodeAddClient)), design.get(getPropertyDraw(LM.barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }
            design.setEditKey(barcodeAddClient, KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK));

            design.get(getPropertyDraw(changeQuantityOrder, objArt)).minimumSize = new Dimension(20, 20);
            design.get(getPropertyDraw(articleQuantity, objArt)).minimumSize = new Dimension(20, 20);
            design.get(getPropertyDraw(documentFreeQuantity, objArt)).minimumSize = new Dimension(90, 20);
            design.get(getPropertyDraw(discountOrderArticle, objArt)).minimumSize = new Dimension(20, 20);
            getPropertyDraw(discountSumOrderArticle, objArt).forceViewType = ClassViewType.HIDE;
            design.get(getPropertyDraw(LM.barcode, objArt)).minimumSize = new Dimension(200, 100);

            return design;
        }
    }

    public class CommitSaleCheckRetailFormEntity extends SaleRetailFormEntity {

        public ObjectEntity objObligation;
        private ObjectEntity objCoupon;
        private ObjectEntity objIssue;

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        @Override
        protected boolean isSubjectImpl() {
            return true;
        }

        private CommitSaleCheckRetailFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, commitSaleCheckArticleRetail, toAdd, allStores);

            objDoc.caption = "Чек";

            objObligation = addSingleGroupObject(obligation, "Оплачено купонами/ сертификатами");
            addPropertyDraw(LM.barcode, objObligation);
            addPropertyDraw(LM.objectClassName, objObligation);
            addPropertyDraw(obligationSum, objObligation);
            addPropertyDraw(obligationSumFrom, objObligation);
            addPropertyDraw(couponFromIssued, objObligation).forceViewType = ClassViewType.GRID;
            addPropertyDraw(obligationIssued,objObligation);
            addPropertyDraw(obligationToIssued, objObligation);

            addPropertyDraw(orderSaleUseObligation, objDoc, objObligation);

            if (toAdd) {
                objArt.groupTo.propertyHighlight = addPropertyObject(actionOutArticle, objDoc, objArt);

                objObligation.groupTo.propertyHighlight = addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation);
                addHintsNoUpdate(obligationDocument);
            }

            objObligation.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleUseObligation, objDoc, objObligation)));

            objCoupon = addSingleGroupObject(coupon, "Выдано купонов", LM.baseGroup, true);
            addPropertyDraw(objDoc, objCoupon, LM.baseGroup, true, issueObligation);
            objCoupon.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(issueObligation, objDoc, objCoupon)));

            if (toAdd) {
                objIssue = addSingleGroupObject(DoubleClass.instance, "Выдать купоны", LM.baseGroup, true);
                addPropertyDraw(couponToIssueQuantity, objDoc, objIssue);
                objIssue.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(couponToIssueConstraint, objDoc, objIssue)));

                addPropertyDraw(cashRegOperGroup, true);
            } else {
                addPropertyDraw(checkRetailExported, objDoc);
            }
            addPropertyDraw(orderUserName, objDoc);
            getPropertyDraw(orderUserName).forceViewType = ClassViewType.HIDE;
            addPropertyDraw(accumulatedClientSum, objDoc);
            getPropertyDraw(accumulatedClientSum).forceViewType = ClassViewType.HIDE;
        }

        @Override
        protected Font getDefaultFont() {
            if (toAdd)
                return FONT_MEDIUM_BOLD;
            else
                return super.getDefaultFont();
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.get(objArt.groupTo).grid.minRowCount = 6;

            if (toAdd) {
                design.get(objIssue.groupTo).grid.constraints.fillHorizontal /= 3;
                design.get(objIssue.groupTo).grid.minRowCount = 2;
                design.get(objIssue.groupTo).grid.hideToolbarItems();
                design.get(objIssue.groupTo).grid.autoHide = true;
                design.addIntersection(design.getGroupObjectContainer(objIssue.groupTo), design.getGroupObjectContainer(objCoupon.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objIssue.groupTo), design.getGroupObjectContainer(objObligation.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

                design.getGroupPropertyContainer((GroupObjectView) null, cashRegOperGroup).add(0, design.get(getPropertyDraw(payWithCard)));
                design.getGroupPropertyContainer((GroupObjectView) null, cashRegOperGroup).add(1, design.get(getPropertyDraw(printOrderCheck)));

                design.get(getPropertyDraw(printOrderCheck)).constraints.insetsSibling.right = 100;

                design.get(objObligation.groupTo).highlightColor = new Color(255, 0, 0);
            }

            design.get(objCoupon.groupTo).grid.minRowCount = 2;
            design.get(objObligation.groupTo).grid.minRowCount = 2;
            design.get(objCoupon.groupTo).grid.hideToolbarItems();
            design.get(objObligation.groupTo).grid.hideToolbarItems();
            design.get(objCoupon.groupTo).grid.autoHide = true;
            design.addIntersection(design.getGroupObjectContainer(objCoupon.groupTo), design.getGroupObjectContainer(objObligation.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            if (toAdd) {
                design.setHeaderFont(FONT_MEDIUM_PLAIN);
                design.setFont(FONT_LARGE_BOLD, objArt.groupTo);
            }

            ObjectView objCouponView = design.get(objCoupon);
            objCouponView.classChooser.show = false;

            ObjectView objObligationView = design.get(objObligation);
            objObligationView.classChooser.show = false;

            design.get(getPropertyDraw(LM.objectClassName, objObligation)).maximumSize = new Dimension(50, 50);

            if (documentFilter != null) {
                RegularFilterView docFilterView = design.get(articleFilterGroup).get(documentFilter);
                RegularFilterView availableFilterView = design.get(articleFilterGroup).get(availableFilter);

                //todo: проставить нужные порядки
//                docFilterView.orders.put(design.getProperty(getPropertyDraw(articleQuantity)), true);
//                availableFilterView.orders.put(design.getProperty(getPropertyDraw(documentFreeQuantity)), true);
//                design.get(articleFilterGroup).nullOrders.put(design.getProperty(getPropertyDraw(documentFreeQuantity)), false);
            }

            return design;
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        public ClientResultAction getClientApply(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);
                ObjectInstance obligation = formInstance.instanceFactory.getInstance(objObligation);

                return VEDBL.cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSet(art.groupTo),
                        getPropertyDraw(priceAllOrderSaleArticle, objArt), getPropertyDraw(articleQuantity, objArt),
                        getPropertyDraw(LM.name, objArt), getPropertyDraw(sumWithDiscountOrderArticle, objArt),
                        getPropertyDraw(sumWithDiscountObligationOrder, objDoc), getPropertyDraw(LM.barcode, objArt),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc),
                        getPropertyDraw(discountOrderArticle), getPropertyDraw(discountSumOrderArticle), getPropertyDraw(orderUserName),
                        getPropertyDraw(nameImplSubjectIncOrder), getPropertyDraw(accumulatedClientSum), getPropertyDraw(discountSumOrder, objDoc),
                        BaseUtils.toSet(obligation.groupTo),getPropertyDraw(LM.objectClassName, objObligation), getPropertyDraw(obligationSum, objObligation), getPropertyDraw(LM.barcode, objObligation));
            } else
                return super.getClientApply(formInstance);
        }

        public ClientAction getPrintOrderAction(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);

                return VEDBL.cashRegController.getPrintOrderAction(formInstance,
                        BaseUtils.toSet(art.groupTo),
                        getPropertyDraw(priceAllOrderSaleArticle, objArt), getPropertyDraw(articleQuantity, objArt),
                        getPropertyDraw(LM.name, objArt), getPropertyDraw(sumWithDiscountOrderArticle, objArt),
                        getPropertyDraw(sumWithDiscountObligationOrder, objDoc), getPropertyDraw(LM.barcode, objArt));
            } else
                return new ListClientAction(new ArrayList<ClientAction>());
        }

        @Override
        public String checkClientApply(Object result) {

            String check = VEDBL.cashRegController.checkCashRegApplyActions(result);
            if (check != null) return check;

            return super.checkClientApply(result);
        }

        @Override
        protected boolean hasExternalScreen() {
            return true;
        }
    }

    public class CommitSaleCheckRetailExcelFormEntity extends FormEntity {

        public CommitSaleCheckRetailExcelFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            // форма по проданным товарам (цена, стоимость, скидки, данные по продаже)
            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            ObjectEntity dateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
            ObjectEntity dateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
            gobjDates.add(dateFrom);
            gobjDates.add(dateTo);

            addGroup(gobjDates);
            gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjDates.initClassView = ClassViewType.PANEL;

            addPropertyDraw(dateFrom, LM.objectValue);
            addPropertyDraw(dateTo, LM.objectValue);

            GroupObjectEntity gobjDocArt = new GroupObjectEntity(genID());

            ObjectEntity objDoc = new ObjectEntity(genID(), commitSaleCheckArticleRetail, "Чек");
            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");

            gobjDocArt.add(objDoc);
            gobjDocArt.add(objArt);
            addGroup(gobjDocArt);

            addPropertyDraw(objArt, LM.barcode, LM.name, nameBrendArticle);
            addPropertyDraw(objDoc, objArt, priceAllOrderSaleArticle, articleQuantity);
            addPropertyDraw(objDoc, LM.objectValue, LM.date, orderHour, orderMinute, orderUserBarcode, orderComputerName, subjectOutOrder, orderContragentBarcode);
            addPropertyDraw(objDoc, objArt, discountSumOrderArticle, discountOrderArticle, sumOrderArticle, sumWithDiscountOrderArticle, sumWithDiscountCouponOrderArticle, sumWithDiscountObligationOrderArticle);


            removePropertyDraw(documentMoveGroup); // нужно, чтобы убрать Доступ. кол-во, которое не может нормально выполнить PostgreSQL

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleQuantity, objDoc, objArt)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objDoc), Compare.GREATER_EQUALS, dateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objDoc), Compare.LESS_EQUALS, dateTo));
        }


        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.overridePageWidth = 3000;
            return design;
        }
    }

    private class OrderSaleInvoiceRetailFormEntity extends SaleRetailFormEntity {

        private OrderSaleInvoiceRetailFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, orderSaleInvoiceArticleRetail, toAdd, allStores);

            addPropertyDraw(objDoc, LM.objectClassName);
        }

        @Override
        protected boolean isSubjectImpl() {
            return false;
        }

    }

    private class DistributeFormEntity extends DocumentInnerFormEntity {
        public DistributeFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass) {
            super(parent, sID, toAdd, documentClass, false);

            addPropertyDraw(objDoc, objArt, quantityCheckCommitInnerArticle);

            if (!toAdd)
                addActionsOnObjectChange(objBarcode, true,
                                         addPropertyObject(barcodeActionCheck, objDoc, objBarcode),
                                         addPropertyObject(LM.seekBarcodeAction, objBarcode),
                                         addPropertyObject(LM.barcodeNotFoundMessage, objBarcode));
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);

            fillExtraCheckFilters(filterGroup, toAdd);
        }

    }

    private class DistributeShopFormEntity extends DistributeFormEntity {
        public DistributeShopFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, orderDistributeShop);
        }
    }

    private class BalanceCheckFormEntity extends DocumentInnerFormEntity {
        public BalanceCheckFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, balanceCheck, false);
        }
    }

    private abstract class ReturnFormEntity extends DocumentFormEntity {

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{discountSumOrder, sumWithDiscountOrder};
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        public final ObjectEntity objInner;
        public final ObjectEntity objArt;
        ObjectEntity objOuter;

        protected String getReturnCaption() {
            return "Документ к возврату";
        }

        protected ReturnFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, CustomClass commitClass) {
            super(parent, sID, documentClass, toAdd);

            objInner = addSingleGroupObject(commitClass, getReturnCaption(), LM.baseGroup, true);

            addPropertyDraw(objInner, objDoc, LM.baseGroup, true, documentGroup, true);

            NotNullFilterEntity documentFilter = new NotNullFilterEntity(getPropertyObject(sumReturnedQuantity));
            NotNullFilterEntity documentFreeFilter = new NotNullFilterEntity(getPropertyObject(sumReturnedQuantityFree));
            if (fixFilters)
                addFixedFilter(new OrFilterEntity(documentFilter, documentFreeFilter));
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    documentFilter,
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)), !toAdd);
            if (!fixFilters)
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        documentFreeFilter,
                        "Дост. кол-во",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK)), toAdd);
            addRegularFilterGroup(filterGroup);

            objArt = addSingleGroupObject(article);

            addPropertyDraw(changeQuantityOrder, objInner, objArt);
            addPropertyDraw(LM.barcode, objArt);
            addPropertyDraw(LM.name, objArt);
            addPropertyDraw(objInner, objDoc, objArt, LM.baseGroup, true, documentGroup, true);
            addPropertyDraw(priceAllOrderSaleArticle, objInner, objArt);
            addPropertyDraw(discountOrderArticle, objInner, objArt);
            addPropertyDraw(sumWithDiscountOrderArticle, objInner, objArt);

            NotNullFilterEntity articleFilter = new NotNullFilterEntity(getPropertyObject(returnInnerQuantity));
            NotNullFilterEntity articleFreeFilter = new NotNullFilterEntity(getPropertyObject(returnFreeQuantity));
            if (fixFilters)
                addFixedFilter(new OrFilterEntity(articleFilter, articleFreeFilter));
            RegularFilterGroupEntity articleFilterGroup = new RegularFilterGroupEntity(genID());
            articleFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    articleFilter,
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd);
            if (!fixFilters)
                articleFilterGroup.addFilter(new RegularFilterEntity(genID(),
                        articleFreeFilter,
                        "Дост. кол-во",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), toAdd);

            addRegularFilterGroup(articleFilterGroup);

//            addHintsNoUpdate(properties, moveGroup);
            addFixedOrder(addPropertyObject(changeQuantityTime, objInner, objArt), false);

            if (!noOuters) {
                objOuter = addSingleGroupObject(commitDelivery, LM.baseGroup, true);

                addPropertyDraw(objInner, objOuter, objDoc, LM.baseGroup, true, documentGroup, true);
                addPropertyDraw(objInner, objOuter, objDoc, objArt, LM.baseGroup, true, documentGroup, true);
                addPropertyDraw(objInner, objOuter, LM.baseGroup, true);
                addPropertyDraw(objInner, objOuter, objArt, LM.baseGroup, true);

                NotNullFilterEntity documentCommitFilter = new NotNullFilterEntity(getPropertyObject(returnInnerCommitQuantity));
                NotNullFilterEntity documentCommitFreeFilter = new NotNullFilterEntity(getPropertyObject(returnInnerFreeQuantity));
                if (fixFilters)
                    addFixedFilter(new OrFilterEntity(documentCommitFilter, documentCommitFreeFilter));
                RegularFilterGroupEntity filterOutGroup = new RegularFilterGroupEntity(genID());
                filterOutGroup.addFilter(new RegularFilterEntity(genID(),
                        documentCommitFilter,
                        "Документ",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !toAdd);
                if (!fixFilters)
                    filterOutGroup.addFilter(new RegularFilterEntity(genID(),
                            documentCommitFreeFilter,
                            "Макс. кол-во",
                            KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)), toAdd);
                addRegularFilterGroup(filterOutGroup);
            }

            addActionsOnObjectChange(objBarcode, true,
                                     addPropertyObject(barcodeAction3, objDoc, objInner, objBarcode),
                                     addPropertyObject(LM.seekBarcodeAction, objBarcode),
                                     addPropertyObject(LM.barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {
                // делаем, чтобы суммы были внизу и как можно правее
                design.mainContainer.addBack(2, design.get(getPropertyDraw(sumWithDiscountOrder)).getContainer());
            }

            if (!noOuters)
                design.addIntersection(design.getGroupObjectContainer(objInner.groupTo), design.getGroupObjectContainer(objOuter.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.setEnabled(objArt, false);
            design.setEnabled(returnInnerQuantity, true, objArt.groupTo);
            design.get(objArt.groupTo).grid.defaultComponent = true;


            return design;
        }

        @Override
        protected Collection<GroupObjectEntity> getDocumentGroups() {
            return BaseUtils.toList(objInner.groupTo, objArt.groupTo);
        }
    }

    private class DistributeWarehouseFormEntity extends ReturnFormEntity {

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{LM.baseGroup, true, documentGroup, true, sumWithDiscountOrder};
        }

        protected boolean isSaleForm() {
            return false;
        }

        public DistributeWarehouseFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, orderDistributeWarehouse, commitDistributeShop);
        }
    }

    private class ReturnSaleFormEntity extends ReturnFormEntity {

        @Override
        protected boolean isSaleForm() {
            return true;
        }

        private ReturnSaleFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, CustomClass commitClass) {
            super(parent, sID, toAdd, documentClass, commitClass);
        }
    }

    private class ReturnSaleWholeFormEntity extends ReturnSaleFormEntity {
        private ReturnSaleWholeFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, returnSaleWhole, commitSaleWhole);
        }

    }

    public class ReturnSaleRetailFormEntity extends ReturnSaleFormEntity {

        public ReturnSaleRetailFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, CustomClass commitClass, boolean allStores) {
            super(parent, sID, toAdd, documentClass, commitClass);

            if (allStores) {
                addPropertyDraw(objDoc, subjectIncOrder, nameSubjectIncOrder);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectIncOrder, objDoc), Compare.EQUALS, shopImplement));
            }
        }
    }

    private class ReturnSaleInvoiceRetailFormEntity extends ReturnSaleRetailFormEntity {
        private ReturnSaleInvoiceRetailFormEntity(NavigatorElement parent, boolean toAdd, String sID, boolean allStores) {
            super(parent, sID, toAdd, returnSaleInvoiceRetail, commitSaleInvoiceArticleRetail, allStores);

            setReadOnly(true, objInner.groupTo);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);

            return design;
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{discountSumOrder, sumWithDiscountOrder, nameSubjectOutOrder};
        }
    }

    public class ReturnSaleCheckRetailFormEntity extends ReturnSaleRetailFormEntity {

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        @Override
        protected String getReturnCaption() {
            return "Номер чека";
        }

        private ReturnSaleCheckRetailFormEntity(NavigatorElement parent, boolean toAdd, String sID, boolean allStores) {
            super(parent, sID, toAdd, returnSaleCheckRetail, commitSaleCheckArticleRetail, allStores);

            objDoc.caption = "Возвратный чек";

            if (toAdd) {
                addPropertyDraw(cashRegOperGroup, true);
            } else {
                addPropertyDraw(LM.date, objDoc);
                addPropertyDraw(checkRetailExported, objDoc);
            }

            addPropertyDraw(documentBarcodePriceOv, objInner, objBarcode).setToDraw(objBarcode.groupTo);
            addPropertyDraw(orderUserName, objDoc);
            getPropertyDraw(orderUserName).forceViewType = ClassViewType.HIDE;
            //addPropertyDraw(returnArticleSalePay, objArt);

            setReadOnly(true, objInner.groupTo);

            PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectOutOrder, objInner), Compare.EQUALS, shopImplement));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objInner.groupTo).title = "Список чеков";
            design.get(getPropertyDraw(LM.objectValue, objInner)).caption = "Номер чека";
            design.getGroupObjectContainer(objArt.groupTo).title = "Товарные позиции";

            PropertyDrawEntity barcodeNavigator = getPropertyDraw(LM.barcodeObjectName);
            if (barcodeNavigator != null) {
                design.get(barcodeNavigator).externalScreen = VEDBL.panelScreen;
                design.get(barcodeNavigator).externalScreenConstraints.order = 1;
            }

            PropertyDrawEntity priceNavigator = getPropertyDraw(documentBarcodePriceOv);
            if (priceNavigator != null) {
                design.get(priceNavigator).externalScreen = VEDBL.panelScreen;
                design.get(priceNavigator).externalScreenConstraints.order = 2;
            }

            PropertyDrawEntity returnSaleSumNavigator = getPropertyDraw(sumWithDiscountOrder);
            if (returnSaleSumNavigator != null) {
                design.get(returnSaleSumNavigator).externalScreen = VEDBL.panelScreen;
                design.get(returnSaleSumNavigator).externalScreenConstraints.order = 4;
            }

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        public ClientResultAction getClientApply(FormInstance formInstance) {
            if (toAdd) {

                ObjectInstance doc = formInstance.instanceFactory.getInstance(objDoc);
                ObjectInstance inner = formInstance.instanceFactory.getInstance(objInner);
                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);

                return VEDBL.cashRegController.getCashRegApplyActions(formInstance, 2,
                        BaseUtils.toSet(inner.groupTo, art.groupTo),
                        getPropertyDraw(priceAllOrderSaleArticle, objArt), getPropertyDraw(returnInnerQuantity, objArt),
                        getPropertyDraw(LM.name, objArt), getPropertyDraw(sumWithDiscountOrderArticle, objArt),
                        getPropertyDraw(sumWithDiscountOrder, objDoc), getPropertyDraw(LM.barcode, objArt), null, null,
                        getPropertyDraw(discountOrderArticle), getPropertyDraw(discountSumOrderArticle),
                        getPropertyDraw(orderUserName), null, null, null, null, null, null, null);
            } else
                return super.getClientApply(formInstance);
        }

        @Override
        public String checkClientApply(Object result) {

            String check = VEDBL.cashRegController.checkCashRegApplyActions(result);
            if (check != null) return check;

            return super.checkClientApply(result);
        }
    }

    private class SpecificationSupplierFormEntity extends FormEntity {
        protected SpecificationSupplierFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Спецификации");

            ObjectEntity objContract = addSingleGroupObject(contract, LM.name, nameLegalOutContract, nameLegalIncContract, LM.date);
            LM.addObjectActions(this, objContract);

            ObjectEntity objSpecification = addSingleGroupObject(specification, LM.name, nameContractSpecification, specificationDateFrom, specificationDateTo);
            LM.addObjectActions(this, objSpecification);

            ObjectEntity objArt = addSingleGroupObject(article, LM.baseGroup, true);
            addPropertyDraw(articleToSpecification, objArt, objSpecification);
            addPropertyDraw(objArt, priceGroup);
            addPropertyDraw(objArt, logisticsGroup);


            RegularFilterGroupEntity filterBalanceGroup = new RegularFilterGroupEntity(genID());
            filterBalanceGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(articleToSpecification, objArt, objSpecification)),
                    "Принадлежащие спецификации",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterBalanceGroup);



            addFixedFilter(new CompareFilterEntity(addPropertyObject(contractSpecification, objSpecification), Compare.EQUALS, objContract));
        }
    }


    private class SupplierArticleFormEntity extends FormEntity {
        protected SupplierArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Ассортимент поставщиков");

            ObjectEntity objSupplier = addSingleGroupObject(supplier, LM.publicGroup, true);
            LM.addObjectActions(this, objSupplier);

            ObjectEntity objContractSupplier = addSingleGroupObject(contractSupplier, LM.name, LM.date, nameLegalIncContract);
            LM.addObjectActions(this, objContractSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(legalOutContract, objContractSupplier), Compare.EQUALS, objSupplier));

            ObjectEntity objArt = addSingleGroupObject(article, LM.baseGroup, true, priceGroup, logisticsGroup);
            LM.addObjectActions(this, objArt);

            addPropertyDraw(objSupplier, objArt, LM.publicGroup, true);


            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSupplier, objArt), Compare.EQUALS, objSupplier));
        }
    }

    private class ContragentWholeArticleFormEntity extends FormEntity {
            ObjectEntity objContragent, objContract, objSpecification, objArt;
        protected ContragentWholeArticleFormEntity(NavigatorElement parent, String sID, boolean issupplier) {
            super(parent, sID, (issupplier?"Ассортимент поставщиков":"Договоры с покупателями"));

            if (issupplier) {
                objContragent = addSingleGroupObject(supplier);
                objContract = addSingleGroupObject(contractSupplier, LM.name, LM.date, nameLegalOutContract);
            } else {
               objContragent = addSingleGroupObject(customerWhole);
               objContract = addSingleGroupObject(contractCustomer, LM.name, LM.date, nameLegalIncContract);
            }
            addPropertyDraw(objContragent, LM.publicGroup, true);
            LM.addObjectActions(this, objContragent);
            LM.addObjectActions(this, objContract);
            addFixedFilter(new OrFilterEntity(new CompareFilterEntity(addPropertyObject(legalOutContract, objContract), Compare.EQUALS, objContragent),
                                                  new CompareFilterEntity(addPropertyObject(legalIncContract, objContract), Compare.EQUALS, objContragent)
                        ));

            objSpecification = addSingleGroupObject(specification, LM.name, specificationDateFrom, specificationDateTo);
            LM.addObjectActions(this, objSpecification);
            objArt = addSingleGroupObject(article, LM.baseGroup, true);
            addPropertyDraw(articleToSpecification, objArt, objSpecification);
            addPropertyDraw(objContragent, objArt, LM.publicGroup, true);
            addPropertyDraw(objArt, priceGroup);
            addPropertyDraw(objArt, logisticsGroup);
            LM.addObjectActions(this, objArt);

            if(issupplier) {
            RegularFilterGroupEntity filterBalanceGroup2 = new RegularFilterGroupEntity(genID());
                filterBalanceGroup2.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(articleSupplier, objArt), Compare.EQUALS, objContragent),
                    "Принадлежащие контрагенту",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterBalanceGroup2);
            }

            addFixedFilter(new CompareFilterEntity(addPropertyObject(contractSpecification, objSpecification), Compare.EQUALS, objContract));


            RegularFilterGroupEntity filterBalanceGroup = new RegularFilterGroupEntity(genID());
            filterBalanceGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(articleToSpecification, objArt, objSpecification)),
                    "Принадлежащие спецификации",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterBalanceGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objContract.groupTo),
                                   design.getGroupObjectContainer(objSpecification.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            return design;
        }
    }



    private class StoreLegalEntityFormEntity extends FormEntity {
        protected StoreLegalEntityFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Юридические лица");

            ObjectEntity objSupplier = addSingleGroupObject(storeLegalEntity, LM.publicGroup, true);
            LM.addObjectActions(this, objSupplier);

            ObjectEntity objContractSale = addSingleGroupObject(contractSale, LM.name, LM.date, nameLegalIncContract);
            LM.addObjectActions(this, objContractSale);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(legalOutContract, objContractSale), Compare.EQUALS, objSupplier));
        }
    }

    private class CustomerWholeFormEntity extends FormEntity {
        protected CustomerWholeFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Оптовые покупатели");

            ObjectEntity objCustomerWhole = addSingleGroupObject(customerWhole, LM.publicGroup, true);
            LM.addObjectActions(this, objCustomerWhole);

            ObjectEntity objContractSale = addSingleGroupObject(contractCustomer, LM.name, LM.date, nameLegalOutContract);
            LM.addObjectActions(this, objContractSale);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(legalIncContract, objContractSale), Compare.EQUALS, objCustomerWhole));
        }
    }

    private class StoreArticleFormEntity extends BarcodeFormEntity {
        private ObjectEntity objArt;

        protected StoreArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Остатки по складу");

            ObjectEntity objStore = addSingleGroupObject(store, LM.publicGroup, true);
            objArt = addSingleGroupObject(article, LM.publicGroup, true);
            ObjectEntity objOuter = addSingleGroupObject(commitDelivery, LM.publicGroup, true);

            addPropertyDraw(objStore, objArt, LM.publicGroup, true);
            addPropertyDraw(objStore, objOuter, LM.publicGroup, true);
            addPropertyDraw(objOuter, objArt, LM.baseGroup, true);
            addPropertyDraw(objStore, objOuter, objArt, LM.publicGroup, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView form = super.createDefaultRichDesign();
            form.get(objArt.groupTo).grid.constraints.fillVertical = 3;
            return form;
        }
    }

    private class ArticleFormatFormEntity extends BarcodeFormEntity {
        protected ArticleFormatFormEntity(NavigatorElement parent, String sID, boolean splitGroup) {
            super(parent, sID, "Определение ассортимента" + (splitGroup?"":" (в таблице)"));

            ObjectEntity objFormat, objArt;
            if (splitGroup) {
                objArt = addSingleGroupObject(article);
                objFormat = addSingleGroupObject(format);
            } else {
                GroupObjectEntity gobjFormatArt = new GroupObjectEntity(genID());
                objFormat = new ObjectEntity(genID(), format, "Формат");
                objArt = new ObjectEntity(genID(), article, "Товар");
                gobjFormatArt.add(objFormat);
                gobjFormatArt.add(objArt);
                addGroup(gobjFormatArt);
            }

            LM.addObjectActions(this, objArt);
            addPropertyDraw(objFormat, LM.publicGroup, true);
            addPropertyDraw(objArt, LM.baseGroup, true, currentRRP);
            addPropertyDraw(objFormat, objArt, LM.publicGroup, true);

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction2, objFormat, objBarcode));

            RegularFilterGroupEntity filterBalanceGroup = new RegularFilterGroupEntity(genID());
            filterBalanceGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(balanceFormatFreeQuantity, objFormat, objArt)),
                    "Есть своб. кол.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
            addRegularFilterGroup(filterBalanceGroup);

            RegularFilterGroupEntity filterToSellGroup = new RegularFilterGroupEntity(genID());
            filterToSellGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(articleFormatToSell, objFormat, objArt)),
                    "В ассорт.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), !splitGroup);
            addRegularFilterGroup(filterToSellGroup);

            RegularFilterGroupEntity filterMinGroup = new RegularFilterGroupEntity(genID());
            filterMinGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(articleFormatMin, objFormat, objArt))),
                    "Без страх. запаса",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)), !splitGroup);
            addRegularFilterGroup(filterMinGroup);
        }
    }

    private class AssortmentFormEntity extends FormEntity {
        protected AssortmentFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Ассортимент товаров");
               //!!!
            ObjectEntity objFormat, objArt, objAssortment;

            objFormat = addSingleGroupObject(format);
            objAssortment = addSingleGroupObject(assortment, LM.name, nameFormatAssortment, assortmentDateFrom, assortmentDateTo);
            objArt = addSingleGroupObject(article);

            LM.addObjectActions(this, objFormat);
            LM.addObjectActions(this, objArt);
            LM.addObjectActions(this, objAssortment);
            addPropertyDraw(objFormat, LM.publicGroup, true);
            addPropertyDraw(objArt, LM.baseGroup, true, currentRRP);
            addPropertyDraw(objFormat, objArt, LM.publicGroup, true);
            addPropertyDraw(objAssortment, objArt, articleFormatToSell);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(formatAssortment, objAssortment), Compare.EQUALS, objFormat));

            RegularFilterGroupEntity filterToSellGroup = new RegularFilterGroupEntity(genID());
            filterToSellGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(articleFormatToSell, objAssortment, objArt)),
                    "В ассорт.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterToSellGroup);

        }
    }

    private class DocumentRevalueFormEntity extends ArticleNoCheckFormEntity {

        protected PropertyObjectEntity getCommitedQuantity() {
            return getPropertyObject(documentRevalued);
        }

        protected DocumentRevalueFormEntity(NavigatorElement parent, boolean toAdd, String sID) {
            super(parent, sID, documentRevalue, toAdd, true);
        }
    }

    private class DocumentNDSFormEntity extends ArticleNoCheckFormEntity {

        @Override
        protected Object[] getArticleProps() {
            return new Object[]{LM.baseGroup, true, currentNDS};
        }

        protected PropertyObjectEntity getCommitedQuantity() {
            return getPropertyObject(NDS);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(getPropertyObject(currentNDS))),
                    "Без НДС",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
        }

        protected DocumentNDSFormEntity(NavigatorElement parent, boolean toAdd, String sID) {
            super(parent, sID, documentNDS, toAdd);

            addHintsNoUpdate(currentNDSDoc);
            addHintsNoUpdate(currentNDSDate);
        }
    }

    private class IncomePriceFormEntity extends FormEntity {

        protected IncomePriceFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр цен", true);

            ObjectEntity objDoc = addSingleGroupObject(commitDoShopInc, "Документ", LM.date, nameSubjectIncOrder, nameSubjectOutOrder, nameLegalEntityIncOrder, nameLegalEntityOutOrder, numberInvoiceDocument, seriesInvoiceDocument, sumNDSOrder, sumWithDiscountOrder, sumNoNDSOrder, sumNDSRetailOrder, sumAddvOrder, sumRetailOrder, sumAddManfrOrder, sumManfrOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, LM.name, LM.objectValue, LM.barcode, nameUnitOfMeasureArticle);

            addPropertyDraw(objDoc, objArt, articleQuantity, priceOrderArticle, addvOrderArticle, ndsShopOrderPriceArticle, shopPrice, sumRetailOrderArticle, priceManfrOrderArticle, addManfrOrderArticle, ndsOrderArticle);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(shopPrice)));

            LM.addFAProp(documentPrintRetailGroup, "Реестр цен", this, objDoc);
        }
    }

    private class RevalueActFormEntity extends FormEntity {

        protected RevalueActFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Акт переоценки", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", LM.date, sumRevalBalance, sumPrevRetailOrder, sumNewPrevRetailOrder, sumPriceChangeOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, LM.name, LM.objectValue, LM.barcode);

            addPropertyDraw(objDoc, objArt, revalChangeBalance, prevPrice, sumPrevRetailOrderArticle, shopPrice, sumNewPrevRetailOrderArticle, sumPriceChangeOrderArticle);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(revalChangeBalance, objDoc, objArt)));

            LM.addFAProp(documentPrintRetailGroup, "Акт переоценки", this, objDoc);
        }
    }

    // накладные
    private abstract class PrintSaleFormEntity extends FormEntity {

        ObjectEntity objDoc;
        ObjectEntity objArt;

        protected PrintSaleFormEntity(NavigatorElement parent, String sID, String caption, boolean inclArticle) {
            super(parent, sID, caption, true);

            objDoc = addSingleGroupObject(getDocClass(), "Документ", LM.date, nameSubjectIncOrder, nameSubjectOutOrder, nameLegalEntityIncOrder, nameLegalEntityOutOrder, addressSubjectIncOrder, addressSubjectOutOrder, sumWithDiscountOrder, sumNDSOrder, sumNoNDSOrder, propsLegalEntityIncOrder, propsLegalEntityOutOrder, quantityOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objDoc, getDocGroups());

            if(inclArticle) {
                objArt = addSingleGroupObject(article, LM.name, nameUnitOfMeasureArticle);
                addPropertyDraw(objDoc, objArt, articleQuantity, priceOrderArticle, sumWithDiscountOrderArticle, ndsOrderArticle, sumNDSOrderArticle, sumNoNDSOrderArticle, priceManfrOrderArticle, addManfrOrderArticle);
                addFixedFilter(new NotNullFilterEntity(getPropertyObject(articleQuantity)));
            }

            LM.addMFAProp(getActionGroup(), caption, this, new ObjectEntity[]{objDoc});
        }

        protected ValueClass getDocClass() {
            return shipmentDocumentOut;
        }
        protected abstract Object[] getDocGroups();
        protected abstract AbstractGroup getActionGroup();
    }

    private class TTNFormEntity extends PrintSaleFormEntity {

        private TTNFormEntity(NavigatorElement parent, String sID, String caption, boolean inclArticle) {
            super(parent, sID, caption, inclArticle);

            addPropertyDraw(objDoc, weightOrder, transOrder);
            if(objArt!=null)
                addPropertyDraw(objDoc, objArt, weightOrderArticle, transOrderArticle);
        }

        @Override
        protected Object[] getDocGroups() {
            return new AbstractGroup[]{documentInvoiceSaleGroup, documentShipmentGroup, documentShipmentOutGroup, documentShipmentTransportGroup};
        }

        @Override
        protected AbstractGroup getActionGroup() {
            return documentShipmentTransportGroup;
        }
    }

    private class TNFormEntity extends PrintSaleFormEntity {

        private TNFormEntity(NavigatorElement parent, String sID, String caption, boolean inclArticle) {
            super(parent, sID, caption, inclArticle);
        }

        @Override
        protected Object[] getDocGroups() {
            return new Object[]{documentInvoiceSaleGroup, documentShipmentGroup, documentShipmentOutGroup};
        }

        @Override
        protected AbstractGroup getActionGroup() {
            return documentShipmentOutGroup;
        }
    }

    private class InvoiceFormEntity extends PrintSaleFormEntity {

        private InvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean inclArticle) {
            super(parent, sID, caption, inclArticle);
        }

        @Override
        protected ValueClass getDocClass() {
            return orderSaleInvoiceArticleRetail; // здесь может более абстрактный класс должен быть но пока нет смысла
        }

        @Override
        protected Object[] getDocGroups() {
            return new AbstractGroup[]{documentInvoiceSaleGroup};
        }

        @Override
        protected AbstractGroup getActionGroup() {
            return documentInvoiceSaleGroup;
        }
    }

    private class ActionFormEntity extends BarcodeFormEntity {
        protected ActionFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Акции");

            ObjectEntity objShop = addSingleGroupObject(shop);

            ObjectEntity objAction = addSingleGroupObject(action, LM.publicGroup, true);
            LM.addObjectActions(this, objAction);

            PropertyDrawEntity exclProp = addPropertyDraw(exclActionStore, objAction, objShop);
            exclProp.addColumnGroupObject(objShop.groupTo);
            exclProp.setPropertyCaption(addPropertyObject(LM.name, objShop));

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, LM.publicGroup, true);
            ObjectEntity objArt = addSingleGroupObject(article, LM.baseGroup, true, priceGroup);

            if (noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            addPropertyDraw(objAction, objArtGroup, LM.publicGroup, true);
            addPropertyDraw(objAction, objArt, LM.publicGroup, true);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(articleToGroup, objArt), Compare.EQUALS, objArtGroup),
                    "В группе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !noArticleGroups);
            addRegularFilterGroup(filterGroup);

            PropertyObjectEntity inActionImpl = addPropertyObject(inAction, objAction, objArt);
            RegularFilterGroupEntity inActionGroup = new RegularFilterGroupEntity(genID());
            inActionGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(inActionImpl),
                    "В акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            inActionGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(inActionImpl)),
                    "Не в акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)));
            addRegularFilterGroup(inActionGroup);

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction2, objAction, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(LM.seekBarcodeAction, objBarcode));
        }
    }

    private class PricersFormEntity extends FormEntity {

        protected PricersFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Ценники", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", LM.baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, LM.baseGroup, true);

            addPropertyDraw(objDoc, objArt, shopPrice);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(shopPrice)));
            addFixedFilter(new NotFilterEntity(new CompareFilterEntity(getPropertyObject(shopPrice), Compare.EQUALS, addPropertyObject(prevPrice, objDoc, objArt))));

            LM.addFAProp(documentPrintRetailGroup, "Ценники", this, objDoc);
        }
    }

        private class StickersFormEntity extends FormEntity {

        protected StickersFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Стикеры", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", LM.baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, LM.baseGroup, true);

            LM.addFAProp(documentPrintRetailGroup, "Стикеры", this, objDoc);
        }
    }

    private class SaleCertFormEntity extends DocumentFormEntity {

        @Override
        protected boolean isSaleForm() {
            return true;
        }

        public ObjectEntity objObligation;

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameImplSubjectIncOrder, propsCustomerImplIncOrder,
                    sumWithDiscountOrder, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum, payWithCard};
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleCertFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean allStores) {
            super(parent, sID, documentClass, toAdd);

            if (!toAdd)
                addPropertyDraw(LM.date, objDoc);

            if (allStores) {
                addPropertyDraw(objDoc, subjectOutOrder, nameSubjectOutOrder);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectOutOrder, objDoc), Compare.EQUALS, shopImplement));
            }

            objObligation = addSingleGroupObject(giftObligation);
            addPropertyDraw(LM.barcode, objObligation);
            addPropertyDraw(LM.name, objObligation);
            addPropertyDraw(obligationSum, objObligation);
            addPropertyDraw(obligationSumFrom, objObligation);
            addPropertyDraw(obligationToIssued, objObligation);
            addPropertyDraw(issueObligation, objDoc, objObligation);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(issueObligation, objDoc, objObligation)),
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), true);
            addRegularFilterGroup(filterGroup);

            if (!toAdd) {
                addPropertyDraw(checkRetailExported, objDoc);
            }

            addPropertyDraw(barcodeAddClient);
            addPropertyDraw(barcodeAddCert);
            addActionsOnObjectChange(objBarcode, true,
                                     addPropertyObject(barcodeAddCertAction, objBarcode),
                                     addPropertyObject(barcodeAddClientAction, objBarcode),
                                     addPropertyObject(barcodeAction2, objDoc, objBarcode),
                                     addPropertyObject(LM.seekBarcodeAction, objBarcode),
                                     addPropertyObject(LM.barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);
            design.setEnabled(LM.publicGroup, false, objObligation.groupTo);
            design.setEnabled(obligationSum, true);

            design.get(objObligation.groupTo).grid.defaultComponent = true;

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddClient)));
            design.addIntersection(design.get(getPropertyDraw(barcodeAddClient)), design.get(getPropertyDraw(LM.barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.setEditKey(barcodeAddClient, KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddCert)));
            design.addIntersection(design.get(getPropertyDraw(barcodeAddCert)), design.get(getPropertyDraw(barcodeAddClient)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.setEditKey(barcodeAddCert, KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.CTRL_DOWN_MASK));

            return design;
        }


    }

    public class SaleCheckCertFormEntity extends SaleCertFormEntity {

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        protected SaleCheckCertFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, saleCheckCert, toAdd, allStores);

            objDoc.caption = "Чек";

            if (toAdd) {
                addPropertyDraw(cashRegOperGroup, true);
            }
            addPropertyDraw(orderUserName, objDoc);
            getPropertyDraw(orderUserName).forceViewType = ClassViewType.HIDE;
            addPropertyDraw(accumulatedClientSum, objDoc);
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        protected Font getDefaultFont() {
            if (toAdd)
                return FONT_MEDIUM_BOLD;
            else
                return super.getDefaultFont();
        }

        @Override
        public ClientResultAction getClientApply(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance doc = formInstance.instanceFactory.getInstance(objDoc);
                ObjectInstance obligation = formInstance.instanceFactory.getInstance(objObligation);

                return VEDBL.cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSet(obligation.groupTo),
                        getPropertyDraw(obligationSum, objObligation), getPropertyDraw(issueObligation, objObligation),
                        getPropertyDraw(LM.name, objObligation), getPropertyDraw(obligationSum, objObligation),
                        getPropertyDraw(sumWithDiscountOrder, objDoc), getPropertyDraw(LM.barcode, objObligation),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc), null, null,
                        getPropertyDraw(orderUserName), getPropertyDraw(nameImplSubjectIncOrder), getPropertyDraw(accumulatedClientSum),
                        null, null, null, null, null);

            } else
                return super.getClientApply(formInstance);
        }

        @Override
        public String checkClientApply(Object result) {

            String check = VEDBL.cashRegController.checkCashRegApplyActions(result);
            if (check != null) return check;

            return super.checkClientApply(result);
        }

        @Override
        protected boolean hasExternalScreen() {
            return true;
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {
                design.getGroupPropertyContainer((GroupObjectView) null, cashRegOperGroup).add(design.get(getPropertyDraw(payWithCard)));
                design.get(getPropertyDraw(payWithCard)).constraints.directions = new SimplexComponentDirections(0.1, -0.1, 0, 0.1);

                design.setHeaderFont(FONT_MEDIUM_PLAIN);
                design.setFont(FONT_LARGE_BOLD, objObligation.groupTo);
            }

            return design;
        }
    }

    private class SaleInvoiceCertFormEntity extends SaleCertFormEntity {
        protected SaleInvoiceCertFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, saleInvoiceCert, toAdd, allStores);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameSubjectIncOrder, propsCustomerIncOrder,
                    sumWithDiscountOrder, orderSalePayCash, orderSalePayCard, orderSaleToDo, documentShipmentGroup};
        }
    }

    private class CouponIntervalFormEntity extends FormEntity {
        protected CouponIntervalFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Интервалы цен по купонам");

            ObjectEntity objIntervalAdd = addSingleGroupObject(DoubleClass.instance, "Цена товара от", LM.objectValue, couponGroup, true);
            objIntervalAdd.groupTo.initClassView = ClassViewType.PANEL;
            objIntervalAdd.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            ObjectEntity objInterval = addSingleGroupObject(DoubleClass.instance, "Цена товара", LM.objectValue, couponGroup, true);
            objInterval.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(couponIssueSum, objInterval)));
        }
    }

    private class CouponArticleFormEntity extends FormEntity {
        private ObjectEntity objArt;

        protected CouponArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Товары по купонам");

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, LM.baseGroup, true, couponGroup, true);
            objArt = addSingleGroupObject(article, LM.baseGroup, true, couponGroup, true);

            if (noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(articleToGroup, objArt), Compare.EQUALS, objArtGroup),
                    "В группе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !noArticleGroups);
            addRegularFilterGroup(filterGroup);

            PropertyObjectEntity inCouponImpl = addPropertyObject(inCoupon, objArt);
            RegularFilterGroupEntity inCouponGroup = new RegularFilterGroupEntity(genID());
            inCouponGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(inCouponImpl),
                    "В акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            inCouponGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(inCouponImpl)),
                    "Не в акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)));
            addRegularFilterGroup(inCouponGroup);

            setReadOnly(objArt, true);
            setReadOnly(xorCouponArticle, false);
        }
    }

    private class obligationDocumentFormEntity extends FormEntity {

        protected obligationDocumentFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Сертификаты");
            //форма отображения подарочных сертификатов и купонов

            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            ObjectEntity dateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
            ObjectEntity dateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
            gobjDates.add(dateFrom);
            gobjDates.add(dateTo);
            addGroup(gobjDates);
            gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjDates.initClassView = ClassViewType.PANEL;
            addPropertyDraw(dateFrom, LM.objectValue);
            addPropertyDraw(dateTo, LM.objectValue);


            ObjectEntity objOblig = addSingleGroupObject(obligation, "Сертификаты", LM.objectValue, LM.name, LM.barcode, LM.objectClassName, certToSaled, obligationSum, obligationSumFrom);

            addPropertyDraw(obligFromIssued, objOblig);
            addPropertyDraw(obligationIssued, objOblig);
            addPropertyDraw(shopOutName, objOblig);
            addPropertyDraw(clientIncName, objOblig);
            addPropertyDraw(obligationDocument, objOblig);
            addPropertyDraw(shopIncName, objOblig);
            addPropertyDraw(obligToIssued, objOblig);
            addPropertyDraw(sumWithDiscountObligation, objOblig);
            addPropertyDraw(discountSumObligation, objOblig);
            addPropertyDraw(clientOutName, objOblig);

            LM.addObjectActions(this, objOblig);


            ObjectEntity objOutArt = addSingleGroupObject(article, "Товары по выданному документу", LM.barcode, LM.name);
            addPropertyDraw(articleOutDocumentQuantity, objOblig, objOutArt);
            addPropertyDraw(articleOutDocumentPrice, objOblig, objOutArt);

            ObjectEntity objIncArt = addSingleGroupObject(article, "Товары по использованному документу", LM.barcode, LM.name);
            addPropertyDraw(articleIncDocumentQuantity, objOblig, objIncArt);
            addPropertyDraw(articleIncDocumentPrice, objOblig, objIncArt);


            addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleIncDocumentQuantity, objOblig, objIncArt)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleOutDocumentQuantity, objOblig, objOutArt)));

             //фильтры
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(betweenObligationIssuedDateFromDateTo, objOblig, dateFrom, dateTo)),
                                  "Выданные в интервале",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));


            addRegularFilterGroup(filterGroup);

            RegularFilterGroupEntity filterGroup2 = new RegularFilterGroupEntity(genID());
            filterGroup2.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(betweenObligationToIssuedDateFromDateTo, objOblig, dateFrom, dateTo)),
                                  "Использованные в интервале",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK)));


            addRegularFilterGroup(filterGroup2);


        }
    }


    private class ArticleInfoFormEntity extends BarcodeFormEntity {
        public final ObjectEntity objArt;

        protected ArticleInfoFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Справочник товаров");

            objArt = addSingleGroupObject(article, LM.objectValue, LM.name, LM.barcode, addvArticle, currentRRP, nameCurrencyArticle, nameArticleGroupArticle, fullNameArticle, nameUnitOfMeasureArticle, nameBrendArticle, nameCountryArticle, gigienaArticle, spirtArticle, statusArticle, weightArticle, coeffTransArticle);
            addPropertyDraw(importArticlesRRP);
            addPropertyDraw(importArticlesInfo);
        }
    }

    private class StoreInfoFormEntity extends FormEntity {
        public final ObjectEntity objStore;

        protected StoreInfoFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Справочник складов");

            objStore = addSingleGroupObject(store, LM.publicGroup, true, importDocs);
        }
    }

    private class DocumentArticleFormEntity extends FormEntity {
        private ObjectEntity objArt;
        private ObjectEntity objDoc;
        private ObjectEntity objShop;

        protected DocumentArticleFormEntity(NavigatorElement parent, String sID) {
                super(parent, sID, "Операции с товарами");

                objArt = addSingleGroupObject(article, LM.barcode, LM.name, nameArticleGroupArticle, nameBrendArticle, nameCountryArticle, nameUnitOfMeasureArticle);

                objShop = addSingleGroupObject(shop, LM.objectValue, LM.name, shopFormat);
                addPropertyDraw(articleFreeQuantity, objShop, objArt);

                GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
                ObjectEntity dateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
                ObjectEntity dateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
                gobjDates.add(dateFrom);
                gobjDates.add(dateTo);

                addGroup(gobjDates);
                gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
                gobjDates.initClassView = ClassViewType.PANEL;

                addPropertyDraw(dateFrom, LM.objectValue);
                addPropertyDraw(dateTo, LM.objectValue);


                objDoc = addSingleGroupObject(order, LM.objectValue, LM.objectClassName);
                addPropertyDraw(nameSubjectIncOrder, objDoc).forceViewType = ClassViewType.GRID;
                addPropertyDraw(priceOrderArticle, objDoc, objArt).forceViewType = ClassViewType.GRID;

                addPropertyDraw(LM.date, objDoc);


                addPropertyDraw(articleDocQuantity, objDoc, objArt);

                addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objDoc), Compare.GREATER_EQUALS, dateFrom));
                addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objDoc), Compare.LESS_EQUALS, dateTo));

                addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleDocQuantity, objDoc, objArt)));
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleFreeQuantity, objShop, objArt)));

                addFixedFilter(new OrFilterEntity(new CompareFilterEntity(addPropertyObject(nameSubjectOutOrder, objDoc), Compare.EQUALS, addPropertyObject(LM.name, objShop)),
                                                  new CompareFilterEntity(addPropertyObject(nameSubjectIncOrder, objDoc), Compare.EQUALS, addPropertyObject(LM.name, objShop))
                                                                     ));
            }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.defaultOrders.put(design.get(getPropertyDraw(LM.date)), true);

            design.get(objShop.groupTo).grid.constraints.fillVertical = 0.5;

            return design;
        }
    }

    private class ArticleSupplierFormEntity extends FormEntity {
           private ObjectEntity objSupplier;
           private ObjectEntity objArt;
           protected ArticleSupplierFormEntity(NavigatorElement parent, String sID) {
                   super(parent, sID, "Справочник поставщиков");

                   objSupplier = addSingleGroupObject(supplier, LM.name, nameLegalEntitySubject, unnLegalEntity, addressLegalEntity);

                   GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
                   ObjectEntity dateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
                   ObjectEntity dateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
                   gobjDates.add(dateFrom);
                   gobjDates.add(dateTo);

                   addGroup(gobjDates);
                   gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
                   gobjDates.initClassView = ClassViewType.PANEL;
                   addPropertyDraw(dateFrom, LM.objectValue);
                   addPropertyDraw(dateTo, LM.objectValue);

                   objArt = addSingleGroupObject(article, LM.objectValue, LM.name, LM.barcode);
                   addPropertyDraw(quantitySupplierArticleBetweenDates, objSupplier, objArt, dateFrom, dateTo);
                   addPropertyDraw(sumNoNDSSupplierArticleBetweenDates, objSupplier, objArt, dateFrom, dateTo);
                   addPropertyDraw(sumNoNDSRetailSupplierArticleBetweenDates, objSupplier, objArt, dateFrom, dateTo);
                   addPropertyDraw(markUpSupplierShopArticleBetweenDates, objSupplier, objArt, dateFrom, dateTo);
                   addPropertyDraw(markUpPercentSupplierShopArticleBetweenDates, objSupplier, objArt, dateFrom, dateTo);
                   addPropertyDraw(objArt, addvArticle, nameArticleGroupArticle, nameUnitOfMeasureArticle, currentRRP, nameCurrencyArticle);

                   addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSupplier, objArt), Compare.EQUALS, objSupplier));
                   }
       }

      private class ArticleSpecificationFormEntity extends FormEntity {
        public final ObjectEntity objArt;
        private ObjectEntity objSpecification;

        protected ArticleSpecificationFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Спецификации товаров");

            objArt = addSingleGroupObject(article, LM.objectValue, LM.name, LM.barcode, addvArticle, currentRRP, nameCurrencyArticle, nameArticleGroupArticle, fullNameArticle, nameUnitOfMeasureArticle, nameBrendArticle, nameCountryArticle, gigienaArticle, spirtArticle, statusArticle, weightArticle, coeffTransArticle);

            objSpecification = addSingleGroupObject(specification, LM.name);

            addPropertyDraw(articleToSpecification, objArt, objSpecification);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleToSpecification, objArt, objSpecification)));
        }
    }



    private class ArticleReportFormEntity extends FormEntity {
        public ObjectEntity objShop;
        public ObjectEntity objOrderInc;
        public ObjectEntity objOrderOut;

        protected ArticleReportFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Товарный отчёт");

            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            ObjectEntity objDateFrom = new ObjectEntity(genID(), DateClass.instance, "от");
            ObjectEntity objDateTo = new ObjectEntity(genID(), DateClass.instance, "до");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);
            addGroup(gobjDates);
            addPropertyDraw(objDateFrom, LM.objectValue);
            addPropertyDraw(objDateTo, LM.objectValue);

            gobjDates.initClassView = ClassViewType.PANEL;

            objShop = addSingleGroupObject(shop, LM.name);
            objShop.groupTo.initClassView = ClassViewType.PANEL;

            objOrderInc = addSingleGroupObject(orderInc, nameSubjectOutOrder, LM.date, numberInvoiceDocument, seriesInvoiceDocument, sumRetailOrder, sumWithDiscountCouponOrder, sumDiscountPayCouponOrder, sumPriceChangeOrder);

            objOrderOut = addSingleGroupObject(orderOut, nameSubjectIncOrder, LM.date, numberInvoiceDocument, seriesInvoiceDocument, sumRetailOrder, sumWithDiscountCouponOrder, sumDiscountPayCouponOrder, sumPriceChangeOrder);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objOrderInc), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objOrderInc), Compare.LESS_EQUALS, objDateTo));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objOrderOut), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objOrderOut), Compare.LESS_EQUALS, objDateTo));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectOutOrder, objOrderOut), Compare.EQUALS, objShop));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectIncOrder, objOrderInc), Compare.EQUALS, objShop));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(sumWithDiscountCouponOrder, objOrderInc)),
                                              new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(sumPriceChangeOrder, objOrderInc)),
                                                                 new NotNullFilterEntity(addPropertyObject(sumRetailOrder, objOrderInc)))));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(sumWithDiscountCouponOrder, objOrderOut)),
                                              new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(sumPriceChangeOrder, objOrderOut)),
                                                                 new NotNullFilterEntity(addPropertyObject(sumRetailOrder, objOrderOut)))));

            addPropertyDraw(sumRetailIncBetweenDate, objShop, objDateFrom, objDateTo);
            addPropertyDraw(sumRetailOutBetweenDate, objShop, objDateFrom, objDateTo);

            addPropertyDraw(sumWithDiscountCouponIncBetweenDate, objShop, objDateFrom, objDateTo);
            addPropertyDraw(sumWithDiscountCouponOutBetweenDate, objShop, objDateFrom, objDateTo);

            addPropertyDraw(sumDiscountPayCouponIncBetweenDate, objShop, objDateFrom, objDateTo);
            addPropertyDraw(sumDiscountPayCouponOutBetweenDate, objShop, objDateFrom, objDateTo);

            addPropertyDraw(sumPriceChangeBetweenDate, objShop, objDateFrom, objDateTo);

        }
    }

    private class CreateArticleFormEntity extends FormEntity {

        ObjectEntity objBarcode;
        ObjectEntity objArticle;

        public CreateArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", LM.objectValue);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticle = addSingleGroupObject(article, "Товар", LM.name, articleToGroup, nameArticleGroupArticle);
            objArticle.groupTo.setSingleClassView(ClassViewType.PANEL);

            //addActionsOnApply(addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            //addActionsOnApply(addPropertyObject(executeArticleCompositeItemSIDSupplier, objItem, objSIDArticleComposite, objSupplier));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setEnabled(objBarcode, false);
            return design;
        }
    }


/*    private class LogClientFormEntity extends FormEntity {
        protected LogClientFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Изменения клиентов");

            ObjectEntity objClient = addSingleGroupObject(customerCheckRetail, "Клиент", properties, LM.baseGroup, true);
            ObjectEntity objSession = addSingleGroupObject(session, "Транзакция", properties, LM.baseGroup, true);
            PropertyDrawEntity drawEntity = addPropertyDraw(logClientInitialSum, objClient, objSession);
            addFixedFilter(new NotNullFilterEntity(drawEntity.propertyObject));
        }
    }*/

    private class PayWithCardActionProperty extends ActionProperty {

        private PayWithCardActionProperty() {
            super(LM.genSID(), "Опл. карт.", new ValueClass[]{orderSaleRetail});

            askConfirm = true;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            //To change body of implemented methods use File | Settings | File Templates.
            DataObject document = BaseUtils.singleValue(keys);
            if(orderSalePayCash.read(session, modifier, document)==null && orderSalePayCard.read(session, modifier, document)==null) {
                orderSalePayCash.execute(null, session, modifier, document);
                orderSalePayCard.execute(sumWithDiscountObligationOrder.read(session, modifier, document), session, modifier, document);

                actions.add(new ApplyClientAction());
            } else
                actions.add(new MessageClientAction("Для оплаты карточкой очистите поля сумм : Карточкой и Наличными", "Оплатить карточкой"));
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK);
        }
    }

    private class PrintOrderCheckActionProperty extends ActionProperty {

        private PrintOrderCheckActionProperty() {
            super(LM.genSID(), "Печать", new ValueClass[]{orderSaleRetail});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            //To change body of implemented methods use File | Settings | File Templates.
            FormInstance<VEDBusinessLogics> remoteForm = executeForm.form;
            actions.add(((CommitSaleCheckRetailFormEntity) remoteForm.entity).getPrintOrderAction(remoteForm));
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK);
        }
    }

    public class SaleExportActionProperty extends ActionProperty {

        private final ClassPropertyInterface shopInterface;
        private final ClassPropertyInterface dateFrom;
        private final ClassPropertyInterface dateTo;

        public SaleExportActionProperty() {
            super(LM.genSID(), "Экспорт реализации", new ValueClass[]{shop, DateClass.instance, DateClass.instance});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            shopInterface = i.next();
            dateFrom = i.next();
            dateTo = i.next();
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            Integer shopID = (Integer) keys.get(shopInterface).object;
            try {
                new AbstractSaleExportTask(VEDBL, ((SaleExportTask) VEDBL.getScheduler().getTask("saleExport")).getPath(shopID), shopID) {
                    protected String getDbfName() {
                        return "datadat.dbf";
                    }

                    protected void setRemoteFormFilter(FormInstance formInstance) throws ParseException {
                        PropertyDrawInstance<?> dateDraw = formInstance.getPropertyDraw(LM.date);
                        dateDraw.toDraw.addTempFilter(new CompareFilterInstance(dateDraw.propertyObject, Compare.GREATER_EQUALS, keys.get(dateFrom)));
                        dateDraw.toDraw.addTempFilter(new CompareFilterInstance(dateDraw.propertyObject, Compare.LESS_EQUALS, keys.get(dateTo)));
                    }

                    protected void updateRemoteFormProperties(FormInstance formInstance) throws SQLException {
                    }
                }.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            actions.add(new MessageClientAction("Данные были успешно выгружены", "Экспорт"));
        }
    }

    public abstract class ImportActionProperty extends ActionProperty {

        protected ImportActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        protected ImportActionProperty(String sID, String caption, ValueClass[] classes) {
            super(sID, caption, classes);
        }

        protected void addImportObjectNameField(java.util.List<ImportField> fields, java.util.List<ImportProperty<?>> properties, java.util.List<ImportKey<?>> keys, ImportKey importKey, LP nameToObject, LP importProp, ConcreteCustomClass customClass) {
            addImportObjectCGField(fields, properties, keys, nameToObject, LM.name, importKey, importProp, customClass);
        }

        protected ImportKey addImportObjectCGField(java.util.List<ImportField> fields, java.util.List<ImportProperty<?>> properties, java.util.List<ImportKey<?>> keys, LP IDToObject, LP objectToID, ImportKey linkKey, LP linkProp, ConcreteCustomClass customClass) {
            ImportField importField = new ImportField(objectToID); fields.add(importField);
            ImportKey<?> importObjectKey = new ImportKey(customClass, IDToObject.getMapping(importField)); keys.add(importObjectKey);
            properties.add(new ImportProperty(importField, linkProp.getMapping(linkKey), LM.object(customClass).getMapping(importObjectKey)));
            properties.add(new ImportProperty(importField, objectToID.getMapping(importObjectKey)));
            return importObjectKey;
        }

        protected void addImportField(java.util.List<ImportField> fields, java.util.List<ImportProperty<?>> properties, ImportKey importKey, LP importProp) {
            addImportField(fields, properties, importProp, importKey);
        }

        protected void addImportField(java.util.List<ImportField> fields, java.util.List<ImportProperty<?>> properties, LP importProp, ImportKeyInterface... importKeys) {
            ImportField importField = new ImportField(importProp); fields.add(importField);
            properties.add(new ImportProperty(importField, importProp.getMapping(importKeys)));
        }

        protected void addImportConvertField(java.util.List<ImportField> fields, java.util.List<ImportProperty<?>> properties, ImportKey articleKey, LP articleProp, LP converter, DataClass fieldClass) {
            ImportField nameField = new ImportField(fieldClass); fields.add(nameField);
            properties.add(new ImportProperty(nameField, articleProp.getMapping(articleKey), converter.getMapping(nameField)));
        }

    }

    public class ImportOrderActionProperty extends ImportActionProperty {

        private final ClassPropertyInterface documentInterface;

        public ImportOrderActionProperty() {
            super(LM.genSID(), "Импортировать заявку", new ValueClass[]{orderDelivery});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            documentInterface = i.next();
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject document = keys.get(documentInterface);

            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xsl файл.");
                return;
            }

            java.util.List<ImportField> fields = new ArrayList<ImportField>();
            java.util.List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            java.util.List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            ImportField barcodeField = new ImportField(StringClass.get(13)); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, LM.barcode.getMapping(articleKey), padl.getMapping(barcodeField)));

            addImportField(fields, properties, articleKey, LM.name);
            addImportField(fields, properties, priceOrderArticle, document, articleKey);
            addImportField(fields, properties, articleOrderQuantity, document, articleKey);
            addImportField(fields, properties, ndsOrderArticle, document, articleKey);
            addImportObjectNameField(fields, properties, importKeys, articleKey, LM.nameToCountry, countryArticle, LM.country);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToUnitOfMeasure, unitOfMeasureArticle, unitOfMeasure);

            java.util.List<java.util.List<Object>> rows = new ArrayList<java.util.List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                java.util.List<Object> row = new ArrayList<Object>();

                for (int j = 0; j < fields.size(); j++) {
                    try {
                        row.add(fields.get(j).getFieldClass().parseString(sh.getCell(j, i).getContents()));
                    } catch (platform.server.data.type.ParseException e) {
                        logger.warn("Не конвертировано значение совйства", e);
                    }
                }

                rows.add(row);
            }

            new IntegrationService(session, new ImportTable(fields, rows), importKeys, properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        }

        @Override
        public DataClass getValueClass() {
            return FileActionClass.getDefinedInstance(false, "Файлы таблиц", "xls");
        }

        @Override
        public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
            super.proceedDefaultDraw(entity, form);
            entity.shouldBeLast = true;
            entity.forceViewType = ClassViewType.PANEL;
        }
    }

    public class ImportArticlesRRPActionProperty extends ImportActionProperty {

        public ImportArticlesRRPActionProperty() {
            super(LM.genSID(), "Импортировать RRP");
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xsl файл.");
                return;
            }

            java.util.List<ImportField> fields = new ArrayList<ImportField>();
            java.util.List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            java.util.List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            ImportField barcodeField = new ImportField(LM.barcode); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, LM.barcode.getMapping(articleKey), padl.getMapping(barcodeField)));

            addImportField(fields, properties, articleKey, LM.name);
            addImportField(fields, properties, articleKey, currentRRP);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToCurrency, currencyArticle, currency);

            java.util.List<java.util.List<Object>> rows = new ArrayList<java.util.List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                java.util.List<Object> row = new ArrayList<Object>();

                for (int j = 0; j < fields.size(); j++) {
                    try {
                        row.add(fields.get(j).getFieldClass().parseString(sh.getCell(j, i).getContents()));
                    } catch (platform.server.data.type.ParseException e) {
                        logger.warn("Не конвертировано значение совйства", e);
                    }
                }

                rows.add(row);
            }

            new IntegrationService(session, new ImportTable(fields, rows), importKeys, properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        }

        @Override
        public DataClass getValueClass() {
            return FileActionClass.getDefinedInstance(false, "Файлы таблиц", "xls");
        }
    }

    public class ImportArticlesInfoActionProperty extends ImportActionProperty {

        public ImportArticlesInfoActionProperty() {
            super(LM.genSID(), "Импортировать справочн.");
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xls файл.");
                return;
            }

            java.util.List<ImportField> fields = new ArrayList<ImportField>();
            java.util.List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            java.util.List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            ImportField barcodeField = new ImportField(LM.barcode); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, LM.barcode.getMapping(articleKey), padl.getMapping(barcodeField)));

            addImportField(fields, properties, articleKey, LM.name);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToArticleGroup, articleToGroup, articleGroup);
            addImportField(fields, properties, articleKey, fullNameArticle);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToUnitOfMeasure, unitOfMeasureArticle, unitOfMeasure);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToBrend, brendArticle, brend);
            addImportObjectNameField(fields, properties, importKeys, articleKey, LM.nameToCountry, countryArticle, LM.country);
            addImportField(fields, properties, articleKey, gigienaArticle);
            addImportField(fields, properties, articleKey, spirtArticle);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToGender, genderArticle, gender);

            java.util.List<java.util.List<Object>> rows = new ArrayList<java.util.List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                java.util.List<Object> row = new ArrayList<Object>();

                for (int j = 0; j < fields.size(); j++) {
                    try {
                        row.add(fields.get(j).getFieldClass().parseString(sh.getCell(j, i).getContents()));
                    } catch (platform.server.data.type.ParseException e) {
                        logger.warn("Не конвертировано значение совйства", e);
                    }
                }

                rows.add(row);
            }

            new IntegrationService(session, new ImportTable(fields, rows), importKeys, properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        }

        @Override
        public DataClass getValueClass() {
            return FileActionClass.getDefinedInstance(false, "Файлы таблиц", "xls");
        }
    }

    public class ImportDocsActionProperty extends ImportActionProperty {

        public ImportDocsActionProperty() {
            super(LM.genSID(), "Импортировать док.", new ValueClass[]{shop});
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject storeObject = BaseUtils.singleValue(keys);

            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xls файл.");
                return;
            }

            java.util.List<ImportField> fields = new ArrayList<ImportField>();
            java.util.List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            java.util.List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            // документ
            ImportField numberField = new ImportField(numberInvoiceDocument); fields.add(numberField);
            ImportKey<?> documentKey = new ImportKey(commitDeliveryShopLocal, invoiceDocumentNumber.getMapping(numberField)); importKeys.add(documentKey);
            properties.add(new ImportProperty(numberField, numberInvoiceDocument.getMapping(documentKey)));
            addImportField(fields, properties, LM.date, documentKey);

            properties.add(new ImportProperty(null, subjectIncOrder.getMapping(documentKey), LM.object(store).getMapping(storeObject)));

            // товар
            ImportField barcodeField = new ImportField(LM.barcode); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, LM.barcode.getMapping(articleKey), padl.getMapping(barcodeField)));
            addImportField(fields, properties, LM.name, articleKey);

            // поставщик
            ImportKey<?> supplierKey = addImportObjectCGField(fields, properties, importKeys, legalEntityUnn, unnLegalEntity, documentKey, subjectOutOrder, localSupplier);
            addImportField(fields, properties, LM.name, supplierKey);

            addImportField(fields, properties, outerCommitedQuantity, documentKey, articleKey); // пока не article так как не может протолкнуть класс внутрь
            addImportField(fields, properties, priceManfrOrderArticle, documentKey, articleKey);
            addImportField(fields, properties, priceOrderArticle, documentKey, articleKey);
            addImportField(fields, properties, ndsOrderArticle, documentKey, articleKey);
            addImportField(fields, properties, ndsShopOrderPriceArticle, documentKey, articleKey);
            addImportField(fields, properties, shopPrice, documentKey, articleKey);

            java.util.List<java.util.List<Object>> rows = new ArrayList<java.util.List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                java.util.List<Object> row = new ArrayList<Object>();

                for (int j = 0; j < fields.size(); j++) {
                    try {
                        row.add(fields.get(j).getFieldClass().parseString(sh.getCell(j, i).getContents()));
                    } catch (platform.server.data.type.ParseException e) {
                        logger.warn("Не конвертировано значение совйства", e);
                    }
                }

                rows.add(row);
            }

            new IntegrationService(session, new ImportTable(fields, rows), importKeys, properties).synchronize(true, true, false);

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        }

        @Override
        public DataClass getValueClass() {
            return FileActionClass.getDefinedInstance(false, "Файлы таблиц", "xls");
        }
    }

    public class DownToZeroActionProperty extends ActionProperty {

        public DownToZeroActionProperty() {
            super(LM.genSID(), "Обнулить остатки", new ValueClass[]{order});
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject documentObject = BaseUtils.singleValue(keys);

            // сколько в документе, сколько на остатках, уменьшаем на разницу
            KeyExpr docKey = new KeyExpr("doc"); KeyExpr articleKey = new KeyExpr("article");
            Expr newQuantity = articleQuantity.getExpr(modifier, documentObject.getExpr(), articleKey).sum(freeIncOrderArticle.getExpr(modifier, documentObject.getExpr(), articleKey).scale(-1));
            session.execute(articleQuantity.getDataChanges(newQuantity, newQuantity.getWhere().and(docKey.compare(documentObject, Compare.EQUALS)), modifier, docKey, articleKey), null, null);

            actions.add(new MessageClientAction("Остатки были успешно обнулены", "Обнуление остатков"));
        }
    }


}
