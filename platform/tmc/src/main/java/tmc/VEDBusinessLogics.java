package tmc;

import net.sf.jasperreports.engine.JRException;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.server.auth.SecurityPolicy;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;
import tmc.integration.PanelExternalScreen;
import tmc.integration.PanelExternalScreenParameters;
import tmc.integration.exp.CashRegController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;


public class VEDBusinessLogics extends BusinessLogics<VEDBusinessLogics> {
    public VEDLogicsModule VEDLM;

    CashRegController cashRegController = new CashRegController(VEDLM);


    public VEDBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        VEDLM = addModule(new VEDLogicsModule(LM, this, logger));
        VEDLM.setRequiredModules(Arrays.asList("System"));
        addModulesFromResource(
                "/scripts/VEDScript.lsf",
                "/scripts/Country.lsf",
                "/scripts/Utils.lsf",
                "/scripts/DefaultData.lsf",
                "/scripts/I18n.lsf",
                "/scripts/Currency.lsf",
                "/scripts/Backup.lsf");
    }

    PanelExternalScreen panelScreen;

    @Override
    protected void initExternalScreens() {
        panelScreen = new PanelExternalScreen();
        addExternalScreen(panelScreen);
    }

    private Integer getPanelComPort(int compId) {
        try {
            DataSession session = createSession();

            Integer result = (Integer) VEDLM.panelScreenComPort.read(session, new DataObject(compId, LM.computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getCashRegComPort(int compId) {
        try {
            DataSession session = createSession();

            Integer result = (Integer) VEDLM.cashRegComPort.read(session, new DataObject(compId, LM.computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        if (panelScreen.getID() == screenID) {
            return new PanelExternalScreenParameters(getPanelComPort(computerId), getCashRegComPort(computerId));
        }
        return null;
    }


    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        SecurityPolicy permitCachRegister = addPolicy("Разрешить только кассовые формы", "Политика разрешает открытие только форма для работы за кассой.");
        permitCachRegister.navigator.defaultPermission = false;
        permitCachRegister.navigator.permit(VEDLM.commitSaleForm);
        permitCachRegister.navigator.permit(VEDLM.saleCheckCertForm);
        permitCachRegister.navigator.permit(VEDLM.returnSaleCheckRetailArticleForm);
        permitCachRegister.navigator.permit(VEDLM.cachRegManagementForm);

        //админ игнорит настройки в базе, ему разрешено всё
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }
}
