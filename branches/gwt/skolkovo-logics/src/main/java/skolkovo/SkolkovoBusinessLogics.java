package skolkovo;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.auth.PolicyManager;
import platform.server.auth.User;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.session.DataSession;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.gwt.shared.VoteInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
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
        SkolkovoLM = new SkolkovoLogicsModule(LM);
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

    public VoteInfo getVoteInfo(String voteId) throws RemoteException {

        try {
            DataSession session = createSession();
            try {
                VoteObjects vo = new VoteObjects(session, voteId);

                VoteInfo voteInfo = new VoteInfo();
                voteInfo.expertName = (String) LM.name.read(session, vo.expertObj);

                Boolean isForeign = (Boolean) SkolkovoLM.isForeignExpert.read(session, vo.expertObj);
                if (isForeign == null) {
                    voteInfo.projectName = (String) SkolkovoLM.nameNative.read(session, vo.projectObj);
                    voteInfo.projectClaimer = (String) SkolkovoLM.nameNativeClaimerProject.read(session, vo.projectObj);
                    voteInfo.projectCluster = (String) SkolkovoLM.nameNativeClusterProject.read(session, vo.projectObj);
                } else {
                    voteInfo.projectName = (String) SkolkovoLM.nameForeign.read(session, vo.projectObj);
                    voteInfo.projectClaimer = (String) SkolkovoLM.nameForeignClaimerProject.read(session, vo.projectObj);
                    voteInfo.projectCluster = (String) SkolkovoLM.nameForeignClusterProject.read(session, vo.projectObj);
                }

                voteInfo.inCluster = nvl((Boolean) SkolkovoLM.inClusterExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.innovative = nvl((Boolean) SkolkovoLM.innovativeExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.innovativeComment = (String) SkolkovoLM.innovativeCommentExpertVote.read(session, vo.expertObj, vo.voteObj);
                voteInfo.foreign = nvl((Boolean) SkolkovoLM.foreignExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.competent = nvl((Integer) SkolkovoLM.competentExpertVote.read(session, vo.expertObj, vo.voteObj), 1);
                voteInfo.complete = nvl((Integer) SkolkovoLM.completeExpertVote.read(session, vo.expertObj, vo.voteObj), 1);
                voteInfo.completeComment = (String) SkolkovoLM.completeCommentExpertVote.read(session, vo.expertObj, vo.voteObj);

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

    public ProfileInfo getProfileInfo(String expertLogin) throws RemoteException {
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
                boolean isForeign = SkolkovoLM.isForeignExpert.read(session, expertObj) != null;

                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("exp", "vote"));
                Expr expExpr = keys.get("exp");
                Expr voteExpr = keys.get("vote");
                Expr projExpr = SkolkovoLM.projectVote.getExpr(session.modifier, voteExpr);

                Query<String, String> q = new Query<String, String>(keys);
                q.and(SkolkovoLM.inNewExpertVote.getExpr(session.modifier, expExpr, voteExpr).getWhere());
                q.and(LM.userLogin.getExpr(session.modifier, expExpr).compare(new DataObject(expertLogin), Compare.EQUALS));

                q.properties.put("projectId", projExpr);
                q.properties.put("projectName", (isForeign ? SkolkovoLM.nameForeign : SkolkovoLM.nameNative).getExpr(session.modifier, projExpr));
                q.properties.put("projectClaimer", (isForeign ? SkolkovoLM.nameForeignClaimerProject : SkolkovoLM.nameNativeClaimerProject).getExpr(session.modifier, projExpr));
                q.properties.put("projectCluster", (isForeign ? SkolkovoLM.nameForeignClusterProject : SkolkovoLM.nameNativeClusterProject).getExpr(session.modifier, projExpr));

                q.properties.put("inCluster", SkolkovoLM.inClusterExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("innovative", SkolkovoLM.innovativeExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("innovativeComment", SkolkovoLM.innovativeCommentExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("foreign", SkolkovoLM.foreignExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("competent", SkolkovoLM.competentExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("complete", SkolkovoLM.completeExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("completeComment", SkolkovoLM.completeCommentExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("vResult", SkolkovoLM.voteResultExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("openedVote", SkolkovoLM.openedVote.getExpr(session.modifier, voteExpr));
                q.properties.put("date", SkolkovoLM.dateExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("voteStartDate", SkolkovoLM.dateStartVote.getExpr(session.modifier, voteExpr));
                q.properties.put("voteEndDate", SkolkovoLM.dateEndVote.getExpr(session.modifier, voteExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);
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

                return profileInfo;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о профиле эксперта", e);
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

                SkolkovoLM.allowedEmailLetterExpertVote.execute(true, session, new DataObject(expertId,  SkolkovoLM.expert), new DataObject(voteId, SkolkovoLM.vote));
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

            voteObj = new DataObject(voteId, SkolkovoLM.vote);

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
