package com.gabriele.redding.reddit.events;

import net.dean.jraw.models.Submission;

import java.util.Collections;
import java.util.List;

public class SubredditEvent implements Event {
    List<Submission> home;

    public SubredditEvent(List<Submission> home) {
        this.home = home;
    }

    public List<Submission> getSubreddit() {
        return Collections.unmodifiableList(home);
    }
}
