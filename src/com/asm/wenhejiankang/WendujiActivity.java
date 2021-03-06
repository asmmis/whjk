package com.asm.wenhejiankang;
import com.github.mikephil.charting.charts.LineChart;
import android.app.Activity;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.charts.BarLineChartBase.BorderPosition;
import android.graphics.Typeface;
import android.graphics.Color;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.data.LineData;
import java.util.ArrayList;
import com.github.mikephil.charting.utils.YLabels;
import com.github.mikephil.charting.utils.XLabels;
import com.github.mikephil.charting.utils.Legend;
import com.github.mikephil.charting.utils.Legend.LegendForm;
import android.os.Bundle;
import com.xl.game.tool.DisplayUtil;
import android.widget.ListView;
import com.xl.view.MyAdapter3;
import com.asm.wenhejiankang.net.Net_whjk;
import com.asm.wenhejiankang.net.Net_whjk_Listener;
import com.asm.wenhejiankang.model.User;
import java.util.Date;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import java.util.Calendar;
import android.widget.TextView;
import com.xl.game.tool.Log;


public class WendujiActivity extends StartActivity implements OnChartValueSelectedListener,Net_whjk_Listener,OnClickListener
	{

		@Override
		public void onUp()
			{
				// TODO: Implement this method
			}


		@Override
		public void onClick(View p1)
			{
				// TODO: Implement this method
				switch(p1.getId())
				{
					case R.id.prior15:
						
						break;
					case R.id.next15:
						
						break;
				}
			}
		

		@Override
		public void onEnter(User user)
			{
				// TODO: Implement this method
			}

		@Override
		public void onError(String text)
			{
				// TODO: Implement this method
			}

		@Override
		public void onTiWen(ArrayList<String> list)
			{
				//
				if(list!=null)
				for(String name:list)
				addData(name);
			}

		@Override
		public void onTiwenError()
			{
				// TODO: Implement this method
			}

		@Override
		public void onXieya(ArrayList<String> list)
			{
				// TODO: Implement this method
			}

		@Override
		public void onXieyaError()
			{
				// TODO: Implement this method
			}

		@Override
		public void onXietang(ArrayList<String> list)
			{
				// TODO: Implement this method
			}

		@Override
		public void onXietangError()
			{
				// TODO: Implement this method
			}

		@Override
		public void onXieyang(ArrayList<String> list)
			{
				// TODO: Implement this method
			}

		@Override
		public void onXieyangError()
			{
				// TODO: Implement this method
			}

		@Override
		public void onUpError()
			{
				// TODO: Implement this method
			}


		@Override
		public void onValueSelected(Entry p1, int p2)
			{
				// 
				
			}

		@Override
		public void onNothingSelected()
			{
				// TODO: Implement this method
			}
		//XlApplication application;
		
	
		private LineChart mChart;
		ArrayList<Entry> entry;
		ListView listview;
    MyAdapter3 adapter;
		Net_whjk net;
		XlApplication application;
		Button btn_prior15,btn_next15;
		TextView text_time;
		Date update,nextdate;
		
		@Override
		protected void onCreate(Bundle savedInstanceState)
			{
				//
				super.onCreate(savedInstanceState);
				
				setTitle(R.string.chart_tiwen);
				
			}

		@Override
		public void onContentView()
			{
				
				setContentView(R.layout.info_tiwen);
				adapter = new MyAdapter3(this);		
				listview = (ListView)findViewById(R.id.list_tiwen);
				btn_prior15=(Button)findViewById(R.id.prior15);
				text_time=(TextView)findViewById(R.id.time);
				btn_next15=(Button)findViewById(R.id.next15);
				btn_prior15.setOnClickListener(this);
				btn_next15.setOnClickListener(this);
				text_time.setOnClickListener(this);
				
				application=(XlApplication)getApplication();
				
				net=application.getNetContext();
				if(net==null)
					finish();
				net.setListener(this);
				Date date=new Date();
				getTiwen(getNextDay(date),date);
				onSetChart();
			}
	
	//获取指定时间到指定时间的体温
	void getTiwen(Date next,Date date)
	{
		net.getTiwen(next,date);
		text_time.setText(""+next.getMonth()+"."+next.getDay()+"-"+date.getMonth()+"."+date.getDay());
	}
	//获取前15天时间
		public static Date getNextDay(Date date) {
			long time=date.getTime();
			time=time-15*1000*60*60*24;
			/*
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.add(Calendar.DATE, -15);
				date = calendar.getTime();
				*/
			date=new Date(time);
				return date;
			}
	
	private void onSetChart()
	{
		mChart = (LineChart) findViewById(R.id.chart1);
		mChart.setOnChartValueSelectedListener(this);

		mChart.setUnit("℃");
		//mChart.setDrawUnitsInChart(true);

		// if enabled, the chart will always start at zero on the y-axis
		//mChart.setStartAtZero(false);

		// disable the drawing of values into the chart
		mChart.setDrawYValues(false);

		mChart.setDrawBorder(true);
		mChart.setBorderPositions(new BorderPosition[] {
				BorderPosition.BOTTOM
			});

		// no description text
		mChart.setDescription("");
		mChart.setNoDataTextDescription(getResources().getString(R.string.no_data));

		// enable value highlighting
		mChart.setHighlightEnabled(true);

		// enable touch gestures
		mChart.setTouchEnabled(true);

		// enable scaling and dragging
		mChart.setDragEnabled(true);
		mChart.setScaleEnabled(true);
		mChart.setDrawGridBackground(false);
		mChart.setDrawVerticalGrid(true);
		mChart.setDrawHorizontalGrid(true);

		// if disabled, scaling can be done on x- and y-axis separately
		mChart.setPinchZoom(true);

		// set an alternative background color
		mChart.setBackgroundColor(getResources().getColor( R.color.background));

		// add data 设置数据
		entry = new ArrayList<Entry>();
		//addData( 1,37.3f);
		//addData(2,37);
		setData(entry,15, 100);
		Typeface tf = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");

		// get the legend (only possible after setting data)
		Legend l = mChart.getLegend();

		// modify the legend ...
		if(l!=null)
			{
				// l.setPosition(LegendPosition.LEFT_OF_CHART);
				l.setForm(LegendForm.LINE);
				l.setTypeface(tf);
				l.setTextColor(getResources().getColor(R.color.chart_legend));
			}
		XLabels xl = mChart.getXLabels();
		if(xl!=null)
			{
				//xl.setCenterXLabelText(true);
				xl.setPosition(XLabels. XLabelPosition.BOTTOM);
				xl.setTypeface(tf);
				xl.setTextColor(getResources().getColor(R.color.chart_xlable));
			}
		YLabels yl = mChart.getYLabels();
		if(yl!=null)
			{
				yl.setTypeface(tf);
				yl.setTextColor(getResources().getColor(R.color.chart_ylable));
			}
		
		mChart.animateX(2500);

		listview.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}
	
	//添加一个温度信息
	//参数：时间 温度
	private void addData(int time,float num)
	{
		entry.add(new Entry(num,time));
		adapter.add(""+time,""+num);
		
	}
	
	//将获取的信息添加到列表
	private void addData(String text)
	{
		String items[]=text.split(" ");
		if(items.length>=2)
		{
		adapter.add(items[0],items[1]);
			entry.add(new Entry(Float.parseFloat(items[1]),adapter.getCount()));
		}
		adapter.notifyDataSetChanged();
		setData(entry,15, 100);
		
				
		Log.e("添加体温数据",text);
	}
	
	
		private void setData(ArrayList<Entry> entry, int count, float range) {

        ArrayList<String> xVals = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            xVals.add((i) + "");
					}

        ArrayList<Entry> yVals = new ArrayList<Entry>();
/*
        for (int i = 0; i < count; i++) {
            float mult = (range + 1);
            float val = (float) (Math.random() * mult) + 3;// + (float)
						// ((mult *
						// 0.1) / 10);
            yVals.add(new Entry(val, i));
					}
					*/

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(entry, "体温");
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setCircleColor(0xff60a0f0);
        set1.setLineWidth(DisplayUtil.dip2px(this,1));
        set1.setCircleSize(DisplayUtil.dip2px(this,1));
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set1); // add the datasets

        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);

        // set data
        mChart.setData(data);
			}
	
}
