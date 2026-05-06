---
title: 'Внутренний вызов (INTERNAL)'
---

Платформа предоставляет возможность lsFusion-системе обращаться к коду, выполняющемуся внутри её собственных компонентов развёртывания — JVM сервера приложений, веб-клиенту пользователя либо БД, используемой платформой, — с использованием различных типов внутренних вызовов, как внутренний аналог [обращения к внешней системе](Access_to_an_external_system_EXTERNAL.md). Интерфейсом такого обращения является выполнение кода в среде целевого компонента с заданными параметрами и, при необходимости, возврат некоторых значений в качестве *результатов*, записываемых в заданные свойства (без параметров). Для типов вызовов, в которых параметры и результаты сериализуются на границе (`CLIENT`, `DB`), значения должны принадлежать [встроенным классам](Built-in_classes.md); Java-цель работает на той же JVM, что и платформа, поэтому в её `executeInternal` объекты пользовательских классов передаются как `DataObject` / `ObjectValue` — без сериализации.

## Типы внутренних вызовов

На данный момент в платформе поддерживаются следующие типы внутренних вызовов:

### Java - выполнение Java-кода в JVM сервера приложений {#java}

Цель — Java-класс, унаследованный от `lsfusion.server.physics.dev.integration.internal.to.InternalAction`; его метод `executeInternal(ExecutionContext context)` вызывается на каждом исполнении. Способ задания цели (компилируемый класс или встраиваемый Java-фрагмент) — на стороне [оператора `INTERNAL`](INTERNAL_operator.md).

Классы параметров действия и поведение по `NULL`-аргументам определяются [оператором `INTERNAL`](INTERNAL_operator.md); по умолчанию действие не принимает `NULL`-значения, на стороне Java приём `NULL` дополнительно управляется переопределением `allowNulls()`.

Внутри Java-кода runtime платформы доступен через параметр `context` (текущая [сессия изменений](Change_sessions.md), значения параметров и окружение исполнения) и через разрешающие методы `InternalAction`. Возврат результатов в этом типе вызова не нуждается в отдельном механизме: Java-код пишет нужные значения непосредственно в свойства lsFusion из самого тела действия, в той же сессии изменений.

Внутри `executeInternal(ExecutionContext<ClassPropertyInterface> context)` доступен полный Java-API интеграций — поиск элементов системы (`InternalAction.findProperty`/`findAction` или `context.getBL()`), доступ к параметрам действия (`context.getDataKeyValue` / `getKeyValue` / `getKeyObject` с учётом `allowNulls()`), чтение и запись свойств (`LP.read` / `LP.change`), выполнение других действий (`LA.execute`), управление сессией и применением (`context.apply` / `context.cancel`, `session.applyException` / `applyMessage`, `setNoCancelInTransaction`) и пользовательские сообщения (`context.messageSuccess` / `messageError` / `delayUserInteraction`). Изменения, сделанные через `LP.change` / `LA.execute`, попадают в ту же [сессию изменений](Change_sessions.md) и применяются атомарно со всеми зависимыми [событиями](Events.md), [ограничениями](Constraints.md), [агрегациями](Aggregations.md) и [материализациями](Materializations.md). Полный каталог классов, сигнатур и правил работы с потоками — в статье [Java API для интеграций](Java_integration_API.md).

Java-обращение к lsFusion-системе из объектов, которые сами не являются `InternalAction` (Spring bean-компоненты вроде планировщика и интеграционных серверов), описано в статье [свой Spring bean (`EventServer`)](Custom_Spring_bean_EventServer.md).

### CLIENT - обращение к коду или файлу в веб-клиенте пользователя {#client}

Для этого типа взаимодействия задаётся имя клиентской цели — JavaScript-функции, уже загруженной в веб-клиенте пользователя, либо клиентского файла (входящего в поставку приложения или заданного во внешнем окружении в момент вызова — как правило, по URL, но также и другими file/path-ссылками, разрешаемыми на сервере приложений). К какой из двух целей относится конкретный вызов, определяется в момент вызова по виду имени ресурса. Скрипты, стили и шрифты обрабатываются клиентом специальным образом, соответствующим виду каждого из них; все остальные типы файлов, включая изображения, предоставляются клиенту как обобщённые файлы.

Клиентские вызовы по умолчанию асинхронные: сервер не ждёт завершения работы клиента. Синхронность нужно указывать явно; захват результата во встраиваемой форме дополнительно делает вызов синхронным.

Вспомогательные операции над уже загруженными файловыми ресурсами — например, выгрузка ранее загруженного скрипта или стиля — выражаются через зарезервированный префикс в имени ресурса; точный синтаксис описан в статье [оператора `INTERNAL`](INTERNAL_operator.md).

### DB - SQL против собственной БД платформы {#db}

Для этого типа взаимодействия задаётся SQL-команда как выражение свойства, вычисляемое в момент вызова. Подстановка параметров, табличные параметры, загрузка команды из ресурса classpath и запись результатов в свойства подчиняются тем же правилам, что и [`EXTERNAL SQL`](Access_to_an_external_system_EXTERNAL.md#sql) — отличия в том, что строка подключения не задаётся, а вызов выполняется внутри текущей [сессии изменений](Change_sessions.md), так что временные таблицы, транзакционное состояние и запись результатов откатываются или фиксируются вместе с этой сессией. Это также единственный поддерживаемый способ выполнять произвольный SQL против собственной БД платформы.

## Язык

Для объявления действия или записи оператора-действия, выполняющего внутренний вызов, используется [оператор `INTERNAL`](INTERNAL_operator.md).

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

Тип первого параметра конструктора должен совпадать с runtime-классом `LogicsModule`, из которого это действие подключается через `INTERNAL` (поиск конструктора идёт по точному классу): для большинства scripted-модулей это `ScriptingLogicsModule`, для модулей с собственным Java-подклассом — этот подкласс.
