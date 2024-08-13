package lsfusion.server.physics.admin.activity;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import org.antlr.runtime.RecognitionException;

public class UserEventsLogicsModule extends ScriptingLogicsModule {
    public LA filterPropertyAction;
    public LA filtersPropertyAction;

    public LP orders;
    public LP filters;
    public LP filterGroups;
    public LP filtersProperty;
    public LP valueFiltersProperty;

    public UserEventsLogicsModule(BusinessLogics BL, BaseLogicsModule baseModule) {
        super(baseModule, BL, "/system/UserEvents.lsf");
    }
    
    @Override
    public void initMainLogic() throws RecognitionException {
        super.initMainLogic();

        filterPropertyAction = findAction("filterProperty[STRING,STRING,STRING]");
        filtersPropertyAction = findAction("filtersProperty[STRING,STRING]");

        orders = findProperty("orders[]");
        filters = findProperty("filters[]");
        filterGroups = findProperty("filterGroups[]");
        filtersProperty = findProperty("filtersProperty[]");
        valueFiltersProperty = findProperty("valueFiltersProperty[]");
    }
}
