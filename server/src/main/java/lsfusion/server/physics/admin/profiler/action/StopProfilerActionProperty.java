package lsfusion.server.physics.admin.profiler.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
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
import lsfusion.interop.ProgressBar;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.base.controller.stack.StackProgress;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.join.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.physics.admin.profiler.ProfileItem;
import lsfusion.server.physics.admin.profiler.ProfileObject;
import lsfusion.server.physics.admin.profiler.ProfileValue;
import lsfusion.server.physics.admin.profiler.Profiler;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static lsfusion.server.physics.admin.profiler.Profiler.profileData;

public class StopProfilerActionProperty extends ScriptingAction {
    private LP totalTime;
    private LP totalSQLTime;
    private LP totalUserInteractionTime;
    private LP callCount;
    private LP minTime;
    private LP maxTime;
    private LP squaresSum;

    private LA<?> writeProfilerBatch;

    private String basePOStaticCaption;

    public StopProfilerActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);

        totalTime = findProperty("totalTime[TEXT, TEXT, INTEGER, VARSTRING[100]]");
        totalSQLTime = findProperty("totalSQLTime[TEXT, TEXT, INTEGER, VARSTRING[100]]");
        totalUserInteractionTime = findProperty("totalUserInteractionTime[TEXT, TEXT, INTEGER, VARSTRING[100]]");
        callCount = findProperty("callCount[TEXT, TEXT, INTEGER, VARSTRING[100]]");
        minTime = findProperty("minTime[TEXT, TEXT, INTEGER, VARSTRING[100]]");
        maxTime = findProperty("maxTime[TEXT, TEXT, INTEGER, VARSTRING[100]]");
        squaresSum = findProperty("squaresSum[TEXT, TEXT, INTEGER, VARSTRING[100]]");
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Profiler.PROFILER_ENABLED = false;

        try {
            ConcreteCustomClass profileObject = (ConcreteCustomClass) findClass("ProfileObject");
            DataObject baseProfileObject = profileObject.getDataObject("top");
            basePOStaticCaption = ((String) LM.baseLM.staticCaption.read(context, baseProfileObject)).trim();
            
            writeProfilerBatch = findAction("writeProfilerBatch[]");

            updateProfileData(context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
        Profiler.clearData();
    }

    private void updateProfileData(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        final ImOrderSet<KeyField> keys = SetFact.fromJavaOrderSet(new ArrayList<>(Arrays.asList(new KeyField("po1", StringClass.text),
                new KeyField("po2", StringClass.text), new KeyField("user", LongClass.instance), new KeyField("form", StringClass.getv(100)))));

        ImOrderSet<LP> props = SetFact.fromJavaOrderSet(new ArrayList<>(Arrays.asList(totalTime, totalSQLTime, 
                totalUserInteractionTime, callCount, minTime, maxTime, squaresSum)));

        MMap<ImMap<KeyField, DataObject>, ProfileValue> mPremap = newPremap();
        GetValue<ImMap<LP, ObjectValue>, ProfileValue> mapProfileValue = new GetValue<ImMap<LP, ObjectValue>, ProfileValue>() {
            @Override
            public ImMap<LP, ObjectValue> getMapValue(ProfileValue profileValue) {
                return MapFact.toMap(
                        totalTime, (ObjectValue) new DataObject(profileValue.totalTime, LongClass.instance),
                        totalSQLTime, new DataObject(profileValue.totalSQLTime, LongClass.instance),
                        totalUserInteractionTime, new DataObject(profileValue.totalUserInteractionTime, LongClass.instance),
                        callCount, new DataObject(profileValue.callCount, LongClass.instance),
                        minTime, new DataObject(profileValue.minTime, LongClass.instance),
                        maxTime, new DataObject(profileValue.maxTime, LongClass.instance),
                        squaresSum, new DataObject(profileValue.squaresSum, DoubleClass.instance)
                );
            }
        };

        int batchCounter = 0;
        int batchSize = Settings.get().getProfilerBatchSize();
        int batchQuantity = (int) Math.ceil((double) profileData.size() / batchSize);
        int batchNumber = 1;

        for (Map.Entry<ProfileItem, ProfileValue> entry : profileData.entrySet()) {
            ProfileItem profileItem = entry.getKey();

            ImList<DataObject> orderValues = ListFact.toList(
                    new DataObject(getProfileObjectText(profileItem.profileObject), (DataClass)keys.get(0).type),
                    new DataObject(getProfileObjectText(profileItem.upperProfileObject), (DataClass)keys.get(1).type),
                    new DataObject(profileItem.userID, (DataClass)keys.get(2).type),
                    new DataObject(profileItem.form == null ? "" : profileItem.form.getSID(), (DataClass)keys.get(3).type));
            mPremap.add(keys.mapList(orderValues), entry.getValue());

            batchCounter++;

            if (batchCounter == batchSize) {
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
    private void writeBatch(final ImOrderSet<KeyField> keys, ImOrderSet<LP> props, ImMap<ImMap<KeyField, DataObject>, ImMap<LP, ObjectValue>> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        SessionTableUsage<KeyField, LP> importTable = new SessionTableUsage<>("stopprof", keys, props, new Type.Getter<KeyField>() {
            @Override
            public Type getType(KeyField key) {
                return key.type;
            }
        }, new Type.Getter<LP>() {
            @Override
            public Type getType(LP key) {
                return ((LP<?>)key).property.getType();
            }
        });

        DataSession session = context.getSession();
        OperationOwner owner = session.getOwner();
        SQLSession sql = session.sql;

        importTable.writeRows(sql, data, owner);

        final ImRevMap<KeyField, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LP> importJoin = importTable.join(mapKeys);
        try {
            for (LP<?> lcp : importTable.getValues()) {
                PropertyChange propChange = new PropertyChange<>(lcp.listInterfaces.mapSet(keys).join(mapKeys), importJoin.getExpr(lcp), importJoin.getWhere());
                context.getEnv().change((Property) lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }

        writeProfilerBatch.execute(context);
    }

    private String getProfileObjectText(ProfileObject po) {
        return BaseUtils.substring(po == null ? basePOStaticCaption : po.getProfileString(), 1000);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
