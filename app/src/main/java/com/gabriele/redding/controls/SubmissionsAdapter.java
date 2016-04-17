package com.gabriele.redding.controls;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gabriele.redding.R;

import net.dean.jraw.models.Submission;

import java.util.List;

public class SubmissionsAdapter extends RecyclerView.Adapter<SubmissionsAdapter.ViewHolder> {
    private List<Submission> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public TextView mTextView;
        public ViewHolder(View v) {
            super(v);
            mView = v;
            mTextView = (TextView) mView.findViewById(R.id.submission);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SubmissionsAdapter(List<Submission> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SubmissionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.submissions_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Submission submission = mDataset.get(position);
        String txt = String.format("%s↑ /r/%s - %s\n",
                submission.getScore(),
                submission.getShortURL(),
                submission.getTitle());

        holder.mTextView.setText(txt);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
