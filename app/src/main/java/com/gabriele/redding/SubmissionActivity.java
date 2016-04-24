package com.gabriele.redding;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.gabriele.actor.android.AppCompatActivityAsActor;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.redding.controls.CommentsAdapter;
import com.gabriele.redding.reddit.cmds.GetFullSubmissionCmd;
import com.gabriele.redding.reddit.cmds.StopRequestsCmd;
import com.gabriele.redding.reddit.events.FullSubmissionEvent;
import com.google.common.base.Function;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.TraversalMethod;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class SubmissionActivity extends AppCompatActivityAsActor {

    @Inject
    @Named("RedditActor")
    ActorRef redditActor;

    private SwipeRefreshLayout mRefreshLayout;
    private NavigationView mNavigationView;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProgressBar mSpinner;
    private Submission submission;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission);
        ((ReddingApp)getApplicationContext()).getRedditComponent().inject(this);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.submissions_refresh);
        mSpinner = (ProgressBar) findViewById(R.id.spinner);
        mRecyclerView = (RecyclerView) findViewById(R.id.comments_list);
        assert mRecyclerView != null;
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                redditActor.tell(new GetFullSubmissionCmd(submission.getId()), getSelf());
            }
        });
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof Submission) {
            redditActor.tell(new GetFullSubmissionCmd(((Submission) o).getId()), getSelf());

        } else if (o instanceof FullSubmissionEvent) {
            mRefreshLayout.setRefreshing(false);
            mSpinner.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);

            submission = ((FullSubmissionEvent) o).getSubmission();

            List<CommentWithDepth> comments = dfs(submission.getComments());
            mAdapter = new CommentsAdapter(this, comments);
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        redditActor.tell(new StopRequestsCmd(), getSelf());
    }

    private List<CommentWithDepth> dfs(CommentNode start) {
        return start.walkTree(TraversalMethod.PRE_ORDER)
                .transform(new Function<CommentNode, CommentWithDepth>() {
                    @Override
                    public CommentWithDepth apply(CommentNode input) {
                        return new CommentWithDepth(input.getComment(), input.getDepth());
                    }
                }).toList();
    }

    public static class CommentWithDepth {
        private Comment comment;
        private int depth;

        public CommentWithDepth(Comment comment, int depth) {
            this.comment = comment;
            this.depth = depth;
        }

        public Comment getComment() {
            return comment;
        }

        public int getDepth() {
            return depth;
        }
    }
}
