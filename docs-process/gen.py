#!/usr/bin/env python3
# Генератор графа блоков и промптов (canonical, в repo platform).
# Запуск:  python3 docs-blocks-gen/gen.py   — пишет DOCS-DESC-BLOCKS.md, DOCS-DESC-PROMPTS.md, prompts/B*.txt в корень platform.
# DOCS-DESC-PLAN.md и DOCS-DESC-EXECUTOR.md ведутся вручную (не генерируются).
import os
OUT=os.path.dirname(os.path.abspath(__file__))  # папка docs-process — туда пишутся BLOCKS/PROMPTS
# Тип статьи (paradigm/language) задаётся самим графом (списки ps/ls ниже) = type-first папкой docs/<type>/.
B=[]
def b(*a): B.append(a)

b("B00","Корни","Корневые обзоры Paradigm/Language",
  "Корни tier'ов и моделей: Paradigm, View_logic, Physical_model; Language и обзор statement'ов.",
  ["Paradigm","View_logic","Physical_model"],["Language","Statements"])
b("B01","Логическая модель","Логическая модель — корень",
  "Корневой обзор логической модели и доменной логики.",
  ["Logical_model","Domain_logic"],[])
b("B02","Логическая модель","Классы",
  "Классы, пользовательские/встроенные классы, статические объекты, операторы классов, классификация.",
  ["Classes","User_classes","Built-in_classes","Static_objects","Class_operators","Classification_IS_AS"],
  ["CLASS_statement","IS_AS_operators","ISCLASS_operator","Property_signature_ISCLASS"])
b("B03","Логическая модель","Свойства и данные",
  "Свойства, data-свойства, опции свойств.",
  ["Properties","Data_properties_DATA"],
  ["DATA_operator","Property_options"])
b("B04","Логическая модель","Операторы-выражения: основа",
  "Зонтик операторов-выражения и операций над примитивами + грамматика выражения, приоритеты, объявление через =.",
  ["Property_operators_paradigm","Operations_with_primitives"],
  ["Property_operators","Expression","Operator_priority","=_statement"])
b("B05","Логическая модель","Арифметика/сравнение/логика",
  "Константа, арифметика, сравнение, логика.",
  ["Constant","Arithmetic_operators_plus_minus_etc","Comparison_operators_=_etc","Logical_operators_AND_OR_NOT_XOR"],
  ["Arithmetic_operators","Comparison_operators","AND_OR_NOT_XOR_operators"])
b("B06","Логическая модель","Строки/округление/экстремум/конверсия/структуры",
  "Строковые операторы, округление, экстремум, конверсия типов, структуры, пользовательская формула.",
  ["String_operators_plus_CONCAT_SUBSTRING","Rounding_operator_ROUND","Extremum_MAX_MIN","Type_conversion","Structure_operators_STRUCT","Custom_formula_FORMULA"],
  ["CONCAT_operator","ROUND_operator","MAX_operator","MIN_operator","Type_conversion_operator","STRUCT_operator","FORMULA_operator","JSON_operator","LIKE_operator","MATCH_operator","Brackets_operator"])
b("B07","Логическая модель","Композиция (JOIN)",
  "Композиция свойств/действий.",
  ["Composition_JOIN"],["JOIN_operator"])
b("B08","Логическая модель","Выборка (CASE/IF/MULTI/OVERRIDE/EXCLUSIVE)",
  "Операторы выборки значения.",
  ["Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE"],
  ["CASE_operator","IF_operator","IF_..._THEN_operator","MULTI_operator","OVERRIDE_operator","EXCLUSIVE_operator"])
b("B09","Логическая модель","Фильтрация и порядок",
  "FILTER и ORDER как абстракции (синтаксис описан внутри операторов, их использующих).",
  ["Filter_FILTER","Order_ORDER"],[])
b("B10","Логическая модель","Set-операции — обзор",
  "Корневой обзор set-операций.",
  ["Set_operations"],[])
b("B11","Логическая модель","Группировка и распределение",
  "GROUP и UNGROUP.",
  ["Grouping_GROUP","Distribution_UNGROUP"],["GROUP_operator","UNGROUP_operator"])
b("B12","Логическая модель","Партиционирование и рекурсия",
  "PARTITION и RECURSION.",
  ["Partitioning_sorting_PARTITION_..._ORDER","Recursion_RECURSION"],["PARTITION_operator","RECURSION_operator"])
b("B13","Логическая модель","Агрегации (AGGR)",
  "Агрегации.",
  ["Aggregations"],["AGGR_operator"])
b("B14","Логическая модель","События",
  "События, вычисляемые/простые события, блоки событий.",
  ["Events","Calculated_events","Simple_event"],
  ["WHEN_statement","ON_statement","AFTER_statement","BEFORE_statement","Event_block","lt-_WHEN_statement","Event_description_block"])
b("B15","Логическая модель","Ограничения",
  "Ограничения, простые ограничения, оператор-следствие =>.",
  ["Constraints","Simple_constraints"],["CONSTRAINT_statement","=gt_statement"])
b("B16","Логическая модель","PREV и change-операторы",
  "Предыдущее значение PREV и операторы изменения SET/CHANGED.",
  ["Previous_value_PREV","Change_operators_SET_CHANGED_etc"],["PREV_operator","Change_operators"])
b("B17","Действия","Действия — обзор, обёртка ACTION, порядок выполнения",
  "Корень действий, операторы-действия, объявление ACTION, опции; обзор порядка выполнения.",
  ["Actions","Action_operators_paradigm","Execution_order"],
  ["Action_operators","ACTION_statement","ACTION_plus_statement","Action_options","Empty_statement"])
b("B18","Действия","Изменение состояния",
  "Изменение состояния: CHANGE/NEW/CHANGECLASS/DELETE.",
  ["State_change","Property_change_CHANGE","New_object_NEW","Class_change_CHANGECLASS_DELETE"],
  ["CHANGE_operator","NEW_operator","CHANGECLASS_operator","DELETE_operator","plus_equals_statement"])
b("B19","Действия","Ветвление, циклы, выход",
  "Последовательность, ветвление/циклы/прерывания/выход/исключения/вызов.",
  ["Sequence","Branching_CASE_IF_MULTI","Loop_FOR","Recursive_loop_WHILE","Interruption_BREAK","Next_iteration_CONTINUE","Exit_RETURN","Exception_handling_TRY","Call_EXEC"],
  ["FOR_operator","WHILE_operator","BREAK_operator","CONTINUE_operator","RETURN_operator","TRY_operator","EXEC_operator","CASE_action_operator","IF_..._THEN_action_operator","MULTI_action_operator","Braces_operator"])
b("B20","Действия","Управление сессиями",
  "Сессии изменений, APPLY/CANCEL, новая/вложенная сессия.",
  ["Session_management","Change_sessions","Apply_changes_APPLY","Cancel_changes_CANCEL","New_session_NEWSESSION_NESTEDSESSION"],
  ["APPLY_operator","CANCEL_operator","NEWSESSION_operator","NESTEDSESSION_operator"])
b("B21","Действия","Потоки и соединения",
  "Создание потоков NEWTHREAD/NEWEXECUTOR, новое соединение.",
  ["New_threads_NEWTHREAD_NEWEXECUTOR","New_connection_NEWCONNECTION"],
  ["NEWTHREAD_operator","NEWEXECUTOR_operator","NEWCONNECTION_operator"])
b("B22","Действия","Активация (ACTIVATE/ACTIVE)",
  "Активация и активность.",
  ["Activation_ACTIVATE","Activity_ACTIVE"],["ACTIVATE_operator","ACTIVE_operator"])
b("B23","Взаимодействие","Сообщения и ввод значений",
  "Сообщения MESSAGE/ASK, ввод значений, примитивный ввод, запрос значения.",
  ["Show_message_MESSAGE_ASK","Value_input","Primitive_input_INPUT","Value_request_REQUEST"],
  ["MESSAGE_operator","ASK_operator","INPUT_operator","REQUEST_operator"])
b("B24","Взаимодействие","Взаимодействие с пользователем и открытие форм",
  "Взаимодействие через IS, открытие формы, интерактивный показ.",
  ["User_IS_interaction","Open_form"],["SHOW_operator","DIALOG_operator"])
b("B25","Взаимодействие","Печать и структурированный вид",
  "Печатное и структурированное представления формы.",
  ["Print_view","In_a_print_view_PRINT","Structured_view","In_a_structured_view_EXPORT_IMPORT"],
  ["PRINT_operator"])
b("B26","Взаимодействие","Фокус/дерево объектов/скриншот",
  "Операторы фокуса, видимость дерева объектов, операторы групп объектов, скриншот.",
  ["Focus_operators","Object_tree_visibility_EXPAND_COLLAPSE","Object_group_operators","Capture_SCREENSHOT"],
  ["SCREENSHOT_operator","EXPAND_operator","COLLAPSE_operator","Object_group_operator"])
b("B27","Формы","Формы — структура и операторы",
  "Корень форм, структура формы, операторы формы.",
  ["Forms","Form_structure","Form_operators"],
  ["FORM_statement","Object_blocks","Properties_and_actions_block","Filters_and_sortings_block"])
b("B28","Формы","Дизайн формы и представления",
  "Дизайн формы, представления, отчётный дизайн, pivot.",
  ["Form_design","Form_views","Report_design"],["DESIGN_statement","Pivot_block"])
b("B29","Формы","Интерактивный вид",
  "Интерактивное представление и SHOW/DIALOG-семантика.",
  ["Interactive_view","In_an_interactive_view_SHOW_DIALOG"],[])
b("B30","Формы","Статический вид (VIEW)",
  "Статическое представление и оператор VIEW.",
  ["Static_view","View_VIEW"],[])
b("B31","Формы","События формы",
  "События формы.",
  ["Form_events"],[])
b("B32","Формы","Навигатор и UI",
  "Навигатор, дизайн навигатора, пользовательский интерфейс, иконки.",
  ["Navigator","Navigator_design","User_interface","Icons"],["NAVIGATOR_statement","WINDOW_statement"])
b("B33","Формы","Расширение формы",
  "Расширение формы.",
  ["Form_extension"],["EXTEND_FORM_statement"])
b("B34","Физическая модель","Таблицы, индексы, материализации",
  "Таблицы, индексы, материализации.",
  ["Tables","Indexes","Materializations"],["TABLE_statement","INDEX_statement"])
b("B35","Развитие","Модульность и идентификация",
  "Модульность, модули, группы свойств/действий, именование, идентификация элементов.",
  ["Modularity","Modules","Groups_of_properties_and_actions","Naming","Element_identification"],["Module_header","GROUP_statement","IDs"])
b("B36","Развитие","Расширения",
  "Расширения класса/свойства/действия + ABSTRACT-полиморфизм.",
  ["Extensions","Class_extension","Property_extension","Action_extension"],
  ["EXTEND_CLASS_statement","ABSTRACT_operator","ABSTRACT_action_operator"])
b("B37","Развитие","Метапрограммирование",
  "Метапрограммирование и META/@.",
  ["Metaprogramming"],["META_statement","commat_statement"])
b("B38","Развитие","Миграция/локализация/поиск",
  "Миграция, интернационализация, поиск.",
  ["Migration","Internationalization","Search_"],[])
b("B39","Развитие","Основы языка",
  "Базовые языковые статьи: токены, литералы, комментарии, соглашения.",
  [],["Tokens","Literals","Comments","Coding_conventions"])
b("B40","Интеграция","Доступ к внешним/внутренним системам",
  "Интеграция (обзор), EXTERNAL/INTERNAL доступ в обе стороны, внутренний вызов.",
  ["Integration","Access_to_an_external_system_EXTERNAL","Access_from_an_external_system","Access_to_an_internal_system_INTERNAL_FORMULA","Access_from_an_internal_system","Internal_call_INTERNAL"],
  ["EXTERNAL_operator","INTERNAL_operator"])
b("B41","Интеграция","EVAL / Java / Spring",
  "EVAL, Java integration API, кастомный Spring-bean.",
  ["Eval_EVAL","Java_integration_API","Custom_Spring_bean_EventServer"],["EVAL_operator"])
b("B42","Интеграция","Файлы и обмен данными",
  "Файловые операторы, READ/WRITE, EXPORT/IMPORT данных, EMAIL.",
  ["File_operators","Read_file_READ","Write_file_WRITE","Data_export_EXPORT","Data_import_IMPORT","Send_mail_EMAIL"],
  ["READ_operator","WRITE_operator","EXPORT_operator","IMPORT_operator","EMAIL_operator"])
b("B43","Исполнение","Исполнение и планировщик",
  "Исполнение (обзор/auto/manual), планировщик, события и параметры запуска.",
  ["Execution","Execution_auto","Execution_manual","Scheduler","Launch_events","Launch_parameters"],[])
b("B44","Управление","Параметры и безопасность",
  "Управление (обзор), системные/рабочие параметры, политика безопасности, интерпретатор.",
  ["Management","System_parameters","Working_parameters","Security_policy","Interpreter"],[])
b("B45","Управление","Мониторинг и сервисы",
  "Монитор процессов, профайлер, журналы, MCP-сервер, чат.",
  ["Process_monitor","Profiler","Journals_and_logs","MCP_server","Chat"],[])
b("B46","Установка","Установка и среда разработки",
  "Установка (авто/ручная), Docker, разработка (auto/manual), IDE, проекты.",
  ["Install","Automatic_installation","Manual_installation","Docker","Development","Development_auto","Development_manual","IDE","Projects"],[])
b("B47","Обучение","Обучающие материалы",
  "Learn, обучающие материалы, примеры, онлайн-демо.",
  ["Learn","Learning_materials","Examples","Online_demo"],[])
b("B48","Обучение","Учебные приложения",
  "Учебные примеры приложений.",
  ["Materials_management","Score_table"],[])

# Слои (кураторские)
L3={"B00","B01"}; L2={"B03","B04","B10","B17","B27","B40","B43","B44","B35","B36","B47"}
def layer(bid): return 3 if bid in L3 else 2 if bid in L2 else 1
def tier(ps,ls): return "P+L" if ps and ls else ("P" if ps else "L")

# Треки (домены) — исполнитель ведёт один трек, не прыгая между темами
TRACKS=[
 ("T1","Логическая модель — данные и выражения", ["B01","B02","B03","B04","B05","B06","B07","B08","B09"]),
 ("T2","Логическая модель — множества, события, ограничения", ["B10","B11","B12","B13","B14","B15","B16"]),
 ("T3","Действия", ["B17","B18","B19","B20","B21","B22","B23"]),
 ("T4","Формы и взаимодействие", ["B24","B25","B26","B27","B28","B29","B30","B31","B32","B33"]),
 ("T5","Физическая модель, развитие, язык, корни", ["B00","B34","B35","B36","B37","B38","B39"]),
 ("T6","Интеграция, исполнение, управление, обучение", ["B40","B41","B42","B43","B44","B45","B46","B47","B48"]),
]
TRK={}; TRKNAME={}
for ti,(tid,tname,bids) in enumerate(TRACKS):
    TRKNAME[tid]=tname
    for bd in bids: TRK[bd]=tid
allb={bid for bid,*_ in B}
assert set(TRK)==allb, (set(TRK)^allb)

Bd={bid:(bid,zone,title,scope,ps,ls) for bid,zone,title,scope,ps,ls in B}
idx={bid:i for i,(bid,*_) in enumerate(B)}
LAY={bid:layer(bid) for bid,*_ in B}

# назначения на уровне трека: исполнитель владеет всем треком; стартует с нижнего листового блока
# Alex берёт базовый/ключевой трек T1 (фундамент: классы/свойства/выражения).
# @DAle — медленнее, но качественнее → неблокирующий трек T6 (терминальные темы, ни от кого не на критическом пути).
ASSIGN_TRACK={"T1":"Alex (оркестратор)","T2":"@Alexey Byimistrov","T3":"@Chavchavadze","T4":"@Pavel Miniutka","T6":"@DAle"}
START={"B02","B11","B18","B24","B41"}   # стартовые блоки (in-progress); остальные блоки трека — todo, но owned
def owner(bid): return ASSIGN_TRACK.get(TRK[bid],"")
def status(bid): return "in-progress" if bid in START else "todo"

# новые статьи (создаём): article -> (block, что это, где paradigm-описание)
CREATE={
 "B18":[("ASYNCUPDATE_operator","действие асинхронного обновления свойства","paradigm — в State_change")],
 "B34":[("RECALCULATE_operator","действие пересчёта хранимого/материализованного свойства (опция NOCLASSES)","paradigm — в Materializations")],
 "B37":[("REFLECTION_operator","свойство-рефлексия (метаданные, напр. CANONICALNAME)","paradigm — в Metaprogramming")],
 "B45":[("SHOWDEP_operator","диагностическое действие (зависимости/рекурсия свойств; покрывает SHOWREC)","paradigm — в обзоре управления")],
}

# порядок: по треку, внутри трека снизу вверх (layer, idx)
def track_blocks(tid):
    bids=[t[2] for t in TRACKS if t[0]==tid][0]
    return sorted(bids, key=lambda b:(LAY[b], idx[b]))

# ---------- BLOCKS.md ----------
o=[]
o.append("# DOCS-DESC-BLOCKS — реестр блоков\n")
o.append("**Блоки file-focused:** суть блока — какие файлы дописывать (каждое имя = пара `docs/<type>/en/<имя>.md` + `docs/<type>/ru/<имя>.md`, type ∈ paradigm/language). Если конструкции блока ещё нет статьи — исполнитель создаёт оба файла в нужной type-папке, а оркестратор подключает их в `docs/sidebars.js` (вне блочного коммита).")
o.append("Процесс — `DOCS-DESC-PLAN.md`; промпты — `DOCS-DESC-PROMPTS.md`; инструкция людям — `DOCS-DESC-EXECUTOR.md`. Граф — из дерева документации (`docs/sidebars.js` / структуры папок paradigm+language); how-to — позже. Каждый блок ведётся в своей ветке от `master`, оркестратор мерджит её в `master`. Отметки ведёт **только оркестратор**.\n")
o.append("## Треки (домены)\n")
o.append("Блоки сгруппированы в **6 тематических треков**. **Один исполнитель ведёт один трек целиком** (снизу вверх внутри трека) — чтобы не прыгать между темами. Несколько исполнителей = несколько треков параллельно.\n")
o.append("| Трек | Домен | Блоков | Исполнитель |")
o.append("|------|-------|:-----:|-------------|")
for tid,tname,bids in TRACKS:
    o.append(f"| {tid} | {tname} | {len(bids)} | {ASSIGN_TRACK.get(tid,'—')} |")
o.append("")
o.append("## Порядок внутри трека — снизу вверх (дедуктивно)\n")
o.append("**Слой 1** — листовые блоки; **слой 2** — обзорные (категория, чьи дочерние статьи в других блоках; пишутся поверх них); **слой 3** — корни. Внутри трека идти от слоя 1 к 3; обзорный/корневой блок не брать, пока его дочерние не описаны (дочерние могут быть в другом треке — тогда обзор ждёт их). Статусы: `todo`→`in-progress`→`done` (+`blocked`). Внутри блока сначала Paradigm, потом Language.\n")
o.append("## Блоки по трекам\n")
o.append("| Трек | Слой | Block | Зона | Заголовок | Tier | Файлы P/L | Status | Owner |")
o.append("|------|:----:|-------|------|-----------|:----:|:---------:|--------|-------|")
for tid,tname,_ in TRACKS:
    for bid in track_blocks(tid):
        _,zone,title,scope,ps,ls=Bd[bid]
        o.append(f"| {tid} | {LAY[bid]} | {bid} | {zone} | {title} | {tier(ps,ls)} | {len(ps)}/{len(ls)} | {status(bid)} | {owner(bid)} |")
o.append("")
o.append("## Файлы по блокам (по трекам)\n")
for tid,tname,_ in TRACKS:
    o.append(f"### Трек {tid} — {tname}\n")
    for bid in track_blocks(tid):
        _,zone,title,scope,ps,ls=Bd[bid]
        o.append(f"#### {bid} — {title}  · слой {LAY[bid]} · {tier(ps,ls)}")
        o.append(f"_{scope}_")
        o.append(f"- **Paradigm:** {', '.join('`'+a+'`' for a in ps) if ps else '—'}")
        o.append(f"- **Language:** {', '.join('`'+a+'`' for a in ls) if ls else '—'}")
        for art,what,phome in CREATE.get(bid,[]):
            o.append(f"- **Создать (новая статья):** `{art}` — {what} ({phome}).")
        o.append("")
o.append("## Первичные назначения\n")
o.append("Каждый владеет всем своим треком, идёт снизу вверх:")
o.append("- **Alex (оркестратор) → T1** (логика: данные/выражения) — **базовый/ключевой** фундамент (классы/свойства/выражения), нужен раньше всех; старт **B02** (Классы). _Это критический путь — не давать ему простаивать из-за оркестрации._")
o.append("- **@Alexey Byimistrov → T2** (множества/события/ограничения); старт **B11** (Группировка и распределение).")
o.append("- **@Chavchavadze → T3** (действия); старт **B18** (Изменение состояния).")
o.append("- **@Pavel Miniutka → T4** (формы и взаимодействие); старт **B24** (Взаимодействие/открытие форм).")
o.append("- **@DAle → T6** (интеграция/исполнение/управление/обучение) — **неблокирующий** трек (терминальные темы, ни от кого не на критическом пути): медленнее, но качественнее, и его темп никого не держит; старт **B41**.")
o.append("Закрыл блок — берёт следующий блок своего трека (снизу вверх). Обзорные/корневые блоки трека (слой 2-3) ждут, пока их дочерние описаны (в т.ч. в других треках). Трек **T5** (физмодель/развитие/язык/корни) подключается позже — корни и так идут последними.")
o.append("\n> Примечание: `MCP_server` (B45) — новая статья, ещё не подключённая в `docs/sidebars.js`; оркестратору определить её место в дереве.")
# ---- Полнота против грамматики (аудит LsfLogics.g против статей) ----
o.append("\n## Полнота против грамматики (сверено с codex)\n")
o.append("Аудит грамматики `LsfLogics.g` против статей: **на уровне статей покрытие полное** — все 159 paradigm + 121 language статей разложены по блокам. Сверка ключевых слов грамматики (мой греп + codex по коду) дала ниже.\n")
o.append("### Новые статьи — СОЗДАЁМ (конструкция нигде не описана)\n")
o.append("Создаёт исполнитель блока (language-статья в `docs/language/{en,ru}/`), paradigm-аспект — в указанной существующей статье; оркестратор подключает новый файл в `docs/sidebars.js`.\n")
o.append("| Создать (language) | Что это | Блок | Paradigm-описание |")
o.append("|---|---|---|---|")
o.append("| `ASYNCUPDATE_operator` | действие асинхронного обновления свойства (`addScriptedAsyncUpdateProp`) | **B18** (T3) | в `State_change` |")
o.append("| `RECALCULATE_operator` | действие пересчёта хранимого/материализованного свойства (`RECALCULATE [CLASSES] prop(...) [WHERE ...]`) | **B34** (T5) | в `Materializations` |")
o.append("| `REFLECTION_operator` | свойство-рефлексия (метаданные, напр. `CANONICALNAME`) | **B37** (T5) | в `Metaprogramming` |")
o.append("| `SHOWDEP_operator` | диагностическое действие (зависимости/рекурсия; покрывает `SHOWREC`, одно правило `addScriptedShowRecDepAction`) | **B45** (T6) | в обзоре управления |")
o.append("")
o.append("### Опции/под-конструкции — покрыть ВНУТРИ существующих статей (content-gap, не новые статьи)\n")
o.append("Цель блока — описать их внутри указанной статьи (требование «полнота по коду»); цели — по codex:\n")
o.append("- **B02 `Built-in_classes`:** `INTERVAL`, `ZDATETIME` (типы), `TEXTFILE`/`TEXTLINK` (файл/ссылка-классы, и семейство *FILE/*LINK).")
o.append("- **B03 `Property_options`:** `HINT`/`NOHINT`, `NOCOMPLEX`, `PREREAD`, `MOUSE` (`CHANGEMOUSE`).")
o.append("- **B14 события (`Event_block`/`WHEN`/`FOR`):** `REPLACE`/`NOREPLACE` (политика обработчиков), `INLINE`/`NOINLINE`.")
o.append("- **B21 `NEWTHREAD_operator`:** `CONNECTION` (`NEWTHREAD ... CONNECTION expr`).")
o.append("- **B22 `ACTIVATE_operator`:** `SEEK` (поиск/позиционирование объекта, FIRST/LAST/NULL).")
o.append("- **B23 `INPUT_operator`:** `ACTIONS` (context actions), `FOCUSED`/`HOVER`/`SELECTED` (quick access/toolbar).")
o.append("- **B24 `DIALOG_operator` / `Open_form`:** `CHECK`, `EMBEDDED` (windowType), `NOCHANGE`, `THISSESSION` (form session scope).")
o.append("- **B26 объекты:** `CONTAINER` (`COLLAPSE`/`EXPAND`), `VIEWTYPE` (`Object_group_operator`).")
o.append("- **B27 form-блоки (`Properties_and_actions_block`/`Object_blocks`):** `COLUMN`/`MEASURE` (pivot роли), `CONFIG` (`PIVOT ... CONFIG`), `DISABLEIF`, `HINTNOUPDATE`, `HINTTABLE`, `NOEXTID`, `NOSELECT`, `OPTIONS`, `POPUP`.")
o.append("- **B28 `Form_design`/`DESIGN_statement`:** `CLASSCHOOSER`, `FILTERBOX`, `FILTERCONTROLS`.")
o.append("- **B28 `Report_design` / B27 `FORM_statement`:** `REPORTS`, `REPORTFILES`.")
o.append("- **B40 `EXTERNAL_operator`:** `JAVA` (формат внешнего вызова).")
o.append("- **B42 `EXPORT_operator`:** `TAG` (XML-export опция).")
o.append("- Опции новых статей: `NOCLASSES` → внутри `RECALCULATE`; `CANONICALNAME` → внутри `REFLECTION`.")
o.append("")
o.append("> Системная библиотека (встроенные модули `.lsf` в `server/src/main/lsfusion`: `currentDate`, метапривязки и т.п.) — это **справочник библиотеки**, не язык/парадигма; вне текущего графа (кандидат на отдельную reference/how-to серию позже).")
open(os.path.join(OUT,"DOCS-DESC-BLOCKS.md"),"w").write("\n".join(o))

# ---------- PROMPTS.md ----------
def files_lines(label, arts):
    typ='paradigm' if label=='Paradigm' else 'language'
    lines=[f"**{label} (доописать обе версии, en+ru построчно синхронно; если статьи ещё нет — создать оба файла):**"]
    for a in arts: lines.append(f"- `docs/{typ}/en/{a}.md` + `docs/{typ}/ru/{a}.md`")
    return lines
p=[]
p.append("# DOCS-DESC-PROMPTS — промпты исполнителям по блокам\n")
p.append("Самодостаточный промпт на каждый блок, сгруппировано по трекам (внутри трека — снизу вверх). Оркестратор копирует нужный блок целиком и отдаёт исполнителю; один исполнитель ведёт один трек. Общий процесс и правила — `DOCS-DESC-PLAN.md` и `docs/AGENTS.md`.\n")
for tid,tname,_ in TRACKS:
    p.append(f"# Трек {tid} — {tname}\n")
    for bid in track_blocks(tid):
        _,zone,title,scope,ps,ls=Bd[bid]
        both=bool(ps) and bool(ls)
        p.append(f"## {bid} — {title}  ·  [{tier(ps,ls)}] · слой {LAY[bid]} · трек {tid} · зона: {zone}\n")
        p.append(f"_Scope:_ {scope}\n")
        p.append("```")
        bstart=len(p)
        p.append(f"Ты дорабатываешь документацию lsFusion по блоку {bid} «{title}» (трек {tid} — {tname}).")
        p.append("")
        p.append("Перед работой прочитай: docs/AGENTS.md (релевантные разделы целиком) и docs-process/DOCS-DESC-PLAN.md.")
        p.append("Источник истины — КОД платформы (LsfLogics.g -> ScriptingLogicsModule.java -> LogicsModule.java -> классы;")
        p.append("примеры в server/src/main/lsfusion; плагин — вторичен). Не описывай по памяти.")
        p.append(f"Работай в выданной под блок ветке (docs/{bid} от master) — создай её ПЕРВЫМ действием и в ОТДЕЛЬНОМ рабочем дереве (git worktree add), не в общем чекауте: иначе параллельные блоки/исполнители молча затрут несохранённое. Коммить в ветку рано (commit-first); в master сам не мерджи и master не пушь.")
        if LAY[bid]>=2:
            p.append("ВАЖНО: это обзорный/корневой блок. Его источник истины — уже описанные ДОЧЕРНИЕ статьи (их состав")
            p.append("и термины) и дерево docs/sidebars.js, в большей степени чем код. Обзорная статья вводит и связывает")
            p.append("дочерние, а не пересказывает реализацию. Не бери этот блок, пока его дочерние блоки не описаны.")
        p.append("EN и RU — построчно синхронные переводы одного текста (одна структура и состав информации).")
        p.append("Tier-чистота: Paradigm — абстракция (как будто синтаксиса/расширений нет); Language — синтаксис/механика")
        p.append("именно этой абстракции, ссылается на Paradigm и не переизлагает её. Не смешивать tier'ы.")
        p.append("")
        if both:
            p.append("ДВА ПОСЛЕДОВАТЕЛЬНЫХ ПОД-ЦИКЛА: сначала ЦЕЛИКОМ Paradigm, потом ЦЕЛИКОМ Language. Каждый под-цикл =")
            p.append("описание + СВОЙ коммит + ЗАВЕРШЁННОЕ ревью. Language не начинать, пока под-цикл Paradigm не закрыт.")
            p.append("")
            p.extend(files_lines("Paradigm", ps)); p.append("")
            p.extend(files_lines("Language", ls)); p.append("")
        else:
            only="Paradigm" if ps else "Language"
            p.append(f"Блок только tier'а {only} — один под-цикл."); p.append("")
            p.extend(files_lines(only, ps if ps else ls)); p.append("")
        if CREATE.get(bid):
            p.append("СОЗДАТЬ новые статьи (нет в доке вообще; создать файлы en+ru, сообщить оркестратору для docs/sidebars.js):")
            for art,what,phome in CREATE[bid]:
                p.append(f"- `docs/language/en/{art}.md` + `docs/language/ru/{art}.md` — {what}; {phome}.")
            p.append("")
        p.append("ЦИКЛ СДАЧИ на КАЖДЫЙ под-цикл (commit-first внутри ветки блока):")
        if both:
            p.append(f"  Под-цикл 1 — PARADIGM: коммит 'docs: {bid} paradigm — {title}'.")
            p.append(f"  Под-цикл 2 — LANGUAGE (только после ПОЛНОГО закрытия Paradigm): коммит 'docs: {bid} language — {title}'.")
        else:
            onlyc="paradigm" if ps else "language"
            p.append(f"  Один под-цикл — {onlyc.upper()}: коммит 'docs: {bid} {onlyc} — {title}'.")
        p.append("  Шаги под-цикла:")
        p.append("  1. Написать/создать файлы под-цикла -> коммит (subject выше).")
        p.append("  2. self-review (явная сверка с каждым релевантным правилом AGENTS.md; полнота по коду/дочерним;")
        p.append("     EN/RU parity; направление cross-link'ов; tier-чистота) + codex review с master:docs/AGENTS.md;")
        p.append("     правки отдельными коммитами до чистого прохода. Это твой контроль ПЕРЕД показом человеку.")
        p.append("  3. РЕВЬЮ ЧЕЛОВЕКОМ: отдаёшь результат человеку (исполнитель/оркестратор) — он даёт замечания.")
        p.append("  4. Исправляешь замечания ОТДЕЛЬНЫМИ коммитами (при необходимости снова self/codex) и возвращаешь на ревью.")
        p.append("  5. Повторяешь шаги 3-4, пока у человека НЕ ОСТАНЕТСЯ претензий — только тогда под-цикл закрыт.")
        p.append("")
        p.append("Если конструкции блока ещё нет статьи — создать файлы en+ru в нужной type-папке (docs/paradigm/ или")
        p.append("docs/language/) и сообщить оркестратору (он подключит их в docs/sidebars.js). Нашёл обобщаемое правило/проблему —")
        p.append("допиши его в AGENTS.md ОТДЕЛЬНЫМ коммитом (AGENTS.md дописывать может любой; это файл для ИИ).")
        p.append("docs/sidebars.js и CLAUDE.md в коммиты блока НЕ включать. Ветку блока пушишь сам (для ревью); merge ветки")
        p.append("в master (squash по необходимости) делает оркестратор после закрытия обоих под-циклов.")
        p.append("```")
        p.append("")
open(os.path.join(OUT,"DOCS-DESC-PROMPTS.md"),"w").write("\n".join(p))
print("tracks:", {t[0]:len(t[2]) for t in TRACKS}, "total", sum(len(t[2]) for t in TRACKS))
