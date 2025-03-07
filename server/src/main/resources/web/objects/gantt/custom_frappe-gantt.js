function gantt() {
    return {
        render: function (element) {
            // element.style.setProperty("min-height", "0px");
            // element.style.setProperty("flex-shrink", "1");
        },
        update: function (element, controller, list, options) {
            if (list.length > 0) {
                if (!element.gantt) {
                    options.infinite_padding = false; // doesn't work so far
                    options.readonly_progress = true; // doesn't work so far
                    element.gantt = new Gantt(element, list, options);
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
                    controller.changeProperties(['start', 'end'], [task, task], [start, new Date(end.getTime() + 1000)], true);
                }
                element.gantt.options.on_progress_change = function(task, progress) {
                    controller.changeProperty('progress', task, progress);
                }
                element.gantt.options.popup = function (el, chart) {
                    let activeTask = $(element).find("[data-id='" + el.task.id + "']");
                    controller.changeObject(el.task, false, activeTask[0]);
                    return false;
                }
            }
        }
    }
}
