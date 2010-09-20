package sample;

import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.linear.LP;
import platform.server.logics.BusinessLogics;
import platform.server.form.navigator.*;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.auth.User;
import platform.server.classes.AbstractCustomClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.IntegerClass;
import platform.interop.Compare;

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

        article = addConcreteClass("Товар", baseClass.named);
        store = addConcreteClass("Склад", baseClass.named);
        document = addAbstractClass("Документ", baseClass.named, transaction);
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
        LP vone = addCProp("1", IntegerClass.instance, 1);
        LP oneProp = addJProp(baseGroup, "Единица", and1, vone, is(document), 1);
        LP documentsCount = addSGProp(baseGroup, "Количество документов по складу", oneProp, documentStore, 1);
        LP itemsCount = addSGProp(baseGroup, "Количество единиц товара в документах", quantity, documentStore, 1, 2);
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }

    FormEntity mainAccountForm, salesArticleStoreForm;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
            FormEntity documentForm = new DocumentFormEntity(primaryData, 110, "Документ");

        NavigatorElement aggregateData = new NavigatorElement(baseElement, 200, "Сводная информация");
            FormEntity storeArticleForm = new StoreArticleFormEntity(aggregateData, 211, "Товары по складам");
            FormEntity systemForm = new SystemFormEntity(aggregateData, 212, "Движение (документ*товар)");

//        extIncomeDocument.relevantElements.set(0, extIncDetailForm);
    }

    private class DocumentFormEntity extends FormEntity {

        public DocumentFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(document, "Документ", baseGroup);
            addObjectActions(this, objDoc);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);

            addPropertyDraw(objDoc, objArt, baseGroup);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(getPropertyObject(quantity)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class StoreArticleFormEntity extends FormEntity {

        public StoreArticleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);
//            objArt.groupTo.initClassView = false; //objArt.groupTo.singleViewType = true;
            ObjectEntity objStore = addSingleGroupObject(store, "Склад", baseGroup);
            ObjectEntity objDoc = addSingleGroupObject(document, "Документ", baseGroup);

            addPropertyDraw(objStore, objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(quantity)));
            addFixedFilter(new CompareFilterEntity(getPropertyObject(documentStore), Compare.EQUALS, objStore));
        }
    }

    private class SystemFormEntity extends FormEntity {

        public SystemFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            GroupObjectEntity group = new GroupObjectEntity(genID());

            ObjectEntity objDoc = new ObjectEntity(genID(), document, "Документ");
            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");

            group.add(objDoc);
            group.add(objArt);
            addGroup(group);

            addPropertyDraw(objDoc, baseGroup);
            addPropertyDraw(objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);
            addPropertyDraw(is(incomeDocument), objDoc);
            addPropertyDraw(incQuantity, objDoc, objArt);
        }
    }

}
