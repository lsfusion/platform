package sample;

import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.linear.LP;
import platform.server.logics.BusinessLogics;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.view.navigator.filter.CompareFilterNavigator;
import platform.server.auth.User;
import platform.server.classes.AbstractCustomClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.DoubleClass;
import platform.interop.Compare;
import platform.interop.UserInfo;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import net.sf.jasperreports.engine.JRException;

import javax.swing.*;

public class SampleBusinessLogics extends BusinessLogics<SampleBusinessLogics> {

    public SampleBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
    }

//    static Registry registry;
    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {

        System.out.println("Server is starting...");
        DataAdapter adapter = new PostgreDataAdapter("sample6","localhost","postgres","11111");
        SampleBusinessLogics BL = new SampleBusinessLogics(adapter,7652);

//        if(args.length>0 && args[0].equals("-F"))
//        BL.fillData();
        LocateRegistry.createRegistry(7652).rebind("BusinessLogics", BL);
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
        System.out.println("Server has successfully started");
    }


    AbstractGroup documentGroup, fixedGroup, currentGroup, lastDocumentGroup;

    protected void initGroups() {
    }

    AbstractCustomClass document;

    ConcreteCustomClass article, store, incomeDocument, outcomeDocument;

    protected void initClasses() {

        article = addConcreteClass("Товар", namedObject);
        store = addConcreteClass("Склад", namedObject);
        document = addAbstractClass("Документ", namedObject, transaction);
        incomeDocument = addConcreteClass("Приход", document);
        outcomeDocument = addConcreteClass("Расход", document);
    }

    LP quantity, documentStore;
    LP balanceQuantity, incQuantity;

    protected void initProperties() {

        documentStore = addDProp(baseGroup, "store", "Склад док-та", store, document);
        quantity = addDProp(baseGroup, "quantity", "Кол-во", DoubleClass.instance, document, article);

        LP storeName = addJProp(baseGroup, "Имя склада", name, documentStore, 1);

        incQuantity = addJProp("Кол-во прихода", and1, quantity, 1, 2, is(incomeDocument), 1);
        LP outQuantity = addJProp("Кол-во расхода", and1, quantity, 1, 2, is(outcomeDocument), 1);

        LP incStoreQuantity = addSGProp(baseGroup, "Прих. по скл.", incQuantity, documentStore, 1, 2);
        LP outStoreQuantity = addSGProp(baseGroup, "Расх. по скл.", outQuantity, documentStore, 1, 2);

        balanceQuantity = addDUProp(baseGroup, "Ост. по скл.", incStoreQuantity, outStoreQuantity);

        addConstraint(addJProp("Остаток должен быть положительным", greater2, vzero, balanceQuantity, 1, 2), false);

        addJProp(baseGroup, "Ост. по скл. (док.)", balanceQuantity, documentStore, 1, 2);
    }

    protected void initConstraints() {
//        balanceQuantity.property.constraint = new PositiveConstraint();
    }

    protected void initPersistents() {
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    protected void initAuthentication() {
        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));
    }

    NavigatorForm mainAccountForm, salesArticleStoreForm;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
            NavigatorForm documentForm = new DocumentNavigatorForm(primaryData, 110, "Документ");

        NavigatorElement aggregateData = new NavigatorElement(baseElement, 200, "Сводная информация");
            NavigatorForm storeArticleForm = new StoreArticleNavigatorForm(aggregateData, 211, "Товары по складам");
            NavigatorForm systemForm = new SystemNavigatorForm(aggregateData, 212, "Движение (документ*товар)");

//        extIncomeDocument.relevantElements.set(0, extIncDetailForm);
    }

    private class DocumentNavigatorForm extends NavigatorForm {

        public DocumentNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(document, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(quantity.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {

        public StoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);
//            objArt.groupTo.gridClassView = false; //objArt.groupTo.singleViewType = true;
            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup);
            ObjectNavigator objDoc = addSingleGroupObjectImplement(document, "Документ", properties, baseGroup);

            addPropertyView(objStore, objArt, properties, baseGroup);
            addPropertyView(objDoc, objArt, properties, baseGroup);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(quantity.property).view));
            addFixedFilter(new CompareFilterNavigator(getPropertyView(documentStore.property).view, Compare.EQUALS, objStore));
        }
    }

    private class SystemNavigatorForm extends NavigatorForm {

        public SystemNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            GroupObjectNavigator group = new GroupObjectNavigator(IDShift(1));

            ObjectNavigator objDoc = new ObjectNavigator(IDShift(1), document, "Документ");
            ObjectNavigator objArt = new ObjectNavigator(IDShift(1), article, "Товар");

            group.add(objDoc);
            group.add(objArt);
            addGroup(group);

            addPropertyView(objDoc, properties, baseGroup);
            addPropertyView(objArt, properties, baseGroup);
            addPropertyView(objDoc, objArt, properties, baseGroup);
            addPropertyView(is(incomeDocument), objDoc);
            addPropertyView(incQuantity, objDoc, objArt);
        }
    }

}
