package com.blogspot.justsimpleinfo.localsocket;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerActivity extends AppCompatActivity implements View.OnClickListener {


    private CommunicationThread commThread;
    private ServerSocket serverSocket;
    private Socket mClientSocket;

    Handler updateConversationHandler;
    Thread serverThread = null;

    private ArrayList<String> mMessages;
    private ListView mServerMessageListview;
    private Button mServerSendMessageBtn;
    private EditText mServerMessageInput;
    private TextView mIPTextView;

    public static final int SERVERPORT = 6000;

    WifiManager.WifiLock mWifiLock;
    PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();
        mServerSendMessageBtn = (Button) this.findViewById(R.id.server_send_message_btn);
        mServerSendMessageBtn.setOnClickListener(this);

        mServerMessageInput = (EditText) this.findViewById(R.id.server_message_input);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        mIPTextView = (TextView) findViewById(R.id.ip_address_textview);
        mIPTextView.setText("IP : "+ip);

        mMessages = new ArrayList<>();
        mServerMessageListview = (ListView) findViewById(R.id.server_message_listview);
        mServerMessageListview.setAdapter(new CustomAdapter(mMessages));

        updateConversationHandler = new Handler();


        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();


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
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {

        try {
            if (mClientSocket.isConnected()) {

                String message = mServerMessageInput.getText().toString();

                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream())), true);
                out.println(message);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    class CustomAdapter extends ArrayAdapter {

        public CustomAdapter(ArrayList<String> messages) {
            super(getApplicationContext(), android.R.layout.simple_list_item_1,messages);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String message = (String) this.getItem(position);
            TextView textView = (TextView) convertView;

            if(textView == null){

                textView = (TextView) LayoutInflater.from(ServerActivity.this).inflate(android.R.layout.simple_list_item_1,null,false);
            }

            textView.setText(message);


            return textView;
        }
    }


    class ServerThread implements Runnable {

        public void run() {

            try {
                serverSocket = new ServerSocket(SERVERPORT);
                serverSocket.setSoTimeout(20000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted() ) {

                try {

                    if(!serverSocket.isClosed()){

                        mClientSocket = serverSocket.accept();

                        Log.e("host name",mClientSocket.getInetAddress().getCanonicalHostName()+"");
                        Log.e("host name 1",mClientSocket.getInetAddress().getHostName()+"");
                        if(commThread == null){

                            commThread = new CommunicationThread(mClientSocket);
                            new Thread(commThread).start();

                        }
                    }



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            if(this.clientSocket.isClosed()){

                return;
            }
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

                    updateConversationHandler.post(new UpdateMessageUI(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class UpdateMessageUI implements Runnable {
        private String msg;

        public UpdateMessageUI(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            mMessages.add(msg);
            ArrayAdapter arrayAdapter = (ArrayAdapter) mServerMessageListview.getAdapter();
            arrayAdapter.notifyDataSetChanged();



        }
    }

    /**
     * for auto discovery
     */
    private static class DiscoveryThread implements Runnable {

        DatagramSocket socket;

        @Override
        public void run() {

            try {
                //Keep a socket open to listen to all the UDP trafic that is destined for this port
                socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
                socket.setBroadcast(true);

                while (true) {
                    System.out.println(getClass().getName() + ">>>Ready to receive broadcast packets!");

                    //Receive a packet
                    byte[] recvBuf = new byte[15000];
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(packet);

                    //Packet received
                    System.out.println(getClass().getName() + ">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                    System.out.println(getClass().getName() + ">>>Packet received; data: " + new String(packet.getData()));

                    //See if the packet holds the right command (message)
                    String message = new String(packet.getData()).trim();
                    if (message.equals("DISCOVER_FUIFSERVER_REQUEST")) {
                        byte[] sendData = "DISCOVER_FUIFSERVER_RESPONSE".getBytes();

                        //Send a response
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                        socket.send(sendPacket);

                        System.out.println(getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                //Logger.getLogger(DiscoveryThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public static DiscoveryThread getInstance() {
            return DiscoveryThreadHolder.INSTANCE;
        }

        private static class DiscoveryThreadHolder {

            private static final DiscoveryThread INSTANCE = new DiscoveryThread();
        }

    }

}
