package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.JavaScriptObject;

// WebMCP: the page hands the browser's AI agent its tools, through document.modelContext where the browser has one.
// The tools of the application's MCP server (/mcp, at url): a tool is the server's - its description, what it is called
// with and what it answers -, and so is its run. The page substitutes one thing, the run of lsfusion_eval and of
// lsfusion_exec: the script, or the action, runs where the user works, who sees what it shows - in the navigator, or
// in the form the call's `form` says. And a tool of the page's own, lsfusion_client: a method of the client's, called by
// its name - of a form's controller, or of the navigator's
public class WebMCP {

    public static native void register(String url, FormsController formsController, JavaScriptObject navigatorController)/*-{
        // navigator.modelContext is where the API was first, and where a polyfill may put it
        var modelContext = $doc.modelContext || $wnd.navigator.modelContext;
        if (!modelContext)
            return;

        // a tool handed to the agent, with its run. A tool's failure is its answer, an MCP tool result as the server's
        // failures are: what a tool throws does not reach the agent, which is told only that the call failed. A tool
        // the browser refuses - a name another script of the page has taken - does not keep the others out; the refusal
        // is thrown where registerTool returns no promise yet
        function registerTool(tool, run) {
            var name = tool.name;
            tool.execute = function (args) {
                return new $wnd.Promise(function (resolve) {
                    resolve(run(args));
                }).then(null, function (e) {
                    return { content: [{ type: 'text', text: name + ' error: ' + e.message }], isError: true };
                });
            };
            new $wnd.Promise(function (resolve) {
                resolve(modelContext.registerTool(tool));
            }).then(null, function (e) {
                $wnd.console.warn('WebMCP: ' + name + ' is not registered: ' + e.message);
            });
        }

        function request(method, params) {
            return $wnd.fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ jsonrpc: '2.0', id: 1, method: method, params: params })
            }).then(function (response) {
                if (!response.ok)
                    throw new Error('HTTP ' + response.status);
                return response.json();
            }).then(function (rpc) {
                if (rpc.error)
                    throw new Error(rpc.error.message);
                return rpc.result;
            });
        }

        // the tool's run on the server; the agent gets the data of the tool's result when that is all of it - the text
        // beside the data is then its copy, for the clients that read no data
        function onServer(name, args) {
            return request('tools/call', { name: name, arguments: args }).then(function (result) {
                return !result.isError && result.structuredContent !== undefined && result.content.length === 1 ? result.structuredContent : result;
            });
        }

        // the tools whose run the page substitutes where the user works: the controller's call that runs each, and the
        // argument that names what is run
        var inContext = {
            lsfusion_eval: { call: 'eval', run: 'script' },
            lsfusion_exec: { call: 'exec', run: 'action' }
        };

        // what a call is checked by where the page checks it, the browser checking no call against a tool's schema: a
        // field it does not take is said with those it does
        function checkFields(given, taken, what) {
            var fields = taken.length > 0 ? taken.join(', ') : "no fields";
            if (given == null || typeof given !== 'object' || Array.isArray(given))
                throw new Error(what + " must be an object of " + fields);
            Object.keys(given).forEach(function (field) {
                if (taken.indexOf(field) < 0)
                    throw new Error("unknown field `" + field + "`: " + what + " has " + fields);
            });
        }

        // THE FORM A CALL IS FOR, as its `form` says - one rule for every tool that is run where the user works. None
        // given: no form, the call is the navigator's. Given, it addresses an open form as ACTIVATE FORM does - by its
        // name, the label it was opened with, the window it was opened into -, which is to be one; with none of the
        // parts, `{}`, it is the current form, which is the platform's - none under a modal popup, and a form embedded
        // in a cell is the editor of the form the cell is in, which stays current
        function readForm(address) {
            if (address == null)
                return null;
            if (typeof address !== 'object' || Array.isArray(address))
                throw new Error("`form` must be an object of name, label, window, each a string - `{}` for the form the user is in");
            Object.keys(address).forEach(function (part) {
                if (part !== 'name' && part !== 'label' && part !== 'window')
                    throw new Error("`form`: unknown field `" + part + "`; a form is addressed by `name`, `label`, `window`");
                if (address[part] != null && typeof address[part] !== 'string')
                    throw new Error("`form." + part + "` must be a string");
            });
            var form;
            if (address.name == null && address.label == null && address.window == null) {
                var container = @lsfusion.gwt.client.view.MainFrame::getCurrentForm()();
                form = container != null ? container.@lsfusion.gwt.client.form.view.FormContainer::getForm()() : null;
            } else {
                try { // a part that is not given is null for Java, not undefined
                    form = formsController.@FormsController::findForm(*)(address.label == null ? null : address.label,
                            address.name == null ? null : address.name, address.window == null ? null : address.window);
                } catch (e) {
                    throw new Error(e.message + ' at the address ' + JSON.stringify(address));
                }
            }
            if (form == null)
                throw new Error("the user is in no form");
            return form;
        }

        // ... and such a tool's run where the user works, which every run of it by the page is: in the navigator, or
        // with `form` in a form. What is no call of the tool - nothing to run, params that are no list - the server
        // says, which runs no such call; a `form` that is none, and a field the tool does not take, the page says
        // itself: the server takes no `form`, and a call it can run it would run
        function runInContext(name, args) {
            var tool = inContext[name];
            if (args == null || typeof args[tool.run] !== 'string' || !args[tool.run] || (args.params != null && !Array.isArray(args.params)))
                return onServer(name, args);
            checkFields(args, [tool.run, 'params', 'form'], "the call");
            var form = readForm(args.form);

            // the controller's call made as the tool's: the arguments are the tool's params and the result is its answer
            var calls = (form != null ? form.@GFormController::controller : navigatorController).mcp;
            return calls[tool.call].apply(calls, [args[tool.run]].concat(args.params || []));
        }

        // THE PAGE'S OWN TOOL, lsfusion_client: a method of the client's, where the user works, called by its name, the
        // method's arguments given by theirs - with `form`, a method of that form's controller
        // (GFormController.controller); with none, of the navigator's (GNavigatorActionDispatcher.controller), as the
        // tools that run an action read it (readForm). So a name may be both a form's method and the navigator's.
        // The call is the page's to check, there being no server to say what is wrong with it (checkFields)
        var formMethods = @WebMCP::getFormMethods()(), navigatorMethods = @WebMCP::getNavigatorMethods()();
        // a ROW is given by its key, a string or a number, as the page shows it on the row's element; the method takes
        // the row, a handle JSON has none of: the row of that key among the rows the form has of the `group` given
        // beside it (GFormController.readRow), or of the `property`'s group (readPropertyRow). Beside a group what is no
        // key is the method's to read - an edge of the rows -, and so is a key beside what is no group of the form's,
        // for the method to say; beside a property it is refused, the classic changeProperty taking it for the value
        function readRow(form, given) {
            var key = given.row;
            if (typeof key !== 'string' && typeof key !== 'number') {
                if (key != null && typeof given.property === 'string')
                    throw new Error("`row` must be the key of a row, a string or a number");
                return key;
            }
            var row = typeof given.group === 'string' ? form.@GFormController::readRow(Ljava/lang/String;Ljava/lang/String;)(given.group, '' + key)
                    : typeof given.property === 'string' ? form.@GFormController::readPropertyRow(Ljava/lang/String;Ljava/lang/String;)(given.property, '' + key) : null;
            return row != null ? row : key;
        }
        function callClientMethod(args) {
            checkFields(args, ['method', 'args', 'form'], "the call");
            var form = readForm(args.form);
            // the methods of what the call is for. A name that is one of the other's is said so, with what to do - and
            // so is a name both have, called with an argument that is the other's
            var methods = form != null ? formMethods : navigatorMethods, others = form != null ? navigatorMethods : formMethods;
            var other = typeof args.method === 'string' && others.hasOwnProperty(args.method) ? others[args.method] : null;
            var whose = " is a method of " + (form != null ? "the navigator: it is called with no `form`" : "a form: it is called with `form` - `{}` for the form the user is in");
            if (typeof args.method !== 'string' || !methods.hasOwnProperty(args.method))
                throw new Error(other != null ? "`" + args.method + "`" + whose
                        : "`method` must be one of " + Object.keys(methods).join(', ') + (form != null ? "" : " - or, with `form`, a method of a form"));
            var method = methods[args.method];
            var given = args.args != null ? args.args : {};
            if (other != null && given != null && typeof given === 'object')
                Object.keys(given).forEach(function (field) {
                    if (method.takes.indexOf(field) < 0 && other.takes.indexOf(field) >= 0)
                        throw new Error("`" + args.method + "` with `" + field + "`" + whose);
                });
            checkFields(given, method.takes, "`args` of " + args.method);

            // the method takes its arguments in their order; what it refuses - a name that names nothing, a value that
            // is none of the property's - it throws, or rejects its Promise with, and that is the tool's failure. A
            // method of the controller's, but where the catalog says how it is called (`call`)
            var controller = form != null ? form.@GFormController::controller : navigatorController, row = readRow(form, given);
            return $wnd.Promise.resolve(method.call != null ? method.call(form, controller, given, row) : controller[args.method].apply(controller, method.takes.map(function (name) {
                return name === 'row' ? row : given[name];
            }))).then(method.answer).then(function (answer) {
                return { result: answer };
            });
        }
        function describeMethods(methods) {
            return Object.keys(methods).map(function (name) {
                return "- `" + name + "(" + methods[name].takes.join(', ') + ")` — " + methods[name].does;
            }).join('\n');
        }
        registerTool({
            name: 'lsfusion_client',
            description: "Call a method of the application's client, where the user works: of an open form — what the user does with the form's tables, sortings, filters and values —, or of the navigator; done in what the user sees.\n\n" +
                "`method` names the method and `args` gives its arguments by their names, as listed below. `form` says whose method it is: with `form`, a form's — of the form it addresses, `{}` being the form the user is in; with no `form`, the navigator's. The answer is `{result}`: what the method answers with, nothing for a method that only changes what the user sees. A call that is refused is an error, with the reason.\n\n" +
                "**Names in a form.** `group` is an object group of the form, by its name; `tree`, a tree by its name; `table`, the table of an object group's rows, by the group's name, or a tree, by its name; `filterGroup`, a filter group by its name. The page shows them in `lsfusion-container`: `GRID(group)` on a group's table, `GRID(TREE tree)` on a tree, `FILTERGROUP(filterGroup)` on a filter group. A `property` is named as the page shows it in `lsfusion-property`, on the property's cells, its column's header and its field: `group.property`, and `property` alone for a property of no group. Beside its `group`, and in the sortings and conditions of a group's `table`, it is the part after the group, `property`; in a tree's sortings and conditions it is whole.\n\n" +
                "**Rows.** `row` is a row of `group` that the form shows: the `lsfusion-row` of a table's or a tree's row element, a string or a number, whose `lsfusion-group` is the group. Beside a `property` it is a row of the property's group.\n\n" +
                "**Values.** A value is given as JSON: a number, a string, `true` or `false`, an object or an array for a JSON property, `null` for no value; a date or a time as its ISO string with no time zone, as the form shows it — `2026-11-15`, `10:30`, `2026-11-15T10:30`.\n\n" +
                "**Methods of a form.**\n" + describeMethods(formMethods) + "\n\n" +
                "**Methods of the navigator.**\n" + describeMethods(navigatorMethods),
            inputSchema: {
                type: 'object',
                properties: {
                    method: { type: 'string', 'enum': Object.keys(formMethods).concat(Object.keys(navigatorMethods).filter(function (name) {
                        return !formMethods.hasOwnProperty(name);
                    })), description: "The method to call." },
                    args: { type: 'object', description: "The method's arguments, by their names; one the method can do without may be left out." },
                    form: {
                        type: 'object',
                        properties: {
                            name: { type: 'string', description: "The form's name, `Namespace.form`." },
                            label: { type: 'string', description: "The label the form was opened with." },
                            window: { type: 'string', description: "The name of the `FORMS` window the form is docked into; left out for a form that has no `lsfusion-form-window`." }
                        },
                        additionalProperties: false,
                        description: "The form whose method is called: the address of ONE open form, by any of its `name`, `label` and `window` — a failure when it names no form, or several —, or `{}`, the form the user is in — a failure when the user is in none. Omitted: the method is the navigator's. The page shows the address on the form's element, in the attributes `lsfusion-form` (the name), `lsfusion-form-label` and `lsfusion-form-window`, each that the form has."
                    }
                },
                required: ['method'],
                additionalProperties: false
            },
            // a property's change runs what the form's author wrote for it, which can save: the agent is to ask the
            // user before a call that it cannot take back
            annotations: { consequentialHint: true }
        }, callClientMethod);

        request('tools/list').then(function (list) {
            list.tools.forEach(function (tool) {
                var name = tool.name;
                var own = inContext.hasOwnProperty(name);
                if (own) { // what is run can save what it changes: the agent is to ask the user before a call that it cannot take back.
                    // The one thing added to a descriptor: the hint is WebMCP's own, an MCP server has no name for it
                    tool.annotations = tool.annotations || {};
                    tool.annotations.consequentialHint = true;
                }
                registerTool(tool, function (args) {
                    return own ? runInContext(name, args) : onServer(name, args);
                });
            });
        }).then(null, function (e) {
            $wnd.console.warn('WebMCP: the tools are not registered: ' + e.message);
        });
    }-*/;

    // THE FORM'S METHODS lsfusion_client calls - the form's controller's (GReactFormData.addFormMethods,
    // GFormController.addFormVerbs) that JSON can call: each with the names of its arguments, in the order the method
    // takes them, and what it does, as the tool's description says it of each; `answer`, what the tool makes of what
    // the method answers with, where that is no JSON. A row is a handle a view has, and JSON has none: a `row` is
    // given by its key, which the page shows, and read here (readRow) - beside a `group` a row of the group, beside a
    // `property` a row of the property's group -, and where the controller's method takes the row in no place of its
    // own - with the value's, among its options -, `call` says how it is called. What takes an element
    // or a function is not here (startEditing, stopEditing, isEditing, batch), nor are the controller's calls of the
    // logic (exec, eval, evalAction, change, mcp), which lsfusion_eval and lsfusion_exec are, nor a view's own filters
    // of the rows it draws (changeViewFilters), which the agent is no view to state: a table's user filters are its
    // changeFilters. And two that are no method of the controller - `call`, what is called in its place -: the form
    // made the current one, and closed, which the client does with the form (GFormController.activate, close), where
    // code of the application runs an operator
    private static native JavaScriptObject getFormMethods()/*-{
        // a lookup's answer: its last one - the server answers before it is sure of the form's unsaved changes, and
        // then again, in `more` -, each value as its strings, its `objects` being the handle of a row
        function lookupAnswer(answer) {
            return answer.more ? answer.more.then(lookupAnswer) : answer.data.map(function (value) {
                return { displayString: value.displayString, rawString: value.rawString };
            });
        }
        return {
            changeObject: { takes: ['group', 'row', 'options'],
                does: "Make `row` the current row of the group: the row `changeProperty` and `getValues` are of when they are given none. Where the group's rows are shown as a grid - not a tree's, nor as a pivot or another view -, `row` is also `{to: 'home'}` or `{to: 'end'}`, the first or the last row, and `options.selection` says what becomes of the rows selected: `start` (the default) selects none, `extend` selects the rows up to this one, `keep` leaves them." },
            changeProperty: { takes: ['property', 'row', 'value'],
                does: "Set the property's value in `row` - with no `row`, in the current row: `value`, `null` for none. With no `value`, start the property's change there as a click on it does: an action is run, a value may be asked of the user — the call then waits for the user's answer.",
                // the controller's method takes (property, value) and (property, row, value): no row is no argument
                call: function (form, controller, given, row) {
                    return row != null ? controller.changeProperty(given.property, row, given.value) : controller.changeProperty(given.property, given.value);
                } },
            getValues: { takes: ['property', 'row', 'value', 'options'], answer: lookupAnswer,
                // ... and this one the row among its options - options of another shape the controller refuses
                call: function (form, controller, given, row) {
                    var options = given.options, isOptions = options == null || @lsfusion.gwt.client.base.GwtClientUtils::isJSObject(*)(options);
                    return controller.getValues(given.property, given.value, row != null && isOptions ? Object.assign({}, options, { row: row }) : options);
                },
                does: "The values the form suggests for the property in `row` - with no `row`, in the current row -, by the text `value`, `''` for any. `options`, any of `{mode, count}`: `mode` is `objects`, the objects the property's value is picked among (the default), `values`, the values the property has, or `change`, what its change suggests; `count`, how many more than the server suggests at once. Answers `[{displayString, rawString}]`, each value as it is shown and as it is typed." },
            changeOrders: { takes: ['table', 'orders'],
                does: "The table's sortings. An array `[{property, desc}]` states them all, in the order of their priority, `[]` none. One `{property, desc}`, not in an array, changes the sorting by that property, and with no `desc` removes it." },
            changeFilters: { takes: ['table', 'conditions'],
                does: "The table's user filters. An array of conditions `{property, compare, value, negation, or}` states them all, `[]` none: `compare` is `=`, `>`, `<`, `>=`, `<=`, `!=`, `_` (contains) or `@` (search), the property's own when left out; a condition with no `value` is \"is empty\"; `negation: true` negates it; `or: true` joins it to the next one by OR. One condition, not in an array, states the first condition on its property, what it leaves out as that one has it, or adds one; with no `value` it takes that one off." },
            changeFilter: { takes: ['property', 'value', 'options'],
                does: "The value of the filter the form has for the property, where it has one: `value`, `null` to take its condition off. `options`, any of `{compare, negation, or}`, as in `changeFilters`." },
            addFilter: { takes: ['table'],
                does: "Add a condition to the table's filter controls, for the user to fill in." },
            applyFilters: { takes: ['table'],
                does: "Apply the table's conditions that wait for it." },
            resetFilters: { takes: ['table'],
                does: "Reset the table's user filters." },
            changeFilterControlsVisible: { takes: ['table', 'shown'],
                does: "Show (`true`) or hide (`false`) the table's filter controls." },
            changeFilterGroup: { takes: ['filterGroup', 'index'],
                does: "Select a filter of the filter group by its index, from 0; `null` for none, where the group may have none selected." },
            getCount: { takes: ['group'],
                does: "The number of rows in the group's table, as its filters leave them — of the rows the user has marked, where any are. Answers a number." },
            getSum: { takes: ['group', 'property'],
                does: "The sum of a number property over the same rows. Answers a number." },
            changeManual: { takes: ['group', 'manual'],
                does: "Turn the manual update of the group's table on (`true`) or off (`false`): on, its rows are read again only by `update`." },
            update: { takes: ['group'],
                does: "Read the rows of the group's table again now." },
            changeListView: { takes: ['group', 'view'],
                does: "Show the group's table as another view of those it has: `grid`, `pivot`, `map`, `calendar`, `custom`." },
            'export': { takes: ['table'],
                does: "Export the table's rows to a file, which the user gets." },
            expand: { takes: ['group', 'row'],
                does: "Open `row`, a node of a tree's group." },
            collapse: { takes: ['group', 'row'],
                does: "Close it." },
            expandAll: { takes: ['group'],
                does: "Open every node of a tree's group that is shown, and every node below them." },
            collapseAll: { takes: ['group'],
                does: "Close them." },
            expandCurrent: { takes: ['tree'],
                does: "Open the tree's current node and every node under it." },
            expandTree: { takes: ['tree'],
                does: "Open the tree from the current node's group down." },
            changeColumn: { takes: ['group', 'column', 'options'],
                does: "Make `column` the current column of the group's grid: a property of its rows, named as beside its `group`, which the grid shows now and the user can move to - a property the form groups in columns (`COLUMNS`) is refused. `options.selection` says what becomes of the columns selected: `start` (the default) selects this column alone, `extend` selects the columns up to it, `keep` leaves them." },
            selectAll: { takes: ['group'],
                does: "Select every row and every column of the group's grid." },
            copy: { takes: ['group'],
                does: "Answers the text of the cells selected in the group's grid: the rows selected, of all its rows, in the columns selected - the current row where the page shows no row as selected, the current column where no column is. A row a line, tabs between its cells. The column of an action, and those of a property the form groups in columns (`COLUMNS`), are none of the cells." },
            paste: { takes: ['group', 'text'],
                does: "Paste such a text into those cells: one value into every one of them; several, into the cells from the current one on, to the right and down - none while the current column is none of the cells." },
            activate: { takes: [],
                does: "Make the form the current one, the form the user is in, as a click on its tab does. Only a form docked into a `FORMS` window - one that has `lsfusion-form-window` - is made the current one.",
                call: function (form) {
                    form.@GFormController::activate()();
                } },
            close: { takes: [],
                does: "Close the form, as the close button of its tab does: a form docked into a `FORMS` window. An edit under way in it is committed first. The call answers at once: a form with unsaved changes asks the user first, and may stay open.",
                call: function (form) {
                    form.@GFormController::close()();
                } }
        };
    }-*/;

    // ... and THE NAVIGATOR'S METHODS it calls with no form - the navigator's controller's own
    // (GNavigatorActionDispatcher.controller): its calls of the logic are lsfusion_eval and lsfusion_exec, as a form's are
    private static native JavaScriptObject getNavigatorMethods()/*-{
        return {
            activate: { takes: ['element'],
                does: "Activate an element of the navigator, as a click on it does: its form is opened, its action run, its folder shown. `element` is the element's name, as the page shows it in the `lsfusion-container` of the element. The call answers at once, before the form is open or the action done; what the action fails with is not this call's error." }
        };
    }-*/;
}
