package net.labhackercd.edemocracia.task;

import net.labhackercd.edemocracia.content.Message;

public class AddMessageFailureEvent {
    private Message message;
    private Exception exception;

    public AddMessageFailureEvent(Message message, Exception exception) {
        this.message = message;
        this.exception = exception;
    }

    public Message getMessage() {
        return message;
    }

    public Exception getException() {
        return exception;
    }
}
