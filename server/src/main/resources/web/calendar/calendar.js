function calendar(element, listJsObject, controller) {
    var listJsObjectIndex = listJsObject.map((obj, index) => Object.assign({}, obj, {index: index}));
    setTimeout(function () {
        var calendar = new FullCalendar.Calendar(element, {
            height: 'parent',
            timeZone: 'UTC',
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay'
            },
            editable: true,
            dayMaxEvents: true,
            events: listJsObjectIndex,
            eventChange: function(info) {
                controller.changeDateTimeProperty('start', listJsObject[info.event.extendedProps.index], info.event.start.getFullYear(),
                    info.event.start.getMonth() + 1,info.event.start.getUTCDate(), info.event.start.getUTCHours(),
                    info.event.start.getUTCMinutes(),info.event.start.getUTCSeconds());
            }
        });
        calendar.render();
    }, 0);
}