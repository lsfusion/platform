package roman.actions;

import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class FiscalDatecsUpdateDataActionProperty extends ScriptingActionProperty {

    public FiscalDatecsUpdateDataActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        DataSession session = context.getSession();

        try {
            Integer comPort = (Integer) LM.findLCPByCompoundName("comPortCurrentCashRegister").read(session);
            Integer baudRate = (Integer) LM.findLCPByCompoundName("baudRateCurrentCashRegister").read(session);


            KeyExpr customUserExpr = new KeyExpr("customUser");
            KeyExpr groupCashRegisterExpr = new KeyExpr("groupCashRegister");
            Map<Object, KeyExpr> operatorKeys = new HashMap<Object, KeyExpr>();
            operatorKeys.put("customUser", customUserExpr);
            operatorKeys.put("groupCashRegister", groupCashRegisterExpr);

            Query<Object, Object> operatorQuery = new Query<Object, Object>(operatorKeys);
            operatorQuery.properties.put("operatorNumberGroupCashRegisterCustomUser", getLCP("operatorNumberGroupCashRegisterCustomUser").getExpr(context.getModifier(), groupCashRegisterExpr, customUserExpr));
            operatorQuery.properties.put("userFirstName", getLCP("userFirstName").getExpr(context.getModifier(), customUserExpr));
            operatorQuery.properties.put("userLastName", getLCP("userLastName").getExpr(context.getModifier(), customUserExpr));

            operatorQuery.and(getLCP("operatorNumberGroupCashRegisterCustomUser").getExpr(context.getModifier(), operatorQuery.mapKeys.get("groupCashRegister"), operatorQuery.mapKeys.get("customUser")).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> operatorResult = operatorQuery.execute(session.sql);
            List<UpdateDataOperator> operatorList = new ArrayList<UpdateDataOperator>();
            for (Map<Object, Object> operatorValues : operatorResult.values()) {
                Integer number = (Integer) operatorValues.get("operatorNumberGroupCashRegisterCustomUser");
                String userFirstName = (String) operatorValues.get("userFirstName");
                String userLastName = (String) operatorValues.get("userLastName");
                if (number != null)
                    operatorList.add(new UpdateDataOperator(number, userFirstName.trim() + " " + userLastName.trim()));
            }
            
            List<UpdateDataTaxRate> taxRateList = new ArrayList<UpdateDataTaxRate>();
            ObjectValue countryObject = LM.findLCPByCompoundName("countryCurrentCashRegister").readClasses(session);
            DataObject taxVATObject = ((StaticCustomClass) LM.findClassByCompoundName("tax")).getDataObject("taxVAT");
            KeyExpr rangeExpr = new KeyExpr("range");
            KeyExpr taxExpr = new KeyExpr("tax");
            Map<Object, KeyExpr> rangeKeys = new HashMap<Object, KeyExpr>();
            rangeKeys.put("range", rangeExpr);
            rangeKeys.put("tax", taxExpr);

            Query<Object, Object> rangeQuery = new Query<Object, Object>(rangeKeys);
            rangeQuery.properties.put("numberRange", getLCP("numberRange").getExpr(context.getModifier(), rangeExpr));
            rangeQuery.properties.put("valueCurrentRateRange", getLCP("valueCurrentRateRange").getExpr(context.getModifier(), rangeExpr));
            rangeQuery.properties.put("countryRange", getLCP("countryRange").getExpr(context.getModifier(), rangeExpr));

            rangeQuery.and(getLCP("countryRange").getExpr(context.getModifier(), rangeQuery.mapKeys.get("range")).compare(countryObject.getExpr(), Compare.EQUALS));
            rangeQuery.and(getLCP("taxRange").getExpr(context.getModifier(), rangeQuery.mapKeys.get("tax")).compare(taxVATObject.getExpr(), Compare.EQUALS));
            rangeQuery.and(getLCP("numberRange").getExpr(context.getModifier(), rangeQuery.mapKeys.get("range")).getWhere());


            OrderedMap<Map<Object, Object>, Map<Object, Object>> rangeResult = rangeQuery.execute(session.sql);
            for (Map<Object, Object> rangeValues : rangeResult.values()) {
                Integer number = (Integer) rangeValues.get("numberRange");
                Double value = (Double) rangeValues.get("valueCurrentRateRange");
                if (number != null)
                    taxRateList.add(new UpdateDataTaxRate(number, value));
            }
            if (context.checkApply(LM.getBL()))
                if(context.requestUserInteraction(new FiscalDatecsUpdateDataClientAction(comPort, baudRate, new UpdateDataInstance(operatorList, taxRateList)))==null)
                    context.apply(LM.getBL());

        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
