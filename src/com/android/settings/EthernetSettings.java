package com.android.settings;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import com.android.settings.R;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.provider.Settings;

import com.android.settings.RestrictedSettingsFragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.net.EthernetDataTracker;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;
import android.net.DhcpResults;

import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.DataOutputStream;

import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.NetworkUtils;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.ProxyProperties;
import android.net.RouteInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.util.Iterator;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.net.SocketException;

import android.os.RemoteException;



import android.os.INetworkManagementService;


public class EthernetSettings extends SettingsPreferenceFragment implements OnClickListener, CompoundButton.OnCheckedChangeListener  {

	private View view;
	private RadioButton radDhcp, radStatic;
	private LinearLayout layStatic;
	private LinearLayout EditStatic;
	private Button btSave,cancel;
	private EditText ethIpaddress, eth_gateway, eth_netmask, eth_dns1, eth_dns2;
	private TextView hintText,stateTV;
	private Switch actionBarSwitch;
	private INetworkManagementService mNetworkService;
	private ConnectivityManager mConnService;
	private LinkProperties linkProperties;
	private EthernetDataTracker ethernetDataTracker;
	private ConnectionChangeReceiver br;
	private IntentFilter intentFilter;
	
	private SharedPreferences preferences;
	
	private ArrayList<String> logcatCMD = new ArrayList<String>();
	protected Process mLogcatProc = null;
	
	
	public class ConnectionChangeReceiver extends BroadcastReceiver {   
        @Override   
        public void onReceive( Context context, Intent intent ) {   
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);   
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();   
            NetworkInfo ethInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);   
            if((activeNetInfo != null) && (ethInfo.getState() == State.CONNECTED))
            {
            	stateTV.setText(R.string.eth_state_connect);
            	//Toast.makeText( context, "以太网络已连接", Toast.LENGTH_SHORT ).show();                           	
            	fillStaticViews(connectivityManager.getActiveLinkProperties());
            	if(!actionBarSwitch.isChecked() && !preferences.getBoolean("SwitchState", true))
            	{
            		//启动activity之前 网络就已经连接上了 所以更新UI
            		actionBarSwitch.setChecked(true);
            		EthernetSettings.this.onCheckedChanged(actionBarSwitch, true);
            	}
            	
            }
            else
            {
            	stateTV.setText(R.string.eth_state_disconnect);
            	//Toast.makeText( context, "以太网络已断开", Toast.LENGTH_SHORT ).show();  
            }
         
          }   
    }  

	/*public EthernetSettings() {
		// super(DISALLOW_CONFIG_WIFI);
	}*/

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ethernetDataTracker = EthernetDataTracker.getInstance();
		mNetworkService = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
		
		mConnService = (ConnectivityManager)
				getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		
		intentFilter  = new IntentFilter();
		intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		intentFilter.addAction("com.rio.broadcast.ETHERNETSETTINGS");
		br = new ConnectionChangeReceiver();
		getActivity().registerReceiver(br,intentFilter);
		
		linkProperties = new LinkProperties();

    }
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		getActivity().unregisterReceiver(br);
	}

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.ethernet_config, container, false);

		layStatic = (LinearLayout) view.findViewById(R.id.lay_eth_static);
		EditStatic = (LinearLayout) view.findViewById(R.id.eth_Edit_static);
		radDhcp = (RadioButton) view.findViewById(R.id.eth_ip_dhcp);
		radStatic = (RadioButton) view.findViewById(R.id.eth_ip_static);
		btSave = (Button) view.findViewById(R.id.eth_dhcp_save);
		cancel = (Button) view.findViewById(R.id.eth_dhcp_cancel);
		ethIpaddress = (EditText)view.findViewById(R.id.eth_ip_address);
		eth_gateway = (EditText)view.findViewById(R.id.eth_gateway);
		eth_netmask = (EditText)view.findViewById(R.id.eth_netmask);
		eth_dns1 = (EditText)view.findViewById(R.id.eth_dns1);
		eth_dns2 = (EditText)view.findViewById(R.id.eth_dns2);
		hintText = (TextView)view.findViewById(R.id.hintText);
		stateTV = (TextView)view.findViewById(R.id.eth_id_stateTV);
		btSave.setOnClickListener(this);
		radDhcp.setOnClickListener(this);
		radStatic.setOnClickListener(this);
		cancel.setOnClickListener(this);

		
		
		
		return view;
	}
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();
        
        actionBarSwitch = new Switch(activity);
        preferences = activity.getSharedPreferences("Eswitch",Context.MODE_PRIVATE);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPaddingRelative(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
                
                actionBarSwitch.setOnCheckedChangeListener(this);                
                actionBarSwitch.setChecked(preferences.getBoolean("SwitchState", true));
                this.onCheckedChanged(actionBarSwitch, preferences.getBoolean("SwitchState", true));
            }
        }
        
        if(preferences.getBoolean("StaticIP", false))   //保存状态为静态IP
        {
        	radStatic.setChecked(true);
        	settingEnabled(true);
        }
    }
    
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event

        // Show toast message if Wi-Fi is not allowed in airplane mode
        if (isChecked) {
        	if(!preferences.getBoolean("SwitchState", true))
        	{
        		EthernetService.s_Etherservice.setInterfaceUp();
        	}
        	//SystemProperties.set("ethernet.up.state", "true");
        	Editor ed = preferences.edit();
        	ed.putBoolean("SwitchState", true);
        	ed.commit();
        	
        	layStatic.setVisibility(View.VISIBLE);
        	hintText.setVisibility(View.GONE);
        }
        else
        {
        	if(preferences.getBoolean("SwitchState", true))
        	{
        		EthernetService.s_Etherservice.setInterfaceDown(); 
        	}
    			 //	SystemProperties.set("ethernet.up.state", "false"); 
        	Editor ed = preferences.edit();
        	ed.putBoolean("SwitchState", false);
        	ed.commit();
        	
         	layStatic.setVisibility(View.GONE);
        	hintText.setVisibility(View.VISIBLE);
        }
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.eth_ip_dhcp:
			settingEnabled(false);
			break;
		case R.id.eth_ip_static:
			settingEnabled(true);
			break;
		case R.id.eth_dhcp_cancel:
			finish();
			break;
		case R.id.eth_dhcp_save:			
			if (radDhcp.isChecked()) {
				//EthernetService.s_Etherservice.setInterfaceDown(); 
				//EthernetService.s_Etherservice.setInterfaceUp();
				//ethernetDataTracker.reconnect();
				try{
					mNetworkService.clearInterfaceAddresses("eth0");
				}
				catch(Exception e)
				{}
				NetworkUtils.resetConnections("eth0", NetworkUtils.RESET_ALL_ADDRESSES);
				if(!NetworkUtils.stopDhcp("eth0"))
				{
					Log.e("ETHERNETSETTINGS", "Can not stop DHCP....");
				}
				//DhcpResults dhcpResults = new DhcpResults();
				//NetworkUtils.runDhcp("eth0", dhcpResults);
				
				runDhcp();
				
				Editor ed = preferences.edit();
				ed.clear();
	        	ed.putBoolean("StaticIP", false);        	
	        	ed.commit();
			} else if (radStatic.isChecked()) {
				if(!validateIpAddress())
					return;	            		
				
				if(EthernetService.s_Etherservice!=null)
				{
					EthernetService.s_Etherservice.setIpAndGateWay(ethIpaddress.getText().toString(),eth_gateway.getText().toString());   
				}
				else
				{
					Toast.makeText( getActivity(), "网络设置失败，以太网服务未运行,正尝试重启服务请稍候再试", Toast.LENGTH_SHORT ).show();  
					Intent it = new Intent(getActivity(),EthernetService.class);
					getActivity().startService(it);
					return;
				}                                       
	            Editor ed = preferences.edit();
	        	ed.putBoolean("StaticIP", true);
	        	ed.commit();
	                                    
			}
			Toast.makeText( getActivity(), "设置已保存", Toast.LENGTH_SHORT ).show();  
			break;
		}
	}
	public static final int DHCP_SUCCESS = 1;
	private Handler mDHCPhandler = new Handler(){
		
		public void handleMessage(Message msg){
			switch(msg.what){
			case DHCP_SUCCESS:
				//fillStaticViews(linkProperties);
				Log.e("", "I dont want crash here!!!!!!!!!!!!!!!!!" );
				Intent it = new Intent("com.rio.broadcast.ETHERNETSETTINGS");
				getActivity().sendBroadcast(it);
				break;
			default:
				break;
			}
		}
		
		
	};
	
	private void runDhcp() {
        Thread dhcpThread = new Thread(new Runnable() {
            public void run() {
                DhcpResults dhcpResults = new DhcpResults();
                if (!NetworkUtils.runDhcp("eth0", dhcpResults)) {
                    Log.e("", "DHCP request error:" + NetworkUtils.getDhcpError());
                    return;
                }
               // linkProperties = dhcpResults.linkProperties;
                Log.e("", "DHCP request Success!!!!!!!!!!!!!!!!!" );
               // mNetworkInfo.setIsAvailable(true);
               // mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, mHwAddr);
              //  Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
               // msg.sendToTarget();
              //  Message msg = new Message();
              //  msg.what = DHCP_SUCCESS;
                mDHCPhandler.sendEmptyMessage(DHCP_SUCCESS);
            }
        });
        dhcpThread.start();
    }
	
	private void fillStaticViews(LinkProperties linkproper)
	{
		String ip,gw,dns1,dns2,nm;
		ip = preferences.getString("ip", "");
		gw = preferences.getString("gateway", "");
		dns1 ="";// preferences.getString("dns1", "");
		dns2 ="";// preferences.getString("dns2", "");
		nm = preferences.getString("netmask", "255.255.255.0");
				   	
		Iterator<LinkAddress> iterator = linkproper.getLinkAddresses().iterator();		
		if(ip.equals("")||ip == null)
		{
			if (iterator.hasNext()) {
		        LinkAddress linkAddress = iterator.next();
		        ethIpaddress.setText(linkAddress.getAddress().getHostAddress());
		    }
		}
		else
		{
			Log.e("ETHERNETSETIING", "Use preference IP "+ ip);
			ethIpaddress.setText(ip);
		}
	      
		for (RouteInfo route : linkproper.getRoutes()) {    
			if (route.isDefaultRoute()) {         
				if(gw.equals("")||gw == null)
				{
					eth_gateway.setText(route.getGateway().getHostAddress()); 
					Log.e("ETHERNETSETIING", "gateway addr is "+ route.getGateway().getHostAddress());
				}
				else
				{
					eth_gateway.setText(gw);
					Log.e("ETHERNETSETIING", "Use preference gateway "+ gw);
				}
				break;           
			}       
		}     

		Iterator<InetAddress> dnsIterator = linkproper.getDnses().iterator();     
		if (dnsIterator.hasNext()) {   
			if(dns1.equals("")||dns1 == null)
			{
				String tmpdns1 = dnsIterator.next().getHostAddress();
				eth_dns1.setText(tmpdns1);   
				Log.e("ETHERNETSETIING", "dns addr is "+ tmpdns1);
			}
			else
			{
				eth_dns1.setText(dns1); 
				Log.e("ETHERNETSETIING", "Use preference dns "+ dns1);
			}
		}
		else
		{
			Log.e("ETHERNETSETIING", "找不到DNS============================================================== ");
			eth_dns1.setText(gw);
		}
		       
		if (dnsIterator.hasNext()) {      
			if(dns2.equals("")||dns2 == null)
			{
				String tmpdns2 = dnsIterator.next().getHostAddress();
				eth_dns2.setText(tmpdns2);
				Log.e("ETHERNETSETIING", "dns addr is "+ tmpdns2);
			}
			else
				eth_dns2.setText(dns2);
		}
		
		eth_netmask.setText(nm);
	}
	
	private boolean isIpAddress(String value)
	{     		        
		int start = 0;         
		int end = value.indexOf('.');        
		int numBlocks = 0;        
        while (start < value.length()) {            
            if (end == -1) { 
                end = value.length(); 
            }         
            try { 
                int block = Integer.parseInt(value.substring(start, end)); 
                if ((block > 255) || (block < 0)) { 
                    return false; 
                } 
            } catch (NumberFormatException e) { 
                return false; 
            }            
            numBlocks++; 
            start = end + 1; 
            end = value.indexOf('.', start); 
        }     
        return numBlocks == 4; 
    }
	
	private boolean validateIpAddress()
	{
		String nm,dns2;
		boolean ret;
		String ip = ethIpaddress.getText().toString();
		if(!isIpAddress(ip))
		{
			Toast.makeText(getActivity(), "请输入合法IP地址", Toast.LENGTH_SHORT ).show();  
			ethIpaddress.requestFocus();
			return false;
		}
        InetAddress inetAddr = null;
        try {
            inetAddr = NetworkUtils.numericToInetAddress(ip);
        } catch (IllegalArgumentException e) {
            return false;
        }	
        linkProperties.addLinkAddress(new LinkAddress(inetAddr, 24));
      
        String gw = eth_gateway.getText().toString();
        if(!isIpAddress(gw))
		{
			Toast.makeText(getActivity(), "请输入合法网关", Toast.LENGTH_SHORT ).show();  
			eth_gateway.requestFocus();
			return false;
		}
        
        InetAddress gatewayAddr = null;
        try {
            gatewayAddr = NetworkUtils.numericToInetAddress(gw);
        } catch (IllegalArgumentException e) {
            return false;
        }
        linkProperties.addRoute(new RouteInfo(gatewayAddr));
        
        
        String dns1 = eth_dns1.getText().toString();
      
        if(!dns1.equals(""))
        {
	        if(!isIpAddress(dns1))
			{
				Toast.makeText(getActivity(), "请输入合法DNS", Toast.LENGTH_SHORT ).show();  
				eth_dns1.requestFocus();
				return false;
			}
        }
        else
        {
        	dns1 = gw;
        }
        InetAddress dnsAddr = null;   
        try {
            dnsAddr = NetworkUtils.numericToInetAddress(dns1);
        } catch (IllegalArgumentException e) {
            return false;
        }
        linkProperties.addDns(dnsAddr);
               
		dns2 = eth_dns2.getText().toString();
		if(!dns2.equals(""))
		{
			if(!isIpAddress(dns2))
			{
				Toast.makeText(getActivity(), "请输入合法DNS", Toast.LENGTH_SHORT ).show();  
				eth_dns2.requestFocus();
				return false;
			}
			try {
                dnsAddr = NetworkUtils.numericToInetAddress(dns2);
            } catch (IllegalArgumentException e) {
                return false;
            }
            linkProperties.addDns(dnsAddr);
			
		}
		nm  = eth_netmask.getText().toString();
		if(!isIpAddress(nm))
		{
			Toast.makeText(getActivity(), "请输入合法子网掩码", Toast.LENGTH_SHORT ).show();  
			eth_netmask.requestFocus();
			return false;
		}
		
		Editor ed = preferences.edit();
    	ed.putString("ip", ip);
    	ed.putString("gateway", gw);
    	ed.putString("netmask", nm);
    	ed.putString("dns1", dns1);
    	ed.putString("dns2", dns2);
    	ed.commit();
        return true;
		
	}
	

	private void settingEnabled(boolean enabled) {
		ethIpaddress.setEnabled(enabled);
		eth_gateway.setEnabled(enabled); 
		eth_netmask.setEnabled(enabled);
		eth_dns1.setEnabled(enabled);
		eth_dns2.setEnabled(enabled);
	}
	
	/*public String getLocalIpAddress() {
		try {
			NetworkInterface ni =NetworkInterface.getByName("eth0");
			if(ni!=null)
			{
				for (Enumeration<InetAddress> enumIpAddr = ni
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}			
			
		} catch (SocketException ex) {
			Log.e("WifiPreference IpAddress", ex.toString());
		}
		return null;
	}*/
}
