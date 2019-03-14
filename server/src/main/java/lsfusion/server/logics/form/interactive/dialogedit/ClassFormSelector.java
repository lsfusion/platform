package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.NullValue;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.sql.SQLException;

public class ClassFormSelector implements FormSelector<ClassFormSelector.VirtualObject> {
    
    private final CustomClass cls;
    private final boolean edit;

    @Override
    public ValueClass getBaseClass(VirtualObject object) {
        assert object == virtualObject;
        return cls;
    }

    public class VirtualObject implements ObjectSelector {
        public boolean noClasses() {
            return false;
        }
    }
    public final VirtualObject virtualObject = new VirtualObject(); 

    public ClassFormSelector(CustomClass cls, boolean edit) {
        this.cls = cls;
        this.edit = edit;
        cls.markUsed(edit);
    }

    @Override
    public FormEntity getStaticForm() {
        return null;
    }

    @Override
    public Pair<FormEntity, ImRevMap<ObjectEntity, VirtualObject>> getForm(BaseLogicsModule LM, DataSession session, ImMap<VirtualObject, ? extends ObjectValue> mapObjectValues) throws SQLException, SQLHandledException {
        assert mapObjectValues.isEmpty() || mapObjectValues.singleKey() == virtualObject;
        
        ClassFormEntity formEntity;
        if(edit)
            formEntity = cls.getEditForm(LM, session, mapObjectValues.isEmpty() ? NullValue.instance : mapObjectValues.singleValue());
        else
            formEntity = cls.getDialogForm(LM);
        if(formEntity == null)
            return null;
        
        return new Pair<>((FormEntity) formEntity.form, MapFact.singletonRev(formEntity.object, virtualObject));
    }
}
