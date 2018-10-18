package com.rozdoum.socialcomponents.managers.listeners;

import com.rozdoum.socialcomponents.model.News;

public interface OnNewsChangedListener {

    public void onObjectChanged(News obj);

    public void onError(String errorText);
}
