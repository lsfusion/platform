package retail;

import net.sf.jasperreports.engine.JRException;
import platform.server.auth.SecurityPolicy;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Paval
 * Date: 03.06.11
 * Time: 11:47
 * To change this template use File | Settings | File Templates.
 */

public class RetailBusinessLogics extends BusinessLogics<RetailBusinessLogics> {
    RetailLogicsModule RetailLM;

    public RetailBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        RetailLM = new RetailLogicsModule(LM, this);
        addLogicsModule(RetailLM);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }


    @Override
    public BusinessLogics getBL() {
        return this;
    }
}
