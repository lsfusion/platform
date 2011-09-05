package skolkovo.actions;

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

public class ExportProjectsActionProperty extends ActionProperty {

    private SkolkovoLogicsModule LM;
    private String projectID;
    private DataSession session;
    private final ClassPropertyInterface projectInterface;
    Map<String, byte[]> files = new HashMap<String, byte[]>();

    public ExportProjectsActionProperty(String caption, SkolkovoLogicsModule LM, ValueClass project) {
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

        DataObject projectObject = context.getKeyValue(projectInterface);

        LP isNonRussianSpecialist = LM.is(LM.nonRussianSpecialist);
        Map<Object, KeyExpr> keys = isNonRussianSpecialist.getMapKeys();
        Query<Object, Object> query = new Query<Object, Object>(Collections.<Object>singleton("nonRussianSpecialist"));
        query.properties.put("fullNameNonRussianSpecialist", LM.fullNameNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
        query.properties.put("fileForeignResumeNonRussianSpecialist", LM.fileForeignResumeNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
        query.properties.put("fileNativeResumeNonRussianSpecialist", LM.fileNativeResumeNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
        query.properties.put("filePassportNonRussianSpecialist", LM.filePassportNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
        query.properties.put("fileStatementNonRussianSpecialist", LM.fileStatementNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
        query.and(isNonRussianSpecialist.property.getExpr(keys).getWhere());
        query.and(LM.projectNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")).compare(projectObject.getExpr(), Compare.EQUALS));
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

        for (Map<Object, Object> values : result.values()) {
            putFileIfNotNull(values.get("fileForeignResumeNonRussianSpecialist"), values.get("fullNameNonRussianSpecialist").toString().trim() + " resume foreign");
            putFileIfNotNull(values.get("fileNativeResumeNonRussianSpecialist"), values.get("fullNameNonRussianSpecialist").toString().trim() + " resume native");
            putFileIfNotNull(values.get("filePassportNonRussianSpecialist"), values.get("fullNameNonRussianSpecialist").toString().trim() + " passport");
            putFileIfNotNull(values.get("fileStatementNonRussianSpecialist"), values.get("fullNameNonRussianSpecialist").toString().trim() + " statement");
        }

        LP isAcademic = LM.is(LM.academic);
        keys = isAcademic.getMapKeys();
        query = new Query<Object, Object>(Collections.<Object>singleton("academic"));
        query.properties.put("fullNameAcademic", LM.fullNameAcademic.getExpr(context.getModifier(), query.mapKeys.get("academic")));
        query.properties.put("fileDocumentConfirmingAcademic", LM.fileDocumentConfirmingAcademic.getExpr(context.getModifier(), query.mapKeys.get("academic")));
        query.properties.put("fileDocumentEmploymentAcademic", LM.fileDocumentEmploymentAcademic.getExpr(context.getModifier(), query.mapKeys.get("academic")));
        query.and(isAcademic.property.getExpr(keys).getWhere());
        query.and(LM.projectAcademic.getExpr(context.getModifier(), query.mapKeys.get("academic")).compare(projectObject.getExpr(), Compare.EQUALS));
        result = query.execute(session.sql);

        for (Map<Object, Object> values : result.values()) {
            putFileIfNotNull(values.get("fileDocumentConfirmingAcademic"), values.get("fullNameAcademic").toString().trim() + " Файл трудового договора");
            putFileIfNotNull(values.get("fileDocumentEmploymentAcademic"), values.get("fullNameAcademic").toString().trim() + " Файл заявления специалиста");
        }

        putFileIfNotNull(LM.fileNativeSummaryProject.read(context, projectObject), "Файл резюме проекта");
        putFileIfNotNull(LM.fileForeignSummaryProject.read(context, projectObject), "Файл резюме проекта (иностр.)");
        putFileIfNotNull(LM.fileNativeRoadMapProject.read(context, projectObject), "Файл дорожной карты");
        putFileIfNotNull(LM.fileForeignRoadMapProject.read(context, projectObject), "Файл дорожной карты (иностр.)");
        putFileIfNotNull(LM.fileResolutionIPProject.read(context, projectObject), "Заявление IP");
        putFileIfNotNull(LM.fileNativeTechnicalDescriptionProject.read(context, projectObject), "Файл технического описания");
        putFileIfNotNull(LM.fileForeignTechnicalDescriptionProject.read(context, projectObject), "Файл технического описания (иностр.)");



        putFileIfNotNull(LM.statementClaimerProject.read(context, projectObject), "Заявление");
        putFileIfNotNull(LM.constituentClaimerProject.read(context, projectObject), "Учредительные документы");
        putFileIfNotNull(LM.extractClaimerProject.read(context, projectObject), "Выписка из реестра");

        context.addAction(new ExportFileClientAction(files));
    }

    private void putFileIfNotNull(Object file, String name) {
        if (file != null) files.put(name + ".pdf", (byte[]) file);
    }
}

