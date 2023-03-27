package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
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

                writeProperty(context, findProperty("fileName[INTEGER]"), (String[]) filesList.get(0), StringClass.text);
                writeProperty(context, findProperty("fileIsDirectory[INTEGER]"), (Boolean[]) filesList.get(1), LogicalClass.instance);
                writeProperty(context, findProperty("fileModifiedDateTime[INTEGER]"), (LocalDateTime[]) filesList.get(2), DateTimeClass.instance);
                writeProperty(context, findProperty("fileSize[INTEGER]"), (Long[]) filesList.get(3), LongClass.instance);

            } else {
                throw new RuntimeException("ListFiles Error. Path not specified.");
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    public static <P extends PropertyInterface> void writeProperty(ExecutionContext context, LP<P> property, Object[] values, ConcreteClass valueClass) throws SQLException, SQLHandledException {
        property.change(context, MapFact.toIndexedMap(values), IntegerClass.instance, valueClass);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}