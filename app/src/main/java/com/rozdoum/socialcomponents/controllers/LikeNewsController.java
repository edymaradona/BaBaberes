package com.rozdoum.socialcomponents.controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.design.widget.Snackbar;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.main.base.BaseActivity;
import com.rozdoum.socialcomponents.main.interactors.NewsInteractor;
import com.rozdoum.socialcomponents.main.main.MainActivity;
import com.rozdoum.socialcomponents.managers.NewsManager;
import com.rozdoum.socialcomponents.managers.PostManager;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectExistListener;
import com.rozdoum.socialcomponents.managers.listeners.OnPostChangedListener;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.Post;

public class LikeNewsController {

    private static final int ANIMATION_DURATION = 300;

    public enum AnimationType {
        COLOR_ANIM, BOUNCE_ANIM
    }

    private Context context;
    private String newsId;
    private String newsAuthorId;

    private LikeController.AnimationType likeAnimationType = LikeController.AnimationType.BOUNCE_ANIM;

    private TextView likeCounterTextView;
    private ImageView likesImageView;

    private boolean isListView = false;

    private boolean isLiked = false;
    private boolean updatingLikeCounter = true;

    public LikeNewsController(Context context, News news, TextView likeCounterTextView,
                          ImageView likesImageView, boolean isListView) {
        this.context = context;
        this.newsId = news.getId();
        this.newsAuthorId = news.getAuthorId();
        this.likeCounterTextView = likeCounterTextView;
        this.likesImageView = likesImageView;
        this.isListView = isListView;
    }

    public void likeClickAction(long prevValue) {
        if (!updatingLikeCounter) {
            startAnimateLikeButton(likeAnimationType);

            if (!isLiked) {
                addLike(prevValue);
            } else {
                removeLike(prevValue);
            }
        }
    }

    public void likeClickActionLocal(News news) {
        setUpdatingLikeCounter(false);
        likeClickAction(news.getLikesCount());
        updateLocalNewsLikeCounter(news);
    }

    private void addLike(long prevValue) {
        updatingLikeCounter = true;
        isLiked = true;
        likeCounterTextView.setText(String.valueOf(prevValue + 1));
        NewsInteractor.getInstance(context).createOrUpdateLike(newsId, newsAuthorId);
    }

    private void removeLike(long prevValue) {
        updatingLikeCounter = true;
        isLiked = false;
        likeCounterTextView.setText(String.valueOf(prevValue - 1));
        NewsInteractor.getInstance(context).removeLike(newsId, newsAuthorId);
    }

    private void startAnimateLikeButton(LikeController.AnimationType animationType) {
        switch (animationType) {
            case BOUNCE_ANIM:
                bounceAnimateImageView();
                break;
            case COLOR_ANIM:
                colorAnimateImageView();
                break;
        }
    }

    private void bounceAnimateImageView() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(likesImageView, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(ANIMATION_DURATION);
        bounceAnimX.setInterpolator(new BounceInterpolator());

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(likesImageView, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(ANIMATION_DURATION);
        bounceAnimY.setInterpolator(new BounceInterpolator());
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                likesImageView.setImageResource(!isLiked ? R.drawable.ic_like_active
                        : R.drawable.ic_like);
            }
        });

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });

        animatorSet.play(bounceAnimX).with(bounceAnimY);
        animatorSet.start();
    }

    private void colorAnimateImageView() {
        final int activatedColor = context.getResources().getColor(R.color.like_icon_activated);

        final ValueAnimator colorAnim = !isLiked ? ObjectAnimator.ofFloat(0f, 1f)
                : ObjectAnimator.ofFloat(1f, 0f);
        colorAnim.setDuration(ANIMATION_DURATION);
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float mul = (Float) animation.getAnimatedValue();
                int alpha = adjustAlpha(activatedColor, mul);
                likesImageView.setColorFilter(alpha, PorterDuff.Mode.SRC_ATOP);
                if (mul == 0.0) {
                    likesImageView.setColorFilter(null);
                }
            }
        });

        colorAnim.start();
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public LikeController.AnimationType getLikeAnimationType() {
        return likeAnimationType;
    }

    public void setLikeAnimationType(LikeController.AnimationType likeAnimationType) {
        this.likeAnimationType = likeAnimationType;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isUpdatingLikeCounter() {
        return updatingLikeCounter;
    }

    public void setUpdatingLikeCounter(boolean updatingLikeCounter) {
        this.updatingLikeCounter = updatingLikeCounter;
    }

    public void initLike(boolean isLiked) {
        likesImageView.setImageResource(isLiked ? R.drawable.ic_like_active : R.drawable.ic_like);
        this.isLiked = isLiked;
    }

    private void updateLocalNewsLikeCounter(News news) {
        if (isLiked) {
            news.setLikesCount(news.getLikesCount() + 1);
        } else {
            news.setLikesCount(news.getLikesCount() - 1);
        }
    }

    public void handleLikeClickAction(final BaseActivity baseActivity, final News news) {
        NewsManager.getInstance(baseActivity.getApplicationContext()).isNewsExistSingleValue(news.getId(), new OnObjectExistListener<News>() {
            @Override
            public void onDataChanged(boolean exist) {
                if (exist) {
                    if (baseActivity.hasInternetConnection()) {
                        doHandleLikeClickAction(baseActivity, news);
                    } else {
                        showWarningMessage(baseActivity, R.string.internet_connection_failed);
                    }
                } else {
                    showWarningMessage(baseActivity, R.string.message_post_was_removed);
                }
            }
        });
    }

    public void handleLikeClickAction(final BaseActivity baseActivity, final String newsId) {
        NewsManager.getInstance(baseActivity.getApplicationContext()).getSingleNewsValue(newsId, new OnNewsChangedListener() {
            @Override
            public void onObjectChanged(News news) {
                if (baseActivity.hasInternetConnection()) {
                    doHandleLikeClickAction(baseActivity, news);
                } else {
                    showWarningMessage(baseActivity, R.string.internet_connection_failed);
                }
            }

            @Override
            public void onError(String errorText) {
                baseActivity.showSnackBar(errorText);
            }
        });
    }

    private void showWarningMessage(BaseActivity baseActivity, int messageId) {
        if (baseActivity instanceof MainActivity) {
            ((MainActivity) baseActivity).showFloatButtonRelatedSnackBar(messageId);
        } else {
            baseActivity.showSnackBar(messageId);
        }
    }

    private void doHandleLikeClickAction(BaseActivity baseActivity, News news) {
        if (baseActivity.checkAuthorization()) {
            if (isListView) {
                likeClickActionLocal(news);
            } else {
                likeClickAction(news.getLikesCount());
            }
        }
    }

    public void changeAnimationType() {
        if (getLikeAnimationType() == LikeController.AnimationType.BOUNCE_ANIM) {
            setLikeAnimationType(LikeController.AnimationType.COLOR_ANIM);
        } else {
            setLikeAnimationType(LikeController.AnimationType.BOUNCE_ANIM);
        }

        Snackbar snackbar = Snackbar
                .make(likesImageView, "Animation was changed", Snackbar.LENGTH_LONG);

        snackbar.show();
    }
}