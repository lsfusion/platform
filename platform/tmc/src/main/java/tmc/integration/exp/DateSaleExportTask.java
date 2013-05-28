package tmc.integration.exp;

import platform.base.DateConverter;
import platform.interop.Compare;
import platform.server.classes.DateClass;
import platform.server.form.instance.CalcPropertyObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.filter.CompareFilterInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateSaleExportTask extends AbstractSaleExportTask {

    public DateSaleExportTask(ExecutionContext<ClassPropertyInterface> context, String path, Integer store) {
        super(context, path, store);
    }

    protected String getDbfName() {
        return "datadat.dbf";
    }

    private java.sql.Date exportDate;
    private Date getExportDate() throws ParseException {

        if (exportDate == null) {

            DateFormat formatter = new SimpleDateFormat("dd.MM.yy");
            exportDate = DateConverter.safeDateToSql(formatter.parse(new String(flagContent)));
        }

        return exportDate;
    }

    protected void setRemoteFormFilter(FormInstance formInstance) throws ParseException {

        PropertyDrawInstance<?> date = formInstance.getPropertyDraw(BL.VEDLM.date);
        date.toDraw.addTempFilter(new CompareFilterInstance((CalcPropertyObjectInstance) date.propertyObject, Compare.EQUALS, new DataObject(getExportDate(), DateClass.instance)));
    }

    protected void updateRemoteFormProperties(FormInstance formInstance) throws SQLException {
    }
}
