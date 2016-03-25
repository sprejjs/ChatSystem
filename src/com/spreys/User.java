package com.spreys;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vspreys on 25/03/16.
 */
public class User {
    private int id;
    private String username;
    private List<Message> messages;

    public User (int id, String username) {
        this.id = id;
        this.username = username;
        messages = new ArrayList<>();
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public String getUsername(){
        return username;
    }

    public int getId(){
        return id;
    }

    public List<Message> getMessages() {
        return this.messages;
    }
}
