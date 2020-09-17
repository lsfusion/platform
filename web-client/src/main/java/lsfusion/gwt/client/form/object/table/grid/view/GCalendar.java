package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

public class GCalendar extends GSimpleStateTableView {

    private final String calendarDateType;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        runFunction(element, list, calendarDateType, getCaptions(new NativeHashMap<>()));
    }

    protected native void runFunction(Element element, JavaScriptObject objects, String calendarDateType, JsArray<JavaScriptObject> columns)/*-{
        var thisObj = this;
        var controller = thisObj.@GCalendar::getController(*)();
        var initialView = calendarDateType === 'date' ? 'dayGridMonth' : 'dayGridWeek';

        function remapList() {
            var events = [];
            for (var i = 0; i < objects.length; i++) {
                var event = {
                    'title': getTitle(objects[i]),
                    'start': objects[i][calendarDateType],
                    'index': i
                };
                events.push(event);
            }
            return events;
        }

        function getTitle(object) {
            var title = '';
            for (var x in columns) {
                if (title !== '')
                    continue;
                title = x === 'name' ? object[x] : '';
            }
            if (title === '' && columns.length >= 2) {
                for (var k = 0; k < 2; k++) {
                    title = title + ' ' + object[columns[k]];
                }
            }
            return title;
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
                    controller.changeDateTimeProperty(calendarDateType, objects[info.event.extendedProps.index], info.event.start.getFullYear(),
                        info.event.start.getMonth() + 1, info.event.start.getUTCDate(), info.event.start.getUTCHours(),
                        info.event.start.getUTCMinutes(), info.event.start.getUTCSeconds());
                }
            });
            calendar.render();
        }, 0);
    }-*/;
}
