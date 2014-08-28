package com.aware.plugin.device_usage;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.plugin.device_usage.Provider.DeviceUsage_Data;
import com.aware.utils.IContextCard;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Calendar;

public class ContextCard implements IContextCard {

    /**
     * Empty constructor required for Java reflection to load the card
     */
    public ContextCard(){};

	static String[] x_hours = new String[]{"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23"};
	
	public View getContextCard( Context context ) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View card = inflater.inflate(R.layout.layout, null);
		
		TextView off = (TextView) card.findViewById(R.id.device_non_usage);
		Cursor phone_usage = context.getContentResolver().query(DeviceUsage_Data.CONTENT_URI, null, DeviceUsage_Data.ELAPSED_DEVICE_OFF + " > 0", null, DeviceUsage_Data.TIMESTAMP + " DESC LIMIT 1");
        if( phone_usage != null && phone_usage.moveToFirst() ) {
        	double elapsed_phone_off = phone_usage.getDouble(phone_usage.getColumnIndex(DeviceUsage_Data.ELAPSED_DEVICE_OFF));
        	off.setText( DateUtils.formatElapsedTime( (long) elapsed_phone_off/1000) + " (off)");
        }
        if( phone_usage != null && ! phone_usage.isClosed() ) phone_usage.close();
        
        LinearLayout chart = (LinearLayout) card.findViewById(R.id.chart_usage); 
        chart.removeAllViews();
        
		//Get today's time from the beginning in milliseconds
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		//stores screen on counts grouped per hour
		int[] frequencies = new int[24];
		
		//add frequencies to the right hour buffer
		Cursor phone_usage_times = context.getContentResolver().query(DeviceUsage_Data.CONTENT_URI, new String[]{ "count(*) as frequency","strftime('%H',"+ DeviceUsage_Data.TIMESTAMP + "/1000, 'unixepoch', 'localtime')+0 as time_of_day" }, DeviceUsage_Data.ELAPSED_DEVICE_ON + " > 0 AND " + DeviceUsage_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ", null, DeviceUsage_Data.TIMESTAMP + " ASC");
		if( phone_usage_times != null && phone_usage_times.moveToFirst() ) {
			do{
				frequencies[phone_usage_times.getInt(1)] = phone_usage_times.getInt(0);
			} while( phone_usage_times.moveToNext() );
			phone_usage_times.close();
		}
		
		XYSeries xy_series = new XYSeries("Device activity");
		for( int i = 0; i<frequencies.length; i++ ) {
			xy_series.add(i, frequencies[i]);
		}
		
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(xy_series);
		
		//Setup the line colors, labels, etc
		XYSeriesRenderer series_renderer = new XYSeriesRenderer();
		series_renderer.setColor(Color.BLACK);
		series_renderer.setPointStyle(PointStyle.POINT);
		series_renderer.setDisplayChartValues(false);
		series_renderer.setLineWidth(2);
		series_renderer.setFillPoints(false);
		
		//Setup graph colors, labels, etc
		XYMultipleSeriesRenderer dataset_renderer = new XYMultipleSeriesRenderer();
		dataset_renderer.setChartTitle("Today's device activity");
		dataset_renderer.setLabelsColor(Color.BLACK);
		dataset_renderer.setDisplayValues(true);
		dataset_renderer.setFitLegend(false);
		dataset_renderer.setXLabelsColor(Color.BLACK);
		dataset_renderer.setYLabelsColor(0, Color.BLACK);
		dataset_renderer.setLegendHeight(0);
		dataset_renderer.setYLabels(0);
		dataset_renderer.setZoomButtonsVisible(false);
		dataset_renderer.setXLabels(0);
		dataset_renderer.setPanEnabled(false);
		dataset_renderer.setShowGridY(false);
		dataset_renderer.setClickEnabled(false);
		dataset_renderer.setAntialiasing(true);
		dataset_renderer.setAxesColor(Color.BLACK);
		dataset_renderer.setApplyBackgroundColor(true);
		dataset_renderer.setBackgroundColor(Color.WHITE);
		dataset_renderer.setMarginsColor(Color.WHITE);
		
		for(int i=0; i< x_hours.length; i++) {
			dataset_renderer.addXTextLabel(i, x_hours[i]);
		}
		
		//Add the series renderer to the chart renderer
		dataset_renderer.addSeriesRenderer(series_renderer);
		
		//Create the chart with our data and setup
		GraphicalView mChart = (GraphicalView) ChartFactory.getBarChartView(context, dataset, dataset_renderer, Type.DEFAULT); //bar chart
		//mChart = (GraphicalView) ChartFactory.getLineChartView(context, dataset, dataset_renderer); //line chart
        
        chart.addView(mChart);
        
        return card;
	}
}
