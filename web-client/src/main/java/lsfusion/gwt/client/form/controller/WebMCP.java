package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.JavaScriptObject;

// WebMCP: the page hands the browser's AI agent the tools of the application's MCP server (/mcp, at url), through
// document.modelContext where the browser has one. A tool is the server's - its description, what it is called with
// and what it answers -, and so is its run. The page substitutes one thing, the run of lsfusion_eval in the contexts
// that are the page's own, `navigator` and `form`: there the script runs where the user works, who sees what it shows
public class WebMCP {

    public static native void register(String url, FormsController formsController, JavaScriptObject navigatorController)/*-{
        // navigator.modelContext is where the API was first, and where a polyfill may put it
        var modelContext = $doc.modelContext || $wnd.navigator.modelContext;
        if (!modelContext)
            return;

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
        // opened into, at least one: what the tool's `form` is, read as the server reads it (MCPEvalTool.isFormAddress)
        function isFormAddress(form) {
            return typeof form === 'object' && !Array.isArray(form) && Object.keys(form).every(function (part) {
                return (part === 'name' || part === 'label' || part === 'window') && (form[part] == null || typeof form[part] === 'string');
            }) && (form.name != null || form.label != null || form.window != null);
        }

        // the form a script is for: the one the address names, else the current one, which is the platform's - none
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

        // ... and the eval tool's run where the user works. A call for another context - `logics` -, and one that is
        // no call of the tool, are the server's: it runs the first and says what is wrong with the second
        function evalInContext(args) {
            var context = args != null ? args.context : null;
            var address = args != null ? args.form : null;
            if (args == null || typeof args.script !== 'string' || !args.script
                    || (args.params != null && !Array.isArray(args.params))
                    || (context != null && context !== 'navigator' && context !== 'form')
                    || (address != null && (context === 'navigator' || !isFormAddress(address))))
                return onServer('lsfusion_eval', args);

            var form = context === 'navigator' ? null : getForm(address);
            if (form == null && context === 'form')
                throw new Error("the user is in no form");

            // the controller's eval made as the tool's: the arguments are the tool's params and the result is its answer
            var calls = (form != null ? form.@GFormController::controller : navigatorController).mcp;
            return calls.eval.apply(calls, [args.script].concat(args.params || []));
        }

        request('tools/list').then(function (list) {
            list.tools.forEach(function (tool) {
                var name = tool.name;
                var inContext = name === 'lsfusion_eval';
                // a tool's failure is its answer, an MCP tool result as the server's failures are: what a tool throws
                // does not reach the agent, which is told only that the call failed
                tool.execute = function (args) {
                    return new $wnd.Promise(function (resolve) {
                        resolve(inContext ? evalInContext(args) : onServer(name, args));
                    }).then(null, function (e) {
                        return { content: [{ type: 'text', text: name + ' error: ' + e.message }], isError: true };
                    });
                };
                if (inContext) { // a script can save what it changes: the agent is to ask the user before a call that it cannot take back.
                    // The one thing added to a descriptor: the hint is WebMCP's own, an MCP server has no name for it
                    tool.annotations = tool.annotations || {};
                    tool.annotations.consequentialHint = true;
                }
                // a tool the browser refuses - a name another script of the page has taken - does not keep the others out;
                // the refusal is thrown where registerTool returns no promise yet
                new $wnd.Promise(function (resolve) {
                    resolve(modelContext.registerTool(tool));
                }).then(null, function (e) {
                    $wnd.console.warn('WebMCP: ' + name + ' is not registered: ' + e.message);
                });
            });
        }).then(null, function (e) {
            $wnd.console.warn('WebMCP: the tools are not registered: ' + e.message);
        });
    }-*/;
}
