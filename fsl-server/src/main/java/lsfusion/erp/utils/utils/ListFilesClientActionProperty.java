package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListFilesClientActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;

    public ListFilesClientActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String path = (String) context.getDataKeyValue(pathInterface).object;

        try {
            if (path != null) {
                Pattern p = Pattern.compile("(file|ftp):(?:\\/\\/)?(.*)");
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    String type = m.group(1).toLowerCase();
                    String url = m.group(2);

                    if(type.equals("file")) {
                        Map<String, Boolean> filesList = (Map<String, Boolean>) context.requestUserInteraction(new ListFilesClientAction(url));
                        if (filesList != null) {

                            context.getSession().dropChanges((DataProperty) findProperty("fileName[INTEGER]").property);
                            context.getSession().dropChanges((DataProperty) findProperty("fileIsDirectory[INTEGER]").property);

                            Integer i = 0;
                            for (Map.Entry<String, Boolean> file : filesList.entrySet()) {
                                findProperty("fileName[INTEGER]").change(file.getKey(), context, new DataObject(i));
                                findProperty("fileIsDirectory[INTEGER]").change(file.getValue(), context, new DataObject(i));
                                i++;
                            }
                        } else {
                            throw new RuntimeException("ListFiles Error. File not found: " + path);
                        }
                    } else throw new RuntimeException("ListFiles Error. Incorrect path: " + path);
                } else {
                    throw new RuntimeException("ListFiles Error. Incorrect path: " + path);
                }
            } else {
                throw new RuntimeException("ListFiles Error. Path not specified.");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }
}