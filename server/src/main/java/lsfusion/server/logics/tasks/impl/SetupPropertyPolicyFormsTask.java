package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public class SetupPropertyPolicyFormsTask extends GroupPropertiesTask {

    LAP<?> setupPolicyForPropByCN;

    public String getCaption() {
        return "Setup property policy";
    }

    @Override
    protected boolean prerun() {
        if (SystemProperties.isDebug) {
            return false;
        }

        BusinessLogics BL = getBL();
        FormEntity policyFormEntity = BL.securityLM.propertyPolicyForm;
        ObjectEntity propertyObj = policyFormEntity.getObject("p");
        LAP<?> setupPolicyFormProperty = BL.LM.addMFAProp(null, LocalizedString.create("sys"), policyFormEntity, new ObjectEntity[]{propertyObj}, true);
        setupPolicyForPropByCN = BL.LM.addJoinAProp(setupPolicyFormProperty, BL.reflectionLM.propertyCanonicalName, 1);
        return true;
    }

    @Override
    protected void runTask(Property property) {
        getBL().setupPropertyPolicyForms(setupPolicyForPropByCN, property);
    }
}
