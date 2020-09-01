package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.ResourceUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ReadResourcesAction extends InternalAction {
    private final ClassPropertyInterface resourcePathInterface;

    public ReadResourcesAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        resourcePathInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String resourcePath = (String) context.getKeyValue(resourcePathInterface).getValue();
        Set<File> files = new HashSet<>();
        Pattern pattern;
        if (resourcePath.startsWith("/")) {
            //absolute path(find in resources folders)
            pattern = Pattern.compile(resourcePath.endsWith("/") ? resourcePath + ".*" : resourcePath + "/.*");
        } else {
            //relative path(find by regexp)
            pattern = Pattern.compile(resourcePath);
        }
        List<String> allResources = ResourceUtils.getResources(pattern);
        allResources
                .stream()
                .filter(r -> ResourceUtils.getResource(r) != null)
                .forEach(r -> files.add(new File(ResourceUtils.getResource(r).getPath())));

        files.forEach(f -> {
            try {
                RawFileData fileData = new RawFileData(f);
                findProperty("resourceFiles[STRING]").change(fileData, context, new DataObject(f.getParentFile().getName() + "/" + f.getName(), StringClass.text));
            } catch (ScriptingErrorLog.SemanticErrorException | IOException | SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        });
    }
}
