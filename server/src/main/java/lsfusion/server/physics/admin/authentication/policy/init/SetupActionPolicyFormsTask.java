package lsfusion.server.physics.admin.authentication.policy.init;

import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.action.ActionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class SetupActionPolicyFormsTask extends SetupActionOrPropertyPolicyFormsTask {

    public String getCaption() {
        return "Setup action policy";
    }

    @Override
    protected FormEntity getForm() {
        return getBL().securityLM.actionPolicyForm;
    }

    @Override
    protected LCP getCanonicalName() {
        return getBL().reflectionLM.actionCanonicalName;
    }

    @Override
    protected void runTask(ActionOrProperty property) {
        if(property instanceof ActionProperty)
            getBL().setupPropertyPolicyForms(setupPolicyByCN, property, true);
    }
}
