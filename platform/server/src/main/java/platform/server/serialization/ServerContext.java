package platform.server.serialization;

import platform.server.form.entity.FormEntity;
import platform.server.logics.BusinessLogics;

public class ServerContext {
    public final BusinessLogics<? extends BusinessLogics<?>> BL;
    public final FormEntity form;

    public ServerContext(BusinessLogics<? extends BusinessLogics<?>> BL, FormEntity form) {
        this.BL = BL;
        this.form = form;
    }
}
