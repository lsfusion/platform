package lsfusion.server.logics.form.open;

import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.util.Collections;
import java.util.List;

public class MappedForm<O extends ObjectSelector> {
    public final FormSelector<O> form;
    
    public final List<O> objects;

    public MappedForm(FormSelector<O> form, List<O> objects) {
        this.form = form;
        this.objects = objects;
    }
    
    public static MappedForm create(FormEntity form, List<ObjectEntity> objects) {
        return new MappedForm(form, objects);
    }
    
    public static MappedForm<ClassFormSelector.VirtualObject> create(CustomClass cls, boolean edit) {
        ClassFormSelector selector = new ClassFormSelector(cls, edit);
        return new MappedForm<>(selector, Collections.singletonList(selector.virtualObject));
    }
}
