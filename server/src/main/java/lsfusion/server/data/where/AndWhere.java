package lsfusion.server.data.where;

import lsfusion.base.ArrayInstancer;
import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.Settings;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.innerjoins.KeyEquals;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.classes.MeanClassWhere;
import lsfusion.server.data.where.classes.MeanClassWheres;


public class AndWhere extends FormulaWhere<OrObjectWhere> implements AndObjectWhere<OrWhere>, ArrayInstancer<OrObjectWhere> {

    AndWhere(OrObjectWhere[] wheres, boolean check) {
        super(wheres, check);
    }
    AndWhere() {
        super(new OrObjectWhere[0], false);
    }

    public OrObjectWhere[] newArray(int length) {
        return new OrObjectWhere[length];
    }

    protected String getOp() {
        return "AND";
    }

    public AndObjectWhere[] getAnd() {
        return new AndObjectWhere[]{this};
    }

    public OrObjectWhere[] getOr() {
        return wheres;
    }

    public Where followFalse(CheckWhere falseWhere, boolean pack, FollowChange change) {
        Where result = not().followFalse(falseWhere, pack, change).not();
        change.not();
        return result;
    }
    
    public final static ArrayInstancer<OrObjectWhere> instancer = new ArrayInstancer<OrObjectWhere>() {
        public OrObjectWhere[] newArray(int size) {
            return new OrObjectWhere[size];
        }
    };

    public boolean isTrue() {
        return wheres.length==0;
    }

    public boolean isFalse() {
        return false;
    }

    public boolean checkFormulaTrue() {
        OrObjectWhere[] maxWheres = wheres.clone();
        for(int i=0;i<maxWheres.length;i++) { // будем бежать с высот поменьше - своего рода пузырьком
            for(int j=maxWheres.length-1;j>i;j--)
                if(maxWheres[j].getHeight()<maxWheres[j-1].getHeight()) {
                    OrObjectWhere t = maxWheres[j];
                    maxWheres[j] = maxWheres[j-1];
                    maxWheres[j-1] = t;
                }
            if(!maxWheres[i].checkTrue())
                return false;
        }
        return true;
    }

    // разобъем чисто для оптимизации
    public void fillJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        for(int i=0;i<wheres.length;i++)
            wheres[i].fillJoinWheres(joins,andWhere.and(toWhere(siblings(wheres, i))));
    }

    public boolean directMeansFrom(AndObjectWhere where) {
        return wheres.length==0 || (where instanceof AndWhere && (BaseUtils.hashEquals(this, where) || (substractWheres(((AndWhere)where).wheres,wheres,instancer)!=null)));
    }

    public boolean directMeansFromNot(AndObjectWhere[] notWheres, boolean[] used, int skip) {
        return wheres.length == 0 || (notWheres.length != 1 && substractWheresNot(notWheres, wheres, used, skip)); // hashEquals не будем проверять так как это оптимистичная проверка
    }

    protected <K extends BaseExpr> GroupJoinsWheres calculateGroupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        GroupJoinsWheres result = new GroupJoinsWheres(TRUE, type);
        for(int i=0;i<wheres.length;i++) {
            // приходится и промежуточные группировать, так как при большом количестве операндов, complexity может до миллиона дорасти
            if(result.isExceededIntermediatePackThreshold()) {
                OrObjectWhere[] procWheres = newArray(i);
                System.arraycopy(wheres, 0, procWheres, 0, i);
                result = packIntermediate(result, type, keepStat, keyStat, toWhere(procWheres, true), orderTop);
            }
            result = result.and(wheres[i].groupJoinsWheres(keepStat, keyStat, orderTop, type));
        }
        return result;
    }
    public KeyEquals calculateGroupKeyEquals() {
        KeyEquals result = new KeyEquals(TRUE, false); // потому что вызывается из calculateGroupKeyEquals, а там уже проверили что все не isSimple
        for(Where where : wheres)
            result = result.and(where.getKeyEquals());
        return result;
    }
    public MeanClassWheres calculateGroupMeanClassWheres(boolean useNots) {
        MeanClassWheres result = new MeanClassWheres(MeanClassWhere.TRUE, TRUE);
        for(int i=0;i<wheres.length;i++) {
            if(result.size() > Settings.get().getLimitClassWhereCount() || result.getComplexity(true) > Settings.get().getLimitClassWhereComplexity()) {
                if(useNots)
                    return groupMeanClassWheres(false);
                else { // приходится и промежуточные группировать, так как при большом количестве операндов, complexity может до миллиона дорасти
                    OrObjectWhere[] procWheres = newArray(i);
                    System.arraycopy(wheres, 0, procWheres, 0, i);
                    result = compactHeuristic(result, toWhere(procWheres, true));
                }
            }
            result = result.and(wheres[i].groupMeanClassWheres(useNots));
        }
        return result;
    }

    public OrWhere not = null;
    @ManualLazy
    public OrWhere not() { // именно здесь из-за того что типы надо перегружать без generics
        if(not==null) {
            OrWhere calcNot = new OrWhere(not(wheres), check);
            calcNot.not = this; // для оптимизации
            not = calcNot;
        }
        return not;
    }

    protected Where translate(MapTranslate translator) {
        return not().translateOuter(translator).not();
    }
    public Where translateQuery(QueryTranslator translator) {
        return not().translateQuery(translator).not();
    }

    protected int hashCoeff() {
        return 7;
    }

    private Decision[] decisions = null;
    @ManualLazy
    private Decision[] getDecisions() {
        if(decisions!=null) return decisions;

        // одно условие может состоять из нескольких decision'ов
        // decision'ом может быть только в случае если у нас ровно 2 узла и оба не object'ы
        if(wheres.length!=2 || wheres[0] instanceof ObjectWhere || wheres[1] instanceof ObjectWhere) {
            decisions = new Decision[0];
            return decisions;
        }
        OrWhere leftWhere = (OrWhere) wheres[0];
        OrWhere rightWhere = (OrWhere) wheres[1];

        Decision[] rawDecisions = new Decision[leftWhere.wheres.length+rightWhere.wheres.length]; int decnum = 0;
        // слева not'им все и ищем справа
        for(int i=0;i<leftWhere.wheres.length;i++) {
            OrObjectWhere notLeftInWhere = leftWhere.wheres[i].not(); // или Object или That
            AndObjectWhere[] rightNotWheres = substractWheres(rightWhere.wheres, notLeftInWhere.getAnd(), OrWhere.instancer);
            if(rightNotWheres!=null) // нашли decision, sibling'и left + оставшиеся right из правого
                rawDecisions[decnum++] = new Decision(leftWhere.wheres[i],siblingsWhere(leftWhere.wheres,i),toWhere(rightNotWheres),leftWhere,rightWhere);
        }
        // справа not'им только не object'ы (чтобы 2 раза не давать одно и тоже)
        for(int i=0;i<rightWhere.wheres.length;i++)
            if(!(rightWhere.wheres[i] instanceof ObjectWhere)) {
                OrWhere notRightInWhere = ((AndWhere)rightWhere.wheres[i]).not();
                AndObjectWhere[] leftNotWheres = substractWheres(rightWhere.wheres, notRightInWhere.wheres, OrWhere.instancer);
                if(leftNotWheres!=null) // нашли decision, sibling'и right + оставшиеся left из правого
                    rawDecisions[decnum++] = new Decision(rightWhere.wheres[i],siblingsWhere(rightWhere.wheres,i),toWhere(leftNotWheres),rightWhere,leftWhere);
            }
        Decision[] calcDecisions = new Decision[decnum]; System.arraycopy(rawDecisions,0,calcDecisions,0,decnum);
        decisions = calcDecisions;
        return decisions;
    }

    public static Where andPairs(Where where1, Where where2) {
        return OrWhere.orPairs(where1.not(), where2.not()).not();
    }

    public Where pairs(AndObjectWhere pair) {

        if(pair instanceof ObjectWhere) return null;
        AndWhere pairAnd = (AndWhere)pair;

        BaseUtils.Paired<OrObjectWhere> paired = new BaseUtils.Paired<OrObjectWhere>(wheres, pairAnd.wheres, this);
        if(paired.common.length > 0) { // нашли пару пошли дальше упрощать
            if(paired.common.length==pairAnd.wheres.length || paired.getDiff1().length==0) // тогда не скобки а следствия пусть followFalse - directMeans устраняют
                return null;

            return andPairs(OrWhere.orPairs(toWhere(paired.getDiff1()),toWhere(paired.getDiff2())),toWhere(paired.common)); // (W1 OR W2) AND P
        }

        // поищем decision'ы
        for(Decision decision : getDecisions())
            for(Decision thatDecision : pairAnd.getDecisions()) {
                Where pairedDecision = decision.pairs(thatDecision);
                if(pairedDecision!=null) return pairedDecision;
            }

        // поищем means'ы

        // значит не сpair'ились
        return null;
    }

    // X OR (Y AND Z) и X=>Y, то равно Y AND (X OR Z)
    public static Where changeMeans(AndObjectWhere where, AndObjectWhere pair, CheckWhere orSiblings, boolean packExprs) {
        OrObjectWhere[] orWheres = pair.getOr();
        for(int k=0;k<orWheres.length;k++) {
            if(where.means(orWheres[k])) { // значит можно поменять местами
                Where andSiblings = toWhere(siblings(orWheres, k), pair); // если сокращается хоть что-то, меняем местами
                FollowChange change = new FollowChange();
                Where meanWhere = where.followFalse(OrWhere.orCheck(OrWhere.orCheck(andSiblings, orWheres[k].not()), orSiblings), packExprs, change);
                if(change.type!= FollowType.EQUALS)
                    return AndWhere.andPairs(orWheres[k],OrWhere.orPairs(meanWhere,andSiblings));
            }
        }

        return null;
    }


    public boolean calcTwins(TwinImmutableObject o) {
        return BaseUtils.equalArraySets(wheres, ((AndWhere) o).wheres);
    }

    public boolean isNot() {
        return true;
    }
}
