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
    public LCP loggableProperty;
    public LCP userLoggableProperty;
    public LCP storedProperty;
    public LCP isSetNotNullProperty;
    public LCP signatureProperty;
    public LCP returnProperty;
    public LCP classProperty;
    public LCP complexityProperty;
    public LCP captionProperty;
    public LCP propertySID;

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
    public LCP sidTableColumn;
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
    public final StringClass propertyCaptionValueClass = StringClass.get(250);
    public final StringClass propertySignatureValueClass = StringClass.get(100);
    public final LogicalClass propertyLoggableValueClass = LogicalClass.instance;
    public final LogicalClass propertyStoredValueClass = LogicalClass.instance;
    public final LogicalClass propertyIsSetNotNullValueClass = LogicalClass.instance;

    public ReflectionLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ReflectionLogicsModule.class.getResourceAsStream("/lsfusion/system/Reflection.lsf"), baseLM, BL);
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
        captionPropertyGroup = getLCPByOldName("captionPropertyGroup");
        captionNavigatorElement = getLCPByOldName("captionNavigatorElement");
        parentPropertyGroup = getLCPByOldName("parentPropertyGroup");
        numberPropertyGroup = getLCPByOldName("numberPropertyGroup");
        SIDPropertyGroup = getLCPByOldName("SIDPropertyGroup");
        propertyGroupSID = getLCPByOldName("propertyGroupSID");

        // Свойства
        parentProperty = getLCPByOldName("parentProperty");
        numberProperty = getLCPByOldName("numberProperty");
        SIDProperty = getLCPByOldName("SIDProperty");
        loggableProperty = getLCPByOldName("loggableProperty");
        userLoggableProperty = getLCPByOldName("userLoggableProperty");
        storedProperty = getLCPByOldName("storedProperty");
        isSetNotNullProperty = getLCPByOldName("isSetNotNullProperty");
        signatureProperty = getLCPByOldName("signatureProperty");
        returnProperty = getLCPByOldName("returnProperty");
        classProperty = getLCPByOldName("classProperty");
        complexityProperty = getLCPByOldName("complexityProperty");
        captionProperty = getLCPByOldName("captionProperty");
        propertySID = getLCPByOldName("propertySID");

        // ------- Логика представлений --------- //

        // Навигатор
        sidNavigatorElement = getLCPByOldName("sidNavigatorElement");
        numberNavigatorElement = getLCPByOldName("numberNavigatorElement");
        navigatorElementSID = getLCPByOldName("navigatorElementSID");
        parentNavigatorElement = getLCPByOldName("parentNavigatorElement");
        isNavigatorElement = getLCPByOldName("isNavigatorElement");
        isForm = getLCPByOldName("isForm");
        isNavigatorAction = getLCPByOldName("isNavigatorAction");

        // ----- Формы ---- //

        // Группа объектов
        sidGroupObject = getLCPByOldName("sidGroupObject");
        navigatorElementGroupObject = getLCPByOldName("navigatorElementGroupObject");
        sidNavigatorElementGroupObject = getLCPByOldName("sidNavigatorElementGroupObject");
        groupObjectSIDGroupObjectSIDNavigatorElementGroupObject = getLCPByOldName("groupObjectSIDGroupObjectSIDNavigatorElementGroupObject");


        // PropertyDraw
        sidPropertyDraw = getLCPByOldName("sidPropertyDraw");
        captionPropertyDraw = getLCPByOldName("captionPropertyDraw");
        formPropertyDraw = getLCPByOldName("formPropertyDraw");
        groupObjectPropertyDraw = getLCPByOldName("groupObjectPropertyDraw");
        // todo : это свойство должно быть для форм, а не навигаторов
        propertyDrawSIDNavigatorElementSIDPropertyDraw = getLCPByOldName("propertyDrawSIDNavigatorElementSIDPropertyDraw");

        // UserPreferences
        showPropertyDraw = getLCPByOldName("showPropertyDraw");
        showPropertyDrawCustomUser = getLCPByOldName("showPropertyDrawCustomUser");

        nameShowPropertyDraw = getLCPByOldName("nameShowPropertyDraw");
        nameShowPropertyDrawCustomUser = getLCPByOldName("nameShowPropertyDrawCustomUser");

        columnWidthPropertyDrawCustomUser = getLCPByOldName("columnWidthPropertyDrawCustomUser");
        columnWidthPropertyDraw = getLCPByOldName("columnWidthPropertyDraw");

        columnOrderPropertyDrawCustomUser = getLCPByOldName("columnOrderPropertyDrawCustomUser");
        columnOrderPropertyDraw = getLCPByOldName("columnOrderPropertyDraw");

        columnSortPropertyDrawCustomUser = getLCPByOldName("columnSortPropertyDrawCustomUser");
        columnSortPropertyDraw = getLCPByOldName("columnSortPropertyDraw");

        columnAscendingSortPropertyDrawCustomUser = getLCPByOldName("columnAscendingSortPropertyDrawCustomUser");
        columnAscendingSortPropertyDraw = getLCPByOldName("columnAscendingSortPropertyDraw");

        hasUserPreferencesGroupObjectCustomUser = getLCPByOldName("hasUserPreferencesGroupObjectCustomUser");
        hasUserPreferencesGroupObject = getLCPByOldName("hasUserPreferencesGroupObject");
        hasUserPreferencesOverrideGroupObjectCustomUser = getLCPByOldName("hasUserPreferencesOverrideGroupObjectCustomUser");

        fontSizeGroupObjectCustomUser = getLCPByOldName("fontSizeGroupObjectCustomUser");
        fontSizeGroupObject = getLCPByOldName("fontSizeGroupObject");

        isFontBoldGroupObjectCustomUser = getLCPByOldName("isFontBoldGroupObjectCustomUser");
        isFontBoldGroupObject = getLCPByOldName("isFontBoldGroupObject");

        isFontItalicGroupObjectCustomUser = getLCPByOldName("isFontItalicGroupObjectCustomUser");
        isFontItalicGroupObject = getLCPByOldName("isFontItalicGroupObject");

        // группировки
        nameFormGrouping = getLCPByOldName("nameFormGrouping");
        itemQuantityFormGrouping = getLCPByOldName("itemQuantityFormGrouping");
        groupObjectFormGrouping = getLCPByOldName("groupObjectFormGrouping");
        formGroupingNameFormGroupingGroupObject = getLCPByOldName("formGroupingNameFormGroupingGroupObject");
        groupOrderFormGroupingPropertyDraw = getLCPByOldName("groupOrderFormGroupingPropertyDraw");
        sumFormGroupingPropertyDraw = getLCPByOldName("sumFormGroupingPropertyDraw");
        maxFormGroupingPropertyDraw = getLCPByOldName("maxFormGroupingPropertyDraw");

        // ------------------------------------------------- Физическая модель ------------------------------------ //

        // Таблицы
        sidTable = getLCPByOldName("sidTable");
        tableSID = getLCPByOldName("tableSID");

        rowsTable = getLCPByOldName("rowsTable");

        // Ключи таблиц
        tableTableKey = getLCPByOldName("tableTableKey");

        sidTableKey = getLCPByOldName("sidTableKey");
        tableKeySID = getLCPByOldName("tableKeySID");

        classTableKey = getLCPByOldName("classTableKey");
        nameTableKey = getLCPByOldName("nameTableKey");

        quantityTableKey = getLCPByOldName("quantityTableKey");

        // Колонки таблиц
        tableTableColumn = getLCPByOldName("tableTableColumn");

        sidTableColumn = getLCPByOldName("sidTableColumn");
        tableColumnSID = getLCPByOldName("tableColumnSID");

        quantityTableColumn = getLCPByOldName("quantityTableColumn");
        notNullQuantityTableColumn = getLCPByOldName("notNullQuantityTableColumn");

        recalculateAggregationTableColumn = getLAPByOldName("recalculateAggregationTableColumn");

        // Удаленные колонки
        sidTableDropColumn = getLCPByOldName("sidTableDropColumn");

        sidDropColumn = getLCPByOldName("sidDropColumn");
        dropColumnSID = getLCPByOldName("dropColumnSID");

        timeDropColumn = getLCPByOldName("timeDropColumn");
        revisionDropColumn = getLCPByOldName("revisionDropColumn");

        dropDropColumn = getLAPByOldName("dropDropColumn");
        //dropDropColumn.setEventAction(this, IncrementType.DROP, false, is(dropColumn), 1); // event, который при удалении колонки из системы удаляет ее из базы
    }
}

