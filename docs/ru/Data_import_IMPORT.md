---
title: 'Импорт данных (IMPORT)'
---

Оператор *импорта данных* создает [действие](Actions.md), которое читает файл из значения некоторого [свойства](Properties.md), затем, в зависимости от его [формата](Structured_view.md), определяет колонки (поля) данных в этом файле, после чего [записывает](Property_change_CHANGE.md) значение каждой колонки (поля) в соответствующее свойство (параметр) - *назначение* импорта. Отображение колонок на свойства может идти как по порядку колонок, так и по их именам.

Строки, в свою очередь, при импорте отображаются на объекты заданных классов (будем называть эти объекты *импортируемыми*). В текущей реализации платформы объект может быть максимум один, а заданный класс должны быть либо [числовым](Built-in_classes.md) либо [конкретным пользовательским](User_classes.md#abstract). При этом отображение строк на импортируемый объект осуществляется следующим образом:

-   для числовых классов - все импортируемые строки нумеруются в порядке, в котором они идут в файле (начиная с 0).
-   для конкретных пользовательских классов - для каждой импортируемой строки [создается новый объект](New_object_NEW.md) заданного класса.

Также, при импорте можно задать *условие* импорта - свойство, в которое для каждой строки будет записано [значение по умолчанию](Built-in_classes.md#defaultvalue) класса значения этого свойства (в отличие от назначений импорта, в которые записываются значения колонок).

### Общий случай

Стоит отметить, что импорт данных является частным случаем (синтаксическим сахаром) [импорта форм](In_a_structured_view_EXPORT_IMPORT.md#importForm), в котором импортируемая форма создается автоматически и состоит из:

-   одной [группы объектов](Form_structure.md) с именем `value`, объекты которой соответствуют импортируемым объектам (не создается, если импортируемых объектов нет)
-   импортируемых свойств. В качестве [группы свойств](Form_structure.md#propertygroup) для создаваемых свойств на форме при этом используется [встроенная](Groups_of_properties_and_actions.md#builtin) группа `System.private`.
-   фильтра равного заданному условию.

Соответственно, поведение оператора импорта данных (например, определение имен результирующих колонок / ключей, [обработка значений `value`](Structured_view.md#value) и т.п.) полностью определяется поведением оператора импорта формы (как если бы ему параметром передали вышеописанную форму).

### Язык

Для объявления действия, импортирующего данные, используется [оператор `IMPORT`](IMPORT_operator.md).

### Примеры


```lsf
import()  {

    LOCAL xlsFile = EXCELFILE ();

    LOCAL field1 = BPSTRING[50] (INTEGER);
    LOCAL field2 = BPSTRING[50] (INTEGER);
    LOCAL field3 = BPSTRING[50] (INTEGER);
    LOCAL field4 = BPSTRING[50] (INTEGER);

    LOCAL headField1 = BPSTRING[50] ();
    LOCAL headField2 = BPSTRING[50] ();

    INPUT f = EXCELFILE DO {
        IMPORT XLS SHEET 2 FROM f TO field1 = C, field2, field3 = F, field4 = A;
        IMPORT XLS SHEET ALL FROM f TO field1 = C, field2, field3 = F, field4 = A;

        // свойство imported - системное свойство, предназначенное для перебора данных
        FOR imported(INTEGER i) DO { 
            MESSAGE 'field1 value = ' + field1(i);
            MESSAGE 'field2 value = ' + field2(i);
            MESSAGE 'field3 value = ' + field3(i);
            MESSAGE 'field4 value = ' + field4(i);
       }
    }

    LOCAL t = FILE ();
    EXTERNAL SQL 'jdbc:postgresql://localhost/test?user=postgres&password=12345' 
             EXEC 'SELECT x.a,x.b,x.c,x.d FROM orders x WHERE x.id = $1;' PARAMS '4553' TO t;
    // импорт с опцией FIELDS
    IMPORT FROM t() FIELDS INTEGER a, DATE b, BPSTRING[50] c, BPSTRING[50] d DO        
        NEW o = Order {
            number(o) <- a;
            date(o) <- b;
            customer(o) <- c;
            // находим currency с данным именем
            currency(o) <- GROUP MAX Currency currency IF name(currency) = d; 
        }


    INPUT f = FILE DO
        IMPORT CSV '*' HEADER CHARSET 'utf-8' FROM f TO field1 = C, field2, field3 = F, field4 = A;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ROOT 'element' ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO() headField1, headField2;

    INPUT f = FILE DO
        INPUT memo = FILE DO
            IMPORT DBF MEMO memo FROM f TO field1 = 'DBFField1', field2 = 'DBFField2';
}
```
