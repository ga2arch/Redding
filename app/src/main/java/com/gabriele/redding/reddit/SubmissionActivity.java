package com.gabriele.redding.reddit;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;

import com.gabriele.actor.android.ActivityActor;
import com.gabriele.actor.dispatchers.MainThreadDispatcher;
import com.gabriele.actor.interfaces.WithReceive;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.actor.internals.ActorSystem;
import com.gabriele.actor.internals.Props;
import com.gabriele.redding.R;
import com.gabriele.redding.ReddingApp;
import com.gabriele.redding.controls.CommentsAdapter;
import com.gabriele.redding.internals.Holder;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import javax.inject.Inject;
import javax.inject.Named;

public class SubmissionActivity extends AppCompatActivity implements WithReceive {
    public static final String EXTRA_URL = "URL";

    @Inject
    @Named("RedditActor")
    ActorRef redditActor;
    @Inject
    ActorSystem system;

    private ActorRef activityRef;
    private CommentNode mComments;

    private SwipeRefreshLayout mRefreshLayout;
    private NavigationView mNavigationView;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProgressBar mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission);
        ((ReddingApp)getApplicationContext()).getRedditComponent().inject(this);

        mComments = ((Submission) Holder.object).getComments();
        activityRef = system.actorOf(ActivityActor.class,
                new Props(this).withDispatcher(MainThreadDispatcher.getInstance()));

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.submissions_refresh);
        mSpinner = (ProgressBar) findViewById(R.id.spinner);
        mRecyclerView = (RecyclerView) findViewById(R.id.comments_list);
        assert mRecyclerView != null;
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CommentsAdapter(this, mComments);
        mRecyclerView.setAdapter(mAdapter);


    }

    @Override
    public void onReceive(Object o) throws Exception {

    }
}
