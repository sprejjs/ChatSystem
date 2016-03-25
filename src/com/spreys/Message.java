package com.spreys;

/**
 * Created by vspreys on 25/03/16.
 */
public class Message {
    private boolean incoming;
    private String message;

    public Message (boolean incoming, String message) {
        this.incoming = incoming;
        this.message = message;
    }

    public boolean getIncoming() {
        return incoming;
    }

    public String getMessage() {
        return message;
    }
}
