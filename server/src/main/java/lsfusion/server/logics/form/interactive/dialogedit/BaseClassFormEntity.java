package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.AutoFormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class BaseClassFormEntity extends AutoFormEntity {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule LM, CustomClass cls, LocalizedString caption) {
        super(caption, LM.getVersion());
        
        Version version = LM.getVersion();

        object = addSingleGroupObject(cls, version);

        ImList<ActionOrPropertyClassImplement> idProps = LM.getRecognizeGroup().getActionOrProperties(cls, version);
        if(idProps.isEmpty()) {
            // we need at least one prop (otherwise there will be no grid in dialog)
            PropertyDrawEntity objectValue = addValuePropertyDraw(LM, object, version);
            objectValue.setEditType(PropertyEditType.READONLY);
        }

        addPropertyDraw(object, version, LM.getBaseGroup());
    }

}
