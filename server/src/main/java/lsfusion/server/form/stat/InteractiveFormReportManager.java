package lsfusion.server.form.stat;

import jasperapi.FormPrintType;
import lsfusion.interop.form.user.FormUserPreferences;
import jasperapi.ReportGenerationData;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;

import java.sql.SQLException;

public class InteractiveFormReportManager extends FormReportManager {
    
    public InteractiveFormReportManager(FormInstance form) {
        this(form, null, null);
    }
    public InteractiveFormReportManager(FormInstance form, Integer groupId, FormUserPreferences preferences) {
        super(new InteractiveFormReportInterface(form, groupId, preferences));
    }

    // backward compatibility
    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, FormUserPreferences preferences) throws SQLException, SQLHandledException {
        return new InteractiveFormReportManager(((InteractiveFormReportInterface)reportInterface).getForm(), groupId, preferences).getReportData(toExcel ? FormPrintType.XLS : FormPrintType.PRINT);
    }

}
