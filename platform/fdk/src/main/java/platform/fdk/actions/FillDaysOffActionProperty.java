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
    BusinessLogics BL;
    public FillDaysOffActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
        BL = LM.getBL();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = createSession();

            LCP<PropertyInterface> generateOrDefaultCountry = LM.addSUProp(Union.OVERRIDE, BL.getModule("Country").getLCPByName("generateDatesCountry"), LM.addJProp(LM.baseLM.equals2, BL.getModule("Country").getLCPByName("defaultCountry"), 1));

            OrderedMap<PropertyInterface, KeyExpr> keys = generateOrDefaultCountry.getMapKeys();

            Query<PropertyInterface, Object> query = new Query<PropertyInterface, Object>(keys);
            query.properties.put("id", generateOrDefaultCountry.property.getExpr(keys));

            query.and(generateOrDefaultCountry.property.getExpr(keys).getWhere());

            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> result = query.execute(session.sql);
            for (Map<PropertyInterface, Object> key : result.keyList()) {
                Integer id = (Integer) BaseUtils.singleValue(key);
                generateDates(id);
            }
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

    private void generateDates(int countryId) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();
        Calendar current = Calendar.getInstance();
        int currentYear = current.get(Calendar.YEAR);
        //если проставлен выходной 1 января через 2 года, пропускаем генерацию
        DataObject countryObject = new DataObject(countryId, (ConcreteClass) BL.getModule("Country").getClassByName("country"));
        if (BL.getModule("Country").getLCPByName("isDayOffCountryDate").read(session, countryObject, new DataObject(new java.sql.Date(new GregorianCalendar(currentYear + 2, 0, 1).getTimeInMillis()), DateClass.instance)) != null) {
            return;
        }

        long wholeYearMillisecs = new GregorianCalendar(currentYear + 3, 0, 1).getTimeInMillis() - current.getTimeInMillis();
        long wholeYearDays = wholeYearMillisecs / 1000 / 60 / 60 / 24;
        Calendar cal = new GregorianCalendar(currentYear, 0, 1);
        for (int i = 0; i < wholeYearDays; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if (day == 1 || day == 7) {
                addDayOff(session, countryId, cal.getTimeInMillis());
            }
        }

        for (int i = 0; i < 3; i++) {
            Calendar calendar = new GregorianCalendar(currentYear + i, 0, 1);
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            if (day != 1 && day != 7)
                addDayOff(session, countryId, calendar.getTimeInMillis());
        }

        session.apply(BL);
        session.close();
    }

    private void addDayOff(DataSession session, int countryId, long timeInMillis) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataObject countryObject = new DataObject(countryId, (ConcreteClass) BL.getModule("Country").getClassByName("country"));
        BL.getModule("Country").getLCPByName("isDayOffCountryDate").change(true, session, countryObject, new DataObject(new java.sql.Date(timeInMillis), DateClass.instance));
    }
}