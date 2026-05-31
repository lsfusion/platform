# DOCS-DESC-BLOCKS — реестр блоков

**Блоки file-focused:** суть блока — какие файлы дописывать (каждое имя = пара `docs/<type>/en/<имя>.md` + `docs/<type>/ru/<имя>.md`, type ∈ paradigm/language). Если конструкции блока ещё нет статьи — исполнитель создаёт оба файла в нужной type-папке И сам подключает их в `docs/sidebars.js` тем же блочным коммитом (`sidebars.js` — часть документации, см. `docs/AGENTS.md`).
Процесс — `DOCS-DESC-PLAN.md`; промпты — `DOCS-DESC-PROMPTS.md`; инструкция людям — `DOCS-DESC-EXECUTOR.md`. Граф — из дерева документации (`docs/sidebars.js` / структуры папок paradigm+language); how-to — позже. Каждый блок ведётся в своей ветке от `master`, оркестратор мерджит её в `master`. Отметки ведёт **только оркестратор**.

## Треки (домены)

Блоки сгруппированы в **6 тематических треков**. **Один исполнитель ведёт один трек целиком** (снизу вверх внутри трека) — чтобы не прыгать между темами. Несколько исполнителей = несколько треков параллельно.

| Трек | Домен | Блоков | Исполнитель |
|------|-------|:-----:|-------------|
| T1 | Логическая модель — данные и выражения | 9 | Alex (оркестратор) |
| T2 | Логическая модель — множества, события, ограничения | 7 | @Alexey Byimistrov |
| T3 | Действия | 7 | @Chavchavadze |
| T4 | Формы и взаимодействие | 10 | @Pavel Miniutka |
| T5 | Физическая модель, развитие, язык, корни | 7 | — |
| T6 | Интеграция, исполнение, управление, обучение | 9 | @DAle |

## Порядок внутри трека — снизу вверх (дедуктивно)

**Слой 1** — листовые блоки; **слой 2** — обзорные (категория, чьи дочерние статьи в других блоках; пишутся поверх них); **слой 3** — корни. Внутри трека идти от слоя 1 к 3; обзорный/корневой блок не брать, пока его дочерние не описаны (дочерние могут быть в другом треке — тогда обзор ждёт их). Статусы: `todo`→`in-progress`→`done` (+`blocked`). Внутри блока сначала Paradigm, потом Language.

## Блоки по трекам

| Трек | Слой | Block | Зона | Заголовок | Tier | Файлы P/L | Status | Owner |
|------|:----:|-------|------|-----------|:----:|:---------:|--------|-------|
| T1 | 1 | B02 | Логическая модель | Классы | P+L | 6/4 | done | Alex (оркестратор) |
| T1 | 1 | B05 | Логическая модель | Арифметика/сравнение/логика | P+L | 4/3 | done | Alex (оркестратор) |
| T1 | 1 | B06 | Логическая модель | Строки/округление/экстремум/конверсия/структуры | P+L | 6/11 | done | Alex (оркестратор) |
| T1 | 1 | B07 | Логическая модель | Композиция (JOIN) | P+L | 1/1 | done | Alex (оркестратор) |
| T1 | 1 | B08 | Логическая модель | Выборка (CASE/IF/MULTI/OVERRIDE/EXCLUSIVE) | P+L | 1/6 | done | Alex (оркестратор) |
| T1 | 1 | B09 | Логическая модель | Фильтрация и порядок | P | 2/0 | done | Alex (оркестратор) |
| T1 | 2 | B03 | Логическая модель | Свойства и данные | P+L | 2/2 | done | Alex (оркестратор) |
| T1 | 2 | B04 | Логическая модель | Операторы-выражения: основа | P+L | 2/4 | done | Alex (оркестратор) |
| T1 | 3 | B01 | Логическая модель | Логическая модель — корень | P | 2/0 | done | Alex (оркестратор) |
| T2 | 1 | B11 | Логическая модель | Группировка и распределение | P+L | 2/2 | in-progress | @Alexey Byimistrov |
| T2 | 1 | B12 | Логическая модель | Партиционирование и рекурсия | P+L | 2/2 | todo | @Alexey Byimistrov |
| T2 | 1 | B13 | Логическая модель | Агрегации (AGGR) | P+L | 1/1 | todo | @Alexey Byimistrov |
| T2 | 1 | B14 | Логическая модель | События | P+L | 3/7 | todo | @Alexey Byimistrov |
| T2 | 1 | B15 | Логическая модель | Ограничения | P+L | 2/2 | todo | @Alexey Byimistrov |
| T2 | 1 | B16 | Логическая модель | PREV и change-операторы | P+L | 2/2 | todo | @Alexey Byimistrov |
| T2 | 2 | B10 | Логическая модель | Set-операции — обзор | P | 1/0 | todo | @Alexey Byimistrov |
| T3 | 1 | B18 | Действия | Изменение состояния | P+L | 4/5 | in-progress | @Chavchavadze |
| T3 | 1 | B19 | Действия | Ветвление, циклы, выход | P+L | 9/11 | todo | @Chavchavadze |
| T3 | 1 | B20 | Действия | Управление сессиями | P+L | 5/4 | todo | @Chavchavadze |
| T3 | 1 | B21 | Действия | Потоки и соединения | P+L | 2/3 | todo | @Chavchavadze |
| T3 | 1 | B22 | Действия | Активация (ACTIVATE/ACTIVE) | P+L | 2/2 | todo | @Chavchavadze |
| T3 | 1 | B23 | Взаимодействие | Сообщения и ввод значений | P+L | 4/4 | todo | @Chavchavadze |
| T3 | 2 | B17 | Действия | Действия — обзор, обёртка ACTION, порядок выполнения | P+L | 3/5 | todo | @Chavchavadze |
| T4 | 1 | B24 | Взаимодействие | Взаимодействие с пользователем и открытие форм | P+L | 2/2 | in-progress | @Pavel Miniutka |
| T4 | 1 | B25 | Взаимодействие | Печать и структурированный вид | P+L | 4/1 | todo | @Pavel Miniutka |
| T4 | 1 | B26 | Взаимодействие | Фокус/дерево объектов/скриншот | P+L | 4/4 | todo | @Pavel Miniutka |
| T4 | 1 | B28 | Формы | Дизайн формы и представления | P+L | 3/2 | todo | @Pavel Miniutka |
| T4 | 1 | B29 | Формы | Интерактивный вид | P | 2/0 | todo | @Pavel Miniutka |
| T4 | 1 | B30 | Формы | Статический вид (VIEW) | P | 2/0 | todo | @Pavel Miniutka |
| T4 | 1 | B31 | Формы | События формы | P | 1/0 | todo | @Pavel Miniutka |
| T4 | 1 | B32 | Формы | Навигатор и UI | P+L | 4/2 | todo | @Pavel Miniutka |
| T4 | 1 | B33 | Формы | Расширение формы | P+L | 1/1 | todo | @Pavel Miniutka |
| T4 | 2 | B27 | Формы | Формы — структура и операторы | P+L | 3/4 | todo | @Pavel Miniutka |
| T5 | 1 | B34 | Физическая модель | Таблицы, индексы, материализации | P+L | 3/2 | todo |  |
| T5 | 1 | B37 | Развитие | Метапрограммирование | P+L | 1/2 | todo |  |
| T5 | 1 | B38 | Развитие | Миграция/локализация/поиск | P | 3/0 | todo |  |
| T5 | 1 | B39 | Развитие | Основы языка | L | 0/4 | todo |  |
| T5 | 2 | B35 | Развитие | Модульность и идентификация | P+L | 5/3 | todo |  |
| T5 | 2 | B36 | Развитие | Расширения | P+L | 4/3 | todo |  |
| T5 | 3 | B00 | Корни | Корневые обзоры Paradigm/Language | P+L | 3/2 | todo |  |
| T6 | 1 | B41 | Интеграция | EVAL / Java / Spring | P+L | 3/1 | in-progress | @DAle |
| T6 | 1 | B42 | Интеграция | Файлы и обмен данными | P+L | 6/5 | todo | @DAle |
| T6 | 1 | B45 | Управление | Мониторинг и сервисы | P | 5/0 | todo | @DAle |
| T6 | 1 | B46 | Установка | Установка и среда разработки | P | 9/0 | todo | @DAle |
| T6 | 1 | B48 | Обучение | Учебные приложения | P | 2/0 | todo | @DAle |
| T6 | 2 | B40 | Интеграция | Доступ к внешним/внутренним системам | P+L | 6/2 | todo | @DAle |
| T6 | 2 | B43 | Исполнение | Исполнение и планировщик | P | 6/0 | todo | @DAle |
| T6 | 2 | B44 | Управление | Параметры и безопасность | P | 5/0 | todo | @DAle |
| T6 | 2 | B47 | Обучение | Обучающие материалы | P | 4/0 | todo | @DAle |

## Файлы по блокам (по трекам)

### Трек T1 — Логическая модель — данные и выражения

#### B02 — Классы  · слой 1 · P+L
_Классы, пользовательские/встроенные классы, статические объекты, операторы классов, классификация._
- **Paradigm:** `Classes`, `User_classes`, `Built-in_classes`, `Static_objects`, `Class_operators`, `Classification_IS_AS`
- **Language:** `CLASS_statement`, `IS_AS_operators`, `ISCLASS_operator`, `Property_signature_ISCLASS`

#### B05 — Арифметика/сравнение/логика  · слой 1 · P+L
_Константа, арифметика, сравнение, логика._
- **Paradigm:** `Constant`, `Arithmetic_operators_plus_minus_etc`, `Comparison_operators_=_etc`, `Logical_operators_AND_OR_NOT_XOR`
- **Language:** `Arithmetic_operators`, `Comparison_operators`, `AND_OR_NOT_XOR_operators`

#### B06 — Строки/округление/экстремум/конверсия/структуры  · слой 1 · P+L
_Строковые операторы, округление, экстремум, конверсия типов, структуры, пользовательская формула._
- **Paradigm:** `String_operators_plus_CONCAT_SUBSTRING`, `Rounding_operator_ROUND`, `Extremum_MAX_MIN`, `Type_conversion`, `Structure_operators_STRUCT`, `Custom_formula_FORMULA`
- **Language:** `CONCAT_operator`, `ROUND_operator`, `MAX_operator`, `MIN_operator`, `Type_conversion_operator`, `STRUCT_operator`, `FORMULA_operator`, `JSON_operator`, `LIKE_operator`, `MATCH_operator`, `Brackets_operator`

#### B07 — Композиция (JOIN)  · слой 1 · P+L
_Композиция свойств/действий._
- **Paradigm:** `Composition_JOIN`
- **Language:** `JOIN_operator`

#### B08 — Выборка (CASE/IF/MULTI/OVERRIDE/EXCLUSIVE)  · слой 1 · P+L
_Операторы выборки значения._
- **Paradigm:** `Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE`
- **Language:** `CASE_operator`, `IF_operator`, `IF_..._THEN_operator`, `MULTI_operator`, `OVERRIDE_operator`, `EXCLUSIVE_operator`

#### B09 — Фильтрация и порядок  · слой 1 · P
_FILTER и ORDER как абстракции (синтаксис описан внутри операторов, их использующих)._
- **Paradigm:** `Filter_FILTER`, `Order_ORDER`
- **Language:** —

#### B03 — Свойства и данные  · слой 2 · P+L
_Свойства, data-свойства, опции свойств._
- **Paradigm:** `Properties`, `Data_properties_DATA`
- **Language:** `DATA_operator`, `Property_options`

#### B04 — Операторы-выражения: основа  · слой 2 · P+L
_Зонтик операторов-выражения и операций над примитивами + грамматика выражения, приоритеты, объявление через =._
- **Paradigm:** `Property_operators_paradigm`, `Operations_with_primitives`
- **Language:** `Property_operators`, `Expression`, `Operator_priority`, `=_statement`

#### B01 — Логическая модель — корень  · слой 3 · P
_Корневой обзор логической модели и доменной логики._
- **Paradigm:** `Logical_model`, `Domain_logic`
- **Language:** —

### Трек T2 — Логическая модель — множества, события, ограничения

#### B11 — Группировка и распределение  · слой 1 · P+L
_GROUP и UNGROUP._
- **Paradigm:** `Grouping_GROUP`, `Distribution_UNGROUP`
- **Language:** `GROUP_operator`, `UNGROUP_operator`

#### B12 — Партиционирование и рекурсия  · слой 1 · P+L
_PARTITION и RECURSION._
- **Paradigm:** `Partitioning_sorting_PARTITION_..._ORDER`, `Recursion_RECURSION`
- **Language:** `PARTITION_operator`, `RECURSION_operator`

#### B13 — Агрегации (AGGR)  · слой 1 · P+L
_Агрегации._
- **Paradigm:** `Aggregations`
- **Language:** `AGGR_operator`

#### B14 — События  · слой 1 · P+L
_События, вычисляемые/простые события, блоки событий._
- **Paradigm:** `Events`, `Calculated_events`, `Simple_event`
- **Language:** `WHEN_statement`, `ON_statement`, `AFTER_statement`, `BEFORE_statement`, `Event_block`, `lt-_WHEN_statement`, `Event_description_block`

#### B15 — Ограничения  · слой 1 · P+L
_Ограничения, простые ограничения, оператор-следствие =>._
- **Paradigm:** `Constraints`, `Simple_constraints`
- **Language:** `CONSTRAINT_statement`, `=gt_statement`

#### B16 — PREV и change-операторы  · слой 1 · P+L
_Предыдущее значение PREV и операторы изменения SET/CHANGED._
- **Paradigm:** `Previous_value_PREV`, `Change_operators_SET_CHANGED_etc`
- **Language:** `PREV_operator`, `Change_operators`

#### B10 — Set-операции — обзор  · слой 2 · P
_Корневой обзор set-операций._
- **Paradigm:** `Set_operations`
- **Language:** —

### Трек T3 — Действия

#### B18 — Изменение состояния  · слой 1 · P+L
_Изменение состояния: CHANGE/NEW/CHANGECLASS/DELETE._
- **Paradigm:** `State_change`, `Property_change_CHANGE`, `New_object_NEW`, `Class_change_CHANGECLASS_DELETE`
- **Language:** `CHANGE_operator`, `NEW_operator`, `CHANGECLASS_operator`, `DELETE_operator`, `plus_equals_statement`
- **Создать (новая статья):** `ASYNCUPDATE_operator` — действие асинхронного обновления свойства (paradigm — в State_change).

#### B19 — Ветвление, циклы, выход  · слой 1 · P+L
_Последовательность, ветвление/циклы/прерывания/выход/исключения/вызов._
- **Paradigm:** `Sequence`, `Branching_CASE_IF_MULTI`, `Loop_FOR`, `Recursive_loop_WHILE`, `Interruption_BREAK`, `Next_iteration_CONTINUE`, `Exit_RETURN`, `Exception_handling_TRY`, `Call_EXEC`
- **Language:** `FOR_operator`, `WHILE_operator`, `BREAK_operator`, `CONTINUE_operator`, `RETURN_operator`, `TRY_operator`, `EXEC_operator`, `CASE_action_operator`, `IF_..._THEN_action_operator`, `MULTI_action_operator`, `Braces_operator`

#### B20 — Управление сессиями  · слой 1 · P+L
_Сессии изменений, APPLY/CANCEL, новая/вложенная сессия._
- **Paradigm:** `Session_management`, `Change_sessions`, `Apply_changes_APPLY`, `Cancel_changes_CANCEL`, `New_session_NEWSESSION_NESTEDSESSION`
- **Language:** `APPLY_operator`, `CANCEL_operator`, `NEWSESSION_operator`, `NESTEDSESSION_operator`

#### B21 — Потоки и соединения  · слой 1 · P+L
_Создание потоков NEWTHREAD/NEWEXECUTOR, новое соединение._
- **Paradigm:** `New_threads_NEWTHREAD_NEWEXECUTOR`, `New_connection_NEWCONNECTION`
- **Language:** `NEWTHREAD_operator`, `NEWEXECUTOR_operator`, `NEWCONNECTION_operator`

#### B22 — Активация (ACTIVATE/ACTIVE)  · слой 1 · P+L
_Активация и активность._
- **Paradigm:** `Activation_ACTIVATE`, `Activity_ACTIVE`
- **Language:** `ACTIVATE_operator`, `ACTIVE_operator`

#### B23 — Сообщения и ввод значений  · слой 1 · P+L
_Сообщения MESSAGE/ASK, ввод значений, примитивный ввод, запрос значения._
- **Paradigm:** `Show_message_MESSAGE_ASK`, `Value_input`, `Primitive_input_INPUT`, `Value_request_REQUEST`
- **Language:** `MESSAGE_operator`, `ASK_operator`, `INPUT_operator`, `REQUEST_operator`

#### B17 — Действия — обзор, обёртка ACTION, порядок выполнения  · слой 2 · P+L
_Корень действий, операторы-действия, объявление ACTION, опции; обзор порядка выполнения._
- **Paradigm:** `Actions`, `Action_operators_paradigm`, `Execution_order`
- **Language:** `Action_operators`, `ACTION_statement`, `ACTION_plus_statement`, `Action_options`, `Empty_statement`

### Трек T4 — Формы и взаимодействие

#### B24 — Взаимодействие с пользователем и открытие форм  · слой 1 · P+L
_Взаимодействие через IS, открытие формы, интерактивный показ._
- **Paradigm:** `User_IS_interaction`, `Open_form`
- **Language:** `SHOW_operator`, `DIALOG_operator`

#### B25 — Печать и структурированный вид  · слой 1 · P+L
_Печатное и структурированное представления формы._
- **Paradigm:** `Print_view`, `In_a_print_view_PRINT`, `Structured_view`, `In_a_structured_view_EXPORT_IMPORT`
- **Language:** `PRINT_operator`

#### B26 — Фокус/дерево объектов/скриншот  · слой 1 · P+L
_Операторы фокуса, видимость дерева объектов, операторы групп объектов, скриншот._
- **Paradigm:** `Focus_operators`, `Object_tree_visibility_EXPAND_COLLAPSE`, `Object_group_operators`, `Capture_SCREENSHOT`
- **Language:** `SCREENSHOT_operator`, `EXPAND_operator`, `COLLAPSE_operator`, `Object_group_operator`

#### B28 — Дизайн формы и представления  · слой 1 · P+L
_Дизайн формы, представления, отчётный дизайн, pivot._
- **Paradigm:** `Form_design`, `Form_views`, `Report_design`
- **Language:** `DESIGN_statement`, `Pivot_block`

#### B29 — Интерактивный вид  · слой 1 · P
_Интерактивное представление и SHOW/DIALOG-семантика._
- **Paradigm:** `Interactive_view`, `In_an_interactive_view_SHOW_DIALOG`
- **Language:** —

#### B30 — Статический вид (VIEW)  · слой 1 · P
_Статическое представление и оператор VIEW._
- **Paradigm:** `Static_view`, `View_VIEW`
- **Language:** —

#### B31 — События формы  · слой 1 · P
_События формы._
- **Paradigm:** `Form_events`
- **Language:** —

#### B32 — Навигатор и UI  · слой 1 · P+L
_Навигатор, дизайн навигатора, пользовательский интерфейс, иконки._
- **Paradigm:** `Navigator`, `Navigator_design`, `User_interface`, `Icons`
- **Language:** `NAVIGATOR_statement`, `WINDOW_statement`

#### B33 — Расширение формы  · слой 1 · P+L
_Расширение формы._
- **Paradigm:** `Form_extension`
- **Language:** `EXTEND_FORM_statement`

#### B27 — Формы — структура и операторы  · слой 2 · P+L
_Корень форм, структура формы, операторы формы._
- **Paradigm:** `Forms`, `Form_structure`, `Form_operators`
- **Language:** `FORM_statement`, `Object_blocks`, `Properties_and_actions_block`, `Filters_and_sortings_block`

### Трек T5 — Физическая модель, развитие, язык, корни

#### B34 — Таблицы, индексы, материализации  · слой 1 · P+L
_Таблицы, индексы, материализации._
- **Paradigm:** `Tables`, `Indexes`, `Materializations`
- **Language:** `TABLE_statement`, `INDEX_statement`
- **Создать (новая статья):** `RECALCULATE_operator` — действие пересчёта хранимого/материализованного свойства (опция NOCLASSES) (paradigm — в Materializations).

#### B37 — Метапрограммирование  · слой 1 · P+L
_Метапрограммирование и META/@._
- **Paradigm:** `Metaprogramming`
- **Language:** `META_statement`, `commat_statement`
- **Создать (новая статья):** `REFLECTION_operator` — свойство-рефлексия (метаданные, напр. CANONICALNAME) (paradigm — в Metaprogramming).

#### B38 — Миграция/локализация/поиск  · слой 1 · P
_Миграция, интернационализация, поиск._
- **Paradigm:** `Migration`, `Internationalization`, `Search_`
- **Language:** —

#### B39 — Основы языка  · слой 1 · L
_Базовые языковые статьи: токены, литералы, комментарии, соглашения._
- **Paradigm:** —
- **Language:** `Tokens`, `Literals`, `Comments`, `Coding_conventions`

#### B35 — Модульность и идентификация  · слой 2 · P+L
_Модульность, модули, группы свойств/действий, именование, идентификация элементов._
- **Paradigm:** `Modularity`, `Modules`, `Groups_of_properties_and_actions`, `Naming`, `Element_identification`
- **Language:** `Module_header`, `GROUP_statement`, `IDs`

#### B36 — Расширения  · слой 2 · P+L
_Расширения класса/свойства/действия + ABSTRACT-полиморфизм._
- **Paradigm:** `Extensions`, `Class_extension`, `Property_extension`, `Action_extension`
- **Language:** `EXTEND_CLASS_statement`, `ABSTRACT_operator`, `ABSTRACT_action_operator`

#### B00 — Корневые обзоры Paradigm/Language  · слой 3 · P+L
_Корни tier'ов и моделей: Paradigm, View_logic, Physical_model; Language и обзор statement'ов._
- **Paradigm:** `Paradigm`, `View_logic`, `Physical_model`
- **Language:** `Language`, `Statements`

### Трек T6 — Интеграция, исполнение, управление, обучение

#### B41 — EVAL / Java / Spring  · слой 1 · P+L
_EVAL, Java integration API, кастомный Spring-bean._
- **Paradigm:** `Eval_EVAL`, `Java_integration_API`, `Custom_Spring_bean_EventServer`
- **Language:** `EVAL_operator`

#### B42 — Файлы и обмен данными  · слой 1 · P+L
_Файловые операторы, READ/WRITE, EXPORT/IMPORT данных, EMAIL._
- **Paradigm:** `File_operators`, `Read_file_READ`, `Write_file_WRITE`, `Data_export_EXPORT`, `Data_import_IMPORT`, `Send_mail_EMAIL`
- **Language:** `READ_operator`, `WRITE_operator`, `EXPORT_operator`, `IMPORT_operator`, `EMAIL_operator`

#### B45 — Мониторинг и сервисы  · слой 1 · P
_Монитор процессов, профайлер, журналы, MCP-сервер, чат._
- **Paradigm:** `Process_monitor`, `Profiler`, `Journals_and_logs`, `MCP_server`, `Chat`
- **Language:** —
- **Создать (новая статья):** `SHOWDEP_operator` — диагностическое действие (зависимости/рекурсия свойств; покрывает SHOWREC) (paradigm — в обзоре управления).

#### B46 — Установка и среда разработки  · слой 1 · P
_Установка (авто/ручная), Docker, разработка (auto/manual), IDE, проекты._
- **Paradigm:** `Install`, `Automatic_installation`, `Manual_installation`, `Docker`, `Development`, `Development_auto`, `Development_manual`, `IDE`, `Projects`
- **Language:** —

#### B48 — Учебные приложения  · слой 1 · P
_Учебные примеры приложений._
- **Paradigm:** `Materials_management`, `Score_table`
- **Language:** —

#### B40 — Доступ к внешним/внутренним системам  · слой 2 · P+L
_Интеграция (обзор), EXTERNAL/INTERNAL доступ в обе стороны, внутренний вызов._
- **Paradigm:** `Integration`, `Access_to_an_external_system_EXTERNAL`, `Access_from_an_external_system`, `Access_to_an_internal_system_INTERNAL_FORMULA`, `Access_from_an_internal_system`, `Internal_call_INTERNAL`
- **Language:** `EXTERNAL_operator`, `INTERNAL_operator`

#### B43 — Исполнение и планировщик  · слой 2 · P
_Исполнение (обзор/auto/manual), планировщик, события и параметры запуска._
- **Paradigm:** `Execution`, `Execution_auto`, `Execution_manual`, `Scheduler`, `Launch_events`, `Launch_parameters`
- **Language:** —

#### B44 — Параметры и безопасность  · слой 2 · P
_Управление (обзор), системные/рабочие параметры, политика безопасности, интерпретатор._
- **Paradigm:** `Management`, `System_parameters`, `Working_parameters`, `Security_policy`, `Interpreter`
- **Language:** —

#### B47 — Обучающие материалы  · слой 2 · P
_Learn, обучающие материалы, примеры, онлайн-демо._
- **Paradigm:** `Learn`, `Learning_materials`, `Examples`, `Online_demo`
- **Language:** —

## Первичные назначения

Каждый владеет всем своим треком, идёт снизу вверх:
- **Alex (оркестратор) → T1** (логика: данные/выражения) — **базовый/ключевой** фундамент (классы/свойства/выражения), нужен раньше всех; старт **B02** (Классы). _Это критический путь — не давать ему простаивать из-за оркестрации._
- **@Alexey Byimistrov → T2** (множества/события/ограничения); старт **B11** (Группировка и распределение).
- **@Chavchavadze → T3** (действия); старт **B18** (Изменение состояния).
- **@Pavel Miniutka → T4** (формы и взаимодействие); старт **B24** (Взаимодействие/открытие форм).
- **@DAle → T6** (интеграция/исполнение/управление/обучение) — **неблокирующий** трек (терминальные темы, ни от кого не на критическом пути): медленнее, но качественнее, и его темп никого не держит; старт **B41**.
Закрыл блок — берёт следующий блок своего трека (снизу вверх). Обзорные/корневые блоки трека (слой 2-3) ждут, пока их дочерние описаны (в т.ч. в других треках). Трек **T5** (физмодель/развитие/язык/корни) подключается позже — корни и так идут последними.

> Примечание: `MCP_server` (B45) — новая статья, ещё не подключённая в `docs/sidebars.js`; исполнителю определить её место в дереве и подключить тем же блочным коммитом.

## Полнота против грамматики (сверено с codex)

Аудит грамматики `LsfLogics.g` против статей: **на уровне статей покрытие полное** — все 159 paradigm + 121 language статей разложены по блокам. Сверка ключевых слов грамматики (мой греп + codex по коду) дала ниже.

### Новые статьи — СОЗДАЁМ (конструкция нигде не описана)

Создаёт исполнитель блока (language-статья в `docs/language/{en,ru}/`), paradigm-аспект — в указанной существующей статье; исполнитель подключает новый файл в `docs/sidebars.js` тем же блочным коммитом.

| Создать (language) | Что это | Блок | Paradigm-описание |
|---|---|---|---|
| `ASYNCUPDATE_operator` | действие асинхронного обновления свойства (`addScriptedAsyncUpdateProp`) | **B18** (T3) | в `State_change` |
| `RECALCULATE_operator` | действие пересчёта хранимого/материализованного свойства (`RECALCULATE [CLASSES] prop(...) [WHERE ...]`) | **B34** (T5) | в `Materializations` |
| `REFLECTION_operator` | свойство-рефлексия (метаданные, напр. `CANONICALNAME`) | **B37** (T5) | в `Metaprogramming` |
| `SHOWDEP_operator` | диагностическое действие (зависимости/рекурсия; покрывает `SHOWREC`, одно правило `addScriptedShowRecDepAction`) | **B45** (T6) | в обзоре управления |

### Опции/под-конструкции — покрыть ВНУТРИ существующих статей (content-gap, не новые статьи)

Цель блока — описать их внутри указанной статьи (требование «полнота по коду»); цели — по codex:

- **B02 `Built-in_classes`:** `INTERVAL`, `ZDATETIME` (типы), `TEXTFILE`/`TEXTLINK` (файл/ссылка-классы, и семейство *FILE/*LINK).
- **B03 `Property_options`:** `HINT`/`NOHINT`, `NOCOMPLEX`, `PREREAD`, `MOUSE` (`CHANGEMOUSE`).
- **B14 события (`Event_block`/`WHEN`/`FOR`):** `REPLACE`/`NOREPLACE` (политика обработчиков), `INLINE`/`NOINLINE`.
- **B21 `NEWTHREAD_operator`:** `CONNECTION` (`NEWTHREAD ... CONNECTION expr`).
- **B22 `ACTIVATE_operator`:** `SEEK` (поиск/позиционирование объекта, FIRST/LAST/NULL).
- **B23 `INPUT_operator`:** `ACTIONS` (context actions), `FOCUSED`/`HOVER`/`SELECTED` (quick access/toolbar).
- **B24 `DIALOG_operator` / `Open_form`:** `CHECK`, `EMBEDDED` (windowType), `NOCHANGE`, `THISSESSION` (form session scope).
- **B26 объекты:** `CONTAINER` (`COLLAPSE`/`EXPAND`), `VIEWTYPE` (`Object_group_operator`).
- **B27 form-блоки (`Properties_and_actions_block`/`Object_blocks`):** `COLUMN`/`MEASURE` (pivot роли), `CONFIG` (`PIVOT ... CONFIG`), `DISABLEIF`, `HINTNOUPDATE`, `HINTTABLE`, `NOEXTID`, `NOSELECT`, `OPTIONS`, `POPUP`.
- **B27 ↔ `Property_options` (из B03):** опции, задаваемые в объявлении свойства целиком, но по сути являющиеся опциями свойства на форме — `CUSTOM`/`SELECT`/`NOSELECT` (customView), `EXTID` — описать в form-блоке B27 и доописать в `Property_options` со ссылкой. (`AGGR` как опция свойства относится к оператору агрегации — покрыть в агрегациях.)
- **B28 `Form_design`/`DESIGN_statement`:** `CLASSCHOOSER`, `FILTERBOX`, `FILTERCONTROLS`.
- **B28 `Report_design` / B27 `FORM_statement`:** `REPORTS`, `REPORTFILES`.
- **B40 `EXTERNAL_operator`:** `JAVA` (формат внешнего вызова).
- **B42 `EXPORT_operator`:** `TAG` (XML-export опция).
- Опции новых статей: `NOCLASSES` → внутри `RECALCULATE`; `CANONICALNAME` → внутри `REFLECTION`.

> Системная библиотека (встроенные модули `.lsf` в `server/src/main/lsfusion`: `currentDate`, метапривязки и т.п.) — это **справочник библиотеки**, не язык/парадигма; вне текущего графа (кандидат на отдельную reference/how-to серию позже).