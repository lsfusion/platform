package platformlocal;

import java.util.*;

interface Where<Not extends Where> extends SourceJoin {

    Where followFalse(Where falseWhere);

    // внутренние
    Where siblingsFollow(Where falseWhere);
    boolean checkTrue();
    boolean directMeansFrom(AndObjectWhere where);

    Not not();

    boolean isTrue();
    boolean isFalse();

    Where and(Where where);
    Where or(Where where);
    boolean means(Where where);

    AndObjectWhere[] getAnd();
    OrObjectWhere[] getOr();

    ObjectWhere[] getObjects();

    boolean evaluate(Collection<DataWhere> data);

    boolean hashEquals(Where where);

    static String TRUE = "1=1";
    static String FALSE = "1<>1";

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    abstract Where translate(Translator translator);

    abstract JoinWheres getInnerJoins();

    abstract boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres);

    int hash();

    int getSize();
    int getHeight();


}

abstract class AbstractWhere<Not extends Where> implements Where<Not> {

    Not not = null;
    public Not not() {
        if(not==null)
            not = getNot();
        return not;
    }
    abstract Not getNot();

    public Where and(Where where) {
        return not().or(where.not()).not(); // A AND B = not(notA OR notB)
    }
    public Where or(Where where) {
        return OrWhere.op(this,where,false);
    }
    public Where followFalse(Where falseWhere) {
        return OrWhere.followFalse(this,falseWhere,false);
    }
    public boolean means(Where where) {
        return OrWhere.op(not(),where,true).checkTrue();
    }

    static Where toWhere(AndObjectWhere[] wheres) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new OrWhere(wheres);
    }
    static Where toWhere(AndObjectWhere[] wheres,int numWheres) {
        if(numWheres==1)
            return wheres[0];
        else {
            AndObjectWhere[] compiledWheres = new AndObjectWhere[numWheres]; System.arraycopy(wheres,0,compiledWheres,0,numWheres);
            return new OrWhere(compiledWheres);
        }
    }
    static Where toWhere(OrObjectWhere[] wheres) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new AndWhere(wheres);
    }
    static Where toWhere(OrObjectWhere[] wheres,int numWheres) {
        if(numWheres==1)
            return wheres[0];
        else {
            OrObjectWhere[] compiledWheres = new OrObjectWhere[numWheres]; System.arraycopy(wheres,0,compiledWheres,0,numWheres);
            return new AndWhere(compiledWheres);
        }
    }

    // не пересекаются ни в одном направлении
    static boolean decomposed(Where where1,Where where2) {
        for(ObjectWhere thisWhere : where1.getObjects())
            for(ObjectWhere thatWhere : where2.getObjects())
                if(thisWhere.depends(thatWhere))
                    return true;
        return false;
    }

    public boolean hashEquals(Where where) {
        return hashCode()==where.hashCode() && equals(where);
    }

    abstract int getHash();

    int Hash = 0;
    public int hash() {
        if(Hash==0)
            Hash = getHash();
        return Hash;
    }

    // системные
    static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i) {
        AndObjectWhere[] siblings = new AndObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }
    static OrObjectWhere[] siblings(OrObjectWhere[] wheres,int i) {
        OrObjectWhere[] siblings = new OrObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }
}


interface OrObjectWhere<Not extends AndObjectWhere> extends Where<Not> {
}

interface AndObjectWhere<Not extends OrObjectWhere> extends Where<Not> {

    Where pairs(AndObjectWhere pair, boolean plainFollow);

}

abstract class FormulaWhere<Not extends FormulaWhere,WhereType extends Where> extends AbstractWhere<Not> {

    WhereType[] wheres;
    protected FormulaWhere(WhereType[] iWheres) {
        wheres = iWheres;
//        for(int i=0;i<wheres.length;i++)
//            if(wheres[i]==null)
//                throw new RuntimeException("1");
        if(wheres.length==1)
            throw new RuntimeException("1");
    }

    abstract String getOp();
    public String toString() {
        if(wheres.length==0) return getOp().equals("AND")?"TRUE":"FALSE";

        String result = "";
        for(Where where : wheres)
            result = (result.length()==0?"":result+" "+getOp()+" ") + where;
        return "("+result+")";
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        if(wheres.length==0) return getOp().equals("AND")?TRUE:FALSE;

        String result = "";
        for(Where where : wheres)
            result = (result.length()==0?"":result+" "+getOp()+" ") + where.getSource(queryData, syntax);
        return "("+result+")";
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        for(Where where : wheres)
            where.fillJoins(joins, values);
    }

    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(where.getClass()!=getClass()) return false;

        FormulaWhere thisWhere = (FormulaWhere)where;

        if(wheres.length!= thisWhere.wheres.length) return false;

        Where[] checkWheres = thisWhere.wheres.clone();
        for(Where andWhere : wheres) {
            boolean found = false;
            for(int i=0;i<checkWheres.length;i++)
                if(checkWheres[i]!=null && andWhere.equals(checkWheres[i],mapExprs, mapWheres)) {
                    checkWheres[i] = null;
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    int getHash() {
        int result = hashCoeff();
        for(Where where : wheres)
            result += where.hash();
        return result;
    }

    abstract int hashCoeff();

    // ручной кэш хэша
    int hashCode = 0;
    public int hashCode() {
        if(hashCode==0) {
            for(Where where : wheres)
                hashCode += where.hashCode();
            hashCode = hashCode*hashCoeff();
        }
        return hashCode;
    }
    
    ObjectWhere[] objects = null;
    public ObjectWhere[] getObjects() {
        if(objects==null) {
            ObjectWhere[] findObjects = new ObjectWhere[100]; int objnum = 0;
            for(Where<?> where : wheres)
                for(ObjectWhere object : where.getObjects()) {
                    boolean found = false;
                    for(int i=0;i<objnum;i++)
                        if(findObjects[i].equals(object)) {
                            found = true;
                            break;
                        }
                    if(!found) findObjects[objnum++]=object;
                }
            objects = new ObjectWhere[objnum];
            System.arraycopy(findObjects,0,objects,0,objnum);
        }
        return objects;
    }

    static OrObjectWhere[] not(AndObjectWhere[] wheres) {
        OrObjectWhere[] result = new OrObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = ((AndObjectWhere<?>)wheres[i]).not();
        return result;
    }

    static AndObjectWhere[] not(OrObjectWhere[] wheres) {
        AndObjectWhere[] result = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            result[i] = ((OrObjectWhere<?>)wheres[i]).not();
        return result;
    }

    // из массива or'ов определяет массив and'ов
    static AndObjectWhere[] reverse(OrObjectWhere[] wheres) {
        if(wheres.length==1) {
            if(wheres[0] instanceof ObjectWhere)
                return new AndObjectWhere[]{(ObjectWhere)wheres[0]};
            else // значит OrWhere
                return ((OrWhere)wheres[0]).wheres;
        } else
            return new AndObjectWhere[]{new AndWhere(wheres)};
    }

    static AndObjectWhere[] reverseNot(AndObjectWhere[] wheres) {
        return reverse(not(wheres));
    }
    
    public int getSize() {
        if(wheres.length==0) return 0;
        
        int size = 1;
        for(Where where : wheres)
            size += where.getSize();
        return size;
    }

    public int getHeight() {
        if(wheres.length==0) return 0;

        int maxHeight = 0;
        for(int i=1;i<wheres.length;i++)
            if(wheres[i].getHeight()>wheres[maxHeight].getHeight())
                maxHeight = i;
        return wheres[maxHeight].getHeight();
    }

    boolean equalWheres(WhereType[] equals) {
        if(wheres.length!=equals.length) return false;
        WhereType[] checkWheres = equals.clone();
        for(WhereType where : wheres) {
            boolean found = false;
            for(int i=0;i<checkWheres.length;i++)
                if(checkWheres[i]!=null && where.hashEquals(checkWheres[i])) {
                    checkWheres[i] = null;
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    // отнимает одно мн-во от второго
    WhereType[] substractWheres(WhereType[] substract) {
        if(substract.length>wheres.length) return null;

        WhereType[] rawRestWheres = wheres.clone();
        for(WhereType andWhere : substract) {
            boolean found = false;
            for(int i=0;i<rawRestWheres.length;i++)
                if(rawRestWheres[i]!=null && andWhere.hashEquals(rawRestWheres[i])) {
                    rawRestWheres[i] = null;
                    found = true;
                    break;
                }
            if(!found) return null;
        }

        WhereType[] restWheres = newArray(wheres.length-substract.length); int rest=0;
        for(WhereType where : rawRestWheres)
            if(where!=null) restWheres[rest++] = where;
        return restWheres;
    }
    abstract WhereType[] newArray(int length);
}

class OrWhere extends FormulaWhere<AndWhere,AndObjectWhere> implements OrObjectWhere<AndWhere> {

    // вообще надо противоположные + Object, но это наследованием не сделаешь
    OrWhere(AndObjectWhere[] iWheres) {
        super(iWheres);
    }
    OrWhere() {
        super(new AndObjectWhere[0]);
    }

    public int getSize() {
        int size = 1;
        for(Where where : wheres)
            size += where.getSize();
        return size;
    }

    static Where op(Where where1,Where where2,boolean plainFollow) {

        if(where1.isFalse()) return where2;
        if(where2.isFalse()) return where1;
        if(where1.isTrue()) return where1;
        if(where2.isTrue()) return where2;

        AndObjectWhere[] wheres1 = where1.getAnd();
        AndObjectWhere[] wheres2 = where2.getAnd();

        // пытаем "сливать" элементы
        Where[] pairedWheres = new Where[wheres1.length]; int pairs = 0;
        AndObjectWhere[] rawWheres1 = new AndObjectWhere[wheres1.length]; int num1 = 0;
        AndObjectWhere[] rawWheres2 = wheres2.clone();
        for(AndObjectWhere andWhere1 : wheres1) {
            boolean paired = false;
            for(int i=0;i<rawWheres2.length;i++)
                if(rawWheres2[i]!=null) {
                    Where pairedWhere = rawWheres2[i].pairs(andWhere1, plainFollow);
                    if(pairedWhere!=null) {
                        pairedWheres[pairs++] = pairedWhere;
                        rawWheres2[i] = null;
                        paired = true;
                        break;
                    }
                }
            if(!paired) rawWheres1[num1++] = andWhere1;
        }

        Where unpairedWhere1,unpairedWhere2;
        if(pairs>0) {
            AndObjectWhere[] unpairedWheres2 = new AndObjectWhere[wheres2.length-pairs]; int num2 = 0;
            for(AndObjectWhere andWhere2 : rawWheres2)
                if(andWhere2!=null) unpairedWheres2[num2++] = andWhere2;
            unpairedWhere2 = toWhere(unpairedWheres2);
            unpairedWhere1 = toWhere(rawWheres1,num1);
        } else {
            unpairedWhere1 = where1;
            unpairedWhere2 = where2;            
        }

        // делаем followFalse друг друга
        Where followWhere1 = followFalse(unpairedWhere1,unpairedWhere2,plainFollow);
        Where followWhere2 = followFalse(unpairedWhere2,followWhere1,plainFollow);

        Where resultWhere;
        if(plainFollow || (followWhere1==unpairedWhere1 && followWhere2==unpairedWhere2)) { // если совпали follow'ы то все отлично
            AndObjectWhere[] followWheres1 = followWhere1.getAnd(); AndObjectWhere[] followWheres2 = followWhere2.getAnd();
            AndObjectWhere[] resultWheres = new AndObjectWhere[followWheres1.length+followWheres2.length];
            System.arraycopy(followWheres1,0,resultWheres,0,followWheres1.length);
            System.arraycopy(followWheres2,0,resultWheres,followWheres1.length,followWheres2.length);
            resultWhere = toWhere(resultWheres);
        } else // иначе погнали еще раз or может новые скобки появились
            resultWhere = op(followWhere1,followWhere2,false);

        for(int i=0;i<pairs;i++)
            resultWhere = op(resultWhere,pairedWheres[i],plainFollow);

        return resultWhere;
    }

    public boolean directMeansFrom(AndObjectWhere where) {
        for(AndObjectWhere meanWhere : wheres)
            if(meanWhere.directMeansFrom(where))
                return true;
        return false;
    }

    public AndObjectWhere[] getAnd() {
        return wheres;
    }

    public OrObjectWhere[] getOr() {
        return new OrObjectWhere[]{this};
    }

    static Where followFalse(Where followWhere, Where falseWhere, boolean plainFollow) {

        if(falseWhere.isTrue()) return new OrWhere();        
        if(!decomposed(followWhere,falseWhere)) return followWhere;

        AndObjectWhere[] wheres = followWhere.getAnd();

        // поищем "элементарные" directMeans
        // для getAnd() - вырезаем not(), которые directMeans хоть один из OrFalse - если осталось 0 элементов, возвращаем True
        //              - если из какого-нить ObjectWhere directMeans что-нить из OrFalse - то вырезаем элемент
        //              - если остался один элемент и он OrWhere для него рекурсивно повторяем операцию 
        // для элементов - проверяем может есть directMeans в OrFalse - если есть вырезаем элемент

        Where[] changedWheres = new Where[wheres.length]; int changed = 0;
        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length]; int statics = 0;
        for(AndObjectWhere where : wheres) {
            if(where instanceof ObjectWhere) {
                if(falseWhere.directMeansFrom(((ObjectWhere<?>)where).not())) // проверяем если not возвращаем true
                    return new AndWhere();
                staticWheres[statics++] = where;
            } else {
                boolean isFalse = false;
                OrObjectWhere[] followWheres = new OrObjectWhere[((AndWhere)where).wheres.length]; int follows = 0;
                for(OrObjectWhere<?> orWhere : ((AndWhere)where).wheres) {
                    if(orWhere instanceof ObjectWhere && falseWhere.directMeansFrom((ObjectWhere<?>)orWhere)) { // если из операнда следует false drop'аем весь orWhere
                        isFalse = true;
                        break;
                    }
                    if(!falseWhere.directMeansFrom(orWhere.not())) // если из not следует один из false'ов
                        followWheres[follows++] = orWhere;
                }
                if(!isFalse) {
                    if(follows==0) // остался True значит и результат true
                        return new AndWhere();

                    if(follows==((AndWhere)where).wheres.length)
                        staticWheres[statics++] = where;
                    else
                        changedWheres[changed++] = toWhere(followWheres,follows);
                }
            }
        }

        if(changed>0) { // по кругу погнали result
            Where result = toWhere(staticWheres,statics);
            for(int i=0;i<changed;i++)
                result = op(result,changedWheres[i],plainFollow);
            return followFalse(result,falseWhere,plainFollow);
        } else {
            AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length]; int results = 0;
            for(int i=0;i<statics;i++)
                if(!falseWhere.directMeansFrom(staticWheres[i])) // вычистим из wheres элементы которые заведомо false
                    resultWheres[results++] = staticWheres[i];
            Where result = (results<wheres.length?toWhere(resultWheres,results):followWhere); // чтобы сохранить ссылку
            if(plainFollow || result.isFalse()) // если глубже не надо выходим
                return result;
            else // иначе погнали основной цикл + по sibling'ам
                return result.siblingsFollow(falseWhere);
        }
    }

    public Where siblingsFollow(Where falseWhere) {
        
        if(op(this,falseWhere,true).checkTrue())
            return new AndWhere();

        Where followWhere = new OrWhere(); //false
        AndObjectWhere[] staticWheres = wheres;
        int current = 0;
        for(AndObjectWhere where : wheres) { // после упрощения важно использовать именно этот элемент, иначе неправильно работать будет
            AndObjectWhere[] siblingWheres = siblings(staticWheres, current);
            Where followAndWhere = followFalse(where,op(op(toWhere(siblingWheres),followWhere,true),falseWhere,true),false);
            if(followAndWhere!=where) { // если изменился static "перекидываем" в result
                followWhere = op(followWhere,followAndWhere,false);
                staticWheres = siblingWheres;
            } else
                current++;
        }

        if(staticWheres.length<wheres.length) // чтобы сохранить ссылку
            return op(toWhere(staticWheres),followWhere,false);
        else
            return this;
    }

    public boolean checkTrue() {
        if(wheres.length==0) return false;

        // ищем максимальную по высоте вершину and
        int maxWhere = -1;
        for(int i=0;i<wheres.length;i++)
            if(wheres[i] instanceof AndWhere && (maxWhere<0 || wheres[i].getHeight()>wheres[maxWhere].getHeight()))
                maxWhere = i;
        if(maxWhere<0)
            return false;

        AndObjectWhere[] siblingWheres = siblings(wheres, maxWhere);
        for(OrObjectWhere where : ((AndWhere)wheres[maxWhere]).wheres)
            if(!op(where,toWhere(siblingWheres),true).checkTrue())
                return false;
        return true;
    }

    AndWhere getNot() {
        return new AndWhere(not(wheres));
    }

    AndObjectWhere[] newArray(int length) {
        return new AndObjectWhere[length];  //To change body of implemented methods use File | Settings | File Templates.
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public Where translate(Translator translator) {

        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length]; int statics = 0;
        Where[] transWheres = new Where[wheres.length]; int trans = 0;
        for(AndObjectWhere where : wheres) {
            Where transWhere = where.translate(translator);
            if(transWhere==where)
                staticWheres[statics++] = where;
            else
                transWheres[trans++] = transWhere;
        }

        if(transWheres.length==0)
            return this;

        AndObjectWhere[] resultWheres = new AndObjectWhere[statics]; System.arraycopy(staticWheres,0,resultWheres,0,statics);
        Where result = toWhere(resultWheres);
        for(int i=0;i<trans;i++)
            result = result.or(transWheres[i]);
        return result;
    }

    // разобъем чисто для оптимизации
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(int i=0;i<wheres.length;i++)
            wheres[i].fillJoinWheres(joins,andWhere.and(toWhere(siblings(wheres,i)).not()));
    }

    String getOp() {
        return "OR";
    }

    public boolean isTrue() {
        return false;
    }

    public boolean isFalse() {
        return wheres.length==0;
    }

    public boolean evaluate(Collection<DataWhere> data) {
        boolean result = false;
        for(Where where : wheres)
            result = result || where.evaluate(data);
        return result;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof OrWhere && equalWheres(((OrWhere)o).wheres);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public JoinWheres getInnerJoins() {
        JoinWheres result = new JoinWheres();
        for(Where<?> where : wheres)
            result.or(where.getInnerJoins());
        return result;
    }

    int hashCoeff() {
        return 5;
    }
}

// нужен для "расщепления" Where на типы Join'ов,равенств ключей и остальных Where
class JoinWheres extends HashMap<Where,Where> {

    void or(JoinWheres joins) {
        for(Map.Entry<Where,Where> whereJoin : joins.entrySet())
            or(whereJoin.getKey(),whereJoin.getValue());
    }

    void or(Where join,Where where) {
        if(!where.isFalse()) {
            Where resultWhereJoin = get(join);
            put(join,resultWhereJoin==null?where:resultWhereJoin.or(where));
        }
    }

    JoinWheres() {
    }

    JoinWheres(Where join,Where where) {
        put(join,where);
    }

    JoinWheres and(JoinWheres joins) {
        JoinWheres result = new JoinWheres();
        // берем все пары joins'ов
        for(Map.Entry<Where,Where> whereJoin1 : entrySet())
            for(Map.Entry<Where,Where> whereJoin2 : joins.entrySet())
                result.or(whereJoin1.getKey().and(whereJoin2.getKey()),whereJoin1.getValue().and(whereJoin2.getValue()));
        return result;
    }
}

class Decision {
    AndObjectWhere condition;
    Where addTrue;
    Where addFalse;

    OrWhere whereTrue;
    OrWhere whereFalse;

    Decision(AndObjectWhere iCondition, Where iAddTrue, Where iAddFalse, OrWhere iWhereTrue, OrWhere iWhereFalse) {
        condition = iCondition;
        addTrue = iAddTrue;
        addFalse = iAddFalse;

        whereTrue = iWhereTrue;
        whereFalse = iWhereFalse;
    }

    Where pairs(Decision decision2,boolean plainFollow) {
        if(condition.hashEquals(decision2.condition))
            return OrWhere.op(whereTrue,decision2.addTrue,plainFollow).and(
                OrWhere.op(whereFalse,decision2.addFalse,plainFollow));

        if(condition.hashEquals(decision2.condition.not()))
            return OrWhere.op(whereTrue,decision2.addFalse,plainFollow).and(
                OrWhere.op(whereFalse,decision2.addTrue,plainFollow));
        
        return null;
    }
}

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

    public Where siblingsFollow(Where falseWhere) {
        OrWhere notWhere = not();
        Where followAnd = OrWhere.followFalse(notWhere,falseWhere,false);
        if(followAnd==notWhere) // чтобы сохранить ссылку
            return this;
        else
            return followAnd.not();
    }

    public boolean checkTrue() {
        for(Where where : wheres)
            if(!where.checkTrue())
                return false;
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
        JoinWheres result = new JoinWheres(new AndWhere(),new AndWhere());
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
            return toWhere(thisWheres,thisnum).or(toWhere(thatWheres)).and(toWhere(pairedWheres,pairs)); // (W1 OR W2) AND P
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

abstract class ObjectWhere<Not extends ObjectWhere> extends AbstractWhere<Not> implements OrObjectWhere<Not>,AndObjectWhere<Not> {

    public Where pairs(AndObjectWhere pair, boolean plainFollow) {
        return null;
    }

    public boolean isTrue() {
        return false;
    }

    public boolean isFalse() {
        return false; 
    }

    public ObjectWhere[] getObjects() {
        return new ObjectWhere[]{this};
    }

    public AndObjectWhere[] getAnd() {
        return new AndObjectWhere[]{this};
    }

    public OrObjectWhere[] getOr() {
        return new OrObjectWhere[]{this};
    }

    public Where siblingsFollow(Where falseWhere) {
        if(OrWhere.op(not(),falseWhere,true).checkTrue())
            return new OrWhere();
        else
            return this;
    }

    public boolean checkTrue() {
        return false;
    }

    public int getSize() {
        return 1;
    }

    public int getHeight() {
        return 1;
    }

    boolean depends(ObjectWhere where) {
        // собсно логика простая из элемента или его not'а должен следовать where элемент
        // потому как иначе положив все where = FALSE мы не изменим формулу никак
        return where.directMeansFrom(this) || where.directMeansFrom(not());
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillDataJoinWheres(joins,andWhere.and(this));
    }

    abstract protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere);
}

abstract class DataWhere extends ObjectWhere<NotWhere> {

    public boolean directMeansFrom(AndObjectWhere where) {
        return where instanceof DataWhere && ((DataWhere)where).follow(this);
    }

    public NotWhere getNot() {
        return new NotWhere(this);
    }

    DataWhere[] follows = null;
    Map<DataWhere,Boolean> cacheFollow = new IdentityHashMap<DataWhere, Boolean>();
    boolean follow(DataWhere dataWhere) {
        if(follows==null) {
            Set<DataWhere> setFollows = getFollows();
            follows = new DataWhere[setFollows.size()];
            setFollows.toArray(follows);            
        }

        for(DataWhere follow : follows)
            if(dataWhere.equals(follow))
                return true;
        return false;
/*      if(!Main.ActivateCaches) return calculateFollow(dataWhere);
        Boolean Result = cacheFollow.get(dataWhere);
        if(Result==null) {
            Result = calculateFollow(dataWhere);
            cacheFollow.put(dataWhere,Result);
        }

        return Result;*/
    }

    abstract boolean calculateFollow(DataWhere dataWhere);
    abstract Set<DataWhere> getFollows();
    
    public boolean evaluate(Collection<DataWhere> data) {
        return data.contains(this);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return NotWhere.PREFIX + getSource(queryData, syntax);
    }
}

class NotWhere extends ObjectWhere<DataWhere> {

    DataWhere where;
    NotWhere(DataWhere iWhere) {
        where = iWhere;
    }

    public boolean directMeansFrom(AndObjectWhere meanWhere) {
        return meanWhere instanceof NotWhere && where.follow(((NotWhere)meanWhere).where);
    }

    public DataWhere getNot() {
        return where;
    }

    static String PREFIX = "NOT ";
    public String toString() {
        return PREFIX+where;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof NotWhere && where.equals(((NotWhere)o).where);
    }

    public int hashCode() {
        return where.hashCode()*31;
    }

    public boolean evaluate(Collection<DataWhere> data) {
        return !where.evaluate(data);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ
    
    public Where translate(Translator translator) {
        Where transWhere = where.translate(translator);
        if(transWhere==where)
            return this;
        return transWhere.not();
    }


    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        where.fillJoins(joins,values);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return where.getNotSource(queryData,syntax);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        where.fillDataJoinWheres(joins, andWhere);
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(new AndWhere(),this);
    }

    public boolean equals(Where equalWhere, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return equalWhere instanceof NotWhere && where.equals(((NotWhere)equalWhere).where, mapExprs, mapWheres) ;
    }

    public int getHash() {
        return where.hash()*3;
    }
}
