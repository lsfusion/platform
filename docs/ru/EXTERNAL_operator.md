---
title: 'Оператор EXTERNAL'
---

Оператор `EXTERNAL` - создание [действия](Actions.md), выполняющего [обращение к внешней системе](Access_to_an_external_system_EXTERNAL.md). 

### Синтаксис

    EXTERNAL externalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1. ..., propertyIdM]

`externalCall` - внешний вызов, задается одним из следующих синтаксисов:

    HTTP [requestType] connectionStrExpr [BODYURL bodyStrExpr] [HEADERS headersPropertyId] [COOKIES cookiesPropertyId] [HEADERSTO headersToPropertyId] [COOKIESTO cookiesToPropertyId]
    TCP [CLIENT] connectionStrExpr
    UDP [CLIENT] connectionStrExpr
    SQL connectionStrExpr EXEC execStrExpr
    LSF connectionStrExpr lsfExecType execStrExpr

### Описание

Оператор `EXTERNAL` создает действие, которое делает запрос к внешней системе.

### Параметры

- `HTTP`

    Ключевое слово. Определяет, что оператор выполняет http-запрос веб-сервера.

- `requestType`

    Ключевое слово. Определяет [метод](https://ru.wikipedia.org/wiki/HTTP#%D0%9C%D0%B5%D1%82%D0%BE%D0%B4%D1%8B) HTTP-запроса:

    - `POST`
    - `GET`
    - `PUT`
    - `DELETE`

  Значением по умолчанию является `POST`.

- `TCP`

  Ключевое слово. Определяет, что оператор выполняет TCP-запрос.

- `UDP`

  Ключевое слово. Определяет, что оператор выполняет UDP-запрос.

- `CLIENT`

  Ключевое слово. Определяет, что запрос выполняется на клиенте. По умолчанию запрос выполняется на сервере.

- `SQL`

    Ключевое слово. Определяет, что оператор выполняет команду(ы) SQL-сервера.

- `LSF`

    Ключевое слово. Определяет, что оператор выполняет действие другого lsFusion-сервера.

- `connectionStrExpr`

    [Выражение](Expression.md). `HTTP`: Строка http-запроса. `SQL`: строка подключения к СУБД. `LSF`: URL lsFusion-сервера (приложений).

- `bodyStrExpr`

    [Выражение](Expression.md). Продолжение строки http-запроса в BODY. Актуально когда параметров в BODY > 1. Если не задано, параметры передаются в формате multipart.

- `headersPropertyId`
- `headersToPropertyId`

    [Идентификатор свойства](IDs.md#propertyid), содержащего заголовки (headers) запроса. Свойство должно иметь ровно один параметр - имя заголовка запроса. Этот параметр должен принадлежать строковому классу. Если свойство не указано, заголовки не задаются / игнорируются.

- `cookiesPropertyId`
- `cookiesToPropertyId`

    [Идентификатор свойства](IDs.md#propertyid), содержащего cookies запроса. Свойство должно иметь ровно один параметр - имя cookie. Этот параметр должен принадлежать строковому классу. Если свойство не указано, cookie не задаются / игнорируются.

- `lsfExecType`

    Ключевое слово. Определяет [способ задания](Access_from_an_external_system.md#actiontype) действия:

    - `EXEC` - задается имя вызываемого действия.
    - `EVAL` - задается код на языке **lsFusion**. Предполагается, что в этом коде присутствует объявление действия с именем `run`, именно это действие и будет вызвано.
    - `EVAL ACTION` - задается код действия на языке **lsFusion**. Для обращения к параметрам можно использовать спецсимвол `$` и номер параметра (начиная с `1`).

- `execStrExpr`

    Выражение. `SQL`: SQL-команда(ы)запроса. `LSF`: имя действия или код, в зависимости от способа задания действия.

- `paramExpr1, ..., paramExprN`

    Список выражений, значения которых будут использоваться в качестве параметров обращения.

- `propertyId1, ..., propertyIdM`

    Список [идентификаторов свойств](IDs.md#propertyid) (без параметров), в которые будут записаны полученные результаты.

### Примеры

```lsf
testExportFile = DATA FILE ();

externalHTTP()  {
    EXTERNAL HTTP GET 'https://www.cs.cmu.edu/~chuck/lennapg/len_std.jpg' TO exportFile;
    open(exportFile());

    // фигурные скобки escape'ся так как используются в интернационализации
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=getExamples' PARAMS JSONFILE('\{"mode"=1\}') TO exportFile; 
    IMPORT FROM exportFile() FIELDS () TEXT caption, TEXT code DO
        MESSAGE 'Example : ' + caption + ', code : ' + code;

    // передает в BODY url-encoded второй и третий параметры
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=doSomething&someprm=$1' 
             BODYURL 'otherprm=$2&andonemore=$3' 
             PARAMS 1,2,'3'; 
}
externalSQL ()  {
    // получаем все штрих-коды товаров с именем мясо
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Мясо%';
    // читаем цены для считанных штрих-кодов 
    EXTERNAL SQL 'jdbc:mysql://$1/test?user=root&password=' 
             EXEC 'select price AS pc, articles.barcode AS brc from $2 x JOIN articles ON x.bc=articles.barcode' 
             PARAMS 'localhost',exportFile() 
             TO exportFile;

    // для всех товаров с полученными штрих-кодами записываем цены
    LOCAL price = INTEGER (INTEGER);
    LOCAL barcode = STRING[30] (INTEGER);
    IMPORT FROM exportFile() TO price=pc,barcode=brc;
    FOR barcode(Article a) = barcode(INTEGER i) DO
        price(a) <- price(i);
}
externalLSF()  {
    EXTERNAL LSF 'http://localhost:7651' EXEC 'System.testAction[]';
};
```
