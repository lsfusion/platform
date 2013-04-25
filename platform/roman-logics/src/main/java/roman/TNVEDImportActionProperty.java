package roman;

import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomReadValueActionProperty;

import java.sql.SQLException;

public class TNVEDImportActionProperty extends CustomReadValueActionProperty {
    public static final int CLASSIFIER_IMPORT = 1;
    public static final int MIN_PRICES_IMPORT = 2;
    public static final int DUTIES_IMPORT = 3;

    private RomanLogicsModule LM;
    private String classifierType;
    private int importType;

    public TNVEDImportActionProperty(String sID, String caption, RomanLogicsModule LM, int importType, String classifierType) {
        super(sID, caption, new ValueClass[]{});
        this.LM = LM;
        this.classifierType = classifierType;
        this.importType = importType;
    }

    public TNVEDImportActionProperty(String sID, String caption, RomanLogicsModule LM, int importType) {
        this(sID, caption, LM, importType, "");
    }

    private DataClass getFileClass() {
        return CustomStaticFormatFileClass.get(false, false, "Файл базы данных \"DBF\"", "dbf");
    }

    protected DataClass getReadType() {
        return getFileClass();
    }

    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        try {
            ObjectValue userObjectValue = context.getSession().getObjectValue(getFileClass(), userValue);
            
            TNVEDImporter importer = null;
            switch (importType) {
                case CLASSIFIER_IMPORT:
                    importer = new TNVEDClassifierImporter(context.getFormInstance(), userObjectValue, LM, classifierType);
                    break;
                case MIN_PRICES_IMPORT:
                    importer = new TNVEDMinPricesImporter(context.getFormInstance(), userObjectValue, LM);
                    break;
                case DUTIES_IMPORT:
                    importer = new TNVEDDutiesImporter(context.getFormInstance(), userObjectValue, LM);
                    break;
            }
            importer.doImport();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
