package com.gabriele.redding.controls;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gabriele.redding.R;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.CommentNode;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private Context mContext;
    private CommentNode mComments;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public TextView mContentView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mContentView = (TextView) mView.findViewById(R.id.content);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public CommentsAdapter(Context context, CommentNode myDataset) {
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
        Comment comment = mComments.get(0).getChildren().get(position).getComment();
        CommentMessage message = new CommentMessage(comment.getDataNode());
        holder.mContentView.setText(message.getBody());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mComments.get(0).getChildren().size();
    }
}
