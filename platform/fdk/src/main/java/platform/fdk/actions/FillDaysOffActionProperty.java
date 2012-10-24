package platform.fdk.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.*;

public class FillDaysOffActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface countryInterface;
    private BusinessLogics BL;
    public FillDaysOffActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.getClassByName("country")});
        BL = LM.getBL();

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        countryInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = createSession();

            DataObject countryObject = context.getKeyValue(countryInterface);
            generateDates(countryObject);

            session.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void generateDates(DataObject countryObject) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();
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
                addDayOff(session, countryObject, cal.getTimeInMillis());
            }
        }

        for (int i = 0; i < 3; i++) {
            Calendar calendar = new GregorianCalendar(currentYear + i, 0, 1);
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            if (day != 1 && day != 7)
                addDayOff(session, countryObject, calendar.getTimeInMillis());
        }

        session.apply(BL);
        session.close();
    }

    private void addDayOff(DataSession session,DataObject countryObject, long timeInMillis) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        BL.getModule("Country").getLCPByName("isDayOffCountryDate").change(true, session, countryObject, new DataObject(new java.sql.Date(timeInMillis), DateClass.instance));
    }
}