---
slug: "/Brief_integration"
title: 'Brief: интеграция'
---

## Доступ извне

Снаружи вызывается [действие](../paradigm/Actions.md) с параметрами, а результатом возвращаются значения указанных свойств без параметров. Действие задается тремя способами: `EXEC` — по имени, `EVAL` и `EVAL ACTION` — переданным кодом lsFusion (изнутри его выполняет [оператор `EVAL`](../language/EVAL_operator.md)).

Запросы принимают сервер приложений (порт `7651`) и веб-сервер:

- Action API — `/exec`, `/eval`, `/eval/action`, оба сервера;
- Form API — `/form`, только веб-сервер: ведение формы в интерактивном представлении;
- File API — `/files/list`, `/files/read`, `/files/search`, только веб-сервер: чтение classpath приложения.

Принимается ли такой вызов вообще, задает настройка: [`enableAPI`](../paradigm/Working_parameters.md) — для Action API и File API, `enableUI` — для Form API, который платформа считает пользовательским интерфейсом, а не программным. Действие, оказавшееся интерактивным, маршрутизируется в запущенный клиент пользователя и требует `enableUI` вдобавок к проверке `enableAPI`. Опция действия [`@@api`](../language/Action_options.md) разрешает конкретное действие при `enableAPI=0` (авторизованный пользователь при этом все равно нужен), а `@@noauth` обходит и проверку аутентификации, и `enableAPI`. Аннотация помечает именованное действие, поэтому она не распространяется ни на `/eval` и `/eval/action`, выполняющие произвольный код, ни на File API, где никакое действие не вызывается. Механизм — [обращение из внешней системы](../paradigm/Access_from_an_external_system.md).

Из той же JVM или того же SQL-сервера к элементам обращаются напрямую — Java-кодом ([Java API для интеграций](../paradigm/Java_integration_API.md)) либо запросом SQL к таблицам платформы ([обращение из внутренней системы](../paradigm/Access_from_an_internal_system.md)).

## Внешние вызовы (EXTERNAL)

[Оператор `EXTERNAL`](../language/EXTERNAL_operator.md) создает действие с одним вызовом внешней системы: параметры — в `PARAMS`, результаты — в свойства без параметров из `TO`.

```
EXTERNAL externalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

`externalCall`:

```
HTTP [CLIENT] [requestType] connectionStrExpr httpOption1 ... httpOptionN
TCP [CLIENT] connectionStrExpr
UDP [CLIENT] connectionStrExpr
SQL connectionStrExpr EXEC execStrExpr
LSF connectionStrExpr lsfExecType execStrExpr
DBF connectionStrExpr APPEND [CHARSET charsetLiteral]
```

`HTTP` — запрос по строке, `TCP` и `UDP` — байты файла в сокет, `SQL` — команда стороннему SQL-серверу, `LSF` — действие на другом сервере lsFusion, `DBF` — дозапись строк в `.dbf` ([обращение к внешней системе](../paradigm/Access_to_an_external_system_EXTERNAL.md)). Подключения `SQL`, `TCP`, `DBF` переиспользуются внутри блока [`NEWCONNECTION`](../language/NEWCONNECTION_operator.md); файл по URL забирает [оператор `READ`](../language/READ_operator.md).

```lsf
readRate () {
    EXTERNAL HTTP GET r'https://www.lsfusion.org/rate?cur=$1' PARAMS r'USD' TO exportFile;
}
```

## Внутренние вызовы (INTERNAL)

[Оператор `INTERNAL`](../language/INTERNAL_operator.md) выполняет код внутри собственных компонент поставки: Java в JVM сервера приложений, JavaScript-функцию или ресурс в веб-клиенте пользователя (`CLIENT`), SQL к базе самой платформы (`DB`).

```
INTERNAL [CLIENT] [syncType] className [(classId1, ..., classIdN)] [NULL]
INTERNAL [syncType] <{anyTokens}> [NULL]
INTERNAL internalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

Java-цель — класс, наследующий `InternalAction`, заданный своим именем или встраиваемым фрагментом кода в `<{ }>`. Такой код пишет значения прямо в свойства lsFusion в той же [сессии изменений](Brief_sessions.md); что ему доступно — [Java API для интеграций](../paradigm/Java_integration_API.md). Механизм всех трех типов — [внутренний вызов (`INTERNAL`)](../paradigm/Internal_call_INTERNAL.md), место оператора рядом с `FORMULA` — [обращение к внутренней системе](../paradigm/Access_to_an_internal_system_INTERNAL_FORMULA.md).

```lsf
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>;
```

## Декларативная интеграция (FORMULA, CUSTOM, JSON)

[Оператор `FORMULA`](../language/FORMULA_operator.md) создает свойство, вычисляемое выражением на SQL, возможно разным для разных СУБД; табличная форма отображает свойство на целую таблицу ([пользовательская формула](../paradigm/Custom_formula_FORMULA.md)).

```
FORMULA [NULL] [className [valueId]] implList [( paramList )] [NULL]
```

`CUSTOM` отдает отрисовку JavaScript-функции клиента: у группы объектов — `CUSTOM renderFunction [OPTIONS optionsExpr]` ([блоки объектов](../language/Object_blocks.md)), у свойства — `CUSTOM renderFunction [CHANGE [editFunction]]` ([блок свойств и действий](../language/Properties_and_actions_block.md)). Функция получает собственный локальный контроллер представления, который читает и изменяет то, что это представление показывает; контроллер формы, а с ним и вызовы сервера, достается из него как `controller.form` ([How-to: Пользовательские компоненты](../how-to/How-to_Custom_components_objects.md)).

[Операторы `JSON` и `JSONTEXT`](../language/JSON_operator.md) создают свойство, собирающее JSON из списка свойств или из формы.

```
jsonKeyword FROM [columnId1 =] propertyExpr1, ..., [columnIdN =] propertyExprN
  [WHERE whereExpr]
  [ORDER orderExpr1 [DESC], ..., orderExprL [DESC]]
  [TOP topExpr] [OFFSET offsetExpr]
jsonKeyword ( formName [OBJECTS objName1 = expr1, ..., objNameK = exprK]
  [FILTERS filterExpr1, ..., filterExprP]
  [TOP topSelect] [OFFSET offsetSelect] )
```

`jsonKeyword` — это `JSON` или `JSONTEXT`.
