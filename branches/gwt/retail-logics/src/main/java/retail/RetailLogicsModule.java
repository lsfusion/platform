package retail;

import org.apache.log4j.Logger;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.StringClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LP;

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
    private LP shopStock;
    private LP nameShopStock;
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

    // классы Material Management
    CustomClass document;

    CustomClass shipment;
    CustomClass recievedShipment;
    CustomClass deliveryRecievedShipment;
    CustomClass deliveryShipment;
    CustomClass distributionShipment;
    CustomClass recievedDistributionShipment;
    public ConcreteCustomClass customerShipment;
    public ConcreteCustomClass supplierShipment;
    public ConcreteCustomClass returnCustomerShipment;
    public ConcreteCustomClass returnSupplierShipment;
    public ConcreteCustomClass directDistributionShipment;
    public ConcreteCustomClass returnDistributionShipment;
    public ConcreteCustomClass directRecievedDistributionShipment;
    public ConcreteCustomClass returnRecievedDistributionShipment;

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
        recievedShipment = addAbstractClass("recievedShipment", "Принятая поставка", shipment);
        deliveryShipment = addAbstractClass("deliveryShipment", "Отгрузка внешнему контрагенту", shipment);
        deliveryRecievedShipment = addAbstractClass("deliveryRecievedShipment", "Принятая отгрузка от внешнего контрагента", recievedShipment);
        supplierShipment = addConcreteClass("supplierShipment", "Принятая отгрузка от поставщика", deliveryRecievedShipment);
        returnCustomerShipment = addConcreteClass("returnCustomerShipment", "Принятый возврат от покупателя", deliveryRecievedShipment);
        distributionShipment = addAbstractClass("distributionShipment", "Внутреннее перемещение отгруженное", shipment);
        recievedDistributionShipment = addAbstractClass("recievedDistributionShipment", "Внутреннее перемещение принятое", distributionShipment);
        customerShipment = addConcreteClass("customerShipment", "Отгрузка покупателю", deliveryShipment);
        returnSupplierShipment = addConcreteClass("returnSupplierShipment", "Возврат внешнему поставщику", deliveryShipment);
        directDistributionShipment = addConcreteClass("directDistributionShipment", "Внутренне перемещение отгруженное прямое", distributionShipment);
        returnDistributionShipment = addConcreteClass("returnDistributionShipment", "Внутренне перемещение отгруженное возвратное", distributionShipment);
        directRecievedDistributionShipment = addConcreteClass("directRecievedDistributionShipment", "Внутренне перемещение принятое прямое", recievedDistributionShipment);
        returnRecievedDistributionShipment = addConcreteClass("returnRecievedDistributionShipment", "Внутренне перемещение принятое возвратное", recievedDistributionShipment);

        // субъекты
        subject = addAbstractClass("subject", "Субъект", baseClass.named);
        store = addAbstractClass("store", "Склад", subject);
        shop = addConcreteClass("shop", "Магазин", store);
        stock = addConcreteClass("department", "Отдел", store);
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

        sidDocument = addDProp(baseGroup, "sidDocument", "Код документа", StringClass.get(50), document);

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
                addDProp("inStockRecievedShipment", "Магазин (прих.)", stock, recievedShipment),
                addDProp("inStockDistributionShipment","Магазин (прих.)", stock, distributionShipment),
                addDProp("supplierReturnSupplierShipment", "Поставщик", supplier, returnSupplierShipment),
                addDProp("customerCostomerShipment", "Покупатель", customer, customerShipment));


        barcodeSku = addJProp(baseGroup, true, "barcodeSku", "Штрих код", baseLM.barcode, addDProp("barSku", "Штрих код (ИД)", barcode, sku), 1);
        skuBatch = addDProp(idGroup, "skuBatch", "SKU (ИД)", sku, batch);
        shopStock = addDProp("shopStock", "Магазин (ИД)", shop, stock);
        nameShopStock = addJProp(baseGroup, "nameShopStock", "Магазин", baseLM.name, shopStock, 1);

        supplierShipmentShipmentDetail = addDProp(baseGroup, "supplierShipmentShipmentDetail", "Документ", supplierShipment, shipmentDetail);
        quantityShipmentBatch = addDProp(baseGroup, "quantityShipmentBatch", "Кол-во", DoubleClass.instance, shipment, batch);

        inQuantityShipmentBatch = addJProp("inQuantityShipmentBatch", "Кол-во прихода", baseLM.and1, quantityShipmentBatch, 1, 2 ,is(recievedShipment), 1);
        outQuantityShipmentBatch = addJProp("outQuantityShipmentBatch", "Кол-во расхода", baseLM.and1, quantityShipmentBatch, 1, 2 ,is(shipment), 1);

        inQuantityInSubjectBatch = addSGProp("inQuantityInSubjectBatch", "Кол-во прихода по субъекту", inQuantityShipmentBatch, inSubjectDocument, 1, 2);
        outQuantityOutSubjectBatch = addSGProp("outQuantityOutSubjectBatch", "Кол-во расхода по субъекту", outQuantityShipmentBatch, outSubjectDocument, 1, 2);
        inQuantityStockBatch = addJProp("inQuantityStockBatch", "Кол-во прихода по складу", baseLM.and1, inQuantityInSubjectBatch, 1, 2, is(stock), 1);
        outQuantityStockBatch = addJProp("outQuantityStockBatch", "Кол-во расхода по складу", baseLM.and1, outQuantityOutSubjectBatch, 1, 2, is(stock), 1);
        freeQuantityStockBatch = addDUProp("freeQuantityStockBatch","Остаток", inQuantityStockBatch, outQuantityStockBatch);
    }

    public void initNavigators(){}

}



