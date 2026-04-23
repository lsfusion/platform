---
title: 'Оператор EXTERNAL'
---

Оператор `EXTERNAL` - создание [действия](Actions.md), выполняющего [обращение к внешней системе](Access_to_an_external_system_EXTERNAL.md). 

### Синтаксис

```
EXTERNAL externalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

`externalCall` - внешний вызов, задается одним из следующих синтаксисов:

```
HTTP [CLIENT] [requestType] connectionStrExpr httpOption1 ... httpOptionN
TCP [CLIENT] connectionStrExpr
UDP [CLIENT] connectionStrExpr
SQL connectionStrExpr EXEC execStrExpr
LSF connectionStrExpr lsfExecType execStrExpr
DBF connectionStrExpr APPEND [CHARSET charsetLiteral]
```

Опции для `HTTP` перечисляются друг за другом в произвольном порядке через пробел или переводы строк:
```
httpOption1 ... httpOptionN
```

```
BODYURL bodyStrExpr
BODYPARAMNAMES bodyParamNameExpr1, ..., bodyParamNameExprK
BODYPARAMHEADERS bodyParamHeadersPropertyId1, ..., bodyParamHeadersPropertyIdK
HEADERS headersPropertyId
COOKIES cookiesPropertyId
HEADERSTO headersToPropertyId
COOKIESTO cookiesToPropertyId
NOENCODE
```

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
    - `PATCH`

  Значением по умолчанию является `POST`.

- `TCP`

  Ключевое слово. Определяет, что оператор выполняет TCP-запрос.

- `UDP`

  Ключевое слово. Определяет, что оператор выполняет UDP-запрос.

- `CLIENT`

  Ключевое слово. Выполняет вызов на клиенте пользователя. Без `CLIENT` вызов выполняется на сервере приложений.

- `SQL`

    Ключевое слово. Определяет, что оператор выполняет команду(ы) SQL-сервера.

- `LSF`

    Ключевое слово. Определяет, что оператор выполняет действие другого lsFusion-сервера.

- `DBF`

    Ключевое слово. Определяет, что оператор записывает строки в `.dbf`-файл.

- `APPEND`

    Обязательное ключевое слово в синтаксисе `DBF`-вызова.

- `charsetLiteral`

    [Строковый литерал](Literals.md#strliteral) с кодировкой `.dbf`-файла. По умолчанию `UTF-8`.

- `connectionStrExpr`

    [Выражение](Expression.md). `HTTP`: строка http-запроса. `TCP` / `UDP`: `host:port` целевого сокета. `SQL`: строка подключения к СУБД. `LSF`: URL сервера приложений или веб-сервера lsFusion. `DBF`: путь к `.dbf`-файлу.

- `bodyStrExpr`

    [Выражение](Expression.md). Строка BODY с подстановкой параметров через `$N`. Для HTTP-методов с телом все параметры, оставшиеся после подстановки в URL, должны быть использованы внутри этой строки, иначе вызов падает; без `BODYURL` оставшиеся параметры упаковываются в BODY напрямую. Для `GET` `BODYURL` не действует, а оставшиеся параметры молча отбрасываются.

- `bodyParamNameExpr1, ..., bodyParamNameExprK`

    Список [выражений](Expression.md). Каждое вычисляется в имя BODY-части в форме `'name'` или `'name;filename'` (часть после `;` задаёт имя файла multipart-части). Без `BODYPARAMNAMES` части BODY получают автоматические имена `param0`, `param1`, ... (файловые части без явного filename аналогично получают `file0`, `file1`, ...).

- `bodyParamHeadersPropertyId1, ..., bodyParamHeadersPropertyIdK`

    Список [идентификаторов свойств](IDs.md#propertyid). Каждое свойство имеет один параметр строкового класса (имя заголовка) и значение строкового класса (значение заголовка). Без `BODYPARAMHEADERS` дополнительные заголовки к частям BODY не добавляются.

- `headersPropertyId`, `headersToPropertyId`

    [Идентификаторы свойств](IDs.md#propertyid). Каждое свойство имеет один параметр строкового класса (имя заголовка) и значение строкового класса (значение заголовка). Без `HEADERS` собственные заголовки запроса не устанавливаются; без `HEADERSTO` заголовки ответа не сохраняются.

- `cookiesPropertyId`, `cookiesToPropertyId`

    [Идентификаторы свойств](IDs.md#propertyid). Каждое свойство имеет один параметр строкового класса (имя cookie) и значение строкового класса (значение cookie). Без `COOKIES` собственные cookies не отправляются; без `COOKIESTO` cookies не сохраняются.

- `NOENCODE`

    Ключевое слово. Отключает предварительное URL-кодирование литерального текста строки подключения и строки `BODYURL` (значения параметров, подставляемых через `$N`, всё равно URL-кодируются отдельно). Без `NOENCODE` литеральный текст URL-кодируется перед отправкой.

- `lsfExecType`

    Ключевое слово. Определяет [способ задания](Access_from_an_external_system.md#actiontype) действия:

    - `EXEC` - задается имя вызываемого действия.
    - `EVAL` - задается код на языке **lsFusion**. Предполагается, что в этом коде присутствует объявление действия с именем `run`, именно это действие и будет вызвано.
    - `EVAL ACTION` - задается код действия на языке **lsFusion**. Для обращения к параметрам можно использовать спецсимвол `$` и номер параметра (начиная с `1`).

- `execStrExpr`

    [Выражение](Expression.md). `SQL`: SQL-команда(ы) — выражение, заканчивающееся на `.sql`, трактуется как путь к ресурсу classpath, содержимое которого используется как команда. `LSF`: имя действия или код, в зависимости от `lsfExecType`.

- `paramExpr1, ..., paramExprN`

    Список выражений, значения которых будут использоваться в качестве параметров обращения.

- `propertyId1, ..., propertyIdM`

    Список [идентификаторов свойств](IDs.md#propertyid) (без параметров), в которые будут записаны полученные результаты.

### Примеры

```lsf
externalHTTP()  {
    // HTTP-метод + TO для результата
    EXTERNAL HTTP GET 'https://www.cs.cmu.edu/~chuck/lennapg/len_std.jpg' TO exportFile;

    // BODYURL: второй и третий параметры уходят в url-encoded BODY через $N
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=doSomething&someprm=$1' 
             BODYURL 'otherprm=$2&andonemore=$3' PARAMS 1, 2, '3';

    // BODYPARAMNAMES в форме 'name;filename' + ручной Content-Type через HEADERS
    LOCAL headers = TEXT(STRING[100]);
    headers('Content-Type') <- 'multipart/form-data; charset=UTF-8';
    EXTERNAL HTTP POST 'https://api.example/upload'
             BODYPARAMNAMES 'document;report.pdf'
             HEADERS headers
             PARAMS exportFile();

    // HEADERSTO / COOKIESTO: захват заголовков и cookies ответа
    LOCAL respHeaders = TEXT(STRING[100]);
    LOCAL respCookies = TEXT(STRING[100]);
    EXTERNAL HTTP GET 'https://api.example/login'
             HEADERSTO respHeaders
             COOKIESTO respCookies
             TO exportFile;
}
externalTCP()  {
    EXTERNAL TCP 'example.com:9100' PARAMS RAWFILE('payload');
    MESSAGE STRING(responseTcp());
}
externalSQL ()  {
    // SQL-команда с параметром формата TABLE, загружаемым во временную таблицу
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Мясо%';
    EXTERNAL SQL 'jdbc:mysql://$1/test?user=root&password=' 
             EXEC 'select price AS pc, articles.barcode AS brc from $2 x JOIN articles ON x.bc=articles.barcode' 
             PARAMS 'localhost',exportFile() 
             TO exportFile;

    // SQL-команда, подгружаемая из ресурса classpath (выражение оканчивается на .sql)
    EXTERNAL SQL 'jdbc:postgresql://localhost/db?user=root' EXEC 'queries/fetch.sql';
}
externalLSF()  {
    EXTERNAL LSF 'http://localhost:7651' EXEC 'System.testAction[]';
}
externalDBF()  {
    EXPORT TABLE FROM bc=barcode(Article a), nm=name(a);
    EXTERNAL DBF '/tmp/articles.dbf' APPEND CHARSET 'CP866' PARAMS exportFile();
}
```
