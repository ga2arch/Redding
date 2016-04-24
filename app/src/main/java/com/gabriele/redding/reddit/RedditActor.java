package com.gabriele.redding.reddit;

import android.util.Log;

import com.gabriele.actor.exceptions.ActorIsTerminatedException;
import com.gabriele.actor.interfaces.OnReceiveFunction;
import com.gabriele.actor.internals.AbstractActor;
import com.gabriele.actor.internals.ActorMessage;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.redding.LoginActivity;
import com.gabriele.redding.ReddingApp;
import com.gabriele.redding.reddit.cmds.GetFullSubmissionCmd;
import com.gabriele.redding.reddit.cmds.GetSubredditCmd;
import com.gabriele.redding.reddit.cmds.GetUserCmd;
import com.gabriele.redding.reddit.cmds.GetUserSubredditsCmd;
import com.gabriele.redding.reddit.cmds.RefreshTokenCmd;
import com.gabriele.redding.reddit.cmds.StopRequestsCmd;
import com.gabriele.redding.reddit.events.AuthFailEvent;
import com.gabriele.redding.reddit.events.AuthOkEvent;
import com.gabriele.redding.reddit.events.FullSubmissionEvent;
import com.gabriele.redding.reddit.events.RefreshedTokenEvent;
import com.gabriele.redding.reddit.events.SubredditEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

public class RedditActor extends AbstractActor {

    public static final String LOG_TAG = "RedditActor";
    private final ExecutorService mService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final HashMap<ActorRef, ArrayList<Future>> mRequests = new HashMap<>();

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

        } else if (o instanceof GetFullSubmissionCmd) {
            runCmd(onGetFullSubmission((GetFullSubmissionCmd) o));

        } else if (o instanceof StopRequestsCmd) {
            ArrayList<Future> fs = mRequests.get(getSender());
            if (fs != null) {
                for (Future f: fs) {
                    f.cancel(true);
                }
            }

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
        final ActorRef sender = getSender();
        return new Runnable() {
            @Override
            public void run() {
                List<Submission> subs = new ArrayList<>();
                Paginator<Submission> paginator;
                if (cmd.getName() != null)
                    paginator = new SubredditPaginator(mReddit, cmd.getName());
                else
                    paginator = new SubredditPaginator(mReddit);

                for (Submission sub : paginator.next()) {
                    if (cmd.isStreamed()) {
                        sender.tell(sub, getSelf());
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else
                        subs.add(sub);
                }
                sender.tell(new SubredditEvent(subs), getSelf());
            }
        };
    }

    private Runnable onGetFullSubmission(final GetFullSubmissionCmd cmd) {
        final ActorRef sender = getSender();
        return new Runnable() {
            @Override
            public void run() {
                Submission fullSub = mReddit.getSubmission(cmd.getSubmissionId());
                sender.tell(new FullSubmissionEvent(fullSub), getSelf());
            }
        };
    }

    private Runnable onGetUserSubredditsCmd() {
        final ActorRef sender = getSender();
        return new Runnable() {
            @Override
            public void run() {
                List<Subreddit> subreddits = new ArrayList<>();
                for (Subreddit subreddit: new UserSubredditsPaginator(mReddit, "subscriber").next()) {
                    subreddits.add(subreddit);
                }
                sender.tell(new UserSubredditsEvent(subreddits), getSelf());
            }
        };
    }

    private Runnable onGetUserCmd() {
        final ActorRef sender = getSender();
        return new Runnable() {
            @Override
            public void run() {
                LoggedInAccount account = AuthenticationManager.get().getRedditClient().me();
                sender.tell(account, getSelf());
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
                            .onUserChallenge(evt.getUrl(), LoginActivity.CREDENTIALS);
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
        Future f = mService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    fun.run();
                } catch (NetworkException e) {
                    if (e.getMessage().contains("403")) {
                        getSelf().tell(new RefreshTokenCmd(), getSelf());
                        getSelf().tell(message.getObject(), message.getSender());
                    }
                } catch (ActorIsTerminatedException e) {
                    Log.d(LOG_TAG, e.getMessage(), e);
                }
            }
        });

        ArrayList<Future> requests = mRequests.get(getSender());
        if (requests == null) {
            requests = new ArrayList<>();
            requests.add(f);
        } else {
            requests.add(f);
            mRequests.put(getSender(), requests);
        }
    }

}
