package skolkovo;

import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.IOUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.interop.exceptions.RemoteMessageException;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.data.type.ObjectType;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.DataObject;
import platform.server.remote.RemoteLogics;
import platform.server.session.DataSession;
import skolkovo.api.gwt.shared.ForesightInfo;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static platform.base.BaseUtils.nullTrim;
import static platform.base.BaseUtils.nvl;

public class SkolkovoRemoteLogics extends RemoteLogics<SkolkovoBusinessLogics> implements SkolkovoRemoteInterface {

    private static final String SKOLKOVO_DISPLAY_NAME = "Skolkovo Project Support System";
    private SkolkovoLogicsModule SkolkovoLM;

    protected SkolkovoRemoteLogics() {
        setDisplayName(SKOLKOVO_DISPLAY_NAME);
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (LifecycleEvent.INIT.equals(event.getType())) {
            this.SkolkovoLM = businessLogics.SkolkovoLM;
        }
    }

    @Override
    public byte[] getMainIcon() throws RemoteException {
        try {
            return IOUtils.readBytesFromResource("/images/sk_icon.png");
        } catch (IOException e) {
            logger.error("Не могу прочитать icon-файл.", e);
            return null;
        }
    }

    @Override
    public byte[] getLogo() throws RemoteException {
        try {
            return IOUtils.readBytesFromResource("/images/sk_logo.png");
        } catch (IOException e) {
            logger.error("Не могу прочитать splash-картинку.", e);
            return null;
        }
    }

    public VoteInfo getVoteInfo(String voteId, String locale) throws RemoteException {

        try {
            DataSession session = createSession();
            try {
                VoteObjects vo = new VoteObjects(session, voteId);

                VoteInfo voteInfo = new VoteInfo();
                voteInfo.expertName = (String) baseLM.name.read(session, vo.expertObj);

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

                voteInfo.date = DateConverter.sqlToDate((java.sql.Date) SkolkovoLM.dateExpertVote.read(session, vo.expertObj, vo.voteObj));
                voteInfo.expertIP = (String) SkolkovoLM.ipExpertVote.read(session, vo.expertObj, vo.voteObj);

                return correctVoteInfo(voteInfo);
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteMessageException("Ошибка при считывании информации о проекте", e);
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
        vi.expertIP = nullTrim(vi.expertIP);

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

                SkolkovoLM.dateExpertVote.change(DateConverter.getCurrentDate(), session, vo.expertObj, vo.voteObj);
                SkolkovoLM.voteResultExpertVote.change(SkolkovoLM.voteResult.getID(voteInfo.voteResult), session, vo.expertObj, vo.voteObj);
                SkolkovoLM.ipExpertVote.change(voteInfo.expertIP, session, vo.expertObj, vo.voteObj);
                if (voteInfo.voteResult.equals("voted")) {
                    SkolkovoLM.inClusterExpertVote.change(voteInfo.inCluster, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.innovativeExpertVote.change(voteInfo.innovative, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.innovativeCommentExpertVote.change(voteInfo.innovativeComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.foreignExpertVote.change(voteInfo.foreign, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.competentExpertVote.change(voteInfo.competent, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.completeExpertVote.change(voteInfo.complete, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.completeCommentExpertVote.change(voteInfo.completeComment, session, vo.expertObj, vo.voteObj);

                    SkolkovoLM.competitiveAdvantagesExpertVote.change(voteInfo.competitiveAdvantages, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentCompetitiveAdvantagesExpertVote.change(voteInfo.competitiveAdvantagesComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commercePotentialExpertVote.change(voteInfo.commercePotential, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentCommercePotentialExpertVote.change(voteInfo.commercePotentialComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.canBeImplementedExpertVote.change(voteInfo.implement, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentCanBeImplementedExpertVote.change(voteInfo.implementComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.haveExpertiseExpertVote.change(voteInfo.expertise, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentHaveExpertiseExpertVote.change(voteInfo.expertiseComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.internationalExperienceExpertVote.change(voteInfo.internationalExperience, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentInternationalExperienceExpertVote.change(voteInfo.internationalExperienceComment, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.enoughDocumentsExpertVote.change(voteInfo.enoughDocuments, session, vo.expertObj, vo.voteObj);
                    SkolkovoLM.commentEnoughDocumentsExpertVote.change(voteInfo.enoughDocumentsComment, session, vo.expertObj, vo.voteObj);
                }

                String result = session.applyMessage(businessLogics);
                if (result != null) {
                    throw new RuntimeException("Не удалось сохранить информацию о голосовании : " + result);
                }
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteMessageException("Ошибка при записи информации о голосовании", e);
        }
    }

    public ProfileInfo getProfileInfo(String expertLogin, String locale) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) businessLogics.authenticationLM.customUserLogin.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }
                DataObject expertObj = new DataObject(expertId, SkolkovoLM.expert);

                ProfileInfo profileInfo = new ProfileInfo();

                profileInfo.expertName = (String) baseLM.name.read(session, expertObj);
                profileInfo.expertEmail = (String) businessLogics.contactLM.emailContact.read(session, expertObj);

                boolean isForeign;
                if (locale != null)
                    isForeign = "en".equals(locale);
                else
                    isForeign = SkolkovoLM.isForeignExpert.read(session, expertObj) != null;

                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.toExclSet("exp", "vote"));
                Expr expertExpr = keys.get("exp");
                Expr voteExpr = keys.get("vote");
                Expr projExpr = SkolkovoLM.projectVote.getExpr(session.getModifier(), voteExpr);

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(SkolkovoLM.inNewExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr).getWhere());
                q.and(businessLogics.authenticationLM.loginCustomUser.getExpr(session.getModifier(), expertExpr).compare(new DataObject(expertLogin), Compare.EQUALS));

                q.addProperty("projectId", projExpr);
                q.addProperty("projectName", (isForeign ? SkolkovoLM.nameForeignProject : SkolkovoLM.nameNativeProject).getExpr(session.getModifier(), projExpr));
                q.addProperty("projectClaimer", (isForeign ? SkolkovoLM.nameForeignClaimerProject : SkolkovoLM.nameNativeClaimerProject).getExpr(session.getModifier(), projExpr));
                q.addProperty("projectCluster", (isForeign ? SkolkovoLM.nameForeignClusterExpert : SkolkovoLM.nameNativeClusterExpert).getExpr(session.getModifier(), expertExpr));

                q.addProperty("inCluster", SkolkovoLM.inClusterExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("innovative", SkolkovoLM.innovativeExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("innovativeComment", SkolkovoLM.innovativeCommentExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("foreign", SkolkovoLM.foreignExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("competent", SkolkovoLM.competentExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("complete", SkolkovoLM.completeExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("completeComment", SkolkovoLM.completeCommentExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));

                q.addProperty("competitive", SkolkovoLM.competitiveAdvantagesExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("competitiveComment", SkolkovoLM.commentCompetitiveAdvantagesExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("commercialPotential", SkolkovoLM.commercePotentialExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("commercialPotentialComment", SkolkovoLM.commentCommercePotentialExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("implement", SkolkovoLM.canBeImplementedExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("implementComment", SkolkovoLM.commentCanBeImplementedExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("expertise", SkolkovoLM.haveExpertiseExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("expertiseComment", SkolkovoLM.commentHaveExpertiseExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("internationalExperience", SkolkovoLM.internationalExperienceExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("internationalExperienceComment", SkolkovoLM.commentInternationalExperienceExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("enoughDocuments", SkolkovoLM.enoughDocumentsExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("enoughDocumentsComment", SkolkovoLM.commentEnoughDocumentsExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));

                q.addProperty("vResult", SkolkovoLM.voteResultExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("openedVote", SkolkovoLM.openedVote.getExpr(session.getModifier(), voteExpr));
                q.addProperty("date", SkolkovoLM.dateExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));
                q.addProperty("voteStartDate", SkolkovoLM.dateStartVote.getExpr(session.getModifier(), voteExpr));
                q.addProperty("voteEndDate", SkolkovoLM.dateEndVote.getExpr(session.getModifier(), voteExpr));
                q.addProperty("expertIP", SkolkovoLM.ipExpertVote.getExpr(session.getModifier(), expertExpr, voteExpr));

                ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values =
                        q.execute(session.sql, MapFact.singletonOrder("voteStartDate", false));
                profileInfo.voteInfos = new VoteInfo[values.size()];

                for (int i=0,size=values.size();i<size;i++) {
                    ImMap<String, Object> propValues = values.getValue(i);

                    VoteInfo voteInfo = new VoteInfo();

                    int voteId = (Integer)values.getKey(i).get("vote");
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
                    voteInfo.expertIP = (String) propValues.get("expertIP");

                    profileInfo.voteInfos[i] = correctVoteInfo(voteInfo);
                }

                profileInfo.scientific = SkolkovoLM.isScientificExpert.read(session, expertObj) != null;
                profileInfo.technical = SkolkovoLM.isTechnicalExpert.read(session, expertObj) != null;
                profileInfo.business = SkolkovoLM.isBusinessExpert.read(session, expertObj) != null;

                profileInfo.commentScientific = (String) SkolkovoLM.commentScientificExpert.read(session, expertObj);
                profileInfo.commentTechnical = (String) SkolkovoLM.commentTechnicalExpert.read(session, expertObj);
                profileInfo.commentBusiness = (String) SkolkovoLM.commentBusinessExpert.read(session, expertObj);

                profileInfo.expertise = SkolkovoLM.expertiseExpert.read(session, expertObj) != null;
                profileInfo.grant = SkolkovoLM.grantExpert.read(session, expertObj) != null;

                keys = KeyExpr.getMapKeys(SetFact.toExclSet("exp", "foresight"));
                expertExpr = keys.get("exp");
                Expr foresightExpr = keys.get("foresight");

                q = new QueryBuilder<String, String>(keys);
                q.and(SkolkovoLM.clusterInExpertForesight.getExpr(session.getModifier(), expertExpr, foresightExpr).getWhere());
                q.and(businessLogics.authenticationLM.loginCustomUser.getExpr(session.getModifier(), expertExpr).compare(new DataObject(expertLogin), Compare.EQUALS));

                q.addProperty("id", foresightExpr);
                q.addProperty("sID", SkolkovoLM.sidForesight.getExpr(session.getModifier(), foresightExpr));
                q.addProperty("nameCluster", isForeign ? SkolkovoLM.nameForeignClusterForesight.getExpr(session.getModifier(), foresightExpr) : SkolkovoLM.nameNativeClusterForesight.getExpr(session.getModifier(), foresightExpr));
                q.addProperty("name", isForeign ? SkolkovoLM.nameForeign.getExpr(session.getModifier(), foresightExpr) : SkolkovoLM.nameNative.getExpr(session.getModifier(), foresightExpr));
                q.addProperty("selected", SkolkovoLM.inExpertForesight.getExpr(session.getModifier(), expertExpr, foresightExpr));
                q.addProperty("comment", SkolkovoLM.commentExpertForesight.getExpr(session.getModifier(), expertExpr, foresightExpr));

                values = q.execute(session.sql, SetFact.toOrderExclSet("nameCluster", "id").toOrderMap(false));
                profileInfo.foresightInfos = new ForesightInfo[values.size()];

                for (int i=0,size=values.size();i<size;i++) {
                    ImMap<String, Object> propValues = values.getValue(i);

                    ForesightInfo foresightInfo = new ForesightInfo();

                    foresightInfo.sID = BaseUtils.nullTrim((String) propValues.get("sID"));
                    foresightInfo.nameCluster = BaseUtils.nullTrim((String) propValues.get("nameCluster"));
                    foresightInfo.name = BaseUtils.nullTrim((String) propValues.get("name"));

                    foresightInfo.selected = propValues.get("selected") != null;
                    foresightInfo.comment = (String) propValues.get("comment");

                    profileInfo.foresightInfos[i] = foresightInfo;
                }

                return profileInfo;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteMessageException("Ошибка при считывании информации о профиле эксперта", e);
        }
    }

    @Override
    protected List<String> getExtraUserRoleNames(String username) {
        List<String> extraRoles = new ArrayList<String>();
        try {
            DataSession session = createSession();
            if (baseLM.is(SkolkovoLM.expert).read(session, session.getDataObject(businessLogics.authenticationLM.customUserLogin.read(session, new DataObject(username)), businessLogics.authenticationLM.customUser.getType())) != null) {
                extraRoles.add("expert");
            }
            session.close();
        } catch (SQLException e) {
            throw new RuntimeException();
        }

        return extraRoles;
    }

    public void setProfileInfo(String expertLogin, ProfileInfo profileInfo) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) businessLogics.authenticationLM.customUserLogin.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }
                DataObject expertObj = new DataObject(expertId, SkolkovoLM.expert);

                SkolkovoLM.isScientificExpert.change(profileInfo.scientific, session, expertObj);
                SkolkovoLM.isTechnicalExpert.change(profileInfo.technical, session, expertObj);
                SkolkovoLM.isBusinessExpert.change(profileInfo.business, session, expertObj);

                SkolkovoLM.commentScientificExpert.change(profileInfo.commentScientific, session, expertObj);
                SkolkovoLM.commentTechnicalExpert.change(profileInfo.commentTechnical, session, expertObj);
                SkolkovoLM.commentBusinessExpert.change(profileInfo.commentBusiness, session, expertObj);

                SkolkovoLM.expertiseExpert.change(profileInfo.expertise, session, expertObj);
                SkolkovoLM.grantExpert.change(profileInfo.grant, session, expertObj);
                SkolkovoLM.profileUpdateDateExpert.change(DateConverter.dateToStamp(new Date()), session, expertObj);

                for (ForesightInfo foresightInfo : profileInfo.foresightInfos) {
                    DataObject foresightObj = (DataObject) SkolkovoLM.foresightSID.readClasses(session, new DataObject(foresightInfo.sID, (ConcreteClass) SkolkovoLM.sidForesight.property.getValueClass()));
                    SkolkovoLM.inExpertForesight.change(foresightInfo.selected, session, expertObj, foresightObj);
                    SkolkovoLM.commentExpertForesight.change(foresightInfo.comment, session, expertObj, foresightObj);
                }

                String result = session.applyMessage(businessLogics);
                if (result != null) {
                    throw new RuntimeException("Не удалось сохранить информацию о профиле эксперта : " + result);
                }
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteMessageException("Ошибка при записи информации о профиле эксперта", e);
        }
    }

    public void sentVoteDocuments(String expertLogin, int voteId) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) businessLogics.authenticationLM.customUserLogin.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }

                SkolkovoLM.allowedEmailLetterExpertVote.execute(session, new DataObject(expertId, SkolkovoLM.expert), session.getDataObject(voteId, ObjectType.instance));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteMessageException("Ошибка при попытке выслать документы.", e);
        }
    }

    @Override
    protected Integer getUserByEmail(DataSession session, String email) throws SQLException {
        return (Integer) SkolkovoLM.emailToExpert.read(session, new DataObject(email));
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

    public void setConfResult(String conferenceHash, boolean result) throws RemoteException {
        Integer[] ids = BaseUtils.decode(2, conferenceHash);

        try {
            DataSession session = createSession();
            try {
                DataObject confObj = session.getDataObject(ids[0], ObjectType.instance);
                DataObject expertObj = session.getDataObject(ids[1], ObjectType.instance);
                if(result)
                    SkolkovoLM.confirmedConferenceExpert.change(true, session, confObj, expertObj);
                else
                    SkolkovoLM.rejectedConferenceExpert.change(true, session, confObj, expertObj);
                String apply = session.applyMessage(businessLogics);
                if (apply != null) {
                    throw new RuntimeException("Не удалось сохранить информацию о участии : " + apply);
                }
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteMessageException("Ошибка при записи результата", e);
        }
    }
}
