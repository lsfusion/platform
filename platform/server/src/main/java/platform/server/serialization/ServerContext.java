package platform.server.serialization;

import platform.server.form.entity.FormEntity;
import platform.server.logics.BusinessLogics;

public class ServerContext {
    public final BusinessLogics<?> BL;
    public final FormEntity form;

    public ServerContext(BusinessLogics<?> BL, FormEntity form) {
        this.BL = BL;
        this.form = form;
    }
}
