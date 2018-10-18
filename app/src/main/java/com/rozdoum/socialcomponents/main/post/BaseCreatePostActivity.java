/*
 * Copyright 2018 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.rozdoum.socialcomponents.main.post;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.main.pickImageBase.PickImageActivity;
import com.rozdoum.socialcomponents.managers.PostManager;
import com.rozdoum.socialcomponents.utils.AppLocationService;
import com.rozdoum.socialcomponents.utils.LocationAddress;

/**
 * Created by Alexey on 03.05.18.
 */
public abstract class BaseCreatePostActivity<V extends BaseCreatePostView, P extends BaseCreatePostPresenter<V>>
        extends PickImageActivity<V, P> implements BaseCreatePostView {

    protected ImageView imageView;
    protected ImageView buttonAddress;
    protected ProgressBar progressBar;
    protected EditText titleEditText;
    protected EditText descriptionEditText;
    protected EditText alamatEditText;
    protected EditText statusEditText;
    protected EditText longitudeEditText;
    protected EditText latitudeEditText;

    AppLocationService appLocationService;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_create_post_activity);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        appLocationService = new AppLocationService(
                BaseCreatePostActivity.this);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        alamatEditText = findViewById(R.id.alamatEditText);
        statusEditText = findViewById(R.id.statusEditText);
        longitudeEditText = findViewById(R.id.txtLongitude);
        latitudeEditText = findViewById(R.id.txtLatitude);
        progressBar = findViewById(R.id.progressBar);

        imageView = findViewById(R.id.imageView);

        imageView.setOnClickListener(v -> onSelectImageClick(v));

        titleEditText.setOnTouchListener((v, event) -> {
            if (titleEditText.hasFocus() && titleEditText.getError() != null) {
                titleEditText.setError(null);
                return true;
            }

            return false;
        });

        buttonAddress = findViewById(R.id.buttonAddress);
        buttonAddress.setOnClickListener(view -> {
            Location gpsLocation = appLocationService
                    .getLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null) {
                double latitude = gpsLocation.getLatitude();
                double longitude = gpsLocation.getLongitude();
                String resultLong = ""+gpsLocation.getLongitude();
                String resultLang = ""+gpsLocation.getLatitude();

                LocationAddress locationAddress = new LocationAddress();
                locationAddress.getAddressFromLocation(latitude, longitude,
                        getApplicationContext(), new GeocoderHandler());

                longitudeEditText.setText(resultLong);
                latitudeEditText.setText(resultLang);
            } else {
                showSettingsAlert();
            }
        });
    }

    @Override
    protected ProgressBar getProgressView() {
        return progressBar;
    }

    @Override
    protected ImageView getImageView() {
        return imageView;
    }

    @Override
    protected void onImagePikedAction() {
        loadImageToImageView(imageUri);
    }

    @Override
    public void setDescriptionError(String error) {
        descriptionEditText.setError(error);
        descriptionEditText.requestFocus();
    }

    @Override
    public void setTitleError(String error) {
        titleEditText.setError(error);
        titleEditText.requestFocus();
    }

    @Override
    public void setAlamatError(String error) {
        alamatEditText.setError(error);
        alamatEditText.requestFocus();
    }


    @Override
    public String getTitleText() {
        return titleEditText.getText().toString();
    }

    @Override
    public String getDescriptionText() {
        return descriptionEditText.getText().toString();
    }

    @Override
    public String getAlamatText() {
        return alamatEditText.getText().toString();
    }

    @Override
    public String getStatusText() {
        return statusEditText.getText().toString();
    }

    @Override
    public Double getLongitudeText() {
        return Double.parseDouble(longitudeEditText.getText().toString());
    }

    @Override
    public Double getLatitudeText() {
        return Double.parseDouble(latitudeEditText.getText().toString());
    }

    @Override
    public void requestImageViewFocus() {
        imageView.requestFocus();
    }

    @Override
    public void onPostSavedSuccess() {
        setResult(RESULT_OK);
        this.finish();
    }

    @Override
    public Uri getImageUri() {
        return imageUri;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                BaseCreatePostActivity.this);
        alertDialog.setTitle("SETTINGS");
        alertDialog.setMessage("Enable Location Provider! Go to settings menu?");
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        BaseCreatePostActivity.this.startActivity(intent);
                    }
                });
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            alamatEditText.setText(locationAddress);
        }
    }
}
