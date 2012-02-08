package rublevski;

import net.sf.jasperreports.engine.JRException;
import platform.interop.action.MessageClientAction;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: DAle
 * Date: 05.01.12
 * Time: 15:34
 */


public class RublevskiBusinessLogics extends BusinessLogics<RublevskiBusinessLogics> {
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
}

