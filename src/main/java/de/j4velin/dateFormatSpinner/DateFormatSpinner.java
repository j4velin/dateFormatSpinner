package de.j4velin.dateFormatSpinner;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormatSpinner extends Spinner {

    public final static String[] DEFAULT_TIME_FORMATS = new String[]{"HH:mm", "hh:mm aa"};
    public final static String[] DEFAULT_DATE_FORMATS =
            new String[]{"MM/dd", "dd.MM.", "EEEE, d. MMM", "EEEE, d. MMMM", "EEE, d. MMMM",
                    "EEEE, MMM/dd", "EEEE, MMMM/dd", "EEE, MMMM/dd", "dd.MM.yyyy", "d. MMMM yyyy",
                    "MM/dd/yy"};

    private final String[] formats;
    private final Context c;
    private String custom;
    private OnItemSelectedListener oisl;
    private int lastPosition = -1;

    private boolean programmaticallySet = false;

    public DateFormatSpinner(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.c = context;
        TypedArray a = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.DateFormatSpinner, 0, 0);
        int defaultFormats = 0;
        try {
            defaultFormats = a.getInteger(R.styleable.DateFormatSpinner_defaultFormats, 0);
        } finally {
            a.recycle();
        }
        this.formats = defaultFormats == 0 ? DEFAULT_TIME_FORMATS : DEFAULT_DATE_FORMATS;
        init();

        // set defaults
        if (formats == DEFAULT_TIME_FORMATS) {
            setSelection(DateFormat.is24HourFormat(c) ? 0 : 1);
        } else {
            Locale setLocale = Locale.getDefault();
            if (setLocale.equals(Locale.US) || setLocale.equals(Locale.CHINESE)) {
                setSelection(0);
            } else {
                setSelection(1);
            }
        }
    }


    public DateFormatSpinner(final Context context, final String[] formats) {
        super(context);
        this.c = context;
        this.formats = formats;
        init();
    }

    private void showCustomDialog() {
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.custom_dialog);
        d.findViewById(R.id.explain).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
        final TextView preview = (TextView) d.findViewById(R.id.preview);
        final EditText text = (EditText) d.findViewById(R.id.text);
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                try {
                    preview.setText(new SimpleDateFormat(s.toString()).format(new Date()));
                } catch (Exception e) {
                    preview.setText("ERROR - Invalid format");
                }
            }

            @Override
            public void afterTextChanged(final Editable editable) {
            }
        });
        text.setText(custom);
        d.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                try {
                    new SimpleDateFormat(text.getText().toString()).format(new Date());
                    custom = text.getText().toString();
                    c.getSharedPreferences("DateFormatSettings", Context.MODE_PRIVATE).edit()
                            .putString("custom_" + getId(), custom).commit();
                    if (oisl != null) oisl.onItemSelected(null, null, formats.length, 0);
                } catch (Exception e) {
                }
                d.dismiss();
            }
        });
        d.show();
    }

    private void init() {
        custom = c.getSharedPreferences("DateFormatSettings", Context.MODE_PRIVATE)
                .getString("custom_" + getId(), formats[0]);
        setAdapter();
        super.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> adapterView, final View view, final int position, final long id) {
                if (lastPosition == position) return;
                if (programmaticallySet) {
                    programmaticallySet = false;
                } else if (position == formats.length) {
                    showCustomDialog();
                } else if (oisl != null) {
                    oisl.onItemSelected(adapterView, view, position, id);
                }
                lastPosition = position;
            }

            @Override
            public void onNothingSelected(final AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void setOnItemSelectedListener(final OnItemSelectedListener listener) {
        oisl = listener;
    }

    private void setAdapter() {
        super.setAdapter(new DateTimeFormatAdapter());
    }

    public String getValue() {
        if (getSelectedItemPosition() < formats.length) {
            return formats[getSelectedItemPosition()];
        } else {
            return custom;
        }
    }

    public void setValue(final String v) {
        programmaticallySet = true; // don't show dialog
        for (int i = 0; i < formats.length; i++) {
            if (formats[i].equals(v)) {
                setSelection(i);
                return;
            }
        }
        // no match yet? -> "custom" selected
        custom = v;
        c.getSharedPreferences("DateFormatSettings", Context.MODE_PRIVATE).edit()
                .putString("custom_" + getId(), custom).commit();
        setSelection(formats.length);
    }

    private class DateTimeFormatAdapter extends ArrayAdapter<String> {

        public DateTimeFormatAdapter() {
            super(c, android.R.layout.simple_spinner_dropdown_item);
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat();
            for (String format : formats) {
                sdf.applyPattern(format);
                add(sdf.format(now));
            }
            add("custom");
        }
    }
}
