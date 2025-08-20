package lsfusion.server.logics.action;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public abstract class BaseAction<P extends PropertyInterface> extends Action<P> {

    protected BaseAction(LocalizedString caption, ImOrderSet<P> interfaces) {
        super(caption, interfaces);
    }

    protected static void writeResult(LP<?> exportFile, RawFileData singleFile, String extension, ExecutionContext<?> context, String charset) throws SQLException, SQLHandledException {
        exportFile.change(exportFile.property.getType().parseFile(singleFile, extension, charset), context);
    }

    protected static FileData readFile(ObjectValue value, Type valueType, String charset) {
        if(value instanceof DataObject) {
            return readFile((DataObject) value, valueType, charset);
        }
        return null;
    }

    protected static FileData readFile(DataObject value, Type valueType, String charset) {
        return getFileClass(value, valueType).formatFile(value.object, charset);
    }

    protected static Type getFileClass(DataObject value, Type valueType) {
        if(!(valueType instanceof StaticFormatFileClass))
            valueType = value.objectClass.getType();
        return valueType;
    }

    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.ANYEFFECT)
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }
}
