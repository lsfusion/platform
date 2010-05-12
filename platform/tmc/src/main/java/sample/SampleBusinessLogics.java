package sample;

import platform.server.data.sql.DataAdapter;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.linear.LP;
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
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import net.sf.jasperreports.engine.JRException;

import javax.swing.*;

public class SampleBusinessLogics extends BusinessLogics<SampleBusinessLogics> {

    public SampleBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
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

            ObjectNavigator objDoc = addSingleGroupObjectImplement(document, "Документ", controls, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", controls, baseGroup);

            addControlView(objDoc, objArt, controls, baseGroup);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyImplement(quantity)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {

        public StoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", controls, baseGroup);
//            objArt.groupTo.initClassView = false; //objArt.groupTo.singleViewType = true;
            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", controls, baseGroup);
            ObjectNavigator objDoc = addSingleGroupObjectImplement(document, "Документ", controls, baseGroup);

            addControlView(objStore, objArt, controls, baseGroup);
            addControlView(objDoc, objArt, controls, baseGroup);

            addFixedFilter(new NotNullFilterNavigator(getPropertyImplement(quantity)));
            addFixedFilter(new CompareFilterNavigator(getPropertyImplement(documentStore), Compare.EQUALS, objStore));
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

            addControlView(objDoc, controls, baseGroup);
            addControlView(objArt, controls, baseGroup);
            addControlView(objDoc, objArt, controls, baseGroup);
            addControlView(is(incomeDocument), objDoc);
            addControlView(incQuantity, objDoc, objArt);
        }
    }

}
