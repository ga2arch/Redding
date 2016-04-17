package com.gabriele.redding.dagger.modules;

import android.content.Context;

import com.gabriele.redding.reddit.AndroidRedditClient;
import com.gabriele.redding.reddit.AndroidTokenStore;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.LoggingMode;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RedditModule {

    @Provides
    @Singleton
    RedditClient providesRedditClient(Context context) {
        RedditClient reddit = new AndroidRedditClient(context);
        reddit.setLoggingMode(LoggingMode.ALWAYS);
        return reddit;
    }

    @Provides
    @Singleton
    AuthenticationManager providesAuthManager(Context context, RedditClient reddit) {
        AuthenticationManager.get().init(reddit,
                new RefreshTokenHandler(new AndroidTokenStore(context), reddit));

        return AuthenticationManager.get();
    }
}
