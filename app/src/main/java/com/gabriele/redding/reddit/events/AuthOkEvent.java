package com.gabriele.redding.reddit.events;

public class AuthOkEvent implements Event {
    public String username;

    public AuthOkEvent(String username) {
        this.username = username;
    }
}
