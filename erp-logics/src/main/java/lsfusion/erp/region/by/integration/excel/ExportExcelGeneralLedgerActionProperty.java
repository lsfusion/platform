package lsfusion.erp.region.by.integration.excel;

import jxl.write.WriteException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.DateClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExportExcelGeneralLedgerActionProperty extends ExportExcelActionProperty {
    private final ClassPropertyInterface dateFromInterface;
    private final ClassPropertyInterface dateToInterface;

    public ExportExcelGeneralLedgerActionProperty(ScriptingLogicsModule LM) {
        super(LM, DateClass.instance, DateClass.instance);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        dateFromInterface = i.next();
        dateToInterface = i.next();
    }

    public ExportExcelGeneralLedgerActionProperty(ScriptingLogicsModule LM, ClassPropertyInterface dateFrom, ClassPropertyInterface dateTo) {
        super(LM, DateClass.instance, DateClass.instance);

        dateFromInterface = dateFrom;
        dateToInterface = dateTo;
    }

    @Override
    public Map<String, byte[]> createFile(ExecutionContext<ClassPropertyInterface> context) throws IOException, WriteException {
        return createFile("exportGeneralLedger", getTitles(), getRows(context));

    }

    private List<String> getTitles() {
        return Arrays.asList("Проведён", "Дата", "Компания", "Регистр-основание", "Описание", "Дебет", "Субконто (дебет)",
                "Кредит", "Субконто (кредит)", "Сумма");
    }

    private List<List<String>> getRows(ExecutionContext<ClassPropertyInterface> context) {

        List<List<String>> data = new ArrayList<List<String>>();

        DataSession session = context.getSession();

        try {

            DataObject dateFromObject = context.getDataKeyValue(dateFromInterface);
            DataObject dateToObject = context.getDataKeyValue(dateToInterface);

            KeyExpr generalLedgerExpr = new KeyExpr("GeneralLedger");
            ImRevMap<Object, KeyExpr> generalLedgerKeys = MapFact.singletonRev((Object) "GeneralLedger", generalLedgerExpr);

            String[] generalLedgerProperties = new String[]{"isPostedGeneralLedger", "dateGeneralLedger",
                    "nameLegalEntityGeneralLedger", "nameGLDocumentGeneralLedger", "descriptionGeneralLedger",
                    "idDebitGeneralLedger", "dimensionsDebitGeneralLedger", "idCreditGeneralLedger",
                    "dimensionsCreditGeneralLedger", "sumGeneralLedger"};
            QueryBuilder<Object, Object> generalLedgerQuery = new QueryBuilder<Object, Object>(generalLedgerKeys);
            for (String uiProperty : generalLedgerProperties) {
                generalLedgerQuery.addProperty(uiProperty, getLCP(uiProperty).getExpr(context.getModifier(), generalLedgerExpr));
            }

            generalLedgerQuery.and(getLCP("sumGeneralLedger").getExpr(context.getModifier(), generalLedgerQuery.getMapExprs().get("GeneralLedger")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> generalLedgerResult = generalLedgerQuery.execute(session.sql);

            for (ImMap<Object, Object> generalLedgerValue : generalLedgerResult.values()) {

                Date date = (Date) generalLedgerValue.get("dateGeneralLedger");

                if ((dateFromObject.object == null || ((Date) dateFromObject.object).getTime() <= date.getTime()) && (dateToObject.object == null || ((Date) dateToObject.object).getTime() >= date.getTime())) {

                    String isPostedGeneralLedger = generalLedgerValue.get("isPostedGeneralLedger") == null ? "FALSE" : "TRUE";
                    String dateGeneralLedger = new SimpleDateFormat("dd.MM.yyyy").format(date);
                    String nameLegalEntity = (String) generalLedgerValue.get("nameLegalEntityGeneralLedger");
                    String nameGLDocument = (String) generalLedgerValue.get("nameGLDocumentGeneralLedger");
                    String description = (String) generalLedgerValue.get("descriptionGeneralLedger");
                    String idDebit = (String) generalLedgerValue.get("idDebitGeneralLedger");
                    String dimensionsDebit = (String) generalLedgerValue.get("dimensionsDebitGeneralLedger");
                    String idCredit = (String) generalLedgerValue.get("idCreditGeneralLedger");
                    String dimensionsCredit = (String) generalLedgerValue.get("dimensionsCreditGeneralLedger");
                    String sumGeneralLedger = String.valueOf(generalLedgerValue.get("sumGeneralLedger"));


                    data.add(Arrays.asList(isPostedGeneralLedger, dateGeneralLedger, trimNotNull(nameLegalEntity),
                            trimNotNull(nameGLDocument), trimNotNull(description), trimNotNull(idDebit),
                            trimNotNull(dimensionsDebit), trimNotNull(idCredit), trimNotNull(dimensionsCredit),
                            trimNotNull(sumGeneralLedger)));
                }
            }

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return data;
    }




}