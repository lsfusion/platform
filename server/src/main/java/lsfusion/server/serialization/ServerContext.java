package lsfusion.server.serialization;

import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.BusinessLogics;

public class ServerContext {
    public final BusinessLogics<?> BL;
    public final FormEntity entity;
    public final FormView view;
    public final SecurityPolicy securityPolicy;

    public ServerContext(SecurityPolicy securityPolicy, FormView view) {
        this.BL = null;
        this.securityPolicy = securityPolicy;
        this.entity = view.entity;
        this.view = view;
    }
}
