package platform.server.serialization;

import platform.server.form.entity.FormEntity;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;

public class ServerContext {
    public final BusinessLogics<?> BL;
    public final FormEntity entity;
    public final FormView view;

    public ServerContext(BusinessLogics<?> BL) {
        this(BL, null);
    }

    public ServerContext(BusinessLogics<?> BL, FormEntity entity) {
        this(BL, entity, null);
    }

    public ServerContext(FormView view) {
        this(null, null, view);
    }

    public ServerContext(BusinessLogics<?> BL, FormEntity entity, FormView view) {
        this.BL = BL;
        this.entity = entity;
        this.view = view;
    }
}
