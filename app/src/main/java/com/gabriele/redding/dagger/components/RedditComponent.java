package com.gabriele.redding.dagger.components;

import com.gabriele.actor.internals.ActorRef;
import com.gabriele.redding.LoginActivity;
import com.gabriele.redding.MainActivity;
import com.gabriele.redding.dagger.modules.ActorModule;
import com.gabriele.redding.dagger.modules.AppModule;
import com.gabriele.redding.dagger.modules.RedditModule;
import com.gabriele.redding.fragments.SubredditFragment;
import com.gabriele.redding.reddit.RedditActor;
import com.gabriele.redding.SubmissionActivity;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={AppModule.class, RedditModule.class, ActorModule.class})
public interface RedditComponent {
    void inject(RedditActor actor);
    void inject(MainActivity activity);
    void inject(SubmissionActivity activity);
    void inject(LoginActivity activity);
    void inject(SubredditFragment subredditFragment);

    RedditClient getRedditClient();
    AuthenticationManager getAuthManager();
    @Named("RedditActor") ActorRef getRedditActor();

}