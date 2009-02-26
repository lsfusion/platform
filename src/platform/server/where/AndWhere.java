package platform.server.where;

import platform.server.data.query.JoinData;
import platform.server.data.query.JoinWheres;
import platform.server.data.query.Translator;
import platform.server.data.query.wheres.MapWhere;

import java.util.Collection;

class AndWhere extends FormulaWhere<OrWhere,OrObjectWhere> implements AndObjectWhere<OrWhere> {

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

    OrObjectWhere[] newArray(int length) {
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

    public Where siblingsFollow(Where falseWhere) {
        OrWhere notWhere = not();
        Where followAnd = OrWhere.followFalse(notWhere,falseWhere,false);
        if(followAnd==notWhere) // чтобы сохранить ссылку
            return this;
        else
            return followAnd.not();
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
            wheres[i].fillJoinWheres(joins,andWhere.and(toWhere(siblings(wheres,i))));
    }

    public boolean directMeansFrom(AndObjectWhere where) {
        return where instanceof AndWhere && ((AndWhere)where).substractWheres(wheres)!=null;
    }

    public boolean evaluate(Collection<DataWhere> data) {
        boolean result = true;
        for(Where where : wheres)
            result = result && where.evaluate(data);
        return result;
    }

    public JoinWheres getInnerJoins() {
        JoinWheres result = new JoinWheres(TRUE, TRUE);
        for(Where<?> where : wheres)
            result = result.and(where.getInnerJoins());
        return result;
    }

    OrWhere getNot() {
        return new OrWhere(not(wheres));
    }

    public Where translate(Translator translator) {
        OrWhere notWhere = not();
        Where translatedNotWhere = notWhere.translate(translator);
        if(translatedNotWhere==notWhere)
            return this;
        else
            return translatedNotWhere.not();
    }

    int hashCoeff() {
        return 7;
    }

    Decision[] decisions = null;
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
                rawDecisions[decnum++] = new Decision(leftWhere.wheres[i],toWhere(siblings(leftWhere.wheres,i)),toWhere(rightNotWheres),leftWhere,rightWhere);
        }
        // справа not'им только не object'ы (чтобы 2 раза не давать одно и тоже)
        for(int i=0;i<rightWhere.wheres.length;i++)
            if(!(rightWhere.wheres[i] instanceof ObjectWhere)) {
                OrWhere notRightInWhere = ((AndWhere)rightWhere.wheres[i]).not();
                AndObjectWhere[] leftNotWheres = rightWhere.substractWheres(notRightInWhere.wheres);
                if(leftNotWheres!=null) // нашли decision, sibling'и right + оставшиеся left из правого
                    rawDecisions[decnum++] = new Decision(rightWhere.wheres[i],toWhere(siblings(rightWhere.wheres,i)),toWhere(leftNotWheres),rightWhere,leftWhere);
            }
        decisions = new Decision[decnum]; System.arraycopy(rawDecisions,0,decisions,0,decnum);
        return decisions;
    }

    public Where pairs(AndObjectWhere pair, boolean plainFollow) {
        if(pair instanceof ObjectWhere) return null;
        AndWhere pairAnd = (AndWhere)pair;

        OrObjectWhere[] pairedWheres = new OrObjectWhere[wheres.length]; int pairs = 0;
        OrObjectWhere[] thisWheres = new OrObjectWhere[wheres.length]; int thisnum = 0;
        OrObjectWhere[] pairedThatWheres = pairAnd.wheres.clone();
        for(OrObjectWhere opWhere : wheres) {
            boolean paired = false;
            for(int i=0;i<pairedThatWheres.length;i++)
                if(pairedThatWheres[i]!=null && pairAnd.wheres[i].hashEquals(opWhere)) {
                    pairedWheres[pairs++] = opWhere;
                    pairedThatWheres[i] = null;
                    paired = true;
                    break;
                }
            if(!paired) thisWheres[thisnum++] = opWhere;
        }

        if(pairs > 0) { // нашли пару пошли дальше упрощать
            if(pairs==pairAnd.wheres.length || thisnum==0) // тогда не скобки а следствия пусть followFalse - directMeans устраняют
                return null;

            OrObjectWhere[] thatWheres = new OrObjectWhere[pairAnd.wheres.length-pairs]; int compiledNum = 0;
            for(OrObjectWhere opWhere : pairedThatWheres)
                if(opWhere!=null) thatWheres[compiledNum++] = opWhere;
            return OrWhere.op(OrWhere.op(toWhere(thisWheres,thisnum),toWhere(thatWheres),plainFollow).not(),toWhere(pairedWheres,pairs).not(),plainFollow).not(); // (W1 OR W2) AND P
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

    public boolean equals(Object o) {
        return this==o || o instanceof AndWhere && equalWheres(((AndWhere)o).wheres);
    }
}
