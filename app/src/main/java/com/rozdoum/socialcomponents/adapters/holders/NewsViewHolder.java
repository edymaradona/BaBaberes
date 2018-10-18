package com.rozdoum.socialcomponents.adapters.holders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rozdoum.socialcomponents.Constants;
import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.controllers.LikeController;
import com.rozdoum.socialcomponents.controllers.LikeNewsController;
import com.rozdoum.socialcomponents.main.base.BaseActivity;
import com.rozdoum.socialcomponents.managers.NewsManager;
import com.rozdoum.socialcomponents.managers.PostManager;
import com.rozdoum.socialcomponents.managers.ProfileManager;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectChangedListenerSimple;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectExistListener;
import com.rozdoum.socialcomponents.model.Like;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.model.Profile;
import com.rozdoum.socialcomponents.utils.FormatterUtil;
import com.rozdoum.socialcomponents.utils.GlideApp;
import com.rozdoum.socialcomponents.utils.ImageUtil;
import com.rozdoum.socialcomponents.utils.Utils;

public class NewsViewHolder extends RecyclerView.ViewHolder {
    public static final String TAG = NewsViewHolder.class.getSimpleName();

    protected Context context;
    private ImageView postImageView;
    private TextView titleTextView;
    private TextView alamatAduan;
    private TextView detailsTextView;
    private TextView likeCounterTextView;
    private ImageView likesImageView;
    private TextView commentsCountTextView;
    private TextView watcherCounterTextView;
    private TextView dateTextView;
    private ImageView authorImageView;
    private TextView authorUserName;
    private ViewGroup likeViewGroup;

    private ProfileManager profileManager;
    protected NewsManager newsManager;

    private LikeNewsController likeController;
    private BaseActivity baseActivity;

    public NewsViewHolder(View view, final NewsViewHolder.OnClickListener onClickListener, BaseActivity activity) {
        this(view, onClickListener, activity, true);
    }

    public NewsViewHolder(View view, final NewsViewHolder.OnClickListener onClickListener, BaseActivity activity, boolean isAuthorNeeded) {
        super(view);
        this.context = view.getContext();
        this.baseActivity = activity;

        postImageView = view.findViewById(R.id.postImageView);
        likeCounterTextView = view.findViewById(R.id.likeCounterTextView);
        likesImageView = view.findViewById(R.id.likesImageView);
        commentsCountTextView = view.findViewById(R.id.commentsCountTextView);
        watcherCounterTextView = view.findViewById(R.id.watcherCounterTextView);
        dateTextView = view.findViewById(R.id.dateTextView);
        titleTextView = view.findViewById(R.id.titleTextView);
        detailsTextView = view.findViewById(R.id.detailsTextView);
        authorImageView = view.findViewById(R.id.authorImageView);
        authorUserName = view.findViewById(R.id.tvAuthor);
        likeViewGroup = view.findViewById(R.id.likesContainer);

        authorImageView.setVisibility(isAuthorNeeded ? View.VISIBLE : View.GONE);
        authorUserName.setVisibility(isAuthorNeeded ? View.VISIBLE : View.GONE);

        profileManager = ProfileManager.getInstance(context.getApplicationContext());
        newsManager = NewsManager.getInstance(context.getApplicationContext());

        view.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                onClickListener.onItemClick(getAdapterPosition(), v);
            }
        });

        likeViewGroup.setOnClickListener(view1 -> {
            int position = getAdapterPosition();
            if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                onClickListener.onLikeClick(likeController, position);
            }
        });

        authorImageView.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                onClickListener.onAuthorClick(getAdapterPosition(), v);
            }
        });
    }

    public void bindData(News news) {

        likeController = new LikeNewsController(context, news, likeCounterTextView, likesImageView, true);

        String title = removeNewLinesDividers(news.getTitle());
        titleTextView.setText(title);
        String description = removeNewLinesDividers(news.getDescription());
        detailsTextView.setText(description);
        likeCounterTextView.setText(String.valueOf(news.getLikesCount()));
        commentsCountTextView.setText(String.valueOf(news.getCommentsCount()));
        watcherCounterTextView.setText(String.valueOf(news.getWatchersCount()));

        CharSequence date = FormatterUtil.getRelativeTimeSpanStringShort(context, news.getCreatedDate());
        dateTextView.setText(date);

        String imageUrl = news.getImagePath();
        int width = Utils.getDisplayWidth(context);
        int height = (int) context.getResources().getDimension(R.dimen.post_detail_image_height);

        // Displayed and saved to cache image, as needs for post detail.
        ImageUtil.loadImageCenterCrop(GlideApp.with(baseActivity), imageUrl, postImageView, width, height);

        if (news.getAuthorId() != null) {
            profileManager.getProfileAdminSingleValue(news.getAuthorId(), createProfileChangeListener(authorUserName,authorImageView));
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            newsManager.hasCurrentUserLikeSingleValue(news.getId(), firebaseUser.getUid(), createOnLikeObjectExistListener());
        }
    }

    private String removeNewLinesDividers(String text) {
        int decoratedTextLength = text.length() < Constants.News.MAX_NEWS_TEXT_LENGTH_IN_LIST ?
                text.length() : Constants.News.MAX_NEWS_TEXT_LENGTH_IN_LIST;
        return text.substring(0, decoratedTextLength).replaceAll("\n", " ").trim();
    }

    private OnObjectChangedListener<Profile> createProfileChangeListener(final TextView authorUserName,
                                                                         final ImageView authorImageView) {
        return new OnObjectChangedListenerSimple<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                String userName = obj.getUsername();
                authorUserName.setText(userName);
                if (obj.getPhotoUrl() != null) {
                    if (!baseActivity.isFinishing() && !baseActivity.isDestroyed()) {
                        ImageUtil.loadImage(GlideApp.with(baseActivity), obj.getPhotoUrl(), authorImageView);
                    }
                }
            }
        };
    }

    private OnObjectExistListener<Like> createOnLikeObjectExistListener() {
        return exist -> likeController.initLike(exist);
    }

    public interface OnClickListener {
        void onItemClick(int position, View view);

        void onLikeClick(LikeNewsController likeController, int position);

        void onAuthorClick(int position, View view);
    }
}