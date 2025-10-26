package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

    private ArrayList<IssueModel> issues;

    public IssueAdapter(ArrayList<IssueModel> issues) {
        this.issues = issues;
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_issue, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        IssueModel issue = issues.get(position);
        holder.title.setText(issue.title);
        holder.desc.setText(issue.description);
        holder.status.setText(issue.status);
        Glide.with(holder.image.getContext()).load(issue.imageUrl).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return issues.size();
    }

    static class IssueViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc, status;

        public IssueViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.issueImage);
            title = itemView.findViewById(R.id.issueTitle);
            desc = itemView.findViewById(R.id.issueDesc);
            status = itemView.findViewById(R.id.issueStatus);
        }
    }
}
