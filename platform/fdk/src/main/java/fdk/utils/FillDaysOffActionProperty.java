package fdk.utils;

import com.google.common.base.Throwables;
import platform.server.classes.DateClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

public class FillDaysOffActionProperty extends ScriptingActionProperty {

    private final ClassPropertyInterface countryInterface;

    public FillDaysOffActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LM.getClassByName("country"));

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        countryInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = context.createSession();

            DataObject countryObject = context.getKeyValue(countryInterface);
            generateDates(context, countryObject);

            session.close();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void generateDates(ExecutionContext<ClassPropertyInterface> context, DataObject countryObject) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = context.createSession();
        Calendar current = Calendar.getInstance();
        int currentYear = current.get(Calendar.YEAR);
        //если проставлен выходной 1 января через 2 года, пропускаем генерацию
        //DataObject countryObject = new DataObject(countryId, (ConcreteClass) BL.getModule("Country").getClassByName("country"));
        //if (BL.getModule("Country").getLCPByName("isDayOffCountryDate").read(session, countryObject, new DataObject(new java.sql.Date(new GregorianCalendar(currentYear + 2, 0, 1).getTimeInMillis()), DateClass.instance)) != null) {
        //    return;
        //}

        long wholeYearMillisecs = new GregorianCalendar(currentYear + 3, 0, 1).getTimeInMillis() - new GregorianCalendar(currentYear, 0, 1).getTimeInMillis()/*current.getTimeInMillis()*/;
        long wholeYearDays = wholeYearMillisecs / 1000 / 60 / 60 / 24;
        Calendar cal = new GregorianCalendar(currentYear, 0, 1);
        for (int i = 0; i < wholeYearDays; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if (day == 1 || day == 7) {
                addDayOff(context, session, countryObject, cal.getTimeInMillis());
            }
        }

        for (int i = 0; i < 3; i++) {
            Calendar calendar = new GregorianCalendar(currentYear + i, 0, 1);
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            if (day != 1 && day != 7)
                addDayOff(context, session, countryObject, calendar.getTimeInMillis());
        }

        session.apply(context.getBL());
        session.close();
    }

    private void addDayOff(ExecutionContext<ClassPropertyInterface> context, DataSession session, DataObject countryObject, long timeInMillis) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        context.getBL().getModule("Country").getLCPByName("isDayOffCountryDate").change(true, session, countryObject, new DataObject(new java.sql.Date(timeInMillis), DateClass.instance));
    }
}