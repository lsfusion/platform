function findGanttElementById(element, id) {
    return $(element).find("[data-id='" + id + "']");
}

function gantt_quarter_day() {
    return gantt({ view_mode: 'Quarter Day' });
}
function gantt_half_day() {
    return gantt({ view_mode: 'Half Day' });
}
function gantt_day() {
    return gantt({ view_mode: 'Day' });
}
function gantt_week() {
    return gantt({ view_mode: 'Week' });
}
function gantt_month() {
    return gantt({ view_mode: 'Month' });
}
function gantt_year() {
    return gantt({ view_mode: 'Year' });
}

function gantt(options) {
    return {
        render: function (element) {
            element.style.setProperty("min-height", "0px");
            element.style.setProperty("flex-shrink", "1");
        },
        update: function (element, controller, list) {
            if (list.length > 0) {
                if (!element.gantt) {
                    element.gantt = new FrappeGantt(element, list, options);
                    setTimeout(function() {
                        element.gantt.set_scroll_position();
                    }, 0);
                } else {
                    let scrollElement = element.gantt.$container;
                    let topPos = scrollElement.scrollTop;
                    let leftPos = scrollElement.scrollLeft;

                    element.gantt.refresh(list);

                    scrollElement.scrollTop = topPos;
                    scrollElement.scrollLeft = leftPos;
                }

                for (let task of list)
                    if (controller.isCurrent(task)) {
                        let activeTask = $(element).find("[data-id='" + task.id + "']");
                        if (activeTask[0]) activeTask[0].classList.add("current");
                    }

                element.gantt.options.on_date_change = function(task, start, end) {
                    if (options.view_mode === "Quarter Day" || options.view_mode === "Half Day")
                        controller.changeDateTimeProperties(['start', 'end'],
                            [task, task],
                            [start.getFullYear(), end.getFullYear()],
                            [start.getMonth() + 1, end.getMonth() + 1],
                            [start.getDate(), end.getDate()],
                            [start.getHours(), end.getHours()],
                            [start.getMinutes(), end.getMinutes()],
                            [start.getSeconds(), end.getSeconds()]);
                    else
                        controller.changeDateProperties(['start', 'end'],
                                                        [task, task],
                                                        [start.getFullYear(), end.getFullYear()],
                                                        [start.getMonth() + 1, end.getMonth() + 1],
                                                        [start.getDate(), end.getDate()]);
                }
                element.gantt.options.on_progress_change = function(task, progress) {
                    controller.changeProperty('progress', task, progress);
                }
                element.gantt.options.on_click = function(task) {
                    controller.changeObject(task, false, element.gantt.popup_wrapper);
                }

            }
        }
    }
}
