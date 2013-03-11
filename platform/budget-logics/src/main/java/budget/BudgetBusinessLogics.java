package budget;


import platform.server.logics.BusinessLogics;

import java.io.IOException;

public class BudgetBusinessLogics extends BusinessLogics<BudgetBusinessLogics> {
    private BudgetLogicsModule budgetLM;

    public void createModules() throws IOException {
        super.createModules();
        budgetLM = addModule(new BudgetLogicsModule(LM, this));

        addModulesFromResource(
                "scripts/utils/DefaultData.lsf",
                "scripts/masterdata/MasterData.lsf",
                "scripts/masterdata/Currency.lsf");
    }
}
