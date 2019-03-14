package lsfusion.server.physics.admin.authentication.policy.init;

import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.oraction.Property;

public class SetupPropertyPolicyFormsTask extends SetupActionOrPropertyPolicyFormsTask {

    public String getCaption() {
        return "Setup property policy";
    }

    @Override
    protected FormEntity getForm() {
        return getBL().securityLM.propertyPolicyForm;
    }

    @Override
    protected LCP getCanonicalName() {
        return getBL().reflectionLM.propertyCanonicalName;
    }

    @Override
    protected void runTask(Property property) {
        if(property instanceof CalcProperty)
            getBL().setupPropertyPolicyForms(setupPolicyByCN, property, false);
    }
}
