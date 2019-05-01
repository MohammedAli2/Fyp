package com.fypvc.moh.fypwithvc;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.ColorFilter;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.vikramezhil.droidspeech.DroidSpeech;                                         //STT - For the library DroidSpeech - continous live transcribe
import com.vikramezhil.droidspeech.OnDSListener;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;




public class MainActivity extends AppCompatActivity implements OnDSListener {                         //Implemented OnDSListener For STT

    //TTS related variables
    private Button ttsBtn;
    private EditText ttsET;
    private TextToSpeech ttsObject;
    private Button premadeBtn;
    private ArrayList<String> premadeBtnArray = new ArrayList<String>();                                     //Saved premades are inserted into this when loaded
    private Button pm1,  pm2, pm3, pm4, pm5;                                                                    //Premdade TTS phrase
    //ETTS


    //STT realted variables
    private TextView speechToOnScreenText;
    private ScrollView scrollSTT;
    private Switch sttOnOff;
    DroidSpeech droidSpeech;
    private ArrayList<String> triggerArray = new ArrayList<String>();                                       //Saved trigger words stored on this reloaded
    private Button triggerWordsBtn;
    private Button t1, t2, t3, t4, t5;                                                                  //Trigger words - triggger vibration when set
    //ESTT


   // private Button btnVibrationOn, btnVibrationOff;
    private BluetoothAdapter bluetoothAdapter;
    private String address;
  //  private Set<BluetoothDevice> pairedDevices;
    private BluetoothSocket btSocket;                                                                   //communication socket
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");        //connect to bluetooth device

    private TextView Input1TxtView;
    private TextView Input2TxtView;
    private TextView Input3TxtView;             //added new

    private Handler InputHandler = new Handler();
    private Button btnStartThread;
    private Button btnStoptThread;





    private volatile boolean stopThread = false;        //volatiole means that it always gives this variables most upto data value - not cached value



    private volatile double AB = 0;
    private volatile double AC = 0;
    private volatile double BC = 0;

    private ArrayList<ImageView> directionalIndicators = new ArrayList<ImageView>();
    private ArrayList<String> indicatorColourTracker = new ArrayList<String>();


    private int greenDirectionIndex = -1;


    //Larger Enviroment
    private volatile boolean largerEnviroment = false;
    private Switch largerEnvSwitch;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button allMotorVibrationBtn = findViewById(R.id.motorTestBtn);

        allMotorVibrationBtn.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view)
            {
                testMotorsSubScreen();
            }
        });

        //TTS set up
        ttsBtn =  findViewById(R.id.ttsButton);
        ttsET =   findViewById(R.id.editText);

        premadeBtn = (Button) findViewById(R.id.premadeBtn);


        ttsObject = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status)
        {
            if(status != TextToSpeech.ERROR)
            {
                ttsObject.setLanguage(Locale.UK);
            }
        }
        });

        ttsBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view)
         {
             ttsObject.speak(ttsET.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
         }
        });

//premade save data and settup
        premadeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences load = getSharedPreferences("Preferences", 0);        //loadSave
                String saved = load.getString("premades", "");
                String[] unloadPremades = saved.split(",");
                ArrayList<String> savedPremadeArray= new ArrayList<String>();
                for(int i = 0; i < unloadPremades.length; i++)
                {
                    premadeBtnArray.set(i, unloadPremades[i]);
                }

                dialogBoxtts();     //load dialog
            }
        });




        premadeBtnArray.add("");
        premadeBtnArray.add("");
        premadeBtnArray.add("");
        premadeBtnArray.add("");
        premadeBtnArray.add("");



        //ETTS


        //larger enviroment setup
        largerEnvSwitch = findViewById(R.id.rangeBoostSwitch);

        largerEnvSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    largerEnviroment = true;
                } else {
                    largerEnviroment = false;
                }
            }
        });



        //STT setup
        speechToOnScreenText = findViewById(R.id.speechToTextTxtVw);
        scrollSTT = findViewById(R.id.SpeechToTextScrollView);
        sttOnOff= findViewById(R.id.sppSwitch);
        triggerWordsBtn = findViewById(R.id.sppTriggerBtn);

        //DroidSpeech droidSpeech = new DroidSpeech(this, null);// made global
        droidSpeech = new DroidSpeech(this, null);

        droidSpeech.setOnDroidSpeechListener(this);

        sttOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    droidSpeech.startDroidSpeechRecognition();
                } else {
                    droidSpeech.closeDroidSpeechOperations();
                }
            }
        });

//trigger array save data setup
        triggerArray.add("");
        triggerArray.add("");
        triggerArray.add("");
        triggerArray.add("");
        triggerArray.add("");

        triggerWordsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences load = getSharedPreferences("Preferences", 0);        //loadSave
                String saved = load.getString("triggers", "");
                String[] getSavedTriggers = saved.split(",");
                for(int i = 0; i < getSavedTriggers.length; i++)
                {
                    triggerArray.set(i, getSavedTriggers[i]);
                }


                dialogSTT1();     //load dialog
            }
        });



        btnStartThread = (Button) findViewById(R.id.btnThread_Start);
        btnStoptThread = (Button) findViewById(R.id.btnThread_Stop);



        initialiseDirectionalIndicators();
        initialisaColoursForDirections();

//bluetooth connection setups
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        address = bluetoothAdapter.getAddress();

       // pairedDevices = bluetoothAdapter.getBondedDevices();

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


        if(bluetoothAdapter == null) //alert if not connectd to bluetooth device
        {
            Toast.makeText(MainActivity.this, "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }







        btnStartThread.setOnClickListener(new View.OnClickListener() {         //for pm
            @Override
            public void onClick(View v) {//start listening to mics
                stopThread = false;
                try{
                    btSocket.getOutputStream().write("a".getBytes());
                    System.out.println("a");

                } catch (IOException e){ System.out.println("COULD NOT SEND INSTRUCTION");}

                startThread();
            }
        });

        btnStoptThread.setOnClickListener(new View.OnClickListener() {         //for pm
            @Override
            public void onClick(View v) { //stop listening to mics
                stopThread = true;
                try{
                    btSocket.getOutputStream().write("z".getBytes());
                    System.out.println("z");

                } catch (IOException e){ System.out.println("COULD NOT SEND INSTRUCTION");}
            }
        });








    }


    // motor testing buttons
    private void testMotorsSubScreen() //send instruction to mega board to vibrate specific motor
    {
        AlertDialog.Builder dialogBoxBuilder = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View motorTstView = layoutInflater.inflate(R.layout.motortesting, null);


        dialogBoxBuilder.setView(motorTstView);


        Button motor1 = motorTstView.findViewById(R.id.motor1);
        Button motor2 = motorTstView.findViewById(R.id.motor2);
        Button motor3 = motorTstView.findViewById(R.id.motor3);
        Button motor4 = motorTstView.findViewById(R.id.motor4);
        Button motor5 = motorTstView.findViewById(R.id.motor5);
        Button motor6 = motorTstView.findViewById(R.id.motor6);
        Button motor7 = motorTstView.findViewById(R.id.motor7);
        Button motor8 = motorTstView.findViewById(R.id.motor8);

        motor1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("t".getBytes());//
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 1");
            }
        });

        motor2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("T".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 2");
            }
        });

        motor3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("u".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 3");
            }
        });

        motor4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("U".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 4");
            }
        });

        motor5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("v".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 5");
            }
        });

        motor6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("V".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 6");
            }
        });

        motor7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("w".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 7");
            }
        });

        motor8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    btSocket.getOutputStream().write("W".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Testing motor 8");
            }
        });



        dialogBoxBuilder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
            }
        });


        dialogBoxBuilder.show();

    }


    //tts
    private void dialogBoxtts()
    {
        AlertDialog.Builder dialogBoxBuilder = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        //pm1 = (Button) findViewById(R.id.premade1);



        View premadeView = layoutInflater.inflate(R.layout.dialog_box_for_tts_premades, null);

        pm1 = premadeView.findViewById(R.id.premade1); //prepare premades to contain any data saved
        pm2 = premadeView.findViewById(R.id.premade2);
        pm3 = premadeView.findViewById(R.id.premade3);
        pm4 = premadeView.findViewById(R.id.premade4);
        pm5 = premadeView.findViewById(R.id.premade5);

        if (premadeBtnArray.get(0) != "")
        {
            pm1.setText(premadeBtnArray.get(0));
        }

        if (premadeBtnArray.get(1) != "")
        {
            pm2.setText(premadeBtnArray.get(1));
        }

        if (premadeBtnArray.get(2) != "")
        {
            pm3.setText(premadeBtnArray.get(2));
        }

        if (premadeBtnArray.get(3) != "")
        {
            pm4.setText(premadeBtnArray.get(3));
        }

        if (premadeBtnArray.get(4) != "")
        {
            pm5.setText(premadeBtnArray.get(4));
        }

        dialogBoxBuilder.setView(premadeView);

        pm1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ttsET.setText(pm1.getText().toString());


            }
        });

        pm1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogBoxtts2(pm1);

                return true;
            }
        });

        pm2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ttsET.setText(pm2.getText().toString());


            }
        });

        pm2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogBoxtts2(pm2);
                return true;
            }
        });

        pm3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ttsET.setText(pm3.getText().toString());


            }
        });

        pm3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogBoxtts2(pm3);
                return true;
            }
        });

        pm4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ttsET.setText(pm4.getText().toString());


            }
        });

        pm4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogBoxtts2(pm4);
                return true;
            }
        });

        pm5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ttsET.setText(pm5.getText().toString());


            }
        });

        pm5.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogBoxtts2(pm5);
                return true;
            }
        });





        dialogBoxBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
            }
        });


        dialogBoxBuilder.show();


    }


    private void dialogBoxtts2(final Button premadeBtnSelected)
    {
        AlertDialog.Builder dialogBoxBuilder2 = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View premadeView = layoutInflater.inflate(R.layout.edit_premade_text, null);
        final EditText textForPremadeSpeech = (EditText)premadeView.findViewById(R.id.premadeEditTxt);
        dialogBoxBuilder2.setView(premadeView);


                    //sets text for premade buttons
        dialogBoxBuilder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int premadeNumber = -1;
                if (premadeBtnSelected.getId() == pm1.getId())
                {
                    pm1.setText(textForPremadeSpeech.getText().toString());
                    premadeNumber = 0;
                }
                else if (premadeBtnSelected.getId() == pm2.getId())
                {
                    pm2.setText(textForPremadeSpeech.getText().toString());
                    premadeNumber = 1;
                }
                else if (premadeBtnSelected.getId() == pm3.getId())
                {
                    pm3.setText(textForPremadeSpeech.getText().toString());
                    premadeNumber = 2;
                }
                else if (premadeBtnSelected.getId() == pm4.getId())
                {
                    pm4.setText(textForPremadeSpeech.getText().toString());
                    premadeNumber = 3;
                }
                else if (premadeBtnSelected.getId() == pm5.getId())
                {
                    pm5.setText(textForPremadeSpeech.getText().toString());
                    premadeNumber = 4;
                }



                if(textForPremadeSpeech.getText().toString() != "")
                {
                    premadeBtnArray.set(premadeNumber, textForPremadeSpeech.getText().toString());
                }


                StringBuilder sb = new StringBuilder();
                for(String allPremadesStr : premadeBtnArray)
                {
                    sb.append(allPremadesStr);
                    sb.append(",");
                }

                SharedPreferences saving = getSharedPreferences("Preferences", 0);
                SharedPreferences.Editor editSave = saving.edit();
                editSave.putString("premades", sb.toString());
                editSave.commit();

            }
            });

        dialogBoxBuilder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {



            }
        });


        dialogBoxBuilder2.show();
    }
    //etts




    public void startThread(){


        ThreadForTDOA2 getTimeStartEndETimes1 = new ThreadForTDOA2();
        new Thread(getTimeStartEndETimes1).start();


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
                        Thread.sleep(100);                                       //prepare to read whole of next sring that wull be sent
                    }

                    int byteCount = inputStream.available();
                    if (byteCount > 0) {
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);
                        string = new String(rawBytes, "UTF-8");
                    } else {
                        string = "";
                    }




                    String micQtime = "";
                    String micRtime = "";
                    String micStime = "";

                    String timeOrder = "";

                            //extrack time difference values and order ofmic times from bluetooth stream

                    for (int x = 0; x < string.length(); x++)  //adding new
                    {

                        if(string.charAt(x) == 'Q')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
                                valueDigits += 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                micQtime = string.substring(x + 1, x + valueDigits);
                            }

                            System.out.println("Q " + micQtime);            //Quick debugging
                            System.out.println(string);
                        }

                        if(string.charAt(x) == 'R')
                        {
                            int valueDigits = 1;

                            while (string.charAt(x + valueDigits) != 'e')
                            {
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
                                valueDigits += 1;
                            }

                            if (string.charAt(x + valueDigits) != '\r' || string.charAt(x + valueDigits) != '\n' || string.charAt(x + valueDigits) != 'e')
                            {
                                timeOrder = string.substring(x + 1, x + valueDigits);
                            }

                            System.out.println("r " + timeOrder);

                        }
                    }


                    if (Integer.parseInt(micQtime) == 0 && Integer.parseInt(micRtime) == 0 && Integer.parseInt(micStime) == 0) //calulate the angle
                    {
                        //do nothing cause no useful values
                        System.out.println("No useable values");

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

                        //Everything below works out the angle in relation to a cricle around the mortors - so to a 360 degreen values which is the surroundings of the user

                        if(largerEnviroment) //adust values if larger enviroment selected
                        {
                            switch (timeOrder) {
                                case "ABC":

                                    if (AB >= 67)
                                    {
                                        if ((AB - 67) < (90.5 - AB))
                                        {
                                            ABout = 135;
                                        }
                                        else
                                        {
                                            ABout = 157.5;
                                        }
                                    }
                                    else if (AB >= 44.7)
                                    {
                                        if ((AB - 44.7) < (67 - AB))
                                        {
                                            ABout = 112.5;
                                        }
                                        else
                                        {
                                            ABout = 135;
                                        }
                                    }
                                    else if (AB >= 23.5)
                                    {
                                        if ((AB - 23.5) < (44.7 - AB))
                                        {
                                            ABout = 90;
                                        }
                                        else
                                        {
                                            ABout = 112.5;
                                        }
                                    }
                                    else if (AB >= 3.1)
                                    {
                                        if ((AB - 3.1) < (23.5 - AB))
                                        {
                                            ABout = 67.5;
                                        }
                                        else
                                        {
                                            ABout = 90;
                                        }
                                    }
                                    else if (AB <= 3.1)
                                    {
                                        ABout = 67.5;
                                    }
                                    else
                                    {
                                        ABout = 157.5;
                                    }


                                    if (BC >= 63.1)
                                    {
                                        if ((BC - 63.1) < (83.5 - BC))
                                        {
                                            BCin = 112.5;
                                        }
                                        else
                                        {
                                            BCin = 90;
                                        }

                                    }
                                    else if (BC >= 43.2)
                                    {
                                        if ((BC - 43.2) < (63.1 - BC))
                                        {
                                            BCin = 135;
                                        }
                                        else
                                        {
                                            BCin = 112.5;
                                        }
                                    }
                                    else if (BC >= 23.4)
                                    {
                                        if ((BC - 23.4) < (43.2 - BC))
                                        {
                                            BCin = 157.5;
                                        }
                                        else
                                        {
                                            BCin = 135;
                                        }
                                    }
                                    else if (BC >= 3.6)
                                    {
                                        if ((BC - 3.6) < (23.4 - BC))
                                        {
                                            BCin = 180;
                                        }
                                        else
                                        {
                                            BCin = 157.5;
                                        }
                                    }
                                    else if (BC <= 3.6)
                                    {
                                        BCin = 180;
                                    }
                                    else
                                    {
                                        BCin = 90;
                                    }


                                    if (AC >= 76.6)
                                    {
                                        if ((AC - 76.6) < (97.5 - AC))
                                        {
                                            ACin = 202.5;
                                        }
                                        else
                                        {
                                            ACin = 225;
                                        }

                                    }
                                    else if (AC >= 56.4)
                                    {
                                        if ((AC - 56.4) < (76.6 - AC))
                                        {
                                            ACin = 180;
                                        }
                                        else
                                        {
                                            ACin = 202.5;
                                        }
                                    }
                                    else if (AC >= 36.6)
                                    {
                                        if ((AC - 36.6) < (56.4 - AC))
                                        {
                                            ACin = 157.5;
                                        }
                                        else
                                        {
                                            ACin = 180;
                                        }
                                    }
                                    else if (AC >= 16.8)
                                    {
                                        if ((AC - 16.8) < (36.6 - AC))
                                        {
                                            ACin = 135;
                                        }
                                        else
                                        {
                                            ACin = 157.5;
                                        }
                                    }
                                    else if (AC >= 5.5)
                                    {
                                        if ((AC - 5.5) < (16.8 - AC))
                                        {
                                            ACin = 112.5;
                                        }
                                        else
                                        {
                                            ACin = 135;
                                        }
                                    }
                                    else if (AC <= 5.5)
                                    {
                                        ACin = 112.5;
                                    }
                                    else
                                    {
                                        ACin = 225;
                                    }

                                    System.out.println("AB out " + ABout);
                                    System.out.println("BCin " + BCin);
                                    System.out.println("ACin " + ACin);

                                    makeDirectionAllEffects(ABout, BCin, ACin, timeOrder);
                                    Thread.sleep(1000);
                                    disableAllDirectionalEffects();

                                    break;
                                case "ACB":

                                    if (AC >= 67)
                                    {
                                        if ((AC - 67) < (90.5 - AC))
                                        {
                                            ACout = 45;
                                        }
                                        else
                                        {
                                            ACout = 22.5;
                                        }

                                    }
                                    else if (AC >= 44.7)
                                    {
                                        if ((AC - 44.7) < (67 - AC))
                                        {
                                            ACout = 67.5;
                                        }
                                        else
                                        {
                                            ACout = 45;
                                        }
                                    }
                                    else if (AC >= 23.5)
                                    {
                                        if ((AC - 23.5) < (44.7 - AC))
                                        {
                                            ACout = 90;
                                        }
                                        else
                                        {
                                            ACout = 67.5;
                                        }
                                    }
                                    else if (AC >= 3.1)
                                    {
                                        if ((AC - 3.1) < (23.5 - AC))
                                        {
                                            ACout = 112.5;
                                        }
                                        else
                                        {
                                            ACout = 90;
                                        }
                                    }
                                    else if (AC <= 3.1)
                                    {
                                        ACout = 112.5;
                                    }
                                    else
                                    {
                                        ACout = 22.5;
                                    }


                                    CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                    if (CB >= 63.1)
                                    {
                                        if ((CB - 63.1) < (83.5 - CB))
                                        {
                                            CBin = 67.5;
                                        }
                                        else
                                        {
                                            CBin = 90;
                                        }
                                    }
                                    else if (CB >= 43.2)
                                    {
                                        if ((CB - 43.2) < (63.1 - CB))
                                        {
                                            CBin = 45;
                                        }
                                        else
                                        {
                                            CBin = 67.5;
                                        }
                                    }
                                    else if (CB >= 23.4)
                                    {
                                        if ((CB - 23.4) < (43.2 - CB))
                                        {
                                            CBin = 22.5;
                                        }
                                        else
                                        {
                                            CBin = 45;
                                        }
                                    }
                                    else if (CB >= 3.6)
                                    {
                                        if ((CB - 3.6) < (23.4 - CB))
                                        {
                                            CBin = 0;
                                        }
                                        else
                                        {
                                            CBin = 22.5;
                                        }
                                    }
                                    else if (CB <= 3.6)
                                    {
                                        CBin = 0;
                                    }
                                    else
                                    {
                                        CBin = 90;
                                    }

                                    if (AB >= 76.6)
                                    {
                                        if ((AB - 76.6) < (97.5 - AB))
                                        {
                                            ABin = 337.5;
                                        }
                                        else
                                        {
                                            ABin = 315;
                                        }

                                    }
                                    else if (AB >= 56.4)
                                    {
                                        if ((AB - 56.4) < (76.6 - AB))
                                        {
                                            ABin = 0;
                                        }
                                        else
                                        {
                                            ABin = 337.5;
                                        }
                                    }
                                    else if (AB >= 36.6)
                                    {
                                        if ((AB - 36.6) < (56.4 - AB))
                                        {
                                            ABin = 22.5;
                                        }
                                        else
                                        {
                                            ABin = 0;
                                        }
                                    }
                                    else if (AB >= 16.8)
                                    {
                                        if ((AB - 16.8) < (36.6 - AB))
                                        {
                                            ABin = 45;
                                        }
                                        else
                                        {
                                            ABin = 22.5;
                                        }
                                    }
                                    else if (AB >= 5.5)
                                    {
                                        if ((AB - 5.5) < (16.8 - AB))
                                        {
                                            ABin = 67.5;
                                        }
                                        else
                                        {
                                            ABin = 45;
                                        }
                                    }
                                    else if (AB <= 5.5)
                                    {
                                        ABin = 67.5;
                                    }
                                    else
                                    {
                                        ABin = 315;
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

                                    if (BA >= 74.7)
                                    {
                                        if ((BA - 74.7) < (98.7 - BA))
                                        {
                                            BAout = 157.5;
                                        }
                                        else
                                        {
                                            BAout = 135;
                                        }
                                    }
                                    else if (BA >= 52)
                                    {
                                        if ((BA - 52) < (74.7 - BA))
                                        {
                                            BAout = 180;
                                        }
                                        else
                                        {
                                            BAout = 157.5;
                                        }
                                    }
                                    else if (BA >= 30.5)
                                    {
                                        if ((BA - 30.5) < (52 - BA))
                                        {
                                            BAout = 202.5;
                                        }
                                        else
                                        {
                                            BAout = 180;
                                        }
                                    }
                                    else if (BA >= 9.8)
                                    {
                                        if ((BA - 9.8) < (30.5 - BA))
                                        {
                                            BAout = 225;
                                        }
                                        else
                                        {
                                            BAout = 202.5;
                                        }
                                    }
                                    else if (BA <= 9.8)
                                    {
                                        BAout = 225;
                                    }
                                    else
                                    {
                                        BAout = 135;
                                    }


                                    if (AC >= 76.6)
                                    {
                                        if ((AC - 76.6) < (97.5 - AC))
                                        {
                                            ACin = 202.5;
                                        }
                                        else
                                        {
                                            ACin = 225;
                                        }

                                    }
                                    else if (AC >= 56.4)
                                    {
                                        if ((AC - 56.4) < (76.6 - AC))
                                        {
                                            ACin = 180;
                                        }
                                        else
                                        {
                                            ACin = 202.5;
                                        }
                                    }
                                    else if (AC >= 36.6)
                                    {
                                        if ((AC - 36.6) < (56.4 - AC))
                                        {
                                            ACin = 157.5;
                                        }
                                        else
                                        {
                                            ACin = 180;
                                        }
                                    }
                                    else if (AC >= 16.8)
                                    {
                                        if ((AC - 16.8) < (36.6 - AC))
                                        {
                                            ACin = 135;
                                        }
                                        else
                                        {
                                            ACin = 157.5;
                                        }
                                    }
                                    else if (AC >= 5.5)
                                    {
                                        if ((AC - 5.5) < (16.8 - AC))
                                        {
                                            ACin = 112.5;
                                        }
                                        else
                                        {
                                            ACin = 135;
                                        }
                                    }
                                    else if (AC <= 5.5)
                                    {
                                        ACin = 112.5;
                                    }
                                    else
                                    {
                                        ACin = 225;
                                    }


                                    if (BC >= 63.1)
                                    {
                                        if ((BC - 63.1) < (83.5 - BC))
                                        {
                                            BCin = 112.5;
                                        }
                                        else
                                        {
                                            BCin = 90;
                                        }

                                    }
                                    else if (BC >= 43.2)
                                    {
                                        if ((BC - 43.2) < (63.1 - BC))
                                        {
                                            BCin = 135;
                                        }
                                        else
                                        {
                                            BCin = 112.5;
                                        }
                                    }
                                    else if (BC >= 23.4)
                                    {
                                        if ((BC - 23.4) < (43.2 - BC))
                                        {
                                            BCin = 157.5;
                                        }
                                        else
                                        {
                                            BCin = 135;
                                        }
                                    }
                                    else if (BC >= 3.6)
                                    {
                                        if ((BC - 3.6) < (23.4 - BC))
                                        {
                                            BCin = 180;
                                        }
                                        else
                                        {
                                            BCin = 157.5;
                                        }
                                    }
                                    else if (BC <= 3.6)
                                    {
                                        BCin = 180;
                                    }
                                    else
                                    {
                                        BCin = 90;
                                    }


                                    System.out.println("BAout " + BAout);
                                    System.out.println("BCin " + BCin);
                                    System.out.println("ACin " + ACin);

                                    makeDirectionAllEffects(BAout, BCin, ACin, timeOrder);
                                    Thread.sleep(1000);
                                    disableAllDirectionalEffects();


                                    break;
                                case "BCA":

                                    if (BC >= 59.4)
                                    {
                                        if ((BC - 59.4) < (82.5 - BC))
                                        {
                                            BCout = 247.5;
                                        }
                                        else
                                        {
                                            BCout = 270;
                                        }
                                    }
                                    else if (BC >= 37.5)
                                    {
                                        if ((BC - 37.5) < (59.4 - BC))
                                        {
                                            BCout = 225;
                                        }
                                        else
                                        {
                                            BCout = 247.5;
                                        }
                                    }
                                    else if (BC >= 16.6)
                                    {
                                        if ((BC - 16.6) < (37.5 - BC))
                                        {
                                            BCout = 202.5;
                                        }
                                        else
                                        {
                                            BCout = 225;
                                        }
                                    }
                                    else if (BC >= 9)
                                    {
                                        if ((BC - 9) < (16.6 - BC))
                                        {
                                            BCout = 180;
                                        }
                                        else
                                        {
                                            BCout = 202.5;
                                        }
                                    }
                                    else if (BC <= 9)
                                    {
                                        BCout = 180;
                                    }
                                    else
                                    {
                                        BCout = 270;
                                    }


                                    BA = Math.abs(AB);

                                    if (BA >= 69.8)
                                    {
                                        if ((BA - 69.8) < (90.5 - BA))
                                        {
                                            BAin = 315;
                                        }
                                        else
                                        {
                                            BAin = 337.5;
                                        }
                                    }
                                    else if (BA >= 49.8)
                                    {
                                        if ((BA - 49.8) < (69.8 - BA))
                                        {
                                            BAin = 292.5;
                                        }
                                        else
                                        {
                                            BAin = 315;
                                        }
                                    }
                                    else if (BA >= 30)
                                    {
                                        if ((BA - 30) < (49.8 - BA))
                                        {
                                            BAin = 270;
                                        }
                                        else
                                        {
                                            BAin = 292.5;
                                        }
                                    }
                                    else if (BA >= 10.2)
                                    {
                                        if ((BA - 10.2) < (30 - BA))
                                        {
                                            BAin = 247.5;
                                        }
                                        else
                                        {
                                            BAin = 270;
                                        }
                                    }
                                    else if (BA <= 10.2)
                                    {
                                        BAin = 247.5;
                                    }
                                    else
                                    {
                                        BAin = 337.5;
                                    }


                                    CA = Math.abs(AC);
                                    if (CA >= 69.8)
                                    {
                                        if ((CA - 69.8) < (90.5 - CA))
                                        {
                                            CAin = 235;
                                        }
                                        else
                                        {
                                            CAin = 202.5;
                                        }

                                    }
                                    else if (CA >= 49.8)
                                    {
                                        if ((CA - 49.8) < (69.8 - CA))
                                        {
                                            CAin = 247.5;
                                        }
                                        else
                                        {
                                            CAin = 235;
                                        }
                                    }
                                    else if (CA >= 30)
                                    {
                                        if ((CA - 30) < (49.8 - CA))
                                        {
                                            CAin = 270;
                                        }
                                        else
                                        {
                                            CAin = 247.5;
                                        }
                                    }
                                    else if (CA >= 10.2)
                                    {
                                        if ((CA - 10.2) < (30 - CA))
                                        {
                                            CAin = 292.5;
                                        }
                                        else
                                        {
                                            CAin = 270;
                                        }
                                    }
                                    else if (CA <= 10.2)
                                    {
                                        CAin = 292.5;
                                    }
                                    else
                                    {
                                        CAin = 202.5;
                                    }

                                    System.out.println("BCout " + BCout);
                                    System.out.println("BAin " + BAin);
                                    System.out.println("CAin " + CAin);

                                    makeDirectionAllEffects(BCout, BAin, CAin, timeOrder);
                                    Thread.sleep(1000);
                                    disableAllDirectionalEffects();

                                    break;
                                case "CAB":
                                    CA = Math.abs(AC);

                                    if (CA >= 74.7)
                                    {
                                        if ((CA - 74.7) < (98.7 - CA))
                                        {
                                            CAout = 22.5;
                                        }
                                        else
                                        {
                                            CAout = 45;
                                        }
                                    }
                                    else if (CA >= 52)
                                    {
                                        if ((CA - 52) < (74.7 - CA))
                                        {
                                            CAout = 0; //360 would also suffice
                                        }
                                        else
                                        {
                                            CAout = 22.5;
                                        }
                                    }
                                    else if (CA >= 30.5)
                                    {
                                        if ((CA - 30.5) < (52 - CA))
                                        {
                                            CAout = 337.5;
                                        }
                                        else
                                        {
                                            CAout = 0;
                                        }
                                    }
                                    else if (CA >= 9.8)
                                    {
                                        if ((CA - 9.8) < (30.5 - CA))
                                        {
                                            CAout = 315;
                                        }
                                        else
                                        {
                                            CAout = 337.5;
                                        }
                                    }
                                    else if (CA <= 9.8)
                                    {
                                        CAout = 315;
                                    }
                                    else
                                    {
                                        CAout = 45;
                                    }


                                    CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                    if (CB >= 63.1)
                                    {
                                        if ((CB - 63.1) < (83.5 - CB))
                                        {
                                            CBin = 67.5;
                                        }
                                        else
                                        {
                                            CBin = 90;
                                        }

                                    }
                                    else if (CB >= 43.2)
                                    {
                                        if ((CB - 43.2) < (63.1 - CB))
                                        {
                                            CBin = 45;
                                        }
                                        else
                                        {
                                            CBin = 67.5;
                                        }
                                    }
                                    else if (CB >= 23.4)
                                    {
                                        if ((CB - 23.4) < (43.2 - CB))
                                        {
                                            CBin = 22.5;
                                        }
                                        else
                                        {
                                            CBin = 45;
                                        }
                                    }
                                    else if (CB >= 3.6)
                                    {
                                        if ((CB - 3.6) < (23.4 - CB))
                                        {
                                            CBin = 0;
                                        }
                                        else
                                        {
                                            CBin = 22.5;
                                        }
                                    }
                                    else if (CB <= 3.6)
                                    {
                                        CBin = 0;
                                    }
                                    else
                                    {
                                        CBin = 90;
                                    }

                                    if (AB >= 76.6)
                                    {
                                        if ((AB - 76.6) < (97.5 - AB))
                                        {
                                            ABin = 337.5;
                                        }
                                        else
                                        {
                                            ABin = 315;
                                        }

                                    }
                                    else if (AB >= 56.4)
                                    {
                                        if ((AB - 56.4) < (76.6 - AB))
                                        {
                                            ABin = 0;
                                        }
                                        else
                                        {
                                            ABin = 337.5;
                                        }
                                    }
                                    else if (AB >= 36.6)
                                    {
                                        if ((AB - 36.6) < (56.4 - AB))
                                        {
                                            ABin = 22.5;
                                        }
                                        else
                                        {
                                            ABin = 0;
                                        }
                                    }
                                    else if (AB >= 16.8)
                                    {
                                        if ((AB - 16.8) < (36.6 - AB))
                                        {
                                            ABin = 45;
                                        }
                                        else
                                        {
                                            ABin = 22.5;
                                        }
                                    }
                                    else if (AB >= 5.5)
                                    {
                                        if ((AB - 5.5) < (16.8 - AB))
                                        {
                                            ABin = 67.5;
                                        }
                                        else
                                        {
                                            ABin = 45;
                                        }
                                    }
                                    else if (AB <= 5.5)
                                    {
                                        ABin = 67.5;
                                    }
                                    else
                                    {
                                        ABin = 315;
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
                                    if (CA >= 69.8)
                                    {
                                        if ((CA - 69.8) < (90.5 - CA))
                                        {
                                            CAin = 235;
                                        }
                                        else
                                        {
                                            CAin = 202.5;
                                        }

                                    }
                                    else if (CA >= 49.8)
                                    {
                                        if ((CA - 49.8) < (69.8 - CA))
                                        {
                                            CAin = 247.5;
                                        }
                                        else
                                        {
                                            CAin = 235;
                                        }
                                    }
                                    else if (CA >= 30)
                                    {
                                        if ((CA - 30) < (49.8 - CA))
                                        {
                                            CAin = 270;
                                        }
                                        else
                                        {
                                            CAin = 247.5;
                                        }
                                    }
                                    else if (CA >= 10.2)
                                    {
                                        if ((CA - 10.2) < (30 - CA))
                                        {
                                            CAin = 292.5;
                                        }
                                        else
                                        {
                                            CAin = 270;
                                        }
                                    }
                                    else if (CA <= 10.2)
                                    {
                                        CAin = 292.5;
                                    }
                                    else
                                    {
                                        CAin = 202.5;
                                    }

                                    BA = Math.abs(AB);
                                    if (BA >= 69.8)
                                    {
                                        if ((BA - 69.8) < (90.5 - BA))
                                        {
                                            BAin = 315;
                                        }
                                        else
                                        {
                                            BAin = 337.5;
                                        }

                                    }
                                    else if (BA >= 49.8)
                                    {
                                        if ((BA - 49.8) < (69.8 - BA))
                                        {
                                            BAin = 292.5;
                                        }
                                        else
                                        {
                                            BAin = 315;
                                        }
                                    }
                                    else if (BA >= 30)
                                    {
                                        if ((BA - 30) < (49.8 - BA))
                                        {
                                            BAin = 270;
                                        }
                                        else
                                        {
                                            BAin = 292.5;
                                        }
                                    }
                                    else if (BA >= 10.2)
                                    {
                                        if ((BA - 10.2) < (30 - BA))
                                        {
                                            BAin = 247.5;
                                        }
                                        else
                                        {
                                            BAin = 270;
                                        }
                                    }
                                    else if (BA <= 10.2)
                                    {
                                        BAin = 247.5;
                                    }
                                    else
                                    {
                                        BAin = 337.5;
                                    }

                                    CB = Math.abs(BC);

                                    if (CB >= 59.4)
                                    {
                                        if ((CB - 59.4) < (82.5 - CB))
                                        {
                                            CBout = 292.5;
                                        }
                                        else
                                        {
                                            CBout = 270;
                                        }

                                    }
                                    else if (CB >= 37.5)
                                    {
                                        if ((CB - 37.5) < (59.4 - CB))
                                        {
                                            CBout = 315;
                                        }
                                        else
                                        {
                                            CBout = 292.5;
                                        }
                                    }
                                    else if (CB >= 16.6)
                                    {
                                        if ((CB - 16.6) < (37.5 - CB))
                                        {
                                            CBout = 337.5;
                                        }
                                        else
                                        {
                                            CBout = 315;
                                        }
                                    }
                                    else if (CB >= 9)
                                    {
                                        if ((CB - 9) < (16.6 - CB))
                                        {
                                            CBout = 0;
                                        }
                                        else
                                        {
                                            CBout = 337.5;
                                        }
                                    }
                                    else if (CB <= 9)
                                    {
                                        CBout = 0;
                                    }
                                    else
                                    {
                                        CBout = 270;
                                    }


                                    System.out.println("CAin " + CAin);
                                    System.out.println("BAin " + BAin);
                                    System.out.println("CBout " + CBout);

                                    makeDirectionAllEffects(CAin, BAin, CBout, timeOrder);
                                    Thread.sleep(1000);
                                    disableAllDirectionalEffects();

                                    break;
                                case "AB":

                                    if (AB >= 67)
                                    {
                                        if ((AB - 67) < (90.5 - AB))
                                        {
                                            ABout = 135;
                                        }
                                        else
                                        {
                                            ABout = 157.5;
                                        }
                                    }
                                    else if (AB >= 44.7)
                                    {
                                        if ((AB - 44.7) < (67 - AB))
                                        {
                                            ABout = 112.5;
                                        }
                                        else
                                        {
                                            ABout = 135;
                                        }
                                    }
                                    else if (AB >= 23.5)
                                    {
                                        if ((AB - 23.5) < (44.7 - AB))
                                        {
                                            ABout = 90;
                                        }
                                        else
                                        {
                                            ABout = 112.5;
                                        }
                                    }
                                    else if (AB >= 3.1)
                                    {
                                        if ((AB - 3.1) < (23.5 - AB))
                                        {
                                            ABout = 67.5;
                                        }
                                        else
                                        {
                                            ABout = 90;
                                        }
                                    }
                                    else if (AB <= 3.1)
                                    {
                                        ABout = 67.5;
                                    }
                                    else
                                    {
                                        ABout = 157.5;
                                    }

                                    System.out.println("ABout " + ABout);


                                    if (AB >= 76.6)
                                    {
                                        if ((AB - 76.6) < (97.5 - AB))
                                        {
                                            ABin = 337.5;
                                        }
                                        else
                                        {
                                            ABin = 315;
                                        }

                                    }
                                    else if (AB >= 56.4)
                                    {
                                        if ((AB - 56.4) < (76.6 - AB))
                                        {
                                            ABin = 0;
                                        }
                                        else
                                        {
                                            ABin = 337.5;
                                        }
                                    }
                                    else if (AB >= 36.6)
                                    {
                                        if ((AB - 36.6) < (56.4 - AB))
                                        {
                                            ABin = 22.5;
                                        }
                                        else
                                        {
                                            ABin = 0;
                                        }
                                    }
                                    else if (AB >= 16.8)
                                    {
                                        if ((AB - 16.8) < (36.6 - AB))
                                        {
                                            ABin = 45;
                                        }
                                        else
                                        {
                                            ABin = 22.5;
                                        }
                                    }
                                    else if (AB >= 5.5)
                                    {
                                        if ((AB - 5.5) < (16.8 - AB))
                                        {
                                            ABin = 67.5;
                                        }
                                        else
                                        {
                                            ABin = 45;
                                        }
                                    }
                                    else if (AB <= 5.5)
                                    {
                                        ABin = 67.5;
                                    }
                                    else
                                    {
                                        ABin = 315;
                                    }
                                    System.out.println("ABin " + ABin);

                                    singleDirection = (int) (ABout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    singleDirection = (int) (ABin / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();


                                    break;
                                case "AC":

                                    if (AC >= 67)
                                    {
                                        if ((AC - 67) < (90.5 - AC))
                                        {
                                            ACout = 45;
                                        }
                                        else
                                        {
                                            ACout = 22.5;
                                        }

                                    }
                                    else if (AC >= 44.7)
                                    {
                                        if ((AC - 44.7) < (67 - AC))
                                        {
                                            ACout = 67.5;
                                        }
                                        else
                                        {
                                            ACout = 45;
                                        }
                                    }
                                    else if (AC >= 23.5)
                                    {
                                        if ((AC - 23.5) < (44.7 - AC))
                                        {
                                            ACout = 90;
                                        }
                                        else
                                        {
                                            ACout = 67.5;
                                        }
                                    }
                                    else if (AC >= 3.1)
                                    {
                                        if ((AC - 3.1) < (23.5 - AC))
                                        {
                                            ACout = 112.5;
                                        }
                                        else
                                        {
                                            ACout = 90;
                                        }
                                    }
                                    else if (AC <= 3.1)
                                    {
                                        ACout = 112.55;
                                    }
                                    else
                                    {
                                        ACout = 22.4;
                                    }

                                    System.out.println("ACout " + ACout);

                                    if (AC >= 76.6)
                                    {
                                        if ((AC - 76.6) < (97.5 - AC))
                                        {
                                            ACin = 202.5;
                                        }
                                        else
                                        {
                                            ACin = 225;
                                        }

                                    }
                                    else if (AC >= 56.4)
                                    {
                                        if ((AC - 56.4) < (76.6 - AC))
                                        {
                                            ACin = 180;
                                        }
                                        else
                                        {
                                            ACin = 202.5;
                                        }
                                    }
                                    else if (AC >= 36.6)
                                    {
                                        if ((AC - 36.6) < (56.4 - AC))
                                        {
                                            ACin = 157.5;
                                        }
                                        else
                                        {
                                            ACin = 180;
                                        }
                                    }
                                    else if (AC >= 16.8)
                                    {
                                        if ((AC - 16.8) < (36.6 - AC))
                                        {
                                            ACin = 135;
                                        }
                                        else
                                        {
                                            ACin = 157.5;
                                        }
                                    }
                                    else if (AC >= 5.5)
                                    {
                                        if ((AC - 5.5) < (16.8 - AC))
                                        {
                                            ACin = 112.5;
                                        }
                                        else
                                        {
                                            ACin = 135;
                                        }
                                    }
                                    else if (AC <= 5.5)
                                    {
                                        ACin = 112.5;
                                    }
                                    else
                                    {
                                        ACin = 225;
                                    }

                                    System.out.println("ACin " + ACin);

                                    singleDirection = (int) (ACout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    singleDirection = (int) (ACin / 22.5);


                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();

                                    break;
                                case "BA":
                                    BA = Math.abs(AB);

                                    if (BA >= 74.7)
                                    {
                                        if ((BA - 74.7) < (98.7 - BA))
                                        {
                                            BAout = 157.5;
                                        }
                                        else
                                        {
                                            BAout = 135;
                                        }

                                    }
                                    else if (BA >= 52)
                                    {
                                        if ((BA - 52) < (74.7 - BA))
                                        {
                                            BAout = 180;
                                        }
                                        else
                                        {
                                            BAout = 157.5;
                                        }
                                    }
                                    else if (BA >= 30.5)
                                    {
                                        if ((BA - 30.5) < (52 - BA))
                                        {
                                            BAout = 202.5;
                                        }
                                        else
                                        {
                                            BAout = 180;
                                        }
                                    }
                                    else if (BA >= 9.8)
                                    {
                                        if ((BA - 9.8) < (30.5 - BA))
                                        {
                                            BAout = 225;
                                        }
                                        else
                                        {
                                            BAout = 202.5;
                                        }
                                    }
                                    else if (BA <= 9.8)
                                    {
                                        BAout = 225;
                                    }
                                    else
                                    {
                                        BAout = 135;
                                    }

                                    System.out.println("BAout " + BAout);

                                    if (BA >= 69.8)
                                    {
                                        if ((BA - 69.8) < (90.5 - BA))
                                        {
                                            BAin = 315;
                                        }
                                        else
                                        {
                                            BAin = 337.5;
                                        }

                                    }
                                    else if (BA >= 49.8)
                                    {
                                        if ((BA - 49.8) < (69.8 - BA))
                                        {
                                            BAin = 292.5;
                                        }
                                        else
                                        {
                                            BAin = 315;
                                        }
                                    }
                                    else if (BA >= 30)
                                    {
                                        if ((BA - 30) < (49.8 - BA))
                                        {
                                            BAin = 270;
                                        }
                                        else
                                        {
                                            BAin = 292.5;
                                        }
                                    }
                                    else if (BA >= 10.2)
                                    {
                                        if ((BA - 10.2) < (30 - BA))
                                        {
                                            BAin = 247.5;
                                        }
                                        else
                                        {
                                            BAin = 270;
                                        }
                                    }
                                    else if (BA <= 10.2)
                                    {
                                        BAin = 247.5;
                                    }
                                    else
                                    {
                                        BAin = 337.5;
                                    }

                                    System.out.println("BAin " + BAin);

                                    singleDirection = (int) (BAout / 22.5);


                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    singleDirection = (int) (BAin / 22.5);


                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    vibrateMotors();


                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();

                                    break;
                                case "CA":

                                    CA = Math.abs(AC);
                                    if (CA >= 74.7)
                                    {
                                        if ((CA - 74.7) < (98.7 - CA))
                                        {
                                            CAout = 22.5;
                                        }
                                        else
                                        {
                                            CAout = 45;
                                        }

                                    }
                                    else if (CA >= 52)
                                    {
                                        if ((CA - 52) < (74.7 - CA))
                                        {
                                            CAout = 0; //360 would also suffice
                                        }
                                        else
                                        {
                                            CAout = 22.5;
                                        }
                                    }
                                    else if (CA >= 30.5)
                                    {
                                        if ((CA - 30.5) < (52 - CA))
                                        {
                                            CAout = 337.5;
                                        }
                                        else
                                        {
                                            CAout = 0;
                                        }
                                    }
                                    else if (CA >= 9.8)
                                    {
                                        if ((CA - 9.8) < (30.5 - CA))
                                        {
                                            CAout = 315;
                                        }
                                        else
                                        {
                                            CAout = 337.5;
                                        }
                                    }
                                    else if (CA <= 9.8)
                                    {
                                        CAout = 315;
                                    }
                                    else
                                    {
                                        CAout = 45;
                                    }

                                    System.out.println("CAout " + CAout);


                                    CA = Math.abs(AC);
                                    if (CA >= 69.8)
                                    {
                                        if ((CA - 69.8) < (90.5 - CA))
                                        {
                                            CAin = 235;
                                        }
                                        else
                                        {
                                            CAin = 202.5;
                                        }

                                    }
                                    else if (CA >= 49.8)
                                    {
                                        if ((CA - 49.8) < (69.8 - CA))
                                        {
                                            CAin = 247.5;
                                        }
                                        else
                                        {
                                            CAin = 235;
                                        }
                                    }
                                    else if (CA >= 30)
                                    {
                                        if ((CA - 30) < (49.8 - CA))
                                        {
                                            CAin = 270;
                                        }
                                        else
                                        {
                                            CAin = 247.5;
                                        }
                                    }
                                    else if (CA >= 10.2)
                                    {
                                        if ((CA - 10.2) < (30 - CA))
                                        {
                                            CAin = 292.5;
                                        }
                                        else
                                        {
                                            CAin = 270;
                                        }
                                    }
                                    else if (CA <= 10.2)
                                    {
                                        CAin = 292.5;
                                    }
                                    else
                                    {
                                        CAin = 202.5;
                                    }

                                    System.out.println("CAin " + CAin);

                                    singleDirection = (int) (CAout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    singleDirection = (int) (CAin / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();


                                    break;
                                case "BC":
                                    if (BC >= 59.4)
                                    {
                                        if ((BC - 59.4) < (82.5 - BC))
                                        {
                                            BCout = 247.5;
                                        }
                                        else
                                        {
                                            BCout = 270;
                                        }

                                    }
                                    else if (BC >= 37.5)
                                    {
                                        if ((BC - 37.5) < (59.4 - BC))
                                        {
                                            BCout = 225;
                                        }
                                        else
                                        {
                                            BCout = 247.5;
                                        }
                                    }
                                    else if (BC >= 16.6)
                                    {
                                        if ((BC - 16.6) < (37.5 - BC))
                                        {
                                            BCout = 202.5;
                                        }
                                        else
                                        {
                                            BCout = 225;
                                        }
                                    }
                                    else if (BC >= 9)
                                    {
                                        if ((BC - 9) < (16.6 - BC))
                                        {
                                            BCout = 180;
                                        }
                                        else
                                        {
                                            BCout = 202.5;
                                        }
                                    }
                                    else if (BC <= 9)
                                    {
                                        BCout = 180;
                                    }
                                    else
                                    {
                                        BCout = 270;
                                    }
                                    System.out.println("BCout " + BCout);

                                    if (BC >= 63.1)
                                    {
                                        if ((BC - 63.1) < (83.5 - BC))
                                        {
                                            BCin = 112.5;
                                        }
                                        else
                                        {
                                            BCin = 90;
                                        }

                                    }
                                    else if (BC >= 43.2)
                                    {
                                        if ((BC - 43.2) < (63.1 - BC))
                                        {
                                            BCin = 135;
                                        }
                                        else
                                        {
                                            BCin = 112.5;
                                        }
                                    }
                                    else if (BC >= 23.4)
                                    {
                                        if ((BC - 23.4) < (43.2 - BC))
                                        {
                                            BCin = 157.5;
                                        }
                                        else
                                        {
                                            BCin = 135;
                                        }
                                    }
                                    else if (BC >= 3.6)
                                    {
                                        if ((BC - 3.6) < (23.4 - BC))
                                        {
                                            BCin = 180;
                                        }
                                        else
                                        {
                                            BCin = 157.5;
                                        }
                                    }
                                    else if (BC <= 3.6)
                                    {
                                        BCin = 180;
                                    }
                                    else
                                    {
                                        BCin = 90;
                                    }


                                    System.out.println("BCin " + BCin);


                                    singleDirection = (int) (BCout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    singleDirection = (int) (BCin / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();

                                    break;
                                case "CB":

                                    CB = Math.abs(BC);
                                    if (CB >= 59.4)
                                    {
                                        if ((CB - 59.4) < (82.5 - CB))
                                        {
                                            CBout = 292.5;
                                        }
                                        else
                                        {
                                            CBout = 270;
                                        }

                                    }
                                    else if (CB >= 37.5)
                                    {
                                        if ((CB - 37.5) < (59.4 - CB))
                                        {
                                            CBout = 315;
                                        }
                                        else
                                        {
                                            CBout = 292.5;
                                        }
                                    }
                                    else if (CB >= 16.6)
                                    {
                                        if ((CB - 16.6) < (37.5 - CB))
                                        {
                                            CBout = 337.5;
                                        }
                                        else
                                        {
                                            CBout = 315;
                                        }
                                    }
                                    else if (CB >= 9)
                                    {
                                        if ((CB - 9) < (16.6 - CB))
                                        {
                                            CBout = 0;
                                        }
                                        else
                                        {
                                            CBout = 337.5;
                                        }
                                    }
                                    else if (CB <= 9)
                                    {
                                        CBout = 0;
                                    }
                                    else
                                    {
                                        CBout = 270;
                                    }

                                    System.out.println("CBout " + CBout);


                                    CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                    if (CB >= 63.1)
                                    {
                                        if ((CB - 63.1) < (83.5 - CB))
                                        {
                                            CBin = 67.5;
                                        }
                                        else
                                        {
                                            CBin = 90;
                                        }

                                    }
                                    else if (CB >= 43.2)
                                    {
                                        if ((CB - 43.2) < (63.1 - CB))
                                        {
                                            CBin = 45;
                                        }
                                        else
                                        {
                                            CBin = 67.5;
                                        }
                                    }
                                    else if (CB >= 23.4)
                                    {
                                        if ((CB - 23.4) < (43.2 - CB))
                                        {
                                            CBin = 22.5;
                                        }
                                        else
                                        {
                                            CBin = 45;
                                        }
                                    }
                                    else if (CB >= 3.6)
                                    {
                                        if ((CB - 3.6) < (23.4 - CB))
                                        {
                                            CBin = 0;
                                        }
                                        else
                                        {
                                            CBin = 22.5;
                                        }
                                    }
                                    else if (CB <= 3.6)
                                    {
                                        CBin = 0;
                                    }
                                    else
                                    {
                                        CBin = 90;
                                    }

                                    System.out.println("CBin " + CBin);

                                    singleDirection = (int) (CBout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    singleDirection = (int) (CBin / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();


                                    break;
                                default:
                                    System.out.println("Do Nothing");
                            }
                        }
                        else if(largerEnviroment == false) {           //if option not selected then to the math for smaller localisations
                            switch (timeOrder) {
                                case "ABC":
                                    if (AB > 60) {
                                        ABout = 210;
                                    } else {
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
                                    if (AC > 60) {
                                        ACout = 330;
                                    } else if (AC > 46 && AC < 61) {
                                        ACout = (90 - (AB * 2)) + 360;
                                    } else {
                                        ACout = (90 - (AB * 2));
                                    }


                                    CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                    CBin = (((CB - 30) * 2) + 30);

                                    if (CBin < 0) {
                                        CBin = CBin + 360;

                                    }

                                    ABin = (90 - (AB * 2));

                                    if (ABin < 0) {
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
                                    if (BA > 60) {
                                        BAout = 90;
                                    } else {
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
                                    if (BC > 60) {
                                        BCout = 330;
                                    } else {
                                        BCout = (BC * 2) + 210;
                                    }

                                    BA = Math.abs(AB);

                                    BAin = ((BA * 2) + 210);

                                    if (BAin > 360) {
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

                                    if (CA > 60) {
                                        CAout = 90;
                                    } else if (CA < 15) {
                                        CAout = (90 - ((180 - (120 + CA)) * 2)) + 360;
                                    } else {
                                        CAout = 90 - ((180 - (120 + CA)) * 2);
                                    }


                                    CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                    CBin = (((CB - 30) * 2) + 30);

                                    if (CBin < 0) {
                                        CBin = CBin + 360;

                                    }

                                    ABin = (90 - (AB * 2));

                                    if (ABin < 0) {
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

                                    if (BAin > 360) {
                                        BAin = BAin - 360;
                                    }

                                    CB = Math.abs(BC);
                                    if (CB > 60) {
                                        CBout = 210;
                                    } else {
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
                                    if (AB > 60) {
                                        ABout = 210;
                                    } else {
                                        ABout = (AB * 2) + 90;
                                    }
                                    System.out.println("ABout " + ABout);


                                    ABin = (90 - (AB * 2));                                     //could be any of the two directions

                                    if (ABin < 0) {
                                        ABin = ABin + 360;
                                    }
                                    System.out.println("ABin " + ABin);

                                    singleDirection = (int) (ABout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    singleDirection = (int) (ABin / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();


                                    break;
                                case "AC":
                                    //adding boundary
                                    if (AC > 60) {
                                        ACout = 330;
                                    } else if (AC > 46 && AC < 61) {
                                        ACout = (90 - (AB * 2)) + 360;
                                    } else {
                                        ACout = (90 - (AB * 2));
                                    }
                                    System.out.println("ACout " + ACout);

                                    ACin = ((AC * 2) + 90);
                                    System.out.println("ACin " + ACin);

                                    singleDirection = (int) (ACout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    singleDirection = (int) (ACin / 22.5);


                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();

                                    break;
                                case "BA":
                                    BA = Math.abs(AB);
                                    if (BA > 60) {
                                        BAout = 90;
                                    } else {
                                        BAout = ((180 - (120 + BA)) * 2) + 90;
                                    }
                                    System.out.println("BAout " + BAout);

                                    BAin = ((BA * 2) + 210);

                                    if (BAin > 360) {
                                        BAin = BAin - 360;
                                    }

                                    System.out.println("BAin " + BAin);

                                    singleDirection = (int) (BAout / 22.5);


                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    singleDirection = (int) (BAin / 22.5);


                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    vibrateMotors();


                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();

                                    break;
                                case "CA":

                                    CA = Math.abs(AC);
                                    if (CA > 60) {
                                        CAout = 90;
                                    } else if (CA < 15) {
                                        CAout = (90 - ((180 - (120 + CA)) * 2)) + 360;
                                    } else {
                                        CAout = 90 - ((180 - (120 + CA)) * 2);
                                    }


                                    System.out.println("CAout " + CAout);


                                    CA = Math.abs(AC);
                                    CAin = (330 - (CA * 2));

                                    System.out.println("CAin " + CAin);

                                    singleDirection = (int) (CAout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    singleDirection = (int) (CAin / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();


                                    break;
                                case "BC":
                                    if (BC > 60) {
                                        BCout = 330;
                                    } else {
                                        BCout = (BC * 2) + 210;
                                    }
                                    System.out.println("BCout " + BCout);

                                    BCin = 210 - (BC * 2);

                                    System.out.println("BCin " + BCin);


                                    singleDirection = (int) (BCout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    singleDirection = (int) (BCin / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();

                                    break;
                                case "CB":

                                    CB = Math.abs(BC);
                                    if (CB > 60) {
                                        CBout = 210;
                                    } else {
                                        CBout = Math.abs(((180 - (120 + CB)) * 2)) + 210;
                                    }

                                    System.out.println("CBout " + CBout);


                                    CB = Math.abs(BC);   // may be wrong - but way of reversing BC to CB
                                    CBin = (((CB - 30) * 2) + 30);

                                    if (CBin < 0) {
                                        CBin = CBin + 360;

                                    }

                                    System.out.println("CB " + CB);

                                    singleDirection = (int) (CBout / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));


                                    singleDirection = (int) (CB / 22.5);
                                    directionalIndicators.get(singleDirection).setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                                    indicatorColourTracker.set(singleDirection, String.valueOf(android.R.color.holo_orange_dark));

                                    vibrateMotors();

                                    Thread.sleep(500);
                                    disableAllDirectionalEffects();


                                    break;
                                default:
                                    System.out.println("Do Nothing");
                            }

                        }

                        System.out.println("debuging  testing");


                    }



                } catch (Exception e) {}
            }
        }
    }





    void makeDirectionAllEffects(double angle1, double angle2, double angle3, String timeOrder)   //calculates the precise way that indicators need to be affecte by obtained angs so the correct indicate range is done
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

                    for (int index = minDirection ; index != msxDirection; index++)
                    {
                        if(index == indicator1)
                        {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_green_dark));
                            greenDirectionIndex = index;
                        }
                        else {
                            ImageView direction = directionalIndicators.get(index);


                            direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_orange_dark));
                        }
                    }

                    ImageView direction = directionalIndicators.get(msxDirection);


                    direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                    indicatorColourTracker.set(msxDirection, String.valueOf(android.R.color.holo_orange_dark));

                }
                else if (midToMin <= minToMax && midToMin <= maxToMid)  // mid to minroosterteeth
                {
                    for (int index = midDirection ; index != minDirection; index++)
                    {

                        if(index == indicator1)
                        {
                            ImageView direction = directionalIndicators.get(index);


                            direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_green_dark));
                            greenDirectionIndex = index;
                        }
                        else {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_orange_dark));
                        }

                        if(index == 16)
                        {
                            index = -1; //so that next loops starts at 0
                        }

                    }

                    ImageView direction = directionalIndicators.get(minDirection);


                    direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                    indicatorColourTracker.set(minDirection, String.valueOf(android.R.color.holo_orange_dark));

                }
                else if (maxToMid <= minToMax && maxToMid <= midToMin)  // max to mid
                {
                    for (int index = msxDirection ; index != midDirection; index++)
                    {
                        if(index == indicator1)
                        {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_green_dark));
                            greenDirectionIndex = index;
                        }
                        else {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_orange_dark));
                        }

                        if(index == 16)
                        {
                            index = -1; //so that next loops starts at 0 // could do this when index == 15 for same efffect also
                        }

                    }

                    ImageView direction = directionalIndicators.get(midDirection);

                    direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                    indicatorColourTracker.set(minDirection, String.valueOf(android.R.color.holo_orange_dark));
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
                        if(index == indicator1)
                        {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_green_dark));
                            greenDirectionIndex = index;

                        }
                        else {
                            ImageView direction = directionalIndicators.get(index);


                            direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_orange_dark));
                        }

                        if(index == 0)
                        {
                            index =16; //so that next loops starts at 15
                        }
                    }

                    //now do the max direction
                    ImageView direction = directionalIndicators.get(midDirection);


                    direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                    indicatorColourTracker.set(midDirection, String.valueOf(android.R.color.holo_orange_dark));
                }
                else if (midToMax <= minToMid && midToMax <= maxToMin)  // midToMax
                {
                    for (int index = midDirection; index != msxDirection; index--)
                    {
                        if(index == indicator1)
                        {
                            ImageView direction = directionalIndicators.get(index);


                            direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_green_dark));
                            greenDirectionIndex = index;
                        }
                        else {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_orange_dark));
                        }

                        if(index == 0)
                        {
                            index =16; //so that next loops starts at 15
                        }
                    }

                    //now do the max direction
                    ImageView direction = directionalIndicators.get(msxDirection);


                    direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                    indicatorColourTracker.set(msxDirection, String.valueOf(android.R.color.holo_orange_dark));

                }
                else if (maxToMin <= midToMax && maxToMin <= minToMid)  //maxToMin
                {
                    for (int index = msxDirection; index != minDirection; index--)
                    {
                        if(index == indicator1)
                        {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_green_dark));
                            greenDirectionIndex = index;
                        }
                        else {
                            ImageView direction = directionalIndicators.get(index);

                            direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_orange_dark));
                        }

                    }

                    ImageView direction = directionalIndicators.get(minDirection);

                    direction.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                    indicatorColourTracker.set(minDirection, String.valueOf(android.R.color.holo_orange_dark));


                }
            }



        }

        vibrateMotors();
        System.out.println("enabling multi directions" );

    }


    void vibrateMotors() //sends values to trigger certain motors
    {
        try {
            btSocket.getOutputStream().write(String.valueOf("j").getBytes());   //adding test thing here - remove if breaks
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (greenDirectionIndex != -1)
        {

            try {
                btSocket.getOutputStream().write(String.valueOf("o").getBytes());   //alter vibration type  111
            } catch (IOException e) {
                e.printStackTrace();
            }


            if(indicatorColourTracker.get(greenDirectionIndex).equals(String.valueOf(android.R.color.holo_green_dark))) // double checck this was marked green
            {
                if (greenDirectionIndex != 0 && (greenDirectionIndex % 2 == 0)) //if index even then its a motor so vibrate it
                {
                    int motorIndex = greenDirectionIndex / 2;
                    try {
                        btSocket.getOutputStream().write(String.valueOf(motorIndex).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Turning on motor " + greenDirectionIndex);                                  //sends to mega board appropriate value, if there a greem indicator and it refers to a motor then send vibration notis else give signal
                }
                else if (greenDirectionIndex == 0)
                {
                    try {
                        btSocket.getOutputStream().write(String.valueOf(greenDirectionIndex).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Turning on motor " + greenDirectionIndex);
                }
                else
                {
                    try {
                        btSocket.getOutputStream().write("m".getBytes()); //109 send value to indicate not valid
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            greenDirectionIndex = -1;
        }



        for (int index = 0; index <= 15; index++) //turn on motors that are orange
        {
            ImageView direction = directionalIndicators.get(index);


            if(/*indicatorColourTracker.get(index).equals(String.valueOf(android.R.color.holo_green_dark)) ||*/ indicatorColourTracker.get(index).equals(String.valueOf(android.R.color.holo_orange_dark)) )
            {
                try {
                    int motorIndex;
                    if (index != 0 /*&& (index % 2 == 0)*/) //if index even then its a motor so vibrate it
                    {
                        motorIndex = index / 2;
                        btSocket.getOutputStream().write(String.valueOf(motorIndex).getBytes());
                        System.out.println("Turning on motor " + index);
                    }
                    if (index == 0)
                    {
                        btSocket.getOutputStream().write(String.valueOf(index).getBytes());
                        System.out.println("Turning on motor " + index);
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error at vibrate motros sectioin");
                }


            }


        }


        try {
            btSocket.getOutputStream().write(String.valueOf("k").getBytes());   //adding test thing here - remove if breaks
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("enabled motors" );
    }




    void disableAllDirectionalEffects()                                     //restore all indicators to red and send signal to Mega board to turn of all motors
    {
        for (int index = 0; index <= 15; index++)
        {
            ImageView direction = directionalIndicators.get(index);

            direction.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
            indicatorColourTracker.set(index, String.valueOf(android.R.color.holo_red_dark));
        }

        try {
            btSocket.getOutputStream().write(String.valueOf("l").getBytes());   //l 108
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("disable directions" );
    }



    //STT
    @Override
    public void onDroidSpeechSupportedLanguages(String currentSpeechLanguage, List<String> supportedSpeechLanguages) {

    }

    @Override
    public void onDroidSpeechRmsChanged(float rmsChangedValue) {

    }

    @Override
    public void onDroidSpeechLiveResult(String liveSpeechResult) {                                          //live transcribe of speach

        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        speechToOnScreenText.append(currentDateTimeString + " : "  + liveSpeechResult + "\n");                  //out put spoken words time stamped

        scrollSTT.fullScroll(View.FOCUS_DOWN); //auto scrolls down


        if(t1 != null && (liveSpeechResult.toLowerCase().contains(t1.getText().toString().toLowerCase())))   //if atleast 1 of the trigger words are said then vibrate motor for 1.5 seconds
        {
            vibratePhone();
        }
        else if (t2 != null && (liveSpeechResult.toLowerCase().contains(t2.getText().toString().toLowerCase())))
        {
            vibratePhone();
        }
        else if (t3 != null && (liveSpeechResult.toLowerCase().contains(t3.getText().toString().toLowerCase())))
        {
            vibratePhone();
        }
        else if (t4 != null && (liveSpeechResult.toLowerCase().contains(t4.getText().toString().toLowerCase())))
        {
            vibratePhone();
        }
        else if (t5 != null && (liveSpeechResult.toLowerCase().contains(t5.getText().toString().toLowerCase())))
        {
            vibratePhone();
        }



    }

    @Override
    public void onDroidSpeechFinalResult(String finalSpeechResult) {

    }

    @Override
    public void onDroidSpeechClosedByUser() {

    }

    @Override
    public void onDroidSpeechError(String errorMsg) {

    }

    private void vibratePhone() {                                                   //vibrate phone for 1.5 seconds
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1500);
        }
    }



    private void dialogSTT1()                                                       //show current trigger is dialog box
    {
        AlertDialog.Builder dialogBoxBuilder = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View triggersView = layoutInflater.inflate(R.layout.dialogbox_trigger_words, null);

        t1 = triggersView.findViewById(R.id.trigger1);                                      //assign view trigger buttons to t variables - so they can be manipulated
        t2 = triggersView.findViewById(R.id.trigger2);
        t3 = triggersView.findViewById(R.id.trigger3);
        t4 = triggersView.findViewById(R.id.trigger4);
        t5 = triggersView.findViewById(R.id.trigger5);

        if (triggerArray.get(0) != "")
        {
            t1.setText(triggerArray.get(0));                                           //makes sures triggers are assigned correct values
        }

        if (triggerArray.get(1) != "")
        {
            t2.setText(triggerArray.get(1));
        }

        if (triggerArray.get(2) != "")
        {
            t3.setText(triggerArray.get(2));
        }

        if (triggerArray.get(3) != "")
        {
            t4.setText(triggerArray.get(3));
        }

        if (triggerArray.get(4) != "")
        {
            t5.setText(triggerArray.get(4));
        }

        dialogBoxBuilder.setView(triggersView);



        t1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {                             //long press button to bring up button that allows for edit text
                dialogTrigger2(t1);

                return true;
            }
        });



        t2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogTrigger2(t2);
                return true;
            }
        });


        t3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogTrigger2(t3);
                return true;
            }
        });


        t4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogTrigger2(t4);
                return true;
            }
        });



        t5.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialogTrigger2(t5);
                return true;
            }
        });



        dialogBoxBuilder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
            }
        });

        dialogBoxBuilder.show();
    }



    private void dialogTrigger2(final Button triggerBtnSelected)    //setes trigger buttons to user inputted text
    {
        AlertDialog.Builder dialogBoxBuilder2 = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View premadeView = layoutInflater.inflate(R.layout.edit_premade_text, null);                                 //variables initialised to refer to correct view
        final EditText textForTriggerWords = (EditText)premadeView.findViewById(R.id.premadeEditTxt);
        dialogBoxBuilder2.setView(premadeView);



        dialogBoxBuilder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {                                                //dialog box

                int triggerNumber = -1;

                if (triggerBtnSelected.getId() == t1.getId())
                {
                    t1.setText(textForTriggerWords.getText().toString());                                                   //assign trigger the inserted text
                    triggerNumber = 0;
                }
                else if (triggerBtnSelected.getId() == t2.getId())
                {
                    t2.setText(textForTriggerWords.getText().toString());
                    triggerNumber = 1;
                }
                else if (triggerBtnSelected.getId() == t3.getId())
                {
                    t3.setText(textForTriggerWords.getText().toString());
                    triggerNumber = 2;
                }
                else if (triggerBtnSelected.getId() == t4.getId())
                {
                    t4.setText(textForTriggerWords.getText().toString());
                    triggerNumber = 3;
                }
                else if (triggerBtnSelected.getId() == t5.getId())
                {
                    t5.setText(textForTriggerWords.getText().toString());
                    triggerNumber = 4;
                }



                if(textForTriggerWords.getText().toString() != "")
                {
                    triggerArray.set(triggerNumber, textForTriggerWords.getText().toString());
                }


                StringBuilder sb = new StringBuilder();                                                             //build string with list of all strings amended into 1 for saving purposes
                for(String allPremadesStr : triggerArray)
                {
                    sb.append(allPremadesStr);
                    sb.append(",");
                }

                SharedPreferences saving = getSharedPreferences("Preferences", 0);                      //saves changes to application data for next time
                SharedPreferences.Editor editSave = saving.edit();
                editSave.putString("triggers", sb.toString());
                editSave.commit();

            }
        });

        dialogBoxBuilder2.setNegativeButton(" ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {



            }
        });


        dialogBoxBuilder2.show();
    }

    //ESTT







    void initialisaColoursForDirections()           //default colour for indicators - same amount as indicators so they are inline to track
    {
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
        indicatorColourTracker.add(String.valueOf(android.R.color.holo_red_dark));
    }


    void initialiseDirectionalIndicators()      //adds on screens indicators to array
    {
        directionalIndicators.add((ImageView) findViewById(R.id.right)  );                     //adding direction indicators images to an array //0
        directionalIndicators.add((ImageView) findViewById(R.id.rightMiddleFrontRight)  );     //1
        directionalIndicators.add((ImageView) findViewById(R.id.frontRight) );                 //2
        directionalIndicators.add((ImageView) findViewById(R.id.frontMiddleFrontRight)  );     //3
        directionalIndicators.add((ImageView) findViewById(R.id.front)  );                     //4
        directionalIndicators.add((ImageView) findViewById(R.id.frontLeftMiddleFront)  );      //5
        directionalIndicators.add((ImageView) findViewById(R.id.frontLeft)  );                 //6
        directionalIndicators.add((ImageView) findViewById(R.id.leftMiddleFrontLeft)  );       //7
        directionalIndicators.add((ImageView) findViewById(R.id.left)  );                      //8
        directionalIndicators.add((ImageView) findViewById(R.id.leftMiddleBackLeft)  );        //9
        directionalIndicators.add((ImageView) findViewById(R.id.backLeft)  );                  //10
        directionalIndicators.add((ImageView) findViewById(R.id.backMiddleBackLeft)  );        //11
        directionalIndicators.add((ImageView) findViewById(R.id.back)  );                      //12
        directionalIndicators.add((ImageView) findViewById(R.id.backMiddleBackRight)  );       //13
        directionalIndicators.add((ImageView) findViewById(R.id.backRight)  );                 //14
        directionalIndicators.add((ImageView) findViewById(R.id.rightMiddleBackRight)  );      //15
        directionalIndicators.add((ImageView) findViewById(R.id.right)  );                     //16   --adding again yes
    }


}
