package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class BaseClassFormEntity extends AutoFinalFormEntity {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule LM, CustomClass cls, LocalizedString caption) {
        super(caption, LM);

        object = addSingleGroupObject(cls);

        ImList<ActionOrPropertyClassImplement> idProps = getActionOrProperties(LM.getIdGroup(), cls);
        if(idProps.isEmpty()) {
            // we need at least one prop (otherwise there will be no grid in dialog)
            PropertyDrawEntity objectValue = addValuePropertyDraw(LM, object);
            objectValue.setEditType(PropertyEditType.READONLY);
        }

        addPropertyDraw(object, LM.getBaseGroup());
    }

}
