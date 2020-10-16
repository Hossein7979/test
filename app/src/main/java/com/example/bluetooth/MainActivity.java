package com.example.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ImageButton check;
    ListView view;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[]devices;
    private final int CHECK_STATUS=1;
    String App_NAME="my_Bluetooth";
    UUID uuid=UUID.fromString("d4505eec-0192-11eb-adc1-0242ac120002");
    EditText text;
    final int SEND_MESSAGE=2;
    sendReceive receive;
    Client client;
    //commit


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(bluetoothAdapter.isEnabled()){

                    bluetoothAdapter.disable();
                    check.setImageResource(R.drawable.blueon);
                    showToast("Now Bluetooth is Off");
                }
                else {
                    Intent enable=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enable,CHECK_STATUS);
                    check.setImageResource(R.drawable.blueoff);
                    showToast("Now Bluetooth is On");
                }
            }
        });



        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                try {
                    client=new Client(devices[position]);
                    client.start();
                    showToast("Connected to bluetooth");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {//hamoon OnActivityResult mibashad

            byte[] writeBuf=(byte[])msg.obj;
            int begin=(int)msg.arg1;
            int end=(int)msg.arg2;

            switch (msg.what){
                case CHECK_STATUS :
                    showToast("checking");
                    break;
                case SEND_MESSAGE:
                String writeMessage=new String(writeBuf);
                writeMessage=writeMessage.substring(begin,end);
                break;
            }
            return  true;
        }
    });



    private void showToast(String str) {

        Toast.makeText(getApplicationContext(),str,Toast.LENGTH_LONG).show();
    }


    private void init() {

        check=findViewById(R.id.ibBlue);
        view=findViewById(R.id.lvBlue);
        text=findViewById(R.id.etMessage);
    }


    public void showList(View v) {

        scanable();//do scanable devise for other device

        Set<BluetoothDevice>device=null;
        if(bluetoothAdapter!=null){

        device = bluetoothAdapter.getBondedDevices();

        }else
            showToast("BluetoothAdapter is null!");
        String[] deviceNames = new String[device.size()];
        int i = 0;
        devices=new BluetoothDevice[device.size()];

        if(device.size()>0&&bluetoothAdapter.isEnabled()) {
            for (BluetoothDevice bd : device) {
                devices[i] = bd;
                deviceNames[i] = bd.getName();
                i++;
            }

            ArrayAdapter<String> lists = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames);
            view.setAdapter(lists);
        }

        else
            showToast("Device not found ");


    }

    private void scanable() {

        Intent scanable=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(scanable,2);
        Toast.makeText(getApplicationContext(),"Your device now Scanable",Toast.LENGTH_LONG).show();
    }

    private class serverClass extends Thread{

        BluetoothServerSocket socket;
        public serverClass() throws IOException {
            socket=bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(App_NAME,uuid);
        }
        public void run(){

            BluetoothSocket bluetoothSocket=null;

            while(bluetoothSocket==null){
                try {
                    bluetoothSocket=socket.accept();
                    showToast("Successfully Connected to server!");
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Connection failed");
                }
                if(bluetoothSocket!=null){
                    try {
                        receive=new sendReceive(bluetoothSocket);
                        receive.start();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    private class Client extends Thread{

        BluetoothSocket socket;
        BluetoothDevice device;
        public Client(BluetoothDevice Device) throws IOException {
            device=Device;
            socket=device.createRfcommSocketToServiceRecord(uuid);
        }
        public void run(){

            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
                showToast("Connected to Client");

                receive=new sendReceive(socket);
                receive.start();

            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
                showToast("Connected failed to client");
            }
        }
        public void cancel() throws IOException {
            socket.close();
        }
    }

    private class sendReceive extends Thread{

        BluetoothSocket bluetoothSocket;
        InputStream inputStream;
        OutputStream outputStream;

        public  sendReceive(BluetoothSocket socket) throws IOException {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=socket.getInputStream();
                tempOut=socket.getOutputStream();
            }catch (IOException io){
                io.printStackTrace();
            }
            inputStream=tempIn;
            outputStream=tempOut;
        }

        public sendReceive() {

        }

        public void run(){
            byte[]buffer=new byte[1024];
            int bytes=0;
            int begin=0;

            while(true){
                try {
                    bytes+=inputStream.read(buffer,bytes,buffer.length-bytes);
                   for(int i=begin;i<bytes;i++){
                       if(buffer[i]=="#".getBytes()[0]){
                           handler.obtainMessage(SEND_MESSAGE,begin,i,buffer).sendToTarget();
                           begin=i+1;
                           if(i==bytes-1){
                               bytes=0;
                               begin=0;
                           }
                       }
                   }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[]bytes) throws IOException {

            showToast("write metttooooood");
            outputStream.write(bytes);
        }
        public void cancel(){
            try {
                bluetoothSocket.close();
            }catch (IOException i){
                i.printStackTrace();
            }
        }
    }
    public void connecting(View v) throws IOException {

        String str=text.getText().toString();

        System.out.println(str);
        receive.write(str.getBytes());
    }
    public void conToServer(View v) throws IOException {
        serverClass obj=new serverClass();
        obj.start();
    }

}
//this test for github
