package skolkovo.actions;

import net.sf.jasperreports.engine.JRException;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.action.ExportFileClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.CustomFileClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;
import skolkovo.SkolkovoBusinessLogics;
import skolkovo.SkolkovoLogicsModule;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class ExportProjectDocumentsActionProperty extends ActionProperty {

    private SkolkovoLogicsModule LM;
    private DataSession session;
    private final ClassPropertyInterface projectInterface;

    public ExportProjectDocumentsActionProperty(String caption, SkolkovoLogicsModule LM, ValueClass project) {
        super(LM.genSID(), caption, new ValueClass[]{project});
        this.LM = LM;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        projectInterface = i.next();
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        throw new RuntimeException("no need");
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        this.session = context.getSession();

        Map<String, byte[]> files = new HashMap<String, byte[]>();

        DataObject projectObject = context.getKeyValue(projectInterface);

        // документы по проекту

        try {
            if ((!putFileIfNotNull(files, LM.fileNativeApplicationFormProject.read(context, projectObject), "Анкета заявителя"))
                    && (LM.fillNativeProject.read(context, projectObject))== (Object)true || (LM.translatedToRussianProject.read(context, projectObject))== (Object)true)
                putFileIfNotNull(files, LM.generateApplicationFile(context, projectObject, false), "Анкета заявителя");
            if ((!putFileIfNotNull(files, LM.fileForeignApplicationFormProject.read(context, projectObject), "Анкета заявителя (иностр.)"))
                && (LM.fillForeignProject.read(context, projectObject)== (Object)true) || (LM.translatedToEnglishProject.read(context, projectObject) == (Object)true))
                putFileIfNotNull(files, LM.generateApplicationFile(context, projectObject, true), "Анкета заявителя (иностр.)");
        } catch (IOException e) {
            new RuntimeException("Ошибка при экспорте документов", e);
        } catch (ClassNotFoundException e) {
            new RuntimeException("Ошибка при экспорте документов", e);
        } catch (JRException e) {
            new RuntimeException("Ошибка при экспорте документов", e);
        }

        putFileIfNotNull(files, LM.fileNativeSummaryProject.read(context, projectObject), "Файл резюме проекта");
        putFileIfNotNull(files, LM.fileForeignSummaryProject.read(context, projectObject), "Файл резюме проекта (иностр.)");
        putFileIfNotNull(files, LM.fileNativeRoadMapProject.read(context, projectObject), "Файл дорожной карты");
        putFileIfNotNull(files, LM.fileForeignRoadMapProject.read(context, projectObject), "Файл дорожной карты (иностр.)");
        putFileIfNotNull(files, LM.fileResolutionIPProject.read(context, projectObject), "Заявление IP");
        putFileIfNotNull(files, LM.fileNativeTechnicalDescriptionProject.read(context, projectObject), "Файл технического описания");
        putFileIfNotNull(files, LM.fileForeignTechnicalDescriptionProject.read(context, projectObject), "Файл технического описания (иностр.)");

        LP isNonRussianSpecialist = LM.is(LM.nonRussianSpecialist);
        Map<Object, KeyExpr> keys = isNonRussianSpecialist.getMapKeys();
        KeyExpr key = BaseUtils.singleValue(keys);
        Query<Object, Object> query = new Query<Object, Object>(keys);
        query.properties.put("fullNameNonRussianSpecialist", LM.fullNameNonRussianSpecialist.getExpr(context.getModifier(), key));
        query.properties.put("fileForeignResumeNonRussianSpecialist", LM.fileForeignResumeNonRussianSpecialist.getExpr(context.getModifier(), key));
        query.properties.put("fileNativeResumeNonRussianSpecialist", LM.fileNativeResumeNonRussianSpecialist.getExpr(context.getModifier(), key));
        query.properties.put("filePassportNonRussianSpecialist", LM.filePassportNonRussianSpecialist.getExpr(context.getModifier(), key));
        query.properties.put("fileStatementNonRussianSpecialist", LM.fileStatementNonRussianSpecialist.getExpr(context.getModifier(), key));
        query.and(isNonRussianSpecialist.getExpr(key).getWhere());
        query.and(LM.projectNonRussianSpecialist.getExpr(context.getModifier(), key).compare(projectObject.getExpr(), Compare.EQUALS));
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

        for (Map<Object, Object> values : result.values()) {
            String fullName = values.get("fullNameNonRussianSpecialist").toString().trim().replace("/", "-").replace("\\", "-");
            putFileIfNotNull(files, values.get("fileForeignResumeNonRussianSpecialist"), fullName + " resume foreign");
            putFileIfNotNull(files, values.get("fileNativeResumeNonRussianSpecialist"), fullName + " resume native");
            putFileIfNotNull(files, values.get("filePassportNonRussianSpecialist"), fullName + " passport");
            putFileIfNotNull(files, values.get("fileStatementNonRussianSpecialist"), fullName + " statement");
        }

        LP isAcademic = LM.is(LM.academic);
        keys = isAcademic.getMapKeys();
        key = BaseUtils.singleValue(keys);
        query = new Query<Object, Object>(keys);
        query.properties.put("fullNameAcademic", LM.fullNameAcademic.getExpr(context.getModifier(), key));
        query.properties.put("fileDocumentConfirmingAcademic", LM.fileDocumentConfirmingAcademic.getExpr(context.getModifier(), key));
        query.properties.put("fileDocumentEmploymentAcademic", LM.fileDocumentEmploymentAcademic.getExpr(context.getModifier(), key));
        query.and(isAcademic.getExpr(key).getWhere());
        query.and(LM.projectAcademic.getExpr(context.getModifier(), key).compare(projectObject.getExpr(), Compare.EQUALS));
        result = query.execute(session.sql);

        for (Map<Object, Object> values : result.values()) {
            putFileIfNotNull(files, values.get("fileDocumentConfirmingAcademic"), values.get("fullNameAcademic").toString().trim() + " Файл трудового договора");
            putFileIfNotNull(files, values.get("fileDocumentEmploymentAcademic"), values.get("fullNameAcademic").toString().trim() + " Файл заявления специалиста");
        }

        // юридические документы
        putFileIfNotNull(files, LM.statementClaimerProject.read(context, projectObject), "Заявление");
        putFileIfNotNull(files, LM.constituentClaimerProject.read(context, projectObject), "Учредительные документы");
        putFileIfNotNull(files, LM.extractClaimerProject.read(context, projectObject), "Выписка из реестра");

        context.addAction(new ExportFileClientAction(files));

        System.gc();
    }

    private boolean putFileIfNotNull(Map<String, byte[]> files, Object file, String name) {
        if (file != null) {
            files.put(name + ".pdf", (byte[]) file);
            return true;
        } else {
            return false;
        }
    }
}