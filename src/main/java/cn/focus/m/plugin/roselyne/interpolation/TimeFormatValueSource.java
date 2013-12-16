package cn.focus.m.plugin.roselyne.interpolation;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.plexus.interpolation.AbstractValueSource;
import org.codehaus.plexus.util.StringUtils;

public class TimeFormatValueSource extends AbstractValueSource {

    protected TimeFormatValueSource(boolean usesFeedback) {
        super(false);
    }

    public Object getValue(String expression) {
        if (StringUtils.isBlank(expression)) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(expression);
        return sdf.format(new Date());

    }

}
