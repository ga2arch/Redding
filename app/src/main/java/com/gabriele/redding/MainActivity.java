package com.gabriele.redding;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.gabriele.actor.android.AppCompatActivityAsActor;
import com.gabriele.actor.interfaces.WithReceive;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.actor.internals.Props;
import com.gabriele.redding.fragments.SubredditFragment;
import com.gabriele.redding.reddit.cmds.GetSubredditCmd;
import com.gabriele.redding.reddit.cmds.GetUserCmd;
import com.gabriele.redding.reddit.cmds.GetUserSubredditsCmd;
import com.gabriele.redding.reddit.cmds.RefreshTokenCmd;
import com.gabriele.redding.reddit.events.AuthOkEvent;
import com.gabriele.redding.reddit.events.UserSubredditsEvent;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.models.Subreddit;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class MainActivity extends AppCompatActivityAsActor
        implements NavigationView.OnNavigationItemSelectedListener, WithReceive {

    public static final String LOG_TAG = "MainActivity";

    @Inject @Named("RedditActor")
    ActorRef redditActor;

    private NavigationView mNavigationView;
    private DrawerLayout mDrawer;
    private List<Subreddit> mSubreddits;
    private ActorRef fragmentRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((ReddingApp)getApplicationContext()).getRedditComponent().inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert mDrawer != null;

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView= (NavigationView) findViewById(R.id.nav_view);
        assert mNavigationView != null;
        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.getMenu().clear();

        SubredditFragment fragment = new SubredditFragment();
        fragmentRef = getActorContext().actorOf(Props.create(fragment));
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();

        if (!hasAuth()) startActivity(new Intent(this, LoginActivity.class));

        getHome();
        getUserSubreddits();
        getUser();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!getTitle().equals("Redding"))
                getHome();
            else
                super.onBackPressed();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Subreddit subreddit = mSubreddits.get(id);
        getSubreddit(subreddit);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getEventBus().subscribe(AuthOkEvent.class, getSelf());

        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Log.d(LOG_TAG, "AuthenticationState for onResume(): " + state);

        if (state == AuthenticationState.NEED_REFRESH)
            redditActor.tell(new RefreshTokenCmd(), getSelf());
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof UserSubredditsEvent) {
            onUserSubredditsEvent((UserSubredditsEvent) o);

        } else if (o instanceof LoggedInAccount) {
            onLoggedInAccount((LoggedInAccount) o);
        }
    }

    private void onLoggedInAccount(LoggedInAccount account) {
        TextView usernameView = (TextView) mDrawer.findViewById(R.id.username);
        TextView karmaView = (TextView) mDrawer.findViewById(R.id.karma);

        usernameView.setText(account.getFullName());
        karmaView.setText(String.valueOf(account.getCommentKarma()));
    }

    private void onUserSubredditsEvent(UserSubredditsEvent evt) {
        Menu menu = mNavigationView.getMenu();
        menu.clear();
        int i = 0;
        mSubreddits = evt.getSubreddits();
        for (Subreddit subreddit: evt.getSubreddits()) {
            menu.add(i, i, Menu.NONE, subreddit.getDisplayName());
            i++;
        }
    }

    private void getHome() {
        fragmentRef.tell(new GetSubredditCmd(), getSelf());
    }

    private void getSubreddit(Subreddit subreddit) {
        fragmentRef.tell(new GetSubredditCmd(subreddit), getSelf());
    }

    private void getUserSubreddits() {
        redditActor.tell(new GetUserSubredditsCmd(), getSelf());
    }

    private void getUser() {
        redditActor.tell(new GetUserCmd(), getSelf());
    }

    private boolean hasAuth() {
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        return state != AuthenticationState.NONE;
    }
}
