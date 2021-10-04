package lsfusion.server.logics.classes.data.utils.pdf;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class PagesCountPdfAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;

    public PagesCountPdfAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue fileObject = context.getDataKeyValue(fileInterface);
        try (PDDocument document = PDDocument.load(((RawFileData) fileObject.getValue()).getBytes())) {
            findProperty("pagesCountPdf[]").change(document.getNumberOfPages(), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}