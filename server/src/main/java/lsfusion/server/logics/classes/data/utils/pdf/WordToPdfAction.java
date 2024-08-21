package lsfusion.server.logics.classes.data.utils.pdf;

import com.google.common.base.Throwables;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.*;
import java.sql.SQLException;
import java.util.Iterator;

public class WordToPdfAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;

    public WordToPdfAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        InputStream docxInputStream = new ByteArrayInputStream(((RawFileData) context.getDataKeyValue(fileInterface).getValue()).getBytes());
        try (XWPFDocument document = new XWPFDocument(docxInputStream);
             ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

            PdfOptions options = PdfOptions.create();
            PdfConverter.getInstance().convert(document, pdfOutputStream, options);

            findProperty("exportPdfFile[]").change(new RawFileData(pdfOutputStream), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
