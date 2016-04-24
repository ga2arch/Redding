package com.gabriele.redding.reddit.cmds;

public class GetSubmissionCmd {
    private String url;

    public GetSubmissionCmd(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
