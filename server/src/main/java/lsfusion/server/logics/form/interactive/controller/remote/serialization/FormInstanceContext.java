package lsfusion.server.logics.form.interactive.controller.remote.serialization;

import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

public class FormInstanceContext {
    // static part
    public final FormEntity entity;
    public final FormView view;

    // dynamic part
    public final SecurityPolicy securityPolicy;
    public final boolean useBootstrap;

    public FormInstanceContext(FormEntity entity, FormView view, SecurityPolicy securityPolicy, boolean useBootstrap) {
        this.entity = entity;
        this.view = view;

        this.securityPolicy = securityPolicy;
        this.useBootstrap = useBootstrap;
    }

    // when we have real context, but want to cache the result so we'll use GLOBAL context
    public static FormInstanceContext CACHE(FormEntity formEntity) {
        return formEntity.getGlobalContext();
    }
}
