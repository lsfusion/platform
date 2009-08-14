package platform.server.where;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.SimpleMap;
import platform.base.ArrayInstancer;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.MeanClassWhere;
import platform.server.data.classes.where.MeanClassWheres;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.JoinData;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.wheres.*;
import platform.server.caches.ParamLazy;


public class OrWhere extends FormulaWhere<AndWhere,AndObjectWhere> implements OrObjectWhere<AndWhere> {

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

    public static Where op(Where where1,Where where2,boolean plainFollow) {

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

        if(unpairedWhere1.getHeight()<unpairedWhere2.getHeight()) { // поменяем местами чтобы поменьше высота была
            Where t = unpairedWhere1; unpairedWhere1 = unpairedWhere2; unpairedWhere2 = t; }

        // делаем followFalse друг друга
        Where resultWhere;

        Where followWhere1 = followFalse(unpairedWhere1,unpairedWhere2,plainFollow,false);
        if(followWhere1.isTrue())
            resultWhere = followWhere1;
        else { // точно NOT unpairedWhere2 OR followWhere1 == TRUE потому как иначе unpairedWhere1 OR unpairedWhere2 == TRUE и тогда followWhere1.isTrue
            Where followWhere2 = followFalse(unpairedWhere2,followWhere1,plainFollow,true);
            if(plainFollow || (BaseUtils.hashEquals(followWhere1,unpairedWhere1) && BaseUtils.hashEquals(followWhere2,unpairedWhere2))) { // если совпали follow'ы то все отлично
                resultWhere = null;
                if(!plainFollow) {
                    resultWhere = changeMeans(followWhere1,followWhere2);
                    if(resultWhere==null) // пробуем в обратную сторону
                        resultWhere = changeMeans(followWhere2,followWhere1);
                }
                if(resultWhere==null)
                    resultWhere = toWhere(BaseUtils.add(followWhere1.getAnd(),followWhere2.getAnd(),instancer));
            } else // иначе погнали еще раз or может новые скобки появились
                resultWhere = op(followWhere1,followWhere2,false);
        }

        for(int i=0;i<pairs;i++)
            resultWhere = op(resultWhere,pairedWheres[i],plainFollow);

        return resultWhere;
    }

    // Метод переворачивающий AND и OR очень хорошо со сравнениями помогает
    // X OR (Y AND Z) если X=>Y равен Y AND (X OR Y), по сути это реверсивная перестановка то есть для and'а в обратную сторону, то есть если ее делать всегда
    // то будет бесконечный цикл, поэтому важно проверить что
    static Where changeMeans(Where where1, Where where2) {
        AndObjectWhere[] wheres1 = where1.getAnd();
        AndObjectWhere[] wheres2 = where2.getAnd();
        for(int i=0;i<wheres1.length;i++)
            for(int j=0;j<wheres2.length;j++) {
                OrObjectWhere[] orWheres = wheres2[j].getOr();
                for(int k=0;k<orWheres.length;k++) {
                    if(wheres1[i].means(orWheres[k])) { // значит можно поменять местами
                        Where andSiblings = siblingsWhere(orWheres,k);
                        Where orSiblings = toWhere(BaseUtils.add(siblings(wheres1,i),siblings(wheres2,j),instancer));
                        if(!BaseUtils.hashEquals(wheres1[i].followFalse(op(op(andSiblings,orWheres[k].not(),true),orSiblings,true)),
                                wheres1[i])) // если сокращается хоть что-то, меняем местами
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

    static Where followFalse(Where followWhere, Where falseWhere, boolean plainFollow, boolean sureNotTrue) {

        assert !(!plainFollow && sureNotTrue && orTrue(followWhere,falseWhere)); // проверим sureNotTrue

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
                if(falseWhere.directMeansFrom(((ObjectWhere<?>)where).not())) // проверяем если not возвращаем true
                    return TRUE;
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
                result = op(result,changedWheres[i],plainFollow);
            return followFalse(result,falseWhere,plainFollow,sureNotTrue);
        } else {
            AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length]; int results = 0;
            for(int i=0;i<statics;i++)
                if(!falseWhere.directMeansFrom(staticWheres[i])) // вычистим из wheres элементы которые заведомо false
                    resultWheres[results++] = staticWheres[i];
            Where result = toWhere(resultWheres,results);
            if(plainFollow || result.isFalse()) // если глубже не надо выходим
                return result;
            else // иначе погнали основной цикл + по sibling'ам
                return result.innerFollowFalse(falseWhere, sureNotTrue);
        }
    }

    public Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects) {
        AndObjectWhere[] staticWheres = new AndObjectWhere[wheres.length]; int stat = 0;
        Where[] decomposedWheres = new Where[wheres.length]; int decomp = 0;
        AndObjectWhere[] maxWheres = wheres.clone();
        for(int i=0;i<maxWheres.length;i++) { // будем бежать с высот поменьше - своего рода пузырьком
            for(int j=maxWheres.length-1;j>i;j--)
                if(maxWheres[j].getHeight()<maxWheres[j-1].getHeight()) {
                    AndObjectWhere t = maxWheres[j];
                    maxWheres[j] = maxWheres[j-1];
                    maxWheres[j-1] = t;
                }
            Where decomposedWhere = maxWheres[i].decompose(decompose,objects);
            if(decomposedWhere == maxWheres[i])
                staticWheres[stat++] = maxWheres[i];
            else {
                if(decomposedWhere.isTrue()) return decomposedWhere;
                decomposedWheres[decomp++] = decomposedWhere;
            }
        }

        if(stat < wheres.length) {
            Where result = toWhere(staticWheres,stat);
            for(int i=0;i<decomp;i++)
                result = op(result,decomposedWheres[i],true);
            return result;
        } else
            return this;
    }

    // вычищает те Or'ы которые заведомо не будут false
    static Where decomposeFalse(Where where,Where toFalse) {

        AndObjectWhere[] wheres = where.getAnd();

        // toFalse может менять знаки
        ObjectWhereSet decompose = new ObjectWhereSet(toFalse.getObjects());
        decompose.addAll(toFalse.not().getObjects());

        // для всех or'ов делаем decompose, получаем доп. objects
        Where[] decomposedWheres = new Where[wheres.length];
        ObjectWhereSet[] objectWheres = new ObjectWhereSet[wheres.length];
        for(int i=0;i<wheres.length;i++) {
            objectWheres[i] = new ObjectWhereSet();
            decomposedWheres[i] = wheres[i].decompose(decompose,objectWheres[i]);
        }

        AndObjectWhere[] rawResult = new AndObjectWhere[wheres.length]; int resnum = 0;

        // разбиваем на компоненты по правилу (доп. objects linked or.getObjects)
        int queue = 0; int first = 0;
        int[] component = new int[wheres.length]; int numWheres = 1; component[0] = 0;
        boolean[] checked = new boolean[wheres.length];
        while(true) {
            for(int i=first+1;i<wheres.length;i++)
                if(!checked[i] && (objectWheres[component[queue]].linked(wheres[i].getObjects()) || objectWheres[i].linked(wheres[component[queue]].getObjects()))) {
                    component[numWheres++] = i;
                    checked[i] = true;
                }
            queue++;
            if(queue >= numWheres) { // в очереди никого нету
                // проверяем or всей компоненты на checkTrue
                Where toCheck = FALSE;
                AndObjectWhere[] staticWheres = new AndObjectWhere[numWheres]; int stats = 0;
                for(int i=0;i<numWheres;i++)
                    if(decomposedWheres[component[i]]==wheres[component[i]])
                        staticWheres[stats++] = wheres[component[i]];
                    else
                        toCheck = op(toCheck,decomposedWheres[component[i]],true);
                if(stats < numWheres && orTrue(toCheck,toWhere(staticWheres,stats))) // если не true - drop'аем
                    for(int i=0;i<numWheres;i++)
                        rawResult[resnum++] = wheres[component[i]];

                first++; while(first<wheres.length && checked[first]) first++; // ищем первый необработанный элемент
                if(first>=wheres.length) break; // больше нету

                component = new int[wheres.length]; numWheres = 1;
                component[0] = first; queue = 0;
            }
        }

        if(resnum==wheres.length)
            return where;
        else
            return toWhere(rawResult,resnum);
    }

    public Where innerFollowFalse(Where falseWhere, boolean sureNotTrue) {

         if(!sureNotTrue && orTrue(this,falseWhere))
            return TRUE;

//        falseWhere = decomposeFalse(falseWhere,this);
//        if(falseWhere.isFalse())
//            return this;

        Where followWhere = FALSE; //false
        AndObjectWhere[] staticWheres = wheres;
        int current = 0;
        for(AndObjectWhere where : wheres) { // после упрощения важно использовать именно этот элемент, иначе неправильно работать будет
            AndObjectWhere[] siblingWheres = siblings(staticWheres, current);
            Where followAndWhere = followFalse(where,op(op(toWhere(siblingWheres),followWhere,true),falseWhere,true),false,true);
            if(!BaseUtils.hashEquals(followAndWhere,where)) { // если изменился static "перекидываем" в result
                followWhere = op(followWhere,followAndWhere,false);
                staticWheres = siblingWheres;
            } else
                current++;
        }
        return op(toWhere(staticWheres),followWhere,false);
    }

    static class Equal {
        AndExpr[] exprs;
        int size;
        ValueExpr value;

        Equal(AndExpr expr,int max) {
            exprs = new AndExpr[max];
            exprs[0] = expr;
            size = 1;
            if(expr instanceof ValueExpr)
                value = (ValueExpr) expr;
        }
    }

    static class EqualMap extends SimpleMap<AndExpr,Equal> {

        int max;
        EqualMap(int max) {
            this.max = max;
        }

        Equal getEqual(AndExpr expr) {
            Equal equal = get(expr);
            if(equal==null) {
                equal = new Equal(expr,max);
                add(expr,equal);
            }
            return equal;
        }

        public boolean add(AndExpr expr1,AndExpr expr2) {
            Equal equal1 = getEqual(expr1);
            Equal equal2 = getEqual(expr2);

            if(equal1.equals(equal2))
                return true;

            if(equal1.value==null) {
                equal1.value = equal2.value;
            } else
                if(equal2.value!=null && !equal1.value.equals(equal2.value)) // если равенство разных value, то false
                    return false;

            for(int i=0;i<equal2.size;i++) // "перекидываем" все компоненты в первую
                add(equal2.exprs[i],equal1);
            System.arraycopy(equal2.exprs,0,equal1.exprs,equal1.size,equal2.size);
            equal1.size += equal2.size;
            return true;
        }
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
        if(maxWhere<0) {
            // проверяем классовую логику - если из and'а.getClassWhere() всех not'ов => or всех IsClassWhere тогда это true
            ClassExprWhere classesNot = ClassExprWhere.TRUE;
            ClassExprWhere classesOr = ClassExprWhere.FALSE;
            for(int i=0;i<numWheres;i++) {
                if(wheres[i] instanceof NotWhere)
                    classesNot = classesNot.and(((NotWhere)wheres[i]).where.getClassWhere());
                if(wheres[i] instanceof IsClassWhere || wheres[i] instanceof MeanClassWhere)
                    classesOr = classesOr.or(wheres[i].getClassWhere());
            }
            if(classesNot.means(classesOr))
                return true;

            // если есть хоть один Compare, запускаем дальше чтобы избавится от них
            for(int i=0;i<numWheres;i++)
                if(wheres[i] instanceof CompareWhere)
                    return ((CompareWhere)wheres[i]).checkTrue(siblingsWhere(wheres,i,numWheres));

            // сначала объединим все EqualsWhere в группы - если найдем разные ValueExpr'ы в группе вывалимся
            EqualMap equals = new EqualMap(numWheres*2);
            for(int i=0;i<numWheres;i++)
                if(wheres[i] instanceof NotWhere && ((NotWhere) wheres[i]).where instanceof EqualsWhere) {
                    EqualsWhere equalsWhere = (EqualsWhere) ((NotWhere) wheres[i]).where;
                    if(!equals.add(equalsWhere.operator1,equalsWhere.operator2)) // противоречивы значит true
                        return true;
                }

            // возьмем все GreaterWhere и построим граф
            CompareMap compare = new CompareMap();
            for(int i=0;i<numWheres;i++)
                if(wheres[i] instanceof NotWhere && ((NotWhere) wheres[i]).where instanceof GreaterWhere) {
                    GreaterWhere greaterWhere = (GreaterWhere) ((NotWhere) wheres[i]).where;
                    if(!compare.add(equals.getEqual(greaterWhere.operator1),equals.getEqual(greaterWhere.operator2))) // противоречивы значит true;
                        return true;
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
        return op(where1,where2,true).checkTrue();
    }

    public boolean checkTrue() {
        if(wheres.length==0) return false;

/*        // разбиваем на компоненты
        // компонента определяется просто (OrObjectWhere) A связан с B если есть oA V oB = TRUE - data intersect followNot или followData intersect not
        // будем вытаскивать компоненты по очереди, пока не закончатся все элементы
        AndObjectWhere[][] components = new AndObjectWhere[wheres.length][]; int numComp = 0;
        int queue = 0;
        int[] nums = new int[wheres.length]; int[] heights = new int[wheres.length];
        AndObjectWhere[] wherePool = wheres.clone();
        int first = 0;
        AndObjectWhere[] component = new AndObjectWhere[wheres.length]; int numWheres = 1;
        component[0] = wheres[0]; int heightComponent = wheres[0].getHeight();
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
                component = new AndObjectWhere[wheres.length]; numWheres = 1;
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

    AndWhere calculateNot() {
        return new AndWhere(not(wheres));
    }

    public AndObjectWhere[] newArray(int length) {
        return new AndObjectWhere[length];
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    @ParamLazy
    Where translateDirect(KeyTranslator translator) {
        AndObjectWhere[] resultWheres = new AndObjectWhere[wheres.length];
        for(int i=0;i<wheres.length;i++)
            resultWheres[i] = (AndObjectWhere) wheres[i].translate(translator);
        return toWhere(resultWheres);
    }

    @ParamLazy
    Where translateQuery(QueryTranslator translator) {
        Where result = Where.FALSE;
        for(Where where : wheres)
            result = result.or(where.translate(translator));
        return result;
    }

    public Where translate(Translator translator) {
        if(translator instanceof KeyTranslator)
            return translateDirect((KeyTranslator)translator);
        else
            return translateQuery((QueryTranslator)translator);
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

    public boolean equals(Object o) {
        return this==o || o instanceof OrWhere && BaseUtils.equalArraySets(wheres, ((OrWhere) o).wheres);
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
