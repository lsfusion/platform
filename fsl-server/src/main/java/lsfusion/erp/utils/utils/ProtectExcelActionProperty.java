package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class ProtectExcelActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface passwordInterface;

    public ProtectExcelActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        passwordInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject fileObject = context.getDataKeyValue(fileInterface);
        String password = (String) context.getDataKeyValue(passwordInterface).object;

        try {
            byte[] file = (byte[]) fileObject.object;
            String extension = ((StaticFormatFileClass)fileObject.objectClass.getType()).getOpenExtension(file);
            byte[] protectedFile = null;
            switch (extension) {
                case "xls": {
                    HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(file));
                    for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext(); ) {
                        Sheet sheet = it.next();
                        if (password != null) {
                            sheet.protectSheet(password);
                        }
                    }

                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        wb.write(bos);
                        protectedFile = bos.toByteArray();
                    }
                    break;
                }
                case "xlsx": {
                    XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file));
                    for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext(); ) {
                        Sheet sheet = it.next();
                        if (password != null) {
                            sheet.protectSheet(password);
                        }
                    }
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        wb.write(bos);
                        protectedFile = bos.toByteArray();
                    }
                    break;
                }
            }

            if (protectedFile != null) {
                findProperty("protectedExcel[]").change(protectedFile, context);
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}