package de.heoegbr.bgproxy.ui;

import com.jjoe64.graphview.DefaultLabelFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Stolen from Android APS :)
 */
public class TimeAsXAxisLabelFormatter extends DefaultLabelFormatter {

    protected final String mFormat;

    public TimeAsXAxisLabelFormatter(String format) {
        mFormat = format;
    }

    @Override
    public String formatLabel(double value, boolean isValueX) {
        if (isValueX) {
            // format as date
            DateFormat dateFormat = new SimpleDateFormat(mFormat);
            return dateFormat.format((long) value);
        } else {
            return super.formatLabel(value, isValueX);
        }
    }
}
