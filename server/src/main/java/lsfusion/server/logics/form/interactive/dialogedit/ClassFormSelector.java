package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
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

        @Override
        public Type getType() {
            return ObjectType.instance;
        }
    }
    public final VirtualObject virtualObject = new VirtualObject(); 

    public ClassFormSelector(CustomClass cls, boolean edit) {
        this.cls = cls;
        this.edit = edit;
        cls.markUsed(edit);
    }

    @Override
    public FormEntity getNFStaticForm() {
        return null;
    }

    @Override
    public Pair<FormEntity, ImRevMap<ObjectEntity, VirtualObject>> getForm(BaseLogicsModule LM, DataSession session, ImMap<VirtualObject, ? extends ObjectValue> mapObjectValues) throws SQLException, SQLHandledException {
        assert mapObjectValues.isEmpty() || mapObjectValues.singleKey() == virtualObject;

        ObjectValue concreteObject = mapObjectValues.isEmpty() ? NullValue.instance : mapObjectValues.singleValue();
        ConcreteCustomClass concreteCustomClass = null;
        if(edit) {
            ConcreteClass concreteClass;
            if (session != null && concreteObject instanceof DataObject && (concreteClass = session.getCurrentClass((DataObject) concreteObject)) instanceof ConcreteCustomClass)
                concreteCustomClass = (ConcreteCustomClass) concreteClass;
        }

        ClassFormEntity formEntity = getForm(LM, concreteCustomClass);
        if(formEntity == null)
            return null;
        
        return new Pair<>(formEntity.form, MapFact.singletonRev(formEntity.object, virtualObject));
    }

    @Override
    public FormEntity getStaticForm(BaseLogicsModule LM, CustomClass customClass) {
        if(customClass == null)
            return getStaticForm(LM);

        ClassFormEntity form = getForm(LM, customClass);
        if(form != null)
            return form.form;
        return null;
    }

    public ClassFormEntity getForm(BaseLogicsModule LM, CustomClass customClass) {
        if(edit)
            return cls.getEditForm(LM, customClass);
        else
            return cls.getDialogForm(LM);
    }

    @Override
    public FormSelector<VirtualObject> merge(FormSelector formSelector) {
        if(!(formSelector instanceof ClassFormSelector))
            return null;

        ClassFormSelector classFormSelector = (ClassFormSelector) formSelector;
        if(edit != classFormSelector.edit)
            return null;

        return new ClassFormSelector(merge(cls, classFormSelector.cls), edit);
    }

    public static CustomClass merge(CustomClass cls1, CustomClass cls2) {
        return cls1.getUpSet().getOr().or(cls2.getUpSet().getOr()).getCommonClass();
    }
}
