package retail;

import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LP;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.util.logging.Filter;

/**
 * Created by IntelliJ IDEA.
 * User: Paval
 * Date: 03.06.11
 * Time: 11:52
 * To change this template use File | Settings | File Templates.
 */
public class RetailLogicsModule extends LogicsModule {
    private RetailBusinessLogics RetailBL;
    private Logger logger;

    public RetailLogicsModule(BaseLogicsModule<RetailBusinessLogics> baseLM, RetailBusinessLogics RetailBL, Logger logger) {
        super("RetailLogicsModule");
        setBaseLogicsModule(baseLM);
        this.RetailBL = RetailBL;
        this.logger = logger;
    }

    private LP round2;
    private LP sidDocument;
    private LP skuBarcode;
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
    private LP quantityShipmentBatch;
    private LP supplierShipmentShipmentDetail;
    private LP outSubjectDocument;
    private LP inSubjectDocument;
    private LP inQuantityShipmentBatch;
    private LP outQuantityShipmentBatch;
    private LP inQuantityInSubjectBatch;
    private LP outQuantityOutSubjectBatch;
    private LP inQuantityStockBatch;
    private LP outQuantityStockBatch;
    private LP freeQuantityStockBatch;
    private LP freeQuantityStockSku;
    private LP supplierShipmentBatch;
    private LP addShipmentDetailSupplierShipment;
    private LP sidDocumentSupplierShipmentBatch;
    private LP nameOutSubjectDocument;
    private LP nameInSubjectDocument;

    // классы Material Management
    CustomClass document;

    CustomClass shipment;
    CustomClass receivedShipment;
    CustomClass deliveryReceivedShipment;
    CustomClass deliveryShipment;
    CustomClass distributionShipment;
    CustomClass receivedDistributionShipment;
    public ConcreteCustomClass customerShipment;
    public ConcreteCustomClass supplierShipment;
    public ConcreteCustomClass returnCustomerShipment;
    public ConcreteCustomClass returnSupplierShipment;
    public ConcreteCustomClass directDistributionShipment;
    public ConcreteCustomClass returnDistributionShipment;
    public ConcreteCustomClass directReceivedDistributionShipment;
    public ConcreteCustomClass returnReceivedDistributionShipment;

    CustomClass order;
    CustomClass outOrder;
    CustomClass inOrder;
    public ConcreteCustomClass supplierOrder;
    public ConcreteCustomClass distributionOrder;
    public ConcreteCustomClass customerOrder;

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
    public ConcreteCustomClass shipmentDetail;
    public ConcreteCustomClass barcode;
    public ConcreteCustomClass unitOfMeasure;

    public void initTables() {
    }

    public void initClasses() {
        initBaseClassAliases();
        // Material Management заявки
        document = addAbstractClass("document", "Документ", baseLM.transaction);
        order = addAbstractClass("order", "Заявка", document);
        inOrder = addAbstractClass("inOrder", "Заявка на приход", order);
        outOrder = addAbstractClass("outOrder", "Заявка на расход", order);
        supplierOrder = addConcreteClass("supplierOrder", "Заявка поставщику", inOrder);
        distributionOrder = addConcreteClass("distributionOrder", "Заявка на перемещение", inOrder, outOrder);
        customerOrder = addConcreteClass("customerOrder", "Заявка на внешний расход", outOrder);

        // Material Management движение
        shipment = addAbstractClass("shipment", "Поставка", document);
        receivedShipment = addAbstractClass("receivedShipment", "Принятая поставка", shipment);
        deliveryShipment = addAbstractClass("deliveryShipment", "Отгрузка внешнему контрагенту", shipment);
        deliveryReceivedShipment = addAbstractClass("deliveryReceivedShipment", "Принятая отгрузка от внешнего контрагента", receivedShipment);
        supplierShipment = addConcreteClass("supplierShipment", "Принятая отгрузка от поставщика", deliveryReceivedShipment);
        returnCustomerShipment = addConcreteClass("returnCustomerShipment", "Принятый возврат от покупателя", deliveryReceivedShipment);
        distributionShipment = addAbstractClass("distributionShipment", "Внутреннее перемещение отгруженное", shipment);
        receivedDistributionShipment = addAbstractClass("receivedDistributionShipment", "Внутреннее перемещение принятое", distributionShipment, receivedShipment);
        customerShipment = addConcreteClass("customerShipment", "Отгрузка покупателю", deliveryShipment);
        returnSupplierShipment = addConcreteClass("returnSupplierShipment", "Возврат внешнему поставщику", deliveryShipment);
        directDistributionShipment = addConcreteClass("directDistributionShipment", "Внутреннее перемещение отгруженное прямое", distributionShipment);
        returnDistributionShipment = addConcreteClass("returnDistributionShipment", "Внутреннее перемещение отгруженное возвратное", distributionShipment);
        directReceivedDistributionShipment = addConcreteClass("directReceivedDistributionShipment", "Внутренне перемещение принятое прямое", receivedDistributionShipment);
        returnReceivedDistributionShipment = addConcreteClass("returnReceivedDistributionShipment", "Внутренне перемещение принятое возвратное", receivedDistributionShipment);

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
        shipmentDetail = addConcreteClass("shipmentDetail", "Строка поставки", batch);
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
                addDProp("supplierSupplierOrder", "Поставщик", supplier, supplierOrder),
                addDProp("outStockDestributionOrder", "Магазин (рacх.)", stock, distributionOrder),
                addDProp("outStockCustomerOrder", "Магазин (рacх.)", stock, customerOrder),
                addDProp("supplierSupplierShipment", "Поставщик", supplier, supplierShipment),
                addDProp("customerReturnCustomerShipment", "Покупатель", customer, returnCustomerShipment),
                addDProp("outStockDeliveryShipment", "Магазин (расх.)", stock, deliveryShipment),
                addDProp("outStockDistributionShipment", "Магазин (расх.)", stock, distributionShipment));

        inSubjectDocument = addCUProp("inSubjectDocument", true, "Кому (ИД)",
                addDProp("inStockIncOrder", "Магазин (прих.)", stock, inOrder),
                addDProp("customerCustomerOrder", "Покупатель", customer, customerOrder),
                addDProp("inStockReceivedShipment", "Магазин (прих.)", stock, deliveryReceivedShipment),
                addDProp("inStockDistributionShipment", "Магазин (прих.)", stock, distributionShipment),
                addDProp("supplierReturnSupplierShipment", "Поставщик", supplier, returnSupplierShipment),
                addDProp("customerCostomerShipment", "Покупатель", customer, customerShipment));


        nameOutSubjectDocument = addJProp("nameOutSubjectDocument", "От кого", baseLM.name, outSubjectDocument, 1);
        nameInSubjectDocument = addJProp("nameInSubjectDocument", "Кому", baseLM.name, inSubjectDocument, 1);
        coeffBarcode = addDProp("coeffBarcode", "Коэффициент бар-кода", DoubleClass.instance, barcode);
        skuBarcode = addDProp("skuBarcode", "Товар", sku, barcode);
        skuBatch = addDProp("skuBatch", "SKU (ИД)", sku, batch);
        unitOfMeasureSku = addDProp("unitOfMeasureSku", "Базовая ЕИ (ИД)", unitOfMeasure, sku);
        shortNameUnitOfMeasure = addDProp(baseGroup, "shortNameUnitOfMeasure", "Краткое наименование ЕИ", StringClass.get(5), unitOfMeasure);
        nameUnitOfMeasureSku = addJProp("nameUnitOfMeasureSku", "Базовая ЕИ", baseLM.name, unitOfMeasureSku, 1);
        shortNameUnitOfMeasureSku = addJProp("shortNameUnitOfMeasureSku", "Базовая ЕИ", shortNameUnitOfMeasure, unitOfMeasureSku, 1);
        countrySku = addDProp("countrySku", "Страна происхождения (ИД)", baseLM.country, sku);
        nameCountrySku = addJProp("nameCountrySku", "Страна происхождения", baseLM.name, countrySku, 1);
        grossWeightSku = addDProp("grossWeightSku", "Вес брутто", DoubleClass.instance, sku);
        netWeightSku = addDProp("netWeightSku", "Вес нетто", DoubleClass.instance, sku);
        supplierShipmentBatch = addDProp("supplierShipmentBatch", "Документ прихода", supplierShipment, batch);
        dateBatch = addJProp("dateBatch", "Дата прихода", baseLM.date, supplierShipmentBatch, 1);
        supplierBatch = addJProp("supplierBatch", "Поставщик (ИД)", outSubjectDocument, supplierShipmentBatch, 1);
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

        supplierShipmentShipmentDetail = addDProp("supplierShipmentShipmentDetail", "Документ", supplierShipment, shipmentDetail);
        quantityShipmentBatch = addDProp(baseGroup, "quantityShipmentBatch", "Кол-во", DoubleClass.instance, shipment, batch);

        inQuantityShipmentBatch = addJProp("inQuantityShipmentBatch", "Кол-во прихода", baseLM.and1, quantityShipmentBatch, 1, 2, is(receivedShipment), 1);
        outQuantityShipmentBatch = addJProp("outQuantityShipmentBatch", "Кол-во расхода", baseLM.and1, quantityShipmentBatch, 1, 2, is(shipment), 1);

        inQuantityInSubjectBatch = addSGProp("inQuantityInSubjectBatch", "Кол-во прихода по субъекту", inQuantityShipmentBatch, inSubjectDocument, 1, 2);
        outQuantityOutSubjectBatch = addSGProp("outQuantityOutSubjectBatch", "Кол-во расхода по субъекту", outQuantityShipmentBatch, outSubjectDocument, 1, 2);
        inQuantityStockBatch = addJProp("inQuantityStockBatch", "Кол-во прихода по складу", baseLM.and1, inQuantityInSubjectBatch, 1, 2, is(stock), 1);
        outQuantityStockBatch = addJProp("outQuantityStockBatch", "Кол-во расхода по складу", baseLM.and1, outQuantityOutSubjectBatch, 1, 2, is(stock), 1);
        freeQuantityStockBatch = addDUProp("freeQuantityStockBatch", "Остаток", inQuantityStockBatch, outQuantityStockBatch);
        freeQuantityStockSku = addSGProp("freeQuantityStockSku", "Остаток", freeQuantityStockBatch, 1, skuBatch, 2);


        addShipmentDetailSupplierShipment = addJProp(true, "Добавить строку поставки", addAAProp(shipmentDetail, supplierShipmentBatch), 1);
        sidDocumentSupplierShipmentBatch = addJProp("sidDocumentSupplierShipmentBatch", "Документ", sidDocument, supplierShipmentBatch, 1);
    }

    public void initNavigators() throws JRException, FileNotFoundException {
        NavigatorElement classifier = new NavigatorElement(baseLM.baseElement, "classifier", "Справочники");
        FormEntity SkuBarcodeForm = addFormEntity(new SkuBarcodeForm(classifier, "SkuBarcodeForm", "Справочник товаров"));
        FormEntity UnitOfMeasureForm = addFormEntity(new UnitOfMeasureForm(classifier, "UnitOfMeasureForm", "Справочник единиц измерения"));
        FormEntity SubjectForm = addFormEntity(new SubjectForm(classifier, "SubjectForm", "Справочник мест учета"));
        FormEntity ContractorForm = addFormEntity(new ContractorForm(classifier, "ContractorForm", "Справочник контрагентов"));
        NavigatorElement materialManagement = new NavigatorElement(baseLM.baseElement, "materialManagement", "Управление материальными потоками");
        FormEntity ReceivingSupplierShipment = addFormEntity(new ReceivingSupplierShipment(materialManagement, "ReceivingSupplierShipment", "Приход от поставщика", false, false));
        FormEntity BalanceStockSkuBatch = addFormEntity(new BalanceStockSkuBatch(materialManagement, "BalanceStockSkuBatch", "Остаток по складам-товарам"));
        FormEntity CustomerShipment = addFormEntity(new CustomerShipment(materialManagement, "CustomerShipment", "Отгрузка покупателю"));
        FormEntity DistributionShipment = addFormEntity(new DistributionShipment(materialManagement, "DistributionShipment", "Внутреннее перемещение"));
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

    private class ReceivingSupplierShipment extends FormEntity<RetailBusinessLogics> {

        private ObjectEntity objSupplierShipment;
        private ObjectEntity objShipmentDetail;
        private ObjectEntity objBarcode;

        private ReceivingSupplierShipment(NavigatorElement parent, String sID, String caption, boolean edit, boolean add) {
            super(parent, sID, caption);

            objSupplierShipment = addSingleGroupObject(supplierShipment, "Документ поставки");
            addPropertyDraw(objSupplierShipment, baseGroup, nameInSubjectDocument, nameOutSubjectDocument, baseLM.delete);
            objShipmentDetail = addSingleGroupObject(shipmentDetail, "Позиции документа");
            addPropertyDraw(objShipmentDetail, baseGroup);
            addPropertyDraw(objSupplierShipment, objShipmentDetail, baseGroup);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierShipmentBatch, objShipmentDetail), Compare.EQUALS, objSupplierShipment));

            if (edit) {
                objSupplierShipment.groupTo.setSingleClassView(ClassViewType.PANEL);
                addPropertyDraw(objShipmentDetail, baseLM.delete);
                addObjectActions(this, objShipmentDetail);

            } else {
                if (add) {
                    objSupplierShipment.groupTo.setSingleClassView(ClassViewType.PANEL);
                    addPropertyDraw(objShipmentDetail, baseLM.delete);
                    objSupplierShipment.addOnTransaction = true;
                    addObjectActions(this, objShipmentDetail);
                } else {
                    ReceivingSupplierShipment addSupplierShipment = new ReceivingSupplierShipment(null, "addSupplierShipment", "Добавление документа", false, true);

                    ReceivingSupplierShipment editSupplierShipment = new ReceivingSupplierShipment(null, "editSupplierShipment", "Редактирование документа", true, false);

                    addPropertyDraw(addMFAProp(null,
                            "Создать документ",
                            addSupplierShipment,
                            new ObjectEntity[0],
                            new PropertyObjectEntity[0],
                            new PropertyObjectEntity[0],
                            true), null).forceViewType = ClassViewType.GRID;

                    addPropertyDraw(addMFAProp(null,
                            "Редактировать",
                            editSupplierShipment,
                            new ObjectEntity[]{editSupplierShipment.objSupplierShipment},
                            new PropertyObjectEntity[0],
                            new PropertyObjectEntity[0],
                            true), objSupplierShipment).forceViewType = ClassViewType.GRID;

                    setReadOnly(baseLM.date, true, objSupplierShipment.groupTo);
                    setReadOnly(nameInSubjectDocument, true, objSupplierShipment.groupTo);
                    setReadOnly(nameOutSubjectDocument, true, objSupplierShipment.groupTo);
                    setReadOnly(sidDocument, true, objSupplierShipment.groupTo);
                    setReadOnly(objShipmentDetail, true);
                }
            }
        }

        //public DefaultFormView createDefaultRichDesign() {
        //    DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
        //
        //    design.get(objBarcode.groupTo).grid.hideToolbarItems();
        //    design.addIntersection(design.getGroupObjectContainer(objShipmentDetail.groupTo),
        //            design.getGroupObjectContainer(objBarcode.groupTo),
        //            DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
        //    design.get(objBarcode.groupTo).grid.constraints.fillHorizontal = 0.2;
        //
        //    return design;
        //
        //}
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
            addPropertyDraw(objBatch, idGroup, nameSkuBatch, nameSupplierBatch, sidDocumentSupplierShipmentBatch, dateBatch);
            addPropertyDraw(objStock, objBatch, freeQuantityStockBatch);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(freeQuantityStockBatch, objStock, objBatch)));

            setReadOnly(objBatch, true);
        }
    }

    private class CustomerShipment extends FormEntity<RetailBusinessLogics> {

        public CustomerShipment(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objCustomerShipment = addSingleGroupObject(customerShipment, "Документ");
            addPropertyDraw(objCustomerShipment, baseGroup, nameOutSubjectDocument, nameInSubjectDocument);
            addObjectActions(this, objCustomerShipment);

            ObjectEntity objBatch = addSingleGroupObject(batch, "Партия");
            addPropertyDraw(objBatch, baseGroup, supplierShipmentBatch, nameSupplierBatch, dateBatch);
            addPropertyDraw(objCustomerShipment, objBatch, quantityShipmentBatch);

            setReadOnly(objBatch, true);
            setReadOnly(quantityShipmentBatch, false);

        }
    }

    private class DistributionShipment extends FormEntity<RetailBusinessLogics> {
        public DistributionShipment(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDistributionShipment = addSingleGroupObject(distributionShipment, "Документ");
            addPropertyDraw(objDistributionShipment, baseGroup, nameOutSubjectDocument, nameInSubjectDocument);
            addObjectActions(this, objDistributionShipment);

            ObjectEntity objBatch = addSingleGroupObject(batch, "Партия");
            addPropertyDraw(objBatch, baseGroup, supplierShipmentBatch, nameSupplierBatch, dateBatch);
            addPropertyDraw(objDistributionShipment, objBatch, quantityShipmentBatch, inQuantityShipmentBatch);
        }
    }
}



