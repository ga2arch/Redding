package com.gabriele.redding.reddit.cmds;

public class GetSubredditCmd implements Cmd {
    private final boolean streamed = false;
    private final String name;

    public GetSubredditCmd(String name) {
        this.name = name;
    }

    public GetSubredditCmd() {
        this.name = null;
    }

    public boolean isStreamed() {
        return streamed;
    }

    public String getName() {
        return name;
    }
}
