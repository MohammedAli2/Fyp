package com.fypvc.moh.fypwithvc;

import android.support.v7.app.AppCompatActivity;
//import android.os.Build;
//import android.os.VibrationEffect;
//import android.os.Vibrator;
import android.os.Bundle;
import android.os.Handler;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;


import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;



public class MainActivity extends AppCompatActivity {


    Button btnVibrationOn, btnVibrationOff;
    BluetoothAdapter bluetoothAdapter;
    String address;
    Set<BluetoothDevice> pairedDevices;
    BluetoothSocket btSocket;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    TextView Input1TxtView;     //for pm
    TextView Input2TxtView;
    TextView Input3TxtView;             //added new

    private Handler InputHandler = new Handler();
    Button btnStartThread;
    Button btnStoptThread;


    Button btnCalibrateMic;//adding new

    private String inputA = null;
    private String inputB = null;

    private String inputC = null;       //added new

    private volatile int inputAint = 0;
    private volatile int inputBint = 0;



    //added new
    private volatile String mic1AverageStr;
    private volatile String mic2AverageStr;
    private volatile String mic3AverageStr;
    private volatile int mic1Average;
    private volatile int mic2Average;
    private volatile int mic3Average;


    private volatile boolean stopThread = false;        //volatiole means that it always gives this variables most upto data value - not cached value




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnCalibrateMic = (Button) findViewById(R.id.calibtrateBtn); //adding new

        btnVibrationOn = (Button) findViewById(R.id.btnVibration_On);
        btnVibrationOff = (Button) findViewById(R.id.btnVibration_Off);

        Input1TxtView = (TextView) findViewById(R.id.input1); // for pm
        Input2TxtView = (TextView) findViewById(R.id.input2);
        Input3TxtView = (TextView) findViewById(R.id.input3);           //added new

        btnStartThread = (Button) findViewById(R.id.btnThread_Start);
        btnStoptThread = (Button) findViewById(R.id.btnThread_Stop);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        address = bluetoothAdapter.getAddress();

        pairedDevices = bluetoothAdapter.getBondedDevices();

        address = "00:14:03:06:73:25";

        BluetoothDevice arduinoModule = bluetoothAdapter.getRemoteDevice(address);


        try {
            btSocket = arduinoModule.createInsecureRfcommSocketToServiceRecord(myUUID);
            System.out.println("BTSOCKET WORKING");

        } catch (IOException e) {
            System.out.println("BTSOCKET NOT WORKING");
        }


        try {

            btSocket.connect();
            System.out.println("BT CONNECTING");

        } catch (IOException e) {
            System.out.println("BT NOT CONNECTING");
        }


        if(bluetoothAdapter == null)
        {
            Toast.makeText(MainActivity.this, "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }


        btnVibrationOn.setOnClickListener(new View.OnClickListener() {      //moving this to its own proc -- this failed
            @Override
            public void onClick(View v) {
                String on = "b";
                setVibrationOnOff(on);
            }
        });

        btnVibrationOff.setOnClickListener(new View.OnClickListener() {         //moving this to its own proc
            @Override
            public void onClick(View v) {
                String off = "c";
                setVibrationOnOff(off);
            }
        });




        btnStartThread.setOnClickListener(new View.OnClickListener() {         //for pm
            @Override
            public void onClick(View v) {
                stopThread = false;

                //experimental code             --make arduino only out put to monitor when start thread is pressed
                try{
                    btSocket.getOutputStream().write("a".getBytes());
                    System.out.println("a");

                } catch (IOException e){ System.out.println("COULD NOT SEND INSTRUCTION");}
                //end of experimental



                startThread();
            }
        });

        btnStoptThread.setOnClickListener(new View.OnClickListener() {         //for pm
            @Override
            public void onClick(View v) {

                stopThread = true;

                //experimental code             --make arduino only out put to monitor when start thread is pressed
                try{
                    btSocket.getOutputStream().write("z".getBytes());
                    System.out.println("z");


                } catch (IOException e){ System.out.println("COULD NOT SEND INSTRUCTION");}
                //end of experimental
            }
        });



        //sdding new - gets averages for each mic
        btnCalibrateMic.setOnClickListener(new View.OnClickListener() {         //for pm
            @Override
            public void onClick(View v) {



                try{

                    btSocket.getOutputStream().write("y".getBytes());       //SENDS y command to arduino
                    System.out.println("y");
                    try{
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e){ System.out.println("1 Second pause failed ");}

                    //adding new
                    int bytes;
                    InputStream tmpIn = null;
                    tmpIn = btSocket.getInputStream();
                    DataInputStream mmInstream = new DataInputStream(tmpIn);
                    bytes = mmInstream.available(); // 344 test
                    byte[] rawBytes = new byte[bytes];
                    mmInstream.read(rawBytes);
                    final String string = new String(rawBytes, "UTF-8");

                    for (int x = 0; x < string.length(); x++)  //adding new
                    {
                        if (string.charAt(x) == 'P')            //b for inpur 2
                        {
                            int valueDigits = 4;

                            while (string.charAt(x + valueDigits) == '\r' || string.charAt(x + valueDigits) == '\n')
                            {
                                valueDigits -= 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                mic1AverageStr = string.substring(x + 1, x + 1 + valueDigits);         // to avoid first letter
                            }

                            System.out.println("mic 1 average " + mic1AverageStr);
                        }



                        if (string.charAt(x) == 'L')            //b for inpur 2
                        {
                            int valueDigits = 4;

                            while (string.charAt(x + valueDigits) == '\r' || string.charAt(x + valueDigits) == '\n')
                            {
                                valueDigits -= 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                mic2AverageStr = string.substring(x + 1, x + 1 + valueDigits);         // to avoid first letter
                            }

                            System.out.println("mic 2 average " + mic2AverageStr);
                        }

                        if (string.charAt(x) == 'M')            //b for inpur 2
                        {
                            int valueDigits = 4;

                            while (string.charAt(x + valueDigits) == '\r' || string.charAt(x + valueDigits) == '\n')
                            {
                                valueDigits -= 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                mic3AverageStr = string.substring(x + 1, x + 1 + valueDigits);         // to avoid first letter
                            }

                            System.out.println("mic 3 average " + mic3AverageStr);
                        }

                    }



                    mic1Average = Integer.parseInt(mic1AverageStr);
                    mic2Average = Integer.parseInt(mic2AverageStr);
                    mic3Average = Integer.parseInt(mic3AverageStr);

                } catch (IOException e){ System.out.println("COULD NOT SEND INSTRUCTION");}
                //end of experimental
            }
        });



    }




    private void setVibrationOnOff(String n){
        if(n == "b")
        {
            try{
                btSocket.getOutputStream().write(n.getBytes());
                System.out.println(n);

            } catch (IOException e){ System.out.println("COULD NOT SEND ON INSTRUCTION");}

        }
        else if(n == "c")
        {
            try{
                btSocket.getOutputStream().write(n.toString().getBytes());
                System.out.println(n);

            } catch (IOException e){System.out.println("COULD NOT SEND ON INSTRUCTION"); }

        }

    }


    public void startThread(){
        ThreadForPotentiometer runnable = new ThreadForPotentiometer();
        new Thread(runnable).start();
    }


    // creating background thread to keep getting potentiometer values
    class ThreadForPotentiometer implements Runnable
    {
        @Override
        public void run()
        {


            if (stopThread)
            {
                return;
            }


            while (true)
            {

                if (stopThread)
                {
                    return;
                }


                try {

                    // byte[] buffer = new byte[4 * 1024];
                    int bytes;
                    InputStream tmpIn = null;
                    tmpIn = btSocket.getInputStream();
                    DataInputStream mmInstream = new DataInputStream(tmpIn);
                    bytes = mmInstream.available(); // 344 test
                    byte[] rawBytes = new byte[bytes];
                    mmInstream.read(rawBytes);
                    final String string = new String(rawBytes, "UTF-8");


                    String input1StrValue = "";
                    String input2StrValue = "";

                    String input3StrValue = "";     //adding new
                    String mic1MaxStrValue = "";     //adding new

                    for (int x = 0; x < string.length(); x++)  //adding new
                    {

                        /*if (string.charAt(x) == 'E')             //E for mic1's max
                        {

                            int valueDigits = 4;

                            while (string.charAt(x + valueDigits) == '\r' || string.charAt(x + valueDigits) == '\n')        //makes sure it gets the input 1 value only
                            {
                                valueDigits -= 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                mic1MaxStrValue = string.substring(x + 1, x + 1 + valueDigits);         // to avoid first letter
                            }

                            //System.out.println(mic1MaxStrValue);
                           //System.out.println("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" + mic1MaxStrValue);
                        }*/


                        if (string.charAt(x) == 'A')             //a for inpur 1
                        {

                            int valueDigits = 4;

                            while (string.charAt(x + valueDigits) == '\r' || string.charAt(x + valueDigits) == '\n')        //makes sure it gets the input 1 value only
                            {
                                valueDigits -= 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                input1StrValue = string.substring(x + 1, x + 1 + valueDigits);         // to avoid first letter
                            }

                            //System.out.println(input1StrValue);

                            if(Integer.parseInt(input1StrValue) <= (mic1Average - 10))                      //testing if min will help me determine which mic pics up value first because max wont help me with this
                            {
                                System.out.println("1 " + input1StrValue);
                            }

                            if(Integer.parseInt(input1StrValue) >= (mic1Average + 5))
                            {
                                System.out.println(input1StrValue);
                            }

                            if(Integer.parseInt(input1StrValue) >= (mic1Average + 5))
                            {
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1" + input1StrValue);

                            }


                            /*if(Integer.parseInt(input1StrValue) > 535)        //experimental
                            {
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + input1StrValue);
                                shakeItBaby();

                            }*/

                           /* if(mic1MaxStrValue == input1StrValue)       // adding new
                            {
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            }*/

                        }


                        if (string.charAt(x) == 'B')            //b for inpur 2
                        {
                            int valueDigits = 4;

                            while (string.charAt(x + valueDigits) == '\r' || string.charAt(x + valueDigits) == '\n')
                            {
                                valueDigits -= 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                input2StrValue = string.substring(x + 1, x + 1 + valueDigits);         // to avoid first letter
                            }

                            //System.out.println(input2StrValue);
                            if(Integer.parseInt(input2StrValue) <= (mic2Average - 10))
                            {
                                System.out.println("2 " + input2StrValue);
                            }

                            if(Integer.parseInt(input2StrValue) >= (mic2Average + 5))
                            {
                                System.out.println(input2StrValue);
                            }

                            if(Integer.parseInt(input2StrValue) >= (mic2Average + 5))
                            {
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!2" + input2StrValue);

                            }
                        }


                        if (string.charAt(x) == 'C')            //b for inpur 2
                        {
                            int valueDigits = 4;

                            while (string.charAt(x + valueDigits) == '\r' || string.charAt(x + valueDigits) == '\n')
                            {
                                valueDigits -= 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                input3StrValue = string.substring(x + 1, x + 1 + valueDigits);         // to avoid first letter
                            }

                            //System.out.println(input3StrValue);
                            if(Integer.parseInt(input3StrValue) <= (mic3Average - 10))
                            {
                                System.out.println("3 " + input3StrValue);
                            }

                            if(Integer.parseInt(input3StrValue) >= (mic3Average + 5))
                            {
                                System.out.println(input3StrValue);
                            }

                            if(Integer.parseInt(input3StrValue) >= (mic3Average + 5))
                            {
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!3" + input3StrValue);

                            }
                        }

                        inputA = input1StrValue;
                        inputB = input2StrValue;
                        inputC = input3StrValue;      //added new


                        InputHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Input1TxtView.setText(inputA);
                                // Input2TxtView.setText(inputB);
                            }
                        });


                        InputHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Input2TxtView.setText(inputB);
                            }
                        });

                        InputHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Input3TxtView.setText(inputC);
                            }
                        });

                    }


                } catch (Exception e) {}

            }


        }
    }





}
