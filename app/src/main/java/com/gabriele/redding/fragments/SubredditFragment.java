package com.gabriele.redding.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.gabriele.actor.android.FragmentAsActor;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.actor.internals.Props;
import com.gabriele.redding.R;
import com.gabriele.redding.ReddingApp;
import com.gabriele.redding.SubmissionActivity;
import com.gabriele.redding.controls.SubmissionsAdapter;
import com.gabriele.redding.reddit.cmds.GetSubredditCmd;
import com.gabriele.redding.reddit.events.SubredditEvent;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SubredditFragment extends FragmentAsActor {

    @Inject @Named("RedditActor") ActorRef redditActor;

    @Bind(R.id.refreshLayout) SwipeRefreshLayout mRefreshLayout;
    @Bind(R.id.submissions_list) RecyclerView mRecyclerView;
    @Bind(R.id.spinner) ProgressBar mSpinner;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<Submission> mSubs = new ArrayList<>();
    private Subreddit currentSubreddit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ReddingApp) getActivity().getApplicationContext()).getRedditComponent().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.subreddit_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SubmissionsAdapter(getSelf(), mSubs);
        mRecyclerView.setAdapter(mAdapter);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                redditActor.tell(new GetSubredditCmd(currentSubreddit), getSelf());
            }
        });
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof SubredditEvent) {
            onSubredditEvent((SubredditEvent) o);

        } else if (o instanceof GetSubredditCmd) {
            onGetSubredditCmd((GetSubredditCmd) o);

        } else if (o instanceof SubmissionsAdapter.OpenSubmissionCmd) {
            onOpenSubmissionCmd((SubmissionsAdapter.OpenSubmissionCmd) o);

        } else if (o instanceof SubmissionsAdapter.OpenUrlCmd) {
            onOpenUrlCmd((SubmissionsAdapter.OpenUrlCmd) o);
        }
    }

    private void onGetSubredditCmd(GetSubredditCmd cmd) {
        // Home to Subreddit
        if (currentSubreddit == null && cmd.getSubreddit() != null) {
            showSpinner();

        // Subreddit to different Subreddit
        } else if (currentSubreddit != null
                && cmd.getSubreddit() != null
                && !currentSubreddit.getDisplayName().equals(cmd.getSubreddit().getDisplayName())) {
            showSpinner();

        // Subreddit to Home
        } else if (currentSubreddit != null && cmd.getSubreddit() == null) {
            showSpinner();
        }

        currentSubreddit = cmd.getSubreddit();
        if (currentSubreddit != null) {
            getActivity().setTitle(currentSubreddit.getTitle());
        } else {
            getActivity().setTitle("Redding");
        }

        redditActor.tell(cmd, getSelf());
    }

    private void onSubredditEvent(SubredditEvent event) {
        hideSpinner();

        List<Submission> submissions = event.getSubmissions();
        mSubs.clear();
        mSubs.addAll(submissions);
        mAdapter.notifyDataSetChanged();
    }

    private void onOpenSubmissionCmd(SubmissionsAdapter.OpenSubmissionCmd cmd) {
        ActorRef ref = getActorContext().actorOf(
                Props.create((AppCompatActivity) getActivity(),
                SubmissionActivity.class));

        ref.tell(cmd.getSubmission(), getSelf());
    }

    private void onOpenUrlCmd(SubmissionsAdapter.OpenUrlCmd cmd) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cmd.getUrl()));
        startActivity(browserIntent);
    }


    private void showSpinner() {
        mSpinner.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    private void hideSpinner() {
        mRefreshLayout.setRefreshing(false);
        mSpinner.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }
}
