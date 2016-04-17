package com.gabriele.redding;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.gabriele.actor.android.ActivityActor;
import com.gabriele.actor.dispatchers.MainThreadDispatcher;
import com.gabriele.actor.interfaces.WithReceive;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.actor.internals.ActorSystem;
import com.gabriele.actor.internals.Props;
import com.gabriele.redding.controls.SubmissionsAdapter;
import com.gabriele.redding.reddit.cmds.GetHomeCmd;
import com.gabriele.redding.reddit.cmds.RefreshTokenCmd;
import com.gabriele.redding.reddit.events.AuthOkEvent;
import com.gabriele.redding.reddit.events.HomeEvent;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.models.Submission;

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
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<Submission> homeSubs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((ReddingApp)getApplicationContext()).getRedditComponent().inject(this);

        activityRef = system.actorOf(ActivityActor.class,
                new Props(this).withDispatcher(MainThreadDispatcher.getInstance()));

        system.getEventBus().subscribe(AuthOkEvent.class, activityRef);

        mRecyclerView = (RecyclerView) findViewById(R.id.submissions_list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SubmissionsAdapter(homeSubs);
        mRecyclerView.setAdapter(mAdapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        if (!hasAuth())
            startActivity(new Intent(this, LoginActivity.class));

        redditActor.tell(new GetHomeCmd(), activityRef);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Log.d(LOG_TAG, "AuthenticationState for onResume(): " + state);

        switch (state) {
            case READY:
                break;
            case NONE:
                break;
            case NEED_REFRESH:
                redditActor.tell(new RefreshTokenCmd(), activityRef);
                break;
        }
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof HomeEvent) {
            List<Submission> home = ((HomeEvent) o).getHome();
            homeSubs.addAll(home);
            mAdapter.notifyDataSetChanged();

        } else if (o instanceof Submission) {
            homeSubs.add((Submission) o);
            mAdapter.notifyDataSetChanged();

        } else if (o instanceof AuthOkEvent) {
            redditActor.tell(new GetHomeCmd(), activityRef);
        }
    }

    private boolean hasAuth() {
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        return state != AuthenticationState.NONE;
    }
}
