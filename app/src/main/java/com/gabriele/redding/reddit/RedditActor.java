package com.gabriele.redding.reddit;

import android.util.Log;

import com.gabriele.actor.interfaces.OnReceiveFunction;
import com.gabriele.actor.internals.AbstractActor;
import com.gabriele.redding.LoginActivity;
import com.gabriele.redding.ReddingApp;
import com.gabriele.redding.reddit.cmds.GetHomeCmd;
import com.gabriele.redding.reddit.cmds.GetUserCmd;
import com.gabriele.redding.reddit.cmds.RefreshTokenCmd;
import com.gabriele.redding.reddit.events.AuthFailEvent;
import com.gabriele.redding.reddit.events.AuthOkEvent;
import com.gabriele.redding.reddit.events.HomeEvent;
import com.gabriele.redding.reddit.events.RefreshedTokenEvent;
import com.gabriele.redding.reddit.events.UserChallengeEvent;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class RedditActor extends AbstractActor {

    public static final String LOG_TAG = "RedditActor";
    ExecutorService mService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Inject
    RedditClient mReddit;

    @Override
    public void preStart() {
        ((ReddingApp) getContext()).getRedditComponent().inject(this);
        if (AuthenticationManager.get().checkAuthState() != AuthenticationState.NONE)
            become(noauth);
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof GetHomeCmd) {
            onGetHomeCmd(((GetHomeCmd) o).streamed);

        } else if (o instanceof GetUserCmd) {
            onGetUserCmd();

        } else if (o instanceof RefreshTokenCmd) {
            become(noauth);
            onRefreshTokenCmd();
        }
    }

    OnReceiveFunction noauth = new OnReceiveFunction() {
        @Override
        public void onReceive(Object o) {
            if (o instanceof RefreshTokenCmd) {
                onRefreshTokenCmd();

            } else if (o instanceof UserChallengeEvent) {
                onUserChallenge((UserChallengeEvent) o);

            } else if (o instanceof RefreshedTokenEvent || o instanceof AuthOkEvent) {
                unbecome();
                unstashAll();

            } else {
                stash();
            }
        }
    };

    private void onGetHomeCmd(final boolean streamed) {
        mService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Submission> subs = new ArrayList<>();
                    for (Submission sub : new SubredditPaginator(mReddit).next()) {
                        if (streamed) {
                            getSender().tell(sub, getSelf());
                            Thread.sleep(80);

                        } else
                            subs.add(sub);
                    }
                    getSender().tell(new HomeEvent(subs), getSelf());

                } catch (NetworkException e) {
                    if (e.getMessage().contains("403")) {
                        getSelf().tell(new RefreshTokenCmd(), getSelf());
                        getSelf().tell(new GetHomeCmd(), getSender());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void onGetUserCmd() {
        mService.execute(new Runnable() {
            @Override
            public void run() {
                getSender().tell(AuthenticationManager.get().getRedditClient().me(), getSelf());
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

    private void onUserChallenge(final UserChallengeEvent evt) {
        mService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    OAuthData data  = AuthenticationManager.get()
                            .getRedditClient()
                            .getOAuthHelper()
                            .onUserChallenge(evt.url, LoginActivity.CREDENTIALS);
                    AuthenticationManager.get().getRedditClient().authenticate(data);
                    String username = AuthenticationManager.get().getRedditClient().getAuthenticatedUser();
                    getSelf().tell(new AuthOkEvent(username), getSelf());
                    getEventBus().publish(new AuthOkEvent(username), getSelf());

                } catch (OAuthException e) {
                    getEventBus().publish(new AuthFailEvent(), getSelf());
                    e.printStackTrace();
                }
            }
        });
    }
}
