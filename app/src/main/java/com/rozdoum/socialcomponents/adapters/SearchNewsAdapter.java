package com.rozdoum.socialcomponents.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.adapters.holders.NewsViewHolder;
import com.rozdoum.socialcomponents.controllers.LikeNewsController;
import com.rozdoum.socialcomponents.main.base.BaseActivity;
import com.rozdoum.socialcomponents.model.News;

import java.util.List;

public class SearchNewsAdapter extends BaseNewsAdapter {
    public static final String TAG = SearchNewsAdapter.class.getSimpleName();

    private SearchNewsAdapter.CallBack callBack;

    public SearchNewsAdapter(final BaseActivity activity) {
        super(activity);
    }

    public void setCallBack(SearchNewsAdapter.CallBack callBack) {
        this.callBack = callBack;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.news_item_list_view, parent, false);

        return new NewsViewHolder(view, createOnClickListener(), activity, true);
    }

    private NewsViewHolder.OnClickListener createOnClickListener() {
        return new NewsViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    selectedNewsPosition = position;
                    callBack.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeNewsController likeController, int position) {
                if (callBack != null && callBack.enableClick()) {
                    News news = getItemByPosition(position);
                    likeController.handleLikeClickAction(activity, news);
                }
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callBack != null && callBack.enableClick()) {
                    callBack.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((NewsViewHolder) holder).bindData(newsList.get(position));
    }

    public void setList(List<News> list) {
        cleanSelectedNewsInformation();
        newsList.clear();
        newsList.addAll(list);
        notifyDataSetChanged();
    }

    public void removeSelectedNews() {
        if (selectedNewsPosition != RecyclerView.NO_POSITION) {
            newsList.remove(selectedNewsPosition);
            notifyItemRemoved(selectedNewsPosition);
        }
    }

    public interface CallBack {
        void onItemClick(News news, View view);
        void onAuthorClick(String authorId, View view);
        boolean enableClick();
    }
}
