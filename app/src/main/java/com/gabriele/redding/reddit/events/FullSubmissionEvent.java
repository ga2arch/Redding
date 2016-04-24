package com.gabriele.redding.reddit.events;

import net.dean.jraw.models.Submission;

public class FullSubmissionEvent implements Event {
    private final Submission submission;

    public FullSubmissionEvent(Submission submission) {
        this.submission = submission;
    }

    public Submission getSubmission() {
        return submission;
    }
}
