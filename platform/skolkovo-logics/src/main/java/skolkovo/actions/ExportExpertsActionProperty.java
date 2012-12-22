package skolkovo.actions;

import org.apache.commons.lang.StringEscapeUtils;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
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

        LCP<?> isExpert = LM.is(LM.expert);
        ImRevMap<Object, KeyExpr> keys = (ImRevMap<Object, KeyExpr>) isExpert.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);

        query.addProperty("inActive", LM.disableExpert.getExpr(context.getModifier(), key));
        query.addProperty("firstName", LM.baseLM.userFirstName.getExpr(context.getModifier(), key));
        query.addProperty("lastName", LM.baseLM.userLastName.getExpr(context.getModifier(), key));
        query.addProperty("documentName", LM.documentNameExpert.getExpr(context.getModifier(), key));
        query.addProperty("userLogin", LM.baseLM.userLogin.getExpr(context.getModifier(), key));
        query.addProperty("email", LM.baseLM.getBL().emailLM.emailContact.getExpr(context.getModifier(), key));
        query.addProperty("nameLanguageExpert", LM.nameLanguageExpert.getExpr(context.getModifier(), key));
        query.addProperty("dateAgreement", LM.dateAgreementExpert.getExpr(context.getModifier(), key));
        query.addProperty("nameCountry", LM.nameCountryExpert.getExpr(context.getModifier(), key));
        query.addProperty("nameCurrency", LM.nameCurrencyExpert.getExpr(context.getModifier(), key));
        query.addProperty("isScientific", LM.isScientificExpert.getExpr(context.getModifier(), key));
        query.addProperty("commentScientific", LM.commentScientificExpert.getExpr(context.getModifier(), key));
        query.addProperty("isTechnical", LM.isTechnicalExpert.getExpr(context.getModifier(), key));
        query.addProperty("commentTechnical", LM.commentTechnicalExpert.getExpr(context.getModifier(), key));
        query.addProperty("isBusiness", LM.isBusinessExpert.getExpr(context.getModifier(), key));
        query.addProperty("commentBusiness", LM.commentBusinessExpert.getExpr(context.getModifier(), key));
        query.addProperty("expertise", LM.expertiseExpert.getExpr(context.getModifier(), key));
        query.addProperty("grant", LM.grantExpert.getExpr(context.getModifier(), key));
        query.addProperty("sid", LM.sidExpert.getExpr(context.getModifier(), key));

        query.and(isExpert.getExpr(key).getWhere());
        
        String xml = "<experts>";
        ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(context);
        for (int i=0,size=result.size();i<size;i++) {
            ImMap<Object, ObjectValue> values = result.getValue(i);
            
            DataObject expertObj = result.getKey(i).singleValue();
            
            xml += "<expert>\n";
            for (int j=0,sizeJ=values.size();j<sizeJ;j++) {
                Object nullValue = values.getValue(j).getValue();
                if (nullValue instanceof String)
                    nullValue = ((String) nullValue).trim();
                if (nullValue != null) {
                    String stringValue = nullValue.toString();
                    if (escape)
                        stringValue = StringEscapeUtils.escapeXml(stringValue);
                    xml += "    <" + values.getKey(j) + ">" + stringValue + "</" + values.getKey(j) + ">\n";
                }
            }

            ImRevMap<Object, KeyExpr> clusterKeys = (ImRevMap<Object, KeyExpr>) LM.is(LM.cluster).getMapKeys();
            KeyExpr clusterKey = clusterKeys.singleValue();
            QueryBuilder<Object, Object> queryCluster = new QueryBuilder<Object, Object>(clusterKeys);
            queryCluster.addProperty("nameShort", LM.nameNativeShort.getExpr(context.getModifier(), clusterKey));
            queryCluster.and(LM.inClusterExpert.getExpr(context.getModifier(), clusterKey, expertObj.getExpr()).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> resultCluster = queryCluster.execute(context);
            for (ImMap<Object, Object> entryCluster : resultCluster.valueIt()) {
                xml += "    <cluster>" + ((String)entryCluster.singleValue()).trim() + "</cluster>\n";
            }

            ImRevMap<Object, KeyExpr> foresightKeys = (ImRevMap<Object, KeyExpr>) LM.is(LM.foresight).getMapKeys();
            KeyExpr foresightKey = foresightKeys.singleValue();
            QueryBuilder<Object, Object> queryForesight = new QueryBuilder<Object, Object>(foresightKeys);
            queryForesight.addProperty("id", LM.sidForesight.getExpr(context.getModifier(), foresightKey));
            queryForesight.addProperty("comment", LM.commentExpertForesight.getExpr(context.getModifier(), expertObj.getExpr(), foresightKey));
            queryForesight.and(LM.inExpertForesight.getExpr(context.getModifier(), expertObj.getExpr(), foresightKey).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> resultForesight = queryForesight.execute(context);
            for (ImMap<Object, Object> entryForesight : resultForesight.valueIt()) {
                xml += "    <foresight>\n";
                xml += "        <id>" + ((String)entryForesight.get("id")).trim() + "</id>\n";
                String comment = ((String) BaseUtils.nvl(entryForesight.get("comment"), "")).trim();
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
