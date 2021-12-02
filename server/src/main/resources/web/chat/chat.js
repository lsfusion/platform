function chatMessageRender() {
    return {
        render: function (element) {
            element.style.removeProperty("max-height");
            element.parentElement.style.removeProperty("height");

            var message = document.createElement("div")
            message.classList.add("chat-message");
            message.classList.add("ql-bubble");

            var header = document.createElement("div");
            header.classList.add("chat-header");

            var author = document.createElement("div");
            author.classList.add("chat-author");

            element.author = author;
            header.appendChild(author);

            var replyAction = document.createElement("a");
            replyAction.classList.add("chat-reply-action");

            var replyCaption = document.createTextNode("Reply");
            replyAction.appendChild(replyCaption);

            element.replyAction = replyAction;
            header.appendChild(replyAction);

            message.appendChild(header);

            var replyContent = document.createElement("div");
            replyContent.classList.add("chat-reply-content");

            var replyAuthor = document.createElement("div");
            replyAuthor.classList.add("chat-reply-author");

            element.replyAuthor = replyAuthor;
            replyContent.appendChild(replyAuthor);

            var replyText = document.createElement("div");
            replyText.classList.add("chat-reply-text");

            element.replyText = replyText;
            replyContent.appendChild(replyText);

            element.replyContent = replyContent;
            message.appendChild(replyContent);

            var text = document.createElement("div");
            text.classList.add("chat-text");
            text.classList.add("ql-editor");

            element.text = text;
            message.appendChild(text);

            var attachments = document.createElement("div");
            attachments.classList.add("chat-attachments");

            element.attachments = attachments;
            message.appendChild(attachments);

            var footer = document.createElement("div");
            footer.classList.add("chat-footer");

            var time = document.createElement("div");
            time.classList.add("chat-time");

            element.time = time;
            footer.appendChild(time);

            var status = document.createElement("div");
            status.classList.add("chat-status");

            element.status = status;
            footer.appendChild(status);

            message.appendChild(footer);

            element.message = message;
            element.appendChild(message);
        },
        update: function (element, controller, value) {
            var obj = JSON.parse(value);
            element.author.innerHTML = obj.author;

            element.replyAction.onclick = function(event) {
                controller.changeValue(JSON.stringify({ action : 'reply' }));
                $(this).closest("div[lsfusion-container='chat']").find(".ql-editor").focus(); // works only in firefox
            }

            element.replyAuthor.innerHTML = obj.replyAuthor;
            element.replyText.innerHTML = obj.replyText;
            element.replyContent.onmousedown = function(event) {
                controller.changeValue(JSON.stringify({ action : 'goToReply' }));
            }

            element.text.innerHTML = obj.text;
            element.time.innerHTML = obj.time;
            element.status.innerHTML = obj.status;

            while (element.attachments.lastElementChild) {
                element.attachments.removeChild(element.attachments.lastElementChild);
            }
            if (obj.attachment !== '') {
                var attachmentA = document.createElement("a");
                attachmentA.classList.add("chat-message-attachment");

                attachmentA.onclick = function(event) {
                    controller.changeValue(JSON.stringify({ action : 'open', id : obj.id }));
                }

                var attachmentCaption = document.createTextNode(obj.attachment);
                attachmentA.appendChild(attachmentCaption);

                element.attachments.appendChild(attachmentA);
            }

            if (obj.own) {
                element.message.classList.add('chat-message-own');
            } else
                element.message.classList.remove('chat-message-own');
        }
    }
}

function chatMessageInputRender() {
    var inputQuill = window.inputQuillFiles();
    return {
        render: function (element) {
            var input = document.createElement("div");
            input.classList.add("chat-message-input");

            var reply = document.createElement("div");
            reply.classList.add("chat-reply");

            var replyContent = document.createElement("div");
            replyContent.classList.add("chat-reply-content");

            var replyAuthor = document.createElement("div");
            replyAuthor.classList.add("chat-reply-author");

            element.replyAuthor = replyAuthor;
            replyContent.appendChild(replyAuthor);

            var replyText = document.createElement("div");
            replyText.classList.add("chat-reply-text");

            element.replyText = replyText;
            replyContent.appendChild(replyText);

            element.replyContent = replyContent;
            reply.appendChild(replyContent);

            var replyRemove = document.createElement("div");
            replyRemove.classList.add("chat-reply-remove");

            element.replyRemove = replyRemove;
            reply.appendChild(replyRemove);

            input.appendChild(reply);

            inputQuill.render(input);

            var attachments = document.createElement("div");
            attachments.classList.add("chat-attachments");

            element.attachments = attachments;
            input.appendChild(attachments);

            element.input = input;
            element.appendChild(input);
        },
        update: function (element, controller, value) {
            if (value !== null) {
                var obj = JSON.parse(value);

                element.replyAuthor.innerHTML = obj.replyAuthor;
                element.replyText.innerHTML = obj.replyText;

                element.replyRemove.innerHTML = (obj.replyAuthor === '') ? '' : '❌';

                element.replyRemove.onclick = function(event) {
                    controller.changeValue(JSON.stringify({ action : 'replyRemove' }));
                }

                inputQuill.update(element.input, controller, obj.text);

                while (element.attachments.lastElementChild) {
                    element.attachments.removeChild(element.attachments.lastElementChild);
                }

                if (obj.attachment !== '') {
                    var attachmentA = document.createElement("a");
                    attachmentA.classList.add("chat-message-attachment");

                    attachmentA.onclick = function(event) {
                        controller.changeValue(JSON.stringify({ action : 'open' }));
                    }

                    var attachmentCaption = document.createTextNode(obj.attachment);
                    attachmentA.appendChild(attachmentCaption);

                    element.attachments.appendChild(attachmentA);

                    attachmentDelete = document.createElement("div")
                    attachmentDelete.classList.add("chat-message-attachment-delete");
                    attachmentDelete.innerHTML = 'x';
                    attachmentDelete.onclick = function(event) {
                        controller.changeValue(JSON.stringify({ action : 'remove' }));
                    }

                    element.attachments.appendChild(attachmentDelete);
                }
            }
        }
    }
}