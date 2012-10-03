package platform.server.data.where;

import platform.base.*;
import platform.server.Settings;
import platform.server.caches.ManualLazy;
import platform.server.caches.ParamLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.expr.where.extra.GreaterWhere;
import platform.server.data.expr.where.extra.IsClassWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.where.classes.PackClassWhere;

import java.util.List;


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

    public static Where or(Where where1,Where where2,boolean pack) {

        if(where1.isFalse() || where2.isTrue()) return where2;
        if(where2.isFalse() || where1.isTrue()) return where1;

        Where followWhere1 = where1.followFalse(where2, pack, new FollowChange());
        while(true) {
            FollowChange change = new FollowChange();
            Where followWhere2 = where2.followFalse(followWhere1, pack, change);
            if(change.type != FollowType.WIDE && change.type != FollowType.DIFF) {
                // если And'ы или Or'ы то они бы при упаковке внутри ушли бы
                if(followWhere1 instanceof ObjectWhere && followWhere2 instanceof ObjectWhere && checkTrue(where1,where2))
                    return TRUE;

                assert BaseUtils.hashEquals(followWhere1.followFalse(followWhere2, pack, new FollowChange()),followWhere1);
                
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
        Where[] pairedWheres = new Where[wheres1.length]; int pairs = 0; CheckWhere checkPaired = Where.FALSE;
        AndObjectWhere[] rawWheres1 = wheres1.clone();
        AndObjectWhere[] rawWheres2 = wheres2.clone();
        for(int j=0;j<rawWheres1.length;j++) {
            for(int i=0;i<rawWheres2.length;i++)
                if(rawWheres2[i]!=null) {
                    Where pairedWhere = rawWheres2[i].pairs(rawWheres1[j]);

                    if(Settings.instance.isRestructWhereOnMeans() && pairedWhere==null) {
                        // нужно считать sibling'и
                        AndObjectWhere[] unpairedWheres = new AndObjectWhere[wheres2.length+wheres1.length-2*pairs-2]; int numu = 0;
                        for(int k=0;k<rawWheres1.length;k++)
                            if(k!=j && rawWheres1[k]!=null) unpairedWheres[numu++] = rawWheres1[k];
                        for(int k=0;k<rawWheres2.length;k++)
                            if(k!=i && rawWheres2[k]!=null) unpairedWheres[numu++] = rawWheres2[k];
                        CheckWhere orSiblings = orCheck(checkPaired, toWhere(unpairedWheres));
                        
                        pairedWhere = AndWhere.changeMeans(rawWheres2[i], rawWheres1[j], orSiblings, false);
                        if(pairedWhere==null)
                            pairedWhere = AndWhere.changeMeans(rawWheres1[j], rawWheres2[i], orSiblings, false);
                    }

                    if(pairedWhere!=null) {
                        pairedWheres[pairs++] = pairedWhere; if(Settings.instance.isRestructWhereOnMeans()) checkPaired = checkPaired.orCheck(pairedWhere);
                        rawWheres2[i] = null;
                        rawWheres1[j] = null;
                        break;
                    }
                }
        }

        AndObjectWhere[] unpairedWheres = new AndObjectWhere[wheres2.length+wheres1.length-2*pairs]; int numu = 0;
        for(AndObjectWhere andWhere : rawWheres1)
            if(andWhere!=null) unpairedWheres[numu++] = andWhere;
        for(AndObjectWhere andWhere : rawWheres2)
            if(andWhere!=null) unpairedWheres[numu++] = andWhere;
        Where result = toWhere(unpairedWheres);
        for(int i=0;i<pairs;i++)
            result = orPairs(result, pairedWheres[i]);
        return result;
    }

    /*    // Метод переворачивающий AND и OR очень хорошо со сравнениями помогает
      // X OR (Y AND Z) если X=>Y равен Y AND (X OR Z), по сути это реверсивная перестановка то есть для and'а в обратную сторону, то есть если ее делать всегда, то будет бесконечный цикл
      // смысл ее в том что для X.FF будет более общее условие ff и нужно проверить если оно уменьшает X то переставить (при этом без рекурсивных changeMeans)
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
                                wheres1[i].followFalse(orCheck(orCheck(andSiblings,orWheres[k].not()),orSiblings), packExprs, new FollowChange())))
                            return orSiblings.or(orWheres[k].and(wheres1[i].or(andSiblings, packExprs), packExprs), packExprs);
                    }
                }
            }
        return null;
      }
    */
    private static CheckWhere orCheckNull(CheckWhere where1, CheckWhere where2) {
        if(where2==null)
            return where1;
        else
            return orCheck(where1,where2);        
    }

    public Where followFalse(CheckWhere falseWhere, boolean pack, FollowChange change) {
        if(isFalse()) return this;
        if(falseWhere.isFalse() && !pack) return this;
        if(falseWhere.isTrue()) {
            change.type = FollowType.NARROW;
            return FALSE;
        }

        // нужно минимизировать orCheck'и
        int N = 2; while(N<wheres.length) N = N*2;
        CheckWhere[] siblingNewWheres = new CheckWhere[N * 2]; // хранит матрицы по компонентам

        FollowType changeType = FollowType.EQUALS;

        int in=0; // количество сколько изменилось
        int ilast = -1; // какой элемент пропускfnm
        for(int i=0;i<wheres.length;i++) {
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
                Where followWhere = (siblingNewWheres[Ni]==null?wheres[i]:(Where)siblingNewWheres[Ni]).followFalse(orCheck(siblingWhere, falseWhere), pack, followChange);
                changeType = changeType.or(followChange.type);
                if(followChange.type!=FollowType.EQUALS) { // если шире стал, изменился, и даже если narrow'е стал (потому как может каскад вызвать)
                    if(followWhere.isTrue()) {
                        change.type = FollowType.WIDE;
                        return TRUE; // простое ускорение
                    }

                    if(siblingNewWheres[Ni]==null) in++;
                    siblingNewWheres[Ni] = followWhere;
                    while(Ni/2>1) { // пересчитываем sibling'и new
                        siblingNewWheres[Ni/2] = orCheckNull(siblingNewWheres[Ni], siblingNewWheres[(Ni%2==0?Ni+1:Ni-1)]);
                        Ni=Ni/2;
                    }

                    if(followChange.type!=FollowType.NARROW) { // начинаем заново, потому как могло повлиять на уже обработанные условия
                        ilast=i; i=-1; }
                }
            }
        }

        // если остались одни Object'ы то проверить на checkTrue
        boolean onlyObjects = true;
        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length-in]; int sn = 0;
        for(int j=0;j<wheres.length;j++)
            if(siblingNewWheres[N+j]!=null)
                onlyObjects = onlyObjects && (siblingNewWheres[N+j].isFalse() || siblingNewWheres[N+j] instanceof ObjectWhere);
            else {
                staticWheres[sn++] = wheres[j];
                onlyObjects = onlyObjects && wheres[j] instanceof ObjectWhere;
            }
        Where result = toWhere(staticWheres, check);
        if(onlyObjects && checkTrue(orCheckNull(orCheckNull(result,siblingNewWheres[2]),siblingNewWheres[3]),falseWhere)) {
            change.type = FollowType.WIDE;
            return TRUE;
        }
        for(int j=0;j<wheres.length;j++)
            if(siblingNewWheres[N+j]!=null)
                result = orPairs(result, (Where) siblingNewWheres[N+j]);

        change.type = changeType;
        if(changeType==FollowType.EQUALS) { // для оптимизации сохраняем ссылку
            assert BaseUtils.hashEquals(result, this);
            return this;
        } else
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

    public static CheckWhere prevOrCheck(CheckWhere where1,CheckWhere where2) {
        CheckWhere followWhere1 = Settings.instance.isCheckFollowsWhenObjects() ?where1:checkff(where1, where2);
        CheckWhere followWhere2 = Settings.instance.isCheckFollowsWhenObjects() ?where2:checkff(where2, followWhere1);

        if(followWhere1.isFalse() || followWhere2.isTrue()) return followWhere2;
        if(followWhere2.isFalse() || followWhere1.isTrue()) return followWhere1;

        return toWhere(BaseUtils.add(followWhere1.getAnd(), followWhere2.getAnd(), instancer), true);
    }

/*    public static CheckWhere orCheck(CheckWhere where1,CheckWhere where2) {
        CheckWhere newCheck = newOrCheck(where1, where2);
        assert BaseUtils.hashEquals(newCheck, prevOrCheck(where1, where2));
        return newCheck;
    }*/

    // из двух where в неправильной форме, делает еще одно where в неправильной форме
    // ничего не перестраивает цель такого CheckWhere найти первый попавшийся not Null элемент и пометиться как не true
    // выполняет основные проверки на directMeansFrom
    public static CheckWhere orCheck(CheckWhere where1,CheckWhere where2) {
        
        if(where1.isFalse() || where2.isTrue()) return where2;
        if(where2.isFalse() || where1.isTrue()) return where1;

        // COPY PASTE с модификациями для OrWhere.orCheck(w1, w2), OrWhere.checkTrue(w[], int, boolean) - для оптимизации как самые критичные места
        if(where2 instanceof OrObjectWhere)
            if (where1.directMeansFrom(((OrObjectWhere) where2).not()))
                return Where.TRUE;

        if(where1 instanceof OrObjectWhere) // если Or то проверим, если And нет смысла, все равно скобки потом отдельно будут раскрываться
            if(where2.directMeansFrom(((OrObjectWhere) where1).not()))
                return Where.TRUE;

        AndObjectWhere[] wheres1 = where1.getAnd();
        AndObjectWhere[] wheres2 = where2.getAnd();
        AndObjectWhere[] rawMerged = new AndObjectWhere[wheres2.length+wheres1.length]; int mnum = 0;

        for(AndObjectWhere andWhere2 : wheres2)
            if(!where1.directMeansFrom(andWhere2))
                rawMerged[mnum++] = andWhere2;

        if(mnum == 0)
            return where1;

        int clean2 = mnum;
        for (AndObjectWhere andWhere1 : wheres1) {
            int j = 0;
            while (j < clean2) {// бежим не по всем, а только по не вычишенным
                if (rawMerged[j].directMeansFrom(andWhere1)) // sibling не нужен
                    break;
                j++;
            }
            if (j == clean2) // не нашли
                rawMerged[mnum++] = andWhere1;
        }

        if(mnum==clean2)
            return where2;
        if(mnum==wheres2.length+wheres1.length)
            return toWhere(rawMerged, true);
        AndObjectWhere[] merged = new AndObjectWhere[mnum]; System.arraycopy(rawMerged, 0, merged, 0, mnum);
        return toWhere(merged, true);
    }

    public static boolean checkTrue(CheckWhere where1, CheckWhere where2) {
        return orCheck(where1, where2).checkTrue();        
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

    // для проверки - без механизма used
    // на самом деле wheres чисто из object where состоит
    static boolean prevCheckObjectTrue(AndObjectWhere[] wheres, int numWheres) {
        if(Settings.instance.isCheckFollowsWhenObjects()) {
            AndObjectWhere[] noFollowWheres = wheres.clone(); int nf = 0;
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
        }

        if(!Settings.instance.isSimpleCheckCompare())
            for(int i=0;i<numWheres;i++)
                if(wheres[i] instanceof CompareWhere) // если есть хоть один Compare, запускаем дальше чтобы избавится от них
                    return ((CompareWhere)wheres[i]).checkTrue(toWhere(siblings(wheres,i,numWheres),true));

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
                if(!greaterWhere.orEquals && !compare.add(equals.getEqual(greaterWhere.operator1),equals.getEqual(greaterWhere.operator2))) // противоречивы значит true;
                    return true;
            }

        if(!Settings.instance.isSimpleCheckCompare()) { // эвристика - для CompareWhere проверяем "единичные" следствия - частный случай самой верхней проверки (CompareWhere.checkTrue)
            for(int i=0;i<numWheres;i++) // вообще говоря исключая логику транзитивности такой подход практически эквивалентен верхней проверке (если только не будет скажем A=B, A>B, B>A)
                if(wheres[i] instanceof CompareWhere) {
                    Equal equal1, equal2; Compare compare1, compare2;
                    CompareWhere compareWhere = (CompareWhere) wheres[i];
                    if((equal1=equals.get(compareWhere.operator1))!=null) {
                        if(compareWhere instanceof EqualsWhere) {
                            if(equal1.contains(compareWhere.operator2))
                                return true;
                        } else {
                            assert compareWhere instanceof GreaterWhere;
                            if(!((GreaterWhere)compareWhere).orEquals && (equal2 = equals.get(compareWhere.operator2))!=null && !equal1.equals(equal2) && (compare1 = compare.get(equal1))!=null &&
                                    (compare2 = compare.get(equal2))!=null && compare1.greater.contains(compare2))
                                return true;
                        }
                    }
                }
        }

        return false;
    }

    // для проверки - без механизма used
    static boolean prevCheckTrue(AndObjectWhere[] wheres, int numWheres, boolean check) {
        // ищем максимальную по высоте вершину and
        int maxWhere = -1;
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof AndWhere && (maxWhere<0 || wheres[i].getHeight()>wheres[maxWhere].getHeight()))
                maxWhere = i;
        if(maxWhere<0) // значит остались одни ObjectWhere
            return prevCheckObjectTrue(wheres, numWheres);

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

    // на самом деле wheres чисто из object where состоит
    static boolean[] checkObjectTrue(AndObjectWhere[] wheres, int numWheres) {
/*        if(Settings.instance.isCheckFollowsWhenObjects()) {
            AndObjectWhere[] noFollowWheres = wheres.clone(); int nf = 0;
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
        }
  */
        /*       if(!Settings.instance.isSimpleCheckCompare())
                 for(int i=0;i<numWheres;i++)
                     if(wheres[i] instanceof CompareWhere) // если есть хоть один Compare, запускаем дальше чтобы избавится от них
                         return ((CompareWhere)wheres[i]).checkTrue(toWhere(siblings(wheres,i,numWheres),true));
        */

        boolean[] result = new boolean[numWheres];

        // проверяем классовую логику - если из and'а.getClassWhere() всех not'ов => or всех IsClassWhere тогда это true
        ClassExprWhere classesNot = ClassExprWhere.TRUE;
        for(int i=0;i<numWheres;i++) {
            if(wheres[i] instanceof NotWhere) {
                ClassExprWhere andClassesNot = classesNot.and(((NotWhere) wheres[i]).where.getClassWhere());
                if(!BaseUtils.hashEquals(andClassesNot, classesNot)) {
                    result[i] = true;
                    classesNot = andClassesNot;
                }
            }
        }
        if(classesNot.isFalse())
            return result;

        // сначала объединим все EqualsWhere в группы - если найдем разные ValueExpr'ы в группе вывалимся, assert что нету CompareWhere
        EqualMap equals = new EqualMap(numWheres*2);
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof NotWhere && ((NotWhere) wheres[i]).where instanceof EqualsWhere) {
                EqualsWhere equalsWhere = (EqualsWhere) ((NotWhere) wheres[i]).where;
                result[i] = true;
                if(!equals.add(equalsWhere.operator1,equalsWhere.operator2)) // противоречивы значит true
                    return result;
            }

        ClassExprWhere equalsClassesNot = classesNot.andEquals(equals);
        if(equalsClassesNot.isFalse())
            return result;

        ClassExprWhere classesOr = ClassExprWhere.FALSE;
        for(int i=0;i<numWheres;i++) {
            if(wheres[i] instanceof IsClassWhere || wheres[i] instanceof PackClassWhere) {
                ClassExprWhere orClassesOr = classesOr.or(wheres[i].getClassWhere());
                if(!BaseUtils.hashEquals(orClassesOr, classesOr)) {
                    result[i] = true;
                    classesOr = orClassesOr;
                }
            }
        }
        if(equalsClassesNot.means(classesOr))
            return result;

        // возьмем все GreaterWhere и построим граф, assert что нету CompareWhere
        CompareMap compare = new CompareMap();
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof NotWhere && ((NotWhere) wheres[i]).where instanceof GreaterWhere) {
                GreaterWhere greaterWhere = (GreaterWhere) ((NotWhere) wheres[i]).where;
                result[i] = true;
                if(!greaterWhere.orEquals && !compare.add(equals.getEqual(greaterWhere.operator1),equals.getEqual(greaterWhere.operator2))) // противоречивы значит true;
                    return result;
            }

        /*       if(!Settings.instance.isSimpleCheckCompare()) { // эвристика - для CompareWhere проверяем "единичные" следствия - частный случай самой верхней проверки (CompareWhere.checkTrue)
            for(int i=0;i<numWheres;i++) // вообще говоря исключая логику транзитивности такой подход практически эквивалентен верхней проверке (если только не будет скажем A=B, A>B, B>A)
                if(wheres[i] instanceof CompareWhere) {
                    Equal equal1, equal2; Compare compare1, compare2;
                    CompareWhere compareWhere = (CompareWhere) wheres[i];
                    if((equal1=equals.get(compareWhere.operator1))!=null) {
                        if(compareWhere instanceof EqualsWhere) {
                            if(equal1.contains(compareWhere.operator2))
                                return true;
                        } else {
                            assert compareWhere instanceof GreaterWhere;
                            if(!((GreaterWhere)compareWhere).orEquals && (equal2 = equals.get(compareWhere.operator2))!=null && !equal1.equals(equal2) && (compare1 = compare.get(equal1))!=null &&
                                    (compare2 = compare.get(equal2))!=null && compare1.greater.contains(compare2))
                                return true;
                        }
                    }
                }
        }*/

        return null;
    }

    static boolean[] checkTrue(AndObjectWhere[] wheres, int numWheres, boolean check) {
        // ищем максимальную по высоте вершину and
        int maxWhere = -1;
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof AndWhere && (maxWhere<0 || wheres[i].getHeight()>wheres[maxWhere].getHeight()))
                maxWhere = i;
        if(maxWhere<0) // значит остались одни ObjectWhere
            return checkObjectTrue(wheres, numWheres);

        boolean[] result = null;

        AndObjectWhere[] siblings = siblings(wheres, maxWhere, numWheres);
        Where siblingWhere = toWhere(siblings, check);

        OrObjectWhere[] maxWheres = ((AndWhere)wheres[maxWhere]).wheres.clone();
        for(int i=0;i<maxWheres.length;i++) {
            for(int j=maxWheres.length-1;j>i;j--) // будем бежать с высот поменьше - своего рода пузырьком
                if(maxWheres[j].getHeight()<maxWheres[j-1].getHeight()) {
                    OrObjectWhere t = maxWheres[j];
                    maxWheres[j] = maxWheres[j-1];
                    maxWheres[j-1] = t;
                }

            OrObjectWhere bracketWhere = maxWheres[i];
            AndObjectWhere[] brackets = bracketWhere.getAnd();

            // COPY PASTE с модификациями для OrWhere.orCheck(w1, w2), OrWhere.checkTrue(w[], int, boolean) - для оптимизации как самые критичные места
            int is=0;
            while(is<siblings.length) {
                if(siblings[is].directMeansFrom(bracketWhere.not()))
                    break;
                is++;
            }
            if(is<siblings.length) { // нашли
                if(result==null)
                    result = new boolean[numWheres];
                result[is<maxWhere?is:is+1] = true;
                continue;
            }

            if(siblingWhere instanceof OrObjectWhere) // если Or то проверим, если And нет смысла, все равно скобки потом отдельно будут раскрываться
                if(bracketWhere.directMeansFrom(((OrObjectWhere)siblingWhere).not())) {
                    if(result==null)
                        result = new boolean[numWheres];
                    for(int j=0;j<numWheres;j++)
                        result[j] = true;
                    continue;
                }

            AndObjectWhere[] mergeBrackets = new AndObjectWhere[brackets.length+siblings.length]; int mnum = 0;

            for(AndObjectWhere bracket : brackets)
                if(!siblingWhere.directMeansFrom(bracket))
                    mergeBrackets[mnum++] = bracket;

            boolean[] droppedSiblings = null;
            int cleanBrackets = mnum;
            for(int s=0;s<siblings.length;s++) {
                AndObjectWhere sibling = siblings[s];
                int j=0;
                while(j<cleanBrackets) {// бежим не по всем, а только по не вычишенным
                    if(mergeBrackets[j].directMeansFrom(sibling)) { // sibling не нужен
                        if(droppedSiblings==null)
                            droppedSiblings = new boolean[siblings.length];
                        droppedSiblings[s] = true;
                        break;
                    }
                    j++;
                }
                if(j==cleanBrackets) // не нашли*/
                    mergeBrackets[mnum++] = sibling;
            }

            boolean[] used = checkTrue(mergeBrackets, mnum, true); // вызываем рекурсию
            if(used==null)
                return null;

            boolean thisUsed = false; // смотрим использовалась ли эта скобка
            for(int j=0;j<cleanBrackets;j++)
                if(used[j]) {
                    thisUsed = true;
                    break;
                }

            if(result==null || !thisUsed) // если текущая скобка не использовалась, старые убираем
                result = new boolean[numWheres];
            int cs = 0;
            for(int j=cleanBrackets;j<mnum;j++) {
                while((droppedSiblings!=null && droppedSiblings[cs])) cs++; // assert что cs меньше result.length
                if(used[j])
                    result[cs<maxWhere?cs:cs+1] = true;
                cs++;
            }
            if(!thisUsed)
                return result;
        }

        result[maxWhere] = true;
        return result;
    }


    public AndWhere not = null;
    @ManualLazy
    public AndWhere not() { // именно здесь из-за того что типы надо перегружать без generics
        if(not==null) {
            not = new AndWhere(not(wheres), check);
            not.not = this; // для оптимизации
        }
        return not;
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected Where translate(MapTranslate translator) {
        AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            resultWheres[i] = (AndObjectWhere) wheres[i].translateOuter(translator);
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
        if(wheres.length==0)
            return false;

        boolean result = checkTrue(wheres, wheres.length, check)!=null;
//        assert prevCheckTrue(wheres, wheres.length, check)==result;
        return result;
    }

    public boolean twins(TwinImmutableInterface o) {
        return BaseUtils.equalArraySets(wheres, ((OrWhere) o).wheres);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected <K extends BaseExpr> GroupJoinsWheres calculateGroupJoinsWheres(QuickSet<K> keepStat, KeyStat keyStat, List<Expr> orderTop, boolean noWhere) {
        GroupJoinsWheres result = new GroupJoinsWheres(noWhere);
        for(Where where : wheres)
            result.or(where.groupJoinsWheres(keepStat, keyStat, orderTop, noWhere));
        return result;
    }
    public KeyEquals calculateGroupKeyEquals() {
        KeyEquals result = new KeyEquals();
        for(Where where : wheres)
            result.addAll(where.getKeyEquals());
        return result;
    }
    public MeanClassWheres calculateGroupMeanClassWheres(boolean useNots) {
        MeanClassWheres result = new MeanClassWheres();
        for(Where where : wheres)
            result.or(where.groupMeanClassWheres(useNots));
        return result;
    }

    public int hashCoeff() {
        return 5;
    }

    public boolean isNot() {
        return false;
    }
}
