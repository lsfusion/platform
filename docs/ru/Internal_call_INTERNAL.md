---
title: 'Внутренний вызов (INTERNAL)'
---

Оператор *внутреннего вызова* позволяет создавать действия на языках программирования, отличных от языка lsFusion. На текущий момент в платформе существует поддержка внешних действий только на языке Java.

В этом операторе можно указывать как непосредственно исходный код действия на языке Java, так и имя Java класса, байт-код которого находится в отдельном Java-файле. Во втором случае предполагается, что этот файл должен находится в classpath'е виртуальной машины сервера, а сам Java класс должен наследоваться от `lsfusion.server.physics.dev.integration.internal.to.InternalAction`. Выполняться при этом будет метод `executeInternal(lsfusion.server.logics.action.controller.context.ExecutionContext context)` этого класса.

Для этого оператора можно задать, какие классы создаваемое действие может принимать на вход, и может ли оно принимать на вход значения `NULL`. Если задаваемые условия не будут выполнены, создаваемое действие не выполнится и просто передаст управление следующему за ним действию.

### Язык

Для объявления действия, реализованного на языке Java, используется [оператор `INTERNAL`](INTERNAL_operator.md).

### Примеры

```lsf
showOnMap 'Показать на карте' 
    INTERNAL 'lsfusion.server.logics.classes.data.utils.geo.ShowOnMapAction' (DOUBLE, DOUBLE, MapProvider, BPSTRING[100]);

serviceDBMT 'Обслуживание БД (многопоточно, threadCount, timeout)' 
    INTERNAL 'lsfusion.server.physics.admin.service.action.ServiceDBMultiThreadAction' (INTEGER, INTEGER) NULL;

printlnAction 'Вывести текст в консоль' INTERNAL <{ System.out.println("action test"); }>;
// здесь context - это параметр метода executeInternal
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>; 
```
