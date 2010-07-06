package tmc.integration.exp;

import tmc.VEDBusinessLogics;
import platform.server.view.form.RemoteForm;
import platform.server.view.form.PropertyView;
import platform.server.view.form.filter.CompareFilter;
import platform.server.logics.DataObject;
import platform.server.classes.DateClass;
import platform.interop.Compare;
import platform.base.DateConverter;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.sql.SQLException;

public class DateSaleExportTask extends AbstractSaleExportTask {

    protected DateSaleExportTask(VEDBusinessLogics BL, String path) {
        super(BL, path);
    }

    protected String getDbfName() {
        return "datadat.dbf";
    }

    private Date exportDate;
    private Date getExportDate() throws ParseException {

        if (exportDate == null) {

            DateFormat formatter = new SimpleDateFormat("dd.MM.yy");
            exportDate = formatter.parse(new String(flagContent)); 
        }

        return exportDate;
    }

    protected void setRemoteFormFilter(RemoteForm remoteForm) throws ParseException {

        PropertyView<?> date = remoteForm.getPropertyView(BL.date);
        date.toDraw.addTempFilter(new CompareFilter(date.view, Compare.EQUALS, new DataObject(DateConverter.dateToSql(getExportDate()), DateClass.instance)));
    }

    protected void updateRemoteFormProperties(RemoteForm remoteForm) throws SQLException {
    }
}
