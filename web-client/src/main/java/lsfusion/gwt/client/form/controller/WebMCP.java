package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.JavaScriptObject;

// WebMCP: the page hands the browser's AI agent its tools, through document.modelContext where the browser has one.
// The tools of the application's MCP server (/mcp, at url): a tool is the server's - its description, what it is called
// with and what it answers -, and so is its run. The page substitutes one thing, the run of lsfusion_eval and of
// lsfusion_exec in the contexts that are the page's own, `navigator` and `form`: there the script, or the action, runs
// where the user works, who sees what it shows. And a tool of the page's own, lsfusion_client: a method of a form's
// controller, called by its name
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

        // an open form as ACTIVATE FORM addresses one - by its name, the label it was opened with, the window it was
        // opened into, at least one: what a tool's `form` is, read as the server reads it (MCPEvalTool.isFormAddress)
        function isFormAddress(form) {
            return typeof form === 'object' && !Array.isArray(form) && Object.keys(form).every(function (part) {
                return (part === 'name' || part === 'label' || part === 'window') && (form[part] == null || typeof form[part] === 'string');
            }) && (form.name != null || form.label != null || form.window != null);
        }

        // the tools whose run the page substitutes where the user works: the controller's call that runs each, and the
        // argument that names what is run
        var inContext = {
            lsfusion_eval: { call: 'eval', run: 'script' },
            lsfusion_exec: { call: 'exec', run: 'action' }
        };

        // the form a call is for: the one the address names, else the current one, which is the platform's - none
        // under a modal popup, and a form embedded in a cell is the editor of the form the cell is in, which stays
        // current
        function getForm(address) {
            if (address != null) {
                try { // a part that is not given is null for Java, not undefined
                    return formsController.@FormsController::findForm(*)(address.label == null ? null : address.label,
                            address.name == null ? null : address.name, address.window == null ? null : address.window);
                } catch (e) {
                    throw new Error(e.message + ' at the address ' + JSON.stringify(address));
                }
            }
            var container = @lsfusion.gwt.client.view.MainFrame::getCurrentForm()();
            return container != null ? container.@lsfusion.gwt.client.form.view.FormContainer::getForm()() : null;
        }

        // ... and such a tool's run where the user works. A call for another context - `logics` -, and one that is no
        // call of the tool, are the server's: it runs the first and says what is wrong with the second
        function runInContext(name, args) {
            var tool = inContext[name];
            var context = args != null ? args.context : null;
            var address = args != null ? args.form : null;
            if (args == null || typeof args[tool.run] !== 'string' || !args[tool.run]
                    || (args.params != null && !Array.isArray(args.params))
                    || (context != null && context !== 'navigator' && context !== 'form')
                    || (address != null && (context === 'navigator' || !isFormAddress(address))))
                return onServer(name, args);

            var form = context === 'navigator' ? null : getForm(address);
            if (form == null && context === 'form')
                throw new Error("the user is in no form");

            // the controller's call made as the tool's: the arguments are the tool's params and the result is its answer
            var calls = (form != null ? form.@GFormController::controller : navigatorController).mcp;
            return calls[tool.call].apply(calls, [args[tool.run]].concat(args.params || []));
        }

        // THE PAGE'S OWN TOOL, lsfusion_client: a method of a form's controller (GFormController.controller) - of the
        // form its `form` addresses, else of the current one -, called by its name, the method's arguments given by
        // theirs. The call is the page's to check, there being no server to say what is wrong with it, and the browser
        // checking no call against a tool's schema: a field it does not take is said with those it does
        var formMethods = @WebMCP::getFormMethods()();
        function checkFields(given, taken, what) {
            if (given == null || typeof given !== 'object' || Array.isArray(given))
                throw new Error(what + " must be an object of " + taken.join(', '));
            Object.keys(given).forEach(function (field) {
                if (taken.indexOf(field) < 0)
                    throw new Error("unknown field `" + field + "`: " + what + " has " + taken.join(', '));
            });
        }
        function callClientMethod(args) {
            checkFields(args, ['method', 'args', 'form'], "the call");
            if (typeof args.method !== 'string' || !formMethods.hasOwnProperty(args.method))
                throw new Error("`method` must be one of " + Object.keys(formMethods).join(', '));
            var method = formMethods[args.method];
            var given = args.args != null ? args.args : {};
            checkFields(given, method.takes, "`args` of " + args.method);
            if (args.form != null && !isFormAddress(args.form))
                throw new Error("`form` must be an object of name, label, window, at least one of them, each a string");

            var form = getForm(args.form);
            if (form == null)
                throw new Error("the user is in no form");

            // the method takes its arguments in their order; what it refuses - a name that names nothing, a value that
            // is none of the property's - it throws, or rejects its Promise with, and that is the tool's failure
            var controller = form.@GFormController::controller;
            return $wnd.Promise.resolve(controller[args.method].apply(controller, method.takes.map(function (name) {
                return given[name];
            }))).then(method.answer).then(function (answer) {
                return { result: answer };
            });
        }
        registerTool({
            name: 'lsfusion_client',
            description: "Call a method of an open form of the application's page: what the user does with the form's tables, sortings, filters and values, done in the form the user sees.\n\n" +
                "`method` names the method and `args` gives its arguments by their names, as listed below; `form` is the form — the one it addresses, else the form the user is in. The answer is `{result}`: what the method answers with, nothing for a method that only changes the form. A call the form refuses is an error, with the reason; one that starts what the user is asked about — a value, a confirmation — waits for the user's answer.\n\n" +
                "**Names.** `group` is an object group of the form, by its name; `tree`, a tree by its name; `table`, the table of an object group's rows, by the group's name, or a tree, by its name; `filterGroup`, a filter group by its name. A `property` given beside its `group`, and one in the sortings and conditions of a group's `table`, is named by its name on the form; any other — one in a tree's sortings and conditions too — by its group's name and its own, `group.property`, and a property of no group by its own name.\n\n" +
                "**Values.** A value is given as JSON: a number, a string, `true` or `false`, an object or an array for a JSON property, `null` for no value; a date or a time as its ISO string with no time zone, as the form shows it — `2026-11-15`, `10:30`, `2026-11-15T10:30`.\n\n" +
                "**Methods.**\n" + Object.keys(formMethods).map(function (name) {
                    return "- `" + name + "(" + formMethods[name].takes.join(', ') + ")` — " + formMethods[name].does;
                }).join('\n'),
            inputSchema: {
                type: 'object',
                properties: {
                    method: { type: 'string', 'enum': Object.keys(formMethods), description: "The method to call." },
                    args: { type: 'object', description: "The method's arguments, by their names; one the method can do without may be left out." },
                    form: {
                        type: 'object',
                        properties: {
                            name: { type: 'string', description: "The form's name, `Namespace.form`." },
                            label: { type: 'string', description: "The label the form was opened with." },
                            window: { type: 'string', description: "The name of the `FORMS` window the form is docked into; left out for a form that has no `lsfusion-form-window`." }
                        },
                        minProperties: 1,
                        additionalProperties: false,
                        description: "The address of ONE open form, by any of its `name`, `label` and `window`: a failure when it names no form, or several. Omitted: the form the user is in. The page shows the address on the form's element, in the attributes `lsfusion-form` (the name), `lsfusion-form-label` and `lsfusion-form-window`, each that the form has."
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
    // the method answers with, where that is no JSON. A row is a handle a view has, and JSON has none: a method that
    // takes one beside the rest is called with none here - for the current row, and the current row's own change for
    // an edge of the rows -, and one that takes a row alone (expand, collapse) is not here. Nor is what takes an
    // element or a function (startEditing, stopEditing, isEditing, batch), nor the controller's calls of the logic
    // (exec, eval, evalAction, change, mcp), which lsfusion_eval and lsfusion_exec are
    private static native JavaScriptObject getFormMethods()/*-{
        // a lookup's answer: its last one - the server answers before it is sure of the form's unsaved changes, and
        // then again, in `more` -, each value as its strings, its `objects` being the handle of a row
        function lookupAnswer(answer) {
            return answer.more ? answer.more.then(lookupAnswer) : answer.data.map(function (value) {
                return { displayString: value.displayString, rawString: value.rawString };
            });
        }
        var viewRows = "Where a React view draws the rows of the group, one not in a tree: ";
        return {
            changeProperty: { takes: ['property', 'value'],
                does: "Set the property's value in the current row: `value`, `null` for none. With no `value`, start the property's change as a click on it does: an action is run, a value may be asked of the user." },
            getValues: { takes: ['property', 'value', 'options'], answer: lookupAnswer,
                does: "The values the form suggests for the property in the current row, by the text `value`, `''` for any. `options`, any of `{mode, count}`: `mode` is `objects`, the objects the property's value is picked among (the default), `values`, the values the property has, or `change`, what its change suggests; `count`, how many more than the server suggests at once. Answers `[{displayString, rawString}]`, each value as it is shown and as it is typed." },
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
            expandAll: { takes: ['group'],
                does: "Open every node of a tree's group that is shown, and every node below them." },
            collapseAll: { takes: ['group'],
                does: "Close them." },
            expandCurrent: { takes: ['tree'],
                does: "Open the tree's current node and every node under it." },
            expandTree: { takes: ['tree'],
                does: "Open the tree from the current node's group down." },
            changeObject: { takes: ['group', 'row', 'options'],
                does: viewRows + "make the first of them (`row`: `{to: 'home'}`) or the last (`{to: 'end'}`) the current row. `options.selection` says what becomes of the rows selected: `start` (the default), `extend` or `keep`." },
            changeColumn: { takes: ['group', 'column', 'options'],
                does: viewRows + "make `column`, a property of the rows by its name on the form, the current column; `options` as in `changeObject`, for the columns selected." },
            selectAll: { takes: ['group'],
                does: viewRows + "select every row and column." },
            copy: { takes: ['group'],
                does: viewRows + "the text of the cells selected, of the current one where none are: a row a line, tabs between its cells. Answers the text." },
            paste: { takes: ['group', 'text'],
                does: viewRows + "paste such a text: one value into every cell selected, several from the current cell on, to the right and down." },
            changeViewFilters: { takes: ['group', 'conditions'],
                does: viewRows + "the view's own filters of them, an array of conditions as in `changeFilters`, none joined by `or`." }
        };
    }-*/;
}
