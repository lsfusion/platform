package lsfusion.server.logics.classes.utils.excel;

import com.google.common.base.Throwables;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.StaticFormatFileClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class ProtectExcelActionProperty extends ScriptingAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface passwordInterface;

    public ProtectExcelActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
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
            RawFileData file = (RawFileData) fileObject.object;
            String extension = ((StaticFormatFileClass)fileObject.objectClass.getType()).getOpenExtension(file);
            RawFileData protectedFile = null;
            switch (extension) {
                case "xls": {
                    HSSFWorkbook wb = new HSSFWorkbook(file.getInputStream());
                    for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext(); ) {
                        Sheet sheet = it.next();
                        if (password != null) {
                            sheet.protectSheet(password);
                        }
                    }

                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        wb.write(bos);
                        protectedFile = new RawFileData(bos);
                    }
                    break;
                }
                case "xlsx": {
                    XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream());
                    for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext(); ) {
                        Sheet sheet = it.next();
                        if (password != null) {
                            sheet.protectSheet(password);
                            //allow resize images
                            CTSheetProtection protection = ReflectionUtils.invokePrivateMethod(sheet.getClass(), sheet, "safeGetProtectionField", new Class<?>[0]);
                            if(protection != null) {
                                protection.setObjects(false);
                            }
                        }
                    }
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        wb.write(bos);
                        protectedFile = new RawFileData(bos);
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