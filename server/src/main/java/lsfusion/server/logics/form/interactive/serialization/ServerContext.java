package lsfusion.server.logics.form.interactive.serialization;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.physics.admin.authentication.policy.SecurityPolicy;

public class ServerContext {
    public final BusinessLogics BL;
    public final FormEntity entity;
    public final FormView view;
    public final SecurityPolicy securityPolicy;

    public ServerContext(SecurityPolicy securityPolicy, FormView view, BusinessLogics BL) {
        this.BL = BL;
        this.securityPolicy = securityPolicy;
        this.entity = view.entity;
        this.view = view;
    }
}
