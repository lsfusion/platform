package lsfusion.server.remote;

import lsfusion.base.Result;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.*;
import lsfusion.server.form.stat.FormReportManager;
import lsfusion.server.form.stat.InteractiveFormReportInterface;
import lsfusion.server.form.stat.StaticDataGenerator;

import java.sql.SQLException;

public class InteractiveFormReportManager extends FormReportManager {
    
    public InteractiveFormReportManager(FormInstance<?> form) {
        this(form, null, null);
    }
    public InteractiveFormReportManager(FormInstance<?> form, Integer groupId, FormUserPreferences preferences) {
        super(new InteractiveFormReportInterface(form, groupId, preferences));
    }

    // backward compatibility
    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, FormUserPreferences preferences) throws SQLException, SQLHandledException {
        return new InteractiveFormReportManager(((InteractiveFormReportInterface)reportInterface).getForm(), groupId, preferences).getReportData(toExcel ? FormPrintType.XLS : FormPrintType.PRINT);
    }

}
