package com.gabriele.redding.reddit.cmds;

public class GetSubredditCmd implements Cmd {
    public boolean streamed = true;
    public String name;

    public GetSubredditCmd(String name) {
        this.name = name;
    }

    public GetSubredditCmd() {
    }
}
