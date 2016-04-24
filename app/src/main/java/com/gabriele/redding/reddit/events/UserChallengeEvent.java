package com.gabriele.redding.reddit.events;

public class UserChallengeEvent implements Event {
    private final String url;

    public UserChallengeEvent(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
