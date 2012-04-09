package com.android.BluetoothManager.UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.BluetoothManager.Application.R;
import com.android.BluetoothManager.UI.viewpager.TitleProvider;

public class ViewPagerAdapter extends PagerAdapter implements TitleProvider {

	public Context context;
	private final String TAG = "ViewPagerAdapter";

	public ArrayList<String> deviceAddresses;
	public ArrayList<String> deviceNames;
	private ArrayList<ListView> listViews;
	
	HashMap<String, ArrayAdapter<String>> conversation_map;

	public ViewPagerAdapter(Context context,
			HashMap<String, ArrayAdapter<String>> conversation_map) {

		Log.d(TAG, " +++ Constructor Called");

		this.context = context;
		this.conversation_map = conversation_map;

		this.deviceNames = new ArrayList<String>();
		this.deviceAddresses = new ArrayList<String>();
		this.listViews = new ArrayList<ListView>();

	}

	public void addDevice(String device, String name, String msg) {

		ListView v = new ListView(context);
		deviceNames.add(name);
		deviceAddresses.add(device);
		listViews.add(v);

		ArrayAdapter<String> newAdapter = new ArrayAdapter<String>(context, R.layout.chat_msg);
		if(!msg.equals("")){
			newAdapter.add(name + ":" + msg);
		}
		
		v.setAdapter(newAdapter);
		conversation_map.put(device, newAdapter);
		
		Log.d(TAG, "Device added:" + device + "Adapter:" + newAdapter);
	}

	public ArrayAdapter<String> getChatAdapter(int position) {
		return conversation_map.get(deviceAddresses.get(position));
	}

	public String getDevice(int position) {
		return deviceAddresses.get(position);
	}
	
	@Override
	public String getTitle(int position) {
		Log.d(TAG, " +++ getTitle() Called");
		return deviceNames.get(position);
	}

	@Override
	public int getCount() {
		Log.d(TAG, " +++ getCount() Called"+ deviceNames.size());
		return deviceNames.size();
	}

	@Override
	public Object instantiateItem(ViewGroup pager, int position) {
		Log.d(TAG, " +++ instantiateItem Called");
		ListView v = listViews.get(position);
		((ViewPager) pager).addView(v, 0);
		return v;
	}

	@Override
	public void destroyItem(View pager, int position, Object view) {
		Log.d(TAG, " +++ destroyItem Called");
		((ViewPager) pager).removeView((ListView) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		Log.d(TAG, " +++ isViewFromObject Called");
		return view.equals(object);
	}

	@Override
	public void finishUpdate(View view) {
		Log.d(TAG, " +++ finishUpdate Called");
	}

	@Override
	public void restoreState(Parcelable p, ClassLoader c) {
		Log.d(TAG, " +++ restoreState Called");
	}

	@Override
	public Parcelable saveState() {
		Log.d(TAG, " +++ saveState Called");
		return null;
	}

	@Override
	public void startUpdate(View view) {
		Log.d(TAG, " +++ startUpdate Called");
	}

	public void printContents(ArrayList list)
	{
		Log.d(TAG,"Printing content of size :"+ list.size());
		Iterator<String> itr=list.iterator();
		while(itr.hasNext())
		{
			Log.d(TAG,itr.next());
		}
	}
}
