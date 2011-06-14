package retail;

import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LP;

import java.io.FileNotFoundException;

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
        this.logger = logger;}

    private LP round2;
    private LP sidDocument;
    private LP barcodeSku;
    private LP skuShipmentDetail;
    private LP skuBatch;
    private LP nameSkuBatch;
    private LP dateBatch;
    private LP supplierBatch;
    private LP nameSupplierBatch;
    private LP shopStock;
    private LP nameShopStock;
    private LP formatShop;
    private LP nameFormatShop;
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

    public void initTables(){}

    public void initClasses(){
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
        subject = addAbstractClass("subject", "Субъект", baseClass.named);
        store = addAbstractClass("store", "Склад", subject);
        shop = addConcreteClass("shop", "Магазин", store);
        stock = addConcreteClass("department", "Отдел", store);
        format = addConcreteClass("format", "Формат", baseClass.named);
        contractor = addAbstractClass("contractor", "Контрагент", subject);
        supplier = addConcreteClass("supplier","Поставщик", contractor);
        customer = addConcreteClass("customer","Покупатель", contractor);

        // объекты учета
        sku = addConcreteClass("sku", "Товар", baseClass.named);
        batch = addAbstractClass("batch", "Партия", baseClass);
        shipmentDetail = addConcreteClass("shipmentDetail", "Строка поставки", batch);
        barcode = addConcreteClass("barcode", "Штрих код", baseLM.barcodeObject);
    }

    public void initIndexes(){}

    public void initGroups(){
        initBaseGroupAliases();
    }

    public void initProperties(){
        idGroup.add(baseLM.objectValue);
        round2 = addSFProp("round(CAST((prm1) as numeric), 2)", DoubleClass.instance, 1);

        sidDocument = addDProp(baseGroup, "sidDocument", "Номер документа", StringClass.get(50), document);

        outSubjectDocument =addCUProp("outSubjectDocument", true, "От кого (ИД)",
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
                addDProp("inStockDistributionShipment","Магазин (прих.)", stock, distributionShipment),
                addDProp("supplierReturnSupplierShipment", "Поставщик", supplier, returnSupplierShipment),
                addDProp("customerCostomerShipment", "Покупатель", customer, customerShipment));


        nameOutSubjectDocument = addJProp("nameOutSubjectDocument", "От кого",  baseLM.name, outSubjectDocument, 1);
        nameInSubjectDocument = addJProp("nameInSubjectDocument", "Кому",  baseLM.name, inSubjectDocument, 1);
        barcodeSku = addJProp(baseGroup, true, "barcodeSku", "Штрих код", baseLM.barcode, addDProp("barSku", "Штрих код (ИД)", barcode, sku), 1);
        skuBatch = addDProp("skuBatch", "SKU (ИД)", sku, batch);
        supplierShipmentBatch = addDProp("supplierShipmentBatch", "Документ прихода", supplierShipment, batch);
        dateBatch = addDProp("dateBatch", "Дата прихода", DateClass.instance, batch);
        supplierBatch = addDProp("supplierBatch","Поставщик (ИД)", supplier, batch);
        nameSupplierBatch = addJProp("nameSupplierBatch","Поставщик", baseLM.name, supplierBatch, 1);
        nameSkuBatch = addJProp(baseGroup, "nameSkuBatch", "Товар", baseLM.name, skuBatch, 1);
        shopStock = addDProp("shopStock", "Магазин (ИД)", shop, stock);
        nameShopStock = addJProp(baseGroup, "nameShopStock", "Магазин", baseLM.name, shopStock, 1);
        formatShop = addDProp("formatShop", "Формат", format, shop);
        nameFormatShop = addJProp(baseGroup, "nameFormatShop", "Формат", baseLM.name, formatShop, 1);

        supplierShipmentShipmentDetail = addDProp("supplierShipmentShipmentDetail", "Документ", supplierShipment, shipmentDetail);
        quantityShipmentBatch = addDProp(baseGroup, "quantityShipmentBatch", "Кол-во", DoubleClass.instance, shipment, batch);

        inQuantityShipmentBatch = addJProp("inQuantityShipmentBatch", "Кол-во прихода", baseLM.and1, quantityShipmentBatch, 1, 2 ,is(receivedShipment), 1);
        outQuantityShipmentBatch = addJProp("outQuantityShipmentBatch", "Кол-во расхода", baseLM.and1, quantityShipmentBatch, 1, 2 ,is(shipment), 1);

        inQuantityInSubjectBatch = addSGProp("inQuantityInSubjectBatch", "Кол-во прихода по субъекту", inQuantityShipmentBatch, inSubjectDocument, 1, 2);
        outQuantityOutSubjectBatch = addSGProp("outQuantityOutSubjectBatch", "Кол-во расхода по субъекту", outQuantityShipmentBatch, outSubjectDocument, 1, 2);
        inQuantityStockBatch = addJProp("inQuantityStockBatch", "Кол-во прихода по складу", baseLM.and1, inQuantityInSubjectBatch, 1, 2, is(stock), 1);
        outQuantityStockBatch = addJProp("outQuantityStockBatch", "Кол-во расхода по складу", baseLM.and1, outQuantityOutSubjectBatch, 1, 2, is(stock), 1);
        freeQuantityStockBatch = addDUProp("freeQuantityStockBatch","Остаток", inQuantityStockBatch, outQuantityStockBatch);
        freeQuantityStockSku = addSGProp("freeQuantityStockSku", "Остаток", freeQuantityStockBatch, 1, skuBatch, 2);


        addShipmentDetailSupplierShipment = addJProp(true, "Добавить строку поставки",addAAProp(shipmentDetail, supplierShipmentBatch), 1);
        sidDocumentSupplierShipmentBatch = addJProp("sidDocumentSupplierShipmentBatch", "Документ", sidDocument, supplierShipmentBatch, 1);
       }

    public void initNavigators() throws JRException, FileNotFoundException {
        NavigatorElement materialManagement = new NavigatorElement(baseLM.baseElement, "materialManagement", "Управление материальными потоками");
           FormEntity ReceivingSupplierShipment =  addFormEntity(new ReceivingSupplierShipment (materialManagement, "ReceivingSupplierShipment", "Приход от поставщика"));
           FormEntity BalanceStockSkuBatch = addFormEntity(new BalanceStockSkuBatch (materialManagement, "BalanceStockSkuBatch", "Остаток по складам-товарам"));
           FormEntity CustomerShipment = addFormEntity(new CustomerShipment (materialManagement, "CustomerShipment", "Отгрузка покупателю"));
           FormEntity DistributionShipment = addFormEntity(new DistributionShipment (materialManagement, "DistributionShipment", "Внутреннее перемещение"));
    }

    private class ReceivingSupplierShipment extends FormEntity<RetailBusinessLogics> {

        public ReceivingSupplierShipment(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objSupplierShipment = addSingleGroupObject(supplierShipment, "Документ поставки");
            addPropertyDraw(objSupplierShipment, baseGroup,nameInSubjectDocument, nameOutSubjectDocument);

          //  if (type.equals(FormType.ADD))
          //      addPropertyDraw(objSupplierShipment);
            addObjectActions(this, objSupplierShipment);

            ObjectEntity objShipmentDetail = addSingleGroupObject(shipmentDetail, "Позиции документа");
            addPropertyDraw(objShipmentDetail, baseGroup);
            addPropertyDraw(objSupplierShipment, objShipmentDetail, baseGroup);

            addObjectActions(this, objShipmentDetail);
            //addActionsOnObjectChange(objShipmentDetail,true, addPropertyObject(addShipmentDetailSupplierShipment, objSupplierShipment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierShipmentBatch, objShipmentDetail),Compare.EQUALS, objSupplierShipment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateBatch, objShipmentDetail),Compare.EQUALS, addPropertyObject(baseLM.date, objSupplierShipment)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBatch, objShipmentDetail),Compare.EQUALS, addPropertyObject(outSubjectDocument,objSupplierShipment)));

        }
    }

    private class  BalanceStockSkuBatch extends FormEntity<RetailBusinessLogics>{

        public BalanceStockSkuBatch(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objStock = addSingleGroupObject(stock, "Склад");
            addPropertyDraw(objStock, baseGroup);

            ObjectEntity objSku = addSingleGroupObject(sku, "Товар");
            addPropertyDraw(objSku, baseGroup);
            addPropertyDraw(objStock, objSku, freeQuantityStockSku);

            ObjectEntity objBatch = addSingleGroupObject(batch, "Партия");
            addPropertyDraw(objBatch, idGroup, nameSkuBatch, nameSupplierBatch, supplierShipmentBatch, dateBatch);
            addPropertyDraw(objStock, objBatch,freeQuantityStockBatch);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(freeQuantityStockBatch, objStock, objBatch)));

            setReadOnly(objBatch, true);
        }
    }

    private class CustomerShipment extends FormEntity<RetailBusinessLogics>{

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
    private class DistributionShipment extends FormEntity<RetailBusinessLogics>{
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



