package com.gabriele.redding.reddit.cmds;

import net.dean.jraw.models.Subreddit;

public class GetSubredditCmd implements Cmd {
    private final boolean streamed = false;
    private final Subreddit subreddit;

    public GetSubredditCmd(Subreddit subreddit) {
        this.subreddit = subreddit;
    }

    public GetSubredditCmd() {
        this.subreddit = null;
    }

    public boolean isStreamed() {
        return streamed;
    }

    public Subreddit getSubreddit() {
        return subreddit;
    }
}
