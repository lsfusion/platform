package lsfusion.server.logics;

import org.antlr.runtime.RecognitionException;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

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
        super(ReflectionLogicsModule.class.getResourceAsStream("/scripts/system/Reflection.lsf"), baseLM, BL);
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
        captionPropertyGroup = getLCPByName("captionPropertyGroup");
        captionNavigatorElement = getLCPByName("captionNavigatorElement");
        parentPropertyGroup = getLCPByName("parentPropertyGroup");
        numberPropertyGroup = getLCPByName("numberPropertyGroup");
        SIDPropertyGroup = getLCPByName("SIDPropertyGroup");
        propertyGroupSID = getLCPByName("propertyGroupSID");

        // Свойства
        parentProperty = getLCPByName("parentProperty");
        numberProperty = getLCPByName("numberProperty");
        SIDProperty = getLCPByName("SIDProperty");
        loggableProperty = getLCPByName("loggableProperty");
        userLoggableProperty = getLCPByName("userLoggableProperty");
        storedProperty = getLCPByName("storedProperty");
        isSetNotNullProperty = getLCPByName("isSetNotNullProperty");
        signatureProperty = getLCPByName("signatureProperty");
        returnProperty = getLCPByName("returnProperty");
        classProperty = getLCPByName("classProperty");
        complexityProperty = getLCPByName("complexityProperty");
        captionProperty = getLCPByName("captionProperty");
        propertySID = getLCPByName("propertySID");

        // ------- Логика представлений --------- //

        // Навигатор
        sidNavigatorElement = getLCPByName("sidNavigatorElement");
        numberNavigatorElement = getLCPByName("numberNavigatorElement");
        navigatorElementSID = getLCPByName("navigatorElementSID");
        parentNavigatorElement = getLCPByName("parentNavigatorElement");
        isNavigatorElement = getLCPByName("isNavigatorElement");
        isForm = getLCPByName("isForm");
        isNavigatorAction = getLCPByName("isNavigatorAction");

        // ----- Формы ---- //

        // Группа объектов
        sidGroupObject = getLCPByName("sidGroupObject");
        navigatorElementGroupObject = getLCPByName("navigatorElementGroupObject");
        sidNavigatorElementGroupObject = getLCPByName("sidNavigatorElementGroupObject");
        groupObjectSIDGroupObjectSIDNavigatorElementGroupObject = getLCPByName("groupObjectSIDGroupObjectSIDNavigatorElementGroupObject");


        // PropertyDraw
        sidPropertyDraw = getLCPByName("sidPropertyDraw");
        captionPropertyDraw = getLCPByName("captionPropertyDraw");
        formPropertyDraw = getLCPByName("formPropertyDraw");
        groupObjectPropertyDraw = getLCPByName("groupObjectPropertyDraw");
        // todo : это свойство должно быть для форм, а не навигаторов
        propertyDrawSIDNavigatorElementSIDPropertyDraw = getLCPByName("propertyDrawSIDNavigatorElementSIDPropertyDraw");

        // UserPreferences
        showPropertyDraw = getLCPByName("showPropertyDraw");
        showPropertyDrawCustomUser = getLCPByName("showPropertyDrawCustomUser");

        nameShowOverridePropertyDrawCustomUser = getLCPByName("nameShowOverridePropertyDrawCustomUser");

        columnWidthPropertyDrawCustomUser = getLCPByName("columnWidthPropertyDrawCustomUser");
        columnWidthPropertyDraw = getLCPByName("columnWidthPropertyDraw");
        columnWidthOverridePropertyDrawCustomUser = getLCPByName("columnWidthOverridePropertyDrawCustomUser");

        columnOrderPropertyDrawCustomUser = getLCPByName("columnOrderPropertyDrawCustomUser");
        columnOrderPropertyDraw = getLCPByName("columnOrderPropertyDraw");
        columnOrderOverridePropertyDrawCustomUser = getLCPByName("columnOrderOverridePropertyDrawCustomUser");

        columnSortPropertyDrawCustomUser = getLCPByName("columnSortPropertyDrawCustomUser");
        columnSortPropertyDraw = getLCPByName("columnSortPropertyDraw");
        columnSortOverridePropertyDrawCustomUser = getLCPByName("columnSortOverridePropertyDrawCustomUser");

        columnAscendingSortPropertyDrawCustomUser = getLCPByName("columnAscendingSortPropertyDrawCustomUser");
        columnAscendingSortPropertyDraw = getLCPByName("columnAscendingSortPropertyDraw");
        columnAscendingSortOverridePropertyDrawCustomUser = getLCPByName("columnAscendingSortOverridePropertyDrawCustomUser");

        hasUserPreferencesGroupObjectCustomUser = getLCPByName("hasUserPreferencesGroupObjectCustomUser");
        hasUserPreferencesGroupObject = getLCPByName("hasUserPreferencesGroupObject");
        hasUserPreferencesOverrideGroupObjectCustomUser = getLCPByName("hasUserPreferencesOverrideGroupObjectCustomUser");

        // ------------------------------------------------- Физическая модель ------------------------------------ //

        // Таблицы
        sidTable = getLCPByName("sidTable");
        tableSID = getLCPByName("tableSID");

        rowsTable = getLCPByName("rowsTable");

        // Ключи таблиц
        tableTableKey = getLCPByName("tableTableKey");

        sidTableKey = getLCPByName("sidTableKey");
        tableKeySID = getLCPByName("tableKeySID");

        classTableKey = getLCPByName("classTableKey");
        nameTableKey = getLCPByName("nameTableKey");

        quantityTableKey = getLCPByName("quantityTableKey");

        // Колонки таблиц
        tableTableColumn = getLCPByName("tableTableColumn");

        sidTableColumn = getLCPByName("sidTableColumn");
        tableColumnSID = getLCPByName("tableColumnSID");

        quantityTableColumn = getLCPByName("quantityTableColumn");
        notNullQuantityTableColumn = getLCPByName("notNullQuantityTableColumn");

        recalculateAggregationTableColumn = getLAPByName("recalculateAggregationTableColumn");

        // Удаленные колонки
        sidTableDropColumn = getLCPByName("sidTableDropColumn");

        sidDropColumn = getLCPByName("sidDropColumn");
        dropColumnSID = getLCPByName("dropColumnSID");

        timeDropColumn = getLCPByName("timeDropColumn");
        revisionDropColumn = getLCPByName("revisionDropColumn");

        dropDropColumn = getLAPByName("dropDropColumn");
        //dropDropColumn.setEventAction(this, IncrementType.DROP, false, is(dropColumn), 1); // event, который при удалении колонки из системы удаляет ее из базы
    }
}

