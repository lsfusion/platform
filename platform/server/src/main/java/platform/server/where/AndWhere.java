package platform.server.where;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.server.caches.ManualLazy;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.MeanClassWheres;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.JoinData;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.wheres.MapWhere;


public class AndWhere extends FormulaWhere<OrWhere,OrObjectWhere> implements AndObjectWhere<OrWhere>, ArrayInstancer<OrObjectWhere> {

    AndWhere(OrObjectWhere[] iWheres) {
        super(iWheres);
    }
    AndWhere() {
        super(new OrObjectWhere[0]);
    }
    static OrObjectWhere[] copyOf(OrObjectWhere[] rawWheres,int numWheres) {
        OrObjectWhere[] iWheres = new OrObjectWhere[numWheres]; System.arraycopy(rawWheres,0,iWheres,0,numWheres);
        return iWheres;
    }
    AndWhere(OrObjectWhere[] rawWheres,int numWheres) {
        super(copyOf(rawWheres,numWheres));
    }

    public OrObjectWhere[] newArray(int length) {
        return new OrObjectWhere[length];
    }

    String getOp() {
        return "AND";
    }

    public boolean isTrue() {
        return wheres.length==0;
    }

    public boolean isFalse() {
        return false;
    }

    public AndObjectWhere[] getAnd() {
        return new AndObjectWhere[]{this};
    }

    public OrObjectWhere[] getOr() {
        return wheres;
    }

    public Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects) {
        // в отличии от or сразу заведомо известный результат мы никак не получим поэтому просто прямо бежим
        OrObjectWhere[] staticWheres = new OrObjectWhere[wheres.length]; int stat = 0;
        Where[] decomposedWheres = new Where[wheres.length]; int decomp = 0;
        for(OrObjectWhere where : wheres) {
            Where decomposedWhere = where.decompose(decompose,objects);
            if(decomposedWhere == where)
                staticWheres[stat++] = where;
            else
                decomposedWheres[decomp++] = decomposedWhere;
        }

        if(stat < wheres.length) {
            Where result = toWhere(staticWheres,stat);
            for(int i=0;i<decomp;i++)
                result = OrWhere.op(result.not(),decomposedWheres[i].not(),true).not();
            return result;
        } else
            return this;
    }

    public Where innerFollowFalse(Where falseWhere, boolean sureNotTrue) {
        return OrWhere.followFalse(not(),falseWhere,false,false).not();
    }

    public boolean checkTrue() {

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
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(int i=0;i<wheres.length;i++)
            wheres[i].fillJoinWheres(joins,andWhere.and(siblingsWhere(wheres,i)));
    }

    public boolean directMeansFrom(AndObjectWhere where) {
        return where instanceof AndWhere && ((AndWhere)where).substractWheres(wheres)!=null;
    }

    public InnerJoins getInnerJoins() {
        InnerJoins result = new InnerJoins(TRUE);
        for(Where<?> where : wheres)
            result = result.and(where.getInnerJoins());
        return result;
    }
    public MeanClassWheres calculateMeanClassWheres() {
        MeanClassWheres result = new MeanClassWheres(ClassExprWhere.TRUE, TRUE);
        for(Where<?> where : wheres)
            result = result.and(where.getMeanClassWheres());
        return result;
    }

    OrWhere calculateNot() {
        return new OrWhere(not(wheres));
    }

    public Where translateDirect(KeyTranslator translator) {
        return not().translateDirect(translator).not();
    }
    public Where translateQuery(QueryTranslator translator) {
        return not().translateQuery(translator).not();
    }

    int hashCoeff() {
        return 7;
    }

    private Decision[] decisions = null;
    @ManualLazy
    Decision[] getDecisions() {
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
            OrObjectWhere notLeftInWhere = ((AndObjectWhere<?>)leftWhere.wheres[i]).not(); // или Object или That
            AndObjectWhere[] rightNotWheres = rightWhere.substractWheres(notLeftInWhere.getAnd());
            if(rightNotWheres!=null) // нашли decision, sibling'и left + оставшиеся right из правого
                rawDecisions[decnum++] = new Decision(leftWhere.wheres[i],siblingsWhere(leftWhere.wheres,i),toWhere(rightNotWheres),leftWhere,rightWhere);
        }
        // справа not'им только не object'ы (чтобы 2 раза не давать одно и тоже)
        for(int i=0;i<rightWhere.wheres.length;i++)
            if(!(rightWhere.wheres[i] instanceof ObjectWhere)) {
                OrWhere notRightInWhere = ((AndWhere)rightWhere.wheres[i]).not();
                AndObjectWhere[] leftNotWheres = rightWhere.substractWheres(notRightInWhere.wheres);
                if(leftNotWheres!=null) // нашли decision, sibling'и right + оставшиеся left из правого
                    rawDecisions[decnum++] = new Decision(rightWhere.wheres[i],siblingsWhere(rightWhere.wheres,i),toWhere(leftNotWheres),rightWhere,leftWhere);
            }
        decisions = new Decision[decnum]; System.arraycopy(rawDecisions,0,decisions,0,decnum);
        return decisions;
    }

    public Where pairs(AndObjectWhere pair, boolean plainFollow) {

        if(pair instanceof ObjectWhere) return null;
        AndWhere pairAnd = (AndWhere)pair;

        BaseUtils.Paired<OrObjectWhere> paired = new BaseUtils.Paired<OrObjectWhere>(wheres, pairAnd.wheres, this);
        if(paired.common.length > 0) { // нашли пару пошли дальше упрощать
            if(paired.common.length==pairAnd.wheres.length || paired.getDiff1().length==0) // тогда не скобки а следствия пусть followFalse - directMeans устраняют
                return null;

            return OrWhere.op(OrWhere.op(toWhere(paired.getDiff1()),toWhere(paired.getDiff2()),plainFollow).not(),toWhere(paired.common).not(),plainFollow).not(); // (W1 OR W2) AND P
        }

        // поищем decision'ы
        for(Decision decision : getDecisions())
            for(Decision thatDecision : pairAnd.getDecisions()) {
                Where pairedDecision = decision.pairs(thatDecision,plainFollow);
                if(pairedDecision!=null) return pairedDecision;
            }

        // значит не сpair'ились
        return null;
    }

    public boolean twins(AbstractSourceJoin o) {
        return BaseUtils.equalArraySets(wheres, ((AndWhere) o).wheres);
    }
}
