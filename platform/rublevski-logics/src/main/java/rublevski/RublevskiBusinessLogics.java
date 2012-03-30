package rublevski;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.Context;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import retail.api.remote.PriceTransaction;
import retail.api.remote.RetailRemoteInterface;
import retail.api.remote.ScalesInfo;

import javax.mail.Session;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.*;

/**
 * User: DAle
 * Date: 05.01.12
 * Time: 15:34
 */


public class RublevskiBusinessLogics extends BusinessLogics<RublevskiBusinessLogics> implements RetailRemoteInterface {
    ScriptingLogicsModule rublevskiLM;

    public RublevskiBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    public ScriptingLogicsModule getLM() {
        return rublevskiLM;
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        rublevskiLM = new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Rublevski.lsf"), LM, this);
        addLogicsModule(rublevskiLM);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }

    @Override
    public PriceTransaction readNextPriceTransaction(String equServerID) throws RemoteException {
        return new PriceTransaction();
    }

    //метод пока не работает
    @Override
    public List<ScalesInfo> readScalesInfo(String equServerID) throws RemoteException, SQLException {

        DataSession session = getBL().createSession();
        List<ScalesInfo> scalesInfoList = new ArrayList<ScalesInfo>();
        List<Object> groupMashineryMPTList = new ArrayList<Object>();

        LP isMachineryPriceTransaction = rublevskiLM.is(rublevskiLM.getClassByName("machineryPriceTransaction"));
        Map<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
        Query<Object, Object> query = new Query<Object, Object>(keys);
        query.properties.put("groupMachineryMPT", rublevskiLM.getLPByName("groupMachineryMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
        query.properties.put("processMPT", rublevskiLM.getLPByName("processMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
        query.and(isMachineryPriceTransaction.property.getExpr(keys).getWhere());
        //query.and(rublevskiLM.getLPByName("processMachineryPriceTransaction").property.getExpr(keys).getWhere());
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : result.entrySet()) {
            String groupMachineryMPT = entry.getValue().get("groupMachineryMPT").toString().trim();
            Boolean processMPT = entry.getValue().get("processMPT") != null;
            if(processMPT)
                groupMashineryMPTList.add(groupMachineryMPT);

            KeyExpr scalesExpr = new KeyExpr("scales");
            KeyExpr groupMachineryExpr = new KeyExpr("groupMachinery");
            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("scales", scalesExpr);
            newKeys.put("groupMachinery", groupMachineryExpr);
            Query<Object, Object> query2 = new Query<Object, Object>(newKeys);
            query2.properties.put("numberScales", rublevskiLM.getLPByName("numberScales").getExpr(scalesExpr));
            query2.properties.put("descriptionMachinery", rublevskiLM.getLPByName("descriptionMachinery").getExpr(scalesExpr));
            query2.properties.put("nameGroupMachineryScales", rublevskiLM.getLPByName("nameGroupMachineryScales").getExpr(scalesExpr));
            query2.and(groupMachineryExpr.compare(new DataObject(groupMachineryMPT, (ConcreteClass) rublevskiLM.getClassByName("groupScales")), Compare.EQUALS));
            OrderedMap<Map<Object, Object>, Map<Object, Object>> result2 = query2.execute(session.sql);
            //тут InvocationTargetException
            for (Map<Object, Object> values : result2.values()) {
                String numberScales = values.get("numberScales").toString().trim();
                String descriptionMachinery = values.get("descriptionMachinery").toString().trim();
                String nameGroupMachineryScales = values.get("nameGroupMachineryScales").toString().trim();
                scalesInfoList.add(new ScalesInfo(numberScales, descriptionMachinery, nameGroupMachineryScales));
            }           
        }
        return scalesInfoList;
    }
}

