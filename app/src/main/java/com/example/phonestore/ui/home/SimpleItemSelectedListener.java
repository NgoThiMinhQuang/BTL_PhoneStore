package com.example.phonestore.ui.home;

import android.view.View;
import android.widget.AdapterView;

class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

    private final Runnable onSelected;

    SimpleItemSelectedListener(Runnable onSelected) {
        this.onSelected = onSelected;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (onSelected != null) {
            onSelected.run();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
