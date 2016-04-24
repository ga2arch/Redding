package com.gabriele.redding.controls;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gabriele.actor.internals.ActorContext;
import com.gabriele.actor.internals.ActorRef;
import com.gabriele.actor.internals.Props;
import com.gabriele.redding.R;
import com.gabriele.redding.SubmissionActivity;

import net.dean.jraw.models.Submission;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.List;
import java.util.Locale;

public class SubmissionsAdapter extends RecyclerView.Adapter<SubmissionsAdapter.ViewHolder> {
    private AppCompatActivity mActivity;
    private ActorContext mActorContext;
    private ActorRef mRef;
    private List<Submission> mDataset;
    private float lastTouchX;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public TextView mTitleView;
        public TextView mTimeView;
        public TextView mCommentsView;
        public TextView mPointsView;
        public TextView mAuthorView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mTitleView = (TextView) mView.findViewById(R.id.title);
            mTimeView = (TextView) mView.findViewById(R.id.time);
            mCommentsView = (TextView) mView.findViewById(R.id.comments);
            mPointsView = (TextView) mView.findViewById(R.id.points);
            mAuthorView = (TextView) mView.findViewById(R.id.author);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SubmissionsAdapter(AppCompatActivity activity,
                              ActorRef activityRef,
                              ActorContext actorContext,
                              List<Submission> myDataset) {
        mActivity = activity;
        mActorContext = actorContext;
        mDataset = myDataset;
        mRef = activityRef;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SubmissionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_submissions_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Submission submission = mDataset.get(position);
        holder.mTitleView.setText(Html.fromHtml(submission.getTitle()));
        holder.mCommentsView.setText(String.format("%s comments", submission.getCommentCount()));
        holder.mPointsView.setText(String.format("%s upvotes", submission.getScore()));

        DateTime startTime = new DateTime(submission.getCreatedUtc());
        Period p = new Period(startTime, DateTime.now(DateTimeZone.UTC));

        long hours = p.getHours();
        long minutes = p.getMinutes();

        String time;
        if (hours > 0) {
            time = String.format(Locale.getDefault(), "%d hours %d minutes ago", hours, minutes);
        } else {
            time = String.format(Locale.getDefault(), "%d minutes ago", minutes);
        }

        holder.mTimeView.setText(time);
        holder.mAuthorView.setText(submission.getAuthor());
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int width = view.getWidth();
                double part = width * .15;
                if (lastTouchX >= width - part) {
                    ActorRef ref = mActorContext.actorOf(Props.create(mActivity, SubmissionActivity.class));
                    ref.tell(submission, mRef);

                } else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(submission.getUrl()));
                    mActivity.startActivity(browserIntent);
                }
            }
        });

        holder.mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                lastTouchX = event.getX();
                return false;
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
