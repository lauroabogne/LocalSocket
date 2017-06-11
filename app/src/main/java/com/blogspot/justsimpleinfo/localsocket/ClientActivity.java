package com.blogspot.justsimpleinfo.localsocket;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

public class ClientActivity extends AppCompatActivity implements View.OnClickListener,Runnable{

    private Socket socket;

    private   int SERVERPORT = ServerActivity.SERVERPORT;
    private  String SERVER_IP = "0.0.0.0";


    Button mClientConnectButton;
    Button mClientSendButton;
    EditText mClientServerIPInputEditText;
    EditText mClientMessageInputEditText;

    ArrayList<String> mMessages;
    ListView mClientMessageListView;



    Handler updateConversationHandler;


    WifiManager.WifiLock mWifiLock;
    PowerManager.WakeLock mWakeLock;


    boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        init();

        mClientConnectButton = (Button) findViewById(R.id.client_connent_btn);
        mClientConnectButton.setOnClickListener(this);

        mClientSendButton = (Button) findViewById(R.id.client_send_btn);
        mClientSendButton.setOnClickListener(this);

        mClientServerIPInputEditText = (EditText) findViewById(R.id.client_server_ip_input);
        mClientMessageInputEditText = (EditText) findViewById(R.id.client_message_input);

        mMessages = new ArrayList<>();

        mClientMessageListView = (ListView) findViewById(R.id.client_server_message_listview);

        mClientMessageListView.setAdapter(new CustomAdapter(mMessages));

        InetAddress serverAddr = null;


        updateConversationHandler = new Handler();





    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mWifiLock.release();
        mWakeLock.release();
    }

    private void init(){
        WifiManager wMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mWifiLock = wMgr.createWifiLock(WifiManager.WIFI_MODE_FULL, MainActivity.WIFI_AND_POWER_LOCK_TAG);
        mWifiLock.acquire();

        PowerManager pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,MainActivity.WIFI_AND_POWER_LOCK_TAG);
        mWakeLock.acquire();
    }

    @Override
    public void onClick(View view) {

        Log.e("xxxxxxxxxxxxxxxxx","xxxxxxxxxxxxx");
        if(1==1 && !isConnected){

            Log.e("yyyyyyyyyyy","yyyyyyyyyy");

            Thread discoveryThread = new Thread(this);
            discoveryThread.start();

            return;
        }
        int id = view.getId();
        switch (id){
            case R.id.client_connent_btn:

                String serverIp = mClientServerIPInputEditText.getText().toString().trim();
                SERVER_IP = serverIp;


                new Thread(new ClientThread()).start();

                break;

            case R.id.client_send_btn:

                try {

                    String str = mClientMessageInputEditText.getText().toString();
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    out.println(str);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:
        }


    }


    DatagramSocket mDatagramSocket;

    /**
     * for server auto search
     */
    @Override
    public void run() {

        // Find the server using UDP broadcast
        try {
            //Open a random port to send the package
            mDatagramSocket = new DatagramSocket();
            mDatagramSocket.setBroadcast(true);

            byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                mDatagramSocket.send(sendPacket);
                System.out.println(getClass().getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
            }

            // Broadcast the message over all the network interfaces
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        mDatagramSocket.send(sendPacket);
                    } catch (Exception e) {
                    }

                    System.out.println(getClass().getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            mDatagramSocket.receive(receivePacket);

            //We have a response
            System.out.println(getClass().getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();

            if (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) {
                //DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
                //SERVERPORT = receivePacket.getPort();
                SERVER_IP = receivePacket.getAddress().toString().replace("/","");

                new Thread(new ClientThread()).start();
                Log.e("==========found======","==========found====== "+receivePacket.getAddress()+" "+receivePacket.getSocketAddress()+ " "+receivePacket.getPort());

            }

            //Close the port!
            mDatagramSocket.close();
        } catch (IOException ex) {

            ex.printStackTrace();
        }



}

    class CustomAdapter extends ArrayAdapter{

        public CustomAdapter(ArrayList<String> messages) {
            super(getApplicationContext(), android.R.layout.simple_list_item_1,messages);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String message = (String) this.getItem(position);
            TextView textView = (TextView) convertView;

            if(textView == null){

                textView = (TextView) LayoutInflater.from(ClientActivity.this).inflate(android.R.layout.simple_list_item_1,null,false);
            }

            textView.setText(message);


            return textView;
        }
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(ClientActivity.this.SERVER_IP);

                socket = new Socket(serverAddr, SERVERPORT);

                isConnected = true;
                Log.e("client connected","client connected");
                ClientActivity.CommunicationThread commThread = new ClientActivity.CommunicationThread(socket);
                new Thread(commThread).start();

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String read = input.readLine();

                    updateConversationHandler.post(new UpdateMessageUi(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class UpdateMessageUi implements Runnable {
        private String msg;

        public UpdateMessageUi(String str) {
            this.msg = str;
        }

        @Override
        public void run() {

            mMessages.add(msg);
            ArrayAdapter arrayAdapter = (ArrayAdapter) mClientMessageListView.getAdapter();
            arrayAdapter.notifyDataSetChanged();



        }
    }


}

