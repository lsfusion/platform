package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.client.ListFilesClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

public class ListFilesAction extends InternalAction {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface recursiveInterface;
    private final ClassPropertyInterface isClientInterface;

    public ListFilesAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        pathInterface = i.next();
        recursiveInterface = i.next();
        isClientInterface = i.next();
    }

    public static <P extends PropertyInterface> void writeProperty(DataSession session, LP<P> property, LocalDateTime[] values) throws SQLException, SQLHandledException {
        writeProperty(session, property, values, DateTimeClass.instance);
    }

    public static <P extends PropertyInterface> void writeProperty(DataSession session, LP<P> property, String[] values) throws SQLException, SQLHandledException {
        writeProperty(session, property, values, StringClass.text);
    }

    public static <P extends PropertyInterface> void writeProperty(DataSession session, LP<P> property, Boolean[] values) throws SQLException, SQLHandledException {
        writeProperty(session, property, values, LogicalClass.instance);
    }

    public static <P extends PropertyInterface> void writeProperty(DataSession session, LP<P> property, Long[] values) throws SQLException, SQLHandledException {
        writeProperty(session, property, values, LongClass.instance);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        String sourcePath = (String) context.getKeyValue(pathInterface).getValue();
        boolean recursive = context.getKeyValue(recursiveInterface).getValue() != null;
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;

        try {
            if (sourcePath != null) {

                List<Object> filesList;
                if (isClient) {
                    Object result = context.requestUserInteraction(new ListFilesClientAction(sourcePath, recursive));
                    if (result instanceof String) {
                        throw new RuntimeException((String) result);
                    }else {
                        filesList = (List<Object>) result;
                    }
                } else {
                    filesList = FileUtils.listFiles(sourcePath, recursive);
                }

                context.getSession().dropChanges((DataProperty) findProperty("fileName[INTEGER]").property);
                context.getSession().dropChanges((DataProperty) findProperty("fileIsDirectory[INTEGER]").property);
                context.getSession().dropChanges((DataProperty) findProperty("fileModifiedDateTime[INTEGER]").property);
                context.getSession().dropChanges((DataProperty) findProperty("fileSize[INTEGER]").property);

                writeProperty(context.getSession(), findProperty("fileName[INTEGER]"), (String[]) filesList.get(0));
                writeProperty(context.getSession(), findProperty("fileIsDirectory[INTEGER]"), (Boolean[]) filesList.get(1));
                writeProperty(context.getSession(), findProperty("fileModifiedDateTime[INTEGER]"), (LocalDateTime[]) filesList.get(2));
                writeProperty(context.getSession(), findProperty("fileSize[INTEGER]"), (Long[]) filesList.get(3));

            } else {
                throw new RuntimeException("ListFiles Error. Path not specified.");
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    public static <P extends PropertyInterface> void writeProperty(DataSession session, LP<P> property, Object[] values, ConcreteClass objectClass) throws SQLException, SQLHandledException {
        Property<P> prop = property.property;
        P name = property.listInterfaces.get(0);

        SingleKeyPropertyUsage table = new SingleKeyPropertyUsage("listFilesAction", prop.interfaceTypeGetter.getType(name), prop.getType());

        MExclMap<DataObject, ObjectValue> mRows = MapFact.mExclMap();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            mRows.exclAdd(new DataObject(i), value != null ? new DataObject(values[i], objectClass) : NullValue.instance);
        }
        try {
            table.writeRows(session.sql, session.getOwner(), mRows.immutable());
            session.change(prop, SingleKeyPropertyUsage.getChange(table, name));
        } finally {
            table.drop(session.sql, session.getOwner());
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}