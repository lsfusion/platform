---
slug: "/Internal_call_INTERNAL"
title: 'Внутренний вызов (INTERNAL)'
---

Платформа предоставляет возможность lsFusion-системе обращаться к коду, выполняющемуся внутри её собственных компонентов развёртывания — JVM сервера приложений, веб-клиенту пользователя либо БД, используемой платформой, — с использованием различных типов внутренних вызовов, как внутренний аналог [обращения к внешней системе](Access_to_an_external_system_EXTERNAL.md). Интерфейсом такого обращения является выполнение кода в среде целевого компонента с заданными параметрами и, при необходимости, возврат некоторых значений в качестве *результатов*, записываемых в заданные свойства (без параметров). Для типов вызовов, в которых параметры и результаты сериализуются на границе (`CLIENT`, `DB`), значения должны принадлежать [встроенным классам](Built-in_classes.md). Код на Java выполняется в той же JVM, что и платформа, и получает объекты пользовательских классов без сериализации.

## Типы внутренних вызовов

На данный момент в платформе поддерживаются следующие типы внутренних вызовов:

### Java - выполнение Java-кода в JVM сервера приложений {#java}

Цель вызова — Java-класс, унаследованный от `InternalAction`. При каждом вызове выполняется его метод `executeInternal`.

Оператор `INTERNAL` определяет классы параметров действия и то, допускаются ли аргументы со значением `NULL`. По умолчанию действие отклоняет такие аргументы.

Через параметр `context` код на Java получает доступ к платформе, в том числе к текущей [сессии изменений](Change_sessions.md), значениям параметров и среде выполнения действия. Чтобы вернуть результаты, код на Java записывает значения непосредственно в свойства lsFusion в текущей сессии изменений. Эти изменения применяются атомарно со всеми зависимыми [событиями](Events.md), [ограничениями](Constraints.md), [агрегациями](Aggregations.md) и [материализациями](Materializations.md).

### CLIENT - обращение к коду или файлу в веб-клиенте пользователя {#client}

Для этого типа взаимодействия задаётся имя клиентской цели — JavaScript-функции, уже загруженной в веб-клиенте пользователя, либо клиентского файла (входящего в поставку приложения или заданного во внешнем окружении в момент вызова — как правило, по URL, но также и другими file/path-ссылками, разрешаемыми на сервере приложений). К какой из двух целей относится конкретный вызов, определяется в момент вызова по виду имени ресурса. Скрипты, стили и шрифты обрабатываются клиентом специальным образом, соответствующим виду каждого из них. Все остальные типы файлов, включая изображения, предоставляются клиенту как обобщённые файлы.

Клиентские вызовы по умолчанию асинхронные: сервер не ждёт завершения работы клиента. Синхронность нужно указывать явно. Захват результата во встраиваемой форме дополнительно делает вызов синхронным.

Вспомогательные операции над уже загруженными файловыми ресурсами — например, выгрузка ранее загруженного скрипта или стиля — выражаются через зарезервированный префикс в имени ресурса. Точный синтаксис описан в статье оператора `INTERNAL`.

### DB - SQL против собственной БД платформы {#db}

Для этого типа взаимодействия задаётся SQL-команда как значение свойства, вычисляемое в момент вызова. Подстановка параметров, табличные параметры, загрузка команды из ресурса classpath и запись результатов в свойства подчиняются тем же правилам, что и [`EXTERNAL SQL`](Access_to_an_external_system_EXTERNAL.md#sql) — отличия в том, что строка подключения не задаётся, а вызов выполняется внутри текущей [сессии изменений](Change_sessions.md), так что временные таблицы, транзакционное состояние и запись результатов откатываются или фиксируются вместе с этой сессией. Это также единственный поддерживаемый способ выполнять произвольный SQL против собственной БД платформы.

## Язык

[Оператор `INTERNAL`](../language/INTERNAL_operator.md) объявляет действие или задаёт оператор-действие для внутреннего вызова. При объявлении действия на Java указывается класс или встраиваемый фрагмент кода для выполнения.

В статье [Java API для интеграций](../language/Java_integration_API.md) описаны классы, сигнатуры методов и правила работы с потоками для действий на Java.

[Свой Spring bean (`EventServer`)](../language/Custom_Spring_bean_EventServer.md) даёт доступ к lsFusion из кода на Java таким компонентам, как планировщик и серверы интеграции.

Функция JavaScript, вызванная через `INTERNAL CLIENT` в веб-клиенте, получает [контроллер формы или навигатора](../language/Form_and_navigator_controllers.md): на форме она может менять форму и вызывать сервер, а в навигаторе — вызывать сервер и активировать элементы навигатора. [Клиентские модули JavaScript](../language/Client_JavaScript_modules.md#named-exports-and-auto-registration) описывают, как такая функция предоставляется и находится по имени.

## Примеры

```lsf
// Java-цель — скомпилированный InternalAction-класс
cmd '{utils.cmd}' (TEXT command, TEXT directory, BOOLEAN isClient, BOOLEAN wait)
    INTERNAL 'lsfusion.server.physics.admin.interpreter.action.RunCommandAction';

// Java-цель — встраиваемый Java-фрагмент, доступ к runtime платформы через `context`
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>;

// Клиентская цель — файловые ресурсы, загружаемые в веб-клиент при старте, с ожиданием каждого
onWebClientStarted() + {
    INTERNAL CLIENT WAIT 'plotly-3.0.1.min.js';
    INTERNAL CLIENT WAIT 'dashboard.js';
}

// Клиентская цель — JavaScript-функция, вызываемая синхронно, возвращаемое значение захватывается
getCookie(STRING name) {
    LOCAL cookie = STRING();
    INTERNAL CLIENT 'getCookie' PARAMS name TO cookie;
}

// Внутренняя SQL-цель — SQL, выполняющийся в текущей сессии изменений
loadPrices() {
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Мясо%';
    INTERNAL DB 'select price, barcode from $1' PARAMS exportFile() TO exportFile;
}
```

Минимальный пример Java-класса, унаследованного от `InternalAction`, — резолвит свойства и действие в конструкторе через `findProperty` / `findAction`, в `executeInternal` читает и пишет свойство и выполняет действие:

```java
package myapp.actions;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class SyncUserLoginAction extends InternalAction {

    private final LP<?> email;
    private final LP<?> login;
    private final LA<?> relogin;

    private final ClassPropertyInterface userInterface;

    public SyncUserLoginAction(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        this.email = findProperty("email[Contact]");
        this.login = findProperty("login[CustomUser]");
        this.relogin = findAction("relogin[CustomUser]");

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        userInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject user = context.getDataKeyValue(userInterface);

        String currentEmail = (String) email.read(context, user);
        if (currentEmail != null) {
            login.change(currentEmail, context, user);
            relogin.execute(context, user);
        }
    }
}
```

Подключение этого класса со стороны lsFusion-кода:

```lsf
syncUserLogin 'Синхронизировать логин из email' (CustomUser u)
    INTERNAL 'myapp.actions.SyncUserLoginAction';
```
