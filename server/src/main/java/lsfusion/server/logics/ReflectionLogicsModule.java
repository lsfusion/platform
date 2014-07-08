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
    public LCP SIDProperty;
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
    
    public LCP navigatorElementSID;
    public LCP parentNavigatorElement;
    public LCP isNavigatorElement;
    public LCP isForm;
    public LCP isNavigatorAction;

    public LCP sidGroupObject;
    public LCP navigatorElementGroupObject;
    public LCP sidNavigatorElementGroupObject;
    public LCP groupObjectSIDGroupObjectSIDNavigatorElementGroupObject;

    public LCP sidPropertyDraw;
    public LCP captionPropertyDraw;
    public LCP formPropertyDraw;
    public LCP groupObjectPropertyDraw;
    public LCP propertyDrawSIDNavigatorElementSIDPropertyDraw;

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
    public final StringClass navigatorElementCaptionClass = StringClass.get(250);
    public final StringClass propertySIDValueClass = StringClass.get(100);
    public final StringClass propertyCanonicalNameValueClass = StringClass.get(512);
    public final StringClass propertyCaptionValueClass = StringClass.get(250);
    public final StringClass propertySignatureValueClass = StringClass.get(100);
    public final StringClass propertyTableValueClass = StringClass.get(100); 
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
        propertyGroup = (ConcreteCustomClass) getClassByName("PropertyGroup");
        navigatorElement = (ConcreteCustomClass) getClassByName("NavigatorElement");
        navigatorAction = (ConcreteCustomClass) getClassByName("NavigatorAction");
        form = (ConcreteCustomClass) getClassByName("Form");
        propertyDraw = (ConcreteCustomClass) getClassByName("PropertyDraw");
        propertyDrawShowStatus = (ConcreteCustomClass) getClassByName("PropertyDrawShowStatus");
        table = (ConcreteCustomClass) getClassByName("Table");
        tableKey = (ConcreteCustomClass) getClassByName("TableKey");
        tableColumn = (ConcreteCustomClass) getClassByName("TableColumn");
        dropColumn = (ConcreteCustomClass) getClassByName("DropColumn");
        property = (ConcreteCustomClass) getClassByName("Property");
        groupObject = (ConcreteCustomClass) getClassByName("GroupObject");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        // ------- Доменная логика --------- //

        // Группы свойства
        captionPropertyGroup = findLCPByCompoundOldName("captionPropertyGroup");
        captionNavigatorElement = findLCPByCompoundOldName("captionNavigatorElement");
        parentPropertyGroup = findLCPByCompoundOldName("parentPropertyGroup");
        numberPropertyGroup = findLCPByCompoundOldName("numberPropertyGroup");
        SIDPropertyGroup = findLCPByCompoundOldName("SIDPropertyGroup");
        propertyGroupSID = findLCPByCompoundOldName("propertyGroupSID");

        // Свойства
        parentProperty = findLCPByCompoundOldName("parentProperty");
        tableSIDProperty = findLCPByCompoundOldName("tableSIDProperty");
        numberProperty = findLCPByCompoundOldName("numberProperty");
        SIDProperty = findLCPByCompoundOldName("SIDProperty");
        canonicalNameProperty = findLCPByCompoundOldName("canonicalNameProperty");
        loggableProperty = findLCPByCompoundOldName("loggableProperty");
        userLoggableProperty = findLCPByCompoundOldName("userLoggableProperty");
        storedProperty = findLCPByCompoundOldName("storedProperty");
        isSetNotNullProperty = findLCPByCompoundOldName("isSetNotNullProperty");
        signatureProperty = findLCPByCompoundOldName("signatureProperty");
        returnProperty = findLCPByCompoundOldName("returnProperty");
        classProperty = findLCPByCompoundOldName("classProperty");
        complexityProperty = findLCPByCompoundOldName("complexityProperty");
        captionProperty = findLCPByCompoundOldName("captionProperty");
        propertySID = findLCPByCompoundOldName("propertySID");
        propertyCanonicalName = findLCPByCompoundOldName("propertyCanonicalName");
        propertyTableSID = findLCPByCompoundOldName("propertyTableSID");

        // ------- Логика представлений --------- //

        // Навигатор
        sidNavigatorElement = findLCPByCompoundOldName("sidNavigatorElement");
        numberNavigatorElement = findLCPByCompoundOldName("numberNavigatorElement");
        navigatorElementSID = findLCPByCompoundOldName("navigatorElementSID");
        parentNavigatorElement = findLCPByCompoundOldName("parentNavigatorElement");
        isNavigatorElement = findLCPByCompoundOldName("isNavigatorElement");
        isForm = findLCPByCompoundOldName("isForm");
        isNavigatorAction = findLCPByCompoundOldName("isNavigatorAction");

        // ----- Формы ---- //

        // Группа объектов
        sidGroupObject = findLCPByCompoundOldName("sidGroupObject");
        navigatorElementGroupObject = findLCPByCompoundOldName("navigatorElementGroupObject");
        sidNavigatorElementGroupObject = findLCPByCompoundOldName("sidNavigatorElementGroupObject");
        groupObjectSIDGroupObjectSIDNavigatorElementGroupObject = findLCPByCompoundOldName("groupObjectSIDGroupObjectSIDNavigatorElementGroupObject");


        // PropertyDraw
        sidPropertyDraw = findLCPByCompoundOldName("sidPropertyDraw");
        captionPropertyDraw = findLCPByCompoundOldName("captionPropertyDraw");
        formPropertyDraw = findLCPByCompoundOldName("formPropertyDraw");
        groupObjectPropertyDraw = findLCPByCompoundOldName("groupObjectPropertyDraw");
        // todo : это свойство должно быть для форм, а не навигаторов
        propertyDrawSIDNavigatorElementSIDPropertyDraw = findLCPByCompoundOldName("propertyDrawSIDNavigatorElementSIDPropertyDraw");

        // UserPreferences
        showPropertyDraw = findLCPByCompoundOldName("showPropertyDraw");
        showPropertyDrawCustomUser = findLCPByCompoundOldName("showPropertyDrawCustomUser");

        nameShowPropertyDraw = findLCPByCompoundOldName("nameShowPropertyDraw");
        nameShowPropertyDrawCustomUser = findLCPByCompoundOldName("nameShowPropertyDrawCustomUser");

        columnCaptionPropertyDrawCustomUser = findLCPByCompoundOldName("columnCaptionPropertyDrawCustomUser");
        columnCaptionPropertyDraw = findLCPByCompoundOldName("columnCaptionPropertyDraw");
        
        columnWidthPropertyDrawCustomUser = findLCPByCompoundOldName("columnWidthPropertyDrawCustomUser");
        columnWidthPropertyDraw = findLCPByCompoundOldName("columnWidthPropertyDraw");

        columnOrderPropertyDrawCustomUser = findLCPByCompoundOldName("columnOrderPropertyDrawCustomUser");
        columnOrderPropertyDraw = findLCPByCompoundOldName("columnOrderPropertyDraw");

        columnSortPropertyDrawCustomUser = findLCPByCompoundOldName("columnSortPropertyDrawCustomUser");
        columnSortPropertyDraw = findLCPByCompoundOldName("columnSortPropertyDraw");

        columnAscendingSortPropertyDrawCustomUser = findLCPByCompoundOldName("columnAscendingSortPropertyDrawCustomUser");
        columnAscendingSortPropertyDraw = findLCPByCompoundOldName("columnAscendingSortPropertyDraw");

        hasUserPreferencesGroupObjectCustomUser = findLCPByCompoundOldName("hasUserPreferencesGroupObjectCustomUser");
        hasUserPreferencesGroupObject = findLCPByCompoundOldName("hasUserPreferencesGroupObject");
        hasUserPreferencesOverrideGroupObjectCustomUser = findLCPByCompoundOldName("hasUserPreferencesOverrideGroupObjectCustomUser");

        fontSizeGroupObjectCustomUser = findLCPByCompoundOldName("fontSizeGroupObjectCustomUser");
        fontSizeGroupObject = findLCPByCompoundOldName("fontSizeGroupObject");

        isFontBoldGroupObjectCustomUser = findLCPByCompoundOldName("isFontBoldGroupObjectCustomUser");
        isFontBoldGroupObject = findLCPByCompoundOldName("isFontBoldGroupObject");

        isFontItalicGroupObjectCustomUser = findLCPByCompoundOldName("isFontItalicGroupObjectCustomUser");
        isFontItalicGroupObject = findLCPByCompoundOldName("isFontItalicGroupObject");

        // группировки
        nameFormGrouping = findLCPByCompoundOldName("nameFormGrouping");
        itemQuantityFormGrouping = findLCPByCompoundOldName("itemQuantityFormGrouping");
        groupObjectFormGrouping = findLCPByCompoundOldName("groupObjectFormGrouping");
        formGroupingNameFormGroupingGroupObject = findLCPByCompoundOldName("formGroupingNameFormGroupingGroupObject");
        groupOrderFormGroupingPropertyDraw = findLCPByCompoundOldName("groupOrderFormGroupingPropertyDraw");
        sumFormGroupingPropertyDraw = findLCPByCompoundOldName("sumFormGroupingPropertyDraw");
        maxFormGroupingPropertyDraw = findLCPByCompoundOldName("maxFormGroupingPropertyDraw");
        pivotFormGroupingPropertyDraw = findLCPByCompoundOldName("pivotFormGroupingPropertyDraw");
        // ------------------------------------------------- Физическая модель ------------------------------------ //

        // Таблицы
        sidTable = findLCPByCompoundOldName("sidTable");
        tableSID = findLCPByCompoundOldName("tableSID");

        rowsTable = findLCPByCompoundOldName("rowsTable");

        // Ключи таблиц
        tableTableKey = findLCPByCompoundOldName("tableTableKey");

        sidTableKey = findLCPByCompoundOldName("sidTableKey");
        tableKeySID = findLCPByCompoundOldName("tableKeySID");

        classTableKey = findLCPByCompoundOldName("classTableKey");
        nameTableKey = findLCPByCompoundOldName("nameTableKey");

        quantityTableKey = findLCPByCompoundOldName("quantityTableKey");

        // Колонки таблиц
        tableTableColumn = findLCPByCompoundOldName("tableTableColumn");
        propertyTableColumn = findLCPByCompoundOldName("propertyTableColumn");

        sidTableColumn = findLCPByCompoundOldName("sidTableColumn");
        longSIDTableColumn = findLCPByCompoundOldName("longSIDTableColumn");
        tableColumnSID = findLCPByCompoundOldName("tableColumnSID");    
        
        quantityTableColumn = findLCPByCompoundOldName("quantityTableColumn");
        notNullQuantityTableColumn = findLCPByCompoundOldName("notNullQuantityTableColumn");

        recalculateAggregationTableColumn = getLAPByOldName("recalculateAggregationTableColumn");

        // Удаленные колонки
        sidTableDropColumn = findLCPByCompoundOldName("sidTableDropColumn");

        sidDropColumn = findLCPByCompoundOldName("sidDropColumn");
        dropColumnSID = findLCPByCompoundOldName("dropColumnSID");

        timeDropColumn = findLCPByCompoundOldName("timeDropColumn");
        revisionDropColumn = findLCPByCompoundOldName("revisionDropColumn");

        dropDropColumn = getLAPByOldName("dropDropColumn");
        //dropDropColumn.setEventAction(this, IncrementType.DROP, false, is(dropColumn), 1); // event, который при удалении колонки из системы удаляет ее из базы
    }
}

