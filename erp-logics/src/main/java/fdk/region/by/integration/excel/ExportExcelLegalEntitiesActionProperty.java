package fdk.region.by.integration.excel;

import jxl.write.WriteException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class ExportExcelLegalEntitiesActionProperty extends ExportExcelActionProperty {

    public ExportExcelLegalEntitiesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public Map<String, byte[]> createFile(ExecutionContext<ClassPropertyInterface> context) throws IOException, WriteException {
        return createFile("exportLegalEntities", getTitles(), getRows(context));

    }

    private List<String> getTitles() {
        return Arrays.asList("Наименование", "Полное наименование", "УНП", "Форма собственности", "Группа организаций",
                "Юридический адрес", "Телефон/Факс", "Внешний код");
    }

    private List<List<String>> getRows(ExecutionContext<ClassPropertyInterface> context) {

        List<List<String>> data = new ArrayList<List<String>>();

        DataSession session = context.getSession();

        try {

            KeyExpr legalEntityExpr = new KeyExpr("LegalEntity");
            ImRevMap<Object, KeyExpr> legalEntityKeys = MapFact.singletonRev((Object) "LegalEntity", legalEntityExpr);

            String[] legalEntityProperties = new String[]{"nameLegalEntity", "fullNameLegalEntity", "UNPLegalEntity",
                    "shortNameOwnershipLegalEntity", "nameLegalEntityGroupLegalEntity", "addressLegalEntity",
                    "phoneLegalEntity"};
            QueryBuilder<Object, Object> legalEntityQuery = new QueryBuilder<Object, Object>(legalEntityKeys);
            for (String uiProperty : legalEntityProperties) {
                legalEntityQuery.addProperty(uiProperty, getLCP(uiProperty).getExpr(context.getModifier(), legalEntityExpr));
            }

            legalEntityQuery.and(getLCP("nameLegalEntity").getExpr(context.getModifier(), legalEntityQuery.getMapExprs().get("LegalEntity")).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> legalEntityResult = legalEntityQuery.execute(session.sql);

            for (int i = 0, size = legalEntityResult.size(); i < size; i++) {

                ImMap<Object, Object> legalEntityValue = legalEntityResult.getValue(i);

                String name = (String) legalEntityValue.get("nameLegalEntity");
                String fullName = (String) legalEntityValue.get("fullNameLegalEntity");
                String unp = (String) legalEntityValue.get("UNPLegalEntity");
                String shortNameOwnership = (String) legalEntityValue.get("shortNameOwnershipLegalEntity");
                String nameLegalEntityGroup = (String) legalEntityValue.get("nameLegalEntityGroupLegalEntity");
                String address = (String) legalEntityValue.get("addressLegalEntity");
                String phone = (String) legalEntityValue.get("phoneLegalEntity");
                Integer legalEntityID = (Integer) legalEntityResult.getKey(i).get("LegalEntity");


                data.add(Arrays.asList(trimNotNull(name), trimNotNull(fullName), trimNotNull(unp), trimNotNull(shortNameOwnership),
                        trimNotNull(nameLegalEntityGroup), trimNotNull(address), trimNotNull(phone), trimNotNull(legalEntityID)));
            }

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return data;
    }


}