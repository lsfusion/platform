package lsfusion.server.logics.action;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.classes.data.file.TableClass;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;

public abstract class BaseAction<P extends PropertyInterface> extends Action<P> {

    protected BaseAction(LocalizedString caption, ImOrderSet<P> interfaces) {
        super(caption, interfaces);
    }

    protected static void writeResult(LP<?> exportFile, RawFileData singleFile, String extension, ExecutionContext<?> context, String charset) throws SQLException, SQLHandledException {
        writeResult(exportFile, new NamedFileData(new FileData(singleFile, extension)), context, charset);
    }
    protected static void writeResult(LP<?> exportFile, NamedFileData singleFile, ExecutionContext<?> context, String charset) throws SQLException, SQLHandledException {
        exportFile.change(exportFile.property.getType().parseFile(singleFile, charset), context);
    }

    protected static RawFileData readRawFile(ObjectValue value, Type valueType, String charset) {
        NamedFileData fileData = readFile(value, valueType, charset);
        return fileData != null ? fileData.getRawFile() : null;
    }

    protected static JDBCTable readTableFile(DataObject value, Type valueType) throws IOException {
        NamedFileData file = readFile(value, valueType, null);
        if(file.getExtension().equals(TableClass.extension))
            return JDBCTable.deserializeJDBC(file.getRawFile());
        return null;
    }

    protected static NamedFileData readFile(ObjectValue value, Type valueType, String charset) {
        if(value instanceof DataObject) {
            return readFile((DataObject) value, valueType, charset);
        }
        return null;
    }

    protected static NamedFileData readFile(DataObject value, Type valueType, String charset) {
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
