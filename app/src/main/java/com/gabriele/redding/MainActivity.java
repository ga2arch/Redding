package com.gabriele.redding;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gabriele.actor.android.ActivityActor;
import com.gabriele.actor.dispatchers.MainThreadDispatcher;
import com.gabriele.actor.interfaces.WithReceive;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.actor.internals.ActorSystem;
import com.gabriele.actor.internals.Props;
import com.gabriele.redding.controls.SubmissionsAdapter;
import com.gabriele.redding.reddit.cmds.GetSubredditCmd;
import com.gabriele.redding.reddit.cmds.GetUserCmd;
import com.gabriele.redding.reddit.cmds.GetUserSubredditsCmd;
import com.gabriele.redding.reddit.cmds.RefreshTokenCmd;
import com.gabriele.redding.reddit.events.AuthOkEvent;
import com.gabriele.redding.reddit.events.SubredditEvent;
import com.gabriele.redding.reddit.events.UserSubredditsEvent;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, WithReceive {

    public static final String LOG_TAG = "MainActivity";

    @Inject @Named("RedditActor")
    ActorRef redditActor;
    @Inject
    ActorSystem system;

    private ActorRef activityRef;
    private SwipeRefreshLayout mRefreshLayout;
    private NavigationView mNavigationView;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private DrawerLayout mDrawer;
    private ProgressBar mSpinner;
    private List<Submission> mSubs = new ArrayList<>();
    private List<Subreddit> mSubreddits = new ArrayList<>();
    private Subreddit currentSubreddit;
    private boolean firstSubmission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((ReddingApp)getApplicationContext()).getRedditComponent().inject(this);

        activityRef = system.actorOf(ActivityActor.class,
                new Props(this).withDispatcher(MainThreadDispatcher.getInstance()));

        system.getEventBus().subscribe(AuthOkEvent.class, activityRef);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);
        mSpinner = (ProgressBar) findViewById(R.id.spinner);
        mRecyclerView = (RecyclerView) findViewById(R.id.submissions_list);
        assert mRecyclerView != null;
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new SubmissionsAdapter(this, mSubs);
        mRecyclerView.setAdapter(mAdapter);

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

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (currentSubreddit == null)
                    getHome();
                else
                    getSubreddit(currentSubreddit);
            }
        });

        showSpinner();

        if (!hasAuth()) startActivity(new Intent(this, LoginActivity.class));

        getHome(true);
        getUserSubreddits();
        getUser();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (currentSubreddit != null)
                getHome(true);
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
        getSubreddit(subreddit, true);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Log.d(LOG_TAG, "AuthenticationState for onResume(): " + state);

        if (state == AuthenticationState.NEED_REFRESH)
            redditActor.tell(new RefreshTokenCmd(), activityRef);
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof SubredditEvent) {
            mSpinner.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            mRefreshLayout.setRefreshing(false);

            List<Submission> subreddit = ((SubredditEvent) o).getSubreddit();
            mSubs.addAll(subreddit);
            mAdapter.notifyDataSetChanged();

        } else if (o instanceof Submission) {
            if (firstSubmission) {
                mRefreshLayout.setRefreshing(false);
                mSpinner.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                firstSubmission = false;
            }

            mSubs.add((Submission) o);
            mAdapter.notifyDataSetChanged();

        } else if (o instanceof UserSubredditsEvent) {
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
        mSubreddits = evt.subreddits;
        for (Subreddit subreddit: evt.subreddits) {
            menu.add(i, i, Menu.NONE, subreddit.getDisplayName());
            i++;
        }
    }

    private void getHome() {
        getHome(false);
    }

    private void getHome(boolean showSpinner) {
        getSubreddit(null, showSpinner);
    }

    private void getUserSubreddits() {
        redditActor.tell(new GetUserSubredditsCmd(), activityRef);
    }

    private void getUser() {
        redditActor.tell(new GetUserCmd(), activityRef);
    }

    private void getSubreddit(Subreddit subreddit) {
        getSubreddit(subreddit, false);
    }

    private void getSubreddit(Subreddit subreddit, boolean showSpinner) {
        firstSubmission = true;
        currentSubreddit = subreddit;
        mSubs.clear();

        if (showSpinner) {
            mSpinner.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        }
        if (subreddit != null) {
            setTitle(subreddit.getTitle());
            redditActor.tell(new GetSubredditCmd(subreddit.getDisplayName()), activityRef);

        } else {
            setTitle("Redding");
            redditActor.tell(new GetSubredditCmd(), activityRef);
        }

    }

    private void showSpinner() {
        mSpinner.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    private boolean hasAuth() {
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        return state != AuthenticationState.NONE;
    }
}
