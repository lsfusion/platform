package lsfusion.server;

import lsfusion.base.ApiResourceBundle;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.logics.ServerResourceBundle;

public class Settings {

    public static Settings get() {
        return ThreadLocalContext.getSettings();
    }

    private int innerGroupExprs = 0; // использовать Subquery Expressions

    public int getInnerGroupExprs() {
        return innerGroupExprs;
    }

    public void setInnerGroupExprs(int innerGroupExprs) {
        this.innerGroupExprs = innerGroupExprs;
    }

    public int packOnCacheComplexity = 300000;

    public int getPackOnCacheComplexity() {
        return packOnCacheComplexity;
    }

    public void setPackOnCacheComplexity(int packOnCacheComplexity) {
        this.packOnCacheComplexity = packOnCacheComplexity;
    }

    private int LRUOftenCleanPeriod = 2;
    private int LRUOftenExpireSecond = 5;
    private int LRUOftenProceedBucket = 10000;
    private int LRURareCleanPeriod = 20;
    private int LRURareExpireSecond = 3600;
    private int LRURareProceedBucket = 10000;

    public int getLRUOftenCleanPeriod() {
        return LRUOftenCleanPeriod;
    }

    public void setLRUOftenCleanPeriod(int LRUOftenCleanPeriod) {
        this.LRUOftenCleanPeriod = LRUOftenCleanPeriod;
    }

    public int getLRUOftenExpireSecond() {
        return LRUOftenExpireSecond;
    }

    public void setLRUOftenExpireSecond(int LRUOftenExpireSecond) {
        this.LRUOftenExpireSecond = LRUOftenExpireSecond;
    }

    public int getLRUOftenProceedBucket() {
        return LRUOftenProceedBucket;
    }

    public void setLRUOftenProceedBucket(int LRUOftenProceedBucket) {
        this.LRUOftenProceedBucket = LRUOftenProceedBucket;
    }

    public int getLRURareCleanPeriod() {
        return LRURareCleanPeriod;
    }

    public void setLRURareCleanPeriod(int LRURareCleanPeriod) {
        this.LRURareCleanPeriod = LRURareCleanPeriod;
    }

    public int getLRURareExpireSecond() {
        return LRURareExpireSecond;
    }

    public void setLRURareExpireSecond(int LRURareExpireSecond) {
        this.LRURareExpireSecond = LRURareExpireSecond;
    }

    public int getLRURareProceedBucket() {
        return LRURareProceedBucket;
    }

    public void setLRURareProceedBucket(int LRURareProceedBucket) {
        this.LRURareProceedBucket = LRURareProceedBucket;
    }

    private boolean cacheInnerHashes = true;

    public boolean isCacheInnerHashes() {
        return cacheInnerHashes;
    }

    public void setCacheInnerHashes(boolean cacheInnerHashes) {
        this.cacheInnerHashes = cacheInnerHashes;
    }

    private int mapInnerMaxIterations = 24;

    public int getMapInnerMaxIterations() {
        return mapInnerMaxIterations;
    }

    public void setMapInnerMaxIterations(int mapInnerMaxIterations) {
        this.mapInnerMaxIterations = mapInnerMaxIterations;
    }

    // обозначает что если компилятор видет включающие join'ы (J1, J2, ... Jk) (J1,...J2, ... Jk,.. Jn) он будет выполнять все в первом подмножестве, предполагая что возникающий OR разберет SQL сервер что мягко говоря не так
    private boolean compileMeans = true;

    // обозначает что компилятор будет проталкивать внутрь order подзапросов, общее условие
    private boolean pushOrderWhere = true;

    // обозначает что при проверке условия на TRUE не будет преобразовывать A cmp B в 3 противоположных NOT'а как правильно, а будет использовать эвристику
    private boolean simpleCheckCompare = true;

    // обозначает что на следствия (и отрицания) условия будет проверять когда остались только термы, не делая этого на промежуточных уровнях
    private boolean checkFollowsWhenObjects = false;

    // будет ли оптимизатор пытаться перестраивать условия по правилу X OR (Y AND Z) и X=>Y, то Y AND (X OR Z)
    private boolean restructWhereOnMeans = false;

    // будет ли оптимизатор разбивать группирующие выражения, чтобы не было FULL JOIN и UNION ALL
    private boolean splitSelectGroupInnerJoins = false;

    // будет ли оптимизатор разбивать inner join'ы по статистике
    private boolean splitGroupStatInnerJoins = false;

    // будет ли компилятор вместо UNION (когда UNION ALL не удается построить) использовать FULL JOIN
    boolean useFJInsteadOfUnion = false;

    // будет ли оптимизатор разбивать группирующие выражения на максимум, так чтобы в группируемом выражении не было бы Case'ов
    private boolean splitGroupSelectExprcases = false;

    // будет ли высчитываться что именно изменилось в группирующих свойствах или же будет считаться что изменилось все
    private boolean calculateGroupDataChanged = false;

    // не использовать инкрементную логику в группирующем свойстве на максимум
    private boolean noIncrementMaxGroupProperty = true;

    // использовать применение изменений "по одному"
    private boolean enableApplySingleStored = true;

    private boolean editLogicalOnSingleClick = false;
    private boolean editActionOnSingleClick = false;

    public boolean isEnabledApplySingleStored() {
        return enableApplySingleStored;
    }

    public void setEnableApplySingleStored(boolean enableApplySingleStored) {
        this.enableApplySingleStored = enableApplySingleStored;
    }

    public boolean isPushOrderWhere() {
        return pushOrderWhere;
    }

    public boolean isSplitSelectGroupInnerJoins() {
        return splitSelectGroupInnerJoins;
    }

    public void setSplitSelectGroupInnerJoins(boolean splitSelectGroupInnerJoins) {
        this.splitSelectGroupInnerJoins = splitSelectGroupInnerJoins;
    }

    public boolean isSplitGroupStatInnerJoins() {
        return splitGroupStatInnerJoins;
    }

    public void setSplitGroupStatInnerJoins(boolean splitGroupStatInnerJoins) {
        this.splitGroupStatInnerJoins = splitGroupStatInnerJoins;
    }

    public boolean isUseFJInsteadOfUnion() {
        return useFJInsteadOfUnion;
    }

    public void setUseFJInsteadOfUnion(boolean useFJInsteadOfUnion) {
        this.useFJInsteadOfUnion = useFJInsteadOfUnion;
    }

    public void setPushOrderWhere(boolean pushOrderWhere) {
        this.pushOrderWhere = pushOrderWhere;
    }

    public boolean isSimpleCheckCompare() {
        return simpleCheckCompare;
    }

    public Boolean getEditLogicalOnSingleClick() {
       return editLogicalOnSingleClick;
    }

    public void setEditLogicalOnSingleClick(boolean editLogicalOnSingleClick) {
       this.editLogicalOnSingleClick = editLogicalOnSingleClick;
    }

    public Boolean getEditActionClassOnSingleClick() {
       return editActionOnSingleClick;
    }

    public void setEditActionOnSingleClick(boolean editActionOnSingleClick) {
            this.editActionOnSingleClick = editActionOnSingleClick;

    }

    public void setSimpleCheckCompare(boolean simpleCheckCompare) {
        this.simpleCheckCompare = simpleCheckCompare;
    }

    public boolean isCheckFollowsWhenObjects() {
        return checkFollowsWhenObjects;
    }

    public void setCheckFollowsWhenObjects(boolean checkFollowsWhenObjects) {
        this.checkFollowsWhenObjects = checkFollowsWhenObjects;
    }

    public boolean isRestructWhereOnMeans() {
        return restructWhereOnMeans;
    }

    public void setRestructWhereOnMeans(boolean restructWhereOnMeans) {
        this.restructWhereOnMeans = restructWhereOnMeans;
    }

    public boolean isSplitGroupSelectExprcases() {
        return splitGroupSelectExprcases;
    }

    public void setSplitGroupSelectExprcases(boolean splitGroupSelectExprcases) {
        this.splitGroupSelectExprcases = splitGroupSelectExprcases;
    }

    public boolean isCalculateGroupDataChanged() {
        return calculateGroupDataChanged;
    }

    public void setCalculateGroupDataChanged(boolean calculateGroupDataChanged) {
        this.calculateGroupDataChanged = calculateGroupDataChanged;
    }

    public boolean isNoIncrementMaxGroupProperty() {
        return noIncrementMaxGroupProperty;
    }

    public void setNoIncrementMaxGroupProperty(boolean noIncrementMaxGroupProperty) {
        this.noIncrementMaxGroupProperty = noIncrementMaxGroupProperty;
    }

    public boolean isCompileMeans() {
        return compileMeans;
    }

    public void setCompileMeans(boolean compileMeans) {
        this.compileMeans = compileMeans;
    }

    private int freeConnections = 5;

    public int getFreeConnections() {
        return freeConnections;
    }

    public void setFreeConnections(int freeConnections) {
        this.freeConnections = freeConnections;
    }

    private boolean commonUnique = false;

    public boolean isCommonUnique() {
        return commonUnique;
    }

    public void setCommonUnique(boolean commonUnique) {
        this.commonUnique = commonUnique;
    }

    private boolean disablePoolConnections = false;

    public boolean isDisablePoolConnections() {
        return disablePoolConnections;
    }

    public void setDisablePoolConnections(boolean disablePoolConnections) {
        this.disablePoolConnections = disablePoolConnections;
    }

    private boolean disablePoolPreparedStatements = true;

    public boolean isDisablePoolPreparedStatements() {
        return disablePoolPreparedStatements;
    }

    public void setDisablePoolPreparedStatements(boolean disablePoolPreparedStatements) {
        this.disablePoolPreparedStatements = disablePoolPreparedStatements;
    }

    private boolean disableWhenCalcDo = true;

    public boolean isDisableWhenCalcDo() {
        return disableWhenCalcDo;
    }

    public void setDisableWhenCalcDo(boolean disableWhenCalcDo) {
        this.disableWhenCalcDo = disableWhenCalcDo;
    }

    private boolean disablePrereadValues = false;

    public boolean isDisablePrereadValues() {
        return disablePrereadValues;
    }

    public void setDisablePrereadValues(boolean disablePrereadValues) {
        this.disablePrereadValues = disablePrereadValues;
    }

    private boolean disableSumGroupNotZero = false;

    public boolean isDisableSumGroupNotZero() {
        return disableSumGroupNotZero;
    }

    public void setDisableSumGroupNotZero(boolean disableSumGroupNotZero) {
        this.disableSumGroupNotZero = disableSumGroupNotZero;
    }

    private int usedChangesCacheLimit = 20;

    public int getUsedChangesCacheLimit() {
        return usedChangesCacheLimit;
    }

    public void setUsedChangesCacheLimit(int usedChangesCacheLimit) {
        this.usedChangesCacheLimit = usedChangesCacheLimit;
    }

    private String locale;

    public void setLocale(String locale) {
        this.locale = locale;
        ServerResourceBundle.load(locale);
        ApiResourceBundle.load(locale);
    }

    // максимум сколько свойств вместе будет применяться в базу
    private int splitIncrementApply = 10;

    public int getSplitIncrementApply() {
        return splitIncrementApply;
    }

    public void setSplitIncrementApply(int splitIncrementApply) {
        this.splitIncrementApply = splitIncrementApply;
    }

    private int statDegree = 20;

    public int getStatDegree() {
        return statDegree;
    }

    public void setStatDegree(int statDegree) {
        this.statDegree = statDegree;
    }

    private int barcodeLength = 13;

    public int getBarcodeLength() {
        return barcodeLength;
    }

    public void setBarcodeLength(int barcodeLength) {
        this.barcodeLength = barcodeLength;
    }

    private boolean useUniPass;

    public void setUseUniPass(boolean useUniPass) {
        this.useUniPass = useUniPass;
    }

    public boolean getUseUniPass() {
        return useUniPass;
    }

    private boolean useSingleJoins = false;

    public boolean isUseSingleJoins() {
        return useSingleJoins;
    }

    public void setUseSingleJoins(boolean useSingleJoins) {
        this.useSingleJoins = useSingleJoins;
    }

    private boolean useQueryExpr = true;

    public boolean isUseQueryExpr() {
        return useQueryExpr;
    }

    public void setUseQueryExpr(boolean useQueryExpr) {
        this.useQueryExpr = useQueryExpr;
    }

    private int limitWhereJoinsCount = 20;
    private int limitWhereJoinsComplexity = 200;

    // очень опасная эвристика - может в определенных случаях "потерять ключ", то есть образуется And в котором не хватает KeyExpr'а
    private int limitClassWhereCount = 40;
    private int limitClassWhereComplexity = 4000;

    private int limitWhereJoinPack = 300;

    private boolean noExclusiveCompile = true;
    private int limitExclusiveCount = 7; // когда вообще не пытаться строить exclusive (count)
    private int limitExclusiveComplexity = 100; // когда вообще не пытаться строить exclusive (complexity)
    private int limitExclusiveSimpleCount = 10; // когда строить exclusive без рекурсии (count)
    private int limitExclusiveSimpleComplexity = 100; // когда строить exclusive без рекурсии (complexity)

    private int limitIncrementCoeff = 1;
    private int limitHintIncrementComplexity = 50;
    private int limitGrowthIncrementComplexity = 2;
    private int limitHintIncrementStat = 1000;
    private int limitHintNoUpdateComplexity = 4000;
    private int limitWrapComplexity = 200;

    private int limitMaterializeComplexity = 20;

    public boolean noApplyIncrement = false;
    private int limitApplyHintIncrementComplexity = 100;
    private int limitApplyHintIncrementStat = 1000;

    public boolean isNoExclusiveCompile() {
        return noExclusiveCompile;
    }

    public void setNoExclusiveCompile(boolean noExclusiveCompile) {
        this.noExclusiveCompile = noExclusiveCompile;
    }


    public int getLimitWhereJoinsCount() {
        return limitWhereJoinsCount;
    }
    public void setLimitWhereJoinsCount(int limitWhereJoinsCount) {
        this.limitWhereJoinsCount = limitWhereJoinsCount;
    }
    public int getLimitWhereJoinsComplexity() {
        return limitWhereJoinsComplexity;
    }
    public void setLimitWhereJoinsComplexity(int limitWhereJoinsComplexity) {
        this.limitWhereJoinsComplexity = limitWhereJoinsComplexity;
    }
    public int getLimitClassWhereCount() {
        return limitClassWhereCount;
    }
    public void setLimitClassWhereCount(int limitClassWhereCount) {
        this.limitClassWhereCount = limitClassWhereCount;
    }
    public int getLimitClassWhereComplexity() {
        return limitClassWhereComplexity;
    }
    public void setLimitClassWhereComplexity(int limitClassWhereComplexity) {
        this.limitClassWhereComplexity = limitClassWhereComplexity;
    }
    public int getLimitWhereJoinPack() {
        return limitWhereJoinPack;
    }
    public void setLimitWhereJoinPack(int limitWhereJoinPack) {
        this.limitWhereJoinPack = limitWhereJoinPack;
    }
    public int getLimitHintIncrementComplexity() {
        return limitHintIncrementComplexity * limitIncrementCoeff;
    }
    public void setLimitHintIncrementComplexity(int limitHintIncrementComplexity) {
        this.limitHintIncrementComplexity = limitHintIncrementComplexity;
    }
    public boolean isNoApplyIncrement() {
        return noApplyIncrement;
    }
    public void setNoApplyIncrement(boolean noApplyIncrement) {
        this.noApplyIncrement = noApplyIncrement;
    }
    public int getLimitApplyHintIncrementComplexity() {
        return limitApplyHintIncrementComplexity * limitIncrementCoeff;
    }
    public void setLimitApplyHintIncrementComplexity(int limitApplyHintIncrementComplexity) {
        this.limitApplyHintIncrementComplexity = limitApplyHintIncrementComplexity;
    }
    public int getLimitHintIncrementStat() {
        return limitHintIncrementStat;
    }
    public void setLimitHintIncrementStat(int limitHintIncrementStat) {
        this.limitHintIncrementStat = limitHintIncrementStat;
    }
    public int getLimitApplyHintIncrementStat() {
        return limitApplyHintIncrementStat;
    }
    public void setLimitApplyHintIncrementStat(int limitApplyHintIncrementStat) {
        this.limitApplyHintIncrementStat = limitApplyHintIncrementStat;
    }
    public int getLimitHintNoUpdateComplexity() {
        return limitHintNoUpdateComplexity * limitIncrementCoeff;
    }
    public void setLimitHintNoUpdateComplexity(int limitHintNoUpdateComplexity) {
        this.limitHintNoUpdateComplexity = limitHintNoUpdateComplexity;
    }
    public int getLimitWrapComplexity() {
        return limitWrapComplexity * limitIncrementCoeff;
    }
    public void setLimitWrapComplexity(int limitWrapComplexity) {
        this.limitWrapComplexity = limitWrapComplexity;
    }
    public int getLimitGrowthIncrementComplexity() {
        return limitGrowthIncrementComplexity;
    }
    public void setLimitGrowthIncrementComplexity(int limitGrowthIncrementComplexity) {
        this.limitGrowthIncrementComplexity = limitGrowthIncrementComplexity;
    }
    public int getLimitExclusiveCount() {
        return limitExclusiveCount;
    }
    public void setLimitExclusiveCount(int limitExclusiveCount) {
        this.limitExclusiveCount = limitExclusiveCount;
    }
    public int getLimitExclusiveSimpleCount() {
        return limitExclusiveSimpleCount;
    }
    public void setLimitExclusiveSimpleCount(int limitExclusiveSimpleCount) {
        this.limitExclusiveSimpleCount = limitExclusiveSimpleCount;
    }
    public int getLimitExclusiveSimpleComplexity() {
        return limitExclusiveSimpleComplexity;
    }
    public void setLimitExclusiveSimpleComplexity(int limitExclusiveSimpleComplexity) {
        this.limitExclusiveSimpleComplexity = limitExclusiveSimpleComplexity;
    }
    public int getLimitExclusiveComplexity() {
        return limitExclusiveComplexity;
    }
    public void setLimitExclusiveComplexity(int limitExclusiveComplexity) {
        this.limitExclusiveComplexity = limitExclusiveComplexity;
    }
    public int getLimitMaterializeComplexity() {
        return limitMaterializeComplexity;
    }
    public void setLimitMaterializeComplexity(int limitMaterializeComplexity) {
        this.limitMaterializeComplexity = limitMaterializeComplexity;
    }

    private boolean autoAnalyzeTempStats = true; // автоматически анализировать статистику после каждого заполнения временной таблицы (прикол в том что после удаления таблицы и добавления новых записей статистика сама увеличивается)
    public boolean isAutoAnalyzeTempStats() {
        return autoAnalyzeTempStats;
    }

    public void setAutoAnalyzeTempStats(boolean autoAnalyzeTempStats) {
        this.autoAnalyzeTempStats = autoAnalyzeTempStats;
    }

    private boolean useGreaterEquals = true;

    public boolean isUseGreaterEquals() {
        return useGreaterEquals;
    }

    public void setUseGreaterEquals(boolean useGreaterEquals) {
        this.useGreaterEquals = useGreaterEquals;
    }

    private boolean disableAutoHints = false;

    public boolean isDisableAutoHints() {
        return disableAutoHints;
    }

    public void setDisableAutoHints(boolean disableAutoHints) {
        this.disableAutoHints = disableAutoHints;
    }

    private boolean disableAutoHintCaches = true;

    public boolean isDisableAutoHintCaches() {
        return disableAutoHintCaches;
    }

    public void setDisableAutoHintCaches(boolean disableAutoHintCaches) {
        this.disableAutoHintCaches = disableAutoHintCaches;
    }

    private boolean disableWrapComplexity = true;

    public boolean isDisableWrapComplexity() {
        return disableWrapComplexity;
    }

    public void setDisableWrapComplexity(boolean disableWrapComplexity) {
        this.disableWrapComplexity = disableWrapComplexity;
    }

    private boolean enablePrevWrapComplexity = false;

    public boolean isEnablePrevWrapComplexity() {
        return enablePrevWrapComplexity;
    }

    public void setEnablePrevWrapComplexity(boolean enablePrevWrapComplexity) {
        this.enablePrevWrapComplexity = enablePrevWrapComplexity;
    }

    public boolean applyVolatileStats = false;

    public boolean isApplyVolatileStats() {
        return applyVolatileStats;
    }

    public void setApplyVolatileStats(boolean applyVolatileStats) {
        this.applyVolatileStats = applyVolatileStats;
    }

    // если prev идет в value, то использовать то значение которое есть сейчас после singleapply,
    // а не высчитывать на начало транзакции потому как все равно "временнОй" целостности не будет
    private boolean useEventValuePrevHeuristic = true;

    public void setUseEventValuePrevHeuristic(boolean useEventValuePrevHeuristic) {
        this.useEventValuePrevHeuristic = useEventValuePrevHeuristic;
    }

    public boolean isUseEventValuePrevHeuristic() {
        return useEventValuePrevHeuristic;
    }

    // отключает оптимизацию с вкладками
    private boolean disableTabbedOptimization = false;

    public boolean isDisableTabbedOptimization() {
        return disableTabbedOptimization;
    }

    public void setDisableTabbedOptimization(boolean disableTabbedOptimization) {
        this.disableTabbedOptimization = disableTabbedOptimization;
    }

    private boolean checkUniqueEvent = false; // проверять на то что для одного свойства один event

    public void setCheckUniqueEvent(boolean checkUniqueEvent) {
        this.checkUniqueEvent = checkUniqueEvent;
    }

    public boolean isCheckUniqueEvent() {
        return checkUniqueEvent;
    }

    private boolean disableChangeModifierAllHints = true; // если есть change modifier то disable'ить hint'ы - временное решение

    public boolean isDisableChangeModifierAllHints() {
        return disableChangeModifierAllHints;
    }

    public void setDisableChangeModifierAllHints(boolean disableChangeModifierAllHints) {
        this.disableChangeModifierAllHints = disableChangeModifierAllHints;
    }

    private boolean disableValueAllHints = true; // если есть change modifier то disable'ить hint'ы - временное решение

    public boolean isDisableValueAllHints() {
        return disableValueAllHints;
    }

    public void setDisableValueAllHints(boolean disableValueAllHints) {
        this.disableValueAllHints = disableValueAllHints;
    }

    public boolean defaultOrdersNotNull = true; // временно

    public boolean isDefaultOrdersNotNull() {
        return defaultOrdersNotNull;
    }

    public void setDefaultOrdersNotNull(boolean defaultOrdersNotNull) {
        this.defaultOrdersNotNull = defaultOrdersNotNull;
    }

    private int commandLengthVolatileStats = 100000000; // определяет при какой длине команды, включать работу с волатильной статистикой

    public int getCommandLengthVolatileStats() {
        return commandLengthVolatileStats;
    }

    public void setCommandLengthVolatileStats(int commandLengthVolatileStats) {
        this.commandLengthVolatileStats = commandLengthVolatileStats;
    }

    private boolean disableReadSingleValues = false; // определять ли конкретные значения при записи запроса в таблицы

    public boolean isDisableReadSingleValues() {
        return disableReadSingleValues;
    }

    public void setDisableReadSingleValues(boolean disableReadSingleValues) {
        this.disableReadSingleValues = disableReadSingleValues;
    }
    
    private int reserveIDStep = 50; // по сколько ID'ков будут резервировать себе сервера приложений у сервера БД

    public int getReserveIDStep() {
        return reserveIDStep;
    }

    public void setReserveIDStep(int reserveIDStep) {
        this.reserveIDStep = reserveIDStep;
    }

    private boolean mergeUpClassSets = false;

    public boolean isMergeUpClassSets() {
        return mergeUpClassSets;
    }

    public void setMergeUpClassSets(boolean mergeUpClassSets) {
        this.mergeUpClassSets = mergeUpClassSets;
    }

    private int queryPrepareLength = 1000; // длина запроса, при которой необходимо pool'ить preparedStatement'ы
    private int queryPrepareRunTime = 40; // время выполнения запроса, при которой необходимо pool'ить preparedStatement'ы

    public int getQueryPrepareLength() {
        return queryPrepareLength;
    }

    public void setQueryPrepareLength(int queryPrepareLength) {
        this.queryPrepareLength = queryPrepareLength;
    }

    public int getQueryPrepareRunTime() {
        return queryPrepareRunTime;
    }

    public void setQueryPrepareRunTime(int queryPrepareRunTime) {
        this.queryPrepareRunTime = queryPrepareRunTime;
    }

    private boolean disableSimpleAddRemoveInNonExclCase = true;

    public boolean isDisableSimpleAddRemoveInNonExclCase() {
        return disableSimpleAddRemoveInNonExclCase;
    }

    public void setDisableSimpleAddRemoveInNonExclCase(boolean disableSimpleAddRemoveInNonExclCase) {
        this.disableSimpleAddRemoveInNonExclCase = disableSimpleAddRemoveInNonExclCase;
    }
}
