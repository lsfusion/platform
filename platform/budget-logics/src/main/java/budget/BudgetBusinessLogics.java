package budget;


import net.sf.jasperreports.engine.JRException;
import platform.server.auth.User;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class BudgetBusinessLogics extends BusinessLogics<BudgetBusinessLogics> {
    private BudgetLogicsModule budgetLM;

    public BudgetBusinessLogics(DataAdapter iAdapter, int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter, port);

        outputPropertyClasses();
    }

    public void createModules() {
        super.createModules();
        budgetLM = new BudgetLogicsModule(LM);
        addLogicsModule(budgetLM);
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }
}
