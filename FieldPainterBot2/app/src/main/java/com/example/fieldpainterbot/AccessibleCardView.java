package com.example.fieldpainterbot;


import android.content.Context;
import android.util.AttributeSet;
import com.google.android.material.card.MaterialCardView;

public class AccessibleCardView extends MaterialCardView {
    public AccessibleCardView(Context context) {
        super(context);
    }

    public AccessibleCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibleCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        // call super so OnClickListener and accessibility are triggered
        super.performClick();
        return true;
    }
}

