function calendar(element, listJsObject, controller) {
    let listJsObjectIndex = JSON.parse(JSON.stringify(listJsObject));
    let index = 0;
    listJsObjectIndex.forEach((item) => (item.index = index++));
    setTimeout(function () {
        element.style.position = 'absolute';
        element.style.top = '0px';
        element.style.left = '0px';
        element.style.bottom = '0px';
        element.style.right = '0px';

        var calendar = new FullCalendar.Calendar(element, {
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
                controller.changeProperty('start', listJsObject[info.event.extendedProps.index], info.event.start.getFullYear(),
                    info.event.start.getMonth() + 1,info.event.start.getUTCDate(), info.event.start.getUTCHours(),
                    info.event.start.getUTCMinutes(),info.event.start.getUTCSeconds());
            }
        });
        calendar.render();
    }, 0);
}