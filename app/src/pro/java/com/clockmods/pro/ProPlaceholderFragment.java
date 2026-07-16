package com.clockmods.pro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clockmods.R;

public final class ProPlaceholderFragment extends Fragment {
    private static final String ARG_TITLE = "title";

    static ProPlaceholderFragment newInstance(int titleRes) {
        ProPlaceholderFragment fragment = new ProPlaceholderFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_TITLE, titleRes);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pro_placeholder, container, false);
        int titleRes = requireArguments().getInt(ARG_TITLE);
        ((TextView) view.findViewById(R.id.pro_page_title)).setText(titleRes);
        return view;
    }
}