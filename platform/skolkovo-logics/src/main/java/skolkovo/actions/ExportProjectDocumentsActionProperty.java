package skolkovo.actions;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.UserActionProperty;
import platform.server.session.DataSession;
import skolkovo.SkolkovoLogicsModule;

import java.io.*;
import java.sql.*;
import java.util.*;

public class ExportProjectDocumentsActionProperty extends UserActionProperty {

    private SkolkovoLogicsModule LM;
    private DataSession session;
    private final ClassPropertyInterface projectInterface;

    public ExportProjectDocumentsActionProperty(String caption, SkolkovoLogicsModule LM, ValueClass project) {
        super("exportProjectDocumentsAction", caption, new ValueClass[]{project});
        this.LM = LM;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        projectInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        this.session = context.getSession();

        Map<String, byte[]> files = new HashMap<String, byte[]>();

        DataObject projectObject = context.getDataKeyValue(projectInterface);

        // документы по проекту

        try {

            boolean newRegulation = false;
            Object regulation = LM.nameRegulationsProject.read(context, projectObject);
            if (regulation != null) {
                if ("R2".equals(regulation.toString().trim()))
                    newRegulation = true;
            }

            if ((!putFileIfNotNull(files, LM.fileNativeApplicationFormProject.read(context, projectObject), "Анкета заявителя" ,true))
                    && LM.needsToBeTranslatedToRussianProject.read(context, projectObject) == null)
                putFileIfNotNull(files, LM.generateApplicationFile(context, projectObject, false, newRegulation, true), "Анкета заявителя" ,false);
            if ((!putFileIfNotNull(files, LM.fileForeignApplicationFormProject.read(context, projectObject), "Анкета заявителя (иностр.)" ,true))
                    && LM.needsToBeTranslatedToEnglishProject.read(context, projectObject) == null)
                putFileIfNotNull(files, LM.generateApplicationFile(context, projectObject, true, newRegulation, true), "Анкета заявителя (иностр.)", false);


            if (!newRegulation) {
                putFileIfNotNull(files, LM.fileNativeSummaryProject.read(context, projectObject), "Файл резюме проекта" ,true);
                putFileIfNotNull(files, LM.fileForeignSummaryProject.read(context, projectObject), "Файл резюме проекта (иностр.)" ,true);
                putFileIfNotNull(files, LM.fileNativeRoadMapProject.read(context, projectObject), "Файл дорожной карты" ,true);
                putFileIfNotNull(files, LM.fileForeignRoadMapProject.read(context, projectObject), "Файл дорожной карты (иностр.)" ,true);
                putFileIfNotNull(files, LM.fileResolutionIPProject.read(context, projectObject), "Заявление IP" ,true);
                putFileIfNotNull(files, LM.fileNativeTechnicalDescriptionProject.read(context, projectObject), "Файл технического описания" ,true);
                putFileIfNotNull(files, LM.fileForeignTechnicalDescriptionProject.read(context, projectObject), "Файл технического описания (иностр.)" ,true);

                LCP isNonRussianSpecialist = LM.is(LM.nonRussianSpecialist);
                ImRevMap<Object, KeyExpr> keys = isNonRussianSpecialist.getMapKeys();
                KeyExpr key = keys.singleValue();
                QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
                query.addProperty("fullNameNonRussianSpecialist", LM.fullNameNonRussianSpecialist.getExpr(context.getModifier(), key));
                query.addProperty("fileForeignResumeNonRussianSpecialist", LM.fileForeignResumeNonRussianSpecialist.getExpr(context.getModifier(), key));
                query.addProperty("fileNativeResumeNonRussianSpecialist", LM.fileNativeResumeNonRussianSpecialist.getExpr(context.getModifier(), key));
                query.addProperty("filePassportNonRussianSpecialist", LM.filePassportNonRussianSpecialist.getExpr(context.getModifier(), key));
                query.addProperty("fileStatementNonRussianSpecialist", LM.fileStatementNonRussianSpecialist.getExpr(context.getModifier(), key));
                query.and(isNonRussianSpecialist.getExpr(key).getWhere());
                query.and(LM.projectNonRussianSpecialist.getExpr(context.getModifier(), key).compare(projectObject.getExpr(), Compare.EQUALS));
                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

                for (ImMap<Object, Object> values : result.valueIt()) {
                    String fullName = values.get("fullNameNonRussianSpecialist").toString().trim().replace("/", "-").replace("\\", "-");
                    putFileIfNotNull(files, values.get("fileForeignResumeNonRussianSpecialist"), fullName + " resume foreign" ,true);
                    putFileIfNotNull(files, values.get("fileNativeResumeNonRussianSpecialist"), fullName + " resume native" ,true);
                    putFileIfNotNull(files, values.get("filePassportNonRussianSpecialist"), fullName + " passport" ,true);
                    putFileIfNotNull(files, values.get("fileStatementNonRussianSpecialist"), fullName + " statement" ,true);
                }

                LCP isAcademic = LM.is(LM.academic);
                keys = isAcademic.getMapKeys();
                key = keys.singleValue();
                query = new QueryBuilder<Object, Object>(keys);
                query.addProperty("fullNameAcademic", LM.fullNameAcademic.getExpr(context.getModifier(), key));
                query.addProperty("fileDocumentConfirmingAcademic", LM.fileDocumentConfirmingAcademic.getExpr(context.getModifier(), key));
                query.addProperty("fileDocumentEmploymentAcademic", LM.fileDocumentEmploymentAcademic.getExpr(context.getModifier(), key));
                query.and(isAcademic.getExpr(key).getWhere());
                query.and(LM.projectAcademic.getExpr(context.getModifier(), key).compare(projectObject.getExpr(), Compare.EQUALS));
                result = query.execute(session.sql);

                for (ImMap<Object, Object> values : result.valueIt()) {
                    putFileIfNotNull(files, values.get("fileDocumentConfirmingAcademic"), values.get("fullNameAcademic").toString().trim() + " Файл трудового договора" ,true);
                    putFileIfNotNull(files, values.get("fileDocumentEmploymentAcademic"), values.get("fullNameAcademic").toString().trim() + " Файл заявления специалиста" ,true);
                }
            } else {

                LCP isSpecialist = LM.is(LM.specialist);
                ImRevMap<Object, KeyExpr> keys = isSpecialist.getMapKeys();
                KeyExpr key = keys.singleValue();
                QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
                query.addProperty("nameNativeSpecialist", LM.nameNativeSpecialist.getExpr(context.getModifier(), key));
                query.addProperty("filePassportSpecialist", LM.filePassportSpecialist.getExpr(context.getModifier(), key));
                query.addProperty("fileStatementSpecialist", LM.fileStatementSpecialist.getExpr(context.getModifier(), key));
                query.and(isSpecialist.getExpr(key).getWhere());
                query.and(LM.projectSpecialist.getExpr(context.getModifier(), key).compare(projectObject.getExpr(), Compare.EQUALS));
                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

                for (ImMap<Object, Object> values : result.valueIt()) {
                    putFileIfNotNull(files, values.get("filePassportSpecialist"), values.get("nameNativeSpecialist").toString().trim() + " Файл документа, удостоверяющего личность" ,true);
                    putFileIfNotNull(files, values.get("fileStatementSpecialist"), values.get("nameNativeSpecialist").toString().trim() + " Файл заявления участника команды" ,true);
                }
            }

            // юридические документы
            putFileIfNotNull(files, LM.statementClaimerProject.read(context, projectObject), "Заявление", true);
            putFileIfNotNull(files, LM.constituentClaimerProject.read(context, projectObject), "Учредительные документы", true);
            putFileIfNotNull(files, LM.extractClaimerProject.read(context, projectObject), "Выписка из реестра", true);

            putFileIfNotNull(files, LM.fileMinutesOfMeetingExpertCollegiumProject.read(context, projectObject), "Протокол заседания экспертной коллегии", true);
            putFileIfNotNull(files, LM.fileWrittenConsentClaimerProject.read(context, projectObject), "Письменное согласие заявителя", true);

            context.delayUserInterfaction(new ExportFileClientAction(files));

            System.gc();

        } catch (IOException e) {
            new RuntimeException("Ошибка при экспорте документов", e);
        } catch (ClassNotFoundException e) {
            new RuntimeException("Ошибка при экспорте документов", e);
        } catch (JRException e) {
            new RuntimeException("Ошибка при экспорте документов", e);
        }
    }

    private boolean putFileIfNotNull(Map<String, byte[]> files, Object file, String name, Boolean isCustomFileClass) {
        if (file != null) {
            if (isCustomFileClass)
                files.put(name + "." + BaseUtils.getExtension((byte[]) file).substring(0, 3).toLowerCase(), BaseUtils.getFile((byte[]) file));
            else
                files.put(name + ".pdf", (byte[]) file);
            return true;
        } else {
            return false;
        }
    }
}