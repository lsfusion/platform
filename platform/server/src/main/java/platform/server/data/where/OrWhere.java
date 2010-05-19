package platform.server.data.where;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.SimpleMap;
import platform.server.caches.ParamLazy;
import platform.server.caches.ManualLazy;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.PackClassWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.JoinData;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.*;


public class OrWhere extends FormulaWhere<AndObjectWhere> implements OrObjectWhere<AndWhere> {

    // вообще надо противоположные + Object, но это наследованием не сделаешь
    OrWhere(AndObjectWhere[] iWheres) {
        super(iWheres);
    }
    OrWhere() {
        super(new AndObjectWhere[0]);
    }

    public final static ArrayInstancer<AndObjectWhere> instancer = new ArrayInstancer<AndObjectWhere>() {
        public AndObjectWhere[] newArray(int size) {
            return new AndObjectWhere[size];
        }
    };

    public static Where op(Where where1,Where where2,FollowDeep followDeep) {

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
                    Where pairedWhere = rawWheres2[i].pairs(andWhere1, followDeep);
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

        if(unpairedWhere1.getHeight()<unpairedWhere2.getHeight()) { // поменяем местами чтобы поменьше высота была
            Where t = unpairedWhere1; unpairedWhere1 = unpairedWhere2; unpairedWhere2 = t; }

        // делаем followFalse друг друга
        Where resultWhere;

        Where followWhere1 = followFalse(unpairedWhere1,unpairedWhere2,followDeep,false);
        if(followWhere1.isTrue())
            resultWhere = followWhere1;
        else { // точно NOT unpairedWhere2 OR followWhere1 == TRUE потому как иначе unpairedWhere1 OR unpairedWhere2 == TRUE и тогда followWhere1.isTrue
            Where followWhere2 = followFalse(unpairedWhere2,followWhere1,followDeep,true);
            if(followDeep==FollowDeep.PLAIN || (BaseUtils.hashEquals(followWhere1,unpairedWhere1) && BaseUtils.hashEquals(followWhere2,unpairedWhere2))) { // если совпали follow'ы то все отлично
                resultWhere = null;
                if(followDeep==FollowDeep.PLAIN) {
                    if(followWhere2.isTrue())
                        resultWhere = followWhere2;
                } else {
                    resultWhere = changeMeans(followWhere1,followWhere2,followDeep==FollowDeep.PACK);
                    if(resultWhere==null) // пробуем в обратную сторону
                        resultWhere = changeMeans(followWhere2,followWhere1,followDeep==FollowDeep.PACK);
                }
                if(resultWhere==null)
                    resultWhere = toWhere(BaseUtils.add(followWhere1.getAnd(),followWhere2.getAnd(),instancer));
            } else // иначе погнали еще раз or может новые скобки появились
                resultWhere = op(followWhere1,followWhere2,followDeep);
        }

        for(int i=0;i<pairs;i++)
            resultWhere = op(resultWhere,pairedWheres[i],followDeep);

        return resultWhere;
    }

    // Метод переворачивающий AND и OR очень хорошо со сравнениями помогает
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
                        Where andSiblings = siblingsWhere(orWheres,k);
                        Where orSiblings = toWhere(BaseUtils.add(siblings(wheres1,i),siblings(wheres2,j),instancer));
                        if(!BaseUtils.hashEquals(wheres1[i], // если сокращается хоть что-то, меняем местами
                                followFalse(wheres1[i],op(op(andSiblings,orWheres[k].not(),FollowDeep.PLAIN),orSiblings,FollowDeep.PLAIN),FollowDeep.inner(packExprs),false)))
                            return orSiblings.or(orWheres[k].and(wheres1[i].or(andSiblings)));
                    }
                }
            }
        return null;
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

    // plainFollow: 0 - не идти внутрь, 1 - идти внутрь 
    static Where followFalse(Where followWhere, Where falseWhere, FollowDeep followDeep, boolean sureNotTrue) {

        assert !(followDeep==FollowDeep.INNER && sureNotTrue && orTrue(followWhere,falseWhere)); // проверим sureNotTrue, из-за packFollowFalse Linear, GroupExpr пока иногда нарушается 

        if(falseWhere.isTrue()) return FALSE;
        if(falseWhere.isFalse()) return followWhere;
//        if(!decomposed(followWhere,falseWhere)) return followWhere;

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
                if(falseWhere.directMeansFrom(((ObjectWhere)where).not())) // проверяем если not возвращаем true
                    return TRUE;
                staticWheres[statics++] = where;
            } else { // значит AndWhere
                boolean isFalse = false;
                OrObjectWhere[] followWheres = new OrObjectWhere[((AndWhere)where).wheres.length]; int follows = 0;
                for(OrObjectWhere orWhere : ((AndWhere)where).wheres) {
                    if(orWhere instanceof ObjectWhere && falseWhere.directMeansFrom((ObjectWhere)orWhere)) { // если из операнда следует false drop'аем весь orWhere
                        isFalse = true;
                        break;
                    }
                    if(!falseWhere.directMeansFrom(orWhere.not())) // если из not следует один из false'ов
                        followWheres[follows++] = orWhere;
                }
                if(!isFalse) {
                    if(follows==0) // остался True значит и результат true
                        return TRUE;

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
                result = op(result,changedWheres[i], followDeep);
            return followFalse(result,falseWhere, followDeep,sureNotTrue);
        } else {
            AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length]; int results = 0;
            for(int i=0;i<statics;i++)
                if(!falseWhere.directMeansFrom(staticWheres[i])) // вычистим из where элементы которые заведомо false
                    resultWheres[results++] = staticWheres[i];
            Where result = toWhere(resultWheres,results);
            if(followDeep==FollowDeep.PLAIN || result.isFalse()) // если глубже не надо выходим
                return result;
            else // иначе погнали основной цикл + по sibling'ам
                return result.innerFollowFalse(falseWhere, sureNotTrue, followDeep == FollowDeep.PACK);
        }
    }

    public Where innerFollowFalse(Where falseWhere, boolean sureNotTrue, boolean packExprs) {

         if(!sureNotTrue) {
             if(orTrue(this,falseWhere))
                return TRUE;
             sureNotTrue = true;
         }

        Where followWhere = FALSE;
        AndObjectWhere[] staticWheres = wheres;
        int current = 0;
        for(AndObjectWhere where : wheres) { // после упрощения важно использовать именно этот элемент, иначе неправильно работать будет
            AndObjectWhere[] siblingWheres = siblings(staticWheres, current);
            Where followAndWhere = followFalse(where,op(op(toWhere(siblingWheres),followWhere,FollowDeep.PLAIN),falseWhere,FollowDeep.PLAIN),FollowDeep.inner(packExprs),sureNotTrue);
            if(!BaseUtils.hashEquals(followAndWhere,where)) { // если изменился static "перекидываем" в result
                followWhere = op(followWhere,followAndWhere,FollowDeep.inner(packExprs));
                staticWheres = siblingWheres;
                sureNotTrue = false; // при изменении follow мог упаковаться и соответственно изменится и уже быть true
            } else
                current++;
        }
        return op(toWhere(staticWheres),followWhere,FollowDeep.inner(packExprs));
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

    static boolean checkTrue(AndObjectWhere[] wheres, int numWheres) {
        // ищем максимальную по высоте вершину and
        int maxWhere = -1;
        for(int i=0;i<numWheres;i++)
            if(wheres[i] instanceof AndWhere && (maxWhere<0 || wheres[i].getHeight()>wheres[maxWhere].getHeight()))
                maxWhere = i;
        if(maxWhere<0) { // значит остались одни ObjectWhere
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

        Where siblingWhere = siblingsWhere(wheres, maxWhere, numWheres);
        OrObjectWhere[] maxWheres = ((AndWhere)wheres[maxWhere]).wheres.clone();
        for(int i=0;i<maxWheres.length;i++) { // будем бежать с высот поменьше - своего рода пузырьком
            for(int j=maxWheres.length-1;j>i;j--)
                if(maxWheres[j].getHeight()<maxWheres[j-1].getHeight()) {
                    OrObjectWhere t = maxWheres[j];
                    maxWheres[j] = maxWheres[j-1];
                    maxWheres[j-1] = t;
                }
            if(!orTrue(maxWheres[i],siblingWhere))
                return false;
        }
        return true;
    }

    public static boolean orTrue(Where where1,Where where2) {
        return op(where1,where2,FollowDeep.PLAIN).checkTrue();
    }

    public boolean checkTrue() {
        if(wheres.length==0) return false;

/*        // разбиваем на компоненты
        // компонента определяется просто (OrObjectWhere) A связан с B если есть oA V oB = TRUE - table intersect followNot или followData intersect not
        // будем вытаскивать компоненты по очереди, пока не закончатся все элементы
        AndObjectWhere[][] components = new AndObjectWhere[where.length][]; int numComp = 0;
        int queue = 0;
        int[] nums = new int[where.length]; int[] heights = new int[where.length];
        AndObjectWhere[] wherePool = where.clone();
        int first = 0;
        AndObjectWhere[] component = new AndObjectWhere[where.length]; int numWheres = 1;
        component[0] = where[0]; int heightComponent = where[0].getHeight();
        while(true) {
            for(int i=first+1;i<wherePool.length;i++)
                if(wherePool[i]!=null && component[queue].getObjects().linked(wherePool[i].getObjects())) {
                    component[numWheres++] = wherePool[i];
                    int heightWhere = wherePool[i].getHeight();
                    if(heightWhere > heightComponent) heightComponent = heightWhere;
                    wherePool[i] = null;
                }
            queue++;
            if(queue >= numWheres) { // в очереди никого нету
                components[numComp] = component; nums[numComp] = numWheres; heights[numComp] = heightComponent; numComp++; // сохраняем
                first++; while(first<wherePool.length && wherePool[first]==null) first++; // ищем первый необработанный элемент
                if(first>=wherePool.length) break; // больше нету
                component = new AndObjectWhere[where.length]; numWheres = 1;
                component[0] = wherePool[first]; heightComponent = wherePool[first].getHeight();
                queue = 0;
            }
        }

        for(int i=0;i<numComp;i++) { // бежим по высотам поменьше
            for(int j=numComp-1;j>i;j--)
                if(heights[j]<heights[j-1]) {
                    AndObjectWhere[] tc = components[j]; components[j] = components[j-1]; components[j-1] = tc;
                    int th = heights[j]; heights[j] = heights[j-1]; heights[j-1] = th;
                    int tn = nums[j]; nums[j] = nums[j-1]; nums[j-1] = tn;
                }
            if(checkTrue(components[i],nums[i]))
                return true;
        }
        return false; */

        return checkTrue(wheres, wheres.length);
    }

    public AndWhere not = null;
    @ManualLazy
    public AndWhere not() { // именно здесь из-за того что типы надо перегружать без generics
        if(not==null)
            not = new AndWhere(not(wheres));
        return not;
    }

    public AndObjectWhere[] newArray(int length) {
        return new AndObjectWhere[length];
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    @ParamLazy
    public Where translateDirect(DirectTranslator translator) {
        AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            resultWheres[i] = (AndObjectWhere) wheres[i].translateDirect(translator);
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

    String getOp() {
        return "OR";
    }

    public boolean isTrue() {
        return false;
    }

    public boolean isFalse() {
        return wheres.length==0;
    }

    public boolean twins(AbstractSourceJoin o) {
        return BaseUtils.equalArraySets(wheres, ((OrWhere) o).wheres);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public InnerJoins getInnerJoins() {
        InnerJoins result = new InnerJoins();
        for(Where where : wheres)
            result.or(where.getInnerJoins());
        return result;
    }
    public MeanClassWheres calculateMeanClassWheres() {
        MeanClassWheres result = new MeanClassWheres();
        for(Where where : wheres)
            result.or(where.getMeanClassWheres());
        return result;
    }

    int hashCoeff() {
        return 5;
    }
}
