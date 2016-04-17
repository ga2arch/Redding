package com.gabriele.redding.reddit.events;

import net.dean.jraw.models.Subreddit;

import java.util.List;

public class UserSubredditsEvent implements Event {
    public List<Subreddit> subreddits;

    public UserSubredditsEvent(List<Subreddit> subreddits) {
        this.subreddits = subreddits;
    }
}
