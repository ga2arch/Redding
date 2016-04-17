package com.gabriele.redding;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.view.View;

import com.gabriele.actor.android.ActivityActor;
import com.gabriele.actor.dispatchers.MainThreadDispatcher;
import com.gabriele.actor.interfaces.WithReceive;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.actor.internals.ActorSystem;
import com.gabriele.actor.internals.Props;
import com.gabriele.redding.controls.SubmissionsAdapter;
import com.gabriele.redding.reddit.LoginActivity;
import com.gabriele.redding.reddit.RedditActor;

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

        mRecyclerView = (RecyclerView) findViewById(R.id.submissions_list);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SubmissionsAdapter(homeSubs);
        mRecyclerView.setAdapter(mAdapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        redditActor.tell(new RedditActor.GetHomeCmd(), activityRef);
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
                startActivityForResult(new Intent(this, LoginActivity.class), 1);
                break;
            case NEED_REFRESH:
                redditActor.tell(new RedditActor.RefreshTokenCmd(), activityRef);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                redditActor.tell(new RedditActor.GetHomeCmd(), activityRef);
            }
        }
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof RedditActor.HomeEvent) {
            List<Submission> home = ((RedditActor.HomeEvent) o).getHome();
            homeSubs.addAll(home);
            mAdapter.notifyDataSetChanged();
        }
    }
}
