function reorderBoard(controller, elements) {
    for (let i = 0; i < elements.length; i++) {
        if (elements[i].object.order !== i) {
            controller.changeProperty("order", elements[i].object, i);
        }
    }
}
function kanban() {
    return {
        render: function (element) {
            element.style.setProperty("min-height", "0px");
            element.style.setProperty("flex-shrink", "1");
        },
        update: function (element, controller, list) {
            if (!element.kanban) {
                element.kanban = new jKanban( {
                    element: element,
                    responsivePercentage: true,

                    click: function(el) {
                        controller.changeObject(el.object, true, el)
                    },
                    dragendBoard: function (el) {
                        reorderBoard(controller, el.parentNode.childNodes);
                    },
                    dropEl: function(el, target, source, sibling) {
                        reorderBoard(controller, target.childNodes);
                        if (source !== target) {
                            controller.changeProperty("boardId", el.object, target.parentElement.object.id);
                            reorderBoard(controller, source.childNodes);
                        }
                    },
                    buttonClick: function(el, boardId) {
                        controller.changeProperty("new", element.kanban.findBoard(boardId).object);
                    },
                } );
            }

            let kanban = element.kanban;

            while (kanban.options.boards.length > 0)
                kanban.removeBoard(kanban.options.boards[0].id);

            if (list[0] && list[0].new)
                kanban.options.itemAddOptions.enabled = true;

            kanban.addBoards(Array.from(list.filter(object => !object.boardId), object => ({id: object.id, title: object.title})));

            for (let object of list) {
                if (!object.boardId) {
                    // kanban.addBoards([{id: object.id, title: object.title}])
                    kanban.findBoard(object.id).object = object;
                }
            }

            for (let object of list) {
                if (object.boardId) {
                    kanban.addElement(object.boardId, {id: object.id, title: object.title});
                    kanban.findElement(object.id).object = object;
                }
            }
                // let diff = controller.getDiff(list);
            // for (let object of diff.remove) {
            //     if (object.boardId)
            //         kanban.removeElement(object.id);
            //     else
            //         kanban.removeBoard(object.id);
            // }
            // for (let object of diff.add) {
            //     if (!object.boardId) {
            //         kanban.addBoards([{id: object.id, title: object.title}])
            //         kanban.findBoard(object.id).object = object;
            //     } else {
            //         kanban.addElement(object.boardId, {id: object.id, title: object.title});
            //         kanban.findElement(object.id).object = object;
            //     }
            // }
            // for (let object of diff.update) {
            //     if (object.boardId) {
            //         kanban.replaceElement(object.id, {id: object.id, title: object.title});
            //         kanban.findElement(object.id).object = object;
            //     } else {
            //         kanban.findBoard(object.id).object = object;
            //     }
            //     // support change board
            // }
        }
    }
}
