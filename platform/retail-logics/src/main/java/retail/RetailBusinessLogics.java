package retail;

import equ.srv.EquipmentModuleProvider;
import platform.server.logics.BusinessLogics;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

import static java.util.Arrays.asList;

public class RetailBusinessLogics extends BusinessLogics<RetailBusinessLogics> implements EquipmentModuleProvider {
    ScriptingLogicsModule equipmentLM;

    @Override
    protected void createModules() throws IOException {
        super.createModules();

        addModulesFromResource(asList("scripts/"), asList("scripts/system/", "scripts/machinery/Equipment.lsf"));

        equipmentLM = addModuleFromResource("scripts/machinery/Equipment.lsf");
    }

    @Override
    public ScriptingLogicsModule getEquipmentModule() {
        return equipmentLM;
    }
}

