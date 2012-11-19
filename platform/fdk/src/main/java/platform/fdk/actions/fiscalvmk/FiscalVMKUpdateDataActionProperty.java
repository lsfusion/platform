package platform.fdk.actions.fiscalvmk;

import platform.base.OrderedMap;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FiscalVMKUpdateDataActionProperty extends ScriptingActionProperty {

    public FiscalVMKUpdateDataActionProperty(ScriptingLogicsModule LM) {
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


            if (context.checkApply(LM.getBL())) {
                String result = (String) context.requestUserInteraction(new FiscalVMKUpdateDataClientAction(baudRate, comPort));
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
