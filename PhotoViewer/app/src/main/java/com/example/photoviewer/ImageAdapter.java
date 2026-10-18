package com.example.photoviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class ImageAdapter
        extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final List<PostItem> postList;


    public ImageAdapter(
            List<PostItem> postList
    ) {

        this.postList = postList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_post,
                                parent,
                                false
                        );


        return new ViewHolder(
                view
        );
    }


    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        PostItem post =
                postList.get(
                        position
                );


        // -----------------------------------------------------
        // 이미지
        // -----------------------------------------------------

        holder.imagePost.setImageBitmap(
                post.getBitmap()
        );


        // -----------------------------------------------------
        // 제목
        // -----------------------------------------------------

        holder.textTitle.setText(
                post.getTitle()
        );


        // -----------------------------------------------------
        // 본문
        // -----------------------------------------------------

        String content =
                post.getText();


        if (
                content == null
                        || content.trim().isEmpty()
        ) {

            holder.textContent.setVisibility(
                    View.GONE
            );

        } else {

            holder.textContent.setVisibility(
                    View.VISIBLE
            );


            holder.textContent.setText(
                    content
            );
        }


        // -----------------------------------------------------
        // 날짜
        // -----------------------------------------------------

        String date =
                formatDate(
                        post.getPublishedDate()
                );


        if (date.isEmpty()) {

            holder.textDate.setVisibility(
                    View.GONE
            );

        } else {

            holder.textDate.setVisibility(
                    View.VISIBLE
            );


            holder.textDate.setText(
                    date
            );
        }
    }


    @Override
    public int getItemCount() {

        return postList.size();
    }


    // =========================================================
    // 날짜를 보기 쉽게 단순 표시
    // =========================================================

    private String formatDate(
            String originalDate
    ) {

        if (
                originalDate == null
                        || originalDate.isEmpty()
        ) {

            return "";
        }


        /*
         * 예:
         *
         * 2026-10-16T22:30:15+09:00
         *
         * →
         *
         * 2026-10-16 22:30
         */

        try {

            if (originalDate.length() >= 16) {

                String date =
                        originalDate.substring(
                                0,
                                10
                        );


                String time =
                        originalDate.substring(
                                11,
                                16
                        );


                return date
                        + " "
                        + time;
            }

        } catch (Exception ignored) {
        }


        return originalDate;
    }


    // =========================================================
    // ViewHolder
    // =========================================================

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imagePost;

        TextView textTitle;
        TextView textContent;
        TextView textDate;


        public ViewHolder(
                @NonNull View itemView
        ) {

            super(
                    itemView
            );


            imagePost =
                    itemView.findViewById(
                            R.id.image_post
                    );


            textTitle =
                    itemView.findViewById(
                            R.id.text_post_title
                    );


            textContent =
                    itemView.findViewById(
                            R.id.text_post_content
                    );


            textDate =
                    itemView.findViewById(
                            R.id.text_post_date
                    );
        }
    }
}