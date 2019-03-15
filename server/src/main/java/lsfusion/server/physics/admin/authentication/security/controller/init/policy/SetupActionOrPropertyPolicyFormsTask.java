package lsfusion.server.physics.admin.authentication.security.controller.init.policy;

import lsfusion.base.col.SetFact;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.language.linear.LA;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.init.GroupPropertiesTask;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class SetupActionOrPropertyPolicyFormsTask extends GroupPropertiesTask {
    
    protected LA<?> setupPolicyByCN;

    protected abstract FormEntity getForm();
    protected abstract LP getCanonicalName();
    
    @Override
    protected boolean prerun() {
        if (SystemProperties.lightStart) {
            return false;
        }

        BusinessLogics BL = getBL();
        FormEntity formEntity = getForm();
        ObjectEntity obj = formEntity.getObject("p");
        LA<?> setupPolicyForm = BL.LM.addMFAProp(LocalizedString.NONAME, formEntity, SetFact.singletonOrder(obj), true);
        setupPolicyByCN = BL.LM.addJoinAProp(setupPolicyForm, getCanonicalName(), 1);
        return true;
    }
}
