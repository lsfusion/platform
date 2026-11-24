package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;

// a .docx of a table with one row of two cells, the first of them holding the first parameter, and under it an item of
// the list 1 - a paragraph with that numbering id, which is what a template's list entry tells an item by - holding the
// second: the keys a template's table and list entries are found by. Into wtTemplate
public class MakeWordTemplateAction extends InternalAction {

    public MakeWordTemplateAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            document.createTable(1, 2).getRow(0).getCell(0).getParagraphs().get(0).createRun().setText((String) getParam(0, context));
            XWPFParagraph item = document.createParagraph();
            item.setNumID(BigInteger.ONE);
            item.createRun().setText((String) getParam(1, context));
            document.write(bytes);
            findProperty("wtTemplate[]").change(new RawFileData(bytes), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
