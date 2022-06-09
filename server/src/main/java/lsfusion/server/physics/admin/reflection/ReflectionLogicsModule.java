package lsfusion.server.physics.admin.reflection;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.property.focus.CurrentFormProperty;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.util.ArrayList;

public class ReflectionLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass propertyGroup;
    public ConcreteCustomClass navigatorElement;
    public ConcreteCustomClass navigatorFolder;
    public ConcreteCustomClass navigatorAction;
    public ConcreteCustomClass form;
    public ConcreteCustomClass noForm;
    public ConcreteCustomClass propertyDraw;
    public ConcreteCustomClass propertyDrawShowStatus;
    public ConcreteCustomClass table;
    public ConcreteCustomClass tableKey;
    public ConcreteCustomClass tableColumn;
    public ConcreteCustomClass dropColumn;
    public ConcreteCustomClass property;
    public ConcreteCustomClass action;
    public ConcreteCustomClass groupObject;
    
    public LP captionPropertyGroup;
    public LP captionNavigatorElement;
    public LP parentPropertyGroup;
    public LP numberPropertyGroup;
    public LP SIDPropertyGroup;
    public LP propertyGroupSID;

    public LP propertyDependencies;
    public LP propertyDependents;

    public LP parentProperty;
    public LP numberProperty;
    public LP dbNameProperty;
    public LP canonicalNameActionOrProperty;
    public LP canonicalNameAction;
    public LP canonicalNameProperty;
    public LP loggableProperty;
    public LP userLoggableProperty;
    public LP storedProperty;
    public LP isSetNotNullProperty;
    public LP returnProperty;
    public LP classProperty;
    public LP complexityProperty;
    public LP captionProperty;
    public LP tableSIDProperty;
    public LP annotationProperty;
    public LP statsProperty;
    public LP overStatsProperty;
    public LP maxStatsProperty;
    public LP propertyCanonicalName;
    public LP actionCanonicalName;
    public LP propertyTableSID;
    public LP quantityProperty;
    public LP quantityTopProperty;
    public LP notNullQuantityProperty;
    public LP lastRecalculateProperty;
    public LP hasNotNullQuantity;

    public LP numberNavigatorElement;
    
    // temporary for migration
    public ImplementTable navigatorElementTable;

    public LP navigatorElementCanonicalName;
    public LP canonicalNameNavigatorElement;
    public LP formCanonicalName;
    public LP formByCanonicalName;

    public LP parentNavigatorElement;
    public LP formNavigatorAction; 
    public LP actionNavigatorAction; 
    
    public LP formCaption;
    public LP isForm;
    public LP isNavigatorAction;
    public LP isNavigatorFolder;

    public LP sidGroupObject;
    public LP formGroupObject;
    public LP groupObjectSIDFormNameGroupObject;

    public LP sidPropertyDraw;
    public LP captionPropertyDraw;
    public LP formPropertyDraw;
    public LP groupObjectPropertyDraw;
    public LP propertyDrawByFormNameAndPropertyDrawSid;

    public LP showPropertyDraw;
    public LP showPropertyDrawCustomUser;
    
    public LP nameShowPropertyDraw;
    public LP nameShowPropertyDrawCustomUser;

    public LP columnCaptionPropertyDrawCustomUser;
    public LP columnCaptionPropertyDraw;
    public LP columnPatternPropertyDrawCustomUser;
    public LP columnPatternPropertyDraw;
    public LP columnWidthPropertyDrawCustomUser;
    public LP columnWidthPropertyDraw;
    public LP columnOrderPropertyDrawCustomUser;
    public LP columnOrderPropertyDraw;
    public LP columnSortPropertyDrawCustomUser;
    public LP columnSortPropertyDraw;
    public LP columnAscendingSortPropertyDrawCustomUser;
    public LP columnAscendingSortPropertyDraw;
    public LP hasUserPreferencesGroupObject;
    public LP hasUserPreferencesGroupObjectCustomUser;
    public LP hasUserPreferencesOverrideGroupObjectCustomUser;
    public LP fontSizeGroupObject;
    public LP fontSizeGroupObjectCustomUser;
    public LP isFontBoldGroupObject;
    public LP isFontBoldGroupObjectCustomUser;
    public LP isFontItalicGroupObject;
    public LP isFontItalicGroupObjectCustomUser;
    public LP pageSizeGroupObject;
    public LP pageSizeGroupObjectCustomUser;
    public LP headerHeightGroupObject;
    public LP headerHeightGroupObjectCustomUser;
    
    public LP nameFormGrouping;
    public LP itemQuantityFormGrouping;
    public LP groupObjectFormGrouping;
    public LP formGroupingNameFormGroupingGroupObject;
    public LP groupOrderFormGroupingPropertyDraw;
    public LP sumFormGroupingPropertyDraw;
    public LP maxFormGroupingPropertyDraw;
    public LP pivotFormGroupingPropertyDraw;
    
    public LP sidTable;
    public LP tableSID;
    public LP rowsTable;
    public LP tableTableKey;
    public LP sidTableKey;
    public LP tableKeySID;
    public LP classTableKey;
    public LP classSIDTableKey;
    public LP nameTableKey;
    public LP quantityTableKey;
    public LP quantityTopTableKey;
    public LP overQuantityTableKey;
    public LP tableTableColumn;
    public LP propertyTableColumn;
    public LP sidTableColumn;
    public LP longSIDTableColumn;
    public LP tableColumnLongSID;
    public LP tableColumnSID;

    public LP overQuantityTableColumn;
    public LP notNullQuantityTableColumn;
    public LA recalculateAggregationTableColumn;

    public LP disableClassesTable;
    public LP disableStatsTable;
    public LP disableAggregationsTableColumn;
    public LP disableClassesTableColumn;
    public LP disableStatsTableColumn;

    public LP disableClassesTableSID;
    public LP disableStatsTableSID;
    public LP disableAggregationsTableColumnSID;
    public LP disableStatsTableColumnSID;

    public LP<?> sidTableDropColumn;
    public LP<?> sidDropColumn;
    public LP dropColumnSID;

    public LP timeDropColumn;
    public LP revisionDropColumn;
    public LA dropDropColumn;

    public final StringClass navigatorElementSIDClass = StringClass.get(50);
    public final StringClass navigatorElementCanonicalNameClass = StringClass.getv(100);
    public final StringClass navigatorElementCaptionClass = StringClass.get(250);
    public final StringClass formCanonicalNameClass = StringClass.getv(100);
    public final StringClass actionCanonicalNameClass = StringClass.get(ExtInt.UNLIMITED);
    public final StringClass formCaptionClass = StringClass.getv(250);
    public final StringClass propertySIDValueClass = StringClass.get(100);
    public final StringClass propertyCanonicalNameValueClass = StringClass.get(ExtInt.UNLIMITED);
    public final StringClass propertyCaptionValueClass = StringClass.get(250);
    public final StringClass propertyClassValueClass = StringClass.get(100);
    public final StringClass propertyTableValueClass = StringClass.get(100);
    public final StringClass propertyDrawSIDClass = StringClass.get(100);
    public final LogicalClass propertyLoggableValueClass = LogicalClass.instance;
    public final LogicalClass propertyStoredValueClass = LogicalClass.instance;
    public final LogicalClass propertyIsSetNotNullValueClass = LogicalClass.instance;

    public LP currentForm;

    public ReflectionLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/Reflection.lsf");
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();
        propertyGroup = (ConcreteCustomClass) findClass("PropertyGroup");
        navigatorElement = (ConcreteCustomClass) findClass("NavigatorElement");
        navigatorFolder = (ConcreteCustomClass) findClass("NavigatorFolder");
        navigatorAction = (ConcreteCustomClass) findClass("NavigatorAction");
        form = (ConcreteCustomClass) findClass("Form");
        noForm = (ConcreteCustomClass) findClass("NoForm");
        propertyDraw = (ConcreteCustomClass) findClass("PropertyDraw");
        propertyDrawShowStatus = (ConcreteCustomClass) findClass("PropertyDrawShowStatus");
        table = (ConcreteCustomClass) findClass("Table");
        tableKey = (ConcreteCustomClass) findClass("TableKey");
        tableColumn = (ConcreteCustomClass) findClass("TableColumn");
        dropColumn = (ConcreteCustomClass) findClass("DropColumn");
        property = (ConcreteCustomClass) findClass("Property");
        action = (ConcreteCustomClass) findClass("Action");
        groupObject = (ConcreteCustomClass) findClass("GroupObject");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        currentForm = addProperty(null, new LP<>(new CurrentFormProperty(form)));
        makePropertyPublic(currentForm, "currentForm", new ArrayList<>());

        super.initMainLogic();

        // ------- Доменная логика --------- //

        // Группы свойства
        captionPropertyGroup = findProperty("caption[PropertyGroup]");
        parentPropertyGroup = findProperty("parent[PropertyGroup]");
        numberPropertyGroup = findProperty("number[PropertyGroup]");
        SIDPropertyGroup = findProperty("SID[PropertyGroup]");
        propertyGroupSID = findProperty("propertyGroup[STRING[100]]");

        // Свойства
        parentProperty = findProperty("parent[ActionOrProperty]");
        tableSIDProperty = findProperty("tableSID[Property]");
        annotationProperty = findProperty("annotation[Property]");
        statsProperty = findProperty("stats[Property]");
        overStatsProperty = findProperty("overStats[Property]");
        maxStatsProperty = findProperty("overMaxStatsProperty[]");
        numberProperty = findProperty("number[ActionOrProperty]");
        dbNameProperty = findProperty("dbName[Property]");
        canonicalNameActionOrProperty = findProperty("canonicalName[ActionOrProperty]");
        canonicalNameAction = findProperty("canonicalName[Action]");
        canonicalNameProperty = findProperty("canonicalName[Property]");
        loggableProperty = findProperty("loggable[Property]");
        userLoggableProperty = findProperty("userLoggable[Property]");
        storedProperty = findProperty("stored[Property]");
        isSetNotNullProperty = findProperty("isSetNotNull[Property]");
        returnProperty = findProperty("return[Property]");
        classProperty = findProperty("class[Property]");
        complexityProperty = findProperty("complexity[Property]");
        captionProperty = findProperty("caption[Property]");
        propertyCanonicalName = findProperty("propertyCanonicalName[STRING]");
        actionCanonicalName = findProperty("actionCanonicalName[STRING]");
        propertyTableSID = findProperty("propertyTable[STRING[100],STRING[100]]");
        quantityProperty = findProperty("quantity[Property]");
        quantityTopProperty = findProperty("quantityTop[Property]");
        notNullQuantityProperty = findProperty("notNullQuantity[Property]");
        lastRecalculateProperty = findProperty("lastRecalculate[Property]");
        hasNotNullQuantity = findProperty("hasNotNullQuantity[]");

        propertyDependencies = findProperty("propertyDependencies[INTEGER]");
        propertyDependents = findProperty("propertyDependents[INTEGER]");

        // ------- Логика представлений --------- //

        // Навигатор
        numberNavigatorElement = findProperty("number[NavigatorElement]");
        navigatorElementCanonicalName = findProperty("navigatorElementCanonicalName[STRING[100]]");
        canonicalNameNavigatorElement = findProperty("canonicalName[NavigatorElement]");
        captionNavigatorElement = findProperty("caption[NavigatorElement]");
        parentNavigatorElement = findProperty("parent[NavigatorElement]");
        
        isNavigatorFolder = findProperty("isNavigatorFolder[?]");
        isNavigatorAction = findProperty("isNavigatorAction[?]");

        navigatorElementTable = findTable("navigatorElement");
        
        // ----- Формы ---- //
        formCanonicalName = findProperty("canonicalName[Form]");
        formByCanonicalName = findProperty("form[STRING[100]]");
        formCaption = findProperty("caption[Form]");
        isForm = findProperty("is[Form]");

        formNavigatorAction = findProperty("form[NavigatorAction]");
        actionNavigatorAction = findProperty("action[NavigatorAction]");
        
        // Группа объектов
        sidGroupObject = findProperty("sid[GroupObject]");
        formGroupObject = findProperty("form[GroupObject]");
        groupObjectSIDFormNameGroupObject = findProperty("groupSIDFormGroupObject[STRING[100],STRING[100]]");


        // PropertyDraw
        sidPropertyDraw = findProperty("sid[PropertyDraw]");
        captionPropertyDraw = findProperty("caption[PropertyDraw]");
        formPropertyDraw = findProperty("form[PropertyDraw]");
        groupObjectPropertyDraw = findProperty("groupObject[PropertyDraw]");
        // todo : это свойство должно быть для форм, а не навигаторов
        propertyDrawByFormNameAndPropertyDrawSid = findProperty("propertyDrawByFormNameAndPropertyDrawSid[STRING[100],STRING[100]]");

        // UserPreferences
        showPropertyDraw = findProperty("show[PropertyDraw]");
        showPropertyDrawCustomUser = findProperty("show[PropertyDraw,CustomUser]");

        nameShowPropertyDraw = findProperty("nameShow[PropertyDraw]");
        nameShowPropertyDrawCustomUser = findProperty("nameShow[PropertyDraw,CustomUser]");

        columnCaptionPropertyDrawCustomUser = findProperty("columnCaption[PropertyDraw,CustomUser]");
        columnCaptionPropertyDraw = findProperty("columnCaption[PropertyDraw]");

        columnPatternPropertyDrawCustomUser = findProperty("columnPattern[PropertyDraw,CustomUser]");
        columnPatternPropertyDraw = findProperty("columnPattern[PropertyDraw]");
        
        columnWidthPropertyDrawCustomUser = findProperty("columnWidth[PropertyDraw,CustomUser]");
        columnWidthPropertyDraw = findProperty("columnWidth[PropertyDraw]");

        columnOrderPropertyDrawCustomUser = findProperty("columnOrder[PropertyDraw,CustomUser]");
        columnOrderPropertyDraw = findProperty("columnOrder[PropertyDraw]");

        columnSortPropertyDrawCustomUser = findProperty("columnSort[PropertyDraw,CustomUser]");
        columnSortPropertyDraw = findProperty("columnSort[PropertyDraw]");

        columnAscendingSortPropertyDrawCustomUser = findProperty("columnAscendingSort[PropertyDraw,CustomUser]");
        columnAscendingSortPropertyDraw = findProperty("columnAscendingSort[PropertyDraw]");

        hasUserPreferencesGroupObjectCustomUser = findProperty("hasUserPreferences[GroupObject,CustomUser]");
        hasUserPreferencesGroupObject = findProperty("hasUserPreferences[GroupObject]");
        hasUserPreferencesOverrideGroupObjectCustomUser = findProperty("hasUserPreferencesOverride[GroupObject,CustomUser]");

        fontSizeGroupObjectCustomUser = findProperty("fontSize[GroupObject,CustomUser]");
        fontSizeGroupObject = findProperty("fontSize[GroupObject]");

        isFontBoldGroupObjectCustomUser = findProperty("isFontBold[GroupObject,CustomUser]");
        isFontBoldGroupObject = findProperty("isFontBold[GroupObject]");

        isFontItalicGroupObjectCustomUser = findProperty("isFontItalic[GroupObject,CustomUser]");
        isFontItalicGroupObject = findProperty("isFontItalic[GroupObject]");

        pageSizeGroupObjectCustomUser = findProperty("pageSize[GroupObject,CustomUser]");
        pageSizeGroupObject = findProperty("pageSize[GroupObject]");

        headerHeightGroupObjectCustomUser = findProperty("headerHeight[GroupObject,CustomUser]");
        headerHeightGroupObject = findProperty("headerHeight[GroupObject]");

        // группировки
        nameFormGrouping = findProperty("name[FormGrouping]");
        itemQuantityFormGrouping = findProperty("itemQuantity[FormGrouping]");
        groupObjectFormGrouping = findProperty("groupObject[FormGrouping]");
        formGroupingNameFormGroupingGroupObject = findProperty("formGrouping[STRING[100],GroupObject]");
        groupOrderFormGroupingPropertyDraw = findProperty("groupOrder[FormGrouping,PropertyDraw]");
        sumFormGroupingPropertyDraw = findProperty("sum[FormGrouping,PropertyDraw]");
        maxFormGroupingPropertyDraw = findProperty("max[FormGrouping,PropertyDraw]");
        pivotFormGroupingPropertyDraw = findProperty("pivot[FormGrouping,PropertyDraw]");
        // ------------------------------------------------- Физическая модель ------------------------------------ //

        // Таблицы
        sidTable = findProperty("sid[Table]");
        tableSID = findProperty("table[ISTRING[100]]");

        rowsTable = findProperty("rows[Table]");

        // Ключи таблиц
        tableTableKey = findProperty("table[TableKey]");

        sidTableKey = findProperty("sid[TableKey]");
        tableKeySID = findProperty("tableKey[ISTRING[100]]");

        classTableKey = findProperty("class[TableKey]");
        classSIDTableKey = findProperty("classSID[TableKey]");
        nameTableKey = findProperty("name[TableKey]");

        quantityTableKey = findProperty("quantity[TableKey]");
        quantityTopTableKey = findProperty("quantityTop[TableKey]");
        overQuantityTableKey = findProperty("overQuantity[TableKey]");

        // Колонки таблиц
        tableTableColumn = findProperty("table[TableColumn]");
        propertyTableColumn = findProperty("property[TableColumn]");

        sidTableColumn = findProperty("sid[TableColumn]");
        longSIDTableColumn = findProperty("longSID[TableColumn]");
        tableColumnLongSID = findProperty("tableColumnLong[ISTRING[100]]");
        tableColumnSID = findProperty("tableColumnSID[ISTRING[100]]");

        overQuantityTableColumn = findProperty("overQuantity[TableColumn]");
        notNullQuantityTableColumn = findProperty("notNullQuantity[TableColumn]");

        recalculateAggregationTableColumn = findAction("recalculateAggregation[TableColumn]");

        //Отключение пересчётов и проверок
        disableClassesTable = findProperty("disableClasses[Table]");
        disableStatsTable = findProperty("disableStatsTable[Table]");
        disableAggregationsTableColumn = findProperty("disableAggregations[TableColumn]");
        disableClassesTableColumn = findProperty("disableClasses[TableColumn]");
        disableStatsTableColumn = findProperty("disableStatsTableColumn[TableColumn]");

        disableClassesTableSID = findProperty("disableClasses[ISTRING[100]]");
        disableStatsTableSID = findProperty("disableStatsTable[ISTRING[100]]");
        disableAggregationsTableColumnSID = findProperty("disableAggregations[ISTRING[100]]");
        disableStatsTableColumnSID = findProperty("disableStatsTableColumn[ISTRING[100]]");

        // Удаленные колонки
        sidTableDropColumn = findProperty("sidTable[DropColumn]");

        sidDropColumn = findProperty("sid[DropColumn]");
        dropColumnSID = findProperty("dropColumn[STRING[100]]");

        timeDropColumn = findProperty("time[DropColumn]");
        revisionDropColumn = findProperty("revision[DropColumn]");

        dropDropColumn = findAction("drop[DropColumn]");
        //dropDropColumn.setEventAction(this, IncrementType.DROP, false, is(dropColumn), 1); // event, который при удалении колонки из системы удаляет ее из базы
    }
}

