package rublevski;

import net.sf.jasperreports.engine.JRException;
import platform.server.auth.SecurityPolicy;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.scripted.ScriptingLogicsModule;
import retail.api.remote.PriceTransaction;
import retail.api.remote.RetailRemoteInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: DAle
 * Date: 05.01.12
 * Time: 15:34
 */


public class RublevskiBusinessLogics extends BusinessLogics<RublevskiBusinessLogics> implements RetailRemoteInterface {
    ScriptingLogicsModule rublevskiLM;

    public RublevskiBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    public ScriptingLogicsModule getLM() {
        return rublevskiLM;
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        rublevskiLM = new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Rublevski.lsf"), LM, this);
        addLogicsModule(rublevskiLM);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }

    @Override
    public PriceTransaction readNextPriceTransaction(String equServerID) throws RemoteException {
        return new PriceTransaction();
    }
}

