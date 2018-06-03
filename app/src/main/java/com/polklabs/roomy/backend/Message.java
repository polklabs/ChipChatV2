package com.polklabs.roomy.backend;

public class Message {
    private String message, user, time;
    private boolean sentByMe;

    public Message(){}

    public Message(String message, String user, String time){
        this.message = message;
        this.user = user;
        this.time = time;
        this.sentByMe = false;
    }

    public void setSentByMe(){ this.sentByMe = true;}

    public boolean isSentByMe(){return this.sentByMe;}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
