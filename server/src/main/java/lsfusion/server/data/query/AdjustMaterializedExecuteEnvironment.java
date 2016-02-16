package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.stat.ExecCost;
import lsfusion.server.data.type.ParseInterface;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class AdjustMaterializedExecuteEnvironment extends DynamicExecuteEnvironment<ImMap<SQLQuery, MaterializedQuery>, AdjustMaterializedExecuteEnvironment.Snapshot> {

    private Step current;

    private static class SubQueryContext {
        private final SQLQuery query;

        public SubQueryContext(SQLQuery query) {
            this.query = query;
        }

        public String toString() {
            return query.hashCode() +  " " + query.toString();
        }

        public void toStrings(MList<String> mList) {
            mList.add(toString());
        }
    }

    private static class SubQueryUpContext extends SubQueryContext {
        private final Step step;
        private final AdjustMaterializedExecuteEnvironment env;

        public SubQueryUpContext(Step step, SQLQuery query, AdjustMaterializedExecuteEnvironment env) {
            super(query);
            this.step = step;
            this.env = env;
        }

        @Override
        public void toStrings(MList<String> mList) {
            env.toStrings(mList);
            mList.add(step + " " + toString());
        }
    }

    private final SubQueryContext context;

    private AdjustMaterializedExecuteEnvironment(SubQueryContext context) {
        this.context = context;
    }

    public AdjustMaterializedExecuteEnvironment(SQLQuery query) {
        this(new SubQueryContext(query));
    }

    public AdjustMaterializedExecuteEnvironment(SubQueryUpContext upContext) {
        this((SubQueryContext)upContext);
    }

    public TypeExecuteEnvironment getType() {
        return TypeExecuteEnvironment.MATERIALIZE;
    }

    private boolean assertNoRecheckBefore() {
        Step previous = current.previous;
        while (previous != null) {
            ServerLoggers.assertLog(!previous.recheck, "NO RECHECK");
            previous = previous.previous;
        }
        return true;
    }

    private ImSet<SQLQuery> outerQueries; // для assertion'а

    private boolean assertSameMaterialized(SQLCommand command, Step previousStep, ImSet<SQLQuery> materialized) {
        ImSet<SQLQuery> queries;
        if(previousStep == null)
            queries = SetFact.EMPTY();
        else
            queries = previousStep.getMaterializedQueries();
        ServerLoggers.assertLog(BaseUtils.hashEquals(SetFact.addExclSet(outerQueries, queries), materialized), "SHOULD ALWAYS BE EQUAL");
        return true;
    }

    @Override
    public synchronized Snapshot getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<ImMap<SQLQuery, MaterializedQuery>, Snapshot> outerEnv) {
        Step previousStep;
        Snapshot snapshot = outerEnv.getSnapshot();
        if(snapshot == null) // "верхний"
            previousStep = null;
        else
            previousStep = snapshot.step;

        ImMap<SQLQuery, MaterializedQuery> materializedQueries = outerEnv.getOuter();
        if(materializedQueries == null)
            materializedQueries = MapFact.EMPTY();

        if(current == null) { // first start
            current = new Step(null, SetFact.<SQLQuery>EMPTYORDER(), false);
            outerQueries = materializedQueries.keys();
            current.setTimeout(getDefaultTimeout(command, materializedQueries));
        }

        ServerLoggers.assertLog(current.recheck, "RECHECK");
        assertNoRecheckBefore(); // assert что предыдущие не recheck

        Step nextStep = current;
        if(previousStep != null && nextStep.getIndex() < previousStep.getIndex()) // если current "вернулся назад", просто идем вперед, назад никогда смысла идти нет
            nextStep = getNextStep(previousStep, command, snapshot);
        return new Snapshot(nextStep, previousStep, command, transactTimeout, materializedQueries);
    }

    public synchronized void succeeded(SQLCommand command, Snapshot snapshot, long runtime, DynamicExecEnvOuter<ImMap<SQLQuery, MaterializedQuery>, Snapshot> outerEnv) {
        if(snapshot.noHandled || snapshot.isTransactTimeout)
            return;

        Step step = snapshot.step;

        int stepTimeout = step.getTimeout();
        if(stepTimeout != 0 && runtime / 1000 > stepTimeout && stepTimeout < snapshot.secondsFromTransactStart) { // если на самом деле больше timeout, то по сути можно считать failed
            innerFailed(command, snapshot, " [SUCCEEDED - RUN : " + (runtime / 1000) + ", STEP : " + stepTimeout + ", TRANSACT : " + snapshot.secondsFromTransactStart + "]");
            return;
        }

        // пытаемся вернуться назад
        long totalTime = runtime;
        int coeff = Settings.get().getLastStepCoeff();
        Step runBack = step;
        assertNoRecheckBefore();
        MOrderExclSet<Step> mBackSteps = SetFact.mOrderExclSet();
        while (true) {
            totalTime += runBack.getMaterializeTime(snapshot.getMaterializedQueries()); // достаточно inner

            runBack = runBack.previous;
            if(runBack == null)
                break;

            long adjtime = totalTime / 1000; // millis -> seconds
            if(adjtime > runBack.getTimeout() * coeff) { // если время выполнения превысило время материализации, пробуем с увеличенным timeout'ом
                runBack.setTimeout((int) adjtime);
                mBackSteps.exclAdd(runBack);
                runBack.recheck = true;
                current = runBack;
            }
        }

        boolean failedBefore = outerEnv.getSnapshot() != null; // не очень красиво конечно
        ImOrderSet<Step> backSteps = mBackSteps.immutableOrder();
        if(failedBefore || !backSteps.isEmpty())
            log("SUCCEEDED" + (failedBefore ? " (AFTER FAILURE)" : "" ) + " TIME (" + (runtime / 1000) + " OF " + snapshot.setTimeout + ")" + (backSteps.isEmpty() ? "" : " - BACK"), step, backSteps);
    }

    public void innerFailed(SQLCommand command, Snapshot snapshot, String outerMessage) {
        // если current до, ничего не трогаем и так идет recheck
        // если current после, тоже ничего не трогаем, current и так уже подвинут
        if(BaseUtils.hashEquals(current, snapshot.step)) { // подвигаем current до следующего recheck'а
            current.recheck = false; // при succeded'е он и так сбросится, так что на всякий случай
            Step nextStep = getNextStep(current, command, snapshot);
            log("FAILED TIMEOUT (" + snapshot.setTimeout + ")" + outerMessage + " - NEXT", current, SetFact.singletonOrder(nextStep));
            current = nextStep;
        }
    }
    public synchronized void failed(SQLCommand command, Snapshot snapshot) {
        assert !snapshot.noHandled;
        if(snapshot.isTransactTimeout)
            return;
        innerFailed(command, snapshot, "");
    }

    private void toStrings(MList<String> mList) {
        context.toStrings(mList);
    }

    private void log(String message, Step baseStep, ImOrderSet<Step> changeTo) {
        // step + его timeout
        MList<String> mList = ListFact.mList();
        toStrings(mList);
        mList.add(message + (changeTo.isEmpty() ? "" : " FROM") + " : " + baseStep);
        for(Step change : changeTo)
            mList.add(" TO : " + change);
        ServerLoggers.adjustLog(mList.immutableList(), true);
    }

    private static Step getNextStep(Step current, SQLCommand command, Snapshot snapshot) {
        assert snapshot.step == current;
        ImMap<SQLQuery, MaterializedQuery> materializedQueries = snapshot.getMaterializedQueries();
        int coeff = Settings.get().getLastStepCoeff();
        while (true) {
            Step next;
            if(current.isLastStep()) { // идем назад, но этому шагу и себе увеличиваем timeout
                next = current.previous;
                next.setTimeout(next.getTimeout() * coeff);
                next.recheck = true;
                current.setTimeout(current.getTimeout() * coeff);
                current.recheck = true;
            } else {
                next = current.next;
                if (next == null) {
                    next = createNextStep(command, current, materializedQueries.keys());
                    assert next.recheck;
                    current.next = next;
                }
            }
            current = next;
            if (current.recheck) {
                current.setTimeout(BaseUtils.max(current.getTimeout(), getDefaultTimeout(command, materializedQueries))); // adjust'м timeout в любом случае
                return current;
            }
        }
    }

    private static class Node {
        private SQLQuery query;
        private Node parent;

        public Node(SQLQuery query, Node parent, SQLCommand command) {
            this.query = query;
            this.parent = parent;

            size = command.getCost(MapFact.<SQLQuery, Stat>EMPTY()).rows.getWeight();
            hasTooLongKeys = query != null && SQLQuery.hasTooLongKeys(query.keyReaders);

            if(parent != null) {
                parent.children.add(this);
            }
        }

        private int degree = 1;
        private final int size;
        private final boolean hasTooLongKeys;
        private Set<Node> children = new HashSet<Node>();
    }

    private static void recCreateNode(SQLCommand<?> command, Node parent, ImSet<SQLQuery> materializedQueries, Set<Node> nodes) {
        for(SQLQuery subQuery : command.subQueries.values()) {
            if(!materializedQueries.contains(subQuery)) {
                Node subNode = new Node(subQuery, parent, subQuery);
                recCreateNode(subQuery, subNode, materializedQueries, nodes);
                nodes.add(subNode);
                if(parent != null)
                    parent.degree += subNode.degree;
            }
        }
    }

    private static void recRemoveChildren(Node node, PriorityQueue<Node> queue) {
        queue.remove(node);
        for(Node child : node.children)
            recRemoveChildren(child, queue);
    }

    private static Step createNextStep(SQLCommand command, Step step, ImSet<SQLQuery> outerQueries) {
        // есть n вершин (с учетом materialized), берем корень n-й степени
        // наша цель найти поддеревья с максимально близкой степенью к этой величине, материализовать ее
        // ищем вершину с максимально подходящей степенью, включаем в результат, берем следующую и т.д. пока не останется пустое дерево

        Set<Node> nodes = new HashSet<>();
        ServerLoggers.assertLog(outerQueries.containsAll(step.getMaterializedQueries()), "SHOULD CONTAIN ALL"); // может включать еще "верхние"
        final Node topNode = new Node(null, null, command);
        recCreateNode(command, topNode, outerQueries, nodes); // не важно inner или нет
        if(!nodes.isEmpty()) { // оптимизация
            nodes.add(topNode);

            int split = Settings.get().getSubQueriesSplit();
            final int threshold = new Stat(Settings.get().getSubQueriesRowsThreshold()).getWeight();
            final int max = new Stat(Settings.get().getSubQueriesRowsMax()).getWeight();
            final int coeff = Settings.get().getSubQueriesRowCountCoeff();

            final int target = (int) Math.round(((double)nodes.size()) / split);

            Comparator<Node> comparator = new Comparator<Node>() {
                private int getPriority(Node o) {
                    if(o == topNode) {
                        if(o.degree > target) // если больше target по сути запретим выбирать
                            return Integer.MAX_VALUE / 2;
                    } else {
                        if (o.size >= max || o.hasTooLongKeys) // если больше порога не выбираем вообще
                            return Integer.MAX_VALUE;
                    }

                    return BaseUtils.max(o.size, threshold) * coeff + Math.abs(o.degree - target);
                }
                public int compare(Node o1, Node o2) {
                    return Integer.compare(getPriority(o1), getPriority(o2));
                }
            };

            PriorityQueue<Node> priority = new PriorityQueue<Node>(nodes.size(), comparator);
            priority.addAll(nodes);

            MOrderSet<SQLQuery> mNextQueries = SetFact.mOrderSet();
            while(true) {
                Node bestNode = priority.poll();

                recRemoveChildren(bestNode, priority); // удаляем поддерево

                if(bestNode.query == null) {
                    assert bestNode == topNode;
                    break;
                }

                mNextQueries.add(bestNode.query);

                // пересчитываем degree
                Node parentNode = bestNode.parent;
                while(parentNode != null) {
                    priority.remove(parentNode); // важно сначала удалить, так как degree используется в компараторе
                    parentNode.degree -= bestNode.degree;
                    priority.add(parentNode);

                    parentNode = parentNode.parent;
                }
            }
            final ImOrderSet<SQLQuery> nextQueries = mNextQueries.immutableOrder();

            if(!nextQueries.isEmpty())
                return new Step(nextQueries, step);
        }

        return new Step(true, step); // включение disableNestedLoop
    }

    private static int getDefaultTimeout(SQLCommand command, ImMap<SQLQuery, MaterializedQuery> queries) {
        ExecCost baseCost = command.getCost(queries.mapValues(new GetValue<Stat, MaterializedQuery>() {
            public Stat getMapValue(MaterializedQuery value) {
                return new Stat(value.count);
            }
        }));
        return baseCost.getDefaultTimeout();
    }

    private static void drop(MaterializedQuery query, SQLSession session, OperationOwner owner) throws SQLException {
        session.lockedReturnTemporaryTable(query.tableName, query.owner, owner);
    }

    private class SubQueryEnv {
        private final MAddExclMap<SQLQuery, DynamicExecuteEnvironment> map = MapFact.mAddExclMap();

        public synchronized DynamicExecuteEnvironment get(SQLQuery subQuery, Step step) {
            DynamicExecuteEnvironment subEnv = map.get(subQuery);
            if(subEnv == null) {
                subEnv = new AdjustMaterializedExecuteEnvironment(new SubQueryUpContext(step, subQuery, AdjustMaterializedExecuteEnvironment.this));
                map.exclAdd(subQuery, subEnv);
            }
            return subEnv;
        }
    }
    private final SubQueryEnv subQueryEnv = new SubQueryEnv();

    // Mutable, inner, not Thread safe
    private static class Step {
        private Step next; // более пессимистичный
        private final Step previous; // более оптимистичный

        private final ImOrderSet<SQLQuery> subQueries; // одновременно является кэшем, чтобы несколько раз не считать
        private final boolean disableNestedLoop;

        public boolean isLastStep() {
            return disableNestedLoop;
        }

        private int timeout; // время за которое запрос обязан выполнится иначе его нет смысла выполнять
        public boolean recheck; // mutable часть - есть вероятность (а точнее не было случая что он не выполнился) что он выполнится за timeout

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        private Step(Step previous, ImOrderSet<SQLQuery> subQueries, boolean disableNestedLoop) {
            recheck = true;
            this.previous = previous;
            this.subQueries = subQueries;
            this.disableNestedLoop = disableNestedLoop;
        }

        public Step(ImOrderSet<SQLQuery> subQueries, Step previous) {
            this(previous, subQueries, false);
        }

        public Step(boolean disableNestedLoop, Step previous) {
            this(previous, SetFact.<SQLQuery>EMPTYORDER(), disableNestedLoop);
            assert disableNestedLoop;
            assert !previous.disableNestedLoop; // именно из-за того что у такого step'а MAX timeout
            timeout = 0;
        }

        public Step() {
            this(null, SetFact.<SQLQuery>EMPTYORDER(), false);
        }

        public int getIndex() {
            return previous == null ? 0 : (previous.getIndex() + (disableNestedLoop ? 0 : 1));
        }

        @Override
        public String toString() {
            return "step : " + getIndex() + " timeout : " + timeout + " " + (disableNestedLoop ? "NONESTED" : "");
        }

        public long getMaterializeTime(ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
            long result = 0;
            for(SQLQuery subQuery : subQueries)
                result += materializedQueries.get(subQuery).timeExec;
            return result;
        }

        public boolean fillSubQueries(MOrderExclSet<SQLQuery> mSubQueries, Step previousStep) {
            if(previousStep != null && equals(previousStep)) // нашли
                return true;
            if((previous != null && previous.fillSubQueries(mSubQueries, previousStep)) || (previous == null && previousStep == null)) {
                mSubQueries.exclAddAll(subQueries);
                return true;
            }
            return false;
        }

        private ImSet<SQLQuery> getMaterializedQueries() {
            MOrderExclSet<SQLQuery> mSubQueries = SetFact.mOrderExclSet();
            fillSubQueries(mSubQueries, null);
            return mSubQueries.immutableOrder().getSet();
        }

    }

    public class Snapshot implements DynamicExecEnvSnapshot<ImMap<SQLQuery, MaterializedQuery>, Snapshot> {
        // immutable часть
        public final Step step;
        public final ImOrderSet<SQLQuery> queries;

        // состояние сессии (точнее потока + сессии), есть assertion что не изменяются вплоть до окончания выполнения
        public final ImMap<SQLQuery, MaterializedQuery> materializedOuterQueries; // param
        public final int transactTimeout; // param

        public boolean noHandled; // ThreadLocal
        public boolean inTransaction; // LockWrite
        public int secondsFromTransactStart; // LockWrite

        // рассчитанное локальное состояние
        public ImMap<SQLQuery, MaterializedQuery> materializedQueries;
        public boolean isTransactTimeout = false;
        public boolean needConnectionLock;
        public boolean disableNestedLoop;
        public int setTimeout;

        public Snapshot getSnapshot() {
            return this;
        }

        public Snapshot(Step step, Step previousStep, SQLCommand command, int transactTimeout, ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
            this.step = step;

            MOrderExclSet<SQLQuery> mSubQueries = SetFact.mOrderExclSet();
            boolean found = step.fillSubQueries(mSubQueries, previousStep); // бежим от nextStep назад пока не встретим previous, определяем queries которые надо материализовать
//            ServerLoggers.assertLog(found, "SHOULD HIT PREVIOUS");
            this.queries = mSubQueries.immutableOrder();

            assertSameMaterialized(command, previousStep, materializedQueries.keys()); // assert'им что previousStep все queries + outer совпадают с materialized
            this.materializedOuterQueries = materializedQueries;

            ServerLoggers.assertLog(!queries.getSet().intersect(materializedOuterQueries.keys()), "SHOULD NOT INTERSECT"); // если быть точным queries должны быть строго выше materialized

            this.transactTimeout = transactTimeout;
        }

        // forAnalyze
        private boolean forAnalyze;
        public Snapshot(Step step, ImOrderSet<SQLQuery> queries, ImMap<SQLQuery, MaterializedQuery> materializedOuterQueries, int transactTimeout, ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
            this.step = step;
            this.queries = queries;
            this.materializedOuterQueries = materializedOuterQueries;
            this.transactTimeout = transactTimeout;
            this.materializedQueries = materializedQueries;
            this.setTimeout = 0;
            this.forAnalyze = true;
        }

        public Snapshot forAnalyze() {
            assert materializedQueries != null;
            assert !forAnalyze;

            return new Snapshot(step, queries, materializedOuterQueries, transactTimeout, materializedQueries);
        }

        public void beforeOuter(SQLCommand command, SQLSession session, ImMap<String, ParseInterface> paramObjects, OperationOwner owner, PureTimeInterface pureTime) throws SQLException, SQLHandledException {
            MExclMap<SQLQuery, MaterializedQuery> mMaterializedQueries = MapFact.mExclMapMax(queries.size());
            try {
                for (int i = 0, size = queries.size(); i < size; i++) {
                    SQLQuery query = queries.get(i);
                    ImMap<SQLQuery, MaterializedQuery> copyMaterializedQueries = MapFact.addExcl(mMaterializedQueries.immutableCopy(), materializedOuterQueries);
                    MaterializedQuery materialized = query.materialize(session, subQueryEnv.get(query, step), owner, copyMaterializedQueries, paramObjects, transactTimeout);
                    mMaterializedQueries.exclAdd(query, materialized);
                    pureTime.add(materialized.timeExec);
                }
            } finally {
                materializedQueries = mMaterializedQueries.immutable();
            }
        }

        public ImMap<SQLQuery, MaterializedQuery> getMaterializedQueries() {
            return materializedQueries.addExcl(materializedOuterQueries);
        }

        public ImMap<SQLQuery, MaterializedQuery> getOuter() {
            return getMaterializedQueries();
        }

        public void afterOuter(SQLSession session, OperationOwner owner) throws SQLException {
            for(MaterializedQuery matQuery : materializedQueries.valueIt())
                drop(matQuery, session, owner);
        }

        // assert что session.locked
        private void prepareEnv(SQLSession session) { // "смешивает" универсальное состояние (при отсуствии ограничений) и "местное", DynamicExecuteEnvironment.checkSnapshot выполняет обратную функцию
            noHandled = forAnalyze || session.isNoHandled();
            if(noHandled)
                return;

            inTransaction = session.isInTransaction();
            setTimeout = step.getTimeout();

            if(session.isInTransaction() && session.syntax.hasTransactionSavepointProblem() && !Settings.get().isUseSavepointsForExceptions()) { // если нет savepoint'ов увеличиваем до времени с начала транзакции
                secondsFromTransactStart = session.getSecondsFromTransactStart();
                if (setTimeout > 0) // если есть транзакция, увеличиваем timeout до времени транзакции
                    setTimeout = BaseUtils.max(setTimeout, secondsFromTransactStart);
            }

            if(session.syntax.supportsDisableNestedLoop()) {
                disableNestedLoop = step.disableNestedLoop;
//                sessionVolatileStats = session.isVolatileStats();
//                if (sessionVolatileStats) { // проверяем локальный volatileStats
//                    disableNestedLoop = true;
//                    setTimeout = 0; // выключаем timeout
//                }
            }

            // уменьшаем timeout до локального максимума
            if(inTransaction && !session.isNoTransactTimeout() && transactTimeout > 0 && (setTimeout >= transactTimeout || setTimeout == 0)) {
                setTimeout = transactTimeout;
                isTransactTimeout = true;
            }

            needConnectionLock = disableNestedLoop || (setTimeout > 0 && session.syntax.hasJDBCTimeoutMultiThreadProblem()); // проверка на timeout из-за бага в драйвере postgresql
        }

        // после readLock сессии, но до получения connection'а
        public void beforeConnection(SQLSession session, OperationOwner owner) throws SQLException {
            prepareEnv(session);

            if(needConnectionLock)
                session.lockNeedPrivate();
        }

        public void afterConnection(SQLSession session, OperationOwner owner) throws SQLException {
            if(needConnectionLock)
                session.lockTryCommon(owner);
        }

        public boolean isTransactTimeout() {
            return isTransactTimeout;
        }

        public boolean needConnectionLock() {
            return needConnectionLock;
        }

        public void beforeStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
            if(disableNestedLoop) {
                assert needConnectionLock; // чтобы запрещать connection должен быть заблокирован
                sqlSession.setEnableNestLoop(connection, owner, false);
            }
        }

        public void afterStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
            if(disableNestedLoop) {
                assert needConnectionLock;
                sqlSession.setEnableNestLoop(connection, owner, true);
            }
        }

        public void beforeExec(Statement statement, SQLSession session) throws SQLException {
            if(setTimeout > 0)
                statement.setQueryTimeout(setTimeout);
        }

        public boolean hasRepeatCommand() {
            return setTimeout > 0;
        }
    }

}
