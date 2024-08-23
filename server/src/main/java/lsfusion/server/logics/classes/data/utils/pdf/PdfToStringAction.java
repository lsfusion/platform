package lsfusion.server.logics.classes.data.utils.pdf;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class PdfToStringAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface sortByPositionInterface;

    public PdfToStringAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        sortByPositionInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try (PDDocument document = PDDocument.load(((RawFileData) context.getDataKeyValue(fileInterface).getValue()).getBytes())) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            if (context.getKeyValue(sortByPositionInterface).getValue() != null)
                pdfTextStripper.setSortByPosition(true);

            String text = pdfTextStripper.getText(document);
            findProperty("resultString[]").change(text, context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
