package com.rozdoum.socialcomponents.adapters;

import android.support.v7.widget.RecyclerView;

import com.rozdoum.socialcomponents.main.base.BaseActivity;
import com.rozdoum.socialcomponents.managers.NewsManager;
import com.rozdoum.socialcomponents.managers.PostManager;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnPostChangedListener;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.utils.LogUtil;

import java.util.LinkedList;
import java.util.List;

public abstract class BaseNewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = BaseNewsAdapter.class.getSimpleName();

    protected List<News> newsList = new LinkedList<>();
    protected BaseActivity activity;
    protected int selectedNewsPosition = RecyclerView.NO_POSITION;

    public BaseNewsAdapter(BaseActivity activity) {
        this.activity = activity;
    }

    protected void cleanSelectedNewsInformation() {
        selectedNewsPosition = -1;
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return newsList.get(position).getItemType().getTypeCode();
    }

    protected News getItemByPosition(int position) {
        return newsList.get(position);
    }

    private OnNewsChangedListener createOnNewsChangeListener(final int newsPosition) {
        return new OnNewsChangedListener() {
            @Override
            public void onObjectChanged(News obj) {
                newsList.set(newsPosition, obj);
                notifyItemChanged(newsPosition);
            }

            @Override
            public void onError(String errorText) {
                LogUtil.logDebug(TAG, errorText);
            }
        };
    }

    public void updateSelectedNews() {
        if (selectedNewsPosition != RecyclerView.NO_POSITION) {
            News selectedNews = getItemByPosition(selectedNewsPosition);
            NewsManager.getInstance(activity).getSingleNewsValue(selectedNews.getId(), createOnNewsChangeListener(selectedNewsPosition));
        }
    }
}
