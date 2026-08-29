package com.rhdevs.rhpatch.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.Slider;
import com.rhdevs.rhpatch.R;

public class FloatSeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private float minValue;
    private float maxValue;
    private float valueSpacing;
    private String format;

    private SeekBar seekbar;
    private Slider slider;
    private TextView textView;

    private float defaultValue = 0F;
    private float newValue = 0F;

    public FloatSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public FloatSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FloatSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.preference.R.attr.seekBarPreferenceStyle);
    }

    public FloatSeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        defaultValue = ta.getFloat(index, 0F);
        return defaultValue;
    }

        @Override
    protected void onSetInitialValue(Object defaultValue) {
        float defaultVal = (defaultValue instanceof Float) ? (Float) defaultValue : this.defaultValue;
        try {
            newValue = getPersistedFloat(defaultVal);
        } catch (ClassCastException e) {
            // Get the old integer value
            int oldInt = getPersistedInt((int) defaultVal);
            newValue = (float) oldInt;
            // Overwrite directly into SharedPreferences to bypass persistFloat's type checking
            getPreferenceManager().getSharedPreferences().edit().putFloat(getKey(), newValue).apply();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.itemView.setClickable(false);
        View view = holder.findViewById(R.id.seekbar);
        textView = (TextView) holder.findViewById(R.id.seekbar_value);

        if (view instanceof Slider) {
            slider = (Slider) view;
            slider.setValueFrom(minValue);
            slider.setValueTo(maxValue);
            slider.setStepSize(valueSpacing > 0 ? valueSpacing : 1F);
            float safeVal = Math.max(minValue, Math.min(maxValue, newValue));
            slider.setValue(safeVal);
            slider.setEnabled(isEnabled());
            slider.clearOnChangeListeners();
            slider.addOnChangeListener((slider1, value, fromUser) -> {
                if (textView != null) {
                    textView.setText(String.format(format, value));
                }
                if (fromUser) {
                    persistFloat(value);
                    newValue = value;
                }
            });
        } else if (view instanceof SeekBar) {
            seekbar = (SeekBar) view;
            seekbar.setOnSeekBarChangeListener(this);
            seekbar.setMax((int) ((maxValue - minValue) / valueSpacing));
            seekbar.setProgress((int) ((newValue - minValue) / valueSpacing));
            seekbar.setEnabled(isEnabled());
        }

        if (textView != null) {
            textView.setText(String.format(format, newValue));
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        float v = minValue + progress * valueSpacing;
        if (textView != null) {
            textView.setText(String.format(format, v));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Not used
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        float v = minValue + seekBar.getProgress() * valueSpacing;
        persistFloat(v);
        newValue = v;
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setWidgetLayoutResource(R.layout.pref_float_seekbar);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FloatSeekBarPreference, defStyleAttr, defStyleRes);
        minValue = ta.getFloat(R.styleable.FloatSeekBarPreference_minValue, 0F);
        maxValue = ta.getFloat(R.styleable.FloatSeekBarPreference_maxValue, 1F);
        valueSpacing = ta.getFloat(R.styleable.FloatSeekBarPreference_valueSpacing, .1F);
        format = ta.getString(R.styleable.FloatSeekBarPreference_format);
        if (format == null) {
            format = "%3.1f";
        }
        ta.recycle();
    }

    public float getFloatValue() {
        if (slider != null) {
            return slider.getValue();
        }
        return (seekbar != null) ? (seekbar.getProgress() * valueSpacing) + minValue : newValue;
    }
}

