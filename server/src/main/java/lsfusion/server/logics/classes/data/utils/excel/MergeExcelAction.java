package lsfusion.server.logics.classes.data.utils.excel;

import com.google.common.base.Throwables;
import lsfusion.base.file.CopyExcelUtil;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class MergeExcelAction extends InternalAction {
    private final ClassPropertyInterface destinationInterface;
    private final ClassPropertyInterface sourceInterface;

    public MergeExcelAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        destinationInterface = i.next();
        sourceInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject destinationObject = context.getDataKeyValue(destinationInterface);
        DataObject sourceObject = context.getDataKeyValue(sourceInterface);
        try {

            RawFileData destinationFile = (RawFileData) destinationObject.object;
            RawFileData sourceFile = (RawFileData) sourceObject.object;

            String sourceExtension = ((StaticFormatFileClass)sourceObject.objectClass.getType()).getOpenExtension(sourceFile);
            String destinationExtension = ((StaticFormatFileClass)destinationObject.objectClass.getType()).getOpenExtension(destinationFile);
            if(sourceExtension.equals(destinationExtension)) {
                byte[] fileBytes = null;

                switch (sourceExtension) {
                    case "xls":
                    try (HSSFWorkbook destinationWorkBook = new HSSFWorkbook(destinationFile.getInputStream());
                         HSSFWorkbook sourceWorkBook = new HSSFWorkbook(sourceFile.getInputStream());
                         ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        CopyExcelUtil.copyHSSFSheets(sourceWorkBook, destinationWorkBook);
                        destinationWorkBook.write(os);
                        fileBytes = os.toByteArray();
                    }
                    break;
                    case "xlsx":
                    try (XSSFWorkbook destinationWorkBook = new XSSFWorkbook(destinationFile.getInputStream());
                         XSSFWorkbook sourceWorkBook = new XSSFWorkbook(sourceFile.getInputStream());
                         ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        CopyExcelUtil.copyXSSFSheets(sourceWorkBook, destinationWorkBook);
                        destinationWorkBook.write(os);
                        fileBytes = os.toByteArray();
                    }
                    break;
                }

                if(fileBytes != null) {
                    findProperty("mergedExcel[]").change(new RawFileData(fileBytes), context);
                }

            } else {
                throw new RuntimeException(String.format("Unable to merge %s and %s", destinationExtension, sourceExtension));
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}