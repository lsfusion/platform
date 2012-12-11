package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.KeyStrokes;
import platform.interop.PropertyEditType;
import platform.interop.action.LogOutClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.action.UserChangedClientAction;
import platform.interop.action.UserReloginClientAction;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.SQLSession;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.PartitionType;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.window.AbstractWindow;
import platform.server.form.window.NavigatorWindow;
import platform.server.form.window.ToolBarNavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.actions.flow.ApplyActionProperty;
import platform.server.logics.property.actions.flow.BreakActionProperty;
import platform.server.logics.property.actions.flow.CancelActionProperty;
import platform.server.logics.property.actions.flow.ReturnActionProperty;
import platform.server.logics.property.actions.form.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.table.TableFactory;
import platform.server.session.DataSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static platform.server.logics.PropertyUtils.mapCalcImplement;
import static platform.server.logics.PropertyUtils.readCalcImplements;
import static platform.server.logics.ServerResourceBundle.getString;

public class ReflectionLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    Logger logger;
    T BL;

    public T getBL(){
        return BL;
    }

    public ConcreteCustomClass abstractGroup;
    public ConcreteCustomClass navigatorElement;
    public ConcreteCustomClass navigatorAction;
    public ConcreteCustomClass form;
    public ConcreteCustomClass propertyDraw;
    public StaticCustomClass propertyDrawShowStatus;
    public ConcreteCustomClass table;
    public ConcreteCustomClass tableKey;
    public ConcreteCustomClass tableColumn;
    public ConcreteCustomClass dropColumn;
    public ConcreteCustomClass property;
    
    public LCP captionAbstractGroup;
    public LCP navigatorElementCaption;
    public LCP parentAbstractGroup;
    public LCP numberAbstractGroup;
    public LCP SIDAbstractGroup;
    public LCP SIDToAbstractGroup;

    public LCP parentProperty;
    public LCP numberProperty;
    public LCP SIDProperty;
    public LCP loggableProperty;
    public LCP userLoggableProperty;
    public LCP storedProperty;
    public LCP isSetNotNullProperty;
    public LCP signatureProperty;
    public LCP returnProperty;
    public LCP classProperty;
    public LCP captionProperty;
    public LCP SIDToProperty;

    public LCP navigatorElementSID;
    public LCP numberNavigatorElement;
    
    public LCP SIDToNavigatorElement;
    public LCP parentNavigatorElement;
    public LCP isNavigatorElement;
    public LCP isForm;
    public LCP isNavigatorAction;

    public LCP groupObjectSID;
    public LCP navigatorElementGroupObject;
    public LCP sidNavigatorElementGroupObject;
    public LCP SIDNavigatorElementSIDGroupObjectToGroupObject;

    public LCP propertyDrawSID;
    public LCP captionPropertyDraw;
    public LCP SIDToPropertyDraw;
    public LCP formPropertyDraw;
    public LCP groupObjectPropertyDraw;
    public LCP SIDNavigatorElementSIDPropertyDrawToPropertyDraw;

    public LCP showPropertyDraw;
    public LCP nameShowPropertyDraw;
    public LCP showOverridePropertyDrawCustomUser;

    public LCP nameShowPropertyDrawCustomUser;
    public LCP showPropertyDrawCustomUser;
    public LCP nameShowOverridePropertyDrawCustomUser;

    public LCP columnWidthPropertyDrawCustomUser;
    public LCP columnWidthPropertyDraw;
    public LCP columnWidthOverridePropertyDrawCustomUser;
    public LCP columnOrderPropertyDrawCustomUser;
    public LCP columnOrderPropertyDraw;
    public LCP columnOrderOverridePropertyDrawCustomUser;
    public LCP columnSortPropertyDrawCustomUser;
    public LCP columnSortPropertyDraw;
    public LCP columnSortOverridePropertyDrawCustomUser;
    public LCP columnAscendingSortPropertyDrawCustomUser;
    public LCP columnAscendingSortPropertyDraw;
    public LCP columnAscendingSortOverridePropertyDrawCustomUser;
    public LCP hasUserPreferencesGroupObject;
    public LCP hasUserPreferencesGroupObjectCustomUser;
    public LCP hasUserPreferencesOverrideGroupObjectCustomUser;

    public LCP sidTable;
    public LCP sidToTable;
    public LCP rowsTable;
    public LCP sparseColumnsTable;
    public LCP tableTableKey;
    public LCP sidTableKey;
    public LCP sidToTableKey;
    public LCP classTableKey;
    public LCP nameTableKey;
    public LCP quantityTableKey;
    public LCP tableTableColumn;
    public LCP sidTableColumn;
    public LCP sidToTableColumn;
    public LCP propertyTableColumn;
    public LCP propertyNameTableColumn;

    public LCP quantityTableColumn;
    public LCP notNullQuantityTableColumn;
    public LCP perсentNotNullTableColumn;
    public LAP recalculateAggregationTableColumn;

    public LCP<?> sidTableDropColumn;
    public LCP<?> sidDropColumn;
    public LCP sidToDropColumn;

    public LCP timeDropColumn;
    public LCP revisionDropColumn;
    public LAP dropDropColumn;
    
    public LCP connectionFormCount;

    public final StringClass navigatorElementSIDClass = StringClass.get(50);
    public final StringClass navigatorElementCaptionClass = StringClass.get(250);
    public final StringClass propertySIDValueClass = StringClass.get(100);
    public final StringClass propertyCaptionValueClass = StringClass.get(250);
    public final StringClass propertySignatureValueClass = StringClass.get(100);
    public final LogicalClass propertyLoggableValueClass = LogicalClass.instance;
    public final LogicalClass propertyStoredValueClass = LogicalClass.instance;
    public final LogicalClass propertyIsSetNotNullValueClass = LogicalClass.instance;
    public final StringClass loginValueClass = StringClass.get(100);

    public ReflectionLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("Reflection", "Reflection");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.logger = logger;
    }
    @Override
    public void initModuleDependencies() {
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        abstractGroup = addConcreteClass("abstractGroup", getString("logics.property.group"), baseLM.baseClass);
        navigatorElement = addConcreteClass("navigatorElement", getString("logics.navigator.element"), baseLM.baseClass);
        navigatorAction = addConcreteClass("navigatorAction", getString("logics.forms.action"), navigatorElement);
        form = addConcreteClass("form", getString("logics.forms.form"), navigatorElement);
        propertyDraw = addConcreteClass("propertyDraw", getString("logics.property.draw"), baseLM.baseClass);
        propertyDrawShowStatus = addStaticClass("propertyDrawShowStatus", getString("logics.forms.property.show"),
                new String[]{"Show", "Hide"},
                new String[]{getString("logics.property.draw.show"), getString("logics.property.draw.hide")});
        table = addConcreteClass("table", getString("logics.tables.table"), baseLM.baseClass);
        tableKey = addConcreteClass("tableKey", getString("logics.tables.key"), baseLM.baseClass);
        tableColumn = addConcreteClass("tableColumn", getString("logics.tables.column"), baseLM.baseClass);
        dropColumn = addConcreteClass("dropColumn", getString("logics.tables.deleted.column"), baseLM.baseClass);
        property = addConcreteClass("property", getString("logics.property"), baseLM.baseClass);


    }

    @Override
    public void initGroups() {

    }

    @Override
    public void initTables() {
        addTable("formPropertyDraw", form, propertyDraw);
        addTable("navigatorElement", navigatorElement);
        addTable("connectionNavigatorElement", BL.systemEventsLM.connection, navigatorElement);
        addTable("userRoleNavigatorElement", BL.securityLM.userRole, navigatorElement);
        addTable("propertyDraw", propertyDraw);
        addTable("propertyDrawCustomUser", propertyDraw, baseLM.customUser);
        addTable("abstractGroup", abstractGroup);
        addTable("tables", table);
        addTable("tableKey", tableKey);
        addTable("tableColumn", tableColumn);
        addTable("dropColumn", dropColumn);
        addTable("property", property);
    }

    @Override
    public void initProperties() {

        // ------------------------------------------------- Логическая модель ------------------------------------ //

        // ------- Доменная логика --------- //

        // Группы свойства
        captionAbstractGroup = addDProp(BL.LM.baseGroup, "captionAbstractGroup", getString("logics.name"), propertyCaptionValueClass, abstractGroup);
        navigatorElementCaption = addDProp(BL.LM.baseGroup, "navigatorElementCaption", getString("logics.forms.name"), navigatorElementCaptionClass, navigatorElement);
        parentAbstractGroup = addDProp(BL.LM.baseGroup, "parentAbstractGroup", getString("logics.property.group"), abstractGroup, abstractGroup);
        numberAbstractGroup = addDProp(BL.LM.baseGroup, "numberAbstractGroup", getString("logics.property.number"), IntegerClass.instance, abstractGroup);
        SIDAbstractGroup = addDProp(BL.LM.baseGroup, "SIDAbstractGroup", getString("logics.property.sid"), propertySIDValueClass, abstractGroup);
        SIDToAbstractGroup = addAGProp("SIDToAbstractGroup", getString("logics.property"), SIDAbstractGroup);

        // Свойства
        parentProperty = addDProp(BL.LM.baseGroup, "parentProperty", getString("logics.property.group"), abstractGroup, property);
        numberProperty = addDProp(BL.LM.baseGroup, "numberProperty", getString("logics.property.number"), IntegerClass.instance, property);
        SIDProperty = addDProp(BL.LM.baseGroup, "SIDProperty", getString("logics.property.sid"), propertySIDValueClass, property);
        loggableProperty = addDProp(BL.LM.baseGroup, "loggableProperty", getString("logics.property.loggable"), LogicalClass.instance, property);
        userLoggableProperty = addDProp(BL.LM.baseGroup, "userLoggableProperty", getString("logics.property.user.loggable"), LogicalClass.instance, property);
        storedProperty = addDProp(BL.LM.baseGroup, "storedProperty", getString("logics.property.stored"), LogicalClass.instance, property);
        isSetNotNullProperty = addDProp(BL.LM.baseGroup, "isSetNotNullProperty", getString("logics.property.set.not.null"), LogicalClass.instance, property);
        signatureProperty = addDProp(BL.LM.baseGroup, "signatureProperty", getString("logics.property.signature"), propertySignatureValueClass, property);
        returnProperty = addDProp(BL.LM.baseGroup, "returnProperty", getString("logics.property.return"), propertySignatureValueClass, property);
        classProperty = addDProp(BL.LM.baseGroup, "classProperty", getString("logics.property.class"), propertySignatureValueClass, property);
        captionProperty = addDProp(BL.LM.baseGroup, "captionProperty", getString("logics.property.caption"), propertyCaptionValueClass, property);
        SIDToProperty = addAGProp("SIDToProperty", getString("logics.property"), SIDProperty);

        // ------- Логика представлений --------- //

        // Навигатор
        navigatorElementSID = addDProp(BL.LM.baseGroup, "navigatorElementSID", getString("logics.forms.code"), navigatorElementSIDClass, navigatorElement);
        numberNavigatorElement = addDProp(BL.LM.baseGroup, "numberNavigatorElement", getString("logics.number"), IntegerClass.instance, navigatorElement);
        SIDToNavigatorElement = addAGProp("SIDToNavigatorElement", getString("logics.forms.form"), navigatorElementSID);
        parentNavigatorElement = addDProp("parentNavigatorElement", getString("logics.forms.parent.form"), navigatorElement, navigatorElement);
        isNavigatorElement = addJProp("isNavigatorElement", and(true, true), is(navigatorElement), 1, is(form), 1, is(navigatorAction), 1);
        isForm = is(form);
        isNavigatorAction = is(navigatorAction);

        // ----- Формы ---- //
        // Группа объектов
        groupObjectSID = addDProp(BL.LM.baseGroup, "groupObjectSID", getString("logics.group.object.sid"), propertySIDValueClass, BL.securityLM.groupObject);
        navigatorElementGroupObject = addDProp(BL.LM.baseGroup, "navigatorElementGroupObject", getString("logics.navigator.element"), navigatorElement, BL.securityLM.groupObject);
        sidNavigatorElementGroupObject = addJProp(BL.LM.baseGroup, "sidNavigatorElementGroupObject", navigatorElementSID, navigatorElementGroupObject, 1);
        SIDNavigatorElementSIDGroupObjectToGroupObject = addAGProp(BL.LM.baseGroup, "SIDToGroupObject", getString("logics.group.object"), groupObjectSID, sidNavigatorElementGroupObject);

        // PropertyDraw
        propertyDrawSID = addDProp(BL.LM.baseGroup, "propertyDrawSID", getString("logics.forms.property.draw.code"), propertySIDValueClass, propertyDraw);
        captionPropertyDraw = addDProp(BL.LM.baseGroup, "captionPropertyDraw", getString("logics.forms.property.draw.caption"), propertyCaptionValueClass, propertyDraw);
        formPropertyDraw = addDProp(BL.LM.baseGroup, "formPropertyDraw", getString("logics.forms.form"), form, propertyDraw);
        groupObjectPropertyDraw = addDProp(BL.LM.baseGroup, "groupObjectPropertyDraw", getString("logics.group.object"), BL.securityLM.groupObject, propertyDraw);
        SIDToPropertyDraw = addAGProp(BL.LM.baseGroup, "SIDToPropertyDraw", getString("logics.property.draw"), formPropertyDraw, propertyDrawSID);
        // todo : это свойство должно быть для форм, а не навигаторов
        SIDNavigatorElementSIDPropertyDrawToPropertyDraw = addJProp(BL.LM.baseGroup, "SIDNavigatorElementSIDPropertyDrawToPropertyDraw", getString("logics.forms.code"), SIDToPropertyDraw, SIDToNavigatorElement, 1, 2);

        // UserPreferences
        showPropertyDraw = addDProp(BL.LM.baseGroup, "showPropertyDraw", getString("logics.forms.property.show"), propertyDrawShowStatus, propertyDraw);
        showPropertyDrawCustomUser = addDProp(BL.LM.baseGroup, "showPropertyDrawCustomUser", getString("logics.forms.property.show.user"), propertyDrawShowStatus, propertyDraw, BL.LM.customUser);
        showOverridePropertyDrawCustomUser = addSUProp(BL.LM.baseGroup, "showOverridePropertyDrawCustomUser", getString("logics.forms.property.show"), Union.OVERRIDE, addJProp(BL.LM.and1, showPropertyDraw, 1, is(BL.LM.customUser), 2), showPropertyDrawCustomUser);

        nameShowPropertyDraw = addJProp(BL.LM.baseGroup, "nameShowPropertyDraw", getString("logics.forms.property.show"), BL.LM.name, showPropertyDraw, 1);
        nameShowPropertyDraw.setPreferredWidth(50);
        nameShowPropertyDrawCustomUser = addJProp(BL.LM.baseGroup, "nameShowPropertyDrawCustomUser", getString("logics.forms.property.show.user"), BL.LM.name, showPropertyDrawCustomUser, 1, 2);
        nameShowPropertyDrawCustomUser.setPreferredWidth(50);
        nameShowOverridePropertyDrawCustomUser = addJProp(BL.LM.baseGroup, "nameShowOverridePropertyDrawCustomUser", getString("logics.forms.property.show"), BL.LM.name, showOverridePropertyDrawCustomUser, 1, 2);

        columnWidthPropertyDrawCustomUser = addDProp(BL.LM.baseGroup, "columnWidthPropertyDrawCustomUser", getString("logics.forms.property.width.user"), IntegerClass.instance, propertyDraw, BL.LM.customUser);
        columnWidthPropertyDraw = addDProp(BL.LM.baseGroup, "columnWidthPropertyDraw", getString("logics.forms.property.width"), IntegerClass.instance, propertyDraw);
        columnWidthOverridePropertyDrawCustomUser = addSUProp(BL.LM.baseGroup, "columnWidthOverridePropertyDrawCustomUser", getString("logics.forms.property.width"), Union.OVERRIDE, addJProp(BL.LM.and1, columnWidthPropertyDraw, 1, is(BL.LM.customUser), 2), columnWidthPropertyDrawCustomUser);

        columnOrderPropertyDrawCustomUser = addDProp(BL.LM.baseGroup, "columnOrderPropertyDrawCustomUser", getString("logics.forms.property.order.user"), IntegerClass.instance, propertyDraw, BL.LM.customUser);
        columnOrderPropertyDraw = addDProp(BL.LM.baseGroup, "columnOrderPropertyDraw", getString("logics.forms.property.order"), IntegerClass.instance, propertyDraw);
        columnOrderOverridePropertyDrawCustomUser = addSUProp(BL.LM.baseGroup, "columnOrderOverridePropertyDrawCustomUser", getString("logics.forms.property.order"), Union.OVERRIDE, addJProp(BL.LM.and1, columnOrderPropertyDraw, 1, is(BL.LM.customUser), 2), columnOrderPropertyDrawCustomUser);

        columnSortPropertyDrawCustomUser = addDProp(BL.LM.baseGroup, "columnSortPropertyDrawCustomUser", getString("logics.forms.property.sort.user"), IntegerClass.instance, propertyDraw, BL.LM.customUser);
        columnSortPropertyDraw = addDProp(BL.LM.baseGroup, "columnSortPropertyDraw", getString("logics.forms.property.sort"), IntegerClass.instance, propertyDraw);
        columnSortOverridePropertyDrawCustomUser = addSUProp(BL.LM.baseGroup, "columnSortOverridePropertyDrawCustomUser", getString("logics.forms.property.sort"), Union.OVERRIDE, addJProp(BL.LM.and1, columnSortPropertyDraw, 1, is(BL.LM.customUser), 2), columnSortPropertyDrawCustomUser);

        columnAscendingSortPropertyDrawCustomUser = addDProp(BL.LM.baseGroup, "columnAscendingSortPropertyDrawCustomUser", getString("logics.forms.property.ascending.sort.user"), LogicalClass.instance, propertyDraw, BL.LM.customUser);
        columnAscendingSortPropertyDraw = addDProp(BL.LM.baseGroup, "columnAscendingSortPropertyDraw", getString("logics.forms.property.ascending.sort"), LogicalClass.instance, propertyDraw);
        columnAscendingSortOverridePropertyDrawCustomUser = addSUProp(BL.LM.baseGroup, "columnAscendingSortOverridePropertyDrawCustomUser", getString("logics.forms.property.ascending.sort"), Union.OVERRIDE, addJProp(BL.LM.and1, columnAscendingSortPropertyDraw, 1, is(BL.LM.customUser), 2), columnAscendingSortPropertyDrawCustomUser);

        hasUserPreferencesGroupObjectCustomUser = addDProp(BL.LM.baseGroup, "hasUserPreferencesGroupObjectCustomUser", getString("logics.group.object.has.user.preferences.user"), LogicalClass.instance, BL.securityLM.groupObject, BL.LM.customUser);
        hasUserPreferencesGroupObject = addDProp(BL.LM.baseGroup, "hasUserPreferencesGroupObject", getString("logics.group.object.has.user.preferences"), LogicalClass.instance, BL.securityLM.groupObject);
        hasUserPreferencesOverrideGroupObjectCustomUser = addSUProp(BL.LM.baseGroup, "hasUserPreferencesOverrideGroupObjectCustomUser", getString("logics.group.object.has.user.preferences"), Union.OVERRIDE, addJProp(BL.LM.and1, hasUserPreferencesGroupObject, 1, is(BL.LM.customUser), 2), hasUserPreferencesGroupObjectCustomUser);

        // ------------------------------------------------- Физическая модель ------------------------------------ //

        // Таблицы
        sidTable = addDProp(recognizeGroup, "sidTable", getString("logics.tables.name"), StringClass.get(100), table);
        sidToTable = addAGProp("sidToTable", getString("logics.tables.table"), sidTable);

        rowsTable = addDProp(BL.LM.baseGroup, "rowsTable", getString("logics.tables.rows"), IntegerClass.instance, table);
        sparseColumnsTable = addDProp(BL.LM.baseGroup, "sparseColumnsTable", getString("logics.tables.sparse.columns"), IntegerClass.instance, table);

        // Ключи таблиц
        tableTableKey = addDProp("tableTableKey", getString("logics.tables.table"), table, tableKey);

        sidTableKey = addDProp("sidTableKey", getString("logics.tables.key.sid"), StringClass.get(100), tableKey);
        sidToTableKey = addAGProp("sidToTableKey", getString("logics.tables.key"), sidTableKey);

        classTableKey = addDProp(BL.LM.baseGroup, "classTableKey", getString("logics.tables.key.class"), StringClass.get(40), tableKey);
        nameTableKey = addDProp(BL.LM.baseGroup, "nameTableKey", getString("logics.tables.key.name"), StringClass.get(20), tableKey);

        quantityTableKey = addDProp(BL.LM.baseGroup, "quantityTableKey", getString("logics.tables.key.variety.quantity"), IntegerClass.instance, tableKey);

        // Колонки таблиц
        tableTableColumn = addDProp("tableTableColumn", getString("logics.tables.table"), table, tableColumn);

        sidTableColumn = addDProp(BL.LM.baseGroup, "sidTableColumn", getString("logics.tables.column.name"), StringClass.get(100), tableColumn);
        sidToTableColumn = addAGProp("sidToTableColumn", getString("logics.tables.column"), sidTableColumn);

        propertyTableColumn = addJProp("propertyTableColumn", getString("logics.property"), SIDToProperty, sidTableColumn, 1);
        propertyNameTableColumn = addJProp(BL.LM.baseGroup, "propertyNameTableColumn", getString("logics.tables.property.name"), captionProperty, propertyTableColumn, 1);

        quantityTableColumn = addDProp(BL.LM.baseGroup, "quantityTableColumn", getString("logics.tables.column.variety.quantity"), IntegerClass.instance, tableColumn);
        notNullQuantityTableColumn = addDProp(BL.LM.baseGroup, "notNullQuantityTableColumn", getString("logics.tables.column.notnull.quantity"), IntegerClass.instance, tableColumn);
        perсentNotNullTableColumn = addDProp(BL.LM.baseGroup, "perсentNotNullTableColumn", getString("logics.tables.column.notnull.per.cent"), NumericClass.get(6, 2), tableColumn);

        recalculateAggregationTableColumn = addAProp(actionGroup, new RecalculateTableColumnActionProperty(getString("logics.recalculate.aggregations"), tableColumn));

        // Удаленные колонки
        sidTableDropColumn = addDProp(BL.LM.baseGroup, "sidTableDropColumn", getString("logics.tables.name"), StringClass.get(100), dropColumn);

        sidDropColumn = addDProp(BL.LM.baseGroup, "sidDropColumn", getString("logics.tables.column.name"), StringClass.get(100), dropColumn);
        sidToDropColumn = addAGProp("sidToDropColumn", getString("logics.tables.deleted.column"), sidDropColumn);

        timeDropColumn = addDProp(BL.LM.baseGroup, "timeDropColumn", getString("logics.tables.deleted.column.time"), DateTimeClass.instance, dropColumn);
        revisionDropColumn = addDProp(BL.LM.baseGroup, "revisionDropColumn", getString("logics.launch.revision"), StringClass.get(10), dropColumn);

        dropDropColumn = addAProp(BL.LM.baseGroup, new DropColumnActionProperty("dropDropColumn", getString("logics.tables.deleted.column.drop"), dropColumn));
        dropDropColumn.setEventAction(this, IncrementType.DROP, false, is(dropColumn), 1); // event, который при удалении колонки из системы удаляет ее из базы

        // Открытые формы во время подключения
        connectionFormCount = addDProp(baseGroup, "connectionFormCount", getString("logics.forms.number.of.opened.forms"), IntegerClass.instance, BL.systemEventsLM.connection, navigatorElement);

        
        initNavigators();
    }

    private void initNavigators() {
        addFormEntity(new PhysicalModelFormEntity(BL.LM.configuration, "physicalModelForm"));
        addFormEntity(new FormsFormEntity(BL.LM.configuration, "formsForm"));
        addFormEntity(new PropertiesFormEntity(BL.LM.configuration, "propertiesForm"));
    }
    
    @Override
    public void initIndexes() {
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    public class DropColumnActionProperty extends AdminActionProperty {
        private DropColumnActionProperty(String sID, String caption, ValueClass dropColumn) {
            super(sID, caption, new ValueClass[]{dropColumn});
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject dropColumnObject = context.getSingleKeyValue();
            String columnName = (String) sidDropColumn.getOld().read(context, dropColumnObject);
            String tableName = (String) sidTableDropColumn.getOld().read(context, dropColumnObject);
            BL.dropColumn(tableName, columnName);
        }
    }

    private class RecalculateTableColumnActionProperty extends AdminActionProperty {

        private final ClassPropertyInterface tableColumnInterface;

        private RecalculateTableColumnActionProperty(String caption, ValueClass tableColumn) {
            super(genSID(), caption, new ValueClass[]{tableColumn});
            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            tableColumnInterface = i.next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            DataObject tableColumnObject = context.getKeyValue(tableColumnInterface);
            String propertySID = (String) sidTableColumn.read(context, tableColumnObject);

            sqlSession.startTransaction();
            BL.recalculateAggregationTableColumn(sqlSession, propertySID.trim());
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
        }
    }


    private class RecalculateStatsActionProperty extends AdminActionProperty {
        private RecalculateStatsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            BL.recalculateStats(context.getSession());
        }
    }
    
    class PhysicalModelFormEntity extends FormEntity{
        PropertyDrawEntity recalculateStats;
        ObjectEntity objTable;
        ObjectEntity objKey;
        ObjectEntity objColumn;
        ObjectEntity objDropColumn;

        protected PhysicalModelFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.physical.model"));

            objTable = addSingleGroupObject(table, getString("logics.tables.tables"), BL.LM.baseGroup);
            objKey = addSingleGroupObject(tableKey, getString("logics.tables.keys"), BL.LM.baseGroup);
            objColumn = addSingleGroupObject(tableColumn, getString("logics.tables.columns"), BL.LM.baseGroup);
            objDropColumn = addSingleGroupObject(dropColumn, getString("logics.tables.deleted.column"), BL.LM.baseGroup);
            setEditType(objDropColumn, PropertyEditType.READONLY);
            setEditType(dropDropColumn, PropertyEditType.EDITABLE);

            recalculateStats = addPropertyDraw(addAProp(new RecalculateStatsActionProperty("recalculateStats", getString("logics.tables.recalculate.stats"))));
            addPropertyDraw(recalculateAggregationTableColumn, objColumn);

            setEditType(propertyNameTableColumn, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(tableTableKey, objKey), Compare.EQUALS, objTable));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(tableTableColumn, objColumn), Compare.EQUALS, objTable));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView modelContainer = design.createContainer(getString("logics.tables.physical.model"));
            modelContainer.add(design.getGroupObjectContainer(objTable.groupTo));
            modelContainer.add(design.getGroupObjectContainer(objKey.groupTo));
            modelContainer.add(design.getGroupObjectContainer(objColumn.groupTo));
            modelContainer.add(design.get(recalculateStats));

            ContainerView dropColumnsContainer = design.createContainer(getString("logics.tables.deleted.columns"));
            dropColumnsContainer.add(design.getGroupObjectContainer(objDropColumn.groupTo));

            ContainerView container = design.createContainer();
            container.type = ContainerType.TABBED_PANE;
            container.add(modelContainer);
            container.add(dropColumnsContainer);

            design.getMainContainer().add(0, container);

            return design;
        }
    }

    class FormsFormEntity extends FormEntity{

        ObjectEntity objTreeForm;
        TreeGroupEntity treeFormObject;
        ObjectEntity objUser;
        ObjectEntity objGroupObject;
        ObjectEntity objPropertyDraw;
        protected FormsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.forms"));

            objTreeForm = addSingleGroupObject(navigatorElement, true);
            objTreeForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeForm));

            treeFormObject = addTreeGroupObject(objTreeForm.groupTo);
            addPropertyDraw(new LP[]{navigatorElementSID, navigatorElementCaption, parentNavigatorElement}, objTreeForm);
            objUser = addSingleGroupObject(BL.LM.customUser, getString("logics.user"), BL.LM.userFirstName, BL.LM.userLastName, BL.LM.userLogin);
            objGroupObject = addSingleGroupObject(BL.securityLM.groupObject, getString("logics.group.object"), groupObjectSID, hasUserPreferencesGroupObject);
            objPropertyDraw = addSingleGroupObject(propertyDraw, getString("logics.property.draw"), propertyDrawSID, captionPropertyDraw);

            addPropertyDraw(hasUserPreferencesGroupObjectCustomUser, objGroupObject, objUser);

            addPropertyDraw(nameShowPropertyDraw, objPropertyDraw);
            addPropertyDraw(nameShowPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnWidthPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnWidthPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnOrderPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnOrderPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnSortPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnAscendingSortPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnSortPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnAscendingSortPropertyDrawCustomUser, objPropertyDraw, objUser);

            objUser.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new CompareFilterEntity(addPropertyObject(navigatorElementGroupObject, objGroupObject), Compare.EQUALS, objTreeForm));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(formPropertyDraw, objPropertyDraw), Compare.EQUALS, objTreeForm));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(BL.LM.currentUser), Compare.EQUALS, objUser));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(groupObjectPropertyDraw, objPropertyDraw), Compare.EQUALS, objGroupObject),
                    getString("logics.group.object.only.current"),
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup);

            setEditType(PropertyEditType.READONLY);
            setEditType(nameShowPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(nameShowPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnWidthPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnWidthPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnOrderPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnOrderPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnSortPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnAscendingSortPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnSortPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnAscendingSortPropertyDrawCustomUser, PropertyEditType.EDITABLE);
        }
    }

    class PropertiesFormEntity extends FormEntity {
        ObjectEntity objProperties;
        ObjectEntity objProps;
        ObjectEntity objTreeProps;
        TreeGroupEntity treePropertiesObject;
        protected PropertiesFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.properties"));

            objProperties = addSingleGroupObject(property, true);

            objTreeProps = addSingleGroupObject(abstractGroup, true);
            objProps = addSingleGroupObject(property, true);

            objTreeProps.groupTo.setIsParents(addPropertyObject(parentAbstractGroup, objTreeProps));
            treePropertiesObject = addTreeGroupObject(objTreeProps.groupTo, objProps.groupTo);

            LP dumb1 = dumb(1);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty, signatureProperty, returnProperty, classProperty, parentProperty, numberProperty, userLoggableProperty, loggableProperty, storedProperty, isSetNotNullProperty}, objProperties);
            addPropertyDraw(new LP[]{captionAbstractGroup, SIDAbstractGroup, dumb1, dumb1, dumb1, parentAbstractGroup, numberAbstractGroup, dumb1, dumb1, dumb1, dumb1}, objTreeProps);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty, signatureProperty, returnProperty, classProperty, parentProperty, numberProperty, userLoggableProperty, loggableProperty, storedProperty, isSetNotNullProperty}, objProps);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(parentProperty, objProps), Compare.EQUALS, objTreeProps));

            setEditType(PropertyEditType.READONLY);
            setEditType(userLoggableProperty, PropertyEditType.EDITABLE);
            setEditType(storedProperty, PropertyEditType.EDITABLE);
            setEditType(isSetNotNullProperty, PropertyEditType.EDITABLE);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView container = design.createContainer();
            container.type = ContainerType.TABBED_PANE;

            ContainerView treeContainer = design.createContainer(getString("logics.tree"));
            ContainerView tableContainer = design.createContainer(getString("logics.tables.table"));

            treeContainer.add(design.getTreeContainer(treePropertiesObject));
            treeContainer.add(design.getGroupObjectContainer(objProperties.groupTo));

            tableContainer.add(design.getGroupObjectContainer(objProperties.groupTo));

            container.add(treeContainer);
            container.add(tableContainer);

            design.getMainContainer().add(0, container);

            addDefaultOrder(getPropertyDraw(numberProperty, objProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(numberAbstractGroup, objTreeProps.groupTo), true);

            return design;
        }
    }
}
