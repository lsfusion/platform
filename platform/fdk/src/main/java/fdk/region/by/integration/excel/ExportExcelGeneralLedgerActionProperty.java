package fdk.region.by.integration.excel;

import jxl.write.WriteException;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.classes.DateClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

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

            DataObject dateFromObject = context.getKeyValue(dateFromInterface);
            DataObject dateToObject = context.getKeyValue(dateToInterface);

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