package fdk.region.by.integration.excel;

import org.apache.commons.lang.time.DateUtils;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;

public class ImportExcelActionProperty extends ScriptingActionProperty {

    public ImportExcelActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    protected static String parseString(String value) throws ParseException {
        return value.isEmpty() ? null : value;
    }

    protected static Double parseDouble(String value) throws ParseException {
        return value.isEmpty() ? null : NumberFormat.getInstance().parse(value).doubleValue();
    }

    protected static BigDecimal parseBigDecimal(String value) throws ParseException {
        return value.isEmpty() ? null : BigDecimal.valueOf(NumberFormat.getInstance().parse(value).doubleValue());
    }

    protected static Date parseDate(String value) throws ParseException {
        return value.isEmpty() ? null : new Date(DateUtils.parseDate(value, new String[]{"dd.mm.yyyy"}).getTime());
    }

    protected static Boolean parseBoolean(String value) {
        return value.equals("1") ? true : null;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
    }
}