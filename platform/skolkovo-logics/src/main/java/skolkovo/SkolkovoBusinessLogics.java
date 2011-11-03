package skolkovo;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.auth.PolicyManager;
import platform.server.auth.User;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.type.ObjectType;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.session.DataSession;
import skolkovo.api.gwt.shared.ForesightInfo;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static platform.base.BaseUtils.nullTrim;
import static platform.base.BaseUtils.nvl;

public class SkolkovoBusinessLogics extends BusinessLogics<SkolkovoBusinessLogics> implements SkolkovoRemoteInterface {
    private SkolkovoLogicsModule SkolkovoLM;

    public SkolkovoBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    public void createModules() {
        super.createModules();
        SkolkovoLM = new SkolkovoLogicsModule(LM, this);
        addLogicsModule(SkolkovoLM);
    }


    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        PolicyManager.defaultSecurityPolicy.navigator.deny(LM.adminElement, LM.objectElement, SkolkovoLM.languageDocumentTypeForm, SkolkovoLM.globalForm);

        PolicyManager.defaultSecurityPolicy.property.view.deny(LM.userPassword);

        PolicyManager.defaultSecurityPolicy.property.change.deny(SkolkovoLM.dateStartVote, SkolkovoLM.dateEndVote, SkolkovoLM.inExpertVote, SkolkovoLM.oldExpertVote, SkolkovoLM.voteResultExpertVote, SkolkovoLM.doneExpertVote);

        for (Property property : SkolkovoLM.voteResultGroup.getProperties())
            PolicyManager.defaultSecurityPolicy.property.change.deny(property);

        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
    }
    //!!!!

    public VoteInfo getVoteInfo(String voteId, String locale) throws RemoteException {

        try {
            DataSession session = createSession();
            try {
                VoteObjects vo = new VoteObjects(session, voteId);

                VoteInfo voteInfo = new VoteInfo();
                voteInfo.expertName = (String) LM.name.read(session, vo.expertObj);

                boolean isForeign = locale != null
                                    ? "en".equals(locale)
                                    : SkolkovoLM.isForeignExpert.read(session, vo.expertObj) != null;

                if (!isForeign) {
                    voteInfo.projectName = (String) SkolkovoLM.nameNativeProject.read(session, vo.projectObj);
                    voteInfo.projectClaimer = (String) SkolkovoLM.nameNativeClaimerProject.read(session, vo.projectObj);
                    voteInfo.projectCluster = (String) SkolkovoLM.nameNativeClusterVote.read(session, vo.voteObj);
                } else {
                    voteInfo.projectName = (String) SkolkovoLM.nameForeignProject.read(session, vo.projectObj);
                    voteInfo.projectClaimer = (String) SkolkovoLM.nameForeignClaimerProject.read(session, vo.projectObj);
                    voteInfo.projectCluster = (String) SkolkovoLM.nameForeignClusterVote.read(session, vo.voteObj);
                }

                voteInfo.revision = (String) SkolkovoLM.revisionVote.read(session, vo.voteObj);

                voteInfo.inCluster = nvl((Boolean) SkolkovoLM.inClusterExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.innovative = nvl((Boolean) SkolkovoLM.innovativeExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.innovativeComment = (String) SkolkovoLM.innovativeCommentExpertVote.read(session, vo.expertObj, vo.voteObj);
                voteInfo.foreign = nvl((Boolean) SkolkovoLM.foreignExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.competent = nvl((Integer) SkolkovoLM.competentExpertVote.read(session, vo.expertObj, vo.voteObj), 1);
                voteInfo.complete = nvl((Integer) SkolkovoLM.completeExpertVote.read(session, vo.expertObj, vo.voteObj), 1);
                voteInfo.completeComment = (String) SkolkovoLM.completeCommentExpertVote.read(session, vo.expertObj, vo.voteObj);

                voteInfo.competitiveAdvantages = nvl((Boolean) SkolkovoLM.competitiveAdvantagesExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.competitiveAdvantagesComment = (String) SkolkovoLM.commentCompetitiveAdvantagesExpertVote.read(session, vo.expertObj, vo.voteObj);
                voteInfo.commercePotential = nvl((Boolean) SkolkovoLM.commercePotentialExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.commercePotentialComment = (String) SkolkovoLM.commentCommercePotentialExpertVote.read(session, vo.expertObj, vo.voteObj);
                voteInfo.implement = nvl((Boolean) SkolkovoLM.canBeImplementedExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.implementComment = (String) SkolkovoLM.commentCanBeImplementedExpertVote.read(session, vo.expertObj, vo.voteObj);
                voteInfo.expertise = nvl((Boolean) SkolkovoLM.haveExpertiseExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.expertiseComment = (String) SkolkovoLM.commentHaveExpertiseExpertVote.read(session, vo.expertObj, vo.voteObj);
                voteInfo.internationalExperience = nvl((Boolean) SkolkovoLM.internationalExperienceExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.internationalExperienceComment = (String) SkolkovoLM.commentInternationalExperienceExpertVote.read(session, vo.expertObj, vo.voteObj);
                voteInfo.enoughDocuments = nvl((Boolean) SkolkovoLM.enoughDocumentsExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.enoughDocumentsComment = (String) SkolkovoLM.commentEnoughDocumentsExpertVote.read(session, vo.expertObj, vo.voteObj);

                Integer vResult = (Integer) SkolkovoLM.voteResultExpertVote.read(session, vo.expertObj, vo.voteObj);
                if (vResult != null) {
                    voteInfo.voteResult = SkolkovoLM.voteResult.getSID(vResult);
                }

                voteInfo.voteDone = voteInfo.voteResult != null
                                    || !nvl((Boolean) SkolkovoLM.openedVote.read(session, vo.voteObj), false);

                voteInfo.date = DateConverter.sqlToDate((java.sql.Date)SkolkovoLM.dateExpertVote.read(session, vo.expertObj, vo.voteObj));

                return correctVoteInfo(voteInfo);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о проекте", e);
        }
    }

    private VoteInfo correctVoteInfo(VoteInfo vi) {
        vi.projectClaimer = nullTrim(vi.projectClaimer);
        vi.projectName = nullTrim(vi.projectName);
        vi.projectCluster = nullTrim(vi.projectCluster);
        vi.voteResult = nullTrim(vi.voteResult);
        vi.innovativeComment = nullTrim(vi.innovativeComment);
        vi.competent = max(1, min(vi.competent, 5));
        vi.complete = max(1, min(vi.complete, 5));
        vi.completeComment = nullTrim(vi.completeComment);
        vi.competitiveAdvantagesComment = nullTrim(vi.competitiveAdvantagesComment);
        vi.commercePotentialComment = nullTrim(vi.commercePotentialComment);
        vi.implementComment = nullTrim(vi.implementComment);
        vi.expertiseComment = nullTrim(vi.expertiseComment);
        vi.internationalExperienceComment = nullTrim(vi.internationalExperienceComment);
        vi.enoughDocumentsComment = nullTrim(vi.enoughDocumentsComment);

        return vi;
    }

    public void setVoteInfo(String voteId, VoteInfo voteInfo) throws RemoteException {
        voteInfo = correctVoteInfo(voteInfo);

        try {
            DataSession session = createSession();
            try {
                VoteObjects vo = new VoteObjects(session, voteId);
                Boolean opVote = (Boolean) SkolkovoLM.openedVote.read(session, vo.voteObj);
                if (opVote == null || !opVote) {
                    throw new RuntimeException("Голосование по заседанию с идентификатором " + vo.voteObj.object + " завершено");
                }

                Integer vResult = (Integer) SkolkovoLM.voteResultExpertVote.read(session, vo.expertObj, vo.voteObj);
                if (vResult != null) {
                    throw new RuntimeException("Эксперт уже голосовал по этому заседанию.");
                }

                SkolkovoLM.dateExpertVote.execute(DateConverter.dateToSql(new Date()), session, vo.expertObj, vo.voteObj);
                SkolkovoLM.voteResultExpertVote.execute(SkolkovoLM.voteResult.getID(voteInfo.voteResult), session, vo.expertObj, vo.voteObj);
                if (voteInfo.voteResult.equals("voted")) {
                    SkolkovoLM.inClusterExpertVote.execute(voteInfo.inCluster, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.innovativeExpertVote.execute(voteInfo.innovative, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.innovativeCommentExpertVote.execute(voteInfo.innovativeComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.foreignExpertVote.execute(voteInfo.foreign, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.competentExpertVote.execute(voteInfo.competent, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.completeExpertVote.execute(voteInfo.complete, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.completeCommentExpertVote.execute(voteInfo.completeComment, session, vo.expertObj, vo.voteObj);

                    SkolkovoLM.competitiveAdvantagesExpertVote.execute(voteInfo.competitiveAdvantages, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentCompetitiveAdvantagesExpertVote.execute(voteInfo.competitiveAdvantagesComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commercePotentialExpertVote.execute(voteInfo.commercePotential, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentCommercePotentialExpertVote.execute(voteInfo.commercePotentialComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.canBeImplementedExpertVote.execute(voteInfo.implement, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentCanBeImplementedExpertVote.execute(voteInfo.implementComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.haveExpertiseExpertVote.execute(voteInfo.expertise, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentHaveExpertiseExpertVote.execute(voteInfo.expertiseComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.internationalExperienceExpertVote.execute(voteInfo.internationalExperience, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentInternationalExperienceExpertVote.execute(voteInfo.internationalExperienceComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.enoughDocumentsExpertVote.execute(voteInfo.enoughDocuments, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentEnoughDocumentsExpertVote.execute(voteInfo.enoughDocumentsComment, session, vo.expertObj, vo.voteObj);
                }

                String result = session.apply(this);
                if (result != null) {
                    throw new RuntimeException("Не удалось сохранить информацию о голосовании : " + result);
                }
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при записи информации о голосовании", e);
        }
    }

    public ProfileInfo getProfileInfo(String expertLogin, String locale) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) LM.loginToUser.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }
                DataObject expertObj = new DataObject(expertId, SkolkovoLM.expert);

                ProfileInfo profileInfo = new ProfileInfo();

                profileInfo.expertName = (String) LM.name.read(session, expertObj);
                profileInfo.expertEmail = (String) LM.email.read(session, expertObj);

                boolean isForeign;
                if (locale != null)
                    isForeign = "en".equals(locale);
                else
                    isForeign = SkolkovoLM.isForeignExpert.read(session, expertObj) != null;

                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("exp", "vote"));
                Expr expertExpr = keys.get("exp");
                Expr voteExpr = keys.get("vote");
                Expr projExpr = SkolkovoLM.projectVote.getExpr(session.modifier, voteExpr);

                Query<String, String> q = new Query<String, String>(keys);
                q.and(SkolkovoLM.inNewExpertVote.getExpr(session.modifier, expertExpr, voteExpr).getWhere());
                q.and(LM.userLogin.getExpr(session.modifier, expertExpr).compare(new DataObject(expertLogin), Compare.EQUALS));

                q.properties.put("projectId", projExpr);
                q.properties.put("projectName", (isForeign ? SkolkovoLM.nameForeignProject : SkolkovoLM.nameNativeProject).getExpr(session.modifier, projExpr));
                q.properties.put("projectClaimer", (isForeign ? SkolkovoLM.nameForeignClaimerProject : SkolkovoLM.nameNativeClaimerProject).getExpr(session.modifier, projExpr));
                q.properties.put("projectCluster", (isForeign ? SkolkovoLM.nameForeignClusterExpert : SkolkovoLM.nameNativeClusterExpert).getExpr(session.modifier, expertExpr));

                q.properties.put("inCluster", SkolkovoLM.inClusterExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("innovative", SkolkovoLM.innovativeExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("innovativeComment", SkolkovoLM.innovativeCommentExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("foreign", SkolkovoLM.foreignExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("competent", SkolkovoLM.competentExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("complete", SkolkovoLM.completeExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("completeComment", SkolkovoLM.completeCommentExpertVote.getExpr(session.modifier, expertExpr, voteExpr));

                q.properties.put("competitive", SkolkovoLM.competitiveAdvantagesExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("competitiveComment", SkolkovoLM.commentCompetitiveAdvantagesExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("commercialPotential", SkolkovoLM.commercePotentialExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("commercialPotentialComment", SkolkovoLM.commentCommercePotentialExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("implement", SkolkovoLM.canBeImplementedExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("implementComment", SkolkovoLM.commentCanBeImplementedExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("expertise", SkolkovoLM.haveExpertiseExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("expertiseComment", SkolkovoLM.commentHaveExpertiseExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("internationalExperience", SkolkovoLM.internationalExperienceExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("internationalExperienceComment", SkolkovoLM.commentInternationalExperienceExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("enoughDocuments", SkolkovoLM.enoughDocumentsExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("enoughDocumentsComment", SkolkovoLM.commentEnoughDocumentsExpertVote.getExpr(session.modifier, expertExpr, voteExpr));

                q.properties.put("vResult", SkolkovoLM.voteResultExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("openedVote", SkolkovoLM.openedVote.getExpr(session.modifier, voteExpr));
                q.properties.put("date", SkolkovoLM.dateExpertVote.getExpr(session.modifier, expertExpr, voteExpr));
                q.properties.put("voteStartDate", SkolkovoLM.dateStartVote.getExpr(session.modifier, voteExpr));
                q.properties.put("voteEndDate", SkolkovoLM.dateEndVote.getExpr(session.modifier, voteExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values =
                        q.execute(session.sql, new OrderedMap<String, Boolean>(Collections.singletonList("voteStartDate"), false));
                profileInfo.voteInfos = new VoteInfo[values.size()];

                int i = 0;
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    Map<String, Object> propValues = entry.getValue();

                    VoteInfo voteInfo = new VoteInfo();

                    int voteId = (Integer)entry.getKey().get("vote");
                    voteInfo.voteId = voteId;
                    voteInfo.linkHash = BaseUtils.encode(voteId, expertId);
                    voteInfo.projectName = (String) propValues.get("projectName");
                    voteInfo.projectClaimer = (String) propValues.get("projectClaimer");
                    voteInfo.projectCluster = (String) propValues.get("projectCluster");
                    voteInfo.inCluster = nvl((Boolean) propValues.get("inCluster"), false);
                    voteInfo.innovative = nvl((Boolean) propValues.get("innovative"), false);
                    voteInfo.innovativeComment = (String) propValues.get("innovativeComment");
                    voteInfo.foreign = nvl((Boolean) propValues.get("foreign"), false);
                    voteInfo.competent = nvl((Integer) propValues.get("competent"), 1);
                    voteInfo.complete = nvl((Integer) propValues.get("complete"), 1);
                    voteInfo.completeComment = (String) propValues.get("completeComment");

                    voteInfo.competitiveAdvantages = nvl((Boolean) propValues.get("competitive"), false);
                    voteInfo.competitiveAdvantagesComment = (String) propValues.get("competitiveComment");
                    voteInfo.commercePotential = nvl((Boolean) propValues.get("commercialPotential"), false);
                    voteInfo.commercePotentialComment = (String) propValues.get("commercialPotentialComment");
                    voteInfo.implement = nvl((Boolean) propValues.get("implement"), false);
                    voteInfo.implementComment = (String) propValues.get("implementComment");
                    voteInfo.expertise = nvl((Boolean) propValues.get("expertise"), false);
                    voteInfo.expertiseComment = (String) propValues.get("expertiseComment");
                    voteInfo.internationalExperience = nvl((Boolean) propValues.get("internationalExperience"), false);
                    voteInfo.internationalExperienceComment = (String) propValues.get("internationalExperienceComment");
                    voteInfo.enoughDocuments = nvl((Boolean) propValues.get("enoughDocuments"), false);
                    voteInfo.enoughDocumentsComment = (String) propValues.get("enoughDocumentsComment");

                    Integer vResult = (Integer) propValues.get("vResult");
                    if (vResult != null) {
                        voteInfo.voteResult = SkolkovoLM.voteResult.getSID(vResult);
                    }

                    voteInfo.voteDone = voteInfo.voteResult != null
                                        || !nvl((Boolean) propValues.get("openedVote"), false);
                    voteInfo.date = DateConverter.sqlToDate((java.sql.Date)propValues.get("date"));
                    voteInfo.voteStartDate = DateConverter.sqlToDate((java.sql.Date)propValues.get("voteStartDate"));
                    voteInfo.voteEndDate = DateConverter.sqlToDate((java.sql.Date)propValues.get("voteEndDate"));

                    profileInfo.voteInfos[i++] = correctVoteInfo(voteInfo);
                }

                profileInfo.scientific = SkolkovoLM.isScientificExpert.read(session, expertObj) != null;
                profileInfo.technical = SkolkovoLM.isTechnicalExpert.read(session, expertObj) != null;
                profileInfo.business = SkolkovoLM.isBusinessExpert.read(session, expertObj) != null;

                profileInfo.commentScientific = (String) SkolkovoLM.commentScientificExpert.read(session, expertObj);
                profileInfo.commentTechnical = (String) SkolkovoLM.commentTechnicalExpert.read(session, expertObj);
                profileInfo.commentBusiness = (String) SkolkovoLM.commentBusinessExpert.read(session, expertObj);

                profileInfo.expertise = SkolkovoLM.expertiseExpert.read(session, expertObj) != null;
                profileInfo.grant = SkolkovoLM.grantExpert.read(session, expertObj) != null;

                keys = KeyExpr.getMapKeys(asList("exp", "foresight"));
                expertExpr = keys.get("exp");
                Expr foresightExpr = keys.get("foresight");

                q = new Query<String, String>(keys);
                q.and(SkolkovoLM.clusterInExpertForesight.getExpr(session.modifier, expertExpr, foresightExpr).getWhere());
                q.and(LM.userLogin.getExpr(session.modifier, expertExpr).compare(new DataObject(expertLogin), Compare.EQUALS));

                q.properties.put("id", foresightExpr);
                q.properties.put("sID", SkolkovoLM.sidForesight.getExpr(session.modifier, foresightExpr));
                q.properties.put("nameCluster", isForeign ? SkolkovoLM.nameForeignClusterForesight.getExpr(session.modifier, foresightExpr) : SkolkovoLM.nameNativeClusterForesight.getExpr(session.modifier, foresightExpr));
                q.properties.put("name", isForeign ? SkolkovoLM.nameForeign.getExpr(session.modifier, foresightExpr) : SkolkovoLM.nameNative.getExpr(session.modifier, foresightExpr));
                q.properties.put("selected", SkolkovoLM.inExpertForesight.getExpr(session.modifier, expertExpr, foresightExpr));
                q.properties.put("comment", SkolkovoLM.commentExpertForesight.getExpr(session.modifier, expertExpr, foresightExpr));

                values = q.execute(session.sql, new OrderedMap<String, Boolean>(Arrays.asList("nameCluster", "id"), false));
                profileInfo.foresightInfos = new ForesightInfo[values.size()];

                i = 0;
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    Map<String, Object> propValues = entry.getValue();

                    ForesightInfo foresightInfo = new ForesightInfo();

                    foresightInfo.sID = ((String) propValues.get("sID")).trim();
                    foresightInfo.nameCluster = ((String) propValues.get("nameCluster")).trim();
                    foresightInfo.name = ((String) propValues.get("name")).trim();

                    foresightInfo.selected = propValues.get("selected") != null;
                    foresightInfo.comment = (String) propValues.get("comment");

                    profileInfo.foresightInfos[i++] = foresightInfo;
                }

                return profileInfo;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о профиле эксперта", e);
        }
    }

    public void setProfileInfo(String expertLogin, ProfileInfo profileInfo) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) LM.loginToUser.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }
                DataObject expertObj = new DataObject(expertId, SkolkovoLM.expert);

                SkolkovoLM.isScientificExpert.execute(profileInfo.scientific, session, expertObj);
                SkolkovoLM.isTechnicalExpert.execute(profileInfo.technical, session, expertObj);
                SkolkovoLM.isBusinessExpert.execute(profileInfo.business, session, expertObj);

                SkolkovoLM.commentScientificExpert.execute(profileInfo.commentScientific, session, expertObj);
                SkolkovoLM.commentTechnicalExpert.execute(profileInfo.commentTechnical, session, expertObj);
                SkolkovoLM.commentBusinessExpert.execute(profileInfo.commentBusiness, session, expertObj);

                SkolkovoLM.expertiseExpert.execute(profileInfo.expertise, session, expertObj);
                SkolkovoLM.grantExpert.execute(profileInfo.grant, session, expertObj);
                SkolkovoLM.profileUpdateDateExpert.execute(DateConverter.dateToStamp(new Date()), session, expertObj);

                for (ForesightInfo foresightInfo : profileInfo.foresightInfos) {
                    DataObject foresightObj = (DataObject) SkolkovoLM.foresightSID.readClasses(session, new DataObject(foresightInfo.sID, (ConcreteClass)SkolkovoLM.sidForesight.getResultClass()));
                    SkolkovoLM.inExpertForesight.execute(foresightInfo.selected, session, expertObj, foresightObj);
                    SkolkovoLM.commentExpertForesight.execute(foresightInfo.comment, session, expertObj, foresightObj);
                }

                String result = session.apply(this);
                if (result != null) {
                    throw new RuntimeException("Не удалось сохранить информацию о профиле эксперта : " + result);
                }
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при записи информации о профиле эксперта", e);
        }
    }

    public void sentVoteDocuments(String expertLogin, int voteId) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) LM.loginToUser.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }

                SkolkovoLM.allowedEmailLetterExpertVote.execute(true, session, new DataObject(expertId,  SkolkovoLM.expert), session.getDataObject(voteId, ObjectType.instance));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при попытке выслать документы.", e);
        }
    }

    @Override
    public void remindPassword(String email) throws RemoteException {
        assert email != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) SkolkovoLM.emailToExpert.read(session, new DataObject(email));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с e-mail: " + email);
                }

                LM.emailUserPassUser.execute(true, session, new DataObject(expertId, LM.customUser));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при попытке выслать напомиание пароля.", e);
        }
    }

    private class VoteObjects {

        DataObject expertObj;
        DataObject voteObj;
        DataObject projectObj;

        private VoteObjects(DataSession session, String svoteId) throws SQLException {

            Integer[] ids = BaseUtils.decode(2, svoteId);

            Integer voteId = ids[0], expertId = ids[1];
//            Integer expertId = (Integer) expertLogin.read(session, new DataObject(login, StringClass.get(30)));
//            if (expertId == null) {
//                throw new RuntimeException("Не удалось найти пользователя с логином " + login);
//            }

            voteObj = session.getDataObject(voteId, ObjectType.instance);

            Integer projectId = (Integer) SkolkovoLM.projectVote.read(session, voteObj);
            if (projectId == null) {
                throw new RuntimeException("Не удалось найти проект для заседания с идентификатором " + voteId);
            }

            expertObj = new DataObject(expertId, SkolkovoLM.expert);

            Boolean inVote = (Boolean) SkolkovoLM.inNewExpertVote.read(session, expertObj, voteObj);
            if (inVote == null || !inVote) {
                throw new RuntimeException("Эксперт с идентификатором " + expertId + " не назначен на заседание с идентификатором " + voteId);
            }

            projectObj = new DataObject(projectId, SkolkovoLM.project);
        }
    }

    public String getDisplayName() throws RemoteException {
        return "Skolkovo Project Support System";
    }

    @Override
    public byte[] getMainIcon() throws RemoteException {
        InputStream in = SkolkovoBusinessLogics.class.getResourceAsStream("/images/sk_icon.png");
        try {
            try {
                return IOUtils.readBytesFromStream(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            logger.error("Не могу прочитать icon-файл.", e);
            return null;
        }
    }

    @Override
    public byte[] getLogo() throws RemoteException {
        InputStream in = SkolkovoBusinessLogics.class.getResourceAsStream("/images/sk_logo.png");
        try {
            try {
                return IOUtils.readBytesFromStream(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            logger.error("Не могу прочитать splash-картинку.", e);
            return null;
        }
    }
}
