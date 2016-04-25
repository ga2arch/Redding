package com.gabriele.redding.controls;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.commonsware.cwac.anddown.AndDown;
import com.gabriele.redding.R;
import com.gabriele.redding.utils.Utils;

import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Submission;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.List;
import java.util.Locale;

import static com.gabriele.redding.SubmissionActivity.CommentWithDepth;

public class CommentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COMMENT = 1;

    private Context mContext;
    private Submission mSubmission;
    private List<CommentWithDepth> mComments;
    private final AndDown mAndDown = new AndDown();

    protected static class HeaderViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public TextView mTitleView;
        public TextView mInfoView;
        public TextView mTextView;
        public TextView mSubredditView;

        public HeaderViewHolder(View v) {
            super(v);
            mView = v;
            mTitleView = (TextView) mView.findViewById(R.id.title);
            mInfoView = (TextView) mView.findViewById(R.id.info);
            mTextView = (TextView) mView.findViewById(R.id.text);
            mSubredditView = (TextView) mView.findViewById(R.id.subreddit);
        }
    }

    protected static class CommentViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public TextView mContentView;
        public TextView mTimeView;
        public TextView mCommentsView;
        public TextView mPointsView;
        public TextView mAuthorView;

        public CommentViewHolder(View v) {
            super(v);
            mView = v;
            mContentView = (TextView) mView.findViewById(R.id.content);
            mTimeView = (TextView) mView.findViewById(R.id.time);
            mCommentsView = (TextView) mView.findViewById(R.id.comments);
            mPointsView = (TextView) mView.findViewById(R.id.points);
            mAuthorView = (TextView) mView.findViewById(R.id.author);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public CommentsAdapter(Context context, Submission submission, List<CommentWithDepth> data) {
        mSubmission = submission;
        mComments = data;
        mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEADER: {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.header_submission_view, parent, false);
                return new HeaderViewHolder(v);
            }
            case TYPE_COMMENT: {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_comment_view, parent, false);
                return new CommentViewHolder(v);
            }
        }

        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder h, int position) {
        switch (getItemViewType(position)) {
            case TYPE_HEADER: {
                HeaderViewHolder holder = ((HeaderViewHolder) h);
                holder.mTitleView.setText(mSubmission.getTitle());
                holder.mInfoView.setText(Html.fromHtml(String.format("<b>%s</b> points from <b>%s</b>",
                        mSubmission.getScore(),
                        mSubmission.getAuthor())));
                holder.mSubredditView.setText(mSubmission.getSubredditName());

                String self = mSubmission.getSelftext();
                if (!self.isEmpty()) {
                    Spannable text = linkify(Html.fromHtml(mAndDown.markdownToHtml(self)));
                    holder.mTextView.setText(text.subSequence(0, text.length()-2));
                } else {
                    holder.mTextView.setVisibility(View.GONE);
                }

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mSubmission.getUrl()));
                        mContext.startActivity(browserIntent);
                    }
                });

                break;
            }
            case TYPE_COMMENT: {
                CommentViewHolder holder = (CommentViewHolder) h;
                CommentWithDepth comment = mComments.get(position-1);
                CommentMessage message = new CommentMessage(comment.getComment().getDataNode());
                Spanned spanned = Html.fromHtml(mAndDown.markdownToHtml(message.getBody().trim()));

                Spannable content = linkify(spanned);
                holder.mContentView.setText(content.subSequence(0, spanned.length()-2));
                holder.mContentView.setMovementMethod(LinkMovementMethod.getInstance());

                holder.mAuthorView.setText(message.getAuthor());
                holder.mPointsView.setText(String.format("%s upvotes", comment.getComment().getScore()));

                DateTime startTime = new DateTime(comment.getComment().getCreatedUtc());
                Period p = new Period(startTime, DateTime.now(DateTimeZone.UTC));

                long hours = p.getHours();
                long minutes = p.getMinutes();

                holder.mTimeView.setText(String.format(Locale.getDefault(), "%d hours %d minutes ago", hours, minutes));
                int padding = Utils.spToPixels(mContext, 10);
                int px = Utils.spToPixels(mContext, (comment.getDepth()-1)*10);
                holder.mView.setPadding(padding + px,
                        0,
                        Utils.spToPixels(mContext, 10),
                        0);
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mComments.size() == 0)
            return 1;
        else
            return mComments.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        } else {
            return TYPE_COMMENT;
        }
    }

    private Spannable linkify(Spanned spanned) {
        String content = spanned.toString();
        final SpannableString spannable = new SpannableString(spanned);

        for (Utils.LinkPosition pos: Utils.findLinks(content)) {
            spannable.setSpan(new CustomClickSpan(mContext, pos.getUrl()),
                    pos.getStart(),
                    pos.getEnd(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    public static class CustomClickSpan extends ClickableSpan {
        private Context context;
        private String url;

        public CustomClickSpan(Context context, String url) {
            super();
            this.context = context;
            this.url = url;
        }

        @Override
        public void onClick(View view) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);
        }
    }
}
