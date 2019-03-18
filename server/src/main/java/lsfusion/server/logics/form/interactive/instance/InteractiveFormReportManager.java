package lsfusion.server.logics.form.interactive.instance;

import lsfusion.interop.form.stat.report.FormPrintType;
import lsfusion.interop.form.stat.report.ReportGenerationData;
import lsfusion.interop.form.user.FormUserPreferences;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.form.stat.print.FormReportManager;
import lsfusion.server.logics.form.stat.print.InteractiveFormReportInterface;

import java.sql.SQLException;

public class InteractiveFormReportManager extends FormReportManager {
    
    public InteractiveFormReportManager(FormInstance form) {
        this(form, null, null);
    }
    public InteractiveFormReportManager(FormInstance form, Integer groupId, FormUserPreferences preferences) {
        super(new InteractiveFormReportInterface(form, groupId, preferences));
    }

    // backward compatibility
    @Deprecated
    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, FormUserPreferences preferences) throws SQLException, SQLHandledException {
        return new InteractiveFormReportManager(((InteractiveFormReportInterface)reportInterface).getForm(), groupId, preferences).getReportData(toExcel ? FormPrintType.XLS : FormPrintType.PRINT);
    }

}
