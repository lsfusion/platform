---
title: 'Разбиение / Упорядочивание (PARTITION ... ORDER)'
---

Оператор *разбиения / упорядочивания* создает [свойство](Properties.md), которое разбивает все наборы объектов в системе на *группы*, и с учетом заданного *порядка* вычисляет для каждого набора обьектов [агрегирующую функцию](Set_operations.md#func). Соответственно множество, на котором вычисляется эта агрегирующая функция определяется как: все наборы объектов принадлежащие группе этого набора объектов, и порядок которых меньше или совпадает с порядком этого набора объектов. 

Группы в этом операторе задаются как множество свойств (*группировок*), порядок задается как список свойств и признак возрастания или убывания. Если агрегирующая функция не [коммутативна](Set_operations.md#commutative), то порядок должен быть однозначно определяемым. 

Отметим, что оператор разбиения / упорядочивания очень похож на [оператор группировки](Grouping_GROUP.md), но в отличие от последнего получает результат не для значений группировок, а для самих наборов объектов, для которых идет вычисление.

### Язык

Для объявления свойства, реализующего разбиение / упорядочивание, используется [оператор `PARTITION`](PARTITION_operator.md). 

### Примеры

```lsf
// определяет место команды в конференции
CLASS Conference;
conference = DATA Conference (Team);
points = DATA INTEGER (Team);
gamesWon = DATA INTEGER (Team);
place 'Место' (Team team) = PARTITION SUM 1 ORDER DESC points(team), gamesWon(team) BY conference(team);

// строим порядковые индексы объектов в базе по возрастанию их внутренних идентификаторов (то есть в порядке создания)
index 'Номер' (Object o) = PARTITION SUM 1 IF o IS Object ORDER o;

// находит команду, следующую в турнирной таблице по конференции
prevTeam (Team team) = PARTITION PREV team ORDER place(team), team BY conference(team);

// пример пропорционального распределения
CLASS Order;
transportSum 'Транспортные расходы' = DATA NUMERIC[10,2] (Order);

CLASS OrderDetail;
order = DATA Order (OrderDetail) NONULL DELETE;
sum = DATA NUMERIC[14,2] (OrderDetail);

transportSum 'Транспортные расходы по строке' (OrderDetail d) = PARTITION UNGROUP transportSum
                                    PROPORTION STRICT ROUND(2) sum(d)
                                    ORDER d
                                    BY order(d);

// пример распределения с лимитами
discountSum 'Скидка' = DATA NUMERIC[10,2] (Order);
discountSum 'Скидка по строке' (OrderDetail d) =
    PARTITION UNGROUP discountSum
                LIMIT STRICT sum(d)
                ORDER sum(d), d
                BY order(d);
;
```
