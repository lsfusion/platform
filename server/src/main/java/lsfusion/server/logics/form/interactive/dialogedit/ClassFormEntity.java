package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

public class ClassFormEntity {

    public FormEntity form;
    public ObjectEntity object;

    public ClassFormEntity(FormEntity form, ObjectEntity object) {
        this.form = form;
        this.object = object;
    }
}
