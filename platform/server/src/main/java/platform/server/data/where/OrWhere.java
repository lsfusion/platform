package platform.server.data.where;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.SimpleMap;
import platform.server.caches.ManualLazy;
import platform.server.caches.ParamLazy;
import platform.server.data.expr.where.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.innerjoins.ObjectJoinSets;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.where.classes.PackClassWhere;


public class OrWhere extends FormulaWhere<AndObjectWhere> implements OrObjectWhere<AndWhere> {

    // вообще надо противоположные + Object, но это наследованием не сделаешь
    OrWhere(AndObjectWhere[] wheres, boolean check) {
        super(wheres, check);
    }
    OrWhere() {
        super(new AndObjectWhere[0], false);
    }

    public final static ArrayInstancer<AndObjectWhere> instancer = new ArrayInstancer<AndObjectWhere>() {
        public AndObjectWhere[] newArray(int size) {
            return new AndObjectWhere[size];
        }
    };

    // выполняет or двух where в правильной форме получая where в правильной форме 
    public static Where or(Where where1,Where where2,boolean pack) {

        if(where1.isFalse() || where2.isTrue()) return where2;
        if(where2.isFalse() || where1.isTrue()) return where1;
        
        Where followWhere1 = where1.followFalse(where2, false, pack, new FollowChange());
        while(true) {
            FollowChange change = new FollowChange();
            Where followWhere2 = where2.followFalse(followWhere1, true, pack, change);
            if(change.type != FollowType.WIDE && change.type != FollowType.DIFF) {
                assert BaseUtils.hashEquals(followWhere1.followFalse(followWhere2, true, pack, new FollowChange()),followWhere1);
                return orPairs(followWhere2, followWhere1);
            } else { // поменяем followWhere1 и followWhere2 местами
                where2 = followWhere1; followWhere1 = followWhere2;
            }
        }
    }

    // выполняет or двух where в правильной форме assert'утых что where1.ff(where2)==where1, и where2.ff(where1)==where2
    // перестраивает дерево - выделяя скобки, decision'ы,  
    public static Where orPairs(Where where1,Where where2) {

        if(where1.isFalse() || where2.isTrue()) return where2;
        if(where2.isFalse() || where1.isTrue()) return where1;
        
        AndObjectWhere[] wheres1 = where1.getAnd();
        AndObjectWhere[] wheres2 = where2.getAnd();

        // пытаем вытащить скобки/decision'ы
        Where[] pairedWheres = new Where[wheres1.length]; int pairs = 0;
        AndObjectWhere[] rawWheres1 = new AndObjectWhere[wheres1.length]; int num1 = 0;
        AndObjectWhere[] rawWheres2 = wheres2.clone();
        for(AndObjectWhere andWhere1 : wheres1) {
            boolean paired = false;
            for(int i=0;i<rawWheres2.length;i++)
                if(rawWheres2[i]!=null) {
                    Where pairedWhere = rawWheres2[i].pairs(andWhere1);
                    if(pairedWhere!=null) {
                        pairedWheres[pairs++] = pairedWhere;
                        rawWheres2[i] = null;
                        paired = true;
                        break;
                    }
                }
            if(!paired) rawWheres1[num1++] = andWhere1;
        }

        AndObjectWhere[] unpairedWheres = new AndObjectWhere[wheres2.length-pairs+num1]; int num2 = 0;
        for(AndObjectWhere andWhere2 : rawWheres2)
            if(andWhere2!=null) unpairedWheres[num2++] = andWhere2;
        System.arraycopy(rawWheres1, 0, unpairedWheres, num2, num1);

        Where result = toWhere(unpairedWheres);
        for(int i=0;i<pairs;i++)
            result = orPairs(result, pairedWheres[i]);
        return result;
    }

    private static CheckWhere orCheckNull(CheckWhere where1, CheckWhere where2) {
        if(where2==null)
            return where1;
        else
            return orCheck(where1,where2);        
    }

    public Where followFalse(CheckWhere falseWhere, boolean sureNotTrue, boolean pack, FollowChange change) {

        if(falseWhere.isTrue()) return FALSE;
        if(isFalse()) return this;
        if(falseWhere.isFalse() && !pack) return this;

        if(!sureNotTrue) {
            if(checkTrue(this,falseWhere)) {
                change.type = FollowType.WIDE;
                return TRUE;
            }
            sureNotTrue = true;
        }

        // нужно минимизировать orCheck'и
        int N = 2; while(N<wheres.length) N = N*2;
        CheckWhere[] siblingNewWheres = new CheckWhere[N * 2]; // хранит матрицы по компонентам

        FollowType changeType = FollowType.EQUALS;

        int i=0;
        int in=0; // количество сколько изменилось
        int ilast = -1; // какой элемент пропускfnm
        while(true) {
            if(i!=ilast) {
                int Ni=N+i;
                
                CheckWhere siblingWhere = Where.FALSE;
                for(int j=Ni;j>1;j=j/2) // расчитываем siblings для изменившихся условий
                    siblingWhere = orCheckNull(siblingWhere,siblingNewWheres[(j%2==0?j+1:j-1)]); // берем siblings

                AndObjectWhere[] siblingStatics = new AndObjectWhere[wheres.length-in-(siblingNewWheres[Ni]!=null?0:1)]; int sw = 0;
                for(int j=0;j<wheres.length;j++) // расчитываем siblings для неизменившихся условий
                    if(j!=i && siblingNewWheres[N+j]==null)
                        siblingStatics[sw++] = wheres[j];
                siblingWhere = orCheck(siblingWhere,toWhere(siblingStatics));

                FollowChange followChange = new FollowChange();
                Where followWhere = (siblingNewWheres[Ni]==null?wheres[i]:(Where)siblingNewWheres[Ni]).followFalse(orCheck(siblingWhere, falseWhere), sureNotTrue, pack, followChange);
                changeType = changeType.or(followChange.type);
                if(followChange.type!=FollowType.EQUALS) { // если шире стал, изменился, и даже если narrow'е стал (потому как может каскад вызвать)
                    if(followWhere.isTrue()) return TRUE; // простое ускорение
                    if(siblingNewWheres[Ni]==null) in++;
                    siblingNewWheres[Ni] = followWhere;
                    while(Ni/2>1) { // пересчитываем sibling'и new
                        siblingNewWheres[Ni/2] = orCheckNull(siblingNewWheres[Ni], siblingNewWheres[(Ni%2==0?Ni+1:Ni-1)]);
                        Ni=Ni/2;
                    }

                    if(followChange.type!=FollowType.NARROW) { // начинаем заново, потому как могло повлиять на уже обработанные условия
                        ilast=i; i=0; }
                    continue;
                }
            }
            i++;
            if(i>=wheres.length)
                break;
        }

        Where result = Where.FALSE;
        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length-in];
        int sn = 0;
        for(int j=0;j<wheres.length;j++)
            if(siblingNewWheres[N+j]!=null)
                result = orPairs(result, (Where) siblingNewWheres[N+j]);
            else
                staticWheres[sn++] = wheres[j];
        result = orPairs(result,toWhere(staticWheres));

        if(pack && (changeType == FollowType.WIDE || changeType == FollowType.DIFF) && checkTrue(result, falseWhere)) { // follow мог упаковаться и соответственно изменится и надо проверить все на checkTrue
            change.type = FollowType.WIDE;
            return TRUE;
        }

        change.type = changeType;       
        return result;
    }

    private static CheckWhere checkff(CheckWhere where, CheckWhere falseWhere) {

        if(falseWhere.isTrue()) return FALSE;
        if(falseWhere.isFalse()) return where;

        // если Or берем not и проверяем
        if(where instanceof OrObjectWhere) // если Or то проверим, если And нет смысла, все равно скобки потом отдельно будут раскрываться
            if(falseWhere.directMeansFrom(((OrObjectWhere)where).not()))
                return Where.TRUE;

        AndObjectWhere[] wheres = where.getAnd();
        AndObjectWhere[] rawWheres = new AndObjectWhere[wheres.length]; int fnum = 0;
        for(AndObjectWhere and2 : wheres)
            if(!falseWhere.directMeansFrom(and2))
                rawWheres[fnum++] = and2;
        AndObjectWhere[] followWheres = new AndObjectWhere[fnum]; System.arraycopy(rawWheres,0,followWheres,0,fnum);
        return toWhere(followWheres, where);
    }

    // из двух where в неправильной форме, делает еще одно where в неправильной форме
    // ничего не перестраивает цель такого CheckWhere найти первый попавшийся not Null элемент и пометиться как не true
    // выполняет основные проверки на directMeansFrom
    public static CheckWhere orCheck(CheckWhere where1,CheckWhere where2) {

/*        if(where1.isFalse() || where2.isTrue()) return where2;
        if(where2.isFalse() || where1.isTrue()) return where1;

        return toWhere(BaseUtils.add(where1.getAnd(), where2.getAnd(), instancer), true);
  */
        CheckWhere followWhere1 = checkff(where1, where2);
        CheckWhere followWhere2 = checkff(where2, followWhere1);

        if(followWhere1.isFalse() || followWhere2.isTrue()) return followWhere2;
        if(followWhere2.isFalse() || followWhere1.isTrue()) return followWhere1;
        
        return toWhere(BaseUtils.add(followWhere1.getAnd(), followWhere2.getAnd(), instancer), true);
    }

    public static boolean checkTrue(CheckWhere where1, CheckWhere where2) {
        return orCheck(where1, where2).checkTrue();        
    }

    /*    // Метод переворачивающий AND и OR очень хорошо со сравнениями помогает
      // X OR (Y AND Z) если X=>Y равен Y AND (X OR Y), по сути это реверсивная перестановка то есть для and'а в обратную сторону, то есть если ее делать всегда
      // то будет бесконечный цикл, поэтому важно проверить что
      static Where changeMeans(Where where1, Where where2, boolean packExprs) {
          AndObjectWhere[] wheres1 = where1.getAnd();
          AndObjectWhere[] wheres2 = where2.getAnd();
          for(int i=0;i<wheres1.length;i++)
              for(int j=0;j<wheres2.length;j++) {
                  OrObjectWhere[] orWheres = wheres2[j].getOr();
                  for(int k=0;k<orWheres.length;k++) {
                      if(wheres1[i].means(orWheres[k])) { // значит можно поменять местами
                          Where andSiblings = toWhere(siblings(orWheres, k), wheres2[j]);
                          Where orSiblings = toWhere(BaseUtils.add(siblings(wheres1,i),siblings(wheres2,j),instancer));
                          if(!BaseUtils.hashEquals(wheres1[i], // если сокращается хоть что-то, меняем местами
                                  followFalse(wheres1[i],opPlain(opPlain(andSiblings,orWheres[k].not()),orSiblings), FollowDeep.inner(packExprs), false)))
                              return orSiblings.or(orWheres[k].and(wheres1[i].or(andSiblings, packExprs), packExprs), packExprs);
                      }
                  }
              }
          return null;
      }
    */
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

    static class Compare {
        QuickSet<Compare> greater;
        QuickSet<Compare> less;

        Compare() {
            greater = new QuickSet<Compare>();
            less = new QuickSet<Compare>();
            greater.add(this);
            less.add(this);
        }
    }

    static class CompareMap extends SimpleMap<Equal, Compare> {

        Compare getCompare(Equal expr) {
            Compare compare = get(expr);
            if(compare==null) {
                compare = new Compare();
                add(expr,compare);
            }
            return compare;
        }

        boolean add(Equal op1, Equal op2) {
            Compare compare1 = getCompare(op1);
            Compare compare2 = getCompare(op2);

            if(compare1.greater.intersect(compare2.less)) // если пересекаются
                return false;

            for(int i=0;i<compare1.greater.size;i++)
                compare1.greater.get(i).less.addAll(compare2.less);
            for(int i=0;i<compare2.less.size;i++)
                compare2.less.get(i).greater.addAll(compare1.greater);

            return true;
        }
    }

    // на самом деле wheres чисто из object where состоит
    static boolean checkObjectTrue(AndObjectWhere[] wheres, int numWheres) {
        // без checkff
/*        AndObjectWhere[] noFollowWheres = wheres.clone(); int nf = 0;
        for(int i=0;i<numWheres;i++)
            if(noFollowWheres[i]!=null) {
                int j=0;
                for(;j<numWheres;j++)
                    if(i!=j && noFollowWheres[j]!=null) // если !where => followWhere или !followWhere => where то return True.
                        if(noFollowWheres[i] instanceof DataWhere) {
                            if (noFollowWheres[j] instanceof NotWhere && (((NotWhere) noFollowWheres[j]).where).follow((DataWhere) noFollowWheres[i]))
                                return true;
                            if(noFollowWheres[j] instanceof DataWhere && ((DataWhere) noFollowWheres[i]).follow((DataWhere)noFollowWheres[j])) {
                                noFollowWheres[i] = null; nf++;
                                break;
                            }
                        } else
                            if(noFollowWheres[j] instanceof NotWhere && ((NotWhere) noFollowWheres[j]).where.follow(((NotWhere)noFollowWheres[i]).where)) {
                                noFollowWheres[i] = null; nf++;
                                break;
                            }
            }
        if(nf>0) {
            wheres = new AndObjectWhere[numWheres-nf]; numWheres = 0;
            for(int i=0;i<noFollowWheres.length;i++)
                if(noFollowWheres[i]!=null)
                    wheres[numWheres++] = noFollowWheres[i];
        }
  */

//            for(int i=0;i<numWheres;i++)
//                if(wheres[i] instanceof CompareWhere) // если есть хоть один Compare, запускаем дальше чтобы избавится от них
//                    return ((CompareWhere)wheres[i]).checkTrue(siblingsWhere(wheres,i,numWheres));

        // сначала объединим все EqualsWhere в группы - если найдем разные ValueExpr'ы в группе вывалимся, assert что нету CompareWhere
        EqualMap equals = new EqualMap(numWheres*2);
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof NotWhere && ((NotWhere) wheres[i]).where instanceof EqualsWhere) {
                EqualsWhere equalsWhere = (EqualsWhere) ((NotWhere) wheres[i]).where;
                if(!equals.add(equalsWhere.operator1,equalsWhere.operator2)) // противоречивы значит true
                    return true;
            }

        // проверяем классовую логику - если из and'а.getClassWhere() всех not'ов => or всех IsClassWhere тогда это true
        ClassExprWhere classesNot = ClassExprWhere.TRUE;
        ClassExprWhere classesOr = ClassExprWhere.FALSE;
        for(int i=0;i<numWheres;i++) {
            if(wheres[i] instanceof NotWhere)
                classesNot = classesNot.and(((NotWhere)wheres[i]).where.getClassWhere());
            if(wheres[i] instanceof IsClassWhere || wheres[i] instanceof PackClassWhere)
                classesOr = classesOr.or(wheres[i].getClassWhere());
        }
        if(classesNot.andEquals(equals).means(classesOr))
            return true;

        // возьмем все GreaterWhere и построим граф, assert что нету CompareWhere
        CompareMap compare = new CompareMap();
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof NotWhere && ((NotWhere) wheres[i]).where instanceof GreaterWhere) {
                GreaterWhere greaterWhere = (GreaterWhere) ((NotWhere) wheres[i]).where;
                if(!compare.add(equals.getEqual(greaterWhere.operator1),equals.getEqual(greaterWhere.operator2))) // противоречивы значит true;
                    return true;
            }

        // эвристика - для CompareWhere проверяем "единичные" следствия - частный случай самой верхней проверки (CompareWhere.checkTrue)
        for(int i=0;i<numWheres;i++) // вообще говоря исключая логику транзитивности такой подход практически эквивалентен верхней проверке (если только не будет скажем A=B, A>B, B>A)
            if(wheres[i] instanceof CompareWhere) {
                Equal equal1, equal2; Compare compare1, compare2;
                CompareWhere compareWhere = (CompareWhere) wheres[i];
                if((equal1=equals.get(compareWhere.operator1))!=null) {
                    if(compareWhere instanceof EqualsWhere) {
                        if(equal1.contains(compareWhere.operator2))
                            return true;
                    }
                    else
                        if((equal2 = equals.get(compareWhere.operator2))!=null && (compare1 = compare.get(equal1))!=null &&
                                (compare2 = compare.get(equal2))!=null && compare1.greater.contains(compare2))
                            return true;
                }
            }


        return false;
    }

    static boolean checkTrue(AndObjectWhere[] wheres, int numWheres, boolean check) {
        // ищем максимальную по высоте вершину and
        int maxWhere = -1;
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof AndWhere && (maxWhere<0 || wheres[i].getHeight()>wheres[maxWhere].getHeight()))
                maxWhere = i;
        if(maxWhere<0) // значит остались одни ObjectWhere
            return checkObjectTrue(wheres, numWheres);

        Where siblingWhere = toWhere(siblings(wheres, maxWhere, numWheres), check);
        OrObjectWhere[] maxWheres = ((AndWhere)wheres[maxWhere]).wheres.clone();
        for(int i=0;i<maxWheres.length;i++) { // будем бежать с высот поменьше - своего рода пузырьком
            for(int j=maxWheres.length-1;j>i;j--)
                if(maxWheres[j].getHeight()<maxWheres[j-1].getHeight()) {
                    OrObjectWhere t = maxWheres[j];
                    maxWheres[j] = maxWheres[j-1];
                    maxWheres[j-1] = t;
                }
            if(!checkTrue(maxWheres[i],siblingWhere))
                return false;
        }
        return true;
    }

    public AndWhere not = null;
    @ManualLazy
    public AndWhere not() { // именно здесь из-за того что типы надо перегружать без generics
        if(not==null)
            not = new AndWhere(not(wheres), check);
        return not;
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    @ParamLazy
    public Where translate(MapTranslate translator) {
        AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            resultWheres[i] = (AndObjectWhere) wheres[i].translate(translator);
        return toWhere(resultWheres);
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        Where result = Where.FALSE;
        for(Where where : wheres)
            result = result.or(where.translateQuery(translator));
        return result;
    }
    
    // разобъем чисто для оптимизации
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(int i=0;i<wheres.length;i++)
            wheres[i].fillJoinWheres(joins,andWhere.and(siblingsWhere(wheres,i).not()));
    }

    protected String getOp() {
        return "OR";
    }

    public boolean isTrue() {
        return false;
    }

    public boolean isFalse() {
        return wheres.length == 0;
    }

    public boolean checkFormulaTrue() {
        return wheres.length != 0 && checkTrue(wheres, wheres.length, check);
    }

    public boolean twins(AbstractSourceJoin o) {
        return BaseUtils.equalArraySets(wheres, ((OrWhere) o).wheres);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public ObjectJoinSets groupObjectJoinSets() {
        ObjectJoinSets result = new ObjectJoinSets();
        for(Where where : wheres)
            result.or(where.groupObjectJoinSets());
        return result;
    }
    public KeyEquals groupKeyEquals() {
        KeyEquals result = new KeyEquals();
        for(Where where : wheres)
            result.or(where.groupKeyEquals());
        return result;
    }
    public MeanClassWheres calculateMeanClassWheres() {
        MeanClassWheres result = new MeanClassWheres();
        for(Where where : wheres)
            result.or(where.groupMeanClassWheres());
        return result;
    }

    public int hashCoeff() {
        return 5;
    }
}
