package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.server.physics.dev.integration.external.to.file.client.ListFilesClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

public class ListFilesActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface isClientInterface;

    public ListFilesActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        charsetInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String sourcePath = (String) context.getKeyValue(pathInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;

        try {
            if (sourcePath != null) {

                Map<String, Boolean> filesList;
                if (isClient) {
                    Object result = context.requestUserInteraction(new ListFilesClientAction(sourcePath, charset));
                    if (result instanceof String) {
                        throw new RuntimeException((String) result);
                    }else {
                        filesList = (Map<String, Boolean>) result;
                    }
                } else {
                    filesList = FileUtils.listFiles(sourcePath, charset);
                }

                context.getSession().dropChanges((DataProperty) findProperty("fileName[INTEGER]").property);
                context.getSession().dropChanges((DataProperty) findProperty("fileIsDirectory[INTEGER]").property);

                Integer i = 0;
                for (Map.Entry<String, Boolean> file : filesList.entrySet()) {
                    findProperty("fileName[INTEGER]").change(file.getKey(), context, new DataObject(i));
                    findProperty("fileIsDirectory[INTEGER]").change(file.getValue(), context, new DataObject(i));
                    i++;
                }

            } else {
                throw new RuntimeException("ListFiles Error. Path not specified.");
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}