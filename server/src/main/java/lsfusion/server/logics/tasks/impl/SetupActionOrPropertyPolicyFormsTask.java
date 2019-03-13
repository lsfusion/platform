package lsfusion.server.logics.tasks.impl;

import lsfusion.base.col.SetFact;
import lsfusion.server.SystemProperties;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public abstract class SetupActionOrPropertyPolicyFormsTask extends GroupPropertiesTask {
    
    protected LAP<?> setupPolicyByCN;

    protected abstract FormEntity getForm();
    protected abstract LCP getCanonicalName();
    
    @Override
    protected boolean prerun() {
        if (SystemProperties.lightStart) {
            return false;
        }

        BusinessLogics BL = getBL();
        FormEntity formEntity = getForm();
        ObjectEntity obj = formEntity.getObject("p");
        LAP<?> setupPolicyForm = BL.LM.addMFAProp(LocalizedString.NONAME, formEntity, SetFact.singletonOrder(obj), true);
        setupPolicyByCN = BL.LM.addJoinAProp(setupPolicyForm, getCanonicalName(), 1);
        return true;
    }
}
