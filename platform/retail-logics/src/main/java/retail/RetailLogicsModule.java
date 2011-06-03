package retail;

import org.apache.log4j.Logger;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.LogicsModule;

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
        setBaseLogicsModule(baseLM);
        this.RetailBL = RetailBL;
        this.logger = logger;}

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

    CustomClass order;
    CustomClass outOrder;
    CustomClass inOrder;
    public ConcreteCustomClass supplierOrder;
    public ConcreteCustomClass distributionOrder;
    public ConcreteCustomClass consumerOrder;

    public void initTables(){}

    public void initClasses(){
        document = addAbstractClass("document", "Документ", baseClass.named);
        order = addAbstractClass("order", "Заявка", document);
        inOrder = addAbstractClass("inOrder", "Заявка на приход", order);
        outOrder = addAbstractClass("outOrder", "Заявка на приход", order);
        supplierOrder = addConcreteClass("supplierOrder", "Заявка поставщику", inOrder);
        distributionOrder = addConcreteClass("distributionOrder", "Заявка на перемещение", inOrder);

        shipment = addAbstractClass("shipment", "Поставка", document);
        recievedShipment = addAbstractClass("recievedShipment", "Принятая поставка", shipment);
        deliveryShipment = addAbstractClass("deliveryShipment", "Отгрузка внешнему контрагенту", shipment);
        deliveryRecievedShipment = addAbstractClass("deliveryRecievedShipment", "Принятая отгрузка от внешнего контрагента", recievedShipment);

    }

    public void initIndexes(){}

    public void initGroups(){}

    public void initProperties(){}

    public void initNavigators(){}

}



