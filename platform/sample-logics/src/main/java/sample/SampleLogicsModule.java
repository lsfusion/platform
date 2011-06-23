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

    public SampleLogicsModule(BaseLogicsModule<SampleBusinessLogics> baseLM) {
        super("SampleLogicsModule");
        setBaseLogicsModule(baseLM);
    }

    protected AbstractCustomClass document;

    protected ConcreteCustomClass article, descriptedArticle, store, descriptedStore, incomeDocument, outcomeDocument;
    protected ConcreteCustomClass articleGroup, descriptedArticleGroup;

    @Override
    public void initClasses() {
        initBaseClassAliases();

        article = addConcreteClass("article", "Товар", baseClass.named);
        descriptedArticle = addConcreteClass("descriptedArticle", "Товар с описанием", article);

        store = addConcreteClass("store", "Склад", baseClass.named);
        descriptedStore = addConcreteClass("descriptedStore", "Склад с описанием", store);

        document = addAbstractClass("document", "Документ", baseClass.named, baseLM.transaction);
        incomeDocument = addConcreteClass("incomeDocument", "Приход", document);
        outcomeDocument = addConcreteClass("outcomeDocument", "Расход", document);

        articleGroup = addConcreteClass("articleGroup", "Группа товаров", baseLM.baseClass.named);
        descriptedArticleGroup = addConcreteClass("descriptedArticleGroup", "Группа товаров с описанием", articleGroup);
    }

    @Override
    public void initTables() {
    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();
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
        articleDescription = addDProp(baseGroup, "articleDescription", "Описание", StringClass.get(50), descriptedArticle);
        articleGroupDescription = addDProp(baseGroup, "articleGroupDescription", "Описание", StringClass.get(50), descriptedArticleGroup);
        storeDescription = addDProp(baseGroup, "storeDescription", "Описание", StringClass.get(50), descriptedStore);

        documentStore = addDProp(baseGroup, "store", "Склад док-та", store, document);
        quantity = addDProp(baseGroup, "quantity", "Кол-во", DoubleClass.instance, document, article);

        LP storeName = addJProp(baseGroup, "Имя склада", baseLM.name, documentStore, 1);

        incQuantity = addJProp("Кол-во прихода", baseLM.and1, quantity, 1, 2, is(incomeDocument), 1);
        LP outQuantity = addJProp("Кол-во расхода", baseLM.and1, quantity, 1, 2, is(outcomeDocument), 1);

        LP incStoreQuantity = addSGProp(baseGroup, "Прих. по скл.", incQuantity, documentStore, 1, 2);
        LP outStoreQuantity = addSGProp(baseGroup, "Расх. по скл.", outQuantity, documentStore, 1, 2);

        balanceQuantity = addDUProp(baseGroup, "Ост. по скл.", incStoreQuantity, outStoreQuantity);

        addConstraint(addJProp("Остаток должен быть положительным", baseLM.greater2, baseLM.vzero, balanceQuantity, 1, 2), false);

        addJProp(baseGroup, "Ост. по скл. (док.)", balanceQuantity, documentStore, 1, 2);
        LP vone = addCProp("1", IntegerClass.instance, 1);
        LP oneProp = addJProp(baseGroup, "Единица", baseLM.and1, vone, is(document), 1);
        documentsCount = addSGProp(baseGroup, "Количество документов по складу", oneProp, documentStore, 1);
        itemsCount = addSGProp(baseGroup, "Количество единиц товара в документах", quantity, documentStore, 1, 2);

        inStore = addDProp(baseGroup, "inStore", "В ассорт.", LogicalClass.instance, store, article);

        parentGroup = addDProp(baseGroup, "parentGroup", "Родитель", articleGroup, articleGroup);
        articleToGroup = addDProp(baseGroup, "articleToGroup", "Группа товаров", articleGroup, article);
    }

    @Override
    public void initIndexes() {
    }

    FormEntity mainAccountForm, salesArticleStoreForm;

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement primaryData = new NavigatorElement(baseLM.baseElement, "primaryData", "Первичные данные");
            FormEntity documentForm = new DocumentFormEntity(primaryData, "documentForm", "Документ");
            addFormEntity(documentForm);

        NavigatorElement aggregateData = new NavigatorElement(baseLM.baseElement, "aggregateData", "Сводная информация");
            FormEntity storeArticleForm = new StoreArticleFormEntity(aggregateData, "storeArticleForm", "Товары по складам");
            FormEntity systemForm = new SystemFormEntity(aggregateData, "systemForm", "Движение (документ*товар)");
            FormEntity treeStoreArticleForm = new TreeStoreArticleFormEntity(aggregateData, "treeStoreArticleForm", "Товары по складам (дерево)");
            addFormEntity(storeArticleForm);
            addFormEntity(systemForm);
            addFormEntity(treeStoreArticleForm);

//        extIncomeDocument.relevantElements.set(0, extIncDetailForm);
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    private class DocumentFormEntity extends FormEntity {

        public DocumentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

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

        public StoreArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);
//            objArt.groupTo.initClassView = false; //objArt.groupTo.singleViewType = true;
            ObjectEntity objStore = addSingleGroupObject(store, "Склад", baseGroup);
            ObjectEntity objDoc = addSingleGroupObject(document, "Документ", baseGroup);

            addPropertyDraw(objStore, objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(quantity)));
            addFixedFilter(new NotNullFilterEntity(getPropertyObject(balanceQuantity)));
            addFixedFilter(new CompareFilterEntity(getPropertyObject(documentStore), Compare.EQUALS, objStore));
        }
    }

    private class TreeStoreArticleFormEntity extends FormEntity {

        public TreeStoreArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objStore = addSingleGroupObject(store, baseLM.name, storeDescription);
            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, baseLM.name, articleGroupDescription);
            ObjectEntity objArt = addSingleGroupObject(article, baseLM.name, articleDescription);
            ObjectEntity objDoc = addSingleGroupObject(document, baseGroup);

            objArtGroup.groupTo.setIsParents(addPropertyObject(parentGroup, objArtGroup));

            addTreeGroupObject(objStore.groupTo, objArtGroup.groupTo, objArt.groupTo);
//
            addPropertyDraw(objStore, objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);

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

            addPropertyDraw(objDoc, baseGroup);
            addPropertyDraw(objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);
            addPropertyDraw(is(incomeDocument), objDoc);
            addPropertyDraw(incQuantity, objDoc, objArt);
        }
    }

}
