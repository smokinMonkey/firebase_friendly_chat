package com.google.firebase.udacity.friendlychat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * Created by smokinMonkey on 9/2/2017.
 */

public class MessageHolder extends RecyclerView.ViewHolder {

    private final TextView tvName;
    private final TextView tvMessage;
    private final ImageView ivPhoto;

    public MessageHolder(View itemView) {
        super(itemView);

        tvName = (TextView) itemView.findViewById(R.id.nameTextView);
        tvMessage = (TextView) itemView.findViewById(R.id.messageTextView);
        ivPhoto = (ImageView) itemView.findViewById(R.id.photoImageView);
    }

    public void setName(String name) {
        tvName.setText(name);
    }

    public void setMessage(String msg) {
        tvMessage.setText(msg);
    }

    public void setPhoto(String photoUrl) {
        tvMessage.setVisibility(View.GONE);
        ivPhoto.setVisibility(View.VISIBLE);
        Glide.with(ivPhoto.getContext())
                .load(photoUrl)
                .into(ivPhoto);
    }
}
