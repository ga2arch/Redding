package com.gabriele.redding.reddit;

import android.util.Log;

import com.gabriele.actor.interfaces.OnReceiveFunction;
import com.gabriele.actor.internals.AbstractActor;
import com.gabriele.actor.internals.ActorMessage;
import com.gabriele.redding.LoginActivity;
import com.gabriele.redding.ReddingApp;
import com.gabriele.redding.reddit.cmds.GetSubredditCmd;
import com.gabriele.redding.reddit.cmds.GetUserCmd;
import com.gabriele.redding.reddit.cmds.GetUserSubredditsCmd;
import com.gabriele.redding.reddit.cmds.RefreshTokenCmd;
import com.gabriele.redding.reddit.events.AuthFailEvent;
import com.gabriele.redding.reddit.events.AuthOkEvent;
import com.gabriele.redding.reddit.events.HomeEvent;
import com.gabriele.redding.reddit.events.RefreshedTokenEvent;
import com.gabriele.redding.reddit.events.UserChallengeEvent;
import com.gabriele.redding.reddit.events.UserSubredditsEvent;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.Paginator;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.UserSubredditsPaginator;

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
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        if (state != AuthenticationState.READY)
            become(noauth);
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof GetUserCmd) {
            runCmd(onGetUserCmd());

        } else if (o instanceof GetSubredditCmd) {
            runCmd(onGetSubredditCmd((GetSubredditCmd) o));

        } else if (o instanceof GetUserSubredditsCmd) {
            runCmd(onGetUserSubredditsCmd());

        } else if (o instanceof RefreshTokenCmd) {
            become(noauth);
            runCmd(onRefreshTokenCmd());

        }
    }

    OnReceiveFunction noauth = new OnReceiveFunction() {
        @Override
        public void onReceive(Object o) {
            if (o instanceof RefreshTokenCmd) {
                runCmd(onRefreshTokenCmd());

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

    private Runnable onGetSubredditCmd(final GetSubredditCmd cmd) {
        return new Runnable() {
            @Override
            public void run() {
                List<Submission> subs = new ArrayList<>();
                Paginator<Submission> paginator;
                if (cmd.name != null)
                    paginator = new SubredditPaginator(mReddit, cmd.name);
                else
                    paginator = new SubredditPaginator(mReddit);

                for (Submission sub : paginator.next()) {
                    if (cmd.streamed) {
                        getSender().tell(sub, getSelf());
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else
                        subs.add(sub);
                }
                getSender().tell(new HomeEvent(subs), getSelf());
            }
        };
    }

    private Runnable onGetUserSubredditsCmd() {
        return new Runnable() {
            @Override
            public void run() {
                List<Subreddit> subreddits = new ArrayList<>();
                for (Subreddit subreddit: new UserSubredditsPaginator(mReddit, "subscriber").next()) {
                    subreddits.add(subreddit);
                }
                getSender().tell(new UserSubredditsEvent(subreddits), getSelf());
            }
        };
    }

    private Runnable onGetUserCmd() {
        return new Runnable() {
            @Override
            public void run() {
                LoggedInAccount account = AuthenticationManager.get().getRedditClient().me();
                getSender().tell(account, getSelf());
            }
        };
    }

    private Runnable onRefreshTokenCmd() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    AuthenticationState state = AuthenticationManager.get().checkAuthState();
                    if (state == AuthenticationState.READY) return;

                    Log.d(LOG_TAG, "Refreshing token");
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                    Log.d(LOG_TAG, "Refreshed token");
                    getSelf().tell(new RefreshedTokenEvent(), getSelf());

                } catch (NoSuchTokenException | OAuthException e) {
                    e.printStackTrace();
                }
            }
        };
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

    private void runCmd(final Runnable fun) {
        final ActorMessage message = getActorContext().getCurrentMessage();
        mService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    fun.run();
                } catch (NetworkException e) {
                    if (e.getMessage().contains("403")) {
                        getSelf().tell(new RefreshTokenCmd(), getSelf());
                        getSelf().tell(message.getObject(), message.getSender());
                    }
                }
            }
        });
    }

}
