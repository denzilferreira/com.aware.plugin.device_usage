package com.aware.plugin.device_usage;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.plugin.device_usage.Provider.DeviceUsage_Data;
import com.aware.utils.Converters;
import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Calendar;

public class ContextCard implements IContextCard {

	public ContextCard(){}

	public View getContextCard( Context context ) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View card = inflater.inflate(R.layout.layout, null);
        LinearLayout chart = (LinearLayout) card.findViewById(R.id.chart_container);

        //Get today's time from the beginning in milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

		TextView just_off = (TextView) card.findViewById(R.id.device_off);
        TextView average_off = (TextView) card.findViewById(R.id.average_unused);

        String[] columns = new String[]{"AVG(" + DeviceUsage_Data.ELAPSED_DEVICE_OFF + ") as average"};
        Cursor avg_off = context.getContentResolver().query(DeviceUsage_Data.CONTENT_URI, columns, DeviceUsage_Data.ELAPSED_DEVICE_OFF + " > 0", null, null);
        if( avg_off != null && avg_off.moveToFirst()) {
            double average_unused = avg_off.getDouble(0);
            average_off.setText( "Average: " + Converters.readable_elapsed((long) average_unused));
        }
        if( avg_off != null && ! avg_off.isClosed() ) avg_off.close();

		Cursor off = context.getContentResolver().query(DeviceUsage_Data.CONTENT_URI, null, DeviceUsage_Data.ELAPSED_DEVICE_OFF + " > 0 AND " + DeviceUsage_Data.TIMESTAMP + " >= " + c.getTimeInMillis(), null, DeviceUsage_Data.TIMESTAMP + " DESC LIMIT 1");
        if( off != null && off.moveToFirst() ) {
        	double phone_off = off.getDouble(off.getColumnIndex(DeviceUsage_Data.ELAPSED_DEVICE_OFF));
            just_off.setText( Converters.readable_elapsed( (long) phone_off ) );
        }
        if( off != null && ! off.isClosed() ) off.close();

		chart.removeAllViews();
		chart.addView(drawGraph(context));
		chart.invalidate();

        return card;
	}

	private BarChart drawGraph( Context context ) {

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
		Cursor off_times = context.getContentResolver().query(DeviceUsage_Data.CONTENT_URI, new String[]{ "count(*) as frequency","strftime('%H',"+ DeviceUsage_Data.TIMESTAMP + "/1000, 'unixepoch', 'localtime')+0 as time_of_day" }, DeviceUsage_Data.ELAPSED_DEVICE_ON + " > 0 AND " + DeviceUsage_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ", null, "time_of_day ASC");
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

		BarChart mChart = new BarChart(context);
        mChart.setContentDescription("");
        mChart.setDescription("");
		mChart.setMinimumHeight(200);
		mChart.setBackgroundColor(Color.WHITE);
		mChart.setDrawGridBackground(false);
		mChart.setDrawBorders(false);

		YAxis left = mChart.getAxisLeft();
		left.setDrawLabels(true);
		left.setDrawGridLines(true);
		left.setDrawAxisLine(true);

		YAxis right = mChart.getAxisRight();
		right.setDrawAxisLine(false);
		right.setDrawLabels(false);
		right.setDrawGridLines(false);

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
