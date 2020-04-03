package lsfusion.server.logics.form.interactive.controller.remote.serialization;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.physics.admin.authentication.security.policy.BaseSecurityPolicy;

public class ServerContext {
    public final BusinessLogics BL;
    public final FormEntity entity;
    public final FormView view;
    public final BaseSecurityPolicy securityPolicy;

    public ServerContext(BaseSecurityPolicy securityPolicy, FormView view, BusinessLogics BL) {
        this.BL = BL;
        this.securityPolicy = securityPolicy;
        this.entity = view.entity;
        this.view = view;
    }
}
