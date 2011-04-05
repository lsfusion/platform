package tmc;

import jxl.Sheet;
import jxl.Workbook;
import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.*;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.OrderType;
import platform.server.data.sql.DataAdapter;
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
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;
import tmc.integration.PanelExternalScreen;
import tmc.integration.PanelExternalScreenParameters;
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
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.List;


public class VEDBusinessLogics extends BusinessLogics<VEDBusinessLogics> {

    CashRegController cashRegController = new CashRegController(this);
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

    public VEDBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

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
    CustomClass orderWhole;
    CustomClass orderInvoiceRetail;
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

    protected void initClasses() {

        subject = addAbstractClass("subject", "Субъект", baseClass.named);

        action = addAbstractClass("action", "Акция", baseClass);
        saleAction = addConcreteClass("saleAction", "Распродажа", action);
        articleAction = addConcreteClass("articleAction", "Акции по позициям", action);

        contract = addAbstractClass("contract", "Договор", baseClass.named, transaction);
        contractDelivery = addAbstractClass("contractDelivery", "Договор на закупку", contract);
        contractSale = addAbstractClass("contractSale", "Договор на продажу", contract);
        contractSupplier = addConcreteClass("contractSupplier", "Договор с поставщиком", contractDelivery);
        contractStore = addConcreteClass("contractStore", "Договор между юр. лицами", contractSale, contractDelivery);
        contractCustomer = addConcreteClass("contractcustomer", "Договор с покупателями", contractSale);

        groupArticleAction = addConcreteClass("groupArticleAction", "Группа акций", baseClass.named);

        store = addAbstractClass("store", "Склад", subject);
        shop = addConcreteClass("shop", "Магазин", store);
        warehouse = addConcreteClass("warehouse", "Распред. центр", store);
        article = addConcreteClass("article", "Товар", baseClass.named, barcodeObject);
        articleGroup = addConcreteClass("articleGroup", "Группа товаров", baseClass.named);
        legalEntity = addAbstractClass("legalEntity", "Юр.лицо", baseClass.named);
        storeLegalEntity = addConcreteClass("storeLegalEntity", "Юр.лицо складов", legalEntity);

        currency = addConcreteClass("currency", "Валюта", baseClass.named);
        unitOfMeasure = addConcreteClass("unitOfMeasure", "Единица измерения", baseClass.named);

        // новый классы
        brend = addConcreteClass("brend", "Бренд", baseClass.named);
        line = addConcreteClass("line", "Линия", baseClass.named);
        gender = addConcreteClass("gender", "Пол", baseClass.named);

        contragent = addAbstractClass("contragent", "Контрагент", subject);
        contragentWhole = addAbstractClass("contragentWhole", "Оптовый контрагент", contragent, legalEntity);

        supplier = addAbstractClass("supplier", "Поставщик", contragentWhole);
        localSupplier = addConcreteClass("localSupplier", "Местный поставщик", supplier);
        importSupplier = addConcreteClass("importSupplier", "Импортный поставщик", supplier);

        customer = addAbstractClass("customer", "Покупатель", contragent);
        customerWhole = addConcreteClass("customerWhole", "Оптовый покупатель", customer, contragentWhole);
        customerInvoiceRetail = addConcreteClass("customerInvoiceRetail", "Покупатель по накладным", customer, legalEntity);
        customerCheckRetail = addConcreteClass("customerCheckRetail", "Розничный покупатель", customer, barcodeObject);

        format = addConcreteClass("format", "Формат", baseClass.named);

        documentShopPrice = addAbstractClass("documentShopPrice", "Изменение цены в магазине", transaction);
        documentRevalue = addConcreteClass("documentRevalue", "Переоценка в магазине", documentShopPrice);

        documentNDS = addConcreteClass("documentNDS", "Изменение НДС", transaction);

        // заявки на приход, расход
        order = addAbstractClass("order", "Заявка", transaction);

        orderInc = addAbstractClass("orderInc", "Заявка прихода на склад", order);
        orderShopInc = addAbstractClass("orderShopInc", "Заявка прихода на магазин", orderInc);
        orderWarehouseInc = addAbstractClass("orderWarehouseInc", "Заявка прихода на распред. центр", orderInc);

        orderDo = addAbstractClass("orderDo", "Прямая операция", order);
        orderReturn = addAbstractClass("orderReturn", "Возврат операции", order);

        orderInner = addAbstractClass("orderInner", "Внутренняя операция", order); // то есть с партиями
        returnOrderInner = addAbstractClass("returnOrderInner", "Возврат внутренней операции", orderInner, orderReturn);
        orderDoInner = addAbstractClass("orderDoInner", "Заказ", orderInner, orderDo);

        orderOut = addAbstractClass("orderOut", "Заявка расхода со склада", orderInner);
        orderStoreOut = addAbstractClass("orderStoreOut", "Заявка расхода со склада", orderOut);
        orderShopOut = addAbstractClass("orderShopOut", "Заявка расхода с магазина", orderOut);
        orderWarehouseOut = addAbstractClass("orderWarehouseOut", "Заявка расхода с распред. центра", orderOut);

        shipmentDocument = addAbstractClass("shipmentDocument", "Заявка на перевозку", order);
        shipmentDocumentOut = addAbstractClass("shipmentDocumentOut", "Заявка на перевозку со склада", shipmentDocument, orderOut);

        commitOut = addAbstractClass("commitOut", "Отгруженная заявка", order);
        commitInc = addAbstractClass("commitInc", "Принятая заявка", commitOut);

        commitInner = addAbstractClass("commitInner", "Отгруженный заказ", commitOut, orderDoInner);

        orderDoOut = addAbstractClass("orderDoOut", "Прямая заявка со склада", orderOut, orderDoInner);

        orderExtInc = addAbstractClass("orderExtInc", "Заявка внешн. контрагенту", orderInc, orderDo);
        orderExtIncReturn = addAbstractClass("orderExtIncReturn", "Заявка на возврат внешн. контрагенту", orderOut, returnOrderInner);
        orderExtOut = addAbstractClass("orderExtOut", "Заявка от внешн. контрагента", orderDoOut);
        orderExtOutReturn = addAbstractClass("orderExtOutReturn", "Заявка на возврат от внешн. контрагента", orderInc, returnOrderInner);

        commitDoShopInc = addAbstractClass("commitDoShopInc", "Принятый приход на магазин", documentShopPrice, orderShopInc, commitInc);
        commitReturnShopOut = addAbstractClass("commitReturnShopOut", "Отгруженный возврат с магазина", orderShopOut, commitOut);

        // внутр. и внешние операции
        orderDelivery = addAbstractClass("orderDelivery", "Закупка", orderExtInc); // всегда прих., создает партию - элементарную единицу учета
        commitDelivery = addAbstractClass("commitDelivery", "Приход от пост.", orderDelivery, commitInc, shipmentDocument);

        orderSale = addAbstractClass("orderSale", "Продажа", orderExtOut);
        orderSaleReturn = addAbstractClass("orderSaleReturn", "Возврат продажи", orderExtOutReturn);
        
        orderWhole = addAbstractClass("orderWhole", "Оптовая операция", order);
        orderInvoiceRetail = addAbstractClass("orderInvoiceRetail", "Розничная операция по накладной", order);

        orderRetail = addAbstractClass("orderRetail", "Розничная операция", baseClass);
        orderSaleRetail = addAbstractClass("orderSaleRetail", "Реализация через кассу", order, orderRetail);

        orderSaleWhole = addConcreteClass("orderSaleWhole", "Оптовый заказ", orderWarehouseOut, orderWhole, orderSale);
        invoiceSaleWhole = addConcreteClass("invoiceSaleWhole", "Выписанный оптовый заказ", orderSaleWhole, shipmentDocumentOut);
        commitSaleWhole = addConcreteClass("commitSaleWhole", "Отгруженный оптовый заказ", invoiceSaleWhole, commitInner);

        orderSaleArticleRetail = addAbstractClass("orderSaleArticleRetail", "Розничный заказ товаров", orderShopOut, orderSale);
        orderSaleInvoiceArticleRetail = addConcreteClass("orderSaleInvoiceArticleRetail", "Розничный заказ товаров по накладной", orderSaleArticleRetail, orderInvoiceRetail);
        commitSaleInvoiceArticleRetail = addConcreteClass("commitSaleInvoiceArticleRetail", "Отгруженный розничный заказ по накладной", commitInner,
                addConcreteClass("writtenOutSaleInvoiceArticleRetail", "Выписанный розничный заказ по накладной", orderSaleInvoiceArticleRetail, shipmentDocumentOut));
        commitSaleCheckArticleRetail = addConcreteClass("commitSaleCheckArticleRetail", "Реализация товаров через кассу", orderSaleArticleRetail, commitInner, orderSaleRetail);

        saleCert = addConcreteClass("saleCert", "Реализация сертификатов", order);
        saleInvoiceCert = addConcreteClass("saleInvoiceCert", "Реализация сертификатов по накладной", saleCert, orderInvoiceRetail);
        saleCheckCert = addConcreteClass("saleCheckCert", "Реализация сертификатов через кассу", saleCert, orderSaleRetail);

        balanceCheck = addConcreteClass("balanceCheck", "Инвентаризация", orderStoreOut, commitOut, orderInner, orderDo);

        orderDistribute = addAbstractClass("orderDistribute", "Внутреннее перемещение", orderOut, orderInc, orderInner);

        orderDistributeShop = addConcreteClass("orderDistributeShop", "Заказ на внутреннее перемещение на магазин", orderWarehouseOut, orderShopInc, orderDistribute, orderDoOut);
        commitDistributeShop = addConcreteClass("commitDistributeShop", "Принятое внутреннее перемещение на магазин", commitDoShopInc,
                addConcreteClass("loadedDistributeShop", "Отгруженное внутреннее перемещение на магазин", commitInner,
                        addConcreteClass("writtenOutDistributeShop", "Выписанное внутреннее перемещение на магазин", orderDistributeShop, shipmentDocumentOut)));

        orderLocal = addAbstractClass("orderLocal", "Операция с местным поставщиком", order);

        orderDeliveryLocal = addAbstractClass("orderDeliveryLocal", "Закупка у местного поставщика", orderDelivery, orderLocal);
        commitDeliveryLocal = addAbstractClass("commitDeliveryLocal", "Приход от местного поставщика", orderDeliveryLocal, commitDelivery);

        orderDeliveryShopLocal = addConcreteClass("orderDeliveryShopLocal", "Закупка у местного поставщика на магазин", orderDeliveryLocal, orderShopInc);
        commitDeliveryShopLocal = addConcreteClass("commitDeliveryShopLocal", "Приход от местного поставщика на магазин", orderDeliveryShopLocal, commitDeliveryLocal, commitDoShopInc);

        orderDeliveryWarehouseLocal = addConcreteClass("orderDeliveryWarehouseLocal", "Закупка у местного поставщика на распред. центр", orderDeliveryLocal, orderWarehouseInc);
        commitDeliveryWarehouseLocal = addConcreteClass("commitDeliveryWarehouseLocal", "Приход от местного поставщика на распред. центр", orderDeliveryWarehouseLocal, commitDeliveryLocal);

        orderDeliveryImport = addConcreteClass("orderDeliveryImport", "Закупка у импортного поставщика", orderDelivery, orderWarehouseInc);
        commitDeliveryImport = addConcreteClass("commitDeliveryImport", "Приход от импортного поставщика", orderDeliveryImport, commitDelivery);

        orderReturnDeliveryLocal = addConcreteClass("orderReturnDeliveryLocal", "Заявка на возврат местному поставщику", orderStoreOut, orderLocal, orderExtIncReturn);
        invoiceReturnDeliveryLocal = addConcreteClass("invoiceReturnDeliveryLocal", "Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal, shipmentDocumentOut);
        commitReturnDeliveryLocal = addConcreteClass("commitReturnDeliveryLocal", "Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleInvoice = addConcreteClass("returnSaleInvoice", "Возврат по накладной", commitInc, shipmentDocument, orderSaleReturn);
        returnSaleWhole = addConcreteClass("returnSaleWhole", "Оптовый возврат", orderWarehouseInc, orderWhole, returnSaleInvoice);
        returnSaleInvoiceRetail = addConcreteClass("returnSaleInvoiceRetail", "Возврат розничного заказа по накладной", orderShopInc, orderInvoiceRetail, returnSaleInvoice);
        returnSaleCheckRetail = addConcreteClass("returnSaleCheckRetail", "Возврат реализации через кассу", orderShopInc, commitInc, orderRetail, orderSaleReturn);

        orderDistributeWarehouse = addConcreteClass("orderDistributeWarehouse", "Заказ на возврат внутр. перемещ. на распред. центр", orderShopOut, orderWarehouseInc, returnOrderInner, orderDistribute);
        addConcreteClass("commitDistributeWarehouse", "Принятый возврат внутр. перемещ. на распред. центр", commitInc,
                addConcreteClass("loadedDistributeWarehouse", "Отгруженный возвр. внутр. перемещ. на распред. центр", commitReturnShopOut,
                        addConcreteClass("writtenOutDistributeWarehouse", "Выписанный возврат внутр. перемещ. на распред. центр", orderDistributeWarehouse, shipmentDocumentOut)));

        obligation = addAbstractClass("obligation", "Сертификат", baseClass.named, barcodeObject);
        coupon = addConcreteClass("coupon", "Купон", obligation);
        giftObligation = addConcreteClass("giftObligation", "Подарочный сертификат", obligation);

        document = addAbstractClass("document", "Документ", baseClass);
        revalueAct = addConcreteClass("revalueAct", "Акт переоценки", document);
    }

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

    protected void initGroups() {
        documentGroup = new AbstractGroup("Параметры документа");
        publicGroup.add(documentGroup);

        moveGroup = new AbstractGroup("Движение товаров");
        publicGroup.add(moveGroup);

        documentMoveGroup = new AbstractGroup("Текущие параметры документа");
        documentGroup.add(documentMoveGroup);

        priceGroup = new AbstractGroup("Ценовые параметры");
        publicGroup.add(priceGroup);

        documentPriceGroup = new AbstractGroup("Ценовые параметры документа");
        documentPriceGroup.createContainer = false;
        documentGroup.add(documentPriceGroup);

        documentRetailGroup = new AbstractGroup("Розн. параметры");
        documentRetailGroup.createContainer = false;
        documentPriceGroup.add(documentRetailGroup);

        documentPayGroup = new AbstractGroup("Параметры оплаты");
        documentPayGroup.createContainer = false;
        documentPriceGroup.add(documentPayGroup);

        documentObligationGroup = new AbstractGroup("Параметры оплаты");
        documentObligationGroup.createContainer = false;
        documentPriceGroup.add(documentObligationGroup);

        documentManfrGroup = new AbstractGroup("Параметры изготовителя");
        documentManfrGroup.createContainer = false;
        documentPriceGroup.add(documentManfrGroup);

        logisticsGroup = new AbstractGroup("Логистические параметры");
        publicGroup.add(logisticsGroup);

        documentLogisticsGroup = new AbstractGroup("Логистические параметры документа");
        documentGroup.add(documentLogisticsGroup);

        cashRegGroup = new AbstractGroup("Операции с ФР");
        cashRegGroup.createContainer = false;
        baseGroup.add(cashRegGroup);

        cashRegOperGroup = new AbstractGroup("Оперативные операции с ФР");
        cashRegGroup.add(cashRegOperGroup);

        cashRegAdminGroup = new AbstractGroup("Административные операции с ФР");
        cashRegGroup.add(cashRegAdminGroup);

        couponGroup = new AbstractGroup("Параметры купона");
        publicGroup.add(couponGroup);

        artExtraGroup = new AbstractGroup("Доп. атрибуты товара");
        publicGroup.add(artExtraGroup);

        documentPrintGroup = new AbstractGroup("Документы");
        publicGroup.add(documentPrintGroup);

        documentInvoiceSaleGroup = new AbstractGroup("Основание");
        documentPrintGroup.add(documentInvoiceSaleGroup);

        documentShipmentGroup = new AbstractGroup("Накладная");
        documentPrintGroup.add(documentShipmentGroup);

        documentShipmentOutGroup = new AbstractGroup("Отпуск");
        documentPrintGroup.add(documentShipmentOutGroup);

        documentShipmentTransportGroup = new AbstractGroup("Транспорт");
        documentPrintGroup.add(documentShipmentTransportGroup);
    }

    CustomClass saleCert;
    CustomClass saleInvoiceCert;
    CustomClass saleCheckCert;
    CustomClass documentShopPrice;

    LP balanceSklFreeQuantity;
    LP articleFreeQuantity;
    LP obligationSumFrom;
    LP couponFromIssued;
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
    LP articleSaleAction;

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
    LP articleDiscount;
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

    protected void initProperties() {

        removePercent = addSFProp("((prm1*(100-prm2))/100)", DoubleClass.instance, 2);
        addPercent = addSFProp("((prm1*(100+prm2))/100)", DoubleClass.instance, 2);
        LP backPercent = addSFProp("prm1*prm2/(100+prm2)", DoubleClass.instance, 2);
        LP calcPercent = addSFProp("prm1*100/prm2", DoubleClass.instance, 2);
        LP diff = addSFProp("prm1-prm2", DoubleClass.instance, 2);

        round1 = addSFProp("(ROUND(CAST((prm1) as NUMERIC(15,3)),-1))", DoubleClass.instance, 1);
        LP round0 = addSFProp("(ROUND(CAST((prm1) as NUMERIC(15,3)),0))", DoubleClass.instance, 1);
        padl = addSFProp("lpad(prm1,12,'0')", StringClass.get(12), 1);

        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);

        LP onlyPositive = addJProp(and1, 1, positive, 1);
        LP min = addSFProp(FormulaExpr.MIN2, DoubleClass.instance, 2);
        LP abs = addSFProp("ABS(prm1)", DoubleClass.instance, 1);

        LP groupParent = addDProp("groupParent", "Родительская группа", articleGroup, articleGroup);
        LP groupParentName = addJProp(baseGroup, "Родительская группа", name, groupParent, 1);

        articleToGroup = addDProp(idGroup, "articleToGroup", "Группа товаров", articleGroup, article); // принадлежность товара группе
        nameArticleGroupArticle = addJProp(baseGroup, "Группа товаров", name, articleToGroup, 1);

        coeffTransArticle = addDProp("coeffTransArticle", "Коэфф. гр. мест", DoubleClass.instance, article); // принадлежность товара группе
        weightArticle = addDProp("weightArticle", "Вес товара", DoubleClass.instance, article); // принадлежность товара группе

        payWithCard = addAProp(new PayWithCardActionProperty());
        printOrderCheck = addAProp(new PrintOrderCheckActionProperty());
        saleExport = addAProp(new SaleExportActionProperty());
        importOrder = addAProp(new ImportOrderActionProperty());
        importArticlesRRP = addAProp(new ImportArticlesRRPActionProperty());
        importArticlesInfo = addAProp(new ImportArticlesInfoActionProperty());
        importDocs = addAProp(new ImportDocsActionProperty());
        downToZero = addAProp(new DownToZeroActionProperty());

        computerShop = addDProp("computerShop", "Магазин рабочего места", shop, computer);
        currentShop = addJProp("Текущий магазин", computerShop, currentComputer);

        panelScreenComPort = addDProp(baseGroup, "panelComPort", "COM-порт табло", IntegerClass.instance, computer);
        cashRegComPort = addDProp(baseGroup, "cashRegComPort", "COM-порт фискального регистратора", IntegerClass.instance, computer);
        addJProp(baseGroup, "Магазин рабочего места", name, computerShop, 1);

        LP legalEntitySubject = addCUProp("legalEntitySubject", "Юр. лицо (ИД)", addDProp("legalEntityStore", "Юр. лицо (ИД)", storeLegalEntity, store), object(legalEntity));
        nameLegalEntitySubject = addJProp(baseGroup, "nameLegalEntitySubject", "Юр. лицо", name, legalEntitySubject, 1);

        // новые свойства местного поставщика
        LP[] propsLegalEntity = addDProp(baseGroup, "LegalEntity", new String[]{"unn", "address", "tel", "bankAddress", "account"},
                new String[]{"УНН", "Адрес", "Контактный телефон", "Адрес банка", "Счёт"}, new ValueClass[]{StringClass.get(20), StringClass.get(100), StringClass.get(20), StringClass.get(100), StringClass.get(30)}, legalEntity);
        unnLegalEntity = propsLegalEntity[0]; addressLegalEntity = propsLegalEntity[1];
        legalEntityUnn = addCGProp(privateGroup, false, "legalEntityUnn", false, "Юр. лицо", object(legalEntity), unnLegalEntity, unnLegalEntity, 1);
        LP[] propsLegalEntitySubject = addJProp(privateGroup, false, "Subject", propsLegalEntity, legalEntitySubject, 1);

        addressSubject = addCUProp(addDProp("addressStore", "Адрес", StringClass.get(100), store), propsLegalEntity[1]);

        LP contragentOrder = addCUProp("contragentOrder", true, "Контрагент", // generics
                addDProp("localSupplier", "Местный поставщик", localSupplier, orderLocal),
                addDProp("importSupplier", "Импортный поставщик", importSupplier, orderDeliveryImport),
                addDProp("wholeCustomer", "Оптовый покупатель", customerWhole, orderWhole),
                addDProp("invoiceRetailCustomer", "Розничный покупатель", customerInvoiceRetail, orderInvoiceRetail),
                addDProp("checkRetailCustomer", "Розничный покупатель", customerCheckRetail, orderSaleRetail));
        subjectOutOrder = addCUProp("subjectOutOrder", true, "От кого (ИД)", // generics
                            addJProp(and1, contragentOrder, 1, is(orderInc), 1),
                            addDProp("outStore", "Склад (расх.)", store, orderStoreOut),
                            addDProp("outShop", "Магазин (расх.)", shop, orderShopOut),
                            addDProp("outWarehouse", "Распред. центр (расх.)", warehouse, orderWarehouseOut),
                            addDProp("certStore", "Магазин (серт.)", shop, saleCert));
        subjectIncOrder = addCUProp("subjectIncOrder", true, "Кому (ИД)", addJProp(and1, contragentOrder, 1, is(orderOut), 1), // generics
                            addDProp("incShop", "Магазин (прих.)", shop, orderShopInc),
                            addDProp("incWarehouse", "Распред. центр (прих.)", warehouse, orderWarehouseInc));
        // имена
        nameSubjectIncOrder = addJProp(baseGroup, "nameSubjectIncOrder", "Кому", name, subjectIncOrder, 1); nameImplSubjectIncOrder = addJProp(true, "Кому", name, subjectIncOrder, 1);
        nameSubjectOutOrder = addJProp(baseGroup, "nameSubjectOutOrder", "От кого", name, subjectOutOrder, 1);

        addressSubjectIncOrder = addJProp("addressSubjectIncOrder", "Адрес (кому)", addressSubject, subjectIncOrder, 1);
        addressSubjectOutOrder = addJProp("addressSubjectOutOrder", "Адрес (от кого)", addressSubject, subjectOutOrder, 1);

        propsLegalEntityIncOrder = addJProp(privateGroup, false, "IncOrder", propsLegalEntitySubject, subjectIncOrder, 1);
        propsLegalEntityOutOrder = addJProp(privateGroup, false, "OutOrder", propsLegalEntitySubject, subjectOutOrder, 1);

        propsCustomerCheckRetail = addDProp(baseGroup, "", new String[]{"checkRetailCustomerPhone", "checkRetailCustomerBorn", "checkRetailCustomerAddress", "clientInitialSum"},
                            new String[]{"Телефон", "Дата рождения", "Адрес", "Начальная сумма"}, new ValueClass[] {StringClass.get(20), DateClass.instance, StringClass.get(40), DoubleClass.instance}, customerCheckRetail);
        bornCustomerCheckRetail = propsCustomerCheckRetail[1]; clientInitialSum = propsCustomerCheckRetail[3];
        propsCustomerIncOrder = addJProp(baseGroup, false, "IncOrder", propsCustomerCheckRetail, subjectIncOrder, 1); propsCustomerImplIncOrder = addJProp(baseGroup, true, "ImplIncOrder", propsCustomerCheckRetail, subjectIncOrder, 1);

        LP legalEntityIncOrder = addJProp("legalEntityIncOrder", "Юр. лицо (кому) (ИД)", legalEntitySubject, subjectIncOrder, 1);
        LP legalEntityOutOrder = addJProp("legalEntityOutOrder", "Юр. лицо (от кого) (ИД)", legalEntitySubject, subjectOutOrder, 1);

        nameLegalEntityIncOrder = addJProp("nameLegalEntityIncOrder", "Юр. лицо (кому)", name, legalEntityIncOrder, 1); propsLegalEntityIncOrder = addJProp(privateGroup, false, "LegalIncOrder", propsLegalEntity, "(кому)", legalEntityIncOrder, 1);
        nameLegalEntityOutOrder = addJProp("nameLegalEntityOutOrder", "Юр. лицо (от кого)", name, legalEntityOutOrder, 1); propsLegalEntityOutOrder = addJProp(privateGroup, false, "LegalOutOrder", propsLegalEntity, "(от кого)", legalEntityOutOrder, 1);

        LP diffOutInc = addJProp(diff2, subjectIncOrder, 1, subjectOutOrder, 2);
        LP allowedReturn = addIfElseUProp(diffOutInc, addJProp(diffOutInc, 2, 1), is(orderOut), 1); // потом по идее надо сравнивать что совпадают юрлица

        outerOrderQuantity = addDProp(documentGroup, "extIncOrderQuantity", "Кол-во заяв.", DoubleClass.instance, orderDelivery, article);
        outerCommitedQuantity = addDProp(documentGroup, "extIncCommitedQuantity", "Кол-во принятое", DoubleClass.instance, commitDelivery, article);
//        outerCommitedQuantity.setDerivedChange(outerOrderQuantity, 1, 2, is(commitInc), 1); // пока не будем делать так как идет ручное штрих-кодирование
        LP expiryDate = addDProp(baseGroup, "expiryDate", "Срок годн.", DateClass.instance, commitDelivery, article);

        // для возвратных своего рода generics
        LP returnOuterQuantity = addDProp("returnDeliveryLocalQuantity", "Кол-во возврата", DoubleClass.instance, orderReturnDeliveryLocal, article, commitDeliveryLocal);

        returnInnerCommitQuantity = addCUProp(documentGroup, "returnInnerCommitQuantity", "Кол-во возврата", // generics
                addDProp("returnSaleWholeQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleWhole, article, commitDelivery, commitSaleWhole),
                addDProp("returnSaleInvoiceRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleInvoiceRetail, article, commitDelivery, commitSaleInvoiceArticleRetail),
                addDProp("returnSaleCheckRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleCheckRetail, article, commitDelivery, commitSaleCheckArticleRetail),
                addDProp("returnDistributeShopQuantity", "Кол-во возврата", DoubleClass.instance, orderDistributeWarehouse, article, commitDelivery, commitDistributeShop));

        LP returnSameClasses = addCUProp( // generics - для ограничения что возвращать те же классы операций, повторяет верхнее свойство
                addCProp(LogicalClass.instance, true, returnSaleWhole, commitSaleWhole),
                addCProp(LogicalClass.instance, true, returnSaleInvoiceRetail, commitSaleInvoiceArticleRetail),
                addCProp(LogicalClass.instance, true, returnSaleCheckRetail, commitSaleCheckArticleRetail),
                addCProp(LogicalClass.instance, true, orderDistributeWarehouse, commitDistributeShop));

        LP orderInnerQuantity = addDProp("outOrderQuantity", "Кол-во", DoubleClass.instance, orderDoInner, article, commitDelivery);

        // инвентаризация
        innerBalanceCheck = addDProp(documentGroup, "innerBalanceCheck", "Остаток инв.", DoubleClass.instance, balanceCheck, article, commitDelivery);
        innerBalanceCheckDB = addDProp("innerBalanceCheckDB", "Остаток (по учету)", DoubleClass.instance, balanceCheck, article, commitDelivery);

        LP returnDistrCommitQuantity = addSGProp(privateGroup, "returnDistrCommitQuantity", true, "Возвр. кол-во", returnInnerCommitQuantity, 1, 2, 3);
        innerQuantity = addCUProp(documentGroup, "innerQuantity", true, "Кол-во", returnOuterQuantity, orderInnerQuantity, returnDistrCommitQuantity, addDUProp("balanceCheckQuantity", "Кол-во инв.", innerBalanceCheckDB, innerBalanceCheck));

        LP incSklCommitedQuantity = addSGProp(moveGroup, "incSklCommitedQuantity", true, "Кол-во прихода парт. на скл.",
                addCUProp(addJProp(and1, outerCommitedQuantity, 1, 2, equals2, 1, 3),
                        addJProp(and1, innerQuantity, 1, 2, 3, is(commitInc), 1)), subjectIncOrder, 1, 2, 3);

        LP outSklCommitedQuantity = addSGProp(moveGroup, "Кол-во отгр. парт. на скл.", addJProp("Кол-во отгр. парт.", and1, innerQuantity, 1, 2, 3, is(commitOut), 1), subjectOutOrder, 1, 2, 3);
        LP outSklQuantity = addSGProp(moveGroup, "Кол-во заяв. парт. на скл.", innerQuantity, subjectOutOrder, 1, 2, 3);

        // тут в общем-то должен не and1 идти а разница через формулу и SUProp на 0 для склада (то есть для склада неизвестно это нет, а для контрагента просто неизвестно)
        balanceSklCommitedQuantity = addJProp(moveGroup, "balanceSklCommitedQuantity", true, "Остаток парт. на скл.", and1, addDUProp(incSklCommitedQuantity, outSklCommitedQuantity), 1, 2, 3, is(store), 1);
        balanceSklFreeQuantity = addJProp(moveGroup, "balanceSklFreeQuantity", true, "Свободное кол-во на скл.", and1, addDUProp(incSklCommitedQuantity, outSklQuantity), 1, 2, 3, is(store), 1);
        addConstraint(addJProp("Кол-во резерва должно быть не меньше нуля", greater2, vzero, balanceSklFreeQuantity, 1, 2, 3), false);

        articleFreeQuantity = addSGProp(moveGroup, "articleFreeQuantity", true, "Свободное кол-во на скл.", balanceSklFreeQuantity, 1, 2);

        innerBalanceCheckDB.setDerivedChange(balanceSklCommitedQuantity, subjectOutOrder, 1, 2, 3);

        addJProp(moveGroup, "Остаток парт. прих.", balanceSklCommitedQuantity, subjectIncOrder, 1, 2, 3);
        addJProp(moveGroup, "Остаток парт. расх.", balanceSklCommitedQuantity, subjectOutOrder, 1, 2, 3);

        LP documentOutSklFreeQuantity = addJProp("Дост. парт. расх.", balanceSklFreeQuantity, subjectOutOrder, 1, 2, 3);

        LP returnedInnerQuantity = addSGProp("Кол-во возвр. парт.", returnInnerCommitQuantity, 4, 2, 3);
        confirmedInnerQuantity = addDUProp("Кол-во подтв. парт.", addJProp(and1, orderInnerQuantity, 1, 2, 3, is(commitOut), 1), returnedInnerQuantity);
        addConstraint(addJProp("Кол-во возврата должно быть не меньше кол-ва самой операции", greater2, vzero, confirmedInnerQuantity, 1, 2, 3), false);

        LP returnInnerConfirmedFreeQuantity = addJProp(documentGroup, "returnInnerFreeQuantity", "Дост. кол-во по возврату парт.", andNot1,
                addCUProp(addJProp(and1, addICProp(DoubleClass.instance, orderReturnDeliveryLocal, article, commitDelivery), 1, 2, 3, equals2, 3, 4), // для возврата поставщику - нет ограничений, все равно остаток меньше
                          addJProp(and1, confirmedInnerQuantity, 4, 2, 3, returnSameClasses, 1, 4)), 1, 2, 3, 4, allowedReturn, 1, 4); // для возврата на склад, diff а не same чтобы можно было реализацию через кассы возвращать без контрагента
        returnDistrCommitQuantity.setDG(false, returnInnerConfirmedFreeQuantity, 1, 2, 3, 4, date, 4, 4);
        
        // создаем свойства ограничения для расчета себестоимости (являются следствием addConstraint)
        LP fullFreeOrderArticleDelivery = addCUProp(documentOutSklFreeQuantity, addICProp(DoubleClass.instance, orderExtOutReturn, article, commitDelivery)); // для расхода не со склада нету ограничений
        documentInnerFreeQuantity = addJProp(documentMoveGroup, "Дост. кол-во по парт.", min, fullFreeOrderArticleDelivery, 1, 2, 3,
                        addCUProp(addSGProp(returnInnerConfirmedFreeQuantity, 1, 2, 3), addICProp(DoubleClass.instance, orderDo, article, commitDelivery)), 1, 2, 3); // для прямой операции нет ограничений
        returnInnerFreeQuantity = addJProp(min, fullFreeOrderArticleDelivery, 1, 2, 3, returnInnerConfirmedFreeQuantity, 1, 2, 3, 4);

        // добавляем свойства по товарам
        articleInnerQuantity = addDGProp(documentGroup, "articleInnerQuantity", true, "Кол-во", 2, false, innerQuantity, 1, 2, documentInnerFreeQuantity, 1, 2, 3, date, 3, 3);
        addIndex(articleInnerQuantity);
        documentFreeQuantity = addSGProp(documentMoveGroup, "Доступ. кол-во", documentInnerFreeQuantity, 1, 2);

        // для док. \ товара \ парт. \ док. прод.   - кол-во подтв. парт. если совпадают контрагенты
        returnInnerQuantity = addDGProp(documentGroup, "returnInnerQuantity", "Кол-во возврата", 2, false, returnInnerCommitQuantity, 1, 2, 4,
                returnInnerFreeQuantity, 1, 2, 3, 4, date, 3, 3);
        returnFreeQuantity = addSGProp(documentGroup, "Дост. кол-во к возврату", returnInnerFreeQuantity, 1, 2, 4);
        LP returnDocumentQuantity = addCUProp("Кол-во возврата", returnOuterQuantity, returnInnerQuantity); // возвратный документ\прямой документ
        addConstraint(addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", and1, allowedReturn, 1, 3, returnDocumentQuantity, 1, 2, 3), false);

        sumReturnedQuantity = addSGProp(documentGroup, "Кол-во возврата", returnInnerQuantity, 1, 3);
        sumReturnedQuantityFree = addSGProp(documentGroup, "Дост. кол-во к возврату", returnFreeQuantity, 1, 3);

        LP saleCertGiftObligation = addDProp("saleCertGiftObligation", "Выдать", LogicalClass.instance, saleCert, giftObligation);

        articleQuantity = addCUProp("articleQuantity", "Кол-во", outerCommitedQuantity, articleInnerQuantity);
        articleOrderQuantity = addCUProp("Заяв. кол-во", outerOrderQuantity, articleInnerQuantity);
        LP articleDocQuantity = addCUProp("Кол-во док.", addSUProp(Union.OVERRIDE, outerCommitedQuantity, outerOrderQuantity), articleInnerQuantity);
        LP absQuantity = addSGProp("Всего тов.", addCUProp(addJProp(abs, articleDocQuantity, 1, 2), addJProp(and1, addCProp(DoubleClass.instance, 1), saleCertGiftObligation, 1, 2)), 1);
        addConstraint(addJProp("Нельзя создавать пустые документы", andNot1, is(order), 1, addJProp(greater2, absQuantity, 1, vzero), 1), false);

        // ожидаемый приход на склад
        articleFreeOrderQuantity = addSUProp("articleFreeOrderQuantity", true, "Ожидаемое своб. кол-во", Union.SUM, articleFreeQuantity, addSGProp(moveGroup, "Ожидается приход", addJProp(andNot1, articleOrderQuantity, 1, 2, is(commitInc), 1), subjectIncOrder, 1, 2)); // сумма по еще не пришедшим

        articleBalanceCheck = addDGProp(documentGroup, "articleBalanceCheck", "Остаток инв.", 2, false, innerBalanceCheck, 1, 2, innerBalanceCheckDB, 1, 2, 3, date, 3, 3);

        LP articleBalanceSklCommitedQuantity = addSGProp(moveGroup, "articleBalanceSklCommitedQuantity", "Остаток тов. на скл.", balanceSklCommitedQuantity, 1, 2);
        addJProp(documentMoveGroup, "Остаток тов. прих.", articleBalanceSklCommitedQuantity, subjectIncOrder, 1, 2);
        addJProp(documentMoveGroup, "Остаток тов. расх.", articleBalanceSklCommitedQuantity, subjectOutOrder, 1, 2);

        // цены
        LP shopFormat = addDProp("shopFormat", "Формат", format, shop);
        addJProp(baseGroup, "Формат", name, shopFormat, 1);

        // новые свойства товара
        fullNameArticle = addDProp(artExtraGroup, "fullNameArticle", "Полное наименование", StringClass.get(100), article);
        gigienaArticle = addDProp(artExtraGroup, "gigienaArticle", "Гигиеническое разрешение", StringClass.get(50), article);
        spirtArticle = addDProp(artExtraGroup, "spirtArticle", "Содержание спирта", DoubleClass.instance, article);
        statusArticle = addDProp(artExtraGroup, "statusArticle", "Собственный/несобственный", LogicalClass.instance, article);

        brendArticle = addDProp("brendArticle", "Бренд товара (ИД)", brend, article);
        nameBrendArticle = addJProp(artExtraGroup, "Бренд товара", name, brendArticle, 1);

        countryArticle = addDProp("countryArticle", "Страна товара", country, article);
        nameCountryArticle = addJProp(baseGroup, "Страна товара", name, countryArticle, 1);

        LP articleLine = addDProp("articleLine", "Линия товара", line, article);
        addJProp(artExtraGroup, "Линия товара", name, articleLine, 1);

        genderArticle = addDProp("genderArticle", "Пол", gender, article);
        addJProp(artExtraGroup, "Пол", name, genderArticle, 1);
        //**************************************************************************************************************
        currentRRP = addDProp(priceGroup, "currentRRP", "RRP", DoubleClass.instance, article);
        currencyArticle = addDProp("currencyArticle", "Валюта (ИД)", currency, article);
        nameCurrencyArticle = addJProp(priceGroup, "nameCurrencyArticle", "Валюта", name, currencyArticle, 1);
        unitOfMeasureArticle = addDProp("unitOfMeasureArticle", "Ед. изм.", unitOfMeasure, article);
        nameUnitOfMeasureArticle = addJProp(baseGroup, "nameUnitOfMeasureArticle", "Ед. изм.", name, unitOfMeasureArticle, 1);
        LP currentCurrencyRate = addDProp(baseGroup, "currentCurrencyRate", "Курс", DoubleClass.instance, currency);
        LP currentFormatDiscount = addDProp(priceGroup, "currentFormatDiscount", "Скидка на формат", DoubleClass.instance, format);
        LP currentWarehouseDiscount = addDProp(priceGroup, "currentWarehouseDiscount", "Опт. скидка", DoubleClass.instance);

        LP addvBrend = addDProp(baseGroup, "addvBrend", "Наценка", DoubleClass.instance, brend);
        LP addvSetArticle = addDProp(priceGroup, "addvSetArticle", "Наценка по тов.", DoubleClass.instance, article);
        LP addvBrendArticle = addJProp("addvBrendArticle", "Наценка по бренду", addvBrend, brendArticle, 1);
        addvArticle = addSUProp(priceGroup, "addvArticle", true, "Дейст. наценка", Union.OVERRIDE, addvBrendArticle, addvSetArticle);

        // простые акции
        LP actionFrom = addDProp(baseGroup, "actionFrom", "От", DateClass.instance, action);
        LP actionTo = addDProp(baseGroup, "actionTo", "До", DateClass.instance, action);
        LP actionDiscount = addDProp(baseGroup, "actionDiscount", "Скидка", DoubleClass.instance, action);

        LP customerCheckRetailDiscount = addDProp(baseGroup, "customerCheckRetailDiscount", "Мин. скидка", DoubleClass.instance, customerCheckRetail);

        LP xorActionAll = addDProp(baseGroup, "xorActionAll", "Вкл./искл.", LogicalClass.instance, action);
        LP xorActionArticleGroup = addDProp(baseGroup, "xorActionArticleGroup", "Вкл./искл.", LogicalClass.instance, action, articleGroup);
        xorActionArticle = addDProp("xorArticle", "Вкл./искл.", LogicalClass.instance, action, article); // не включаем в группу, предполагается что реактирование через inAction идет
        inAction = addXorUProp(baseGroup, "inAction", true, "В акции", addJProp(and1, xorActionAll, 1, is(article), 2), addJProp(xorActionArticleGroup, 1, articleToGroup, 2), xorActionArticle);

        LP isStarted = addJProp(baseGroup, "Началась", and(true, true), is(action), 1,
                addJProp(less2, currentDate, actionFrom, 1), 1,  // активация акции, если текущая дата в диапазоне акции
                addJProp(greater2, currentDate, actionTo, 1), 1);

        articleSaleAction = addCGProp(priceGroup, false, "articleAction", "Дейст. распродажа",
                addJProp(and1, 1, addJProp(and1, inAction, 1, 2, addJProp(and1, isStarted, 1, is(saleAction), 1), 1), 1, 2), inAction, 2);
        articleDiscount = addSUProp("articleDiscount", true, "Тек. скидка", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, article), addJProp(priceGroup, "Тек. скидка", actionDiscount, articleSaleAction, 1));
        LP actionNoExtraDiscount = addDProp(baseGroup, "actionNoExtraDiscount", "Без доп. скидок", LogicalClass.instance, saleAction);

        LP articleActionToGroup = addDProp("articleActionToGroup", "Группа акций", groupArticleAction, articleAction);
        addJProp(baseGroup, "Группа акций", name, articleActionToGroup, 1);

        LP articleActionHourFrom = addDProp(baseGroup, "articleActionHourFrom", "Час от", DoubleClass.instance, articleAction);
        LP articleActionHourTo = addDProp(baseGroup, "articleActionHourTo", "Час до", DoubleClass.instance, articleAction);
        LP articleActionClientSum = addDProp(baseGroup, "articleActionClientSum", "Нак. сумма от", DoubleClass.instance, articleAction);
        LP articleActionQuantity = addDProp(baseGroup, "articleActionQuantity", "Кол-во от", DoubleClass.instance, articleAction);
        LP articleActionBirthDay = addDProp(baseGroup, "articleActionBirthDay", "День рожд.", LogicalClass.instance, articleAction);
        LP articleActionWithCheck = addDProp(baseGroup, "articleActionWithCheck", "Нак. с тек. чеком", LogicalClass.instance, articleAction);

        // продажа облигаций
        //**************************************************************************************************************
        // новые свойства для подарочных сертификатов
        LP certToSaled = addDProp(baseGroup, "certToSaled", "Продан заранее", LogicalClass.instance, obligation);
        LP sverka = addJProp(equals2, 1, addJProp(and1, 1, certToSaled, 1), 2);
        LP issueCoupon = addDProp("orderSaleCoupon", "Выдать купон", LogicalClass.instance, commitSaleCheckArticleRetail, coupon);
        issueObligation = addCUProp(documentPriceGroup, "Выдать", sverka, saleCertGiftObligation, issueCoupon);

        LP obligationIssued = addCGProp(null, "obligationIssued", true, "Выд. документ", addJProp(and1, 1, issueObligation, 1, 2), issueObligation, 2);

        obligationSum = addDProp(baseGroup, "obligationSum", "Сумма", DoubleClass.instance, obligation);
        setNotNull(obligationSum);
        obligationSumFrom = addDProp(baseGroup, "obligationSumFrom", "Сумма покупки", DoubleClass.instance, obligation);

        LP couponMaxPercent = addDProp(baseGroup, "couponMaxPercent", "Макс. процент по купонам", DoubleClass.instance);

        NDS = addDProp(documentGroup, "NDS", "НДС", DoubleClass.instance, documentNDS, article);
        LP[] maxNDSProps = addMGProp((AbstractGroup) null, true, new String[]{"currentNDSDate", "currentNDSDoc"}, new String[]{"Дата посл. НДС", "Посл. док. НДС"}, 1,
                addJProp(and1, date, 1, NDS, 1, 2), 1, 2);
        currentNDSDate = maxNDSProps[0];
        currentNDSDoc = maxNDSProps[1];
        addPersistent(currentNDSDate);
        addPersistent(currentNDSDoc);
        currentNDS = addJProp("currentNDS", true, "Тек. НДС", NDS, currentNDSDoc, 1, 1);

        LP ndsOrderDoArticle = addDCProp("ndsOrderDoArticle", "НДС", currentNDS, 2, articleQuantity, 1, 2, orderDo);
        LP ndsOrderReturnArticle = addMGProp(privateGroup, "ndsOrderReturnArticle", "НДС возвр.", addJProp(and1, ndsOrderDoArticle, 3, 2, returnDocumentQuantity, 1, 2, 3), 1, 2);
        ndsOrderArticle = addCUProp(baseGroup, "ndsOrderArticle", "НДС", ndsOrderDoArticle, ndsOrderReturnArticle);

        currentStoreDiscount = addCUProp(priceGroup, "Скидка на складе",
                addJProp(and1, currentWarehouseDiscount, is(warehouse), 1),
                addJProp(currentFormatDiscount, shopFormat, 1));    // берётся скидка формата, если её нет - оптовая скидка

        balanceFormatFreeQuantity = addSGProp(moveGroup, "Своб. кол-во по форм.", articleFreeQuantity, shopFormat, 1, 2);

        freeIncOrderArticle = addJProp("freeIncOrderArticle", "Своб. кол-во (прих.)", articleFreeQuantity, subjectIncOrder, 1, 2);

        // текущая схема
        articleSupplier = addDProp("articleSupplier", "Поставщик товара", supplier, article);
        addJProp(logisticsGroup, "Поставщик товара", name, articleSupplier, 1);
        LP shopWarehouse = addDProp("storeWarehouse", "Распред. центр", warehouse, shop); // магазин может числиться не более чем в одном распределяющем центре
        addJProp(logisticsGroup, "Распред. центр", name, shopWarehouse, 1);
        LP articleSupplierPrice = addDProp(logisticsGroup, "articleSupplierPrice", "Цена поставок", DoubleClass.instance, article);

        LP revalueShop = addDProp("revalueShop", "Магазин", shop, documentRevalue);
        addJProp(baseGroup, "Магазин", name, revalueShop, 1);

        documentRevalued = addDProp(documentGroup, "isRevalued", "Переоц.", LogicalClass.instance, documentRevalue, article);

        // ЦЕНЫ

        LP commitArticleQuantity = addJProp(and1, is(commitDoShopInc), 1, articleQuantity, 1, 2);

        LP[] maxCommitIncProps = addMGProp((AbstractGroup) null, true, new String[]{"currentCommitIncDate", "currentCommitIncDoc"}, new String[]{"Дата посл. прих. в маг.", "Посл. док. прих. в маг."}, 1,
                addJProp(and1, date, 1, commitArticleQuantity, 1, 2), 1, subjectIncOrder, 1, 2);
        LP currentCommitIncDate = maxCommitIncProps[0]; addPersistent(currentCommitIncDate);
        LP currentCommitIncDoc = maxCommitIncProps[1]; addPersistent(currentCommitIncDoc);

        LP[] maxRevaluePriceProps = addMGProp((AbstractGroup) null, true, new String[]{"currentRevalueDate", "currentRevalueDoc"}, new String[]{"Дата посл. переоц. в маг.", "Посл. док. переоц. в маг."}, 1,
                addJProp(and1, date, 1, documentRevalued, 1, 2), 1, revalueShop, 1, 2);
        LP currentRevalueDate = maxRevaluePriceProps[0]; addPersistent(currentRevalueDate);
        LP currentRevalueDoc = maxRevaluePriceProps[1]; addPersistent(currentRevalueDoc);

        LP[] maxShopPriceProps = addMUProp((AbstractGroup) null, new String[]{"currentShopPriceDate", "currentShopPriceDoc"}, new String[]{"Дата посл. цены в маг.", "Посл. док. цены в маг."}, 1,
                currentCommitIncDate, currentRevalueDate, currentCommitIncDoc, currentRevalueDoc);
        currentShopPriceDate = maxShopPriceProps[0]; addPersistent(currentShopPriceDate);
        currentShopPriceDoc = maxShopPriceProps[1]; addPersistent(currentShopPriceDoc);

        LP currentRRPPriceArticle = addJProp(priceGroup, "Необх. цена RRP", multiplyDouble2, currentRRP, 1, addJProp(currentCurrencyRate, currencyArticle, 1), 1);
        currentRRPPriceStoreArticle = addJProp(and1, currentRRPPriceArticle, 2, is(store), 1);

        LP revalueShopPrice = addDProp("revalueShopPrice", "Цена (прих.)", DoubleClass.instance, documentRevalue, article);
        LP returnShopOutPrice = addDProp("returnShopOutPrice", "Цена (возвр.)", DoubleClass.instance, commitReturnShopOut, article);
        LP incomeShopPrice = addDProp("shopPrice", "Цена (прих.)", DoubleClass.instance, commitDoShopInc, article);
        shopPrice = addCUProp(documentRetailGroup, "priceDocument", "Цена (маг.)", incomeShopPrice, returnShopOutPrice, revalueShopPrice);

        currentShopPrice = addJProp(priceGroup, "currentShopPrice", true, "Цена на маг. (тек.)", shopPrice, currentShopPriceDoc, 1, 2, 2);

        // цены в документах
        LP priceOrderDeliveryArticle = addDCProp("priceOrderDeliveryArticle", "Цена пост. с НДС", true, articleSupplierPrice, 2, articleQuantity, 1, 2, orderDelivery);
        priceAllOrderDeliveryArticle = addSUProp(documentPriceGroup, "priceAllOrderDeliveryArticle", "Цена пост. с НДС", Union.OVERRIDE, addJProp(and1, articleSupplierPrice, 2, is(orderDelivery), 1), priceOrderDeliveryArticle);
        LP orderSalePrice = addDProp("orderSalePrice", "Цена прод.", DoubleClass.instance, orderDoOut, article);
        LP priceOrderDoArticle = addCUProp(priceOrderDeliveryArticle, orderSalePrice);
        priceOrderArticle = addCUProp(documentPriceGroup, "priceOrderArticle", "Цена", priceOrderDoArticle, addMGProp(privateGroup, "priceOrderReturnArticle", "Цена возвр.", addJProp(and1, priceOrderDoArticle, 3, 2, returnDocumentQuantity, 1, 2, 3), 1, 2));
        LP priceNDSOrderArticle = addJProp(round1, addJProp(backPercent, priceOrderArticle, 1, 2, ndsOrderArticle, 1, 2), 1, 2);
        priceNoNDSOrderArticle = addDUProp("Цена без НДС", priceOrderArticle, priceNDSOrderArticle);

        LP priceManfrOrderDeliveryArticle = addDProp("priceManfrOrderDeliveryArticle", "Цена изг.", DoubleClass.instance, orderDelivery, article);
        priceManfrOrderDeliveryArticle.setDerivedForcedChange(true, priceNoNDSOrderArticle, 1, 2, articleQuantity, 1, 2);
        priceManfrOrderArticle = addCUProp(documentManfrGroup, "priceManfrOrderArticle", "Цена изг.", priceManfrOrderDeliveryArticle, addMGProp(privateGroup, "maxPriceManfrInnerArticle", "Макс. цена закуп.", addJProp(and1, priceManfrOrderDeliveryArticle, 3, 2, innerQuantity, 1, 2, 3), 1, 2));

        LP priceExtOrderIncArticle = addCUProp(addJProp(and1, priceNoNDSOrderArticle, 1, 2, is(orderDelivery), 1),
                addMGProp(privateGroup, "maxPriceInnerArticle", "Макс. цена закуп.", addJProp(and1, priceNoNDSOrderArticle, 3, 2, innerQuantity, 1, 2, 3), 1, 2));

        // с дублированием initRequiredStorePrice, чтобы не делать defaultChanged true
        LP requiredStorePrice = initRequiredStorePrice(priceGroup, "requiredStorePrice", true, "Необх. цена",
                addJProp(priceGroup, "Посл. цена прихода", priceExtOrderIncArticle, currentCommitIncDoc, 1, 2, 2), object(store));
        revalueShopPrice.setDerivedForcedChange(requiredStorePrice, revalueShop, 1, 2, documentRevalued, 1, 2);
        returnShopOutPrice.setDerivedForcedChange(currentShopPrice, subjectOutOrder, 1, 2, articleQuantity, 1, 2);
        incomeShopPrice.setDerivedForcedChange(true, initRequiredStorePrice(privateGroup, genSID(), false, "Необх. цена (прих.)", priceExtOrderIncArticle, subjectIncOrder), 1, 2, commitArticleQuantity, 1, 2);

        LP saleStorePrice = addCUProp(priceGroup, "Цена прод.", addJProp(and1, requiredStorePrice, 1, 2, is(warehouse), 1), currentShopPrice);
        orderSalePrice.setDerivedForcedChange(saleStorePrice, subjectOutOrder, 1, 2, articleQuantity, 1, 2);
        priceAllOrderSaleArticle = addSUProp(documentPriceGroup, "Цена прод.", Union.OVERRIDE, addJProp(and1, addJProp(saleStorePrice, subjectOutOrder, 1, 2), 1, 2, is(orderDoOut), 1), priceOrderDoArticle);

        LP outOfDatePrice = addJProp(and(false, false), vtrue, articleBalanceSklCommitedQuantity, 1, 2, addJProp(diff2, requiredStorePrice, 1, 2, currentShopPrice, 1, 2), 1, 2);
        documentRevalued.setDerivedChange(outOfDatePrice, revalueShop, 1, 2);

        priceStore = addCUProp("priceStore", true, "Склад (цены)", subjectIncOrder, revalueShop);
        inDocumentPrice = addCUProp("inDocumentPrice", true, "Изм. цены", documentRevalued, commitArticleQuantity);
        prevPrice = addDCProp(documentRetailGroup, "prevPrice", "Цена пред.", true, currentShopPrice, priceStore, 1, 2, inDocumentPrice, 1, 2);
        revalBalance = addDCProp(documentRetailGroup, "revalBalance", "Остаток переоц.", true, articleBalanceSklCommitedQuantity, priceStore, 1, 2, inDocumentPrice, 1, 2);
        isRevalued = addJProp(diff2, shopPrice, 1, 2, prevPrice, 1, 2); // для акта переоценки
        isNewPrice = addJProp(andNot1, inDocumentPrice, 1, 2, addJProp(equals2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2); // для ценников
        ndsShopOrderPriceArticle = addDCProp(documentRetailGroup, "ndsShopOrderPriceArticle", "НДС (маг.)", currentNDS, 2, inDocumentPrice, 1, 2);

        LP supplierCycle = addDProp(logisticsGroup, "supplierCycle", "Цикл поставок", DoubleClass.instance, supplier);
        LP shopCycle = addDProp(logisticsGroup, "shopCycle", "Цикл распределения", DoubleClass.instance, shop);

        LP supplierToWarehouse = addDProp(logisticsGroup, "supplierToWarehouse", "Пост. на распред. центр", LogicalClass.instance, supplier);

        // абстрактный товар \ склад - поставщик
        LP articleSuppliedOnWarehouse = addJProp(supplierToWarehouse, articleSupplier, 1);
        articleStoreSupplier = addSUProp("articleStoreSupplier", true, "Пост. товара на склад", Union.OVERRIDE, addJProp(and1, articleSupplier, 2, is(store), 1),
                addJProp(and1, shopWarehouse, 1, articleSuppliedOnWarehouse, 2));

        LP storeSupplierCycle = addCUProp(addJProp(and1, supplierCycle, 2, is(store), 1), addJProp(and1, shopCycle, 1, is(warehouse), 2));
        // цикл распределения, если от распределяющего центра или цикл поставок, если от поставщика

        articleStorePeriod = addJProp("articleStorePeriod", true, "Цикл поставок на склад", storeSupplierCycle, 1, articleStoreSupplier, 1, 2);

        articleFormatToSell = addDProp(logisticsGroup, "articleFormatToSell", "В ассортименте", LogicalClass.instance, format, article);
        articleFormatMin = addDProp(logisticsGroup, "articleFormatMin", "Страх. запас", DoubleClass.instance, format, article);

        LP articleStoreToSell = addCUProp(logisticsGroup, "articleStoreToSell", "В ассортименте", addJProp(articleFormatToSell, shopFormat, 1, 2),
                addDProp("articleWarehouseToSell", "В ассортименте", LogicalClass.instance, warehouse, article));

        articleStoreMin = addJProp("articleStoreMin", true, "Страх. запас", and1, addCUProp(logisticsGroup, "Страх. запас", addJProp(articleFormatMin, shopFormat, 1, 2),
                addDProp("articleWarehouseMin", "Страх. запас", DoubleClass.instance, warehouse, article)), 1, 2, articleStoreToSell, 1, 2);
        LP articleStoreForecast = addJProp(and1, addDProp(logisticsGroup, "articleStoreForecast", "Прогноз прод. (в день)", DoubleClass.instance, store, article), 1, 2, articleStoreToSell, 1, 2);

        // MAX((страховой запас+прогноз расхода до следующего цикла поставки)-остаток,0) (по внутренним складам)
        articleFullStoreDemand = addSUProp("articleFullStoreDemand", true, "Общ. необходимость", Union.SUM, addJProp(multiplyDouble2, addSupplierProperty(articleStoreForecast), 1, 2, articleStorePeriod, 1, 2), addSupplierProperty(articleStoreMin));
        LP articleStoreRequired = addJProp(onlyPositive, addDUProp(articleFullStoreDemand, addSupplierProperty(articleFreeOrderQuantity)), 1, 2);

        documentLogisticsRequired = addJProp(documentLogisticsGroup, "Необходимо", articleStoreRequired, subjectIncOrder, 1, 2);
        documentLogisticsSupplied = addJProp(documentLogisticsGroup, "Поставляется", equals2, subjectOutOrder, 1, addJProp(articleStoreSupplier, subjectIncOrder, 1, 2), 1, 2);
        documentLogisticsRecommended = addJProp(documentLogisticsGroup, "Рекомендовано", min, documentLogisticsRequired, 1, 2, documentFreeQuantity, 1, 2);

        LP orderClientSaleSum = addDProp("orderClientSaleSum", "Нак. сумма", DoubleClass.instance, orderSaleArticleRetail);
        LP orderClientInitialSum = addDCProp("orderClientInitialSum", "Нак. сумма", clientInitialSum, true, subjectIncOrder, 1);
        orderClientSum = addSUProp(baseGroup, "Нак. сумма", Union.SUM, addCProp(DoubleClass.instance, 0, orderSaleArticleRetail), orderClientSaleSum, orderClientInitialSum);
        orderHour = addDCProp(baseGroup, "orderHour", "Час", currentHour, is(orderSale), 1, orderSaleArticleRetail);

        changeQuantityTime = addTCProp(Time.EPOCH, "changeQuantityTime", "Время выбора", articleInnerQuantity, orderSaleArticleRetail);
        changeQuantityOrder = addOProp(documentGroup, "Номер", OrderType.SUM, addJProp(and1, addCProp(IntegerClass.instance, 1), articleInnerQuantity, 1, 2), true, true, 1, 1, changeQuantityTime, 1, 2);

        LP monthDay = addSFProp("EXTRACT(MONTH FROM prm1) * 40 + EXTRACT(DAY FROM prm1)", IntegerClass.instance, 1);
        orderBirthDay = addDCProp("orderBirthDay", "День рожд.", addJProp(equals2, monthDay, 1, addJProp(monthDay, bornCustomerCheckRetail, 1), 2), true, date, 1, subjectIncOrder, 1);

        sumManfrOrderArticle = addJProp(documentManfrGroup, "sumManfrOrderArticle", "Сумма изг.", multiplyDouble2, articleQuantity, 1, 2, priceManfrOrderArticle, 1, 2);
        sumManfrOrder = addSGProp(documentManfrGroup, "sumManfrOrder", "Сумма изг.", sumManfrOrderArticle, 1);

        sumOrderArticle = addJProp(documentPriceGroup, "sumOrderArticle", "Сумма", multiplyDouble2, articleQuantity, 1, 2, priceOrderArticle, 1, 2);
        weightOrderArticle = addJProp("weightOrderArticle", "Вес поз.", multiplyDouble2, articleQuantity, 1, 2, weightArticle, 2);
        transOrderArticle = addJProp("transOrderArticle", "Кол-во гр. мест", multiplyDouble2, articleQuantity, 1, 2, coeffTransArticle, 2);

        LP orderActionClientSum = addSUProp(Union.SUM, addJProp(and1, orderClientSum, 1, is(articleAction), 2), addJProp(and1, addSGProp(sumOrderArticle, 1), 1, articleActionWithCheck, 2));
        LP articleActionActive = addJProp(and(false, false, false, false, true, true, true, true, true), articleQuantity, 1, 2, is(orderSaleArticleRetail), 1, is(articleAction), 3, inAction, 3, 2, isStarted, 3,
                addJProp(less2, articleQuantity, 1, 2, articleActionQuantity, 3), 1, 2, 3,
                addJProp(and(false, true), articleActionBirthDay, 2, is(orderSaleArticleRetail), 1, orderBirthDay, 1), 1, 3,
                addJProp(less2, orderActionClientSum, 1, 2, articleActionClientSum, 2), 1, 3,
                addJProp(less2, orderHour, 1, articleActionHourFrom, 2), 1, 3,
                addJProp(greater2, orderHour, 1, articleActionHourTo, 2), 1, 3);

        orderNoDiscount = addDProp(baseGroup, "orderNoDiscount", "Без. скидок", LogicalClass.instance, orderSaleArticleRetail);
        LP orderArticleSaleDiscount = addDCProp("orderArticleSaleDiscount", "Скидка", true, addJProp(and(true, true),
                addSUProp(Union.MAX,
                        addSGProp(addMGProp(addJProp(and1, actionDiscount, 3, articleActionActive, 1, 2, 3), 1, 2, articleActionToGroup, 3), 1, 2),
                        addJProp(and1, addJProp(customerCheckRetailDiscount, subjectIncOrder, 1), 1, is(article), 2)), 1, 2,
                addJProp(actionNoExtraDiscount, articleSaleAction, 1), 2, orderNoDiscount, 1),
                true, 1, 2, articleQuantity, 1, 2);
        LP discountOrderReturnArticle = addMGProp(privateGroup, "discountOrderReturnArticle", "Скидка возвр.", addJProp(and1, orderArticleSaleDiscount, 3, 2, returnDocumentQuantity, 1, 2, 3), 1, 2);
        discountOrderArticle = addCUProp(baseGroup, "discountOrderArticle", "Скидка", orderArticleSaleDiscount, discountOrderReturnArticle); // возвращаем ту же скидку при возврате

        discountSumOrderArticle = addJProp(documentPriceGroup, "discountSumOrderArticle", "Сумма скидки", round1, addJProp(percent, sumOrderArticle, 1, 2, discountOrderArticle, 1, 2), 1, 2);
        sumWithDiscountOrderArticle = addDUProp(documentPriceGroup, "sumWithDiscountOrderArticle", "Сумма со скидкой", sumOrderArticle, discountSumOrderArticle);

        LP orderSalePayGift = addSGProp(addJProp(and(false, false, false), obligationSum, 2, issueObligation, 1, 2, is(order), 1, is(giftObligation), 2), 1);
        discountSumOrder = addSGProp(documentPriceGroup, "discountSumOrder", true, "Сумма скидки", discountSumOrderArticle, 1);
        sumWithDiscountOrder = addCUProp(documentPriceGroup, "sumWithDiscountOrder", true, "Сумма со скидкой", orderSalePayGift, addSGProp(sumWithDiscountOrderArticle, 1));

        sumNDSOrderArticle = addJProp(documentPriceGroup, "sumNDSOrderArticle", "Сумма НДС", round1, addJProp(backPercent, sumWithDiscountOrderArticle, 1, 2, ndsOrderArticle, 1, 2), 1, 2);
        sumNoNDSOrderArticle = addDUProp(documentPriceGroup, "sumNoNDSOrderArticle", "Сумма без НДС", sumWithDiscountOrderArticle, sumNDSOrderArticle);
        // док.
        sumNDSOrder = addSGProp(documentPriceGroup, "sumNDSOrder", "Сумма НДС", sumNDSOrderArticle, 1);
        sumNoNDSOrder = addDUProp(documentPriceGroup, "sumNoNDSOrder", "Сумма без НДС", sumWithDiscountOrder, sumNDSOrder);

        quantityOrder = addSGProp("quantityOrder", "Кол-во", articleQuantity, 1);
        weightOrder = addSGProp("weightOrder", "Общий вес", weightOrderArticle, 1);
        transOrder = addSGProp("transOrder", "Общее кол-во гр. мест", transOrderArticle, 1);

        // для бухгалтерии магазинов
        sumRetailOrderArticle = addJProp(documentRetailGroup, "sumRetailOrderArticle", "Сумма розн.", multiplyDouble2, shopPrice, 1, 2, articleQuantity, 1, 2);
        sumNDSRetailOrderArticle = addJProp(documentRetailGroup, "sumNDSRetailOrderArticle", "Сумма НДС (розн.)", round1, addJProp(backPercent, sumRetailOrderArticle, 1, 2, ndsShopOrderPriceArticle, 1, 2), 1, 2);
        sumWithoutNDSRetailOrderArticle = addJProp(documentRetailGroup, "sumWithoutNDSRetailOrderArticle", "Сумма розн. без НДС", diff, sumRetailOrderArticle, 1, 2, sumNDSRetailOrderArticle, 1, 2);
        sumAddvOrderArticle = addJProp(documentRetailGroup, "sumAddvOrderArticle", "Сумма нацен.", diff, sumWithoutNDSRetailOrderArticle, 1, 2, sumNoNDSOrderArticle, 1, 2);
        addvOrderArticle = addJProp(documentRetailGroup, "addvOrderArticle", "Наценка", round0, addJProp(calcPercent, sumAddvOrderArticle, 1, 2, sumNoNDSOrderArticle, 1, 2), 1, 2);
        isNegativeAddvOrderArticle = addJProp(less2, addvOrderArticle, 1, 2, vzero);

        // изг.
        sumAddManfrOrderArticle = addJProp(documentManfrGroup, "sumAddManfrOrderArticle", "Сумма опт. нац.", diff, sumNoNDSOrderArticle, 1, 2, sumManfrOrderArticle, 1, 2);
        addManfrOrderArticle = addJProp(documentManfrGroup, "addManfrOrderArticle", "Опт. нац.", round0, addJProp(calcPercent, sumAddManfrOrderArticle, 1, 2, sumManfrOrderArticle, 1, 2), 1, 2);

        // док.
        sumRetailOrder = addSGProp(documentRetailGroup, "sumRetailOrder", "Сумма розн.", sumRetailOrderArticle, 1);
        sumNDSRetailOrder = addSGProp(documentRetailGroup, "sumNDSRetailOrder", "Сумма НДС (розн.)", sumNDSRetailOrderArticle, 1);
        sumWithoutNDSRetailOrder = addDUProp(documentRetailGroup, "sumWithoutNDSRetailOrder", "Сумма розн. без НДС", sumRetailOrder, sumNDSRetailOrder);
        sumAddvOrder = addJProp(documentRetailGroup, "sumAddvOrder", "Сумма нацен.", diff, sumWithoutNDSRetailOrder, 1, sumNoNDSOrder, 1);
        addvOrder = addJProp(documentRetailGroup, "addvOrder", "Наценка", round0, addJProp(calcPercent, sumAddvOrder, 1, sumNoNDSOrder, 1), 1);

        // изг.
        sumAddManfrOrder = addJProp(documentManfrGroup, "sumAddManfrOrder", "Сумма опт. нац.", diff, sumNoNDSOrder, 1, sumManfrOrder, 1);
        addManfrOrder = addJProp(documentManfrGroup, "addManfrOrder", "Опт. нац.", round0, addJProp(calcPercent, sumAddManfrOrder, 1, sumManfrOrder, 1), 1);

        // переоценка
        revalChangeBalance = addJProp("revalChangeBalance", "Остаток (изм.)", and1, revalBalance, 1, 2, addJProp(diff2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2);
        sumNewPrevRetailOrderArticle = addJProp("sumNewPrevRetailOrderArticle", "Сумма розн. (нов.)", multiplyDouble2, shopPrice, 1, 2, revalChangeBalance, 1, 2);
        sumPrevRetailOrderArticle = addJProp("sumPrevRetailOrderArticle", "Сумма розн. (пред.)", multiplyDouble2, prevPrice, 1, 2, revalChangeBalance, 1, 2);
        sumPriceChangeOrderArticle = addJProp("sumPriceChangeOrderArticle", "Сумма переоц.", diff, sumNewPrevRetailOrderArticle, 1, 2, sumPrevRetailOrderArticle, 1, 2);
        sumRevalBalance = addSGProp("sumRevalBalance", "Кол-во переоц.", revalChangeBalance, 1);
        sumNewPrevRetailOrder = addSGProp("sumNewPrevRetailOrder", "Сумма розн. (нов.)", sumNewPrevRetailOrderArticle, 1);
        sumPrevRetailOrder = addSGProp("sumPrevRetailOrder", "Сумма розн. (пред.)", sumPrevRetailOrderArticle, 1);
        sumPriceChangeOrder = addJProp("sumPriceChangeOrder", "Сумма переоц.", diff, sumNewPrevRetailOrder, 1, sumPrevRetailOrder, 1);

/*        // для товарного отчета
        documentRevalueAct = addDProp(baseGroup, "documentRevalueAct", "Документ", documentShopPrice, revalueAct);
        LP revalueActDocument = addAGProp(baseGroup, false, "revalueActDocument", false, "Акт переоценки", revalueAct, documentRevalueAct);
        sumPriceChangeOrder.follows(revalueActDocument, 1);
        is(revalueAct).follows(documentRevalueAct, 1);
*/
        orderSaleUseObligation = addDProp("orderSaleUseObligation", "Использовать", LogicalClass.instance, commitSaleCheckArticleRetail, obligation);
        LP obligationUseSum = addJProp(and1, obligationSum, 2, orderSaleUseObligation, 1, 2);

        obligationDocument = addCGProp(null, "obligationDocument", "Исп. документ", addJProp(and1, 1, orderSaleUseObligation, 1, 2), orderSaleUseObligation, 2);

        LP addDays = addSFProp("prm1+prm2", DateClass.instance, 2);

        couponStart = addDProp(baseGroup, "couponStart", "Дата начала купонов", DateClass.instance);
        LP couponExpiry = addDProp(baseGroup, "couponExpiry", "Дата окончания купонов", DateClass.instance);
        LP certExpiry = addDProp(baseGroup, "certExpiry", "Срок действия серт.", IntegerClass.instance);

        LP dateIssued = addJProp("Дата выдачи", date, obligationIssued, 1);
        couponFromIssued = addDCProp(baseGroup, "couponFromIssued", "Дата начала", couponStart, dateIssued, 1, coupon);
        LP couponToIssued = addDCProp("couponToIssued", "Дата окончания", couponExpiry, obligationIssued, 1, coupon);
        LP certToIssued = addDCProp("certToIssued", "Дата окончания", addJProp(addDays, 1, certExpiry), dateIssued, 1, giftObligation);

        obligationToIssued = addCUProp(baseGroup, "obligationToIssued", "Дата окончания", couponToIssued, certToIssued);
        LP orderSaleObligationAllowed = addJProp(and(false, true, true, true), is(commitSaleCheckArticleRetail), 1, obligationIssued, 2,
                addJProp(less2, sumWithDiscountOrder, 1, obligationSumFrom, 2), 1, 2,
                addJProp(greater2, date, 1, obligationToIssued, 2), 1, 2,
                addJProp(less2, date, 1, couponFromIssued, 2), 1, 2);
        addConstraint(addJProp("Нельзя использовать выбранный сертификат", andNot1, orderSaleUseObligation, 1, 2, orderSaleObligationAllowed, 1, 2), false);

        LP orderSaleObligationCanBeUsed = addJProp(andNot1, orderSaleObligationAllowed, 1, 2, obligationDocument, 2);
        orderSaleObligationCanNotBeUsed = addJProp(and(false, true), is(commitSaleCheckArticleRetail), 1, is(obligation), 2, orderSaleObligationCanBeUsed, 1, 2);

        LP orderMaxCoupon = addDCProp("orderMaxCoupon", "Макс. процент по купонам", couponMaxPercent, is(orderSaleArticleRetail), 1);

        // сумма без сертификатов
        LP orderSalePayCoupon = addJProp("orderSalePayCoupon", true, "Сумма куп." , min, addSGProp(addJProp(and1, obligationUseSum, 1, 2, is(coupon), 2), 1), 1, addJProp(percent, sumWithDiscountOrder, 1, orderMaxCoupon, 1), 1);
        LP orderSalePayGiftObligation = addSGProp("orderSalePayGiftObligation", true, "Сумма под. серт.", addJProp(and1, obligationUseSum, 1, 2, is(giftObligation), 2), 1);
        orderSalePayObligation = addSUProp(documentObligationGroup, "orderSalePayObligation", true, "Сумма серт.", Union.SUM, orderSalePayGiftObligation, orderSalePayCoupon);
        sumWithDiscountObligationOrder = addJProp(documentObligationGroup, "sumWithDiscountObligationOrder", true, "Сумма к опл.", onlyPositive, addDUProp(sumWithDiscountOrder, orderSalePayObligation), 1);
        sumWithDiscountObligationOrderArticle = addPGProp(privateGroup, "sumWithDiscountObligationOrderArticle", false, -1, true, "Сумма со скидкой", sumWithDiscountOrderArticle, sumWithDiscountObligationOrder, 1);

        // пока для товарного отчета
        sumWithDiscountCouponOrder = addJProp(documentObligationGroup, "sumWithDiscountCouponOrder", true, "Сумма без куп.", onlyPositive, addDUProp(sumWithDiscountOrder, orderSalePayCoupon), 1);
        sumDiscountPayCouponOrder = addSUProp(documentObligationGroup, "sumDiscountPayCouponOrder", true, "Сумма серт.", Union.SUM, discountSumOrder, orderSalePayCoupon);

        sumRetailIncBetweenDate = addSGProp(baseGroup, "sumRetailIncBetweenDate", "Приходная сумма за интервал",
                addJProp(and(false, false), sumRetailOrder, 1, is(orderInc), 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), subjectIncOrder, 1, 2, 3);
        sumRetailOutBetweenDate = addSGProp(baseGroup, "sumRetailOutBetweenDate", "Расходная сумма за интервал",
                addJProp(and(false, false), sumRetailOrder, 1, is(orderOut), 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), subjectOutOrder, 1, 2, 3);

        sumWithDiscountCouponIncBetweenDate = addSGProp(baseGroup, "sumWithDiscountCouponIncBetweenDate", "Приходная сумма без скидки за интервал",
                addJProp(and(false, false), sumWithDiscountCouponOrder, 1, is(orderInc), 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), subjectIncOrder, 1, 2, 3);
        sumWithDiscountCouponOutBetweenDate = addSGProp(baseGroup, "sumWithDiscountCouponOutBetweenDate", "Расходная сумма без скидки за интервал",
                addJProp(and(false, false), sumWithDiscountCouponOrder, 1, is(orderOut), 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), subjectOutOrder, 1, 2, 3);

        sumDiscountPayCouponIncBetweenDate = addSGProp(baseGroup, "sumDiscountPayCouponIncBetweenDate", "Приходная сумма скидки за интервал",
                addJProp(and(false, false), sumDiscountPayCouponOrder, 1, is(orderInc), 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), subjectIncOrder, 1, 2, 3);
        sumDiscountPayCouponOutBetweenDate = addSGProp(baseGroup, "sumDiscountPayCouponOutBetweenDate", "Расходная сумма скидки за интервал",
                addJProp(and(false, false), sumDiscountPayCouponOrder, 1, is(orderOut), 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), subjectOutOrder, 1, 2, 3);

        sumPriceChangeBetweenDate = addSGProp(baseGroup, "sumPriceChangeBetweenDate", "Сумма переоценки за интервал",
                addJProp(and(false, false), sumPriceChangeOrder, 1, is(orderInc), 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), subjectIncOrder, 1, 2, 3);





        LP clientSaleSum = addSGProp(sumWithDiscountObligationOrder, subjectIncOrder, 1);
        orderClientSaleSum.setDerivedChange(clientSaleSum, subjectIncOrder, 1);
        clientSum = addSUProp(baseGroup, "clientSum", true, "Нак. сумма", Union.SUM, clientSaleSum, clientInitialSum);
        accumulatedClientSum = addJProp("Накопленная сумма", clientSum, subjectIncOrder, 1);

        orderSalePayCash = addDProp(documentPayGroup, "orderSalePayCash", "Наличными", DoubleClass.instance, orderSaleRetail);
        orderSalePayCard = addDProp(documentPayGroup, "orderSalePayCard", "Карточкой", DoubleClass.instance, orderSaleRetail);

        impSumCard = addDProp(baseGroup, "inpSumCard", "Безнал. в кассе (ввод)", DoubleClass.instance, DateClass.instance, shop);
        LP curCard = addJProp(cashRegGroup, true, "Безнал. в кассе (ввод)", impSumCard, currentDate, currentShop);
        impSumCash = addDProp(baseGroup, "inpSumCash", "Наличных в кассе (ввод)", DoubleClass.instance, DateClass.instance, shop);
        LP curCash = addJProp(cashRegGroup, true, "Наличных в кассе (ввод)", impSumCash, currentDate, currentShop);
        impSumBank = addDProp(baseGroup, "inpSumBank", "Отправить в банк", DoubleClass.instance, DateClass.instance, shop);
        LP curBank = addJProp(cashRegGroup, true, "Отправить в банк (ввод)", impSumBank, currentDate, currentShop);

        LP allOrderSalePayCard = addSGProp(baseGroup, "Безнал. в кассе", orderSalePayCard, date, 1, subjectOutOrder, 1);
        LP retailSumOrderRetail = addJProp(and1, sumWithDiscountObligationOrder, 1, is(orderRetail), 1);
        LP allOrderSalePayCash = addDUProp(cashRegGroup, "Наличных в кассе", addDUProp(addSGProp(retailSumOrderRetail, date, 1, subjectOutOrder, 1), addSGProp(retailSumOrderRetail, date, 1, subjectIncOrder, 1)), allOrderSalePayCard);

        LP allOrderSalePayCardCur = addJProp(cashRegGroup, "allOrderSalePayCardCur", "Безнал. в кассе", allOrderSalePayCard, currentDate, currentShop);
        LP allOrderSalePayCashCur = addJProp(cashRegGroup, "Наличных в кассе", allOrderSalePayCash, currentDate, currentShop);

        // сдача/доплата
        LP orderSalePayAll = addSUProp(Union.SUM, orderSalePayCard, orderSalePayCash);
        LP orderSaleDiffSum = addJProp(and1, addDUProp(orderSalePayAll, sumWithDiscountObligationOrder), 1, is(orderSaleRetail), 1);
        LP notEnoughSum = addJProp(negative, orderSaleDiffSum, 1);
        orderSaleToDo = addJProp(documentPayGroup, "Необходимо", and1, addIfElseUProp(addCProp(StringClass.get(5), "Итого", orderSaleRetail),
                addCProp(StringClass.get(5), "Сдача", orderSaleRetail), notEnoughSum, 1), 1, orderSaleDiffSum, 1);
        orderSaleToDoSum = addJProp(documentPayGroup, "Сумма необх.", abs, orderSaleDiffSum, 1);

        addConstraint(addJProp("Сумма наличными меньше сдачи", greater2, orderSalePayCard, 1, sumWithDiscountObligationOrder, 1), false);
        addConstraint(addJProp("Всё оплачено карточкой", and1, addJProp(equals2, orderSalePayCard, 1, sumWithDiscountObligationOrder, 1), 1, orderSalePayCash, 1), false);
        addConstraint(addJProp("Введенной суммы не достаточно", and1, notEnoughSum, 1, orderSalePayAll, 1), false); // если ни карточки ни кэша не задали, значит заплатитли без сдачи

        documentBarcodePrice = addJProp("Цена", priceAllOrderSaleArticle, 1, barcodeToObject, 2);
        documentBarcodePriceOv = addSUProp("Цена", Union.OVERRIDE, documentBarcodePrice, addJProp(and1, addJProp(obligationSum, barcodeToObject, 1), 2, is(order), 1));

        barcodeAddClient = addSDProp("Доб. клиента", LogicalClass.instance);
        barcodeAddClientAction = addJProp(true, "", andNot1, addBAProp(customerCheckRetail, barcodeAddClient), 1, barcodeToObject, 1);

        barcodeAddCert = addSDProp("Доб. серт.", LogicalClass.instance);
        barcodeAddCertAction = addJProp(true, "", andNot1, addBAProp(giftObligation, barcodeAddCert), 1, barcodeToObject, 1);

        barcodeAction2 = addJProp(true, "Ввод штрих-кода 2",
                addCUProp(
                        addSCProp(addIfElseUProp(articleQuantity, articleOrderQuantity, is(commitInc), 1)),
                        addIfElseUProp(orderSaleUseObligation, issueObligation, addJProp(diff2, 1, obligationIssued, 2), 1, 2),
                        addJProp(equals2, subjectIncOrder, 1, 2),
                        xorActionArticle, articleFormatToSell, NDS, documentRevalued,
                        addJProp(and1, changeUser, 2, is(baseClass), 1)
                ), 1, barcodeToObject, 2);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addJProp(and(false, false), changeUser, 2, is(baseClass), 1, is(baseClass), 3),
                        addSCProp(returnInnerQuantity)
                ), 1, barcodeToObject, 3, 2);

        LP xorCouponArticleGroup = addDProp(couponGroup, "xorCouponArticleGroup", "Вкл.", LogicalClass.instance, articleGroup);
        xorCouponArticle = addDProp(couponGroup, "xorCouponArticle", "Вкл./искл.", LogicalClass.instance, article);
        inCoupon = addXorUProp(couponGroup, "inCoupon", true, "Выд. купон", xorCouponArticle, addJProp(xorCouponArticleGroup, articleToGroup, 1));

        couponIssueSum = addDProp(couponGroup, "couponIssueSum", "Сумма купона", DoubleClass.instance, DoubleClass.instance);

        LP couponDocToIssueSum = addDCProp("couponDocToIssueSum", "Сумма купона к выд.", addIfProp(addMGProp(addJProp(and1, couponIssueSum, 3, addJProp(greater2, priceOrderArticle, 1, 2, 3), 1, 2, 3), 1, 2), false, inCoupon, 2), true, 1, 2, articleQuantity, 1, 2, commitSaleCheckArticleRetail); // здесь конечно хорошо было бы orderSaleDocPrice вытащить за скобки, но будет висячий ключ поэтому приходится пока немого извращаться

        couponToIssueQuantity = addDUProp("К выдаче", addSGProp(articleQuantity, 1, couponDocToIssueSum, 1, 2),
                addSGProp(addJProp(and1, addCProp(DoubleClass.instance, 1), issueCoupon, 1, 2), 1, obligationSum, 2));
        couponToIssueConstraint = addJProp("Кол-во выданных купонов не соответствует требуемому", diff2, couponToIssueQuantity, 1, 2, vzero);
        addConstraint(couponToIssueConstraint, false);

        orderUser = addDCProp("orderUser", "Исп-ль заказа", currentUser, true, is(order), 1);
        orderUserName = addJProp("Исп-ль заказа", name, orderUser, 1);
        // вспомогательные свойства
        orderContragentBarcode = addJProp("Штрих-код (кому)", barcode, subjectIncOrder, 1);
        orderUserBarcode = addJProp("Кассир", barcode, orderUser, 1);

        orderComputer = addDCProp("orderComputer", "Компьютер заказа", currentComputer, true, is(order), 1);
        orderComputerName = addJProp("Компьютер заказа", hostname, orderComputer, 1);


        setNotNull(addJProp("Штрих-код покупателя", and1, barcode, 1, is(customerCheckRetail), 1));
        //setNotNull(addJProp("Штрих-код товара", and1, barcode, 1, is(article), 1));
        setNotNull(addJProp("Штрих-код сертификата", and1, barcode, 1, is(obligation), 1));
        //setNotNull(addJProp(andNot1, barcode, 1, is(user), 1));

        checkRetailExported = addDProp("checkRetailExported", "Экспортирован", LogicalClass.instance, orderRetail);

        cashRegController = new CashRegController(this); // бред конечно создавать его здесь, но влом создавать getCashRegController()
        cashRegController.addCashRegProperties();

        LP importCustomerCheckRetail = addProp(baseGroup, new CustomerCheckRetailImportActionProperty(this, genSID()));

        quantityCheckCommitInnerArticle = addSDProp("quantityCheckCommitInnerArticle", "Кол-во свер.", DoubleClass.instance, commitInner, article);
        barcodeActionCheck = addJProp(true, "Ввод штрих-кода (проверки)",
                addCUProp(
                        addSCProp(addIfElseUProp(quantityCheckCommitInnerArticle, articleOrderQuantity, is(commitInner), 1))
                ), 1, barcodeToObject, 2);

        quantityDiffCommitArticle = addDUProp(articleOrderQuantity, addCUProp("Кол-во свер.", outerCommitedQuantity, quantityCheckCommitInnerArticle));

        // для импорта
        nameToCurrency = addCGProp(null, "nameToCurrency", "Валюта", object(currency), name, name, 1);
        nameToArticleGroup = addCGProp(null, "nameToArticleGroup", "Гр. тов.", object(articleGroup), name, name, 1);
        nameToUnitOfMeasure = addCGProp(null, "nameToUnitOfMeasure", "Ед. изм.", object(unitOfMeasure), name, name, 1);
        nameToBrend = addCGProp(null, "nameToBrend", "Бренд", object(brend), name, name, 1);
        nameToGender = addCGProp(null, "nameToGender", "Пол", object(gender), name, name, 1);

        dateLastImportShop = addDProp(cashRegGroup, "dateLastImportSh", "Дата прайса", DateClass.instance, shop);
        dateLastImport = addJProp(cashRegGroup, "dateLastImport", "Дата прайса", dateLastImportShop, currentShop);

        padlBarcodeToObject = addJProp(privateGroup, true, "Объект (до 12)", barcodeToObject, padl, 1);

        // для накладной
        seriesInvoiceDocument = addDProp(documentShipmentGroup, "seriesInvoiceDocument", "Серия", StringClass.get(4), shipmentDocument);
        numberInvoiceDocument = addDProp(documentShipmentGroup, "numberInvoiceDocument", "Номер", StringClass.get(15), shipmentDocument);
        invoiceDocumentNumber = addCGProp(privateGroup, false, "invoiceDocumentNumber", "Документ", object(shipmentDocument), numberInvoiceDocument, numberInvoiceDocument, 1);
        
        legalOutContract = addGDProp(baseGroup, "Contract", "legalOut", "Кого (ИД)", new ValueClass[]{supplier, storeLegalEntity}, new CustomClass[]{contractSupplier, contractSale});
        nameLegalOutContract = addJProp("nameLegalOutContract", "Кого", name, legalOutContract, 1);
        legalIncContract = addGDProp(baseGroup, "Contract", "legalInc", "С кем (ИД)", new ValueClass[]{storeLegalEntity, customerWhole}, new CustomClass[]{contractDelivery, contractCustomer});
        nameLegalIncContract = addJProp("nameLegalIncContract", "С кем", name, legalIncContract, 1);
        contractLegalEntityLegalEntity = addCGProp(privateGroup, true, "contractLegalEntityLegalEntity", "Договор (ИД)", object(contract), object(contract), legalOutContract, 1, legalIncContract, 1);

        LP contractOutIncOrder = addJProp(contractLegalEntityLegalEntity, legalEntityOutOrder, 1, legalEntityIncOrder, 1);
        LP contractIncOutOrder = addJProp(contractLegalEntityLegalEntity, legalEntityIncOrder, 1, legalEntityOutOrder, 1);
        LP contractOrder = addIfElseUProp(privateGroup, "contractOrder", "Договор (ИД)", contractOutIncOrder, contractIncOutOrder, is(orderDo), 1);
        LP nameContractOrder = addJProp("nameContractOrder", "Договор", name, contractOrder, 1);

        LP invoiceOrderRetail = addDProp("invoiceOrderRetail", "Счет-фактура", StringClass.get(50), orderSaleInvoiceArticleRetail);
        LP purposeOrderRetail = addDProp("purposeOrderRetail", "Цель приобретения", StringClass.get(50), orderSaleInvoiceArticleRetail);
        permissionOrder = addCUProp(documentInvoiceSaleGroup, "permissionOrder", "Основание отпуска", invoiceOrderRetail, nameContractOrder);
        purposeOrder = addCUProp(documentInvoiceSaleGroup, "purposeOrder", "Цель приобретения", purposeOrderRetail);

        LP[] propsInvoiceDocument = addDProp(documentShipmentOutGroup, "InvoiceDocument",
                        new String[]{"personPermission", "personOut", "personWarrant", "warrantBy", "personInc"},
                        new String[]{"Отпуск разрешил", "Отпуск произвел", "Кому выд. ТМЦ (по дов.)", "По доверенности выд.", "Товар получил"},
                        StringClass.getArray(50,60,60,60,70,60), shipmentDocumentOut);
        LP[] propsInvoiceTransportDocument = addDProp(documentShipmentTransportGroup, "InvoiceDocument",
                        new String[]{"personPRR", "typePRR", "codePRR", "timeOut", "timeInc", "timeDelay",
                                "transport", "transportList", "personTransport", "personDriver", "personRespTransport", "typeTransport", "route", "readdress", "trailer", "garageNumber"},
                        new String[]{"Исполнитель ПРР", "Способ ПРР", "Код ПРР", "Убытие", "Прибытие", "Простой",
                                "Автомобиль", "Путевой лист", "Владелец автотранспорта", "Водитель", "Экспедитор", "Вид перевозки", "Маршрут", "Переадресовка", "Прицеп", "Гаражный номер", ""},
                        StringClass.getArray(60,20,10,8,8,8, 20,10,60,60,60,20,20,50,30,15), shipmentDocumentOut);
    }

    LP padlBarcodeToObject;
    LP nameToCurrency;
    LP nameToArticleGroup;
    LP nameToUnitOfMeasure;
    LP nameToBrend;

    private LP initRequiredStorePrice(AbstractGroup group, String sID, boolean persistent, String caption, LP deliveryPriceStoreArticle, LP storeProp) {
        LP currentRRPPriceObjectArticle = addJProp(currentRRPPriceStoreArticle, storeProp, 1, 2);
        LP currentObjectDiscount = addJProp(currentStoreDiscount, storeProp, 1);

        LP addDeliveryObjectArticle = addJProp("Необх. цена с нац.", addPercent, addJProp(addPercent, deliveryPriceStoreArticle, 1, 2, addvArticle, 2), 1, 2, currentNDS, 2);
        LP currentPriceObjectArticle = addSUProp("Необх. цена", Union.MAX, addDeliveryObjectArticle, currentRRPPriceObjectArticle);
        LP actionPriceObjectArticle = addJProp("Акц. цена", removePercent, currentPriceObjectArticle, 1, 2, articleDiscount, 2);
        return addJProp(group, sID, persistent, caption, round1, addJProp(removePercent, actionPriceObjectArticle, 1, 2, currentObjectDiscount, 1), 1, 2);
    }

    LP barcodeActionCheck, quantityCheckCommitInnerArticle, quantityDiffCommitArticle;
    LP impSumCard;
    LP impSumCash;
    LP impSumBank;

    private LP addSupplierProperty(LP property) {
        return addSUProp(Union.SUM, property, addSGProp(property, articleStoreSupplier, 1, 2, 2));
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
    LP orderSaleCoupon;
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
    LP priceManfrOrderArticle;
    LP sumManfrOrderArticle;
    LP sumOrderArticle;
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

    protected void initTables() {
        tableFactory.include("article", article);
        tableFactory.include("orders", order);
        tableFactory.include("store", store);
        tableFactory.include("localsupplier", localSupplier);
        tableFactory.include("importsupplier", importSupplier);
        tableFactory.include("customerwhole", customerWhole);
        tableFactory.include("customerretail", customerCheckRetail);
        tableFactory.include("articlestore", article, store);
        tableFactory.include("articleorder", article, order);
        tableFactory.include("articleaction", article, action);
        tableFactory.include("rates", DateClass.instance);
        tableFactory.include("intervals", DoubleClass.instance);
        tableFactory.include("shoprates", DateClass.instance, shop);

        tableFactory.include("obligation", obligation);
        tableFactory.include("obligationorder", obligation, order);
    }

    protected void initIndexes() {
    }

    PanelExternalScreen panelScreen;

    @Override
    protected void initExternalScreens() {
        panelScreen = new PanelExternalScreen();
        addExternalScreen(panelScreen);
    }

    private Integer getPanelComPort(int compId) {
        try {
            DataSession session = createSession();

            Integer result = (Integer) panelScreenComPort.read(session, new DataObject(compId, computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getCashRegComPort(int compId) {
        try {
            DataSession session = createSession();

            Integer result = (Integer) cashRegComPort.read(session, new DataObject(compId, computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        if (panelScreen.getID() == screenID) {
            return new PanelExternalScreenParameters(getPanelComPort(computerId), getCashRegComPort(computerId));
        }
        return null;
    }

    private NavigatorElement saleRetailCashRegisterElement;
    private FormEntity commitSaleForm;
    private FormEntity saleCheckCertForm;
    private FormEntity cachRegManagementForm;
    private FormEntity returnSaleCheckRetailArticleForm;

    public CommitSaleCheckRetailFormEntity commitSaleBrowseForm;
    public SaleCheckCertFormEntity saleCheckCertBrowseForm;
    public ReturnSaleCheckRetailFormEntity returnSaleCheckRetailBrowse;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement print = new NavigatorElement(baseElement, "print", "Печатные формы");
        FormEntity incomePrice = addFormEntity(new IncomePriceFormEntity(print, "incomePrice"));
        FormEntity revalueAct = addFormEntity(new RevalueActFormEntity(print, "revalueAct"));
        FormEntity pricers = addFormEntity(new PricersFormEntity(print, "pricers"));
        FormEntity stickers = addFormEntity(new StickersFormEntity(print, "stickers"));
        FormEntity invoice = addFormEntity(new InvoiceFormEntity(print, "invoice", "Счет-фактура", true));
        FormEntity ttn1Blank = addFormEntity(new TTNFormEntity(print, "ttn1blank", "ТТН-1 бланк (гориз.)", false));
        FormEntity ttn1BlankV = addFormEntity(new TTNFormEntity(print, "ttn1blank_v", "ТТН-1 бланк", false));
        FormEntity ttn1Attach = addFormEntity(new TTNFormEntity(print, "ttn1attach", "ТТН-1 приложение (гориз.)", true));
        FormEntity ttn1AttachV = addFormEntity(new TTNFormEntity(print, "ttn1attach_v", "ТТН-1 приложение", true));
        FormEntity ttn1SideA = addFormEntity(new TTNFormEntity(print, "ttn1a", "ТТН-1 сторона A (гориз.)", true));
        FormEntity ttn1SideAV = addFormEntity(new TTNFormEntity(print, "ttn1a_v", "ТТН-1 сторона A", true));
        FormEntity ttn1SideB = addFormEntity(new TTNFormEntity(print, "ttn1b", "ТТН-1 сторона B (гориз.)", false));
        FormEntity ttn1SideBV = addFormEntity(new TTNFormEntity(print, "ttn1b_v", "ТТН-1 сторона B", false));
        FormEntity tn2Blank = addFormEntity(new TNFormEntity(print, "tn2blank", "ТН-2 бланк (гориз.)", false));
        FormEntity tn2BlankV = addFormEntity(new TNFormEntity(print, "tn2blank_v", "ТН-2 бланк", false));
        FormEntity tn2Attach = addFormEntity(new TNFormEntity(print, "tn2attach", "ТН-2 приложение (гориз.)", true));
        FormEntity tn2AttachV = addFormEntity(new TNFormEntity(print, "tn2attach_v", "ТН-2 приложение", true));
        FormEntity tn2 = addFormEntity(new TNFormEntity(print, "tn2", "ТН-2 (одн. стр.) (гориз.)", true));
        FormEntity tn2V = addFormEntity(new TNFormEntity(print, "tn2_v", "ТН-2 (одн. стр.)", true));

        addFormEntity(new ArticleReportFormEntity(print, "articleReport"));

        NavigatorElement classifier = new NavigatorElement(baseElement, "classifier", "Справочники");
            addFormEntity(new ArticleInfoFormEntity(classifier, "articleInfoForm"));
            addFormEntity(new StoreInfoFormEntity(classifier, "storeInfoForm"));

        NavigatorElement delivery = new NavigatorElement(baseElement, "delivery", "Управление закупками");
            addFormEntity(new SupplierArticleFormEntity(delivery, "supplierArticleForm"));
            NavigatorElement deliveryLocal = new NavigatorElement(delivery, "deliveryLocal", "Закупки у местных поставщиков");
                NavigatorElement deliveryShopLocal = new NavigatorElement(deliveryLocal, "deliveryShopLocal", "Закупки на магазин");
                    FormEntity deliveryCommitShopLocal = addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, true, "deliveryCommitShopLocal", 1));
                    deliveryCommitShopLocal.caption = "Ввод прихода";
                    FormEntity deliveryOrderShopLocal = addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, true, "deliveryOrderShopLocal", 0));
                    deliveryOrderShopLocal.caption = "Ввод заявки";
                    FormEntity deliveryShopLocalBrowse = addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, false, "deliveryShopLocalBrowse", 0));
                    deliveryShopLocalBrowse.caption = "Список документов";
                NavigatorElement deliveryWarehouseLocal = new NavigatorElement(deliveryLocal, "deliveryWarehouseLocal", "Закупки на распред. центр");
                    FormEntity deliveryWarehouseShopLocal = addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, true, "deliveryWarehouseShopLocal", 1));
                    deliveryWarehouseShopLocal.caption = "Ввод прихода";
                    FormEntity deliveryOrderWarehouseLocal = addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, true, "deliveryOrderWarehouseLocal", 0));
                    deliveryOrderWarehouseLocal.caption = "Ввод заявки";
                    FormEntity deliveryCommitWarehouseLocal = addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, false, "deliveryCommitWarehouseLocal", 0));
                    deliveryCommitWarehouseLocal.caption = "Список документов";
                NavigatorElement returnDeliveryLocal = new NavigatorElement(deliveryLocal, "returnDeliveryLocal", "Возвраты поставщику");
                    FormEntity returnCommitDeliveryLocal = addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, true, "returnCommitDeliveryLocal", 1));
                    returnCommitDeliveryLocal.caption = "Ввод отгрузки";
                    FormEntity returnOrderDeliveryLocal = addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, true, "returnOrderDeliveryLocal", 0));
                    returnOrderDeliveryLocal.caption = "Ввод заявки";
                    FormEntity returnDeliveryLocalBrowse = addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, false, "returnDeliveryLocalBrowse", 0));
                    returnDeliveryLocalBrowse.caption = "Список документов";
            NavigatorElement deliveryImport = new NavigatorElement(delivery, "deliveryImport", "Закупки у импортных поставщиков");
                FormEntity deliveryCommitImport = addFormEntity(new DeliveryImportFormEntity(deliveryImport, true, "deliveryCommitImport", 1));
                deliveryCommitImport.caption = "Ввод прихода";
                FormEntity deliveryOrderImport = addFormEntity(new DeliveryImportFormEntity(deliveryImport, true, "deliveryOrderImport", 0));
                deliveryOrderImport.caption = "Ввод заявки";
                FormEntity deliveryImportBrowse = addFormEntity(new DeliveryImportFormEntity(deliveryImport, false, "deliveryImportBrowse", 0));
                deliveryImportBrowse.caption = "Список документов";

        NavigatorElement sale = new NavigatorElement(baseElement, "sale", "Управление продажами");
            NavigatorElement saleRetailElement = new NavigatorElement(sale, "saleRetailElement", "Управление розничными продажами");
                saleRetailCashRegisterElement = new NavigatorElement(saleRetailElement, "saleRetailCashRegisterElement", "Касса");
                    commitSaleForm = addFormEntity(new CommitSaleCheckRetailFormEntity(saleRetailCashRegisterElement, "commitSaleForm", true, false));
                        addFormEntity(new CommitSaleCheckRetailFormEntity(commitSaleForm, "commitSaleCheckRetailForm", false, false));
                        commitSaleBrowseForm = addFormEntity(new CommitSaleCheckRetailFormEntity(commitSaleForm, "commitSaleBrowseForm", false, true));
                        addFormEntity(new CommitSaleCheckRetailExcelFormEntity(commitSaleForm, "commitSaleCheckRetailExcelForm", "Выгрузка в Excel"));
                    saleCheckCertForm = addFormEntity(new SaleCheckCertFormEntity(saleRetailCashRegisterElement, "saleCheckCertForm", true, false));
                        addFormEntity(new SaleCheckCertFormEntity(saleCheckCertForm, "saleCheckCertForm2", false, false));
                        saleCheckCertBrowseForm = addFormEntity(new SaleCheckCertFormEntity(saleCheckCertForm, "saleCheckCertBrowseForm", false, true));
                    returnSaleCheckRetailArticleForm = addFormEntity(new ReturnSaleCheckRetailFormEntity(saleRetailCashRegisterElement, true, "returnSaleCheckRetailArticleForm", false));
                        addFormEntity(new ReturnSaleCheckRetailFormEntity(returnSaleCheckRetailArticleForm, false, "returnSaleCheckRetailArticleForm2", false));
                        returnSaleCheckRetailBrowse = addFormEntity(new ReturnSaleCheckRetailFormEntity(returnSaleCheckRetailArticleForm, false, "returnSaleCheckRetailBrowse", true));
                    cachRegManagementForm = addFormEntity(cashRegController.createCashRegManagementFormEntity(saleRetailCashRegisterElement, "cachRegManagementForm"));
                    addFormEntity(new ShopMoneyFormEntity(saleRetailCashRegisterElement, "shopMoneyForm", "Данные из касс"));
                    addFormEntity(new ClientFormEntity(saleRetailCashRegisterElement, "clientForm", "Редактирование клиентов"));
                NavigatorElement saleRetailInvoice = new NavigatorElement(saleRetailElement, "saleRetailInvoice", "Безналичный расчет");
                    FormEntity saleRetailInvoiceForm = addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoice, "saleRetailInvoiceForm", true, false));
                        addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoiceForm, "orderSaleInvoiceRetailForm", false, false));
                    FormEntity saleInvoiceCert = addFormEntity(new SaleInvoiceCertFormEntity(saleRetailInvoice, "saleInvoiceCert", true, false));
                        addFormEntity(new SaleInvoiceCertFormEntity(saleInvoiceCert, "saleInvoiceCert2", false, false));
                    FormEntity returnSaleInvoiceRetailArticle = addFormEntity(new ReturnSaleInvoiceRetailFormEntity(saleRetailInvoice, true, "returnSaleInvoiceRetailArticle", false));
                        addFormEntity(new ReturnSaleInvoiceRetailFormEntity(returnSaleInvoiceRetailArticle, false, "returnSaleInvoiceRetailArticle2", false));
                        addFormEntity(new ReturnSaleInvoiceRetailFormEntity(returnSaleInvoiceRetailArticle, false, "returnSaleInvoiceRetailArticle3", true));
            NavigatorElement saleWhole = new NavigatorElement(sale, "saleWhole", "Управление оптовыми продажами");
                addFormEntity(new CustomerWholeFormEntity(saleWhole, "customerWholeForm"));
                FormEntity saleWholeForm = addFormEntity(new SaleWholeFormEntity(saleWhole, "saleWholeForm", true));
                    addFormEntity(new SaleWholeFormEntity(saleWholeForm, "saleWholeForm2", false));
                FormEntity returnSaleWholeArticle = addFormEntity(new ReturnSaleWholeFormEntity(saleWhole, "returnSaleWholeArticle", true));
                    addFormEntity(new ReturnSaleWholeFormEntity(returnSaleWholeArticle, "returnSaleWholeArticle2", false));

        NavigatorElement distribute = new NavigatorElement(baseElement, "distribute", "Управление распределением");
            addFormEntity(new StoreLegalEntityFormEntity(distribute, "storeLegalEntityForm"));
            FormEntity distributeShopForm = addFormEntity(new DistributeShopFormEntity(distribute, "distributeShopForm", true));
                FormEntity distributeShopBrowseForm = addFormEntity(new DistributeShopFormEntity(distributeShopForm, "distributeShopBrowseForm", false));
            FormEntity distributeWarehouseForm = addFormEntity(new DistributeWarehouseFormEntity(distribute, "distributeWarehouseForm", true));
                FormEntity distributeWarehouseBrowseForm = addFormEntity(new DistributeWarehouseFormEntity(distributeWarehouseForm, "distributeWarehouseBrowseForm", false));

        NavigatorElement price = new NavigatorElement(baseElement, "price", "Управление ценообразованием");
            FormEntity documentRevalue = addFormEntity(new DocumentRevalueFormEntity(price, true, "documentRevalue"));
                addFormEntity(new DocumentRevalueFormEntity(documentRevalue, false, "documentRevalue2"));

        NavigatorElement toSell = new NavigatorElement(baseElement, "toSell", "Управление ассортиментом");
            addFormEntity(new ArticleFormatFormEntity(toSell, "articleFormatForm", true));
            addFormEntity(new ArticleFormatFormEntity(toSell, "articleFormatForm2", false));

        NavigatorElement tax = new NavigatorElement(baseElement, "tax", "Управление налогами");
            FormEntity nds = addFormEntity(new DocumentNDSFormEntity(tax, true, "nds"));
                addFormEntity(new DocumentNDSFormEntity(nds, false, "nds2"));

        NavigatorElement actions = new NavigatorElement(baseElement, "actions", "Управление акциями");
            FormEntity saleAction = addFormEntity(new ActionFormEntity(actions, "saleAction"));
            FormEntity couponInterval = addFormEntity(new CouponIntervalFormEntity(actions, "couponInterval"));
            FormEntity couponArticle = addFormEntity(new CouponArticleFormEntity(actions, "couponArticle"));

        NavigatorElement balance = new NavigatorElement(baseElement, "balance", "Управление хранением");
            FormEntity balanceCheck = addFormEntity(new BalanceCheckFormEntity(balance, "balanceCheck", true));
                addFormEntity(new BalanceCheckFormEntity(balanceCheck, "balanceCheck2", false));

        NavigatorElement store = new NavigatorElement(baseElement, "store", "Сводная информация");
            addFormEntity(new StoreArticleFormEntity(store, "storeArticleForm"));

        addFormEntity(new GlobalFormEntity(baseElement, "globalForm"));
        FormEntity deliveryShopImport = addFormEntity(new DeliveryShopLocalFormEntity(baseElement, false, "deliveryShopImport", 0, true));
        deliveryShopImport.caption = "Импорт";

//        FormEntity logClient = addFormEntity(new LogClientFormEntity(actions, "logClientForm"));

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
            addPropertyDraw(publicGroup, true);

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

            addPropertyDraw(barcodeClient, objectValue);
            addPropertyDraw(nameClient, objectValue);

            ObjectEntity objClient = addSingleGroupObject(customerCheckRetail, "Клиент");
            ObjectEntity objDoc = addSingleGroupObject(commitSaleCheckArticleRetail, "Чек");

            addPropertyDraw(objClient, barcode, name, propsCustomerCheckRetail, clientSum);
            addPropertyDraw(objDoc, objectValue, date, orderHour, subjectOutOrder, sumWithDiscountObligationOrder);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(barcode, objClient), Compare.EQUALS, addPropertyObject(orderContragentBarcode, objDoc)));

            //addFixedFilter(new NotNullFilterEntity(addPropertyObject(barcode, objClient)));

            addFixedFilter(new OrFilterEntity(
                    new CompareFilterEntity(addPropertyObject(barcode, objClient), Compare.EQUALS, barcodeClient),
                    new CompareFilterEntity(addPropertyObject(name, objClient), Compare.EQUALS, nameClient)));
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

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseGroup, true);
            objBarcode.groupTo.initClassView = ClassViewType.PANEL;
            objBarcode.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            objBarcode.resetOnApply = true;

            addPropertyDraw(reverseBarcode);

//            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(objectValue, objBarcode));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(reverseBarcode)));
            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;
            design.addIntersection(barcodeView, design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(barcodeView, design.get(getPropertyDraw(reverseBarcode)), DoNotIntersectSimplexConstraint.TOTHE_BOTTOM);
            design.addIntersection(design.get(getPropertyDraw(reverseBarcode)), design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            if (getPropertyDraw(documentBarcodePriceOv) != null) {
                design.addIntersection(design.get(getPropertyDraw(barcodeObjectName)), design.get(getPropertyDraw(documentBarcodePriceOv)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.setFont(barcodeView, FONT_LARGE_BOLD);
            design.setFont(reverseBarcode, FONT_SMALL_BOLD);
            design.setFont(barcodeAddClient, FONT_SMALL_BOLD);
            design.setFont(barcodeAddCert, FONT_SMALL_BOLD);
            design.setFont(barcodeObjectName, FONT_LARGE_BOLD);
            design.setFont(documentBarcodePriceOv, FONT_LARGE_BOLD);
            design.setBackground(barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            design.setFocusable(reverseBarcode, false);
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
            return new Object[]{baseGroup, true, documentGroup, true};
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
                    addObjectActions(this, objDoc);
            }

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction2, objDoc, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeNotFoundMessage, objBarcode));

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

                // так конечно делать неправильно, но DocumentFormEntity - это первый общий класс у продажи сертификатов и кассы
                PropertyDrawEntity payView = getPropertyDraw(sumWithDiscountOrder);

                if (payView != null) {
                    // делаем, чтобы суммы были внизу и как можно правее
                    ContainerView docSumsContainer = design.get(payView).getContainer();
                    docSumsContainer.getContainer().setSID(null); // сделано, чтобы при визуальной настройке она сразу не перетаскивала все свойства в другой контейнер при помощи ContainerMover
                    design.mainContainer.addBack(2, docSumsContainer);

                    PropertyDrawEntity payCash = getPropertyDraw(orderSalePayCash);
                    PropertyDrawEntity payCard = getPropertyDraw(orderSalePayCard);
                    PropertyDrawEntity toDo = getPropertyDraw(orderSaleToDo);
                    PropertyDrawEntity toDoSum = getPropertyDraw(orderSaleToDoSum);

                    if (payCash != null || payCard != null || toDo != null || toDoSum != null) {

                        ContainerView payContainer = design.createContainer("Платежные средства");
                        design.mainContainer.addBack(2, payContainer);
                        payContainer.constraints.directions = new SimplexComponentDirections(0.1, -0.1, 0, 0.1);

                        if (payCash != null) payContainer.add(design.get(payCash));
                        if (payCard != null) payContainer.add(design.get(payCard));
                        if (toDo != null) {
                            payContainer.add(design.get(toDo));
                            design.get(toDo).design.background = Color.yellow;
                        }
                        if (toDoSum != null) {
                            payContainer.add(design.get(toDoSum));
                            design.get(toDoSum).design.background = Color.yellow;
                        }

                        design.addIntersection(docSumsContainer, payContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                    }
                }

                if (hasExternalScreen()) {
                    PropertyDrawEntity barcodeNavigator = getPropertyDraw(barcodeObjectName);
                    if (barcodeNavigator != null) {
                        design.get(barcodeNavigator).externalScreen = panelScreen;
                        design.get(barcodeNavigator).externalScreenConstraints.order = 1;
                    }

                    PropertyDrawEntity orderSaleNavigator = getPropertyDraw(orderSaleToDo);
                    if (orderSaleNavigator != null) {
                        design.get(orderSaleNavigator).externalScreen = panelScreen;
                        design.get(orderSaleNavigator).externalScreenConstraints.order = 3;
                    }

                    PropertyDrawEntity orderSaleSumNavigator = getPropertyDraw(orderSaleToDoSum);
                    if (orderSaleSumNavigator != null) {
                        design.get(orderSaleSumNavigator).externalScreen = panelScreen;
                        design.get(orderSaleSumNavigator).externalScreenConstraints.order = 4;
                    }

                    PropertyDrawEntity priceNavigator = getPropertyDraw(documentBarcodePriceOv);
                    if (priceNavigator != null) {
                        design.get(priceNavigator).externalScreen = panelScreen;
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

            PropertyDrawView objectValueView = design.get(getPropertyDraw(objectValue, objDoc));
            if(objectValueView!=null)
                objectValueView.caption = "Код";
            PropertyDrawView objectClassNameView = design.get(getPropertyDraw(objectClassName, objDoc));
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
            return new Object[]{baseGroup, true};
        }

        protected Object[] getDocumentArticleProps() {
            return new Object[]{baseGroup, true, documentGroup, true};
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
                addCheckFilter(articleFilterGroup, true);

            addPropertyDraw(objArt, nameCountryArticle, nameUnitOfMeasureArticle);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);

            fillExtraCheckFilters(filterGroup, toAdd);
        }

        @Override
        protected Object[] getDocumentProps() {
            return BaseUtils.add(super.getDocumentProps(), importOrder);
        }

        @Override
        protected Object[] getArticleProps() {
            return new Object[]{name, barcode, currentRRP, nameCurrencyArticle};
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
                addSingleGroupObject(store, name, importDocs);
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
                objOuter = addSingleGroupObject(commitClass, baseGroup, true);
                addPropertyDraw(objOuter, objDoc, baseGroup, true, documentGroup, true);
                addPropertyDraw(objOuter, objDoc, objArt, baseGroup, true, documentGroup, true);
                addPropertyDraw(objOuter, objArt, baseGroup, true, priceAllOrderDeliveryArticle);

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
                    filterGroup.addFilter(new RegularFilterEntity(genID(),
                            documentFreeFilter,
                            "Дост. кол-во",
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), toAdd && !filled);
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

    public class ShopMoneyFormEntity extends FormEntity {
        public ShopMoneyFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objShop = addSingleGroupObject(shop, baseGroup);

            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            ObjectEntity objDateFrom = new ObjectEntity(genID(), DateClass.instance, "Выгрузить от");
            ObjectEntity objDateTo = new ObjectEntity(genID(), DateClass.instance, "Выгрузить до");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);
            addGroup(gobjDates);

            gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjDates.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objDateFrom, objectValue);
            addPropertyDraw(objDateTo, objectValue);
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
                addPropertyDraw(date, objDoc);

            if (allStores) {
                addPropertyDraw(objDoc, subjectOutOrder, nameSubjectOutOrder);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectOutOrder, objDoc), Compare.EQUALS, shopImplement));
            }

            // чтобы в порядке нужном
            addPropertyDraw(changeQuantityOrder, objDoc, objArt);
            addPropertyDraw(barcode, objArt);

            objArt.groupTo.filterProperty = addPropertyDraw(name, objArt);

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
                                     addPropertyObject(seekBarcodeAction, objBarcode),
                                     addPropertyObject(barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.setEnabled(orderClientSum, false);

            design.get(objDoc.groupTo).grid.constraints.fillVertical = 2;

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);

            design.setEnabled(publicGroup, false, objArt.groupTo);
            design.setEnabled(articleQuantity, true, objArt.groupTo);

            design.get(objArt.groupTo).grid.defaultComponent = true;

            ObjectView objArtView = design.get(objArt);
            objArtView.classChooser.show = false;

            if(isSubjectImpl()) {
                design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddClient)));
                design.addIntersection(design.get(getPropertyDraw(barcodeAddClient)), design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }
            design.setEditKey(barcodeAddClient, KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK));

            design.get(getPropertyDraw(changeQuantityOrder, objArt)).minimumSize = new Dimension(20, 20);
            design.get(getPropertyDraw(articleQuantity, objArt)).minimumSize = new Dimension(20, 20);
            design.get(getPropertyDraw(documentFreeQuantity, objArt)).minimumSize = new Dimension(90, 20);
            design.get(getPropertyDraw(discountOrderArticle, objArt)).minimumSize = new Dimension(20, 20);
            getPropertyDraw(discountSumOrderArticle, objArt).forceViewType = ClassViewType.HIDE;
            design.get(getPropertyDraw(barcode, objArt)).minimumSize = new Dimension(200, 100);

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
            addPropertyDraw(barcode, objObligation);
            addPropertyDraw(objectClassName, objObligation);
            addPropertyDraw(obligationSum, objObligation);
            addPropertyDraw(obligationSumFrom, objObligation);
            addPropertyDraw(couponFromIssued, objObligation).forceViewType = ClassViewType.GRID;
            addPropertyDraw(obligationToIssued, objObligation);
            addPropertyDraw(orderSaleUseObligation, objDoc, objObligation);

            if (toAdd) {
                objArt.groupTo.propertyHighlight = addPropertyObject(articleSaleAction, objArt);

                objObligation.groupTo.propertyHighlight = addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation);
                addHintsNoUpdate(obligationDocument);
            }

            objObligation.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleUseObligation, objDoc, objObligation)));

            objCoupon = addSingleGroupObject(coupon, "Выдано купонов", baseGroup, true);
            addPropertyDraw(objDoc, objCoupon, baseGroup, true, issueObligation);
            objCoupon.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(issueObligation, objDoc, objCoupon)));

            if (toAdd) {
                objIssue = addSingleGroupObject(DoubleClass.instance, "Выдать купоны", baseGroup, true);
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

            design.get(getPropertyDraw(objectClassName, objObligation)).maximumSize = new Dimension(50, 50);

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

                return cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSet(art.groupTo),
                        getPropertyDraw(priceAllOrderSaleArticle, objArt), getPropertyDraw(articleQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(sumWithDiscountOrderArticle, objArt),
                        getPropertyDraw(sumWithDiscountObligationOrder, objDoc), getPropertyDraw(barcode, objArt),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc),
                        getPropertyDraw(discountOrderArticle), getPropertyDraw(discountSumOrderArticle), getPropertyDraw(orderUserName),
                        getPropertyDraw(nameImplSubjectIncOrder), getPropertyDraw(accumulatedClientSum), getPropertyDraw(discountSumOrder, objDoc),
                        BaseUtils.toSet(obligation.groupTo),getPropertyDraw(objectClassName, objObligation), getPropertyDraw(obligationSum, objObligation), getPropertyDraw(barcode, objObligation));
            } else
                return super.getClientApply(formInstance);
        }

        public ClientAction getPrintOrderAction(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);

                return cashRegController.getPrintOrderAction(formInstance,
                        BaseUtils.toSet(art.groupTo),
                        getPropertyDraw(priceAllOrderSaleArticle, objArt), getPropertyDraw(articleQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(sumWithDiscountOrderArticle, objArt),
                        getPropertyDraw(sumWithDiscountObligationOrder, objDoc), getPropertyDraw(barcode, objArt));
            } else
                return new ListClientAction(new ArrayList<ClientAction>());
        }

        @Override
        public String checkClientApply(Object result) {

            String check = cashRegController.checkCashRegApplyActions(result);
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

            addPropertyDraw(dateFrom, objectValue);
            addPropertyDraw(dateTo, objectValue);

            GroupObjectEntity gobjDocArt = new GroupObjectEntity(genID());

            ObjectEntity objDoc = new ObjectEntity(genID(), commitSaleCheckArticleRetail, "Чек");
            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");

            gobjDocArt.add(objDoc);
            gobjDocArt.add(objArt);
            addGroup(gobjDocArt);

            addPropertyDraw(objArt, barcode, name);
            addPropertyDraw(objDoc, objArt, priceAllOrderSaleArticle, articleQuantity);
            addPropertyDraw(objDoc, objectValue, date, orderHour, orderUserBarcode, orderComputerName, subjectOutOrder, orderContragentBarcode);
            addPropertyDraw(objDoc, objArt, discountSumOrderArticle, discountOrderArticle, sumWithDiscountOrderArticle, sumWithDiscountObligationOrderArticle);


            removePropertyDraw(documentMoveGroup); // нужно, чтобы убрать Доступ. кол-во, которое не может нормально выполнить PostgreSQL

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleQuantity, objDoc, objArt)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objDoc), Compare.GREATER_EQUALS, dateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objDoc), Compare.LESS_EQUALS, dateTo));
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

            addPropertyDraw(objDoc, objectClassName);
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
                                         addPropertyObject(seekBarcodeAction, objBarcode),
                                         addPropertyObject(barcodeNotFoundMessage, objBarcode));
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

            objInner = addSingleGroupObject(commitClass, getReturnCaption(), baseGroup, true);

            addPropertyDraw(objInner, objDoc, baseGroup, true, documentGroup, true);

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
            addPropertyDraw(barcode, objArt);
            addPropertyDraw(name, objArt);
            addPropertyDraw(objInner, objDoc, objArt, baseGroup, true, documentGroup, true);
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
                objOuter = addSingleGroupObject(commitDelivery, baseGroup, true);

                addPropertyDraw(objInner, objOuter, objDoc, baseGroup, true, documentGroup, true);
                addPropertyDraw(objInner, objOuter, objDoc, objArt, baseGroup, true, documentGroup, true);
                addPropertyDraw(objInner, objOuter, baseGroup, true);
                addPropertyDraw(objInner, objOuter, objArt, baseGroup, true);

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
                                     addPropertyObject(seekBarcodeAction, objBarcode),
                                     addPropertyObject(barcodeNotFoundMessage, objBarcode));
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
            return new Object[]{baseGroup, true, documentGroup, true, sumWithDiscountOrder};
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
                addPropertyDraw(date, objDoc);
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
            design.get(getPropertyDraw(objectValue, objInner)).caption = "Номер чека";
            design.getGroupObjectContainer(objArt.groupTo).title = "Товарные позиции";

            PropertyDrawEntity barcodeNavigator = getPropertyDraw(barcodeObjectName);
            if (barcodeNavigator != null) {
                design.get(barcodeNavigator).externalScreen = panelScreen;
                design.get(barcodeNavigator).externalScreenConstraints.order = 1;
            }

            PropertyDrawEntity priceNavigator = getPropertyDraw(documentBarcodePriceOv);
            if (priceNavigator != null) {
                design.get(priceNavigator).externalScreen = panelScreen;
                design.get(priceNavigator).externalScreenConstraints.order = 2;
            }

            PropertyDrawEntity returnSaleSumNavigator = getPropertyDraw(sumWithDiscountOrder);
            if (returnSaleSumNavigator != null) {
                design.get(returnSaleSumNavigator).externalScreen = panelScreen;
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

                return cashRegController.getCashRegApplyActions(formInstance, 2,
                        BaseUtils.toSet(inner.groupTo, art.groupTo),
                        getPropertyDraw(priceAllOrderSaleArticle, objArt), getPropertyDraw(returnInnerQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(sumWithDiscountOrderArticle, objArt),
                        getPropertyDraw(sumWithDiscountOrder, objDoc), getPropertyDraw(barcode, objArt), null, null,
                        getPropertyDraw(discountOrderArticle), getPropertyDraw(discountSumOrderArticle),
                        getPropertyDraw(orderUserName), null, null, null, null, null, null, null);
            } else
                return super.getClientApply(formInstance);
        }

        @Override
        public String checkClientApply(Object result) {

            String check = cashRegController.checkCashRegApplyActions(result);
            if (check != null) return check;

            return super.checkClientApply(result);
        }
    }

    private class SupplierArticleFormEntity extends FormEntity {
        protected SupplierArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Ассортимент поставщиков");

            ObjectEntity objSupplier = addSingleGroupObject(supplier, publicGroup, true);
            addObjectActions(this, objSupplier);

            ObjectEntity objContractSupplier = addSingleGroupObject(contractSupplier, name, date, nameLegalIncContract);
            addObjectActions(this, objContractSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(legalOutContract, objContractSupplier), Compare.EQUALS, objSupplier));

            ObjectEntity objArt = addSingleGroupObject(article, baseGroup, true, priceGroup, logisticsGroup);
            addObjectActions(this, objArt);

            addPropertyDraw(objSupplier, objArt, publicGroup, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSupplier, objArt), Compare.EQUALS, objSupplier));
        }
    }

    private class StoreLegalEntityFormEntity extends FormEntity {
        protected StoreLegalEntityFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Юридические лица");

            ObjectEntity objSupplier = addSingleGroupObject(storeLegalEntity, publicGroup, true);
            addObjectActions(this, objSupplier);

            ObjectEntity objContractSale = addSingleGroupObject(contractSale, name, date, nameLegalIncContract);
            addObjectActions(this, objContractSale);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(legalOutContract, objContractSale), Compare.EQUALS, objSupplier));
        }
    }

    private class CustomerWholeFormEntity extends FormEntity {
        protected CustomerWholeFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Оптовые покупатели");

            ObjectEntity objCustomerWhole = addSingleGroupObject(customerWhole, publicGroup, true);
            addObjectActions(this, objCustomerWhole);

            ObjectEntity objContractSale = addSingleGroupObject(contractCustomer, name, date, nameLegalOutContract);
            addObjectActions(this, objContractSale);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(legalIncContract, objContractSale), Compare.EQUALS, objCustomerWhole));
        }
    }

    private class StoreArticleFormEntity extends BarcodeFormEntity {
        private ObjectEntity objArt;

        protected StoreArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Остатки по складу");

            ObjectEntity objStore = addSingleGroupObject(store, publicGroup, true);
            objArt = addSingleGroupObject(article, publicGroup, true);
            ObjectEntity objOuter = addSingleGroupObject(commitDelivery, publicGroup, true);

            addPropertyDraw(objStore, objArt, publicGroup, true);
            addPropertyDraw(objStore, objOuter, publicGroup, true);
            addPropertyDraw(objOuter, objArt, baseGroup, true);
            addPropertyDraw(objStore, objOuter, objArt, publicGroup, true);
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

            addObjectActions(this, objArt);
            addPropertyDraw(objFormat, publicGroup, true);
            addPropertyDraw(objArt, baseGroup, true, currentRRP);
            addPropertyDraw(objFormat, objArt, publicGroup, true);

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
            return new Object[]{baseGroup, true, currentNDS};
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

            ObjectEntity objDoc = addSingleGroupObject(commitDoShopInc, "Документ", date, nameSubjectIncOrder, nameSubjectOutOrder, nameLegalEntityIncOrder, nameLegalEntityOutOrder, numberInvoiceDocument, seriesInvoiceDocument, sumNDSOrder, sumWithDiscountOrder, sumNoNDSOrder, sumNDSRetailOrder, sumAddvOrder, sumRetailOrder, sumAddManfrOrder, sumManfrOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, name, objectValue, barcode, nameUnitOfMeasureArticle);

            addPropertyDraw(objDoc, objArt, articleQuantity, priceOrderArticle, addvOrderArticle, ndsShopOrderPriceArticle, shopPrice, sumRetailOrderArticle, priceManfrOrderArticle, addManfrOrderArticle, ndsOrderArticle);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(shopPrice)));

            addFAProp(documentPriceGroup, "Реестр цен", this, objDoc);
        }
    }

    private class RevalueActFormEntity extends FormEntity {

        protected RevalueActFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Акт переоценки", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", date, sumRevalBalance, sumPrevRetailOrder, sumNewPrevRetailOrder, sumPriceChangeOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, name, objectValue, barcode);

            addPropertyDraw(objDoc, objArt, revalChangeBalance, prevPrice, sumPrevRetailOrderArticle, shopPrice, sumNewPrevRetailOrderArticle, sumPriceChangeOrderArticle);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(revalChangeBalance, objDoc, objArt)));

            addFAProp(documentPriceGroup, "Акт переоценки", this, objDoc);
        }
    }

    // накладные
    private abstract class PrintSaleFormEntity extends FormEntity {

        ObjectEntity objDoc;
        ObjectEntity objArt;

        protected PrintSaleFormEntity(NavigatorElement parent, String sID, String caption, boolean inclArticle) {
            super(parent, sID, caption, true);

            objDoc = addSingleGroupObject(getDocClass(), "Документ", date, nameSubjectIncOrder, nameSubjectOutOrder, nameLegalEntityIncOrder, nameLegalEntityOutOrder, addressSubjectIncOrder, addressSubjectOutOrder, sumWithDiscountOrder, sumNDSOrder, sumNoNDSOrder, propsLegalEntityIncOrder, propsLegalEntityOutOrder, quantityOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objDoc, getDocGroups());

            if(inclArticle) {
                objArt = addSingleGroupObject(article, name, nameUnitOfMeasureArticle);
                addPropertyDraw(objDoc, objArt, articleQuantity, priceOrderArticle, sumWithDiscountOrderArticle, ndsOrderArticle, sumNDSOrderArticle, sumNoNDSOrderArticle, priceManfrOrderArticle, addManfrOrderArticle);
                addFixedFilter(new NotNullFilterEntity(getPropertyObject(articleQuantity)));
            }

            addMFAProp(getActionGroup(), caption, this, new ObjectEntity[] {objDoc});
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

            ObjectEntity objAction = addSingleGroupObject(action, publicGroup, true);
            addObjectActions(this, objAction);

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, publicGroup, true);
            ObjectEntity objArt = addSingleGroupObject(article, baseGroup, true, priceGroup);

            if (noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            addPropertyDraw(objAction, objArtGroup, publicGroup, true);
            addPropertyDraw(objAction, objArt, publicGroup, true);

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
            addActionsOnObjectChange(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));
        }
    }

    private class PricersFormEntity extends FormEntity {

        protected PricersFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Ценники", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, baseGroup, true);

            addPropertyDraw(objDoc, objArt, shopPrice);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(shopPrice)));
            addFixedFilter(new NotFilterEntity(new CompareFilterEntity(getPropertyObject(shopPrice), Compare.EQUALS, addPropertyObject(prevPrice, objDoc, objArt))));

            addFAProp(documentPriceGroup, "Ценники", this, objDoc);
        }
    }

        private class StickersFormEntity extends FormEntity {

        protected StickersFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Стикеры", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, baseGroup, true);

            addFAProp(documentPriceGroup, "Стикеры", this, objDoc);
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
                addPropertyDraw(date, objDoc);

            if (allStores) {
                addPropertyDraw(objDoc, subjectOutOrder, nameSubjectOutOrder);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(subjectOutOrder, objDoc), Compare.EQUALS, shopImplement));
            }

            objObligation = addSingleGroupObject(giftObligation);
            addPropertyDraw(barcode, objObligation);
            addPropertyDraw(name, objObligation);
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
                                     addPropertyObject(seekBarcodeAction, objBarcode),
                                     addPropertyObject(barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);
            design.setEnabled(publicGroup, false, objObligation.groupTo);
            design.setEnabled(obligationSum, true);

            design.get(objObligation.groupTo).grid.defaultComponent = true;

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddClient)));
            design.addIntersection(design.get(getPropertyDraw(barcodeAddClient)), design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
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

                return cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSet(obligation.groupTo),
                        getPropertyDraw(obligationSum, objObligation), getPropertyDraw(issueObligation, objObligation),
                        getPropertyDraw(name, objObligation), getPropertyDraw(obligationSum, objObligation),
                        getPropertyDraw(sumWithDiscountOrder, objDoc), getPropertyDraw(barcode, objObligation),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc), null, null,
                        getPropertyDraw(orderUserName), getPropertyDraw(nameImplSubjectIncOrder), getPropertyDraw(accumulatedClientSum),
                        null, null, null, null, null);

            } else
                return super.getClientApply(formInstance);
        }

        @Override
        public String checkClientApply(Object result) {

            String check = cashRegController.checkCashRegApplyActions(result);
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

            ObjectEntity objIntervalAdd = addSingleGroupObject(DoubleClass.instance, "Цена товара от", objectValue, couponGroup, true);
            objIntervalAdd.groupTo.initClassView = ClassViewType.PANEL;
            objIntervalAdd.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            ObjectEntity objInterval = addSingleGroupObject(DoubleClass.instance, "Цена товара", objectValue, couponGroup, true);
            objInterval.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(couponIssueSum, objInterval)));
        }
    }

    private class CouponArticleFormEntity extends FormEntity {
        private ObjectEntity objArt;

        protected CouponArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Товары по купонам");

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, baseGroup, true, couponGroup, true);
            objArt = addSingleGroupObject(article, baseGroup, true, couponGroup, true);

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

    private class ArticleInfoFormEntity extends FormEntity {
        public final ObjectEntity objArt;

        protected ArticleInfoFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Справочник товаров");

            objArt = addSingleGroupObject(article, objectValue, name, barcode, currentRRP, nameCurrencyArticle, nameArticleGroupArticle, fullNameArticle, nameUnitOfMeasureArticle, nameBrendArticle, nameCountryArticle, gigienaArticle, spirtArticle, statusArticle, weightArticle, coeffTransArticle);
            addPropertyDraw(importArticlesRRP);
            addPropertyDraw(importArticlesInfo);
        }
    }

    private class StoreInfoFormEntity extends FormEntity {
        public final ObjectEntity objStore;

        protected StoreInfoFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Справочник складов");

            objStore = addSingleGroupObject(store, publicGroup, true, importDocs);
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
            addPropertyDraw(objDateFrom, objectValue);
            addPropertyDraw(objDateTo, objectValue);
            
            gobjDates.initClassView = ClassViewType.PANEL;

            objShop = addSingleGroupObject(shop, name);
            objShop.groupTo.initClassView = ClassViewType.PANEL;
            
            objOrderInc = addSingleGroupObject(orderInc, nameSubjectOutOrder, date, numberInvoiceDocument, seriesInvoiceDocument, sumRetailOrder, sumWithDiscountCouponOrder, sumDiscountPayCouponOrder, sumPriceChangeOrder);

            objOrderOut = addSingleGroupObject(orderOut, nameSubjectIncOrder, date, numberInvoiceDocument, seriesInvoiceDocument, sumRetailOrder, sumWithDiscountCouponOrder, sumDiscountPayCouponOrder, sumPriceChangeOrder);
                        
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objOrderInc), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objOrderInc), Compare.LESS_EQUALS, objDateTo));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objOrderOut), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objOrderOut), Compare.LESS_EQUALS, objDateTo));

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

/*    private class LogClientFormEntity extends FormEntity {
        protected LogClientFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Изменения клиентов");

            ObjectEntity objClient = addSingleGroupObject(customerCheckRetail, "Клиент", properties, baseGroup, true);
            ObjectEntity objSession = addSingleGroupObject(session, "Транзакция", properties, baseGroup, true);
            PropertyDrawEntity drawEntity = addPropertyDraw(logClientInitialSum, objClient, objSession);
            addFixedFilter(new NotNullFilterEntity(drawEntity.propertyObject));
        }
    }*/

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        SecurityPolicy permitCachRegister = addPolicy("Разрешить только кассовые формы", "Политика разрешает открытие только форма для работы за кассой.");
        permitCachRegister.navigator.defaultPermission = false;
        permitCachRegister.navigator.permit(commitSaleForm);
        permitCachRegister.navigator.permit(saleCheckCertForm);
        permitCachRegister.navigator.permit(returnSaleCheckRetailArticleForm);
        permitCachRegister.navigator.permit(cachRegManagementForm);

        User admin = addUser("admin", "fusion");
        //админ игнорит настройки в базе, ему разрешено всё
        admin.addSecurityPolicy(permitAllPolicy);
    }

    private class PayWithCardActionProperty extends ActionProperty {

        private PayWithCardActionProperty() {
            super(genSID(), "Опл. карт.", new ValueClass[]{orderSaleRetail});

            askConfirm = true;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            //To change body of implemented methods use File | Settings | File Templates.
            FormInstance<?> remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            DataObject document = BaseUtils.singleValue(keys);
            if(orderSalePayCash.read(session, document)==null && orderSalePayCard.read(session, document)==null) {
                orderSalePayCash.execute(null, session, remoteForm, document);
                orderSalePayCard.execute(sumWithDiscountObligationOrder.read(session.sql, remoteForm, session.env, document), session, remoteForm, document);

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
            super(genSID(), "Печать", new ValueClass[]{orderSaleRetail});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
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
            super(genSID(), "Экспорт реализации", new ValueClass[]{shop, DateClass.instance, DateClass.instance});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            shopInterface = i.next();
            dateFrom = i.next();
            dateTo = i.next();
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            Integer shopID = (Integer) keys.get(shopInterface).object;
            try {
                new AbstractSaleExportTask(VEDBusinessLogics.this, ((SaleExportTask) scheduler.getTask("saleExport")).getPath(shopID), shopID) {
                    protected String getDbfName() {
                        return "datadat.dbf";
                    }

                    protected void setRemoteFormFilter(FormInstance formInstance) throws ParseException {
                        PropertyDrawInstance<?> dateDraw = formInstance.getPropertyDraw(date);
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

        protected void addImportObjectNameField(List<ImportField> fields, List<ImportProperty<?>> properties, List<ImportKey<?>> keys, ImportKey importKey, LP nameToObject, LP importProp, ConcreteCustomClass customClass) {
            addImportObjectCGField(fields, properties, keys, nameToObject, name, importKey, importProp, customClass);
        }
        
        protected ImportKey addImportObjectCGField(List<ImportField> fields, List<ImportProperty<?>> properties, List<ImportKey<?>> keys, LP IDToObject, LP objectToID, ImportKey linkKey, LP linkProp, ConcreteCustomClass customClass) {
            ImportField importField = new ImportField(objectToID); fields.add(importField);
            ImportKey<?> importObjectKey = new ImportKey(customClass, IDToObject.getMapping(importField)); keys.add(importObjectKey);
            properties.add(new ImportProperty(importField, linkProp.getMapping(linkKey), object(customClass).getMapping(importObjectKey)));
            properties.add(new ImportProperty(importField, objectToID.getMapping(importObjectKey)));
            return importObjectKey;
        }

        protected void addImportField(List<ImportField> fields, List<ImportProperty<?>> properties, ImportKey importKey, LP importProp) {
            addImportField(fields, properties, importProp, importKey);
        }

        protected void addImportField(List<ImportField> fields, List<ImportProperty<?>> properties, LP importProp, ImportKeyInterface... importKeys) {
            ImportField importField = new ImportField(importProp); fields.add(importField);
            properties.add(new ImportProperty(importField, importProp.getMapping(importKeys)));
        }

        protected void addImportConvertField(List<ImportField> fields, List<ImportProperty<?>> properties, ImportKey articleKey, LP articleProp, LP converter, DataClass fieldClass) {
            ImportField nameField = new ImportField(fieldClass); fields.add(nameField);
            properties.add(new ImportProperty(nameField, articleProp.getMapping(articleKey), converter.getMapping(nameField)));
        }

    }

    public class ImportOrderActionProperty extends ImportActionProperty {

        private final ClassPropertyInterface documentInterface;

        public ImportOrderActionProperty() {
            super(genSID(), "Импортировать заявку", new ValueClass[]{orderDelivery});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            documentInterface = i.next();
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            DataObject document = keys.get(documentInterface);
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xsl файл.");
                return;
            }

            List<ImportField> fields = new ArrayList<ImportField>();
            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            ImportField barcodeField = new ImportField(StringClass.get(13)); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, barcode.getMapping(articleKey), padl.getMapping(barcodeField)));

            addImportField(fields, properties, articleKey, name);
            addImportField(fields, properties, priceOrderArticle, document, articleKey);
            addImportField(fields, properties, articleOrderQuantity, document, articleKey);
            addImportField(fields, properties, ndsOrderArticle, document, articleKey);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToCountry, countryArticle, country);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToUnitOfMeasure, unitOfMeasureArticle, unitOfMeasure);

            List<List<Object>> rows = new ArrayList<List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                List<Object> row = new ArrayList<Object>();

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
        protected DataClass getValueClass() {
            return FileActionClass.getInstance("Файлы таблиц", "xls");
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
            super(genSID(), "Импортировать RRP");
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xsl файл.");
                return;
            }

            List<ImportField> fields = new ArrayList<ImportField>();
            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            ImportField barcodeField = new ImportField(barcode); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, barcode.getMapping(articleKey), padl.getMapping(barcodeField)));

            addImportField(fields, properties, articleKey, name);
            addImportField(fields, properties, articleKey, currentRRP);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToCurrency, currencyArticle, currency);

            List<List<Object>> rows = new ArrayList<List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                List<Object> row = new ArrayList<Object>();

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
        protected DataClass getValueClass() {
            return FileActionClass.getInstance("Файлы таблиц", "xls");
        }
    }

    public class ImportArticlesInfoActionProperty extends ImportActionProperty {

        public ImportArticlesInfoActionProperty() {
            super(genSID(), "Импортировать справочн.");
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xls файл.");
                return;
            }

            List<ImportField> fields = new ArrayList<ImportField>();
            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            ImportField barcodeField = new ImportField(barcode); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, barcode.getMapping(articleKey), padl.getMapping(barcodeField)));

            addImportField(fields, properties, articleKey, name);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToArticleGroup, articleToGroup, articleGroup);
            addImportField(fields, properties, articleKey, fullNameArticle);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToUnitOfMeasure, unitOfMeasureArticle, unitOfMeasure);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToBrend, brendArticle, brend);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToCountry, countryArticle, country);
            addImportField(fields, properties, articleKey, gigienaArticle);
            addImportField(fields, properties, articleKey, spirtArticle);
            addImportObjectNameField(fields, properties, importKeys, articleKey, nameToGender, genderArticle, gender);

            List<List<Object>> rows = new ArrayList<List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                List<Object> row = new ArrayList<Object>();

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
        protected DataClass getValueClass() {
            return FileActionClass.getInstance("Файлы таблиц", "xls");
        }
    }

    public class ImportDocsActionProperty extends ImportActionProperty {

        public ImportDocsActionProperty() {
            super(genSID(), "Импортировать док.", new ValueClass[]{shop});
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            DataObject storeObject = BaseUtils.singleValue(keys);

            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xls файл.");
                return;
            }

            List<ImportField> fields = new ArrayList<ImportField>();
            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            List<ImportKey<?>> importKeys = new ArrayList<ImportKey<?>>();

            // документ
            ImportField numberField = new ImportField(numberInvoiceDocument); fields.add(numberField);
            ImportKey<?> documentKey = new ImportKey(commitDeliveryShopLocal, invoiceDocumentNumber.getMapping(numberField)); importKeys.add(documentKey);
            properties.add(new ImportProperty(numberField, numberInvoiceDocument.getMapping(documentKey)));
            addImportField(fields, properties, date, documentKey);

            properties.add(new ImportProperty(null, subjectIncOrder.getMapping(documentKey), object(store).getMapping(storeObject)));

            // товар
            ImportField barcodeField = new ImportField(barcode); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, padlBarcodeToObject.getMapping(barcodeField)); importKeys.add(articleKey);
            properties.add(new ImportProperty(barcodeField, barcode.getMapping(articleKey), padl.getMapping(barcodeField)));
            addImportField(fields, properties, name, articleKey);

            // поставщик
            ImportKey<?> supplierKey = addImportObjectCGField(fields, properties, importKeys, legalEntityUnn, unnLegalEntity, documentKey, subjectOutOrder, localSupplier);
            addImportField(fields, properties, name, supplierKey);

            addImportField(fields, properties, outerCommitedQuantity, documentKey, articleKey); // пока не article так как не может протолкнуть класс внутрь
            addImportField(fields, properties, priceManfrOrderArticle, documentKey, articleKey);
            addImportField(fields, properties, priceOrderArticle, documentKey, articleKey);
            addImportField(fields, properties, ndsOrderArticle, documentKey, articleKey);
            addImportField(fields, properties, ndsShopOrderPriceArticle, documentKey, articleKey);
            addImportField(fields, properties, shopPrice, documentKey, articleKey);

            List<List<Object>> rows = new ArrayList<List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                List<Object> row = new ArrayList<Object>();

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
        protected DataClass getValueClass() {
            return FileActionClass.getInstance("Файлы таблиц", "xls");
        }
    }

    public class DownToZeroActionProperty extends ActionProperty {

        public DownToZeroActionProperty() {
            super(genSID(), "Обнулить остатки", new ValueClass[]{order});
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            DataObject documentObject = BaseUtils.singleValue(keys);

            // сколько в документе, сколько на остатках, уменьшаем на разницу
            KeyExpr docKey = new KeyExpr("doc"); KeyExpr articleKey = new KeyExpr("article");
            Expr newQuantity = articleQuantity.getExpr(remoteForm, documentObject.getExpr(), articleKey).sum(freeIncOrderArticle.getExpr(remoteForm, documentObject.getExpr(), articleKey).scale(-1));
            session.execute(articleQuantity.getDataChanges(newQuantity, newQuantity.getWhere().and(docKey.compare(documentObject, Compare.EQUALS)), remoteForm, docKey, articleKey), null, null);

            actions.add(new MessageClientAction("Остатки были успешно обнулены", "Обнуление остатков"));
        }
    }
}
