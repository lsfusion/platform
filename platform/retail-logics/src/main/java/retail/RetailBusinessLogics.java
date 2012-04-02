package retail;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: DAle
 * Date: 05.01.12
 * Time: 15:34
 */


public class RetailBusinessLogics extends BusinessLogics<RetailBusinessLogics> implements RetailRemoteInterface {
    ScriptingLogicsModule retailLM;

    public RetailBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    public ScriptingLogicsModule getLM() {
        return retailLM;
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        retailLM = new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/retail.lsf"), LM, this);
        addLogicsModule(retailLM);
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

        LP isMachineryPriceTransaction = retailLM.is(retailLM.getClassByName("machineryPriceTransaction"));
        Map<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
        Query<Object, Object> query = new Query<Object, Object>(keys);
        query.properties.put("groupMachineryMPT", retailLM.getLPByName("groupMachineryMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
        query.properties.put("processMPT", retailLM.getLPByName("processMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
        query.and(isMachineryPriceTransaction.property.getExpr(keys).getWhere());
        //query.and(retailLM.getLPByName("processMachineryPriceTransaction").property.getExpr(keys).getWhere());
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : result.entrySet()) {
            String groupMachineryMPT = entry.getValue().get("groupMachineryMPT").toString().trim();
            Boolean processMPT = entry.getValue().get("processMPT") != null;
            if(processMPT)
                groupMashineryMPTList.add(groupMachineryMPT);

            KeyExpr scalesExpr = new KeyExpr("scales");
            KeyExpr groupScalesExpr = new KeyExpr("groupScales");
            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("scales", scalesExpr);
            newKeys.put("groupScales", groupScalesExpr);
            Query<Object, Object> query2 = new Query<Object, Object>(newKeys);
            query2.properties.put("numberScales", retailLM.getLPByName("numberScales").getExpr(scalesExpr));
            query2.properties.put("descriptionMachinery", retailLM.getLPByName("descriptionMachinery").getExpr(scalesExpr));
            query2.properties.put("nameGroupMachineryScales", retailLM.getLPByName("nameGroupMachineryScales").getExpr(scalesExpr));
            query2.and(groupScalesExpr.compare(new DataObject(groupMachineryMPT, (ConcreteClass) retailLM.getClassByName("groupScales")), Compare.EQUALS));
            //query.and(projectExpr.compare(projectObject, Compare.EQUALS));
            OrderedMap<Map<Object, Object>, Map<Object, Object>> result2 = query2.execute(session.sql);
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

