package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

public interface CustomClassListener {

    Long getObject(CustomClass cls, FormEntity form, GroupObjectEntity groupObject);
    void objectChanged(ConcreteCustomClass cls, FormEntity form, GroupObjectEntity groupObject, long objectID);
    
    boolean isDeactivated(); // потом надо будет переделать на отдельный интерфейс
}
