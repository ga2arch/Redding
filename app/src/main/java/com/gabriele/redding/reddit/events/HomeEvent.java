package com.gabriele.redding.reddit.events;

import net.dean.jraw.models.Submission;

import java.util.Collections;
import java.util.List;

public class HomeEvent implements Event {
    List<Submission> home;

    public HomeEvent(List<Submission> home) {
        this.home = home;
    }

    public List<Submission> getHome() {
        return Collections.unmodifiableList(home);
    }
}
