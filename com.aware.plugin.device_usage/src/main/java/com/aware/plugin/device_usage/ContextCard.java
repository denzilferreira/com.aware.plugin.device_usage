package com.aware.plugin.device_usage;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aware.utils.Converters;
import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ContextCard implements IContextCard {

	public ContextCard(){}

	public View getContextCard( Context context ) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View card = inflater.inflate(R.layout.layout, null);

        BarChart chart = (BarChart) card.findViewById(R.id.device_usage_plot);

        //Get today's time from the beginning in milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

		TextView just_off = (TextView) card.findViewById(R.id.device_usage_off);
        TextView average_off = (TextView) card.findViewById(R.id.device_usage_avg_unused);

        String[] columns = new String[]{"AVG(" + Provider.DeviceUsage_Data.ELAPSED_DEVICE_OFF + ") as average"};
        Cursor avg_off = context.getContentResolver().query(Provider.DeviceUsage_Data.CONTENT_URI, columns, Provider.DeviceUsage_Data.ELAPSED_DEVICE_OFF + " > 0", null, null);
        if( avg_off != null && avg_off.moveToFirst()) {
            double average_unused = avg_off.getDouble(0);
            average_off.setText( "Average: " + Converters.readable_elapsed((long) average_unused));
        }
        if( avg_off != null && ! avg_off.isClosed() ) avg_off.close();

		Cursor off = context.getContentResolver().query(Provider.DeviceUsage_Data.CONTENT_URI, null, Provider.DeviceUsage_Data.ELAPSED_DEVICE_OFF + " > 0 AND " + Provider.DeviceUsage_Data.TIMESTAMP + " >= " + c.getTimeInMillis(), null, Provider.DeviceUsage_Data.TIMESTAMP + " DESC LIMIT 1");
        if( off != null && off.moveToFirst() ) {
        	double phone_off = off.getDouble(off.getColumnIndex(Provider.DeviceUsage_Data.ELAPSED_DEVICE_OFF));
            just_off.setText( Converters.readable_elapsed( (long) phone_off ) );
        }
        if( off != null && ! off.isClosed() ) off.close();

		drawGraph(context, chart);

        return card;
	}

	private BarChart drawGraph( Context context, BarChart mChart ) {

		//Get today's time from the beginning in milliseconds
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		ArrayList<String> x_hours = new ArrayList<>();
		for(int i=0; i<24; i++) {
			x_hours.add(String.valueOf(i));
		}

		ArrayList<BarEntry> barEntries = new ArrayList<>();
		//add frequencies to the right hour buffer
		Cursor off_times = context.getContentResolver().query(Provider.DeviceUsage_Data.CONTENT_URI, new String[]{ "count(*) as frequency","strftime('%H',"+ Provider.DeviceUsage_Data.TIMESTAMP + "/1000, 'unixepoch', 'localtime')+0 as time_of_day" }, Provider.DeviceUsage_Data.ELAPSED_DEVICE_ON + " > 0 AND " + Provider.DeviceUsage_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ", null, "time_of_day ASC");
		if( off_times != null && off_times.moveToFirst() ) {
			do{
				barEntries.add( new BarEntry(off_times.getInt(0), off_times.getInt(1)) );
			} while( off_times.moveToNext() );
		}
		if( off_times != null && ! off_times.isClosed()) off_times.close();

		BarDataSet dataSet = new BarDataSet(barEntries, "Amount of times used");
		dataSet.setColor(Color.parseColor("#33B5E5"));
		dataSet.setDrawValues(false);

		BarData data = new BarData(x_hours, dataSet);

		mChart.setContentDescription("");
        mChart.setDescription("");

		ViewGroup.LayoutParams params = mChart.getLayoutParams();
		params.height = 300;
		mChart.setLayoutParams(params);

		mChart.setBackgroundColor(Color.WHITE);
		mChart.setDrawGridBackground(false);
		mChart.setDrawBorders(false);

		YAxis left = mChart.getAxisLeft();
		left.setDrawLabels(true);
		left.setDrawGridLines(true);
		left.setDrawAxisLine(true);
		left.setLabelCount(4, true);
		left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(),"%s", (long) value);
            }
        });

        mChart.getAxisRight().setEnabled(false);

		XAxis bottom = mChart.getXAxis();
		bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
		bottom.setSpaceBetweenLabels(0);
		bottom.setDrawGridLines(false);

		mChart.setData(data);
		mChart.invalidate();
		mChart.animateX(1000);

		return mChart;
	}
}
