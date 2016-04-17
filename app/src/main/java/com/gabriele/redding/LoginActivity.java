package com.gabriele.redding;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.gabriele.actor.internals.ActorRef;
import com.gabriele.redding.reddit.events.UserChallengeEvent;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthHelper;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;

public class LoginActivity extends AppCompatActivity  {
    public static final Credentials CREDENTIALS = Credentials.installedApp("N8oKowou05ZHXQ", "http://gabriele.xyz:8080");

    private final String TAG = getClass().getSimpleName();
    @Inject @Named("RedditActor")
    ActorRef redditActor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ((ReddingApp) getApplicationContext()).getRedditComponent().inject(this);
        // Create our RedditClient
        final OAuthHelper helper = AuthenticationManager.get().getRedditClient().getOAuthHelper();

        // OAuth2 scopes to request. See https://www.reddit.com/dev/api/oauth for a full list
        String[] scopes = {"identity", "read", "mysubreddits"};

        final URL authorizationUrl = helper.getAuthorizationUrl(CREDENTIALS, true, true, scopes);
        final WebView webView = ((WebView) findViewById(R.id.webview));
        // Load the authorization URL into the browser
        webView.loadUrl(authorizationUrl.toExternalForm());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.contains("code=")) {
                    // We've detected the redirect URL
                    redditActor.tell(new UserChallengeEvent(url), ActorRef.noSender());
                    LoginActivity.this.finish();

                } else if (url.contains("error=")) {
                    Toast.makeText(LoginActivity.this, "You must press 'allow' to log in with this account", Toast.LENGTH_SHORT).show();
                    webView.loadUrl(authorizationUrl.toExternalForm());
                }
            }
        });
    }
}
