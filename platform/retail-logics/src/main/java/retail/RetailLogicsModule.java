package retail;

import org.apache.log4j.Logger;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.DoubleClass;
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
    private LP quantityShipmentDetail;
    private LP barcodeSku;
    private LP skuShipmentDetail;
    private LP quantityDocumentBatch;
    private LP documentShipmentDetail;
    private LP outSubjectDocument;

    // классы Material Management
    CustomClass document;

    CustomClass shipment;
    CustomClass recievedShipment;
    CustomClass deliveryRecievedShipment;
    CustomClass deliveryShipment;
    CustomClass distributionShipment;
    CustomClass recievedDistributionShipment;
    public ConcreteCustomClass consumerShipment;
    public ConcreteCustomClass supplierShipment;
    public ConcreteCustomClass returnConsumerShipment;
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
    public ConcreteCustomClass consumerOrder;

    CustomClass subject;
    CustomClass store;
    CustomClass shop;
    public ConcreteCustomClass department;
    CustomClass contractor;
    public ConcreteCustomClass supplier;
    public ConcreteCustomClass consumer;

    public ConcreteCustomClass sku;
    public ConcreteCustomClass batch;
    public ConcreteCustomClass shipmentDetail;
    public ConcreteCustomClass barcode;

    public void initTables(){}

    public void initClasses(){

        // Material Management заявки
        document = addAbstractClass("document", "Документ", baseClass.named);
        order = addAbstractClass("order", "Заявка", document);
        inOrder = addAbstractClass("inOrder", "Заявка на приход", order);
        outOrder = addAbstractClass("outOrder", "Заявка на приход", order);
        supplierOrder = addConcreteClass("supplierOrder", "Заявка поставщику", inOrder);
        distributionOrder = addConcreteClass("distributionOrder", "Заявка на перемещение", inOrder, outOrder);
        consumerOrder = addConcreteClass("consumerOrder", "Заявка на внешний расход", outOrder);

        // Material Management движение
        shipment = addAbstractClass("shipment", "Поставка", document);
        recievedShipment = addAbstractClass("recievedShipment", "Принятая поставка", shipment);
        deliveryShipment = addAbstractClass("deliveryShipment", "Отгрузка внешнему контрагенту", shipment);
        deliveryRecievedShipment = addAbstractClass("deliveryRecievedShipment", "Принятая отгрузка от внешнего контрагента", recievedShipment);
        supplierShipment = addConcreteClass("supplierShipment", "Принятая отгрузка от поставщика", deliveryRecievedShipment);
        returnConsumerShipment = addConcreteClass("returnConsumerShipment", "Принятый возврат от покупателя", deliveryRecievedShipment);
        distributionShipment = addAbstractClass("distributionShipment", "Внутреннее перемещение отгруженное", shipment);
        recievedDistributionShipment = addAbstractClass("recievedDistributionShipment", "Внутреннее перемещение принятое", distributionShipment);
        consumerShipment = addConcreteClass("consumerShipment", "Отгрузка покупателю", deliveryShipment);
        returnSupplierShipment = addConcreteClass("returnSupplierShipment", "Возврат внешнему поставщику", deliveryShipment);
        directDistributionShipment = addConcreteClass("directDistributionShipment", "Внутренне перемещение отгруженное прямое", distributionShipment);
        returnDistributionShipment = addConcreteClass("returnDistributionShipment", "Внутренне перемещение отгруженное возвратное", distributionShipment);
        directRecievedDistributionShipment = addConcreteClass("directRecievedDistributionShipment", "Внутренне перемещение принятое прямое", recievedDistributionShipment);
        returnRecievedDistributionShipment = addConcreteClass("returnRecievedDistributionShipment", "Внутренне перемещение принятое возвратное", recievedDistributionShipment);

        // субъекты
        subject = addAbstractClass("subject", "Субъект", baseClass.named);
        store = addAbstractClass("store", "Склад", subject);
        shop = addAbstractClass("shop", "Магазин", store);
        department = addConcreteClass("department", "Отдел", shop);
        contractor = addAbstractClass("contractor", "Контрагент", subject);
        supplier = addConcreteClass("supplier","Поставщик", contractor);
        consumer = addConcreteClass("consumer","Покупатель", contractor);

        // объекты учета
        sku = addConcreteClass("sku", "Товар", baseClass.named);
        batch = addConcreteClass("batch", "Партия", baseClass.named);
        shipmentDetail = addConcreteClass("shipmentDetail", "Строка поставки", baseClass);
        barcode = addConcreteClass("barcode", "Штрих код", baseClass);
    }

    public void initIndexes(){}

    public void initGroups(){
        initBaseGroupAliases();
    }

    public void initProperties(){
        idGroup.add(baseLM.objectValue);
        round2 = addSFProp("round(CAST((prm1) as numeric), 2)", DoubleClass.instance, 1);

        // outSubjectDocument =addCUProp("outSubjectDocument", true, "От кого (ИД)",
        //        addDProp(),
        quantityShipmentDetail = addDProp(baseGroup, "quantityShipmentDetail", "Кол-во", DoubleClass.instance, shipmentDetail);
        documentShipmentDetail = addDProp(baseGroup, "documentShipmentDetail", "Документ", document, shipmentDetail);


        barcodeSku = addDProp(baseGroup, "barcodeSku", "Штрих код", barcode, sku);
        skuShipmentDetail = addDProp(idGroup, "skuShipmentDetail", "SKU (ИД)", sku, shipmentDetail);
    }

    public void initNavigators(){}

}



