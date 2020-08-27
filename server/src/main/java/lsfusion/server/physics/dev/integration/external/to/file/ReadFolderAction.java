package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.stream.Collectors;

public class ReadFolderAction extends InternalAction {
    private final ClassPropertyInterface resourcePathInterface;

    public ReadFolderAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        resourcePathInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String resourcePath = (String) context.getKeyValue(resourcePathInterface).getValue();

        File[] listOfFiles = new File(resourcePath).listFiles();
        if (listOfFiles != null){
            JSONObject jsonObject = new JSONObject();
            try {
                for (File listOfFile : listOfFiles) {
                    jsonObject.put(listOfFile.getName(), Files.lines(Paths.get(listOfFile.getAbsolutePath())).collect(Collectors.joining(System.lineSeparator())));
                }
                findProperty("jsFiles[]").change(jsonObject.toString(), context);
            } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
