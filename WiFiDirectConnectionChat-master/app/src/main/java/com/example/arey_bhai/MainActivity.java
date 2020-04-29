package com.example.arey_bhai;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button btnOnOff,btnDiscover,btnSend, btnCreateGroup;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText writeMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ=1;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;
    ArrayList<SendReceive> sendReceiveArrayList;
    HashMap<InetAddress, SendReceive> sendReceiveHashMap;
    boolean groupCreated;
    boolean serverCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_READ:
                    byte[] readBuff=(byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    try {
                        JSONObject obj= new JSONObject(tempMsg);
                        read_msg_box.setText(obj.getString("diMes"));

                       // Log.d("Message - ", tempMsg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    break;
            }
            return true;
        }
    });

    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("ON");
                }
                else{
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discover karega ab");
                    }

                    @Override
                    public void onFailure(int reason) {
                    connectionStatus.setText("Nahi shuru ho payi discpvery");
                    }
                });
            }
        });
        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!groupCreated)
                {
                    mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(),"group successfully created ",Toast.LENGTH_SHORT).show();
                            groupCreated = true;
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(getApplicationContext(),"group not created!!",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device=deviceArray[position];
//                String client_mac_fixed = new String(device.deviceAddress).replace("99", "19");
////                String clientIP = Utils.getIPFromMac(client_mac_fixed);
                WifiP2pConfig config=new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"connected to "+device.deviceName,Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(),"Not connected",Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeMsg=(EditText) findViewById(R.id.writeMsg);
                String msg1=writeMsg.getText().toString();
                String msg=new String();
                msg="{\"username\":\"abc\",\"type:\":\"groupMessage\",\"diMes\":\""+msg1+"\"}";
                try {
                    if(serverCreated) {
                        for(SendReceive sendReceiveDevice: sendReceiveHashMap.values()){
                            try {
                                sendReceiveDevice.write(msg.getBytes());
                            }
                            catch(Exception e){
                                Log.e("Exception is " ,  e.toString());
                            }
                        }
                    }
                    else{
                        sendReceive.write(msg.getBytes());
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }

    private void initialWork() {
        btnOnOff=(Button) findViewById(R.id.onOff);
        btnDiscover=(Button) findViewById(R.id.discover);
        btnSend=(Button) findViewById(R.id.sendButton);
        btnCreateGroup = (Button) findViewById(R.id.createGroup);
        listView=(ListView) findViewById(R.id.peerListView);
        read_msg_box=(TextView) findViewById(R.id.readMsg);
        connectionStatus=(TextView) findViewById(R.id.connectionStatus);
        writeMsg=(EditText) findViewById(R.id.writeMsg);

        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);

        mReceiver =new WifiDirectBroadcastReceiver(mManager, mChannel,this);
        mIntentFilter=new IntentFilter();

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray= new String[peerList.getDeviceList().size()];
                deviceArray=new WifiP2pDevice[peerList.getDeviceList().size()];
                int index=0;

                for(WifiP2pDevice device: peerList.getDeviceList()){
                    deviceNameArray[index]=device.deviceName;
                    deviceArray[index]=device;
                    index++;
                }

                ArrayAdapter<String> adapter= new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
                String mseg = "\nPeers";
                for(WifiP2pDevice dev: peerList.getDeviceList()){
                    mseg += "\n" + dev.deviceAddress;
                }
                Log.d("onPeersAvailable", mseg);
            }
            if (peers.size()==0){
                Toast.makeText(getApplicationContext(),"No device Found",Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress= info.groupOwnerAddress;

            if(info.groupFormed && info.isGroupOwner){
                connectionStatus.setText("HOST");
                Log.d("ConnInfoListener", "I am GO");
                if(!serverCreated) {
                    serverClass = new ServerClass();
                    serverClass.start();
                    serverCreated = true;
                }
            }
            else if(info.groupFormed ){
                connectionStatus.setText("Client");
                clientClass=new ClientClass(groupOwnerAddress);
                clientClass.start();
                serverCreated = false;
            }
            else{
                groupCreated = false;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerClass extends  Thread{
        Socket socket;
        ServerSocket serverSocket;
        public ServerClass(){
            sendReceiveArrayList = new ArrayList<SendReceive>();
            sendReceiveHashMap = new HashMap<InetAddress, SendReceive>();
        }
        @Override
        public  void run(){
            try {
                serverSocket=new ServerSocket(2323);
                serverSocket.setReuseAddress(true);
                while(true) {
                    Log.d("ServerClass", "run() listening to connections");
                    socket = serverSocket.accept();
                    socket.setKeepAlive(true);
                    Log.d("ServerClass", "run() accepted connection from "+socket.getInetAddress().getHostName());
                    sendReceive = new SendReceive(socket);
                    sendReceiveArrayList.add(sendReceive);
                    if(!sendReceiveHashMap.containsKey(socket.getInetAddress())) {
                        sendReceiveHashMap.put(socket.getInetAddress(), sendReceive);
                    }
                    else{
                        try{
                            socket.close();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                        finally {
                            sendReceiveHashMap.remove(socket.getInetAddress());
                            sendReceiveHashMap.put(socket.getInetAddress(), sendReceive);
                        }

                    }
                    Log.d("SendReceive Size", String.valueOf(sendReceiveHashMap.size()));
                    Log.d("ServerClass", "run() added client to sendReceiveHashMap");
                    sendReceive.start();
                }
            }catch(SocketException se){
                se.printStackTrace();
                sendReceiveHashMap.remove(socket.getInetAddress());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    private  class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket socket)  {
            this.socket=socket;
            try {
                inputStream=this.socket.getInputStream();
                outputStream=this.socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            byte[] buffer=new byte[1024];
            int bytes;

            while(socket!=null){
                try {
                    bytes=inputStream.read(buffer);
                    if(bytes>0){
                        Log.d("MessageReceived", "from "  + socket.getInetAddress().getHostAddress());
                        handler.obtainMessage(MESSAGE_READ,bytes,-1 ,buffer).sendToTarget();
                        if(serverCreated) {
                            Log.d("Forwarding", "Start forwarding messages because I'm the GO");
                            for (SendReceive sendReceiveDevice : sendReceiveHashMap.values()) {
                                if (sendReceiveDevice != this) {
                                    Log.d("Forwarding Message", "from "+socket.getInetAddress().getHostAddress()+
                                            " to "+sendReceiveDevice.socket.getInetAddress().getHostAddress());
                                    sendReceiveDevice.write(buffer);
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public  class ClientClass extends Thread{
        Socket socket;
        String hostAdd;


        public ClientClass(InetAddress hostAddress)
        {
            hostAdd=hostAddress.getHostAddress();
            socket=new Socket();
        }

        @Override
        public void run() {
            try {
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress(hostAdd,2323),500);
                sendReceive =new SendReceive(socket);
                Log.d("ClientClass", "run() sendReceive Object Created");
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
