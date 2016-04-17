package com.gabriele.redding.reddit.events;

public class UserChallengeEvent implements Event {
    public String url;

    public UserChallengeEvent(String url) {
        this.url = url;
    }
}
