## Brief on lsFusion elements for AI agents

Format: **very concise**, for understanding and code generation. Detailed syntax is retrieved via RAG in docs.

---

## Core elements (from simpler to more complex)

### Classes
- **Analogy**: OOP classes, with multiple dispatch by parameter classes. Define object types; used in signatures of properties/actions/forms.
- **Description**: base element — set of objects. Inheritance. Built-in vs user classes. Polymorphism via inheritance, `ABSTRACT` + `+=` / `ACTION+`, `MULTI`.
- **Syntax (search)**: `CLASS ClassName ['Caption'] [: ParentClass];`

### Properties
- **Analogy**: math / pure functions. Compute facts, no state change. Declarative, close to SQL.
- **Description**: DATA (stored) vs calculated (formulas, aggregates, compositions).
- **Syntax (search)**: `name 'Caption' = DATA Type (Class1, ...);` or `name (Params...) = Expression;`

### Actions
- **Analogy**: procedures / methods. Change state (DB or external). Imperative, close to Java.
- **Description**: dual to properties: properties = what; actions = how it evolves.
- **Syntax (search)**: `actionName 'Caption' (Params...) { ActionOperators }`

### Forms
- **Analogy**: SQL query but broader (many tables at once). Universal data/UI element.
- **Description**: `OBJECTS` (groups), `PROPERTIES` (what to show / actions), `FILTERS` (row filter). Views: interactive (`SHOW`), print (`PRINT`), structured (`EXPORT`/`IMPORT`). Extensible via `EXTEND FORM`.
- **Syntax (search)**: `FORM FormName 'Caption' OBJECTS ... PROPERTIES ... FILTERS ... ;`

### Events
- **Analogy**: DB triggers but broader. Time-dependent reactions to data changes.
- **Description**: main operator `WHEN`; session-change analyzers `CHANGED`, `SET`, `DROPPED`, `PREV`, etc. Form events: `ON CHANGE`/`ON EDIT`/`ON CONTEXTMENU`/`ON GROUPCHANGE`/`ON CHANGEWYS`. Block events: `BEFORE` / `AFTER`.
- **Syntax (search)**: `prop(...) <- Expr WHEN CHANGED(...);` or `PROPERTIES(...) prop ON CHANGE action(...);`

### Constraints
- **Analogy**: integrity constraints, time-independent invariants.
- **Description**: checked on `APPLY`; types: general (`CONSTRAINT`), simple (`=>`, `NONULL`, uniqueness via `GROUP AGGR`).
- **Syntax (search)**: `CONSTRAINT name 'Caption' CHECK condition(...);` or `premise(...) => consequence(...) [RESOLVE LEFT|RIGHT];`

### Aggregations
- **Analogy**: aggregate objects maintained declaratively.
- **Description**: created via `AGGR` (see property operators). Platform maintains aggregate objects by rules.
- **Syntax (search)**: see `AGGR` operator below.

### Additional elements
- **Navigator (`NAVIGATOR`)**: menu / routing binding forms and actions.
- **Windows (`WINDOW`)**: layout of multiple forms/areas.
- **Tables (`TABLE`)**: explicit DB tables (rare; low-level IN/integration).
- **Indexes (`INDEX`)**: explicit DB indexes (mostly auto-generated).

### Modules
- **Analogy**: package / assembly (`.lsf` file).
- **Description**: unit of reuse; contains classes/properties/actions/forms/events/constraints; dependencies via `REQUIRE`.
- **Syntax (search)**: `MODULE ModuleName;` `REQUIRE ModuleA, ModuleB;`

---

## Property operators

### Basic expressions
- **Description**: arithmetic, logic, strings, comparisons, type tests (`IS`/`AS`), conditional (`IF ... ELSE`, postfix `f(a) IF g(a)`).
- **Syntax (search)**: standard expression operators.

### Composition
- **Analogy**: function call inside function / function composition in math. Using one property in another's expression.
- **Description**: property-from-property. Usually implicit (simple property substitution in expression).
- **Syntax (search)**: using properties in other properties' expressions.

### Grouping (`GROUP`)
- **Description**: value aggregates (sum/count/max/min/last/concat). Not object aggregates.
- **Syntax (search)**: `GROUP SUM source(...) IF Condition;` or `GROUP SUM source(...) BY groupExprs...;`

### Partition / window (`PARTITION`)
- **Description**: window logic: ranks, previous value, cumulative.
- **Syntax (search)**: `PARTITION AggOp expression IF Condition ORDER orderExprs...;`

### Aggregating objects (`GROUP AGGR`)
- **Description**: maintains aggregate objects via `BY`.
- **Syntax (search)**: `GROUP AGGR Class x WHERE BaseCondition BY groupExprs...;`

### Operator `AGGR`
- **Description**: creates/maintains aggregate objects automatically.
- **BNF**: `aggrPropertyDefinition ::= AGGR baseEventPE customClassUsage WHERE propertyExpression (NEW baseEventNotPE)? (DELETE baseEventNotPE)?`
- **Syntax (search)**: `AGGR Class x WHERE condition(...) [NEW newEvent] [DELETE deleteEvent] [MATERIALIZED] [INDEXED];`

### OVERRIDE
- **Description**: merge multiple sources with priority.
- **Syntax (search)**: `OVERRIDE expr1, expr2, expr3;`

### RECURSION
- **Description**: recursive properties (trees/graphs/closures).
- **Syntax (search)**: `RECURSION baseExpr THEN stepExpr;`

### ABSTRACT
- **Description**: declare abstract property; implementations via `+=` in other modules.
- **Syntax (search)**: `ABSTRACT [MODIFIERS] Type (ParamClasses...);` and `prop[Context](Params...) += implExpr;`

---

## Action operators

### Basic operators
- **Mutations**: assignment (`<-`), NEW, DELETE.
- **Action call**: **Analogy** — function / procedure / method call. Simply action name with parameters. Not a mutation, executes another action.
- **Syntax (search)**: `property(...) <- Expression [WHERE ...];`, `NEW alias = Class { ... }`, `DELETE ... WHERE ...;`, `actionName(params...);`

### Sequence
- **Description**: ordered block of actions.
- **Syntax (search)**: `actionName(Params...) { op1; op2; }`

### FOR loop
- **Description**: iterate all tuples where condition property is not NULL; can create objects.
- **Syntax (search)**: `FOR Expression [ORDER ...] [TOP n] [OFFSET m] [NEW alias = Class] DO { ... } [ELSE { ... }]`

### WHILE loop
- **Description**: repeat while condition property somewhere is true (not NULL).
- **Syntax (search)**: `WHILE condition DO { ... }`

### Branching
- **Description**: `IF`, `CASE`, `MULTI` (polymorphic branching).
- **Syntax (search)**: `IF ... THEN { ... } ELSE { ... }`, `CASE WHEN ...`, `MULTI impl1[p](p), impl2[p](p);`

### Flow control
- **Description**: `BREAK`, `CONTINUE`, `RETURN`, `TRY...CATCH`, `NEWTHREAD`, `NEWEXECUTOR`.
- **Syntax (search)**: standard flow keywords.

### Form actions
- **Description**: `SHOW`, `DIALOG`, `SEEK`, `EXPAND`, `COLLAPSE`.
- **Syntax (search)**: `SHOW FormName [OBJECTS ...];`, `DIALOG FormName OBJECTS ... INPUT [FILTERS ...] DO { ... }`, `SEEK [FIRST|LAST] group = obj;`

---

## Change sessions
- **Description**: isolated change set. Before `APPLY`, data is session-local. On `APPLY`: success → commit; failure → `canceled() = TRUE`, `applyMessage()` has error, data remains in session.
- **Session ops**: `NEWSESSION action` (independent), `NESTEDSESSION` (dependent nested). Use for work units (dialog, import, file processing).

---

## Form representations

### Interactive view
- **Analogy**: usual app pages but single execution flow (no split server/client language).
- **Description**: `SHOW` opens interactive form; `DIALOG` modal with `INPUT` / `FILTERS`; `DESIGN` sets UI layout. `INPUT` — prompt user for value. Reactive updates; WYSIWYG.
- **Syntax (search)**: `SHOW formName [OBJECTS ...] [DOCKED];`, `DIALOG formName OBJECTS ... INPUT [FILTERS ...] DO { ... }`, `DESIGN formName { ... }`, `INPUT var = Type DO { ... }`

### Print view
- **Analogy**: reports (jrxml + JasperReports).
- **Description**: jrxml defines print layout; report initiated from lsFusion.
- **Syntax (search)**: `PRINT formName [OBJECTS ...] TO FILE fileVar;`

### Structured view
- **Analogy**: structured data formats (JSON, XML, XLSX, DBF).
- **Description**: `EXPORT` action outputs to file/stream; `JSON` property builds JSON; `IMPORT` loads back.
- **Syntax (search)**: `EXPORT FROM expr1, expr2 TO fileVar;`, `prop(...) = JSON expr;`, `IMPORT JSON|CSV|XLS|XML FROM fileVar TO props...;`

---

## Integration

- **Idea**: same property/action paradigm for external and internal systems.

### From current system to external (imperative, actions)
- **Description**: `EXTERNAL` — HTTP/external code/SQL as actions.
- **Syntax (search)**: `EXTERNAL Name 'id' [OPTIONS] (Params...);`

### From current system to external (declarative, properties)
- **Description**: `CUSTOM` UI components with JSON interface; `JSON` build structures for UI/API.
- **Syntax (search)**: `OBJECTS alias = Class CUSTOM 'componentName' ...`, `prop(...) = JSON expr;`

### From external systems to current
- **Description**: Action API — call lsFusion actions via HTTP. Protocol HTTP (ports `7651`). Action modes: `EXEC` (by name), `EVAL` (code with action `run`), `EVAL ACTION` (action code). Form API — work with forms via HTTP for frontends. Actions marked `@@api` for API access.
- **Syntax (search)**: action option `@@api`, URLs `/exec?action=...`, `/eval?script=...`, `/eval/action?script=...`

### From current system to internal (imperative)
- **Description**: `INTERNAL` — call Java code; on Java side use `findProperty`, `read`, `change`.
- **Syntax (search)**: `INTERNAL Name 'id' [OPTIONS] (Params...);`

### From current system to internal (declarative)
- **Description**: `FORMULA` — properties computed by SQL.
- **Syntax (search)**: `prop(...) = FORMULA 'sql expression with $1, $2, ...';`

---

## Physical model (DB structure)
- **Analogy**: manual/dynamic DB schema control (tables, indexes, materialization).
- **Description**: performance/open schema focus.
- **Syntax (search)**: `TABLE tableName ['dbName'] (ClassOrParams...);`, `prop = DATA Type (Class) MATERIALIZED ['dbFieldName'] INDEXED ['indexName'];`

---

## Extensions
- **Description**: modularity/polymorphism at module level: `EXTEND CLASS`, `EXTEND FORM`, `ABSTRACT` + `+=`, `ACTION+`.
- **Syntax (search)**: `EXTEND CLASS ClassName : ParentClass;`, `EXTEND FORM FormName ... ;`, `prop(...) += implExpr;`, `ACTION actionName(...) + { ... }`

---

## Metaprogramming
- **Analogy**: code generators/templates in language.
- **Description**: `@` operator generates code from descriptions; IDE can auto-generate.
- **Syntax (search)**: `@metaName { ... }`

---

## Identification & ergonomics
- **Namespaces**: control visible names. **Syntax (search)**: `NAMESPACE MyNamespace;`
- **Explicit typing**: params/local props are typed. **Syntax (search)**: `LOCAL var = Type ();`
- **String interpolation**: build strings from identifiers/values (also for i18n). **Syntax (search)**: `'{namespace.element}'`

---

## Internationalization
- **Description**: captions via resource bundles (`*ResourceBundle.properties`). Use IDs like `{use.case.i18n.book}`; UI switches language automatically.
- **Syntax (search)**: `CLASS Book '{use.case.i18n.book}';`

---

## Migration
- **Description**: migration files describe schema/data evolution; IDE auto-generates lines on renames; safe DB evolution.

---

## Mini map for AI
1. `CLASS` → object type (table).
2. `= DATA` or `=` expression → property.
3. `{ ... }` without `=` → action (imperative).
4. `FORM` → UI/query/report definition.
5. `SHOW / DIALOG / PRINT` → open/print form.
6. `EXPORT / IMPORT` → external formats for properties/forms.
7. `GROUP / PARTITION` → value aggregates.
8. `GROUP AGGR / AGGR` → aggregate objects.
9. `NEWSESSION / APPLY / canceled()` → transaction/session control.
10. `EXTEND / ABSTRACT / += / +{` → extension & polymorphism points.
11. `WHEN` → data-change event; `ON` → form event.

