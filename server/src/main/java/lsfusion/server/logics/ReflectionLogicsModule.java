package lsfusion.server.logics;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class ReflectionLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass propertyGroup;
    public ConcreteCustomClass navigatorElement;
    public ConcreteCustomClass navigatorAction;
    public ConcreteCustomClass form;
    public ConcreteCustomClass propertyDraw;
    public ConcreteCustomClass propertyDrawShowStatus;
    public ConcreteCustomClass table;
    public ConcreteCustomClass tableKey;
    public ConcreteCustomClass tableColumn;
    public ConcreteCustomClass dropColumn;
    public ConcreteCustomClass property;
    public ConcreteCustomClass groupObject;
    
    public LCP captionPropertyGroup;
    public LCP captionNavigatorElement;
    public LCP parentPropertyGroup;
    public LCP numberPropertyGroup;
    public LCP SIDPropertyGroup;
    public LCP propertyGroupSID;

    public LCP parentProperty;
    public LCP numberProperty;
    public LCP dbNameProperty;
    public LCP canonicalNameProperty;
    public LCP loggableProperty;
    public LCP userLoggableProperty;
    public LCP storedProperty;
    public LCP isSetNotNullProperty;
    public LCP signatureProperty;
    public LCP returnProperty;
    public LCP classProperty;
    public LCP complexityProperty;
    public LCP captionProperty;
    public LCP tableSIDProperty;
    public LCP propertySID;
    public LCP propertyCanonicalName;
    public LCP propertyTableSID;

    public LCP sidNavigatorElement;
    public LCP numberNavigatorElement;

    public LCP navigatorElementCanonicalName;
    public LCP canonicalNameNavigatorElement;

    public LCP navigatorElementSID;
    public LCP parentNavigatorElement;
    public LCP isNavigatorElement;
    public LCP isForm;
    public LCP isNavigatorAction;

    public LCP sidGroupObject;
    public LCP navigatorElementGroupObject;
    public LCP groupObjectSIDNavigatorElementNameGroupObject;

    public LCP sidPropertyDraw;
    public LCP captionPropertyDraw;
    public LCP formPropertyDraw;
    public LCP groupObjectPropertyDraw;
    public LCP propertyDrawSIDNavigatorElementNamePropertyDraw;

    public LCP showPropertyDraw;
    public LCP showPropertyDrawCustomUser;
    
    public LCP nameShowPropertyDraw;
    public LCP nameShowPropertyDrawCustomUser;

    public LCP columnCaptionPropertyDrawCustomUser;
    public LCP columnCaptionPropertyDraw;
    public LCP columnWidthPropertyDrawCustomUser;
    public LCP columnWidthPropertyDraw;
    public LCP columnOrderPropertyDrawCustomUser;
    public LCP columnOrderPropertyDraw;
    public LCP columnSortPropertyDrawCustomUser;
    public LCP columnSortPropertyDraw;
    public LCP columnAscendingSortPropertyDrawCustomUser;
    public LCP columnAscendingSortPropertyDraw;
    public LCP hasUserPreferencesGroupObject;
    public LCP hasUserPreferencesGroupObjectCustomUser;
    public LCP hasUserPreferencesOverrideGroupObjectCustomUser;
    public LCP fontSizeGroupObject;
    public LCP fontSizeGroupObjectCustomUser;
    public LCP isFontBoldGroupObject;
    public LCP isFontBoldGroupObjectCustomUser;
    public LCP isFontItalicGroupObject;
    public LCP isFontItalicGroupObjectCustomUser;
    public LCP pageSizeGroupObject;
    public LCP pageSizeGroupObjectCustomUser;
    
    public LCP nameFormGrouping;
    public LCP itemQuantityFormGrouping;
    public LCP groupObjectFormGrouping;
    public LCP formGroupingNameFormGroupingGroupObject;
    public LCP groupOrderFormGroupingPropertyDraw;
    public LCP sumFormGroupingPropertyDraw;
    public LCP maxFormGroupingPropertyDraw;
    public LCP pivotFormGroupingPropertyDraw;
    
    public LCP sidTable;
    public LCP tableSID;
    public LCP rowsTable;
    public LCP tableTableKey;
    public LCP sidTableKey;
    public LCP tableKeySID;
    public LCP classTableKey;
    public LCP nameTableKey;
    public LCP quantityTableKey;
    public LCP tableTableColumn;
    public LCP propertyTableColumn;
    public LCP sidTableColumn;
    public LCP longSIDTableColumn;
    public LCP tableColumnSID;

    public LCP quantityTableColumn;
    public LCP notNullQuantityTableColumn;
    public LAP recalculateAggregationTableColumn;

    public LCP<?> sidTableDropColumn;
    public LCP<?> sidDropColumn;
    public LCP dropColumnSID;

    public LCP timeDropColumn;
    public LCP revisionDropColumn;
    public LAP dropDropColumn;

    public final StringClass navigatorElementSIDClass = StringClass.get(50);
    public final StringClass navigatorElementCanonicalNameClass = StringClass.get(100);
    public final StringClass navigatorElementCaptionClass = StringClass.get(250);
    public final StringClass propertySIDValueClass = StringClass.get(100);
    public final StringClass propertyCanonicalNameValueClass = StringClass.get(512);
    public final StringClass propertyCaptionValueClass = StringClass.get(250);
    public final StringClass propertySignatureValueClass = StringClass.get(100);
    public final StringClass propertyTableValueClass = StringClass.get(100);
    public final StringClass propertyDrawSIDClass = StringClass.get(100);
    public final LogicalClass propertyLoggableValueClass = LogicalClass.instance;
    public final LogicalClass propertyStoredValueClass = LogicalClass.instance;
    public final LogicalClass propertyIsSetNotNullValueClass = LogicalClass.instance;

    public ReflectionLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ReflectionLogicsModule.class.getResourceAsStream("/lsfusion/system/Reflection.lsf"), "/lsfusion/system/Reflection.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
        propertyGroup = (ConcreteCustomClass) findClass("PropertyGroup");
        navigatorElement = (ConcreteCustomClass) findClass("NavigatorElement");
        navigatorAction = (ConcreteCustomClass) findClass("NavigatorAction");
        form = (ConcreteCustomClass) findClass("Form");
        propertyDraw = (ConcreteCustomClass) findClass("PropertyDraw");
        propertyDrawShowStatus = (ConcreteCustomClass) findClass("PropertyDrawShowStatus");
        table = (ConcreteCustomClass) findClass("Table");
        tableKey = (ConcreteCustomClass) findClass("TableKey");
        tableColumn = (ConcreteCustomClass) findClass("TableColumn");
        dropColumn = (ConcreteCustomClass) findClass("DropColumn");
        property = (ConcreteCustomClass) findClass("Property");
        groupObject = (ConcreteCustomClass) findClass("GroupObject");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        // ------- Доменная логика --------- //

        // Группы свойства
        captionPropertyGroup = findProperty("captionPropertyGroup");
        captionNavigatorElement = findProperty("captionNavigatorElement");
        parentPropertyGroup = findProperty("parentPropertyGroup");
        numberPropertyGroup = findProperty("numberPropertyGroup");
        SIDPropertyGroup = findProperty("SIDPropertyGroup");
        propertyGroupSID = findProperty("propertyGroupSID");

        // Свойства
        parentProperty = findProperty("parentProperty");
        tableSIDProperty = findProperty("tableSIDProperty");
        numberProperty = findProperty("numberProperty");
        dbNameProperty = findProperty("dbNameProperty");
        canonicalNameProperty = findProperty("canonicalNameProperty");
        loggableProperty = findProperty("loggableProperty");
        userLoggableProperty = findProperty("userLoggableProperty");
        storedProperty = findProperty("storedProperty");
        isSetNotNullProperty = findProperty("isSetNotNullProperty");
        signatureProperty = findProperty("signatureProperty");
        returnProperty = findProperty("returnProperty");
        classProperty = findProperty("classProperty");
        complexityProperty = findProperty("complexityProperty");
        captionProperty = findProperty("captionProperty");
        propertySID = findProperty("propertySID");
        propertyCanonicalName = findProperty("propertyCanonicalName");
        propertyTableSID = findProperty("propertyTableSID");

        // ------- Логика представлений --------- //

        // Навигатор
        sidNavigatorElement = findProperty("sidNavigatorElement");
        navigatorElementSID = findProperty("navigatorElementSID");
        
        numberNavigatorElement = findProperty("numberNavigatorElement");
        navigatorElementCanonicalName = findProperty("navigatorElementCanonicalName");
        canonicalNameNavigatorElement = findProperty("canonicalNameNavigatorElement");
        parentNavigatorElement = findProperty("parentNavigatorElement");
        isNavigatorElement = findProperty("isNavigatorElement");
        isForm = findProperty("isForm");
        isNavigatorAction = findProperty("isNavigatorAction");

        // ----- Формы ---- //

        // Группа объектов
        sidGroupObject = findProperty("sidGroupObject");
        navigatorElementGroupObject = findProperty("navigatorElementGroupObject");
        groupObjectSIDNavigatorElementNameGroupObject = findProperty("groupObjectSIDNavigatorElementNameGroupObject");


        // PropertyDraw
        sidPropertyDraw = findProperty("sidPropertyDraw");
        captionPropertyDraw = findProperty("captionPropertyDraw");
        formPropertyDraw = findProperty("formPropertyDraw");
        groupObjectPropertyDraw = findProperty("groupObjectPropertyDraw");
        // todo : это свойство должно быть для форм, а не навигаторов
        propertyDrawSIDNavigatorElementNamePropertyDraw = findProperty("propertyDrawSIDNavigatorElementNamePropertyDraw");

        // UserPreferences
        showPropertyDraw = findProperty("showPropertyDraw");
        showPropertyDrawCustomUser = findProperty("showPropertyDrawCustomUser");

        nameShowPropertyDraw = findProperty("nameShowPropertyDraw");
        nameShowPropertyDrawCustomUser = findProperty("nameShowPropertyDrawCustomUser");

        columnCaptionPropertyDrawCustomUser = findProperty("columnCaptionPropertyDrawCustomUser");
        columnCaptionPropertyDraw = findProperty("columnCaptionPropertyDraw");
        
        columnWidthPropertyDrawCustomUser = findProperty("columnWidthPropertyDrawCustomUser");
        columnWidthPropertyDraw = findProperty("columnWidthPropertyDraw");

        columnOrderPropertyDrawCustomUser = findProperty("columnOrderPropertyDrawCustomUser");
        columnOrderPropertyDraw = findProperty("columnOrderPropertyDraw");

        columnSortPropertyDrawCustomUser = findProperty("columnSortPropertyDrawCustomUser");
        columnSortPropertyDraw = findProperty("columnSortPropertyDraw");

        columnAscendingSortPropertyDrawCustomUser = findProperty("columnAscendingSortPropertyDrawCustomUser");
        columnAscendingSortPropertyDraw = findProperty("columnAscendingSortPropertyDraw");

        hasUserPreferencesGroupObjectCustomUser = findProperty("hasUserPreferencesGroupObjectCustomUser");
        hasUserPreferencesGroupObject = findProperty("hasUserPreferencesGroupObject");
        hasUserPreferencesOverrideGroupObjectCustomUser = findProperty("hasUserPreferencesOverrideGroupObjectCustomUser");

        fontSizeGroupObjectCustomUser = findProperty("fontSizeGroupObjectCustomUser");
        fontSizeGroupObject = findProperty("fontSizeGroupObject");

        isFontBoldGroupObjectCustomUser = findProperty("isFontBoldGroupObjectCustomUser");
        isFontBoldGroupObject = findProperty("isFontBoldGroupObject");

        isFontItalicGroupObjectCustomUser = findProperty("isFontItalicGroupObjectCustomUser");
        isFontItalicGroupObject = findProperty("isFontItalicGroupObject");

        pageSizeGroupObjectCustomUser = findProperty("pageSizeGroupObjectCustomUser");
        pageSizeGroupObject = findProperty("pageSizeGroupObject");

        // группировки
        nameFormGrouping = findProperty("nameFormGrouping");
        itemQuantityFormGrouping = findProperty("itemQuantityFormGrouping");
        groupObjectFormGrouping = findProperty("groupObjectFormGrouping");
        formGroupingNameFormGroupingGroupObject = findProperty("formGroupingNameFormGroupingGroupObject");
        groupOrderFormGroupingPropertyDraw = findProperty("groupOrderFormGroupingPropertyDraw");
        sumFormGroupingPropertyDraw = findProperty("sumFormGroupingPropertyDraw");
        maxFormGroupingPropertyDraw = findProperty("maxFormGroupingPropertyDraw");
        pivotFormGroupingPropertyDraw = findProperty("pivotFormGroupingPropertyDraw");
        // ------------------------------------------------- Физическая модель ------------------------------------ //

        // Таблицы
        sidTable = findProperty("sidTable");
        tableSID = findProperty("tableSID");

        rowsTable = findProperty("rowsTable");

        // Ключи таблиц
        tableTableKey = findProperty("tableTableKey");

        sidTableKey = findProperty("sidTableKey");
        tableKeySID = findProperty("tableKeySID");

        classTableKey = findProperty("classTableKey");
        nameTableKey = findProperty("nameTableKey");

        quantityTableKey = findProperty("quantityTableKey");

        // Колонки таблиц
        tableTableColumn = findProperty("tableTableColumn");
        propertyTableColumn = findProperty("propertyTableColumn");

        sidTableColumn = findProperty("sidTableColumn");
        longSIDTableColumn = findProperty("longSIDTableColumn");
        tableColumnSID = findProperty("tableColumnSID");    
        
        quantityTableColumn = findProperty("quantityTableColumn");
        notNullQuantityTableColumn = findProperty("notNullQuantityTableColumn");

        recalculateAggregationTableColumn = findAction("recalculateAggregationTableColumn");

        // Удаленные колонки
        sidTableDropColumn = findProperty("sidTableDropColumn");

        sidDropColumn = findProperty("sidDropColumn");
        dropColumnSID = findProperty("dropColumnSID");

        timeDropColumn = findProperty("timeDropColumn");
        revisionDropColumn = findProperty("revisionDropColumn");

        dropDropColumn = findAction("dropDropColumn");
        //dropDropColumn.setEventAction(this, IncrementType.DROP, false, is(dropColumn), 1); // event, который при удалении колонки из системы удаляет ее из базы
    }
}

