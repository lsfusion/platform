package tmc;

import net.sf.jasperreports.engine.JRException;

import java.sql.SQLException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;

import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.linear.LP;
import platform.server.logics.property.AggregateProperty;
import platform.server.classes.CustomClass;
import platform.server.classes.StringClass;
import platform.server.classes.DoubleClass;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.OrFilterNavigator;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.auth.User;
import platform.interop.UserInfo;


public class UsmeshkaBusinessLogics extends BusinessLogics<UsmeshkaBusinessLogics> {

    public UsmeshkaBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    //    static Registry registry;
    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {

        System.out.println("Server is starting...");
        DataAdapter adapter = new PostgreDataAdapter("usmeshka","localhost","postgres","11111");
        UsmeshkaBusinessLogics BL = new UsmeshkaBusinessLogics(adapter,7652);

//        if(args.length>0 && args[0].equals("-F"))
//        BL.fillData();
        LocateRegistry.createRegistry(7652).rebind("BusinessLogics", BL);
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
        System.out.println("Server has successfully started");
    }

    // конкретные классы
    // реализация по безналу (в опт)
    CustomClass orderSaleWhole, invoiceSaleWhole, commitSaleWhole;
    // реализация в розницу
    CustomClass orderSaleRetail, commitSaleRetail;
    // инвентаризация
    CustomClass balanceCheck;
    // закупка у местного поставщика
    CustomClass orderDeliveryLocal, commitDeliveryLocal;
    // закупка у импортного поставщика
    CustomClass orderDeliveryImport, commitDeliveryImport;
    // внутреннее перемещение
    CustomClass orderDistribute, invoiceDistribute, commitOutDistribute, commitIncDistribute;
    // возвраты
    // возврат местному поставщику
    CustomClass orderReturnDeliveryLocal, invoiceReturnDeliveryLocal, commitReturnDeliveryLocal;
    // возврат реализации по безналу
    CustomClass returnSaleWhole;
    // возврат реализации за наличный расчет
    CustomClass returnSaleRetail;

    CustomClass order, orderInc, orderOut;
    CustomClass invoiceDocument;
    CustomClass commitOut, commitInc;
    CustomClass orderExtInc, commitExtInc;

    CustomClass documentInner, orderOuter, commitOuter;

    CustomClass move, moveInner, returnInner, returnOuter;

    CustomClass store, article, localSupplier, importSupplier, orderLocal;
    CustomClass customerWhole, customerRetail, orderWhole, orderRetail;

    protected void initClasses() {
        // заявки на приход, расход
        order = addAbstractClass("Заявка", transaction);
        orderInc = addAbstractClass("Заявка прихода на склад", order);
        orderOut = addAbstractClass("Заявка расхода со склада", order);

        invoiceDocument = addAbstractClass("Заявка на перевозку", order);
        commitOut = addAbstractClass("Отгруженная заявка", order);
        commitInc = addAbstractClass("Принятая заявка", commitOut);

        // внутр. и внешние операции
        orderOuter = addAbstractClass("Заявка на внешнюю операцию", orderInc); // всегда прих., создает партию - элементарную единицу учета
        commitOuter = addAbstractClass("Внешняя операция", orderOuter, commitInc);

        documentInner = addAbstractClass("Внутренняя операция", order);
        returnInner = addAbstractClass("Возврат внутренней операции", order);

        orderExtInc = addAbstractClass("Закупка", orderOuter);
        commitExtInc = addAbstractClass("Приход от пост.", commitOuter, orderExtInc);

        orderWhole = addAbstractClass("Операция по безналу", order);
        orderRetail = addAbstractClass("Операция за наличный расчет", order);

        orderSaleWhole = addConcreteClass("Заказ по безналу", orderOut, documentInner, orderWhole);
        invoiceSaleWhole = addConcreteClass("Выписанный заказ по безналу", orderSaleWhole, invoiceDocument);
        commitSaleWhole = addConcreteClass("Отгруженный заказ по безналу", invoiceSaleWhole, commitOut);

        orderSaleRetail = addConcreteClass("Заказ за наличный расчет", orderOut, documentInner, orderRetail);
        commitSaleRetail = addConcreteClass("Реализация за наличный расчет", orderSaleRetail, commitOut);

        balanceCheck = addConcreteClass("Инвентаризация", orderOut, commitOut, documentInner);

        orderDistribute = addConcreteClass("Заказ на внутреннее перемещение", orderOut, orderInc, documentInner);
        invoiceDistribute = addConcreteClass("Выписанное внутреннее перемещение", orderDistribute, invoiceDocument);
        commitOutDistribute = addConcreteClass("Отгруженное внутреннее перемещение", invoiceDistribute, commitOut);
        commitIncDistribute = addConcreteClass("Принятое внутреннее перемещение", commitOutDistribute, commitInc);

        orderLocal = addConcreteClass("Операция с местным поставщиком", order);

        orderDeliveryLocal = addConcreteClass("Закупка у местного поставщика", orderExtInc, orderLocal);
        commitDeliveryLocal = addConcreteClass("Приход от местного поставщика", orderDeliveryLocal, commitExtInc);

        orderDeliveryImport = addConcreteClass("Закупка у импортного поставщика", orderExtInc);
        commitDeliveryImport = addConcreteClass("Приход от импортного поставщика", orderDeliveryImport, commitExtInc);

        orderReturnDeliveryLocal = addConcreteClass("Заявка на возврат местному поставщику", orderOut, orderLocal);
        invoiceReturnDeliveryLocal = addConcreteClass("Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal,invoiceDocument);
        commitReturnDeliveryLocal = addConcreteClass("Возврат местному поставщику", invoiceReturnDeliveryLocal);

        returnSaleWhole = addConcreteClass("Возврат реализации по безналу", orderInc, returnInner, commitInc, orderWhole);
        returnSaleRetail = addConcreteClass("Возврат реализации за наличный расчет", orderInc, returnInner, commitInc, orderRetail);

        store = addConcreteClass("Склад", namedObject);
        article = addConcreteClass("Товар", namedObject);
        localSupplier = addConcreteClass("Местный поставщик", namedObject);
        importSupplier = addConcreteClass("Импортный поставщик", namedObject);
        customerWhole = addConcreteClass("Оптовый покупатель", namedObject);
        customerRetail = addConcreteClass("Розничный покупатель", namedObject);
    }

    LP balanceSklFreeQuantity;

    protected void initProperties() {
        LP incStore = addDProp(baseGroup, "incStore", "Склад (прих.)", store, orderInc);
        LP outStore = addDProp(baseGroup, "outStore", "Склад (расх.)", store, orderOut);

        LP orderContragent = addCUProp(baseGroup, "Контрагент", // generics
                addDProp("localSupplier", "Поставщик", localSupplier, orderLocal),
                addDProp("importSupplier", "Поставщик", importSupplier, orderDeliveryImport),
                addDProp("wholeCustomer", "Покупатель", customerWhole, orderWhole),
                addDProp("retailCustomer", "Покупатель", customerRetail, orderRetail));

        LP invoiceNumber = addDProp(baseGroup, "Накладная", StringClass.get(20), invoiceDocument);

        LP outerOrderQuantity = addDProp(baseGroup, "extIncOrderQuantity", "Кол-во заяв.", DoubleClass.instance, orderOuter, article);
        LP outerCommitedQuantity = addDProp(baseGroup, "extIncCommitedQuantity", "Кол-во принятое", DoubleClass.instance, commitOuter, article);

        // для возвратных своего рода generics
        LP returnOuterQuantity = addDProp(baseGroup, "returnDeliveryLocalQuantity", "Кол-во возврата", DoubleClass.instance, orderReturnDeliveryLocal, article, commitDeliveryLocal);

        LP returnInnerCommitQuantity = addCUProp(baseGroup, "Кол-во возврата", // generics
                         addDProp("returnSaleWholeQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleWhole, article, commitOuter, commitSaleWhole),
                         addDProp("returnSaleRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleRetail, article, commitOuter, commitSaleRetail));

        LP returnQuantity = addCUProp(baseGroup, "Кол-во возврата", returnOuterQuantity, // возвратный документ\прямой документ
                                addSGProp("Кол-во возврата", returnInnerCommitQuantity, 1, 2, 4));
        addConstraint(addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", and1, addJProp(diff2, orderContragent, 1, orderContragent, 2), 1, 3, returnQuantity, 1, 2, 3), false);

        LP documentInnerQuantity = addDProp("outOrderQuantity", "Кол-во операции", DoubleClass.instance, documentInner, article, commitOuter);

        LP innerQuantity = addCUProp(baseGroup, "Кол-во операции", returnOuterQuantity, documentInnerQuantity,
                                addSGProp("Кол-во операции", returnInnerCommitQuantity, 1, 2, 3));

        LP incCommitedQuantity = addCUProp(baseGroup, "Кол-во прихода парт.",
                        addJProp(and1, outerCommitedQuantity, 1, 2, split(commitOuter), 1, 3), // избыточно так как не может сама класс определить
                        addJProp(and1, innerQuantity, 1, 2, 3, is(commitInc), 1));
        LP incSklCommitedQuantity = addSGProp(baseGroup, "Кол-во прихода парт. на скл.", incCommitedQuantity, incStore, 1, 2, 3);

        LP outCommitedQuantity = addJProp("Кол-во отгр. парт.", and1, innerQuantity, 1, 2, 3, is(commitOut), 1);
        LP outSklCommitedQuantity = addSGProp(baseGroup, "Кол-во отгр. парт. на скл.", outCommitedQuantity, outStore, 1, 2, 3);
        LP outSklQuantity = addSGProp(baseGroup, "Кол-во заяв. парт. на скл.", innerQuantity, outStore, 1, 2, 3);

        LP balanceSklCommitedQuantity = addDUProp(baseGroup, "Остаток парт. на скл.", incSklCommitedQuantity, outSklCommitedQuantity);
        balanceSklFreeQuantity = addDUProp(baseGroup, "Свободное кол-во на скл.", incSklCommitedQuantity, outSklQuantity);
        addConstraint(addJProp("Кол-во резерва должно быть не меньше нуля", greater2, vzero, balanceSklFreeQuantity, 1, 2, 3), false);

        LP documentIncSklCommitedQuantity = addJProp(baseGroup, "Остаток парт. прих.", balanceSklCommitedQuantity, incStore, 1, 2, 3);
        LP documentOutSklCommitedQuantity = addJProp(baseGroup, "Остаток парт. расх.", balanceSklCommitedQuantity, outStore, 1, 2, 3);
        LP documentOutSklFreeQuantity = addJProp(baseGroup, "Свободно парт. расх.", balanceSklFreeQuantity, outStore, 1, 2, 3);

        LP returnedInnerQuantity = addSGProp("Кол-во возвр. паст.", returnInnerCommitQuantity, 4, 2, 3);
        LP confirmedInnerQuantity = addDUProp("Кол-во подтв. парт.", documentInnerQuantity, returnedInnerQuantity);
        addConstraint(addJProp("Кол-во возврата должно быть не меньше кол-ва самой операции", greater2, vzero, confirmedInnerQuantity, 1, 2, 3), false);

        // собственно для возврата по товару поставщику - ограничение что MIN(кол-во подтв.,кол-во остатка)
        // для внутренней операции - кол-во остатка
        // для возврата от покупателя - кол-во подтв. 
    }

    protected void initGroups() {
    }

    protected void initConstraints() {
    }

    protected void initPersistents() {
//        persistents.add((AggregateProperty) balanceSklFreeQuantity.property);
    }

    protected void initTables() {
        tableFactory.include("article",article);
        tableFactory.include("orders", order);
        tableFactory.include("store",store);
        tableFactory.include("localsupplier",localSupplier);
        tableFactory.include("importsupplier",importSupplier);
        tableFactory.include("customerwhole",customerWhole);
        tableFactory.include("customerretail",customerRetail);
        tableFactory.include("articlestore",article,store);
        tableFactory.include("articleorder",article,order);
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {

        createDefaultClassForms(baseClass, baseElement);

        NavigatorElement documents = new NavigatorElement(baseElement, 1000, "Документы");
            new LocalNavigatorForm(documents, 1100);
            new DeliveryImportNavigatorForm(documents, 1150);
            NavigatorElement innerSplit = new NavigatorElement(documents, 1200, "Внутренние документы по товарам");
                new WholeNavigatorForm(innerSplit, 1250, true);
                new RetailNavigatorForm(innerSplit, 1300, true);
                new DistributeNavigatorForm(innerSplit, 1320, true);
                new BalanceCheckNavigatorForm(innerSplit, 1350, true);
                new ReturnDeliveryLocalNavigatorForm(innerSplit, 1400, true);
                new ReturnSaleWholeNavigatorForm(innerSplit, 1450, true);
                new ReturnSaleRetailNavigatorForm(innerSplit, 1500, true);
            NavigatorElement inner = new NavigatorElement(documents, 1600, "Внутренние документы по партиям");
                new WholeNavigatorForm(inner, 1650, false);
                new RetailNavigatorForm(inner, 1700, false);
                new DistributeNavigatorForm(inner, 1720, false);
                new BalanceCheckNavigatorForm(inner, 1750, false);
                new ReturnDeliveryLocalNavigatorForm(inner, 1800, false);
                new ReturnSaleWholeNavigatorForm(inner, 1850, false);
                new ReturnSaleRetailNavigatorForm(inner, 1900, false);        
        NavigatorElement store = new NavigatorElement(baseElement, 2000, "Сводная информация");
            new StoreArticleNavigatorForm(store, 2100);
    }

    private class DocumentNavigatorForm extends NavigatorForm {
        final ObjectNavigator objDoc;

        protected DocumentNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass) {
            super(parent, ID, documentClass.caption);

            objDoc = addSingleGroupObjectImplement(documentClass, "Документ", properties, baseGroup, true);
        }
    }

    private class OuterNavigatorForm extends DocumentNavigatorForm {
        final ObjectNavigator objArt;

        protected OuterNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass) {
            super(parent, ID, documentClass);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, true);
        }
    }

    private class LocalNavigatorForm extends OuterNavigatorForm {
        public LocalNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, orderLocal);
        }
    }

    private class DeliveryImportNavigatorForm extends OuterNavigatorForm {
        public DeliveryImportNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, orderDeliveryImport);
        }
    }

    private class InnerNavigatorForm extends DocumentNavigatorForm {
        final ObjectNavigator objOuter;
        final ObjectNavigator objArt;

        protected InnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean split) {
            super(parent, ID, documentClass);

            if(split) {
                objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true); 
                objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true);
            } else {
                GroupObjectNavigator gobjArtOuter = new GroupObjectNavigator(IDShift(1));

                objArt = new ObjectNavigator(IDShift(1), article, "Товар");
                objOuter = new ObjectNavigator(IDShift(1), commitOuter, "Партия");

                gobjArtOuter.add(objArt);
                gobjArtOuter.add(objOuter);
                addGroup(gobjArtOuter);

                addPropertyView(objArt, properties, baseGroup, true);
                addPropertyView(objOuter, properties, baseGroup, true);
            }


            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objOuter, objDoc, properties, baseGroup, true);
            addPropertyView(objOuter, objDoc, objArt, properties, baseGroup, true);
        }
    }

    private class WholeNavigatorForm extends InnerNavigatorForm {
        public WholeNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderWhole, split);
        }
    }

    private class RetailNavigatorForm extends InnerNavigatorForm {
        public RetailNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderRetail, split);
        }
    }

    private class DistributeNavigatorForm extends InnerNavigatorForm {
        public DistributeNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderDistribute, split);
        }
    }

    private class BalanceCheckNavigatorForm extends InnerNavigatorForm {
        public BalanceCheckNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, balanceCheck, split);
        }
    }

    private class ReturnDeliveryLocalNavigatorForm extends InnerNavigatorForm {
        public ReturnDeliveryLocalNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderReturnDeliveryLocal, split);
        }
    }

    private class ReturnInnerNavigatorForm extends InnerNavigatorForm {
        final ObjectNavigator objInner;

        protected ReturnInnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, CustomClass commitClass, boolean split) {
            super(parent, ID, documentClass, split);

            objInner = addSingleGroupObjectImplement(commitClass, "Документ к возврату", properties, baseGroup, true);

            addPropertyView(objInner, objArt, properties, baseGroup, true);
            addPropertyView(objInner, objDoc, properties, baseGroup, true);
            addPropertyView(objInner, objDoc, objArt, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objDoc, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objDoc, objArt, properties, baseGroup, true);
        }
    }

    private class ReturnSaleWholeNavigatorForm extends ReturnInnerNavigatorForm {
        private ReturnSaleWholeNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, returnSaleWhole, commitSaleWhole, split);
        }
    }

    private class ReturnSaleRetailNavigatorForm extends ReturnInnerNavigatorForm {
        private ReturnSaleRetailNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, returnSaleRetail, commitSaleRetail, split);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {
        protected StoreArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по складу");

            ObjectNavigator objDoc = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);
            ObjectNavigator objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, true);
            addPropertyView(objDoc, objOuter, properties, baseGroup, true);
            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objDoc, objOuter, objArt, properties, baseGroup, true);
        }
    }

    protected void initAuthentication() {
        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));
    }
}
