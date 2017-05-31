/*
 * Copyright (C) 2017 Chris Tarazi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.chris.randomrestaurantgenerator.fragments;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chris.randomrestaurantgenerator.BuildConfig;
import com.chris.randomrestaurantgenerator.R;

import org.sufficientlysecure.htmltextview.HtmlTextView;

public class AboutFragment extends Fragment {

    RelativeLayout rootLayout;
    ImageView icon;
    TextView appVersion;
    TextView githubLink;
    HtmlTextView htmlAbout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        rootLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_about, container, false);

        icon = (ImageView) rootLayout.findViewById(R.id.appIcon);
        try {
            icon.setImageDrawable(getActivity().getPackageManager().getApplicationIcon("com.chris.randomrestaurantgenerator"));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        appVersion = (TextView) rootLayout.findViewById(R.id.appVersion);
        appVersion.setText(String.format("Version: %s", BuildConfig.VERSION_NAME));

        githubLink = (TextView) rootLayout.findViewById(R.id.githubLink);
        githubLink.setMovementMethod(LinkMovementMethod.getInstance());

        String htmlLink = "<a href='https://github.com/christarazi/random-restaurant-generator'> Source code</a>";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            githubLink.setText(Html.fromHtml(htmlLink, Html.FROM_HTML_MODE_LEGACY));
        }
        else {
            githubLink.setText(Html.fromHtml(htmlLink));
        }

        htmlAbout = (HtmlTextView) rootLayout.findViewById(R.id.htmlAbout);
        htmlAbout.setHtml(R.raw.about, new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String s) {
                return null;
            }
        });

        return rootLayout;
    }
}
