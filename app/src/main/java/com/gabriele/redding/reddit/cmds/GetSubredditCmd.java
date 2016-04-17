package com.gabriele.redding.reddit.cmds;

public class GetSubredditCmd implements Cmd {
    public boolean streamed;
    public String name;

    public GetSubredditCmd(String name) {
        this.name = name;
    }

    public GetSubredditCmd() {
    }
}
