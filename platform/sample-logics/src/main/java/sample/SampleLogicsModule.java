package sample;

import net.sf.jasperreports.engine.JRException;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LP;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;

/**
 * User: DAle
 * Date: 20.05.11
 * Time: 13:57
 */

public class SampleLogicsModule extends LogicsModule {
    private BaseLogicsModule<SampleBusinessLogics> LM;

    public SampleLogicsModule(BaseLogicsModule<SampleBusinessLogics> LM) {
        this.LM = LM;
    }

    protected AbstractCustomClass document;

    protected ConcreteCustomClass article, descriptedArticle, store, descriptedStore, incomeDocument, outcomeDocument;
    protected ConcreteCustomClass articleGroup, descriptedArticleGroup;

    @Override
    public void initClasses() {
        article = LM.addConcreteClass("article", "Товар", LM.baseClass.named);
        descriptedArticle = LM.addConcreteClass("descriptedArticle", "Товар с описанием", article);

        store = LM.addConcreteClass("store", "Склад", LM.baseClass.named);
        descriptedStore = LM.addConcreteClass("descriptedStore", "Склад с описанием", store);

        document = LM.addAbstractClass("document", "Документ", LM.baseClass.named, LM.transaction);
        incomeDocument = LM.addConcreteClass("incomeDocument", "Приход", document);
        outcomeDocument = LM.addConcreteClass("outcomeDocument", "Расход", document);

        articleGroup = LM.addConcreteClass("articleGroup", "Группа товаров", LM.baseClass.named);
        descriptedArticleGroup = LM.addConcreteClass("descriptedArticleGroup", "Группа товаров с описанием", articleGroup);
    }

    @Override
    public void initTables() {
    }

    @Override
    public void initGroups() {
    }

    private LP documentsCount;
    private LP itemsCount;
    private LP articleDescription;
    private LP articleGroupDescription;
    private LP storeDescription;

    protected LP quantity, documentStore;
    protected LP balanceQuantity, incQuantity;
    protected LP inStore, parentGroup, articleToGroup;

    @Override
    public void initProperties() {
        articleDescription = LM.addDProp(LM.baseGroup, "articleDescription", "Описание", StringClass.get(50), descriptedArticle);
        articleGroupDescription = LM.addDProp(LM.baseGroup, "articleGroupDescription", "Описание", StringClass.get(50), descriptedArticleGroup);
        storeDescription = LM.addDProp(LM.baseGroup, "storeDescription", "Описание", StringClass.get(50), descriptedStore);

        documentStore = LM.addDProp(LM.baseGroup, "store", "Склад док-та", store, document);
        quantity = LM.addDProp(LM.baseGroup, "quantity", "Кол-во", DoubleClass.instance, document, article);

        LP storeName = LM.addJProp(LM.baseGroup, "Имя склада", LM.name, documentStore, 1);

        incQuantity = LM.addJProp("Кол-во прихода", LM.and1, quantity, 1, 2, LM.is(incomeDocument), 1);
        LP outQuantity = LM.addJProp("Кол-во расхода", LM.and1, quantity, 1, 2, LM.is(outcomeDocument), 1);

        LP incStoreQuantity = LM.addSGProp(LM.baseGroup, "Прих. по скл.", incQuantity, documentStore, 1, 2);
        LP outStoreQuantity = LM.addSGProp(LM.baseGroup, "Расх. по скл.", outQuantity, documentStore, 1, 2);

        balanceQuantity = LM.addDUProp(LM.baseGroup, "Ост. по скл.", incStoreQuantity, outStoreQuantity);

        LM.addConstraint(LM.addJProp("Остаток должен быть положительным", LM.greater2, LM.vzero, balanceQuantity, 1, 2), false);

        LM.addJProp(LM.baseGroup, "Ост. по скл. (док.)", balanceQuantity, documentStore, 1, 2);
        LP vone = LM.addCProp("1", IntegerClass.instance, 1);
        LP oneProp = LM.addJProp(LM.baseGroup, "Единица", LM.and1, vone, LM.is(document), 1);
        documentsCount = LM.addSGProp(LM.baseGroup, "Количество документов по складу", oneProp, documentStore, 1);
        itemsCount = LM.addSGProp(LM.baseGroup, "Количество единиц товара в документах", quantity, documentStore, 1, 2);

        inStore = LM.addDProp(LM.baseGroup, "inStore", "В ассорт.", LogicalClass.instance, store, article);

        parentGroup = LM.addDProp(LM.baseGroup, "parentGroup", "Родитель", articleGroup, articleGroup);
        articleToGroup = LM.addDProp(LM.baseGroup, "articleToGroup", "Группа товаров", articleGroup, article);
    }

    @Override
    public void initIndexes() {
    }

    FormEntity mainAccountForm, salesArticleStoreForm;

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement primaryData = new NavigatorElement(LM.baseElement, "primaryData", "Первичные данные");
            FormEntity documentForm = new DocumentFormEntity(primaryData, "documentForm", "Документ");
            LM.addFormEntity(documentForm);

        NavigatorElement aggregateData = new NavigatorElement(LM.baseElement, "aggregateData", "Сводная информация");
            FormEntity storeArticleForm = new StoreArticleFormEntity(aggregateData, "storeArticleForm", "Товары по складам");
            FormEntity systemForm = new SystemFormEntity(aggregateData, "systemForm", "Движение (документ*товар)");
            FormEntity treeStoreArticleForm = new TreeStoreArticleFormEntity(aggregateData, "treeStoreArticleForm", "Товары по складам (дерево)");
            LM.addFormEntity(storeArticleForm);
            LM.addFormEntity(systemForm);
            LM.addFormEntity(treeStoreArticleForm);

//        extIncomeDocument.relevantElements.set(0, extIncDetailForm);
    }

    private class DocumentFormEntity extends FormEntity {

        public DocumentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDoc = addSingleGroupObject(document, "Документ", LM.baseGroup);
            LM.addObjectActions(this, objDoc);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", LM.baseGroup);

            addPropertyDraw(objDoc, objArt, LM.baseGroup);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(getPropertyObject(quantity)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class StoreArticleFormEntity extends FormEntity {

        public StoreArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", LM.baseGroup);
//            objArt.groupTo.initClassView = false; //objArt.groupTo.singleViewType = true;
            ObjectEntity objStore = addSingleGroupObject(store, "Склад", LM.baseGroup);
            ObjectEntity objDoc = addSingleGroupObject(document, "Документ", LM.baseGroup);

            addPropertyDraw(objStore, objArt, LM.baseGroup);
            addPropertyDraw(objDoc, objArt, LM.baseGroup);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(quantity)));
            addFixedFilter(new NotNullFilterEntity(getPropertyObject(balanceQuantity)));
            addFixedFilter(new CompareFilterEntity(getPropertyObject(documentStore), Compare.EQUALS, objStore));
        }
    }

    private class TreeStoreArticleFormEntity extends FormEntity {

        public TreeStoreArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objStore = addSingleGroupObject(store, LM.name, storeDescription);
            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, LM.name, articleGroupDescription);
            ObjectEntity objArt = addSingleGroupObject(article, LM.name, articleDescription);
            ObjectEntity objDoc = addSingleGroupObject(document, LM.baseGroup);

            objArtGroup.groupTo.setIsParents(addPropertyObject(parentGroup, objArtGroup));

            addTreeGroupObject(objStore.groupTo, objArtGroup.groupTo, objArt.groupTo);
//
            addPropertyDraw(objStore, objArt, LM.baseGroup);
            addPropertyDraw(objDoc, objArt, LM.baseGroup);

//            addFixedFilter(new NotNullFilterEntity(getPropertyObject(quantity)));
//            addFixedFilter(new NotNullFilterEntity(getPropertyObject(balanceQuantity)));

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(inStore)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleToGroup, objArt), Compare.EQUALS, objArtGroup));
            addFixedFilter(new CompareFilterEntity(getPropertyObject(documentStore), Compare.EQUALS, objStore));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
//            design.get(getPropertyDraw(documentStore)).autoHide = true;

            return design;
        }
    }

    private class SystemFormEntity extends FormEntity {

        public SystemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            GroupObjectEntity group = new GroupObjectEntity(genID());

            ObjectEntity objDoc = new ObjectEntity(genID(), document, "Документ");
            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");

            group.add(objDoc);
            group.add(objArt);
            addGroup(group);

            addPropertyDraw(objDoc, LM.baseGroup);
            addPropertyDraw(objArt, LM.baseGroup);
            addPropertyDraw(objDoc, objArt, LM.baseGroup);
            addPropertyDraw(LM.is(incomeDocument), objDoc);
            addPropertyDraw(incQuantity, objDoc, objArt);
        }
    }

}
