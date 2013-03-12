package skolkovo;

import platform.server.logics.BusinessLogics;
import platform.server.logics.SecurityManager;
import platform.server.logics.property.Property;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.SQLException;

public class SkolkovoBusinessLogics extends BusinessLogics<SkolkovoBusinessLogics> {

    SkolkovoLogicsModule SkolkovoLM;
    ScriptingLogicsModule I18n;

    @Override
    public void createModules() throws IOException {
        super.createModules();
        SkolkovoLM = addModule(new SkolkovoLogicsModule(LM, emailLM, this));
        I18n = addModuleFromResource("scripts/utils/I18n.lsf");
        addModulesFromResource(
                "scripts/masterdata/Currency.lsf",
                "scripts/masterdata/Country.lsf",
                "scripts/masterdata/MasterData.lsf",
                "scripts/utils/DefaultData.lsf",
                "scripts/utils/Historizable.lsf",
                "scripts/utils/Utils.lsf");
    }

    protected void initAuthentication(SecurityManager securityManager) throws SQLException {
        super.initAuthentication(securityManager);

        securityManager.defaultPolicy.navigator.deny(LM.administration, LM.objects, SkolkovoLM.languageDocumentTypeForm, SkolkovoLM.globalForm);

        securityManager.defaultPolicy.property.view.deny(LM.userPassword);

        securityManager.defaultPolicy.property.change.deny(SkolkovoLM.dateStartVote, SkolkovoLM.dateEndVote, SkolkovoLM.inExpertVote, SkolkovoLM.oldExpertVote, SkolkovoLM.voteResultExpertVote, SkolkovoLM.doneExpertVote);

        for (Property property : SkolkovoLM.voteResultGroup.getProperties()) {
            securityManager.defaultPolicy.property.change.deny(property);
        }
    }
}
