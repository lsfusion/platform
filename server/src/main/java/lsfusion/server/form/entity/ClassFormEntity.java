package lsfusion.server.form.entity;

public class ClassFormEntity {

    public FormEntity form;
    public ObjectEntity object;

    public ClassFormEntity(FormEntity form, ObjectEntity object) {
        this.form = form;
        this.object = object;
    }
}
