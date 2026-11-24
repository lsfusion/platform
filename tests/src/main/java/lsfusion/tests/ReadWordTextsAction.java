package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;

// what a .docx shows: the text of each cell of its first table into wtCell, by the cell's row and column, the text of each
// paragraph of its body into wtParagraph, by the paragraph's number - all from 0 -, and into wtList the numbering id of
// a paragraph that has one, which is what makes it an item of that list
public class ReadWordTextsAction extends InternalAction {

    public ReadWordTextsAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try (XWPFDocument document = new XWPFDocument(((RawFileData) getParam(0, context)).getInputStream())) {
            LP<?> cellText = findProperty("wtCell[INTEGER,INTEGER]");
            List<XWPFTableRow> rows = document.getTables().get(0).getRows();
            for (int row = 0; row < rows.size(); row++) {
                List<XWPFTableCell> cells = rows.get(row).getTableCells();
                for (int column = 0; column < cells.size(); column++)
                    cellText.change(cells.get(column).getText(), context, new DataObject(row), new DataObject(column));
            }

            LP<?> paragraphText = findProperty("wtParagraph[INTEGER]");
            LP<?> paragraphList = findProperty("wtList[INTEGER]");
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (int number = 0; number < paragraphs.size(); number++) {
                paragraphText.change(paragraphs.get(number).getText(), context, new DataObject(number));
                BigInteger list = paragraphs.get(number).getNumID();
                paragraphList.change(list != null ? list.intValue() : null, context, new DataObject(number));
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
