package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class DialogFormEntity extends BaseClassFormEntity {

    public DialogFormEntity(BaseLogicsModule LM, CustomClass cls) {
        super(LM, cls, cls.caption);

        object.groupTo.setViewTypeList();

//        LM.addObjectActions(this, object);

        setNFEditType(PropertyEditType.READONLY);

        if (!cls.dialogReadOnly) {
            FormSessionScope scope = FormSessionScope.NEWSESSION;
            addPropertyDraw(LM.getAddFormAction(this, object, null), scope, SetFact.EMPTYORDER());
            addPropertyDraw(LM.getEditFormAction(object, null), scope, SetFact.singletonOrder(object));
            addPropertyDraw(LM.getDeleteAction(object), scope, SetFact.singletonOrder(object));
        }

        if (hasConcreteStaticObjects(cls))
            addFixedOrder(addPropertyObject(LM.staticOrder, SetFact.singletonOrder(object)), false);

        finalizeInit();
    }

    private boolean hasConcreteStaticObjects(CustomClass cls) {
        boolean hasConcreteStaticObjects = cls.hasConcreteStaticObjects();
        if (!hasConcreteStaticObjects && cls.hasChildren())
            for (CustomClass customClass : cls.getChildrenIt()) {
                if (customClass.hasConcreteStaticObjects())
                    return true;
            }

        return hasConcreteStaticObjects;
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LAP<P, ?> property, FormSessionScope scope, ImOrderSet<ObjectEntity> objects) {
        PropertyDrawEntity propertyDraw = addPropertyDraw(property, ComplexLocation.LAST(), objects);
        propertyDraw.defaultChangeEventScope = scope;
        return propertyDraw;
    }
}
