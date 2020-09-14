package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

public class GCalendar extends GSimpleStateTableView {

    public GCalendar(GFormController form, GGridController grid) {
        super(form, grid);
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        runFunction(element, list);
    }

    protected native void runFunction(Element element, JavaScriptObject list)/*-{
        var thisObj = this;
        var controller = thisObj.@GCalendar::getController(*)();
        var start = list[0]['time'] == null ? 'date' : 'time';
        var initialView = start === 'date' ? 'dayGridMonth' : 'dayGridWeek';

        function remapList() {
            var events = [];
            for (var i = 0; i < list.length; i++) {
                var event = {
                    'title': list[i]['name'],
                    'start': list[i][start],
                    'index': i
                };
                events.push(event);
            }
            return events;
        }

        setTimeout(function () {
            var calendar = new $wnd.FullCalendar.Calendar(element, {
                initialView: initialView,
                height: 'parent',
                timeZone: 'UTC',
                firstDay: 1,
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'dayGridMonth,dayGridWeek,timeGridDay'
                },
                editable: true,
                dayMaxEvents: 4,
                events: remapList(),
                eventChange: function (info) {
                    controller.changeDateTimeProperty(start, list[info.event.extendedProps.index], info.event.start.getFullYear(),
                        info.event.start.getMonth() + 1, info.event.start.getUTCDate(), info.event.start.getUTCHours(),
                        info.event.start.getUTCMinutes(), info.event.start.getUTCSeconds());
                }
            });
            calendar.render();
        }, 0);
    }-*/;
}
