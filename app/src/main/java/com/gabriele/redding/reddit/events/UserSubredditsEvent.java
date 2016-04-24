package com.gabriele.redding.reddit.events;

import net.dean.jraw.models.Subreddit;

import java.util.Collections;
import java.util.List;

public class UserSubredditsEvent implements Event {
    private final List<Subreddit> subreddits;

    public UserSubredditsEvent(List<Subreddit> subreddits) {
        this.subreddits = subreddits;
    }

    public List<Subreddit> getSubreddits() {
        return Collections.unmodifiableList(subreddits);
    }
}
