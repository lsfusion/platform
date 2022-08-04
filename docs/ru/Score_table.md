---
title: 'Турнирная таблица'
---

## Описание задачи "Турнирная таблица"

Создаваемая с помощью платформы **lsFusion** информационная система должна содержать функциональность для ведения турнирной таблицы хоккейного турнира.

Под турниром понимается подмножество игр между командами (участниками каждой из игр являются 2 команды), по результатам которых командам начисляются очки.

Результатом каждой игры может быть победа одной из команд в основное время (команде-победителю начисляется 3 очка), победа в овертайме (команде-победителю начисляется 2 очка, а проигравшей команде 1 очко) и победа в серии буллитов (команде-победителю начисляется 2 очка, а проигравшей команде 1 очко).

Рейтинговое место команды в турнирной таблице определяется количеством набранных очков, а в случае их равенства дополнительными параметрами: количеством побед в основное время, количеством побед в овертайме, количеством побед в серии буллитов, разницей заброшенных и пропущенных шайб, количеством заброшенных шайб. Дополнительные параметры для определения итогового места последовательно применяются в заданном порядке до достижения ситуации, при которой результаты команд ранжируются однозначно.

## Задание предметной логики

### Объявление модуля

Объявляем [модуль](Modules.md), в рамках которого будет реализован требуемый функционал. Присваиваем модулю произвольное наименование (например, `HockeyStats1).

```lsf
MODULE HockeyStats;
```

Зададим использование в модуле `HockeyStats` функциональности из других модулей. В частности, нам понадобится системный модуль `System`, в котором объявляются некоторые используемые в примере элементы системы.

```lsf
REQUIRE System;
```

### Определение команды

Вводим понятие команды, для чего создаем отдельный [класс](Classes.md) с помощью соответствующей [`инструкции CLASS`](CLASS_statement.md). 

```lsf
CLASS Team 'Команда';
```

Создаваемому классу присваиваем название (например, `Team`), которое в дальнейшем будет использоваться при построении [выражений](Expression.md), а также подпись для отображения на пользовательских формах (например, `'Команда'`).

Чтобы при работе с созданными позднее формами все команды можно было легко идентифицировать, создаем для команды название. Иными словами, создаем [свойство](Properties.md) "Название", которое может быть определено для объектов класса `Team`.

```lsf
name 'Название команды' = DATA STRING[30] (Team) IN base;
```

Таким образом, название команды является [первичным](Data_properties_DATA.md) (вводимым пользователем) свойством строчного типа. Опцией `IN` созданное свойство добавляется в предопределенную [группу свойств](Groups_of_properties_and_actions.md) `base`. Свойства объекта, относящиеся к группе `base`, будут автоматически отображаться на диалоговой форме для выбора объекта класса `Team`.

### Определение игры

Вводим понятие игры и ее атрибуты: дата, участники игры (команда хозяев и команда гостей) и их названия.

```lsf
CLASS Game 'Игра';

date 'Дата' = DATA DATE (Game);
hostTeam = DATA Team (Game);
guestTeam = DATA Team (Game);
hostTeamName 'Хозяева' (Game game) = name(hostTeam(game));
guestTeamName 'Гости' (Game game) = name(guestTeam(game));
```

Свойства `hostTeam` и `guestTeam` являются [первичными](Data_properties_DATA.md) объектными свойствами игры, дающими в качестве результата ссылку на команду хозяев и команду гостей соответственно (т.е. на конкретные объекты класса `Team`). Свойства названий команды хозяев и гостей игры (`hostTeamName` и `guestTeamName`) создаются для использования в дальнейшем на формах. Если на форму выносить сами свойства `hostTeam` и `guestTeam`, то пользователю будут отображаться внутренние идентификаторы объектов из базы данных.

Введем ограничение, что участниками игры должны быть две разные команды.

```lsf
CONSTRAINT hostTeam(Game team) = guestTeam(team) CHECKED BY hostTeam, guestTeam MESSAGE 'Хозяйская и гостевая команды должны быть разными';
```

Механизм работы данного выражения следующий: при изменении у игры команды хозяев или команды гостей система проверяет условие равенства этих команд `hostTeam(team) == guestTeam(team)` и в случае его выполнения блокирует применение изменений в базу данных, а также выдает пользователю заданное сообщение `'Хозяйская и гостевая команды должны быть разными'`. Иными словами, результатом выражения, заданном после оператора `CONSTRAINT`, должен быть `NULL`. В иных случаях ограничение будет считаться нарушенным.  Кроме того, благодаря конструкции `CHECKED BY`, добавленное ограничение будет фильтровать исходя из заданного правила команды при выборе для игры команды хозяев или команды гостей (т.е. в появляющемся диалоговом окне исключать из перечня команд уже заданную в качестве соперника команду).

Задаем количество голов, забитых каждой из команд за время игры.

```lsf
hostGoals 'Х голы' = DATA INTEGER (Game);
guestGoals 'Г голы' = DATA INTEGER (Game);
```

В заданных свойствах используется тип `INTEGER`, поскольку количество забитых голов каждой из команд является целым числом.

Вводим ограничение, что игра не может закончиться вничью. Система должна запретить пользователю задавать одинаковое количество голов для обоих команд игры, выдавая при этом сообщение с заданным текстом.

```lsf
CONSTRAINT hostGoals(Game game) = guestGoals(game) MESSAGE 'Игра не может закончиться вничью';
```

### Определение победителя игры

Определяем победителя игры - команду, которая забила голов больше, чем соперник.

```lsf
winner(Game game) = IF hostGoals(game) > guestGoals(game)
                    THEN hostTeam(game)
                    ELSE guestTeam(game);
```

В данном случае используется оператор [`IF... THEN... ELSE`](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) , который проверяет верность условия, что команда хозяев в данной игре забила больше голов, чем команда гостей, и в случае его выполнения победителем игры является команда хозяев, в противном случае - команда гостей.

По аналогичному принципу команда, участвовавшая в игре и забившая голов меньше соперника, будет считаться проигравшей.

```lsf
looser(Game game) = IF hostGoals(game) > guestGoals(game)
                    THEN guestTeam(game)
                    ELSE hostTeam(game);
```

### Определение результата игры

Вводим понятие возможного результата игры с предопределенным набором значений: победа в основное время, победа в овертайме, победа в серии буллитов.

```lsf
CLASS GameResult 'Р/И' {
    win 'П',
    winOT 'ПО',
    winSO 'ПБ'
}
```

Для этой цели создаем класс `GameResult` и добавляем ему три [статических объекта](Static_objects.md), которые задаются с помощью выражений, заданных в фигурных скобках `{ }`. При этом значения `win`, `winOT`, `winSO` и `П`, `ПО`, `ПБ` будут находится в системных свойства `staticName` и `staticCaption`, соответственно.

Создаем свойство `resultName`, которое будет возвращать заголовок результата игры (`П`, `ПО` или `ПБ`). Для этого берется системное свойство `staticCaption`, которое действует для всех объектов в системе, и ограничиваем его сигнатуру при помощи конструкции `IF`, указывая что объект должен быть класса `Game`. Это свойство добавляется в группу свойств `base`, чтобы оно показывалось в автоматическом диалоге по выбору объекта класса `GameResult`.

```lsf
resultName 'Имя' (GameResult game) = staticCaption(game) IF game IS GameResult IN base;
```

Определяем результат конкретной игры. В том случае, если одна из команд победила с разницей в 2 и более гола, то результатом игры будем считать победу в основное время. В ином и только ином случае результат игры (тип победы при заданном счете) будет задаваться пользователем. При этом пользователь не может задать результатом игры победу в основное время.

```lsf
userResult = DATA GameResult (Game);
result (Game game) = OVERRIDE userResult(game),
    (GameResult.win IF ((hostGoals(game) (-) guestGoals(game)) > 1 OR (guestGoals(game) (-) hostGoals(game)) > 1));
resultName 'Р/И' (Game game) = resultName(result(game));

CONSTRAINT ((hostGoals(Game game) (-) guestGoals(game)) > 1 OR (hostGoals(game) (-) guestGoals(game)) < -1) AND userResult(game)
    MESSAGE 'Результат игры определен автоматически';
```

Для определения результата игры используется оператор `OVERRIDE`, который возвращает первое значение по порядку задания выражений, которое не равняется `NULL`. В данном случае, результатом вычисления свойства `result` будет либо объект статического класса `GameResult.win` при условии, что разница голов в игре больше `1`, либо значение первичного объектного свойства `userResult`.

Для того, чтобы результат для игры определялся всегда, создаем ограничение, которое обеспечит обязательность задания пользователем значения свойства `userResult`, если результат не вычисляется исходя из счета игры.

```lsf
CONSTRAINT ((hostGoals(Game game) (-) guestGoals(game)) < 2 AND (hostGoals(game) (-) guestGoals(game)) > -2) AND NOT userResult(game)
    MESSAGE 'Укажите результат игры';
```

Результатом выражения `NOT userResult(game)` будет истина только в том случае, если `userResult(game)` не определено (т.е. является `NULL`). Таким образом, ограничение сработает в случае, если разница счета будет `1`, а тип победы пользователем не будет задан.

### Создание турнирной таблицы

Турнирная таблица представляет собой рейтинг команд турнира - список команд, отсортированных по рейтинговому месту.

Задаем показатели, определяющие место команды в турнирной таблице:

-   количество игр, сыгранных командой дома и в гостях, и суммарное их количество  
      
    ```lsf
    hostGamesPlayed = GROUP SUM 1 BY hostTeam(Game game);
    guestGamesPlayed = GROUP SUM 1 BY guestTeam(Game game);
    gamesPlayed 'И' (Team team) = hostGamesPlayed(team) (+) guestGamesPlayed(team);
    ```

:::info
Здесь конструкция `(+)` используется вместо арифметического `+` для получения корректного результата в случае, если хотя бы одно из слагаемых имеет значение `NULL`. Использование `(+)` в данном случае равноценно замене возможного `NULL` на `0`. Если одно из слагаемых равно `NULL`, то результатом использования арифметического `+` будет также значение `NULL`.
:::

Для определения количества сыгранных командой дома и в гостях игр используется [оператор `GROUP SUM`](Grouping_GROUP.md), который позволяет получить сумму результатов вычислений заданного выражения для объектов некоторого класса, группированных по одному или нескольким своим атрибутам (аналог промежуточных сумм в Excel). В данном случае для суммирования задается число `1`, а группировка всех игр (блок `BY`) выполняется по команде гостей и команде хозяев. В итоге, например, свойство `hostGamesPlayed` определяет для команды (поскольку результатом вычисления свойства `hostTeam` является объект класса `Team`) количество (т.е. сумму единиц, заданных для каждого случая равенства заданной команды и команды хозяев игры) сыгранных в качестве хозяев игр (свойство `hostTeam` определено только для объектов класса `Game`). При данном расчете система анализирует все игры, введенные в системе.

-   количество игр, выигранных в основное время, в овертайме и в дополнительное время  
      
    ```lsf
    gamesWonBy(Team team, GameResult type) = OVERRIDE [GROUP SUM 1 BY winner(Game game), result(game)](team, type), 0;
    
    gamesWon 'В' (Team team) = gamesWonBy(team, GameResult.win);
    gamesWonOT 'ВО' (Team team) = gamesWonBy(team, GameResult.winOT);
    gamesWonSO 'ВБ' (Team team) = gamesWonBy(team, GameResult.winSO);
    ```

Поскольку логика определения количества одержанных командой побед каждого типа практически идентична, создаем и используем промежуточное свойство `gamesWonByResult`, определяемое для пары объектов (аргументов). Данное свойство рассчитывает для команды (первый аргумент) количество побед данного типа (второй аргумент). Значение свойства `gamesWonBy` рассчитывается через оператор `OVERRIDE`, получающий на вход заданное в квадратных скобках `[...]` выражение и `0`. Если значение выражения будет равно `NULL`, то результатом всего свойства будет значение `0`. В квадратных скобках задано вложенное выражение с использованием [оператора `GROUP SUM`](Grouping_GROUP.md). Использование в квадратных скобках некоторого выражения идентично использованию некоторого ранее заданного свойства с аналогичным выражением. Таким образом конструкция `[...]` позволяет просто уменьшить количество строк кода. В данном случае [`GROUP SUM`](Grouping_GROUP.md) возвращает сумму единиц для игр, сгруппированных по победителю игры и результату игры.

Общим результатом свойства gamesWonByResult будет количество побед данного типа для данной команды или ноль в случае, если побед данного типа данная команда не одерживала (т.е. в случае, если `[GROUP SUM 1 BY winner(Game game), result(game)]` для данной команды и типа победы равняется `NULL`).

-   количество игр, проигранных в основное время, в овертайме и в дополнительное время (определяем по аналогии с выше заданными свойствами количества побед)  
      
    ```lsf
    gamesLostBy(Team team, GameResult type) = OVERRIDE [GROUP SUM 1 BY looser(Game game), result(game)](team, type), 0;
    
    gamesLost 'П' (Team team) = gamesLostBy(team, GameResult.win);
    gamesLostOT 'ПО' (Team team) = gamesLostBy(team, GameResult.winOT);
    gamesLostSO 'ПБ' (Team team) = gamesLostBy(team, GameResult.winSO);
    ```

Рассчитываем количество очков, набранных командой в турнире. Расчет представляет собой сумму умножений для каждой из команд количества побед конкретного типа на количество причитающихся в данном случае очков.

```lsf
points 'Очки' (Team team) = gamesWon(team) * 3 + (gamesWonSO(team) + gamesWonOT(team)) * 2 + gamesLostOT(team) + gamesLostSO(team);
```

Для использования в качестве дополнительных показателей ранжирования команд рассчитываем общее количество забитых и пропущенных командой голов.

```lsf
hostGoalsScored = GROUP SUM hostGoals(Game game) BY hostTeam(game);
guestGoalsScored = GROUP SUM guestGoals(Game game) BY guestTeam(game);
goalsScored 'Кол-во забитых голов' (Team team) = OVERRIDE hostGoalsScored(team) (+) guestGoalsScored(team), 0 IF team IS Team;

hostGoalsConceded = GROUP SUM guestGoals(Game game) BY hostTeam(game);
guestGoalsConceded = GROUP SUM hostGoals(Game game) BY guestTeam(game);
goalsConceded 'Кол-во пропущенных голов' (Team team) = OVERRIDE hostGoalsConceded(team) (+) guestGoalsConceded(team), 0 IF team IS Team;
```

Определяем место команды в турнирной таблице.

```lsf
place 'Место' (Team team) = PARTITION SUM 1 ORDER DESC points(team), gamesWon(team), gamesWonOT(team), gamesWonSO(team),
                                               (OVERRIDE goalsScored(team) (-) goalsConceded(team), 0), goalsScored(team);
```

  

Свойство `place` "Место команды в турнирной таблице" определяется с помощью конструкции [`PARTITION SUM`](Partitioning_sorting_PARTITION_..._ORDER.md), которая для всех объектов некоторого класса нарастающим итогом, последовательность которого задается оператором `ORDER`, рассчитывает сумму результатов вычисления заданного выражения. Важно помнить, что значения всех свойств, которые участвуют в определении порядка, должно не равняться `NULL`. Для этой цели предпоследнее выражение использует оператор `OVERRIDE`, чтобы вместо `NULL` использовалось число `0`.

Таким образом логика определения свойства `place` для каждой из команд следующая:

-   все команды выстраиваются в последовательность (ранжируются) в порядке убывания значений некоторых параметров (количества набранных очков, выигранных в основное время матчей и иных свойств, заданных после оператора `ORDER DESC`)
-   для каждой команды подсчитывается сумма значений указанного оператором `SUM` выражения (в данном случае числа `1`), посчитанных для каждой из команд, находящихся в отранжированном списке до данной команды, и значения выражения для данной команды. Т.е для первой команды `1`, второй - `1+1`, третьей - `1+1+1` и т.д.

## Задание логики представления

Добавляем интерфейс, позволяющий работать с создаваемой системой: вводить в систему данные и получать из нее необходимую информацию. Создаваемая форма будет состоять из двух вертикально расположенных блоков, в верхнем из которых пользователь сможет добавлять, изменять и удалять игры со всеми ее атрибутами, а в нижней будет располагаться турнирная таблица по результатам игр с возможностью добавления, удаления команд и изменения их названий.

Объявляем форму с наименованием и подписью. Добавляем на форму блок объектов класса `Game` со всеми заданными в системе свойствами. Также выносим на форму кнопки добавления новой игры и ее удаления (эти кнопки автоматически определены для всех объектов в системе).

```lsf
FORM MainForm 'Турнирная таблица'
    OBJECTS game = Game
    PROPERTIES(game) date, hostTeamName, hostGoals, guestGoals, guestTeamName, resultName, NEW, DELETE
;
```

Инструкция `FORM` создает пустую форму с [некоторой функциональностью по умолчанию](Form_structure.md). С помощью выражения `OBJECTS game=Game` на форму добавляется объект `game`: блок табличного вида, содержащий все экземпляры класса `Game`, введенные в системе. Выражением `PROPERTIES(game)` с последующим перечислением подмножества свойств на форму добавляются указанные свойства и им на вход в качестве аргументов подставляются объекты блока game. Помимо ранее созданных свойств на форму выносятся также [действия](Actions.md) `NEW` и `DELETE`, которые визуально будут выглядеть в виде кнопок и позволят добавлять и удалять объекты класса `Game`.

Вынесенные на форму первичные свойства примитивного типа (`date`, `hostGoals`, `guestGoals`) будут визуально выглядеть в виде ячеек, доступных для заполнения и изменения пользователем. Расчетные свойства, возвращающие атрибут иного объекта (`hostTeamName`, `guestTeamName`, `resultName`), будут выглядеть в виде ячеек, при нажатии мышкой на которую будет вызываться диалоговой окне со списком объектов и их свойствами группы `base`, значение атрибута которых возвращается (например, при нажатии на ячейку `hostTeamName` "Гости" появиться диалоговой окно со списком команд). В диалоговом окне можно выбрать один из объектов, изменив таким образом значение свойства для объекта исходной формы (например, изменить команду хозяев игры).

Расширяем форму добавлением в нее блока турнирной таблицы. Турнирная таблица будет представлять список команд (объектов класса `Team`) с их статистическими показателями, отсортированных с помощью оператора `ORDERS` по рейтинговому месту.

```lsf
EXTEND FORM MainForm
    OBJECTS team = Team
    PROPERTIES(team) place, name, gamesPlayed, gamesWon, gamesWonOT, gamesWonSO,
                     gamesLostSO, gamesLostOT, gamesLost, goalsScored, goalsConceded, points, NEW, DELETE
    ORDERS place(team)
;
```

:::info
Указанную форму можно задать и одним блоком кода без использования конструкции `EXTEND`.

```lsf
FORM MainFormSingle 'Турнирная таблица'
    OBJECTS game = Game
    PROPERTIES(game) date, hostTeamName, hostGoals, guestGoals, guestTeamName, resultName, NEW, DELETE

    OBJECTS team = Team
    PROPERTIES(team) place, name, gamesPlayed, gamesWon, gamesWonOT, gamesWonSO,
                     gamesLostSO, gamesLostOT, gamesLost, goalsScored, goalsConceded, points, NEW, DELETE
    ORDERS place(team)
;
```
:::
Выносим созданную форму на основное меню программы - предопределенную папку `root` навигатора, причем указываем, чтобы она располагалась самым первым элементом перед системным пунктом меню `'Администрирование'`.

```lsf
NAVIGATOR {
    NEW MainForm FIRST;
}
```

Процесс создания информационной системы завершен.

## Исходный код целиком

```lsf
MODULE HockeyStats;

REQUIRE System;

CLASS Team 'Команда';

name 'Название команды' = DATA STRING[30] (Team) IN base;

CLASS Game 'Игра';

date 'Дата' = DATA DATE (Game);
hostTeam = DATA Team (Game);
guestTeam = DATA Team (Game);
hostTeamName 'Хозяева' (Game game) = name(hostTeam(game));
guestTeamName 'Гости' (Game game) = name(guestTeam(game));

CONSTRAINT hostTeam(Game team) = guestTeam(team) CHECKED BY hostTeam, guestTeam MESSAGE 'Хозяйская и гостевая команды должны быть разными';

hostGoals 'Х голы' = DATA INTEGER (Game);
guestGoals 'Г голы' = DATA INTEGER (Game);

CONSTRAINT hostGoals(Game game) = guestGoals(game) MESSAGE 'Игра не может закончиться вничью';

winner(Game game) = IF hostGoals(game) > guestGoals(game)
                    THEN hostTeam(game)
                    ELSE guestTeam(game);

looser(Game game) = IF hostGoals(game) > guestGoals(game)
                    THEN guestTeam(game)
                    ELSE hostTeam(game);

CLASS GameResult 'Р/И' {
    win 'П',
    winOT 'ПО',
    winSO 'ПБ'
}

resultName 'Имя' (GameResult game) = staticCaption(game) IF game IS GameResult IN base;

userResult = DATA GameResult (Game);
result (Game game) = OVERRIDE userResult(game),
    (GameResult.win IF ((hostGoals(game) (-) guestGoals(game)) > 1 OR (guestGoals(game) (-) hostGoals(game)) > 1));
resultName 'Р/И' (Game game) = resultName(result(game));

CONSTRAINT ((hostGoals(Game game) (-) guestGoals(game)) > 1 OR (hostGoals(game) (-) guestGoals(game)) < -1) AND userResult(game)
    MESSAGE 'Результат игры определен автоматически';

CONSTRAINT ((hostGoals(Game game) (-) guestGoals(game)) < 2 AND (hostGoals(game) (-) guestGoals(game)) > -2) AND NOT userResult(game)
    MESSAGE 'Укажите результат игры';

hostGamesPlayed = GROUP SUM 1 BY hostTeam(Game game);
guestGamesPlayed = GROUP SUM 1 BY guestTeam(Game game);
gamesPlayed 'И' (Team team) = hostGamesPlayed(team) (+) guestGamesPlayed(team);

gamesWonBy(Team team, GameResult type) = OVERRIDE [GROUP SUM 1 BY winner(Game game), result(game)](team, type), 0;

gamesWon 'В' (Team team) = gamesWonBy(team, GameResult.win);
gamesWonOT 'ВО' (Team team) = gamesWonBy(team, GameResult.winOT);
gamesWonSO 'ВБ' (Team team) = gamesWonBy(team, GameResult.winSO);

gamesLostBy(Team team, GameResult type) = OVERRIDE [GROUP SUM 1 BY looser(Game game), result(game)](team, type), 0;

gamesLost 'П' (Team team) = gamesLostBy(team, GameResult.win);
gamesLostOT 'ПО' (Team team) = gamesLostBy(team, GameResult.winOT);
gamesLostSO 'ПБ' (Team team) = gamesLostBy(team, GameResult.winSO);

points 'Очки' (Team team) = gamesWon(team) * 3 + (gamesWonSO(team) + gamesWonOT(team)) * 2 + gamesLostOT(team) + gamesLostSO(team);

hostGoalsScored = GROUP SUM hostGoals(Game game) BY hostTeam(game);
guestGoalsScored = GROUP SUM guestGoals(Game game) BY guestTeam(game);
goalsScored 'Кол-во забитых голов' (Team team) = OVERRIDE hostGoalsScored(team) (+) guestGoalsScored(team), 0 IF team IS Team;

hostGoalsConceded = GROUP SUM guestGoals(Game game) BY hostTeam(game);
guestGoalsConceded = GROUP SUM hostGoals(Game game) BY guestTeam(game);
goalsConceded 'Кол-во пропущенных голов' (Team team) = OVERRIDE hostGoalsConceded(team) (+) guestGoalsConceded(team), 0 IF team IS Team;

place 'Место' (Team team) = PARTITION SUM 1 ORDER DESC points(team), gamesWon(team), gamesWonOT(team), gamesWonSO(team),
                                               (OVERRIDE goalsScored(team) (-) goalsConceded(team), 0), goalsScored(team);

FORM MainForm 'Турнирная таблица'
    OBJECTS game = Game
    PROPERTIES(game) date, hostTeamName, hostGoals, guestGoals, guestTeamName, resultName, NEW, DELETE
;

EXTEND FORM MainForm
    OBJECTS team = Team
    PROPERTIES(team) place, name, gamesPlayed, gamesWon, gamesWonOT, gamesWonSO,
                     gamesLostSO, gamesLostOT, gamesLost, goalsScored, goalsConceded, points, NEW, DELETE
    ORDERS place(team)
;

FORM MainFormSingle 'Турнирная таблица'
    OBJECTS game = Game
    PROPERTIES(game) date, hostTeamName, hostGoals, guestGoals, guestTeamName, resultName, NEW, DELETE

    OBJECTS team = Team
    PROPERTIES(team) place, name, gamesPlayed, gamesWon, gamesWonOT, gamesWonSO,
                     gamesLostSO, gamesLostOT, gamesLost, goalsScored, goalsConceded, points, NEW, DELETE
    ORDERS place(team)
;

NAVIGATOR {
    NEW MainForm FIRST;
}
```
