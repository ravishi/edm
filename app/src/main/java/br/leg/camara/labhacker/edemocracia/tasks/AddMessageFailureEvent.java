package br.leg.camara.labhacker.edemocracia.tasks;

import br.leg.camara.labhacker.edemocracia.content.Message;

/**
 * Created by dirley on 27/01/15.
 */
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
