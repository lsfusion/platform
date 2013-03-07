package tmc;

import platform.interop.form.screen.ExternalScreenParameters;
import platform.server.auth.SecurityPolicy;
import platform.server.logics.*;
import platform.server.logics.SecurityManager;
import platform.server.session.DataSession;
import tmc.integration.PanelExternalScreen;
import tmc.integration.PanelExternalScreenParameters;
import tmc.integration.exp.CashRegController;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;


public class VEDBusinessLogics extends BusinessLogics<VEDBusinessLogics> {
    public VEDLogicsModule VEDLM;

    PanelExternalScreen panelScreen;
    CashRegController cashRegController = new CashRegController(VEDLM);

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        VEDLM = addModule(new VEDLogicsModule(LM, this, logger));
        addModulesFromResource(
                "scripts/VEDScript.lsf",
                "scripts/masterdata/Country.lsf",
                "scripts/utils/DefaultData.lsf",
                "scripts/masterdata/Currency.lsf",
                "scripts/utils/Backup.lsf");
    }

    @Override
    protected void initExternalScreens() {
        panelScreen = new PanelExternalScreen();
        addExternalScreen(panelScreen);
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        if (panelScreen.getID() == screenID) {
            return new PanelExternalScreenParameters(getPanelComPort(computerId), getCashRegComPort(computerId));
        }
        return null;
    }

    private Integer getCashRegComPort(int compId) {
        try {
            DataSession session = getDbManager().createSession();

            Integer result = (Integer) VEDLM.cashRegComPort.read(session, new DataObject(compId, LM.computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getPanelComPort(int compId) {
        try {
            DataSession session = getDbManager().createSession();

            Integer result = (Integer) VEDLM.panelScreenComPort.read(session, new DataObject(compId, LM.computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected void initAuthentication(SecurityManager securityManager) throws SQLException {
        super.initAuthentication(securityManager);

        SecurityPolicy permitCachRegisterPolicy = securityManager.addPolicy("Разрешить только кассовые формы", "Политика разрешает открытие только форма для работы за кассой.");
        permitCachRegisterPolicy.navigator.defaultPermission = false;
        permitCachRegisterPolicy.navigator.permit(VEDLM.commitSaleForm);
        permitCachRegisterPolicy.navigator.permit(VEDLM.saleCheckCertForm);
        permitCachRegisterPolicy.navigator.permit(VEDLM.returnSaleCheckRetailArticleForm);
        permitCachRegisterPolicy.navigator.permit(VEDLM.cachRegManagementForm);
    }
}
