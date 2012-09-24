package sample;

import platform.interop.Compare;
import platform.interop.action.ConfirmClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.LogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.UserActionProperty;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

/**
 * User: DAle
 * Date: 20.05.11
 * Time: 13:57
 */

public class SampleLogicsModule extends LogicsModule {

//    private LCP inGroup;
//    private LCP inRecGroup;

    public SampleLogicsModule(BaseLogicsModule<SampleBusinessLogics> baseLM) {
        super("SampleLogicsModule");
        setBaseLogicsModule(baseLM);
    }

    protected AbstractCustomClass document;

    protected ConcreteCustomClass article, descriptedArticle, store, descriptedStore, incomeDocument, outcomeDocument;
    protected ConcreteCustomClass articleGroup, descriptedArticleGroup;

    @Override
    public void initModule() {
    }

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

    private LCP documentsCount;
    private LCP itemsCount;
    private LCP articleDescription;
    private LCP articleGroupDescription;
    private LCP storeDescription;

    protected LCP quantity, documentStore;
    protected LCP balanceQuantity, incQuantity;
    protected LCP inStore, parentGroup, articleToGroup;

    private LAP annoyingChangeArticleDescriptionAction;

    @Override
    public void initProperties() {
        articleDescription = addDProp(baseGroup, "articleDescription", "Описание", StringClass.get(50), descriptedArticle);
        articleGroupDescription = addDProp(baseGroup, "articleGroupDescription", "Описание", StringClass.get(50), descriptedArticleGroup);
        storeDescription = addDProp(baseGroup, "storeDescription", "Описание", StringClass.get(50), descriptedStore);

        documentStore = addDProp(baseGroup, "store", "Склад док-та", store, document);
        quantity = addDProp(baseGroup, "quantity", "Кол-во", DoubleClass.instance, document, article);

        LCP storeName = addJProp(baseGroup, "Имя склада", baseLM.name, documentStore, 1);

        incQuantity = addJProp("Кол-во прихода", baseLM.and1, quantity, 1, 2, is(incomeDocument), 1);
        LCP outQuantity = addJProp("Кол-во расхода", baseLM.and1, quantity, 1, 2, is(outcomeDocument), 1);

        LCP incStoreQuantity = addSGProp(baseGroup, "Прих. по скл.", incQuantity, documentStore, 1, 2);
        LCP outStoreQuantity = addSGProp(baseGroup, "Расх. по скл.", outQuantity, documentStore, 1, 2);

        balanceQuantity = addDUProp(baseGroup, "Ост. по скл.", incStoreQuantity, outStoreQuantity);

        addConstraint(addJProp("Остаток должен быть положительным", baseLM.greater2, baseLM.vzero, balanceQuantity, 1, 2), false);

        addJProp(baseGroup, "Ост. по скл. (док.)", balanceQuantity, documentStore, 1, 2);
        LCP vone = addCProp("1", IntegerClass.instance, 1);
        LCP oneProp = addJProp(baseGroup, "Единица", baseLM.and1, vone, is(document), 1);
        documentsCount = addSGProp(baseGroup, "Количество документов по складу", oneProp, documentStore, 1);
        itemsCount = addSGProp(baseGroup, "Количество единиц товара в документах", quantity, documentStore, 1, 2);

        inStore = addDProp(baseGroup, "inStore", "В ассорт.", LogicalClass.instance, store, article);

        parentGroup = addDProp(baseGroup, "parentGroup", "Родитель", articleGroup, articleGroup);
        articleToGroup = addDProp(baseGroup, "articleToGroup", "Группа товаров", articleGroup, article);

//        inGroup = addDProp(baseGroup, "inGroup", "Входит", LogicalClass.instance, articleGroup, articleGroup);
//        inRecGroup = addRProp(baseGroup, "inRecGroup", true, "Входит (рек)", Cycle.NO, 2, addJProp(baseLM.and1, is(articleGroup), 1, baseLM.equals2, 1, 2), 1, 2, inGroup, 3, 2);

        annoyingChangeArticleDescriptionAction = addProperty(null, new LAP(new AnnoyingChangeArticleDescriptionAction(genSID())));

        initNavigators();
    }

    private class AnnoyingChangeArticleDescriptionAction extends UserActionProperty {
        protected AnnoyingChangeArticleDescriptionAction(String sID) {
            super(sID, descriptedArticle);
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            for (int i = 0; i < 5; ++i) {
                if (JOptionPane.OK_OPTION != (Integer)context.requestUserInteraction(new ConfirmClientAction("Попытка №" + i, "Вы уверены, что хотите изменить это свойство?"))) {
                    context.requestUserInteraction(new MessageClientAction("Too bad :(", ":("));
                    return;
                }
            }

            ObjectValue result = context.requestUserData(IntegerClass.instance, null);
            if (result!=null) {
                Object value = result.getValue();
                articleDescription.change(value == null ? null : "Descr # " + value, context.getSession(), context.getSingleKeyValue());
            }
        }
    }

    @Override
    public void initIndexes() {
    }

    private void initNavigators() {

        NavigatorElement primaryData = addNavigatorElement(baseLM.baseElement, "primaryData", "Первичные данные");
            FormEntity documentForm = new DocumentFormEntity(primaryData, "documentForm", "Документ");
            addFormEntity(documentForm);

        NavigatorElement aggregateData = addNavigatorElement(baseLM.baseElement, "aggregateData", "Сводная информация");
            FormEntity storeArticleForm = new StoreArticleFormEntity(aggregateData, "storeArticleForm", "Товары по складам");
            FormEntity systemForm = new SystemFormEntity(aggregateData, "systemForm", "Движение (документ*товар)");
            FormEntity treeStoreArticleForm = new TreeStoreArticleFormEntity(aggregateData, "treeStoreArticleForm", "Товары по складам (дерево)");
            FormEntity treeForm = new TreeFormEntity(aggregateData, "treeForm", "Дерево групп");
            addFormEntity(storeArticleForm);
            addFormEntity(systemForm);
            addFormEntity(treeStoreArticleForm);
            addFormEntity(treeForm);

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
                                  new NotNullFilterEntity(getCalcPropertyObject(quantity)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class StoreArticleFormEntity extends FormEntity {

        private final ObjectEntity objArt;

        public StoreArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objArt = addSingleGroupObject(descriptedArticle, "Товар", baseGroup);
//            objArt.groupTo.initClassView = false; //objArt.groupTo.singleViewType = true;
            ObjectEntity objStore = addSingleGroupObject(store, "Склад", baseGroup);
            ObjectEntity objDoc = addSingleGroupObject(document, "Документ", baseGroup);

            addPropertyDraw(objStore, objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);

            PropertyDrawEntity descriptionDraw = getPropertyDraw(articleDescription, objArt);
            descriptionDraw.setMouseAction("annoyingChange");
            descriptionDraw.setKeyEditAction(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "annoyingChange", addPropertyObject(annoyingChangeArticleDescriptionAction, objArt));

            descriptionDraw.setContextMenuAction("Annoying change", "annoyingChange");
            descriptionDraw.setContextMenuAction("Change annoyingly", "annoyingChange");

            addFixedFilter(new NotNullFilterEntity(getCalcPropertyObject(quantity)));
            addFixedFilter(new NotNullFilterEntity(getCalcPropertyObject(balanceQuantity)));
            addFixedFilter(new CompareFilterEntity(getCalcPropertyObject(documentStore), Compare.EQUALS, objStore));
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

            addFixedFilter(new NotNullFilterEntity(getCalcPropertyObject(inStore)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleToGroup, objArt), Compare.EQUALS, objArtGroup));
            addFixedFilter(new CompareFilterEntity(getCalcPropertyObject(documentStore), Compare.EQUALS, objStore));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
//            design.get(getPropertyDraw(documentStore)).autoHide = true;

            return design;
        }
    }

    private class TreeFormEntity extends FormEntity {

        public TreeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objArtGroup1 = addSingleGroupObject(articleGroup, baseLM.name, articleGroupDescription);
            addObjectActions(this, objArtGroup1);

            ObjectEntity objArtGroup2 = addSingleGroupObject(articleGroup, baseLM.name, articleGroupDescription);
            addObjectActions(this, objArtGroup2);

//            addPropertyDraw(inGroup, objArtGroup1, objArtGroup2);
            addPropertyDraw(parentGroup, objArtGroup1, objArtGroup2);
//            addPropertyDraw(inRecGroup, objArtGroup1, objArtGroup2);
//            addPropertyDraw(inRecGroup, objArtGroup2, objArtGroup1);

//            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
//            filterGroup.addFilter(new RegularFilterEntity(genID(),
//                    new NotNullFilterEntity(getPropertyObject(inGroup)),
//                    "В группе",
//                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
//            filterGroup.addFilter(new RegularFilterEntity(genID(),
//                    new NotNullFilterEntity(getPropertyObject(inRecGroup)),
//                    "В рек. группе",
//                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK)));
//            addRegularFilterGroup(filterGroup);

//            addHintsNoUpdate(inRecGroup);
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
            addGroupObject(group);

            addPropertyDraw(objDoc, baseGroup);
            addPropertyDraw(objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);
            addPropertyDraw(is(incomeDocument), objDoc);
            addPropertyDraw(incQuantity, objDoc, objArt);
        }
    }

}
