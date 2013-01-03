package platform.fdk.actions.fiscaldatecs;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.interop.action.MessageClientAction;
import platform.server.classes.StaticCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
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
            ImRevMap<Object, KeyExpr> operatorKeys = MapFact.toRevMap((Object)"customUser", customUserExpr, "groupCashRegister", groupCashRegisterExpr);

            QueryBuilder<Object, Object> operatorQuery = new QueryBuilder<Object, Object>(operatorKeys);
            operatorQuery.addProperty("operatorNumberGroupCashRegisterCustomUser", getLCP("operatorNumberGroupCashRegisterCustomUser").getExpr(context.getModifier(), groupCashRegisterExpr, customUserExpr));
            operatorQuery.addProperty("userFirstName", getLCP("userFirstName").getExpr(context.getModifier(), customUserExpr));
            operatorQuery.addProperty("userLastName", getLCP("userLastName").getExpr(context.getModifier(), customUserExpr));

            operatorQuery.and(getLCP("operatorNumberGroupCashRegisterCustomUser").getExpr(context.getModifier(), operatorQuery.getMapExprs().get("groupCashRegister"), operatorQuery.getMapExprs().get("customUser")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> operatorResult = operatorQuery.execute(session.sql);
            List<UpdateDataOperator> operatorList = new ArrayList<UpdateDataOperator>();
            for (ImMap<Object, Object> operatorValues : operatorResult.valueIt()) {
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
            ImRevMap<Object, KeyExpr> rangeKeys = MapFact.toRevMap((Object) "range", rangeExpr, "tax", taxExpr);

            QueryBuilder<Object, Object> rangeQuery = new QueryBuilder<Object, Object>(rangeKeys);
            rangeQuery.addProperty("numberRange", getLCP("numberRange").getExpr(context.getModifier(), rangeExpr));
            rangeQuery.addProperty("valueCurrentRateRange", getLCP("valueCurrentRateRange").getExpr(context.getModifier(), rangeExpr));
            rangeQuery.addProperty("countryRange", getLCP("countryRange").getExpr(context.getModifier(), rangeExpr));

            rangeQuery.and(getLCP("countryRange").getExpr(context.getModifier(), rangeQuery.getMapExprs().get("range")).compare(countryObject.getExpr(), Compare.EQUALS));
            rangeQuery.and(getLCP("taxRange").getExpr(context.getModifier(), rangeQuery.getMapExprs().get("tax")).compare(taxVATObject.getExpr(), Compare.EQUALS));
            rangeQuery.and(getLCP("numberRange").getExpr(context.getModifier(), rangeQuery.getMapExprs().get("range")).getWhere());


            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> rangeResult = rangeQuery.execute(session.sql);
            for (ImMap<Object, Object> rangeValues : rangeResult.valueIt()) {
                Integer number = (Integer) rangeValues.get("numberRange");
                Double value = (Double) rangeValues.get("valueCurrentRateRange");
                if (number != null)
                    taxRateList.add(new UpdateDataTaxRate(number, value));
            }
            if (context.checkApply(LM.getBL())) {
                String result = (String) context.requestUserInteraction(new FiscalDatecsUpdateDataClientAction(baudRate, comPort, new UpdateDataInstance(operatorList, taxRateList)));
                if (result == null)
                    context.apply(LM.getBL());
                else
                    context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
