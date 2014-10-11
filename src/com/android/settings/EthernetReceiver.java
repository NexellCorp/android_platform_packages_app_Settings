package com.android.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.util.Log;
import android.widget.Toast;

public class EthernetReceiver extends BroadcastReceiver {   
        @Override   
        public void onReceive( Context context, Intent intent ) {   
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);   
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();   
            NetworkInfo ethInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET); 
            
            Log.e("EthernetReceiver", "BOOT COMPLETE");
            
            String action = intent.getAction();
            Log.v("EthernetReceiver", "Received: " + action);

            if (action.equals("android.intent.action.BOOT_COMPLETED")){   
                Intent it = new Intent(context,EthernetService.class);
                context.startService(it);
            }
            else
            {
	           /* if((activeNetInfo != null) && (ethInfo.getState() == State.CONNECTED))
	            {
	            	Log.e("EthernetReceiver", "以太网络已连接");
	            }
	            else
	            {
	            	Log.e("EthernetReceiver", "以太网络已");
	            }*/
            	Intent it = new Intent(context,EthernetService.class); //为了保证服务不被杀死，收到网络变化广播也会重新启动一次
                context.startService(it);
            }
         
          }   
}


