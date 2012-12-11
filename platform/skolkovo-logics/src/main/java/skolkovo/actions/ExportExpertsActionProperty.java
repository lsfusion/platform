package skolkovo.actions;

import org.apache.commons.lang.StringEscapeUtils;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.UserActionProperty;
import skolkovo.SkolkovoLogicsModule;

import java.sql.SQLException;
import java.util.Map;

public class ExportExpertsActionProperty extends UserActionProperty {

    boolean escape;
    SkolkovoLogicsModule LM;

    public ExportExpertsActionProperty(SkolkovoLogicsModule LM, boolean escape) {
        super("exportExperts" + (escape ? "Escape" : ""), "Экспортировать экспертов" + (escape ? "(Escape)" : ""), new ValueClass[] {});

        this.escape = escape;
        this.LM = LM;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        LCP isExpert = LM.is(LM.expert);
        Map<Object, KeyExpr> keys = isExpert.getMapKeys();
        KeyExpr key = BaseUtils.singleValue(keys);
        Query<Object, Object> query = new Query<Object, Object>(keys);

        query.properties.put("inActive", LM.disableExpert.getExpr(context.getModifier(), key));
        query.properties.put("firstName", LM.baseLM.userFirstName.getExpr(context.getModifier(), key));
        query.properties.put("lastName", LM.baseLM.userLastName.getExpr(context.getModifier(), key));
        query.properties.put("documentName", LM.documentNameExpert.getExpr(context.getModifier(), key));
        query.properties.put("userLogin", LM.baseLM.userLogin.getExpr(context.getModifier(), key));
        query.properties.put("email", LM.baseLM.getBL().emailLM.email.getExpr(context.getModifier(), key));
        query.properties.put("nameLanguageExpert", LM.nameLanguageExpert.getExpr(context.getModifier(), key));
        query.properties.put("dateAgreement", LM.dateAgreementExpert.getExpr(context.getModifier(), key));
        query.properties.put("nameCountry", LM.nameCountryExpert.getExpr(context.getModifier(), key));
        query.properties.put("nameCurrency", LM.nameCurrencyExpert.getExpr(context.getModifier(), key));
        query.properties.put("isScientific", LM.isScientificExpert.getExpr(context.getModifier(), key));
        query.properties.put("commentScientific", LM.commentScientificExpert.getExpr(context.getModifier(), key));
        query.properties.put("isTechnical", LM.isTechnicalExpert.getExpr(context.getModifier(), key));
        query.properties.put("commentTechnical", LM.commentTechnicalExpert.getExpr(context.getModifier(), key));
        query.properties.put("isBusiness", LM.isBusinessExpert.getExpr(context.getModifier(), key));
        query.properties.put("commentBusiness", LM.commentBusinessExpert.getExpr(context.getModifier(), key));
        query.properties.put("expertise", LM.expertiseExpert.getExpr(context.getModifier(), key));
        query.properties.put("grant", LM.grantExpert.getExpr(context.getModifier(), key));
        query.properties.put("sid", LM.sidExpert.getExpr(context.getModifier(), key));

        query.and(isExpert.getExpr(key).getWhere());
        
        String xml = "<experts>";
        OrderedMap<Map<Object,DataObject>, Map<Object,ObjectValue>> result = query.executeClasses(context);
        for (Map.Entry<Map<Object, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
            Map<Object, ObjectValue> values = entry.getValue();
            
            DataObject expertObj = BaseUtils.singleValue(entry.getKey());
            
            xml += "<expert>\n";
            for (Map.Entry<Object, ObjectValue> value : values.entrySet()) {
                Object nullValue = value.getValue().getValue();
                if (nullValue instanceof String)
                    nullValue = ((String) nullValue).trim();
                if (nullValue != null) {
                    String stringValue = nullValue.toString();
                    if (escape)
                        stringValue = StringEscapeUtils.escapeXml(stringValue);
                    xml += "    <" + value.getKey() + ">" + stringValue + "</" + value.getKey() + ">\n";
                }
            }

            Map<Object, KeyExpr> clusterKeys = (Map<Object, KeyExpr>) LM.is(LM.cluster).getMapKeys();
            KeyExpr clusterKey = BaseUtils.singleValue(clusterKeys);
            Query<Object, Object> queryCluster = new Query<Object, Object>(clusterKeys);
            queryCluster.properties.put("nameShort", LM.nameNativeShort.getExpr(context.getModifier(), clusterKey));
            queryCluster.and(LM.inClusterExpert.getExpr(context.getModifier(), clusterKey, expertObj.getExpr()).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> resultCluster = queryCluster.execute(context);
            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entryCluster : resultCluster.entrySet()) {
                xml += "    <cluster>" + ((String)BaseUtils.singleValue(entryCluster.getValue())).trim() + "</cluster>\n";
            }

            Map<Object, KeyExpr> foresightKeys = (Map<Object, KeyExpr>) LM.is(LM.foresight).getMapKeys();
            KeyExpr foresightKey = BaseUtils.singleValue(foresightKeys);
            Query<Object, Object> queryForesight = new Query<Object, Object>(foresightKeys);
            queryForesight.properties.put("id", LM.sidForesight.getExpr(context.getModifier(), foresightKey));
            queryForesight.properties.put("comment", LM.commentExpertForesight.getExpr(context.getModifier(), expertObj.getExpr(), foresightKey));
            queryForesight.and(LM.inExpertForesight.getExpr(context.getModifier(), expertObj.getExpr(), foresightKey).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> resultForesight = queryForesight.execute(context);
            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entryForesight : resultForesight.entrySet()) {
                xml += "    <foresight>\n";
                xml += "        <id>" + ((String)entryForesight.getValue().get("id")).trim() + "</id>\n";
                String comment = ((String) BaseUtils.nvl(entryForesight.getValue().get("comment"), "")).trim();
                if (escape)
                    comment = StringEscapeUtils.escapeXml(comment);
                xml += "        <comment>" + comment + "</comment>\n";
                xml += "    </foresight>\n";
            }
            xml += "</expert>\n";
        }

        xml += "</experts>\n";

        context.delayUserInterfaction(new ExportFileClientAction("experts.xml", xml, "utf-8"));
    }
}
