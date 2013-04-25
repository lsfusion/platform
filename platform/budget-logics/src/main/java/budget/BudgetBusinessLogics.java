package budget;


import platform.server.logics.BusinessLogics;

import java.io.IOException;

public class BudgetBusinessLogics extends BusinessLogics<BudgetBusinessLogics> {
    private BudgetLogicsModule budgetLM;

    public void createModules() throws IOException {
        super.createModules();
        addModulesFromResource(
                "scripts/utils/DefaultData.lsf",
                "scripts/utils/Utils.lsf",
                "scripts/masterdata/MasterData.lsf",
                "scripts/masterdata/Currency.lsf");

        budgetLM = addModule(new BudgetLogicsModule(LM, this));
    }
}
