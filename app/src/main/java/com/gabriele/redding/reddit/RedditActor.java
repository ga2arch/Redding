package com.gabriele.redding.reddit;

import android.util.Log;

import com.gabriele.actor.interfaces.OnReceiveFunction;
import com.gabriele.actor.internals.AbstractActor;
import com.gabriele.redding.ReddingApp;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class RedditActor extends AbstractActor {

    public static final String LOG_TAG = "RedditActor";
    ExecutorService mService = Executors.newSingleThreadExecutor();

    @Inject
    RedditClient mReddit;

    @Override
    public void preStart() {
        ((ReddingApp) getContext()).getRedditComponent().inject(this);
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof GetHomeCmd) {
            onGetHomeCmd();

        } else if (o instanceof RefreshTokenCmd) {
            become(noauth);
            onRefreshTokenCmd();
        }
    }

    OnReceiveFunction noauth = new OnReceiveFunction() {
        @Override
        public void onReceive(Object o) {
            if (o instanceof RefreshTokenCmd) {

            } else if (o instanceof RefreshedTokenEvent) {
                unbecome();
                unstashAll();

            } else {
                stash();
            }
        }
    };

    private void onGetHomeCmd() {
        mService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Submission> subs = new ArrayList<>();
                    for (Submission sub : new SubredditPaginator(mReddit).next()) {
                        subs.add(sub);
                    }
                    getSender().tell(new HomeEvent(subs), getSelf());
                } catch (NetworkException e) {
                    if (e.getMessage().contains("403")) {
                        getSelf().tell(new RefreshTokenCmd(), getSelf());
                        getSelf().tell(new GetHomeCmd(), getSender());
                    }
                }
            }
        });
    }

    private void onRefreshTokenCmd() {
        mService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(LOG_TAG, "Refreshing token");
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                    Log.d(LOG_TAG, "Refreshed token");
                    getSelf().tell(new RefreshedTokenEvent(), getSelf());

                } catch (NoSuchTokenException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (OAuthException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Cmds
    static public class GetHomeCmd {}
    static public class RefreshTokenCmd {}

    // Events
    static public class HomeEvent {
        List<Submission> home;

        public HomeEvent(List<Submission> home) {
            this.home = home;
        }

        public List<Submission> getHome() {
            return Collections.unmodifiableList(home);
        }
    }
    static public class RefreshedTokenEvent {}

}
