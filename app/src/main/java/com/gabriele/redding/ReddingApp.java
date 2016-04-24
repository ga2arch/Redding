package com.gabriele.redding;

import com.gabriele.actor.android.ActorApplication;
import com.gabriele.redding.dagger.components.DaggerRedditComponent;
import com.gabriele.redding.dagger.components.RedditComponent;
import com.gabriele.redding.dagger.modules.ActorModule;
import com.gabriele.redding.dagger.modules.AppModule;
import com.gabriele.redding.dagger.modules.RedditModule;

public class ReddingApp extends ActorApplication {

    private RedditComponent mRedditComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        mRedditComponent = DaggerRedditComponent
                .builder()
                .appModule(new AppModule(this))
                .redditModule(new RedditModule())
                .actorModule(new ActorModule())
                .build();

        mRedditComponent.getRedditClient();
        mRedditComponent.getAuthManager();
        mRedditComponent.getRedditActor();
    }

    public RedditComponent getRedditComponent() {
        return mRedditComponent;
    }
}
