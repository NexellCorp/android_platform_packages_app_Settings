package com.android.settings;

import java.net.InetAddress;

import com.android.settings.EthernetSettings;
import com.android.settings.R;
import com.android.settings.Settings.EthernetSettingsActivity;

import android.provider.Settings;
import android.graphics.drawable.Drawable;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.NetworkInfo.State;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("NewApi")
public class EthernetService extends Service {
	
	private SharedPreferences preferences;
	private INetworkManagementService mNetworkService;
	public static EthernetService s_Etherservice;
	public NotificationManager nm;
	public Notification connectNotify;

	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	@Override
	public void onCreate(){
		super.onCreate();	
		s_Etherservice = this;
		IntentFilter intentFilter  = new IntentFilter();
		intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		ConnectionReceiver br = new ConnectionReceiver();
		registerReceiver(br,intentFilter);
		
		
		nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		
		connectNotify = new Notification(R.drawable.network_connect,"以太网状态改变",System.currentTimeMillis());
		
		
		preferences = getSharedPreferences("Eswitch",Context.MODE_PRIVATE);
		
		mNetworkService = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
		
		if(!preferences.getBoolean("SwitchState", true))
		{
			//以太网为关闭状态
			setInterfaceDown();
		}
		
		Log.e("EthernetService", "EthernetService On Create");
	}
	@Override
	public int onStartCommand(Intent intent,int flags,int startId){
		return super.onStartCommand(intent, flags, startId);
		
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		s_Etherservice=null;
	}
	
	public void setStaticIpAddress(String mIface, String ip) 	 
	{	
		try {		
			if (!mIface.startsWith("eth")) {	
				Log.e("", "interface is error");		
				return;         
			}	
				
			InterfaceConfiguration ifcg = mNetworkService.getInterfaceConfig(mIface);				
	
			ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(ip), 24));			
			ifcg.setInterfaceUp();			 
			mNetworkService.setInterfaceConfig(mIface, ifcg);
			
			Log.v("", "Static IP configuration succeeded");			
		} catch (Exception e) {
			Log.e("", "Error configuring interface " + mIface + ", :" + e);
			return;
		}
	     
	}   	
	
	public void setInterfaceDown()
	{
		try {
			mNetworkService.setInterfaceDown("eth0");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setInterfaceUp()
	{
		try {
			mNetworkService.setInterfaceUp("eth0");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addRoute(String mIface, RouteInfo route) 
	{
		try { 	
			if (!mIface.startsWith("eth")) {
				Log.e("", "interface is error");	
				return;      	
			}  		
			if (mNetworkService != null) {					
				mNetworkService.addRoute(mIface, route);	
			}					
			Log.e("", "addRoute-------------------------route success");	   		 
		} catch (Exception e) {	     
			Log.e("", "Error addRoute interface " + mIface + ", :" + e);         
			return;	        
		}	   	
	}   	
	
	public void setIpAndGateWay(String ip,String gw)
	{
		InetAddress gatewayAddr = null;
        try {
            gatewayAddr = NetworkUtils.numericToInetAddress(gw);
        } catch (IllegalArgumentException e) {
            Log.e("", "EthernetSettings===========================gateway error");
        }
		setStaticIpAddress("eth0", ip);
        addRoute("eth0", new RouteInfo(gatewayAddr));
	}
	
	
	public class ConnectionReceiver extends BroadcastReceiver {   
        @Override   
        public void onReceive( Context context, Intent intent ) {   
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);   
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();   
            NetworkInfo ethInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);   
            Intent it = new Intent(context,EthernetSettingsActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_CANCEL_CURRENT);
            if((activeNetInfo != null) && (ethInfo.getState() == State.CONNECTED))
            {
            	//Toast.makeText( context, "以太网络已连接", Toast.LENGTH_SHORT ).show();  
            	Log.e("EthernetService", "以太网络已连接");  
            	boolean staticIp = preferences.getBoolean("StaticIP", false);
            	String ip = preferences.getString("ip", "");
        		String gw = preferences.getString("gateway", "");
            	if(staticIp && !ip.equals("") && !gw.equals(""))
            	{
            		Log.e("EthernetService", "设置之前保存的IP信息"); 
            		setIpAndGateWay(ip, gw);
            	}
            	if(connectNotify.icon!=R.drawable.network_connect)
            	{
	            	nm.cancel(1);
	            	connectNotify.icon = R.drawable.network_connect;
	            	connectNotify.flags = Notification.FLAG_ONGOING_EVENT;
	            	connectNotify.setLatestEventInfo(context, "网线已插入", "网络已连接", pi);
	            	nm.notify(1, connectNotify);
            	}            	
            }
            else
            {
            	//Toast.makeText( context, "以太网络已断开", Toast.LENGTH_SHORT ).show();  
            	Log.e("EthernetService", "以太网络已断开");   
            	if(connectNotify.icon!=R.drawable.network_disconnect)
            	{
	            	nm.cancel(1);
	            	connectNotify.flags = Notification.FLAG_ONGOING_EVENT;
	            	connectNotify.icon = R.drawable.network_disconnect;
	            	connectNotify.setLatestEventInfo(context, "网线已拔出", "网络已断开连接", pi);
	            	nm.notify(1, connectNotify);
            	}
            }
        }   
    }  
}