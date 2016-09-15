package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.base.ProgressBar;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.*;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.profiler.ProfileItem;
import lsfusion.server.profiler.ProfileObject;
import lsfusion.server.profiler.ProfileValue;
import lsfusion.server.profiler.Profiler;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SessionTableUsage;
import lsfusion.server.stack.StackProgress;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static lsfusion.server.profiler.Profiler.profileData;

public class StopProfilerActionProperty extends ScriptingActionProperty {
    @SuppressWarnings("FieldCanBeLocal")
    private static int BATCH_SIZE = 500;

    private ConcreteCustomClass profileObject;
    private ValueClass user;
    private ConcreteCustomClass form;

    private LCP totalTime;
    private LCP totalSQLTime;
    private LCP totalUserInteractionTime;
    private LCP callCount;
    private LCP minTime;
    private LCP maxTime;
    private LCP squaresSum;

    private LCP text;

    private LCP formCanonicalName;

    private LAP<?> writeProfilerBatch;

    private String basePOStaticCaption;
    private DataObject noForm;

    public StopProfilerActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);

        profileObject = (ConcreteCustomClass) findClass("ProfileObject");
        user = findClass("User");
        form = (ConcreteCustomClass) findClass("ProfileForm");

        text = findProperty("text[ProfileObject]");

        totalTime = findProperty("totalTime[TEXT, TEXT, User, Form]");
        totalSQLTime = findProperty("totalSQLTime[TEXT, TEXT, User, Form]");
        totalUserInteractionTime = findProperty("totalUserInteractionTime[TEXT, TEXT, User, Form]");
        callCount = findProperty("callCount[TEXT, TEXT, User, Form]");
        minTime = findProperty("minTime[TEXT, TEXT, User, Form]");
        maxTime = findProperty("maxTime[TEXT, TEXT, User, Form]");
        squaresSum = findProperty("squaresSum[TEXT, TEXT, User, Form]");

        formCanonicalName = findProperty("navigatorElementCanonicalName[VARSTRING[100]]");
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject baseProfileObject = profileObject.getDataObject("top");
        basePOStaticCaption = (String) LM.baseLM.staticCaption.read(context, baseProfileObject);
        text.change(getProfileObjectText(null), context.getSession(), baseProfileObject);
        noForm = form.getDataObject("noForm");

        Profiler.PROFILER_ENABLED = false;

        try {
            writeProfilerBatch = findAction("writeProfilerBatch");

            updateProfileData(context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
        Profiler.clearData();
    }

    private void updateProfileData(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        DataSession session = context.getSession();

        final ImOrderSet<KeyField> keys = SetFact.fromJavaOrderSet(new ArrayList<>(Arrays.asList(new KeyField("po1", StringClass.text),
                new KeyField("po2", StringClass.text), new KeyField("user", user.getType()), new KeyField("form", form.getType()))));

        ImOrderSet<LCP> props = SetFact.fromJavaOrderSet(new ArrayList<>(Arrays.asList(totalTime, totalSQLTime, 
                totalUserInteractionTime, callCount, minTime, maxTime, squaresSum)));

        MMap<ImMap<KeyField, DataObject>, ProfileValue> mPremap = newPremap();
        GetValue<ImMap<LCP, ObjectValue>, ProfileValue> mapProfileValue = new GetValue<ImMap<LCP, ObjectValue>, ProfileValue>() {
            @Override
            public ImMap<LCP, ObjectValue> getMapValue(ProfileValue profileValue) {
                return MapFact.toMap(totalTime, (ObjectValue) new DataObject(profileValue.totalTime, LongClass.instance),
                        totalSQLTime, new DataObject(profileValue.totalSQLTime, LongClass.instance),
                        totalUserInteractionTime, new DataObject(profileValue.totalUserInteractionTime, LongClass.instance),
                        callCount, new DataObject(profileValue.callCount, LongClass.instance),
                        minTime, new DataObject(profileValue.minTime, LongClass.instance),
                        maxTime, new DataObject(profileValue.maxTime, LongClass.instance),
                        squaresSum, new DataObject(profileValue.squaresSum, DoubleClass.instance));
            }
        };

        int batchCounter = 0;
        int batchQuantity = (int) Math.ceil((double) profileData.size() / BATCH_SIZE);
        int batchNumber = 1;

        for (Map.Entry<ProfileItem, ProfileValue> entry : profileData.entrySet()) {
            ProfileItem profileItem = entry.getKey();

            ImList<DataObject> orderValues = ListFact.toList(new DataObject(getProfileObjectText(profileItem.profileObject)),
                    new DataObject(getProfileObjectText(profileItem.upperProfileObject)),
                    session.getDataObject(user, profileItem.userID),
                    getFormObject(profileItem, session));
            mPremap.add(keys.mapList(orderValues), entry.getValue());

            batchCounter++;

            if (batchCounter == BATCH_SIZE) {
                writeBatch(keys, props, mPremap.immutable().mapValues(mapProfileValue), context, new ProgressBar("Profiler", batchNumber, batchQuantity));
                batchNumber++;
                mPremap = newPremap();
                batchCounter = 0;
            }
        }
        if (batchCounter > 0) {
            writeBatch(keys, props, mPremap.immutable().mapValues(mapProfileValue), context, new ProgressBar("Profiler", batchNumber, batchQuantity));
        }
    }

    private MMap<ImMap<KeyField, DataObject>, ProfileValue> newPremap() {
        return MapFact.mMap(new SymmAddValue<ImMap<KeyField, DataObject>, ProfileValue>() {
            @Override
            public ProfileValue addValue(ImMap<KeyField, DataObject> key, ProfileValue prevValue, ProfileValue newValue) {
                return ProfileValue.merge(prevValue, newValue);
            }
        });
    }

    @StackProgress
    private void writeBatch(final ImOrderSet<KeyField> keys, ImOrderSet<LCP> props, ImMap<ImMap<KeyField, DataObject>, ImMap<LCP, ObjectValue>> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        SessionTableUsage<KeyField, LCP> importTable = new SessionTableUsage<>(keys, props, new Type.Getter<KeyField>() {
            @Override
            public Type getType(KeyField key) {
                return key.type;
            }
        }, new Type.Getter<LCP>() {
            @Override
            public Type getType(LCP key) {
                return key.property.getType();
            }
        });

        DataSession session = context.getSession();
        OperationOwner owner = session.getOwner();
        SQLSession sql = session.sql;

        importTable.writeRows(sql, data, owner);

        final ImRevMap<KeyField, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LCP> importJoin = importTable.join(mapKeys);
        try {
            for (LCP<?> lcp : importTable.getValues()) {
                PropertyChange propChange = new PropertyChange<>(lcp.listInterfaces.mapSet(keys).join(mapKeys), importJoin.getExpr(lcp), importJoin.getWhere());
                context.getEnv().change((CalcProperty) lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }

        writeProfilerBatch.execute(context);
    }

    private String getProfileObjectText(ProfileObject po) {
        return po == null ? basePOStaticCaption : po.getProfileString();
    }

    private DataObject getFormObject(ProfileItem profileItem, DataSession session) throws SQLException, SQLHandledException {
        if (profileItem.form == null) {
            return noForm;
        } else {
            Object formName = formCanonicalName.read(session, new DataObject(profileItem.form.getSID()));
            return formName == null ? noForm : session.getDataObject(form, formName);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
