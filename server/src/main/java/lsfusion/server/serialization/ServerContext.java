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

    public ServerContext(BusinessLogics<?> BL) {
        this(BL, null);
    }

    public ServerContext(BusinessLogics<?> BL, FormEntity entity) {
        this(BL, entity, null);
    }

    public ServerContext(SecurityPolicy securityPolicy, FormView view) {
        this(null, securityPolicy, view.entity, view);
    }

    public ServerContext(BusinessLogics<?> BL, FormEntity entity, FormView view) {
        this(BL, null, entity, view);
    }

    public ServerContext(BusinessLogics<?> BL, SecurityPolicy securityPolicy, FormEntity entity, FormView view) {
        this.BL = BL;
        this.securityPolicy = securityPolicy;
        this.entity = entity;
        this.view = view;
    }
}
