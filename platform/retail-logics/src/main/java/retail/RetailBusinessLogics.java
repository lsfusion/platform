package retail;

import platform.server.logics.BusinessLogics;

import java.io.IOException;

import static java.util.Arrays.asList;

public class RetailBusinessLogics extends BusinessLogics<RetailBusinessLogics> {
    @Override
    protected void createModules() throws IOException {
        super.createModules();
        addModulesFromResource(asList("scripts/"), asList("scripts/system/"));
    }
}

