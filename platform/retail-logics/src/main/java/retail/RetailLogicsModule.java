package retail;

import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.window.PanelNavigatorWindow;
import platform.server.form.window.ToolBarNavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LP;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA.
 * User: Paval
 * Date: 03.06.11
 * Time: 11:52
 * To change this template use File | Settings | File Templates.
 */
    public class RetailLogicsModule extends LogicsModule {
    private final RetailBusinessLogics BL;

    public RetailLogicsModule(BaseLogicsModule<RetailBusinessLogics> baseLM, RetailBusinessLogics BL) {
        super("RetailLogicsModule");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
    }

    private LP round2;
    private LP sidDocument;
    private LP skuBarcode;
    private LP barcodeSku;
    private LP seekBarcodeSku;
    private LP seekBarcodeSkuDocumentLine;
    private LP coeffBarcode;
    private LP skuShipmentDetail;
    private LP skuBatch;
    private LP nameSkuBatch;
    private LP nameSkuBarcode;
    private LP unitOfMeasureSku;
    private LP shortNameUnitOfMeasure;
    private LP nameUnitOfMeasureSku;
    private LP shortNameUnitOfMeasureSku;
    private LP countrySku;
    private LP nameCountrySku;
    private LP grossWeightSku;
    private LP netWeightSku;
    private LP dateBatch;
    private LP supplierBatch;
    private LP nameSupplierBatch;
    private LP addressShop;
    private LP shopStock;
    private LP nameShopStock;
    private LP formatShop;
    private LP nameFormatShop;
    private LP addressLegalEntity;
    private LP unnLegalEntity;
    private LP legalEntityUnn;
    private LP legalAddressShop;
    private LP unnShop;
    private LP legalEntityShop;
    private LP nameLegalEntityShop;
    private LP quantityDocumentBatch;
    private LP quantityDocumentLine;
    private LP purchaseRecadvPurchaseRecadvLine;
    private LP purchaseInvoicePurchaseInvoiceLine;
    private LP inDesadvInvoice;
    private LP priceDocumentLine;
    private LP sumDocumentLine;
    private LP sumDocument;
    private LP skuDocumentLine;
    private LP nameSkuDocumentLine;
    private LP documentDocumentLine;
    private LP outSubjectDocument;
    private LP inSubjectDocument;
    private LP inQuantityDocumentBatch;
    private LP outQuantityDocumentBatch;
    private LP inQuantityInSubjectBatch;
    private LP outQuantityOutSubjectBatch;
    private LP inQuantityStockBatch;
    private LP outQuantityStockBatch;
    private LP freeQuantityStockBatch;
    private LP freeQuantityStockSku;
    private LP purchaseRecadvBatch;
    private LP addShipmentDetailSupplierShipment;
    private LP sidDocumentPurchaseRecadvBatch;
    private LP nameOutSubjectDocument;
    private LP nameInSubjectDocument;

    private LP desadvRecadv;
    private LP sidDesadvRecadv;
    private LP supplierDesadvRecadv;
    private LP priceDocumentSku;
    private LP priceDocumentSkuDocumentLine;

    private LP dateOfInvoice;
    private LP shippedInvoice;
    private LP notShippedinvoice;
    private LP inDesadvInvoiceDocumentLine;
    private LP priceInDesadvInvoiceDocumentLine;
    private LP priceInDesadvInvoiceSku;
    private LP priceInDesadvInvoiceSkuDocumentLine;
    private LP priceInDesadvInvoiceRecadvSkuDocumentLine;

    // классы Material Management
    CustomClass document;

    CustomClass invoice;
    private ConcreteCustomClass saleInvoice;
    private ConcreteCustomClass purchaseInvoice;

    CustomClass order;
    private ConcreteCustomClass saleOrder;
    private ConcreteCustomClass purchaseOrder;

    CustomClass orderReply;
    private ConcreteCustomClass saleOrderReply;
    private ConcreteCustomClass purchaseOrderReply;

    CustomClass desadv;
    private ConcreteCustomClass saleDesadv;
    private ConcreteCustomClass purchaseDesadv;
    private ConcreteCustomClass intraDesadv;

    CustomClass recadv;
    private ConcreteCustomClass saleRecadv;
    private ConcreteCustomClass purchaseRecadv;
    private ConcreteCustomClass intraRecadv;


    public ConcreteCustomClass legalEntity;
    CustomClass subject;
    CustomClass store;
    public ConcreteCustomClass shop;
    public ConcreteCustomClass stock;
    public ConcreteCustomClass format;
    CustomClass contractor;
    public ConcreteCustomClass supplier;
    public ConcreteCustomClass customer;

    public ConcreteCustomClass sku;
    CustomClass batch;
    CustomClass documentLine;
    public ConcreteCustomClass purchaseRecadvLine;
    public ConcreteCustomClass purchaseInvoiceLine;
    public ConcreteCustomClass purchaseDesadvLine;
    public ConcreteCustomClass barcode;
    public ConcreteCustomClass unitOfMeasure;

    public void initTables() {
    }

    public void initClasses() {
        initBaseClassAliases();
        // Material Management заявки
        document = addAbstractClass("document", "Документ", baseLM.transaction);
        order = addAbstractClass("order", "Заявка", document);
        purchaseOrder = addConcreteClass("purchaseOrder", "Заявка на приход", order);
        saleOrder = addConcreteClass("saleOrder", "Заявка на расход", order);

        // Material Management движение
        desadv = addAbstractClass("desadv", "Отгруженный документ", document);
        saleDesadv = addConcreteClass("saleDesadv", "Отгруженный поставщиком документ", desadv);
        purchaseDesadv = addConcreteClass("purchaseDesadv", "Отгруженный покупателю документ", desadv);
        intraDesadv = addConcreteClass("intraDesadv", "Отгруженный документ перемещения", desadv);

        recadv = addAbstractClass("recadv", "Принятый документ", document);
        saleRecadv = addConcreteClass("saleRecadv", "Принятый покупателем документ", recadv);
        purchaseRecadv = addConcreteClass("purchaseRecadv", "Принятый документ поставщика", recadv);
        intraRecadv = addConcreteClass("intraRecadv", "Принятый документ перемещения", recadv, desadv);

        invoice = addAbstractClass("invoice", "Инвойс", document);
        saleInvoice = addConcreteClass("saleInvoice", "Инвойс на продажу", invoice);
        purchaseInvoice = addConcreteClass("purchaseInvoice", "Инвойс на покупку", invoice);

        // субъекты
        legalEntity = addConcreteClass("legalEntity", "Юр.лицо", baseClass.named);
        subject = addAbstractClass("subject", "Субъект", baseClass.named);
        store = addAbstractClass("store", "Склад", subject);
        shop = addConcreteClass("shop", "Магазин", store);
        stock = addConcreteClass("department", "Отдел", store);
        format = addConcreteClass("format", "Формат", baseClass.named);
        contractor = addAbstractClass("contractor", "Контрагент", subject);
        supplier = addConcreteClass("supplier", "Поставщик", contractor);
        customer = addConcreteClass("customer", "Покупатель", contractor);

        // объекты учета
        sku = addConcreteClass("sku", "Товар", baseClass.named);
        batch = addAbstractClass("batch", "Партия", baseClass);
        documentLine = addAbstractClass("documentLine", "Позиция документа", baseClass);
        purchaseRecadvLine = addConcreteClass("purchaseRecadvLine", "Строка поставки", batch, documentLine);
        purchaseInvoiceLine = addConcreteClass("purchaseInvoiceLine", "Строка инвойса", documentLine);
        purchaseDesadvLine = addConcreteClass("purchaseDesadvLine", "Отгрузка поставщика", documentLine);
        barcode = addConcreteClass("barcode", "Бар-код", baseLM.barcodeObject);
        unitOfMeasure = addConcreteClass("unitOfMeasure", "Единица измерения", baseClass.named);

    }

    public void initIndexes() {
    }

    public void initGroups() {
        initBaseGroupAliases();
    }

    public void initProperties() {
        idGroup.add(baseLM.objectValue);
        round2 = addSFProp("round(CAST((prm1) as numeric), 2)", DoubleClass.instance, 1);

        sidDocument = addDProp(baseGroup, "sidDocument", "Номер документа", StringClass.get(50), document);

        outSubjectDocument = addCUProp("outSubjectDocument", true, "От кого (ИД)",
                addDProp("supplierPurchaseOrder", "Поставщик", supplier, purchaseOrder),
                addDProp("outStockIntraDesadv", "Магазин (рacх.)", stock, intraDesadv),
                addDProp("outStockSaleDesadv", "Магазин (рacх.)", stock, saleDesadv),
                addDProp("supplierPurchaseDesadv", "Поставщик", supplier, purchaseDesadv),
                addDProp("outStockIntraRecadv", "Магазин (рacх.)", stock, intraRecadv),
                addDProp("outStockSaleRecadv", "Магазин (рacх.)", stock, saleRecadv),
                addDProp("supplierPurchaseRecadv", "Поставщик", supplier, purchaseRecadv),
                addDProp("outStockSaleInvoice", "Магазин (расх.)", stock, saleInvoice),
                addDProp("supplierPurchaseInvoice", "Поставщик", supplier, purchaseInvoice));

        inSubjectDocument = addCUProp("inSubjectDocument", true, "Кому (ИД)",
                addDProp("inShopPurchaseOrder", "Магазин (прих.)", shop, purchaseOrder),
                addDProp("customerSaleOrder", "Покупатель", customer, saleOrder),
                addDProp("inShopPurchaseDesadv", "Магазин (прих.)", shop, purchaseDesadv),
                addDProp("inStockIntraDesadv", "Магазин (прих.)", stock, intraDesadv),
                addDProp("customerSaleDesadv", "Покупатель", customer, saleDesadv),
                addDProp("inStockPurchaseRecadv", "Магазин (прих.)", stock, purchaseRecadv),
                addDProp("inStockIntraRecadv", "Магазин (прих.)", stock, intraRecadv),
                addDProp("customerSaleRecadv", "Покупатель", customer, saleRecadv),
                addDProp("customerSaleInvoice", "Покупатель", customer, saleInvoice),
                addDProp("inShopPurchaseInvoice", "Магазин (прих.)", shop, purchaseInvoice));


        nameOutSubjectDocument = addJProp("nameOutSubjectDocument", "От кого", baseLM.name, outSubjectDocument, 1);
        nameInSubjectDocument = addJProp("nameInSubjectDocument", "Кому", baseLM.name, inSubjectDocument, 1);
        coeffBarcode = addDProp("coeffBarcode", "Коэффициент бар-кода", DoubleClass.instance, barcode);
        skuBarcode = addDProp("skuBarcode", "Товар", sku, barcode);
        barcodeSku = addAGProp("barcodeSku", "Товар (ИД)", skuBarcode);
        seekBarcodeSku = addJProp(true, "Поиск товара", addSAProp(null), barcodeSku, 1);
        skuBatch = addDProp("skuBatch", "SKU (ИД)", sku, batch);
        unitOfMeasureSku = addDProp("unitOfMeasureSku", "Базовая ЕИ (ИД)", unitOfMeasure, sku);
        shortNameUnitOfMeasure = addDProp(baseGroup, "shortNameUnitOfMeasure", "Краткое наименование ЕИ", StringClass.get(5), unitOfMeasure);
        nameUnitOfMeasureSku = addJProp("nameUnitOfMeasureSku", "Базовая ЕИ", baseLM.name, unitOfMeasureSku, 1);
        shortNameUnitOfMeasureSku = addJProp("shortNameUnitOfMeasureSku", "Базовая ЕИ", shortNameUnitOfMeasure, unitOfMeasureSku, 1);
        countrySku = addDProp("countrySku", "Страна происхождения (ИД)", baseLM.country, sku);
        nameCountrySku = addJProp("nameCountrySku", "Страна происхождения", baseLM.name, countrySku, 1);
        grossWeightSku = addDProp("grossWeightSku", "Вес брутто", DoubleClass.instance, sku);
        netWeightSku = addDProp("netWeightSku", "Вес нетто", DoubleClass.instance, sku);
        purchaseRecadvBatch = addDProp("purchaseRecadvBatch", "Документ прихода", purchaseRecadv, batch);
        dateBatch = addJProp("dateBatch", "Дата прихода", baseLM.date, purchaseRecadvBatch, 1);
        supplierBatch = addJProp("supplierBatch", "Поставщик (ИД)", outSubjectDocument, purchaseRecadvBatch, 1);
        nameSupplierBatch = addJProp("nameSupplierBatch", "Поставщик", baseLM.name, supplierBatch, 1);
        nameSkuBatch = addJProp(baseGroup, "nameSkuBatch", "Товар", baseLM.name, skuBatch, 1);
        nameSkuBarcode = addJProp(baseGroup, "nameSkuBarcode", "Товар", baseLM.name, skuBarcode, 1);
        addressShop = addDProp("addressShop", "Адрес", StringClass.get(50), shop);
        shopStock = addDProp("shopStock", "Магазин (ИД)", shop, stock);
        nameShopStock = addJProp(baseGroup, "nameShopStock", "Магазин", baseLM.name, shopStock, 1);
        formatShop = addDProp("formatShop", "Формат", format, shop);
        nameFormatShop = addJProp(baseGroup, "nameFormatShop", "Формат", baseLM.name, formatShop, 1);
        addressLegalEntity = addDProp(baseGroup, "addressLegalEntity", "Адрес (юр.)", StringClass.get(50), legalEntity);
        unnLegalEntity = addDProp(baseGroup, "unnLegalEntity", "УНН", StringClass.get(9), legalEntity);
        legalEntityUnn = addAGProp("legalEntityUnn", "Юр. лицо", unnLegalEntity);
        legalEntityShop = addDProp("legalEntityShop", "Юр.лицо (ИД)", legalEntity, shop);
        nameLegalEntityShop = addJProp("nameLegalEntityShop", "Юр.лицо", baseLM.name, legalEntityShop, 1);
        legalAddressShop = addJProp("legalAddressShop", "Адрес (юр.)", addressLegalEntity, legalEntityShop, 1);
        unnShop = addJProp("unnShop", "УНН", unnLegalEntity, legalEntityShop, 1);

        dateOfInvoice = addDProp("dateOfInvoice", "Дата выставления инвойса", DateClass.instance, invoice);

        purchaseRecadvPurchaseRecadvLine = addDProp("purchaseRecadvPurchaseRecadvLine", "Документ", purchaseRecadv, purchaseRecadvLine);
        quantityDocumentBatch = addDProp(baseGroup, "quantityDocumentBatch", "Кол-во", DoubleClass.instance, document, batch);
        quantityDocumentLine = addDProp(baseGroup, "quantityDocumentLine", "Кол-во", DoubleClass.instance, documentLine);
        skuDocumentLine = addDProp(baseGroup, "skuDocumentLine", "Товар (ИД)", sku, documentLine);
        nameSkuDocumentLine = addJProp(baseGroup, "nameSkuDocumentLine", "Товар", baseLM.name, skuDocumentLine, 1);
        documentDocumentLine = addDProp("documentDocumentLine", "Документ", document, documentLine);

        purchaseInvoicePurchaseInvoiceLine = addDProp("purchaseInvoicePurchaseInvoiceLine", "Инвойс", purchaseInvoice, purchaseInvoiceLine);
        priceDocumentLine = addDProp("priceDocumentLine", "Цена", DoubleClass.instance, documentLine);
        sumDocumentLine = addJProp("sumDocumentLine", "Сумма", baseLM.multiplyDouble2, quantityDocumentLine, 1, priceDocumentLine, 1);
        sumDocument = addSGProp("sumDocument", "Сумма по документу", sumDocumentLine, documentDocumentLine, 1);

        desadvRecadv = addDProp("desadvRecadv", "По отгрузке", desadv, recadv);
        sidDesadvRecadv = addJProp("sidDesadvRecadv", "По отгрузке", sidDocument, desadvRecadv, 1);
        supplierDesadvRecadv = addJProp("supplierDesadvRecadv", "Поставщик", outSubjectDocument, desadvRecadv, 1);

        inDesadvInvoice = addDProp("inDesadvInvoice", "В поставке", LogicalClass.instance ,desadv, invoice);
        inDesadvInvoiceDocumentLine = addJProp("inDesadvInvoiceDocumentLine", "В поставке", inDesadvInvoice, 1, documentDocumentLine, 2);
        priceInDesadvInvoiceDocumentLine = addJProp("priceInDesadvInvoiceDocumentLine", "Цена в инвойсе", baseLM.and1, priceDocumentLine, 2, inDesadvInvoiceDocumentLine, 1, 2);
        priceInDesadvInvoiceSku = addMGProp("priceInDesadvInvoiceSku", "Цена товара в инвойсе", priceInDesadvInvoiceDocumentLine, skuDocumentLine, 2, 1);
        priceInDesadvInvoiceSkuDocumentLine = addJProp("priceInDesadvInvoiceSkuDocumentLine", "Цена товара в инвойсе", priceInDesadvInvoiceSku, skuDocumentLine, 2, 1);
        priceInDesadvInvoiceRecadvSkuDocumentLine = addJProp("priceInDesadvInvoiceRecadvSkuDocumentLine", "Цена товара в инвойсе", priceInDesadvInvoiceSkuDocumentLine, 1, desadvRecadv, 2);


        shippedInvoice = addMGProp("shippedInvoice", "Отгруженный инвойс", inDesadvInvoice, 2);

        priceDocumentSku = addMGProp(baseGroup, "priceDocumentSku", "Цена товара", priceDocumentLine, skuDocumentLine, 1, documentDocumentLine, 1);
        priceDocumentSkuDocumentLine = addJProp("priceDocumentSkuDocumentLine", "Цена товара", priceDocumentSku, skuDocumentLine, 1, documentDocumentLine, 1);

        inQuantityDocumentBatch = addJProp("inQuantityDocumentBatch", "Кол-во прихода", baseLM.and1, quantityDocumentBatch, 1, 2, is(recadv), 1);
        outQuantityDocumentBatch = addJProp("outQuantityDocumentBatch", "Кол-во расхода", baseLM.and1, quantityDocumentBatch, 1, 2, is(desadv), 1);

        inQuantityInSubjectBatch = addSGProp("inQuantityInSubjectBatch", "Кол-во прихода по субъекту", inQuantityDocumentBatch, inSubjectDocument, 1, 2);
        outQuantityOutSubjectBatch = addSGProp("outQuantityOutSubjectBatch", "Кол-во расхода по субъекту", outQuantityDocumentBatch, outSubjectDocument, 1, 2);
        inQuantityStockBatch = addJProp("inQuantityStockBatch", "Кол-во прихода по складу", baseLM.and1, inQuantityInSubjectBatch, 1, 2, is(stock), 1);
        outQuantityStockBatch = addJProp("outQuantityStockBatch", "Кол-во расхода по складу", baseLM.and1, outQuantityOutSubjectBatch, 1, 2, is(stock), 1);
        freeQuantityStockBatch = addDUProp("freeQuantityStockBatch", "Остаток", inQuantityStockBatch, outQuantityStockBatch);
        freeQuantityStockSku = addSGProp("freeQuantityStockSku", "Остаток", freeQuantityStockBatch, 1, skuBatch, 2);

   //     addShipmentDetailSupplierShipment = addJProp(true, "Добавить строку поставки", addAAProp(shipmentDetail, purchaseRecadvBatch), 1);
        sidDocumentPurchaseRecadvBatch = addJProp("sidDocumentSupplierShipmentBatch", "Документ", sidDocument, purchaseRecadvBatch, 1);

        initNavigators();
    }

    private void initNavigators() {

        ToolBarNavigatorWindow mainToolbar = new ToolBarNavigatorWindow(JToolBar.HORIZONTAL, "mainToolbar", "Навигатор");
        mainToolbar.titleShown = false;
        baseLM.baseElement.window = mainToolbar;

        baseLM.navigatorWindow.y = 10;
        baseLM.navigatorWindow.height -= 10;

        ToolBarNavigatorWindow leftToolbar = new ToolBarNavigatorWindow(JToolBar.VERTICAL, "leftToolbar", "Список");
        leftToolbar.titleShown = false;
        baseLM.adminElement.window = leftToolbar;

        PanelNavigatorWindow generateToolbar = new PanelNavigatorWindow(SwingConstants.HORIZONTAL, "generateToolbar", "Генерация");
        generateToolbar.titleShown = false;
        generateToolbar.drawRoot = true;
        generateToolbar.drawScrollBars = false;

        mainToolbar.setDockPosition(0, 0, 100, 6);
        leftToolbar.setDockPosition(0, 6, 20, 64);
        generateToolbar.setDockPosition(20, 6, 80, 4);

        TreeNavigatorWindow objectsWindow = new TreeNavigatorWindow("objectsWindow", "Объекты");
        objectsWindow.drawRoot = true;
        baseLM.objectElement.window = objectsWindow;

        NavigatorElement classifier = addNavigatorElement(baseLM.baseElement, "classifier", "Справочники");
        classifier.window = leftToolbar;
        FormEntity SkuBarcodeForm = addFormEntity(new SkuBarcodeForm(classifier, "SkuBarcodeForm", "Справочник товаров"));
        FormEntity UnitOfMeasureForm = addFormEntity(new UnitOfMeasureForm(classifier, "UnitOfMeasureForm", "Справочник единиц измерения"));
        FormEntity SubjectForm = addFormEntity(new SubjectForm(classifier, "SubjectForm", "Справочник мест учета"));
        FormEntity ContractorForm = addFormEntity(new ContractorForm(classifier, "ContractorForm", "Справочник контрагентов"));

        NavigatorElement purchaseManagement = addNavigatorElement(baseLM.baseElement, "purchaseManagement", "Закупки");
        purchaseManagement.window = leftToolbar;
        FormEntity PurchaseInvoiceForm = addFormEntity(new PurchaseInvoiceForm(purchaseManagement, "PurchaseInvoiceForm", "Инвойсы"));
        FormEntity PurchaseDesadvForm = addFormEntity(new PurchaseDesadvForm(purchaseManagement, "PurchaseDesadvForm", "Отгрузки поставщиков"));
        FormEntity InDesadvInvoiceForm = addFormEntity(new InDesadvInvoiceForm(purchaseManagement, "InDesadvInvoiceForm", "Отгрузки по инвойсам"));

        NavigatorElement materialManagement = addNavigatorElement(baseLM.baseElement, "materialManagement", "Управление материальными потоками");
        materialManagement.window = leftToolbar;
        FormEntity ReceivingSupplierShipment = addFormEntity(new ReceivingSupplierShipment(materialManagement, "ReceivingSupplierShipment", "Приход от поставщика", false, false));
        FormEntity BalanceStockSkuBatch = addFormEntity(new BalanceStockSkuBatch(materialManagement, "BalanceStockSkuBatch", "Остаток по складам-товарам"));
        FormEntity CustomerShipment = addFormEntity(new CustomerShipment(materialManagement, "CustomerShipment", "Отгрузка покупателю"));
        FormEntity DistributionShipment = addFormEntity(new DistributionShipment(materialManagement, "DistributionShipment", "Внутреннее перемещение"));

    //    NavigatorElement purchaseCreate = new NavigatorElement(baseLM.baseElement, "purchaseCreate", "Создать");
    //    FormEntity addSupplierShipment = addFormEntity(new ReceivingSupplierShipment(purchaseCreate, "addSupplierShipment", "Добавление документа", false, true));
    //    purchaseCreate.window = generateToolbar;

        baseLM.baseElement.add(baseLM.adminElement);
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    private class SkuBarcodeForm extends FormEntity<RetailBusinessLogics>{
        private ObjectEntity objSku;
        private ObjectEntity objBarcode;

        private SkuBarcodeForm(NavigatorElement parent, String sID, String caption){
            super(parent, sID, caption);

            objSku = addSingleGroupObject(sku, "Товары");
            addPropertyDraw(objSku, baseLM.name, shortNameUnitOfMeasureSku, nameCountrySku, grossWeightSku, netWeightSku);
            addObjectActions(this, objSku);
            objBarcode = addSingleGroupObject(barcode, "Бар-коды товара");
            addPropertyDraw(objBarcode, baseLM.barcode, coeffBarcode);
            addObjectActions(this, objBarcode);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(skuBarcode, objBarcode), Compare.EQUALS, objSku));
        }
    }

    private class  UnitOfMeasureForm extends FormEntity<RetailBusinessLogics>{
        private ObjectEntity objUnitOfMeasure;

        private UnitOfMeasureForm(NavigatorElement parent, String sID, String caption){
            super(parent, sID, caption);

            objUnitOfMeasure = addSingleGroupObject(unitOfMeasure, "Единицы измерения");
            addPropertyDraw(objUnitOfMeasure, baseLM.name, shortNameUnitOfMeasure);
            addObjectActions(this, objUnitOfMeasure);

        }
    }

    private class SubjectForm extends FormEntity<RetailBusinessLogics>{
        private ObjectEntity objShop;
        private ObjectEntity objStock;

        private SubjectForm(NavigatorElement parent, String sID, String caption){
            super(parent, sID, caption);

            objShop = addSingleGroupObject(shop, "Магазины");
            addPropertyDraw(objShop, baseLM.name, unnShop, addressShop, legalAddressShop, nameLegalEntityShop, nameFormatShop);
            addObjectActions(this, objShop);

            objStock = addSingleGroupObject(stock, "Отделы");
            addPropertyDraw(objStock, baseLM.name);
            addObjectActions(this, objStock);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(shopStock, objStock), Compare.EQUALS, objShop));
        }
    }

    private class ContractorForm extends FormEntity<RetailBusinessLogics>{
        private ObjectEntity objContractor;

        private ContractorForm(NavigatorElement parent, String sID, String caption){
            super(parent, sID, caption);

            objContractor = addSingleGroupObject(contractor, "Контрагенты");
            addPropertyDraw(objContractor, baseLM.name);
            addObjectActions(this, objContractor);
        }
    }

    private class PurchaseInvoiceForm extends FormEntity<RetailBusinessLogics>{
        private ObjectEntity objSupplier;
        private ObjectEntity objPurchaseInvoice;
        private ObjectEntity objPurchaseInvoiceLine;

        private PurchaseInvoiceForm(NavigatorElement parent, String sID, String caption){
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objSupplier, true);

            objPurchaseInvoice = addSingleGroupObject(purchaseInvoice, "Инвойсы");
            addPropertyDraw(objPurchaseInvoice, baseLM.date, sidDocument, nameOutSubjectDocument, nameInSubjectDocument, sumDocument, dateOfInvoice, shippedInvoice);
            addObjectActions(this, objPurchaseInvoice);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outSubjectDocument, objPurchaseInvoice), Compare.EQUALS, objSupplier));

            objPurchaseInvoiceLine = addSingleGroupObject(purchaseInvoiceLine, "Позиции инвойса");
            addPropertyDraw(objPurchaseInvoiceLine, nameSkuDocumentLine, priceDocumentLine, quantityDocumentLine, sumDocumentLine);
            addObjectActions(this, objPurchaseInvoiceLine);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(documentDocumentLine, objPurchaseInvoiceLine), Compare.EQUALS, objPurchaseInvoice));

            addActionsOnObjectChange(objPurchaseInvoiceLine, addPropertyObject(baseLM.apply));

            RegularFilterGroupEntity filterShippedInvoice = new RegularFilterGroupEntity(genID());
            filterShippedInvoice.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(shippedInvoice, objPurchaseInvoice))),
                    "Ожидается к поставке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterShippedInvoice);
       }
    }

    private class PurchaseDesadvForm extends FormEntity<RetailBusinessLogics>{
        private ObjectEntity objSupplier;
        private ObjectEntity objPurchaseDesadv;
        private ObjectEntity objPurchaseDesadvLine;
        private ObjectEntity objBarcode;

        private PurchaseDesadvForm(NavigatorElement parent, String sID, String caption){
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objSupplier, true);

            objPurchaseDesadv = addSingleGroupObject(purchaseDesadv, "Отгрузка поставщика");
            addPropertyDraw(objPurchaseDesadv, baseLM.date, nameOutSubjectDocument, nameInSubjectDocument, sidDocument, sumDocument);
            addObjectActions(this, objPurchaseDesadv);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outSubjectDocument, objPurchaseDesadv), Compare.EQUALS, objSupplier));

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseGroup, true);
            objBarcode.groupTo.initClassView = ClassViewType.PANEL;
           // objBarcode.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            //objBarcode.resetOnApply = true;

            //addPropertyDraw(baseLM.reverseBarcode);

            objPurchaseDesadvLine = addSingleGroupObject(purchaseDesadvLine, "Позиции отгрузки");
            addPropertyDraw(objPurchaseDesadvLine, nameSkuDocumentLine, quantityDocumentLine, priceDocumentLine, sumDocumentLine);
            addObjectActions(this, objPurchaseDesadvLine);
            addPropertyDraw(objPurchaseDesadv, objPurchaseDesadvLine, priceInDesadvInvoiceSkuDocumentLine);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(documentDocumentLine, objPurchaseDesadvLine), Compare.EQUALS, objPurchaseDesadv));

            addActionsOnObjectChange(objBarcode, true,
                        addPropertyObject(baseLM.seekBarcodeAction, objBarcode));
        }
    }

    private class InDesadvInvoiceForm extends FormEntity<RetailBusinessLogics>{
        private ObjectEntity objSupplier;
        private ObjectEntity objPurchaseDesadv;
        private ObjectEntity objPurchaseInvoice;

        private InDesadvInvoiceForm(NavigatorElement parent, String sID, String caption){
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objSupplier, true);

            objPurchaseDesadv = addSingleGroupObject(purchaseDesadv, "Поставки");
            addPropertyDraw(objPurchaseDesadv, baseLM.date, sidDocument, nameOutSubjectDocument, nameOutSubjectDocument, sumDocument);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outSubjectDocument, objPurchaseDesadv), Compare.EQUALS, objSupplier));
            setReadOnly(objPurchaseDesadv, true);

            objPurchaseInvoice = addSingleGroupObject(purchaseInvoice, "Инвойсы");
            addPropertyDraw(objPurchaseInvoice, baseLM.date, sidDocument, nameOutSubjectDocument, nameInSubjectDocument, sumDocument);
            addPropertyDraw(objPurchaseDesadv, objPurchaseInvoice, inDesadvInvoice);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outSubjectDocument, objPurchaseInvoice), Compare.EQUALS, objSupplier));
            setReadOnly(objPurchaseInvoice, true);
            setReadOnly(inDesadvInvoice, false);
        }
    }
    private class ReceivingSupplierShipment extends FormEntity<RetailBusinessLogics> {

        private ObjectEntity objPurchaseRecadv;
        private ObjectEntity objPurchaseRecadvLine;
        private ObjectEntity objBarcode;

        private PropertyDrawEntity createPurchaseRecadv;

        private boolean add, edit;

        private ReceivingSupplierShipment(NavigatorElement parent, String sID, String caption, boolean edit, boolean add) {
            super(parent, sID, caption);

            this.add = add;
            this.edit = edit;

            objPurchaseRecadv = addSingleGroupObject(purchaseRecadv, "Документ поставки");
            addPropertyDraw(objPurchaseRecadv, baseGroup, nameOutSubjectDocument, nameInSubjectDocument, desadvRecadv ,baseLM.delete);
            //addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDesadvRecadv, objPurchaseRecadv), Compare.EQUALS, addPropertyObject(outSubjectDocument, objPurchaseRecadv)));

            objPurchaseRecadvLine = addSingleGroupObject(purchaseRecadvLine, "Позиции документа");
            addPropertyDraw(objPurchaseRecadvLine, nameSkuBatch, priceDocumentLine);
            addPropertyDraw(objPurchaseRecadv, objPurchaseRecadvLine, quantityDocumentBatch);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(purchaseRecadvBatch, objPurchaseRecadvLine), Compare.EQUALS, objPurchaseRecadv));

            if (edit) {
                objPurchaseRecadv.groupTo.setSingleClassView(ClassViewType.PANEL);
                addObjectActions(this, objPurchaseRecadvLine);

            } else {
                if (add) {
                    objPurchaseRecadv.groupTo.setSingleClassView(ClassViewType.PANEL);
                    objPurchaseRecadv.setAddOnTransaction();
                    addObjectActions(this, objPurchaseRecadvLine);
                } else {
                    ReceivingSupplierShipment addSupplierShipment = new ReceivingSupplierShipment(null, "addSupplierShipment", "Добавление документа", false, true);

                    ReceivingSupplierShipment editSupplierShipment = new ReceivingSupplierShipment(null, "editSupplierShipment", "Редактирование документа", true, false);

                    createPurchaseRecadv = addPropertyDraw(addMFAProp(actionGroup,
                            "Создать документ",
                            addSupplierShipment,
                            new ObjectEntity[0] ,
                            true), objPurchaseRecadv);

                    createPurchaseRecadv.forceViewType = ClassViewType.GRID;

                    addPropertyDraw(addMFAProp(null,
                            "Редактировать",
                            editSupplierShipment,
                            new ObjectEntity[]{editSupplierShipment.objPurchaseRecadv},
                            true), objPurchaseRecadv).forceViewType = ClassViewType.GRID;

                    setReadOnly(baseLM.date, true, objPurchaseRecadv.groupTo);
                    setReadOnly(nameInSubjectDocument, true, objPurchaseRecadv.groupTo);
                    setReadOnly(nameOutSubjectDocument, true, objPurchaseRecadv.groupTo);
                    setReadOnly(sidDocument, true, objPurchaseRecadv.groupTo);
                    setReadOnly(objPurchaseRecadvLine, true);
                }
            }
        }


        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (!this.add && !this.edit)
                design.getMainContainer().addBefore(design.get(createPurchaseRecadv), design.getGroupObjectContainer(objPurchaseRecadv.groupTo));

            return design;
        }
    }

    private class BalanceStockSkuBatch extends FormEntity<RetailBusinessLogics> {

        public BalanceStockSkuBatch(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objStock = addSingleGroupObject(stock, "Склад");
            addPropertyDraw(objStock, baseGroup);

            ObjectEntity objSku = addSingleGroupObject(sku, "Товар");
            addPropertyDraw(objSku, baseGroup);
            addPropertyDraw(objStock, objSku, freeQuantityStockSku);

            ObjectEntity objBatch = addSingleGroupObject(batch, "Партия");
            addPropertyDraw(objBatch, idGroup, nameSkuBatch, nameSupplierBatch, dateBatch);
            addPropertyDraw(objStock, objBatch, freeQuantityStockBatch);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(freeQuantityStockBatch, objStock, objBatch)));

            setReadOnly(objBatch, true);
        }
    }

    private class CustomerShipment extends FormEntity<RetailBusinessLogics> {

        public CustomerShipment(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objCustomerShipment = addSingleGroupObject(saleDesadv, "Документ");
            addPropertyDraw(objCustomerShipment, baseGroup, nameOutSubjectDocument, nameInSubjectDocument);
            addObjectActions(this, objCustomerShipment);

            ObjectEntity objBatch = addSingleGroupObject(batch, "Партия");
            addPropertyDraw(objBatch, baseGroup, purchaseRecadvBatch, nameSupplierBatch, dateBatch);
            addPropertyDraw(objCustomerShipment, objBatch, quantityDocumentBatch);

            setReadOnly(objBatch, true);
            setReadOnly(quantityDocumentBatch, false);

        }
    }

    private class DistributionShipment extends FormEntity<RetailBusinessLogics> {
        public DistributionShipment(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDistributionShipment = addSingleGroupObject(intraRecadv, "Документ");
            addPropertyDraw(objDistributionShipment, baseGroup, nameOutSubjectDocument, nameInSubjectDocument);
            addObjectActions(this, objDistributionShipment);

            ObjectEntity objBatch = addSingleGroupObject(batch, "Партия");
            addPropertyDraw(objBatch, baseGroup, purchaseRecadvBatch, nameSupplierBatch, dateBatch);
            addPropertyDraw(objDistributionShipment, objBatch, quantityDocumentBatch, inQuantityDocumentBatch, outQuantityDocumentBatch);
        }
    }
}



