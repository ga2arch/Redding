package com.gabriele.redding.reddit.events;

import net.dean.jraw.models.Submission;

import java.util.Collections;
import java.util.List;

public class SubredditEvent implements Event {
    private final List<Submission> home;

    public SubredditEvent(List<Submission> home) {
        this.home = home;
    }

    public List<Submission> getSubmissions() {
        return Collections.unmodifiableList(home);
    }
}
