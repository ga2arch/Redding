package com.gabriele.redding.reddit.cmds;

public class GetFullSubmissionCmd implements Cmd {
    private final String submissionId;

    public GetFullSubmissionCmd(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() {
        return submissionId;
    }
}
