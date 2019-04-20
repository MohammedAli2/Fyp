package com.fypvc.moh.fypwithvc;

import android.content.BroadcastReceiver;
import android.media.Image;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;




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


    private volatile ArrayList<arrayOfMicResults> micTimesList = new ArrayList<arrayOfMicResults>();        //t3

    private volatile double AB = 0;
    private volatile double AC = 0;
    private volatile double BC = 0;

    private ArrayList<ImageView> directionalIndicators = new ArrayList<ImageView>();


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


        directionalIndicators.add( (ImageView) findViewById(R.id.right)  ); //adding direction indicators images to an array //0
        directionalIndicators.add( (ImageView) findViewById(R.id.rightMiddleFrontRight)  );     //1
        directionalIndicators.add( (ImageView) findViewById(R.id.frontRight) );                 //2
        directionalIndicators.add( (ImageView) findViewById(R.id.frontMiddleFrontRight)  );     //3
        directionalIndicators.add( (ImageView) findViewById(R.id.front)  );                     //4
        directionalIndicators.add( (ImageView) findViewById(R.id.frontLeftMiddleFront)  );      //5
        directionalIndicators.add( (ImageView) findViewById(R.id.frontLeft)  );                 //6
        directionalIndicators.add( (ImageView) findViewById(R.id.leftMiddleFrontLeft)  );       //7
        directionalIndicators.add( (ImageView) findViewById(R.id.left)  );                      //8
        directionalIndicators.add( (ImageView) findViewById(R.id.leftMiddleBackLeft)  );        //9
        directionalIndicators.add( (ImageView) findViewById(R.id.backLeft)  );                  //10
        directionalIndicators.add( (ImageView) findViewById(R.id.backMiddleBackLeft)  );        //11
        directionalIndicators.add( (ImageView) findViewById(R.id.back)  );                      //12
        directionalIndicators.add( (ImageView) findViewById(R.id.backMiddleBackRight)  );       //13
        directionalIndicators.add( (ImageView) findViewById(R.id.backRight)  );                 //14
        directionalIndicators.add( (ImageView) findViewById(R.id.rightMiddleBackRight)  );      //15
        directionalIndicators.add( (ImageView) findViewById(R.id.right)  );                     //16   --adding again yes



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
        /*ThreadForPotentiometer runnable = new ThreadForPotentiometer();               //having 2 threads running impacts read performance/accuracy
        new Thread(runnable).start();*/

       // ThreadForTDOA1 getTimeStartEndETiems = new ThreadForTDOA1();            //ADDED NEW VC1 - for TDOA calculations
       // new Thread(getTimeStartEndETiems).start();

        ThreadForTDOA2 getTimeStartEndETimes1 = new ThreadForTDOA2();            //ADDED NEW VC1 - for TDOA calculations
        new Thread(getTimeStartEndETimes1).start();

        /*ThreadForTDOA3 getTimeStartEndETimes2 = new ThreadForTDOA3();            //ADDED NEW VC1 - for TDOA calculations
        new Thread(getTimeStartEndETimes2).start();

        ThreadForTDOA4 getTimeStartEndETimes3 = new ThreadForTDOA4();            //ADDED NEW VC1 - for TDOA calculations
        new Thread(getTimeStartEndETimes3).start();*/

    }



    class ThreadForTDOA1 implements Runnable                       //ADDED NEW VC1 - for TDOA calculations
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

                    //byte[] buffer = new byte[4 * 1024];
                    //int bytes;
                    /*InputStream tmpIn = null;                                      CURRENTLY BEST WORK
                    tmpIn = btSocket.getInputStream();
                    DataInputStream mmInstream = new DataInputStream(tmpIn);
                    //bytes = mmInstream.available(); // 344 test
                    byte[] rawBytes = new byte[4*1024];
                    mmInstream.read(rawBytes);
                    final String string = new String(rawBytes, "UTF-8");*/

                    /*InputStream tmpIn = null;
                    tmpIn = btSocket.getInputStream();
                    DataInputStream mmInstream = new DataInputStream(tmpIn);
                    //bytes = mmInstream.available(); // 344 test
                    byte[] rawBytes = new byte[4*1024];
                    mmInstream.read(rawBytes);
                    final String string = new String(rawBytes, "UTF-8");
                   // mmInstream.close();
                    //System.out.println(string);*/


                   // Thread.sleep(100); //this may actuall improve reading accuracy

 /*                   byte[] buffer = new byte[512];
                    int bytes;
                    InputStream tmpIn = null;
                    tmpIn = btSocket.getInputStream();
                    DataInputStream mmInStream = new DataInputStream(tmpIn);
                    bytes = mmInStream.read(buffer);
                    String string = new String(buffer, 0, bytes);
*/
                    final String string;
                    InputStream inputStream = btSocket.getInputStream();

                    int byteCount = inputStream.available();
                    if(byteCount > 0)
                    {
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);
                        string=new String(rawBytes,"UTF-8");
                        System.out.println("test " + string);
                    }
                    else
                    {
                        string = "";
                    }

                    String startTimeForMica = "";     //adding new
                    String endTimeForMicb = "";     //adding new
                    String difference = "";     //adding new

                    String micQtime = "";
                    String micRtime = "";
                    String micStime = "";

                    String timeOrder = "";



                    for (int x = 0; x < string.length(); x++)  //adding new
                    {

                          if(string.charAt(x) == 'Q')
                          {
                              int valueDigits = 1;

                              while (string.charAt(x + valueDigits) != 'e')
                              {
                                  //char dis = string.charAt(x + valueDigits);
                                  valueDigits += 1;

                              }



                              if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                              {
                                  micQtime = string.substring(x + 1, x + valueDigits);
                              }


                              System.out.println("Q " + micQtime);

                            //  System.out.println( string.substring(1, string.length()));
                              System.out.println(string);
                          }

                        if(string.charAt(x) == 'R')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                //char dis = string.charAt(x + valueDigits);
                                valueDigits += 1;

                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                micRtime = string.substring(x + 1, x + valueDigits);
                            }


                            System.out.println("R " + micRtime);

                        }

                        if(string.charAt(x) == 'S')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                //char dis = string.charAt(x + valueDigits);
                                valueDigits += 1;

                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                micStime = string.substring(x + 1, x + valueDigits);
                            }


                            System.out.println("S " + micStime);

                        }

                        if(string.charAt(x) == 'r')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                //char dis = string.charAt(x + valueDigits);
                                valueDigits += 1;

                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                timeOrder = string.substring(x + 1, x + valueDigits);
                            }

                            System.out.println("r " + timeOrder);

                        }
                    }


                    if (Integer.parseInt(micQtime) == 0 && Integer.parseInt(micRtime) == 0 && Integer.parseInt(micStime) == 0)
                    {
                        //do nothing cause no useful values

                    }
                    else  //calulate degrees for each
                    {
                        int distanceCm = 20;//cm;//30; //cm
                        float distanceFt =  (((float)distanceCm) / (float)30.48); //fr
                        float speedOfSoundFt = 1125;

                        if (Integer.parseInt(micQtime) == 0)
                        {
                            micQtime = null;
                        }
                        else
                        {
                            float timeDelayInMicroSeconds = Integer.parseInt(micQtime);
                            float microToSeconds = (timeDelayInMicroSeconds / 1000000);
                            float distanceSoudnIsDifferennt =  (microToSeconds * speedOfSoundFt);
                            float adjDivHyp = distanceSoudnIsDifferennt / distanceFt;
                            float rad = (float)Math.acos(adjDivHyp);
                            //double rad = Math.acos((Integer.parseInt(micQtime) * speedOfSoundFt) / distanceFt);
                            double deg = Math.toDegrees(rad);
                            AB = deg;
                            System.out.println("Angle between m1 and m2 = " + deg);
                        }

                        if (Integer.parseInt(micRtime) == 0)
                        {
                            micRtime = null;
                        }
                        else
                        {


                            float timeDelayInMicroSeconds = Integer.parseInt(micRtime);
                            float microToSeconds = (timeDelayInMicroSeconds / 1000000);
                            float distanceSoudnIsDifferennt =  (microToSeconds * speedOfSoundFt);
                            float adjDivHyp = distanceSoudnIsDifferennt / distanceFt;
                            float rad = (float)Math.acos(adjDivHyp);
                            //double rad = Math.acos((Integer.parseInt(micQtime) * speedOfSoundFt) / distanceFt);
                            double deg = Math.toDegrees(rad);
                            AC = deg;

                            System.out.println("Angle between m1 and m3 = " + deg);
                        }

                        if (Integer.parseInt(micStime) == 0)
                        {
                            micStime = null;
                        }
                        else
                        {
                            float timeDelayInMicroSeconds = Integer.parseInt(micStime);
                            float microToSeconds = (timeDelayInMicroSeconds / 1000000);
                            float distanceSoudnIsDifferennt =  (microToSeconds * speedOfSoundFt);
                            float adjDivHyp = distanceSoudnIsDifferennt / distanceFt;
                            float rad = (float)Math.acos(adjDivHyp);
                            //double rad = Math.acos((Integer.parseInt(micQtime) * speedOfSoundFt) / distanceFt);
                            double deg = Math.toDegrees(rad);
                            BC = deg;

                            System.out.println("Angle between m2 and m3 = " + deg);
                        }



                        int singleDirection = -1;
                        double ABout = 0;
                        double BCin = 0;
                        double ACin = 0;

                        double ACout = 0;
                        double CB = 0;
                        double CBin = 0;
                        double ABin = 0;

                        double BA = 0;
                        double BAout = 0;

                        double BCout = 0;
                        double BAin = 0;
                        double CA = 0;
                        double CAin = 0;

                        double CAout = 0;

                        double CBout = 0;


                        switch (timeOrder)
                        {
                            case "ABC":
                                if(AB > 60)
                                {
                                    ABout = 210;
                                }
                                else {
                                    ABout = (AB * 2) + 90;
                                }
                                //BCin = ((((BC - 30) * 2)  + 30) + 90); changing
                                BCin = 210 - (BC * 2);
                                ACin = ((AC * 2) + 90);

                                System.out.println("AB out " + ABout);
                                System.out.println("BCin " + BCin);
                                System.out.println("ACin " + ACin);

                                makeDirectionAllEffects(ABout, BCin, ACin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "ACB":
                                //adding boundary
                                if(AC > 60)
                                {
                                    ACout = 330;
                                }
                                else if(AC > 46 && AC < 61)
                                {
                                    ACout = (90 - (AB * 2)) + 360 ;
                                }
                                else
                                {
                                    ACout = (90 - (AB * 2)) ;
                                }


                                CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                CBin = (((CB - 30) * 2)  + 30);

                                if(CBin < 0)
                                {
                                    CBin = CBin + 360;

                                }

                                ABin = (90 - (AB * 2));

                                if(ABin < 0)
                                {
                                    ABin = ABin + 360;
                                }

                                System.out.println("ACout " + ACout);
                                System.out.println("CBin " + CBin);
                                System.out.println("ABin " + ABin);


                                makeDirectionAllEffects(ACout, CBin, ABin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();


                                break;
                            case "BAC":

                                BA = Math.abs(AB);
                                if(BA > 60)
                                {
                                    BAout = 90;
                                }
                                else {
                                    BAout = ((180 - (120 + BA)) * 2) + 90;
                                }

                                ACin = ((AC * 2) + 90);

                                //BCin = ((((BC - 30) * 2)  + 30) + 90);  //changing
                                BCin = 210 - (BC * 2);

                                System.out.println("BAout " + BAout);
                                System.out.println("BCin " + BCin);
                                System.out.println("ACin " + ACin);

                                makeDirectionAllEffects(BAout, BCin, ACin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();


                                break;
                            case "BCA":
                                if(BC > 60)
                                {
                                    BCout = 330;
                                }
                                else {
                                    BCout = (BC * 2) + 210;
                                }

                                BA = Math.abs(AB);

                                BAin = ((BA * 2) + 210);

                                if(BAin > 360)
                                {
                                    BAin = BAin - 360;
                                }



                                CA = Math.abs(AC);
                                CAin = (330 - (CA * 2));

                                System.out.println("BCout " + BCout);
                                System.out.println("BAin " + BAin);
                                System.out.println("CAin " + CAin);

                                makeDirectionAllEffects(BCout, BAin, CAin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CAB":
                                CA = Math.abs(AC);

                                if(CA > 60)
                                {
                                    CAout = 90;
                                }
                                else if(CA < 15)
                                {
                                    CAout = (90 - ((180 - (120 + CA)) * 2)) + 360;
                                }
                                else
                                {
                                    CAout = 90 - ((180 - (120 + CA)) * 2);
                                }


                                CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                CBin = (((CB - 30) * 2)  + 30);

                                if(CBin < 0)
                                {
                                    CBin = CBin + 360;

                                }

                                ABin = (90 - (AB * 2));

                                if(ABin < 0)
                                {
                                    ABin = ABin + 360;
                                }

                                System.out.println("CAout " + CAout);
                                System.out.println("CBin " + CBin);
                                System.out.println("ABin " + ABin);

                                makeDirectionAllEffects(CAout, CBin, ABin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CBA":

                                CA = Math.abs(AC);
                                CAin = (330 - (CA * 2));

                                BA = Math.abs(AB);
                                BAin = ((BA * 2) + 210);

                                if(BAin > 360)
                                {
                                    BAin = BAin - 360;
                                }

                                CB = Math.abs(BC);
                                if(CB > 60)
                                {
                                    CBout = 210;
                                }
                                else {
                                    CBout = Math.abs(((180 - (120 + CB)) * 2)) + 210;
                                }

                                System.out.println("CAin " + CAin);
                                System.out.println("BAin " + BAin);
                                System.out.println("CBout " + CBout);

                                makeDirectionAllEffects(CAin, BAin, CBout, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "AB":
                                if(AB > 60)
                                {
                                    ABout = 210;
                                }
                                else {
                                    ABout = (AB * 2) + 90;
                                }
                                System.out.println("ABout " + ABout);

                                singleDirection = (int) (ABout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();


                             break;
                            case "AC":
                                //adding boundary
                                if(AC > 60)
                                {
                                    ACout = 330;
                                }
                                else if(AC > 46 && AC < 61)
                                {
                                    ACout = (90 - (AB * 2)) + 360 ;
                                }
                                else {
                                    ACout = (90 - (AB * 2));
                                }
                                System.out.println("ACout " + ACout);

                                singleDirection = (int) (ACout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "BA":
                                if(BA > 60)
                                {
                                    BAout = 90;
                                }
                                else {
                                    BAout = ((180 - (120 + BA)) * 2) + 90;
                                }
                                System.out.println("BAout " + BAout);

                                singleDirection = (int) (BAout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CA":

                                CA = Math.abs(AC);
                                if(CA > 60)
                                {
                                    CAout = 90;
                                }
                                else if(CA < 15)
                                {
                                    CAout = (90 - ((180 - (120 + CA)) * 2)) + 360;
                                }
                                else {
                                    CAout = 90 - ((180 - (120 + CA)) * 2);
                                }


                                System.out.println("CAout " + CAout);

                                singleDirection = (int) (CAout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "BC":
                                if(BC > 60)
                                {
                                    BCout = 330;
                                }
                                else {
                                    BCout = (BC * 2) + 210;
                                }
                                System.out.println("BCout " + BCout);

                                singleDirection = (int) (BCout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CB":

                                CB = Math.abs(BC);
                                if(CB > 60)
                                {
                                    CBout = 210;
                                }
                                else {
                                    CBout = Math.abs(((180 - (120 + CB)) * 2)) + 210;
                                }

                                System.out.println("CBout " + CBout);

                                singleDirection = (int) (CBout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            default:
                                System.out.println("Do Nothing");
                        }

                        System.out.println("debuging  testing");


                    }

/*
                    if (difference != "") // ((startTimeForMica != "" && startTimeForMica != null) && (endTimeForMicb != "" && endTimeForMicb != null))
                    {

                        double distanceBetweenMics = 1.08268;       //33cm ibn feet
                        float differenceBetweenTimes =  Integer.parseInt(difference) ; // (Integer.parseInt(startTimeForMica)  - Integer.parseInt(endTimeForMicb));
                        float microSecondsToSeconds =  (differenceBetweenTimes / 1000000);
                        float secondsTimesFeetPerSec = microSecondsToSeconds * 1125;
                        float tdoa = secondsTimesFeetPerSec / differenceBetweenTimes;
                        //double timeDifferenceOfArrival = ((microSecondsToSeconds * 1125) / distanceBetweenMics);
                        //double rad = Math.acos(timeDifferenceOfArrival);
                        //double deg = Math.toDegrees(rad);
                        double rad = Math.acos(tdoa);
                        double deg = Math.toDegrees(rad);

                        System.out.println(deg);
                        difference = "";

                    }
*/


                } catch (Exception e) {}

            }


        }
    }


    class ThreadForTDOA2 implements Runnable                       //ADDED NEW VC1 - for TDOA calculations
    {
        @Override
        public void run() {


            if (stopThread) {
                return;
            }


            while (true) {

                if (stopThread) {
                    return;
                }


                try {



                    final String string;
                    InputStream inputStream = btSocket.getInputStream();

                    if(inputStream.available() > 0)
                    {
                        Thread.sleep(100);                                         ///FIXED THE STREAM ISSUE !!!!
                    }

                    int byteCount = inputStream.available();
                    if (byteCount > 0) {
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);
                        string = new String(rawBytes, "UTF-8");
                        //System.out.println("test " + string);
                        System.out.println("test 1 " + string);
                    } else {
                        string = "";
                    }



                    String startTimeForMica = "";     //adding new
                    String endTimeForMicb = "";     //adding new
                    String difference = "";     //adding new

                    String micQtime = "";
                    String micRtime = "";
                    String micStime = "";

                    String timeOrder = "";



                    for (int x = 0; x < string.length(); x++)  //adding new
                    {

                        if(string.charAt(x) == 'Q')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                //char dis = string.charAt(x + valueDigits);
                                valueDigits += 1;

                            }



                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                micQtime = string.substring(x + 1, x + valueDigits);
                            }


                            System.out.println("Q " + micQtime);

                            //  System.out.println( string.substring(1, string.length()));
                            System.out.println(string);
                        }

                        if(string.charAt(x) == 'R')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                //char dis = string.charAt(x + valueDigits);
                                valueDigits += 1;

                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                micRtime = string.substring(x + 1, x + valueDigits);
                            }


                            System.out.println("R " + micRtime);

                        }

                        if(string.charAt(x) == 'S')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                //char dis = string.charAt(x + valueDigits);
                                valueDigits += 1;

                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                micStime = string.substring(x + 1, x + valueDigits);
                            }


                            System.out.println("S " + micStime);

                        }

                        if(string.charAt(x) == 'r')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                //char dis = string.charAt(x + valueDigits);
                                valueDigits += 1;

                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                timeOrder = string.substring(x + 1, x + valueDigits);
                            }

                            System.out.println("r " + timeOrder);

                        }
                    }


                    if (Integer.parseInt(micQtime) == 0 && Integer.parseInt(micRtime) == 0 && Integer.parseInt(micStime) == 0)
                    {
                        //do nothing cause no useful values

                    }
                    else  //calulate degrees for each
                    {
                        int distanceCm = 20;//cm;//30; //cm
                        float distanceFt =  (((float)distanceCm) / (float)30.48); //fr
                        float speedOfSoundFt = 1125;

                        if (Integer.parseInt(micQtime) == 0)
                        {
                            micQtime = null;
                        }
                        else
                        {
                            float timeDelayInMicroSeconds = Integer.parseInt(micQtime);
                            float microToSeconds = (timeDelayInMicroSeconds / 1000000);
                            float distanceSoudnIsDifferennt =  (microToSeconds * speedOfSoundFt);
                            float adjDivHyp = distanceSoudnIsDifferennt / distanceFt;
                            float rad = (float)Math.acos(adjDivHyp);
                            //double rad = Math.acos((Integer.parseInt(micQtime) * speedOfSoundFt) / distanceFt);
                            double deg = Math.toDegrees(rad);
                            AB = deg;
                            System.out.println("Angle between m1 and m2 = " + deg);
                        }

                        if (Integer.parseInt(micRtime) == 0)
                        {
                            micRtime = null;
                        }
                        else
                        {


                            float timeDelayInMicroSeconds = Integer.parseInt(micRtime);
                            float microToSeconds = (timeDelayInMicroSeconds / 1000000);
                            float distanceSoudnIsDifferennt =  (microToSeconds * speedOfSoundFt);
                            float adjDivHyp = distanceSoudnIsDifferennt / distanceFt;
                            float rad = (float)Math.acos(adjDivHyp);
                            //double rad = Math.acos((Integer.parseInt(micQtime) * speedOfSoundFt) / distanceFt);
                            double deg = Math.toDegrees(rad);
                            AC = deg;

                            System.out.println("Angle between m1 and m3 = " + deg);
                        }

                        if (Integer.parseInt(micStime) == 0)
                        {
                            micStime = null;
                        }
                        else
                        {
                            float timeDelayInMicroSeconds = Integer.parseInt(micStime);
                            float microToSeconds = (timeDelayInMicroSeconds / 1000000);
                            float distanceSoudnIsDifferennt =  (microToSeconds * speedOfSoundFt);
                            float adjDivHyp = distanceSoudnIsDifferennt / distanceFt;
                            float rad = (float)Math.acos(adjDivHyp);
                            //double rad = Math.acos((Integer.parseInt(micQtime) * speedOfSoundFt) / distanceFt);
                            double deg = Math.toDegrees(rad);
                            BC = deg;

                            System.out.println("Angle between m2 and m3 = " + deg);
                        }



                        int singleDirection = -1;
                        double ABout = 0;
                        double BCin = 0;
                        double ACin = 0;

                        double ACout = 0;
                        double CB = 0;
                        double CBin = 0;
                        double ABin = 0;

                        double BA = 0;
                        double BAout = 0;

                        double BCout = 0;
                        double BAin = 0;
                        double CA = 0;
                        double CAin = 0;

                        double CAout = 0;

                        double CBout = 0;


                        switch (timeOrder)
                        {
                            case "ABC":
                                if(AB > 60)
                                {
                                    ABout = 210;
                                }
                                else {
                                    ABout = (AB * 2) + 90;
                                }
                                //BCin = ((((BC - 30) * 2)  + 30) + 90); changing
                                BCin = 210 - (BC * 2);
                                ACin = ((AC * 2) + 90);

                                System.out.println("AB out " + ABout);
                                System.out.println("BCin " + BCin);
                                System.out.println("ACin " + ACin);

                                makeDirectionAllEffects(ABout, BCin, ACin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "ACB":
                                //adding boundary
                                if(AC > 60)
                                {
                                    ACout = 330;
                                }
                                else if(AC > 46 && AC < 61)
                                {
                                    ACout = (90 - (AB * 2)) + 360 ;
                                }
                                else
                                {
                                    ACout = (90 - (AB * 2)) ;
                                }


                                CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                CBin = (((CB - 30) * 2)  + 30);

                                if(CBin < 0)
                                {
                                    CBin = CBin + 360;

                                }

                                ABin = (90 - (AB * 2));

                                if(ABin < 0)
                                {
                                    ABin = ABin + 360;
                                }

                                System.out.println("ACout " + ACout);
                                System.out.println("CBin " + CBin);
                                System.out.println("ABin " + ABin);


                                makeDirectionAllEffects(ACout, CBin, ABin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();


                                break;
                            case "BAC":

                                BA = Math.abs(AB);
                                if(BA > 60)
                                {
                                    BAout = 90;
                                }
                                else {
                                    BAout = ((180 - (120 + BA)) * 2) + 90;
                                }

                                ACin = ((AC * 2) + 90);

                                //BCin = ((((BC - 30) * 2)  + 30) + 90);  //changing
                                BCin = 210 - (BC * 2);

                                System.out.println("BAout " + BAout);
                                System.out.println("BCin " + BCin);
                                System.out.println("ACin " + ACin);

                                makeDirectionAllEffects(BAout, BCin, ACin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();


                                break;
                            case "BCA":
                                if(BC > 60)
                                {
                                    BCout = 330;
                                }
                                else {
                                    BCout = (BC * 2) + 210;
                                }

                                BA = Math.abs(AB);

                                BAin = ((BA * 2) + 210);

                                if(BAin > 360)
                                {
                                    BAin = BAin - 360;
                                }



                                CA = Math.abs(AC);
                                CAin = (330 - (CA * 2));

                                System.out.println("BCout " + BCout);
                                System.out.println("BAin " + BAin);
                                System.out.println("CAin " + CAin);

                                makeDirectionAllEffects(BCout, BAin, CAin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CAB":
                                CA = Math.abs(AC);

                                if(CA > 60)
                                {
                                    CAout = 90;
                                }
                                else if(CA < 15)
                                {
                                    CAout = (90 - ((180 - (120 + CA)) * 2)) + 360;
                                }
                                else
                                {
                                    CAout = 90 - ((180 - (120 + CA)) * 2);
                                }


                                CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                CBin = (((CB - 30) * 2)  + 30);

                                if(CBin < 0)
                                {
                                    CBin = CBin + 360;

                                }

                                ABin = (90 - (AB * 2));

                                if(ABin < 0)
                                {
                                    ABin = ABin + 360;
                                }

                                System.out.println("CAout " + CAout);
                                System.out.println("CBin " + CBin);
                                System.out.println("ABin " + ABin);

                                makeDirectionAllEffects(CAout, CBin, ABin, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CBA":

                                CA = Math.abs(AC);
                                CAin = (330 - (CA * 2));

                                BA = Math.abs(AB);
                                BAin = ((BA * 2) + 210);

                                if(BAin > 360)
                                {
                                    BAin = BAin - 360;
                                }

                                CB = Math.abs(BC);
                                if(CB > 60)
                                {
                                    CBout = 210;
                                }
                                else {
                                    CBout = Math.abs(((180 - (120 + CB)) * 2)) + 210;
                                }

                                System.out.println("CAin " + CAin);
                                System.out.println("BAin " + BAin);
                                System.out.println("CBout " + CBout);

                                makeDirectionAllEffects(CAin, BAin, CBout, timeOrder);
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "AB":
                                if(AB > 60)
                                {
                                    ABout = 210;
                                }
                                else {
                                    ABout = (AB * 2) + 90;
                                }
                                System.out.println("ABout " + ABout);

                                singleDirection = (int) (ABout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();


                                break;
                            case "AC":
                                //adding boundary
                                if(AC > 60)
                                {
                                    ACout = 330;
                                }
                                else if(AC > 46 && AC < 61)
                                {
                                    ACout = (90 - (AB * 2)) + 360 ;
                                }
                                else {
                                    ACout = (90 - (AB * 2));
                                }
                                System.out.println("ACout " + ACout);

                                singleDirection = (int) (ACout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "BA":
                                if(BA > 60)
                                {
                                    BAout = 90;
                                }
                                else {
                                    BAout = ((180 - (120 + BA)) * 2) + 90;
                                }
                                System.out.println("BAout " + BAout);

                                singleDirection = (int) (BAout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CA":

                                CA = Math.abs(AC);
                                if(CA > 60)
                                {
                                    CAout = 90;
                                }
                                else if(CA < 15)
                                {
                                    CAout = (90 - ((180 - (120 + CA)) * 2)) + 360;
                                }
                                else {
                                    CAout = 90 - ((180 - (120 + CA)) * 2);
                                }


                                System.out.println("CAout " + CAout);

                                singleDirection = (int) (CAout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "BC":
                                if(BC > 60)
                                {
                                    BCout = 330;
                                }
                                else {
                                    BCout = (BC * 2) + 210;
                                }
                                System.out.println("BCout " + BCout);

                                singleDirection = (int) (BCout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            case "CB":

                                CB = Math.abs(BC);
                                if(CB > 60)
                                {
                                    CBout = 210;
                                }
                                else {
                                    CBout = Math.abs(((180 - (120 + CB)) * 2)) + 210;
                                }

                                System.out.println("CBout " + CBout);

                                singleDirection = (int) (CBout / 22.5);
                                directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                Thread.sleep(1000);
                                disableAllDirectionalEffects();

                                break;
                            default:
                                System.out.println("Do Nothing");
                        }

                        System.out.println("debuging  testing");


                    }



                } catch (Exception e) {}
            }
        }
    }

    class ThreadForTDOA3 implements Runnable                       //ADDED NEW VC1 - for TDOA calculations
    {
        @Override
        public void run() {


            if (stopThread) {
                return;
            }


            while (true) {

                if (stopThread) {
                    return;
                }


                try {


                    final String string;
                    InputStream inputStream = btSocket.getInputStream();

                    int byteCount = inputStream.available();
                    if (byteCount > 0) {
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);
                        string = new String(rawBytes, "UTF-8");
                        System.out.println("test 2 " + string);
                        //System.out.println("test " + string);
                    } else {
                        string = "";
                    }



                } catch (Exception e) {}
            }
        }
    }

    class ThreadForTDOA4 implements Runnable                       //ADDED NEW VC1 - for TDOA calculations
    {
        @Override
        public void run() {


            if (stopThread) {
                return;
            }


            while (true) {

                if (stopThread) {
                    return;
                }


                try {


                    final String string;
                    InputStream inputStream = btSocket.getInputStream();

                    int byteCount = inputStream.available();
                    if (byteCount > 0) {
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);
                        string = new String(rawBytes, "UTF-8");
                       // System.out.println("test " + string);
                        System.out.println("test 3 " + string);
                    } else {
                        string = "";
                    }



                } catch (Exception e) {}
            }
        }
    }




    void makeDirectionAllEffects(double angle1, double angle2, double angle3, String timeOrder)
    {
        if(angle1 != 0 && angle2 != 0 && angle3 != 0)
        {

            int indicator1 = 0;
            int indicator2 = 0;
            int indicator3 = 0;



            if(Double.isNaN(angle1))
            {

                indicator2 = (int) (angle2 / 22.5);
                indicator3 = (int) (angle3 / 22.5);
                indicator1 = indicator2;
            }
            else if(Double.isNaN(angle2))
            {
                indicator1 = (int) (angle1 / 22.5);
                indicator3 = (int) (angle3 / 22.5);
                indicator2 = indicator1;
            }
            else if(Double.isNaN(angle3))
            {
                indicator1 = (int) (angle1 / 22.5);
                indicator2 = (int) (angle2 / 22.5);
                indicator3 = indicator2;
            }
            else
            {
                indicator1 = (int) (angle1 / 22.5);
                indicator2 = (int) (angle2 / 22.5);
                indicator3 = (int) (angle3 / 22.5);
            }

            /*int indicator1 = (int) (angle1 / 22.5);  //360/16 = 22.5
            int indicator2 = (int) (angle2 / 22.5);
            int indicator3 = (int) (angle3 / 22.5);*/

            int minDirection = 0;
            int msxDirection = 15;
            int midDirection = minDirection;




            if (indicator1 >= indicator2 && indicator1 > indicator3)
            {
                msxDirection = indicator1;
            }
            else if (indicator2 > indicator1 && indicator2 >= indicator3)
            {
                msxDirection = indicator2;
            }
            else if (indicator3 >= indicator1 && indicator3 > indicator2)
            {
                msxDirection = indicator3;
            }

            if (indicator1 <= indicator2 && indicator1 < indicator3)
            {
                minDirection = indicator1;
            }
            else if (indicator2 < indicator1 && indicator2 <= indicator3)
            {
                minDirection = indicator2;
            }
            else if (indicator3 <= indicator1 && indicator3 < indicator2)
            {
                minDirection = indicator3;
            }


            if ((indicator1 < indicator2 && indicator1 > indicator3) || (indicator1 > indicator3 && indicator1 < indicator2)) //fixes flow of direction
            {
                midDirection = indicator1;
            }
            else if ((indicator2 < indicator1 && indicator2 > indicator3) || (indicator2 > indicator1 && indicator2 < indicator3) )
            {
                midDirection = indicator2;
            }
            else if ((indicator3 < indicator1 && indicator3 > indicator2) || (indicator3 > indicator1 && indicator3 < indicator2))
            {
                midDirection = indicator3;
            }
            else
            {
                midDirection = minDirection;
            }


            if(timeOrder.equals("ABC") || timeOrder.equals("BCA") || timeOrder.equals("CAB")) // anticlockwise
            {
                int minToMax = msxDirection - minDirection;
                int midToMin = (16 - midDirection) + minDirection;
                int maxToMid = (16 - msxDirection) + midDirection;

                if (minToMax <= midToMin && minToMax <= maxToMid)  //mintomax
                {

                    for (int index = minDirection ; index <= msxDirection; index++)
                    {
                        ImageView direction = directionalIndicators.get(index);
                        direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                    }

                }
                else if (midToMin <= minToMax && midToMin <= maxToMid)  // mid to min
                {
                    for (int index = midDirection ; index != minDirection; index++)
                    {
                        ImageView direction = directionalIndicators.get(index);
                        direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                        if(index == 16)
                        {
                            index = -1; //so that next loops starts at 0
                        }

                    }

                    ImageView direction = directionalIndicators.get(minDirection);
                    direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                }
                else if (maxToMid <= minToMax && maxToMid <= midToMin)  // max to mid
                {
                    for (int index = msxDirection ; index != midDirection; index++)
                    {
                        ImageView direction = directionalIndicators.get(index);
                        direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                        if(index == 16)
                        {
                            index = -1; //so that next loops starts at 0 // could do this when index == 15 for same efffect also
                        }

                    }

                    ImageView direction = directionalIndicators.get(midDirection);
                    direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                }

            }
            else if(timeOrder.equals("ACB") || timeOrder.equals("BAC") || timeOrder.equals("CBA")) //clockwise direction
            {
                int minToMid = (16 - midDirection) + minDirection;
                int midToMax = (16 - msxDirection) + midDirection;
                int maxToMin = msxDirection - minDirection;

                if (minToMid <= midToMax && minToMid <= maxToMin)  //minToMid
                {
                    for (int index = minDirection; index != midDirection; index--)
                    {
                        ImageView direction = directionalIndicators.get(index);
                        direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                        if(index == 0)
                        {
                            index =16; //so that next loops starts at 15
                        }
                    }

                    //now do the max direction
                    ImageView direction = directionalIndicators.get(midDirection);
                    direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                }
                else if (midToMax <= minToMid && midToMax <= maxToMin)  // midToMax
                {
                    for (int index = midDirection; index != msxDirection; index--)
                    {
                        ImageView direction = directionalIndicators.get(index);
                        direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                        if(index == 0)
                        {
                            index =16; //so that next loops starts at 15
                        }
                    }

                    //now do the max direction
                    ImageView direction = directionalIndicators.get(msxDirection);
                    direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                }
                else if (maxToMin <= midToMax && maxToMin <= minToMid)  //maxToMin
                {
                    for (int index = msxDirection; index <= minDirection; index--)
                    {
                        ImageView direction = directionalIndicators.get(index);
                        direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                    }

                }
            }



           /* if(timeOrder.equals("ACB") || timeOrder.equals("BAC") || timeOrder.equals("CBA")) //clockwise direction
            {

                for (int index = minDirection; index != msxDirection; index--)
                {
                    ImageView direction = directionalIndicators.get(index);
                    direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                    if(index == 0)
                    {
                        index =16; //so that next loops starts at 15
                    }
                }

                //now do the max direction
                ImageView direction = directionalIndicators.get(msxDirection);
                direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

            }
            else if(timeOrder.equals("ABC") || timeOrder.equals("BCA") || timeOrder.equals("CAB")) //if anticlockwise direction
            {

                for (int index = msxDirection ; index != minDirection; index++)
                {
                    ImageView direction = directionalIndicators.get(index);
                    direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

                    if(index == 16)
                    {
                        index = 0; //so that next loops starts at 15
                    }
                }

                //now do the max direction
                ImageView direction = directionalIndicators.get(minDirection);
                direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

            }*/

/*
            for (int index = minDirection; index <= msxDirection; index++)
            {
                ImageView direction = directionalIndicators.get(index);
                direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));

            }
*/

        }

        System.out.println("enabling multi directions" );

    }

    void disableAllDirectionalEffects()
    {
        for (int index = 0; index <= 15; index++)
        {
            ImageView direction = directionalIndicators.get(index);
            direction.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));

        }

        System.out.println("disable directions" );
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


                    String startTimeForMica = "";     //adding new                 vc1
                    String endTimeForMicb = "";     //adding new                    vc1



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


                       /* if (string.charAt(x) == 'a')            //b for inpur 2     vc1
                        {

                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != '\n' )
                            {
                                valueDigits += 1;
                            }



                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                startTimeForMica = string.substring(x + 1, x + valueDigits);         // to avoid first letter
                            }


                            System.out.println("o " + startTimeForMica);


                        }

                        if (string.charAt(x) == 'c')            //b for inpur 2     vc1
                        {

                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != '\n' && string.charAt(x + valueDigits) != '\r' )
                            {
                                valueDigits += 1;
                            }



                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n')
                            {
                                endTimeForMicb = string.substring(x + 1, x + valueDigits);         // to avoid first letter
                            }


                            System.out.println("t " + endTimeForMicb);


                        }*/




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




    class arrayOfMicResults //t3
    {

        private long m1Time, m2Time, m3Time;



        public arrayOfMicResults(long mictime1, long mictime2, long mictime3)        //constructor
        {
            m1Time = mictime1;
            m2Time = mictime2;
            m3Time = mictime3;

        }


        long getM1Time()
        {
            return m1Time;
        }

        long getM2Time()
        {
            return m2Time;
        }

        long getM3Time()
        {
            return m3Time;
        }


    }





}
