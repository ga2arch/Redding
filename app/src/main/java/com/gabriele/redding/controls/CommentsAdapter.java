package com.gabriele.redding.controls;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.List;
import java.util.Locale;

import static com.gabriele.redding.SubmissionActivity.CommentWithDepth;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private Context mContext;
    private List<CommentWithDepth> mComments;
    private final AndDown mAndDown = new AndDown();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public TextView mContentView;
        public TextView mTimeView;
        public TextView mCommentsView;
        public TextView mPointsView;
        public TextView mAuthorView;

        public ViewHolder(View v) {
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
    public CommentsAdapter(Context context, List<CommentWithDepth> myDataset) {
        mComments = myDataset;
        mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                          int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_comment_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        CommentWithDepth comment = mComments.get(position);
        CommentMessage message = new CommentMessage(comment.getComment().getDataNode());
        Spanned spanned = Html.fromHtml(mAndDown.markdownToHtml(message.getBody().trim()));
        String content = spanned.toString();
        final SpannableString spannable = new SpannableString(spanned);

        for (Utils.LinkPosition pos: Utils.findLinks(content)) {
            spannable.setSpan(new CustomClickSpan(mContext, pos.getUrl()),
                    pos.getStart(),
                    pos.getEnd(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.mContentView.setText(spannable.subSequence(0, spanned.length()-2));
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

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mComments.size();
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
