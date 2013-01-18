package retail;

import equ.srv.EquipmentServer;
import equ.srv.EquipmentServerHolder;
import net.sf.jasperreports.engine.JRException;
import platform.server.auth.SecurityPolicy;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class RetailBusinessLogics extends BusinessLogics<RetailBusinessLogics> implements EquipmentServerHolder {
    EquipmentServer equipmentServer;

    public RetailBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        addModulesFromResource(Arrays.asList("/scripts"), Arrays.asList("/scripts/system", "/scripts/machinery/Equipment.lsf"));
        equipmentServer = new EquipmentServer(addModuleFromResource("/scripts/machinery/Equipment.lsf"));
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.add(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public EquipmentServer getEquipmentServer() {
        return equipmentServer;
    }
}

