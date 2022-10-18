package projet.bts_snir.four_connecter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayList;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected {False, Pending, True}

    private View view;
    private String deviceAddress;
    private static SerialService service;


    private TextView receiveText;
    private TextView T1, T2, T3, TEMP;
    private AlertDialog alertDialog;
    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;
    private ArrayList<String> txt = new ArrayList<>();
    private View dialogView;
    /*
     * =============================================================================================
     */

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("affirmatif");

    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class));
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("déprécier")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * =============================================================================================
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());


        T1 = view.findViewById(R.id.T1);
        T2 = view.findViewById(R.id.T2);
        T3 = view.findViewById(R.id.T3);
        TEMP = view.findViewById(R.id.TEMP);


        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> {
            String sT1 = T1.getText().toString().trim();
            String sT2 = T2.getText().toString().trim();
            String sT3 = T3.getText().toString().trim();
            String sTEMP = TEMP.getText().toString().trim();
            if (sT1.equals("") || sT2.equals("") || sT3.equals("") || sTEMP.equals(""))
                Toast.makeText(view.getContext(), "Veillez remplir tous les paramètres.", Toast.LENGTH_SHORT).show();
            else {

                sendParam(sT1, sT2, sT3, sTEMP,v);
            }


        });

        return view;
    }

    /*
     * =============================================================================================
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("Connexion en cours...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void sendParam(String T1, String T2, String T3, String TEMP,View v) {
        // Convertir String to Int
        int iT1 = Integer.parseInt(T1);
        int iT2 = Integer.parseInt(T2);
        int iT3 = Integer.parseInt(T3);
        int iTEMP = Integer.parseInt(TEMP);
        // Condition du paramètrage
        if (iT1 < 15 || iT1 > 60)
            Toast.makeText(getActivity(), "La valeur " + T1 + " n'est pas accepté, elle doit être entre 15 et 60 min.", Toast.LENGTH_SHORT).show();
        else if (iT2 < 0 || iT2 > 120)
            Toast.makeText(getActivity(), "La valeur " + T2 + " n'est pas accepté, elle doit être entre 0 et 120 min", Toast.LENGTH_SHORT).show();
        else if (iT3 < 0 || iT3 > 360)
            Toast.makeText(getActivity(), "La valeur " + T3 + " n'est pas accepté, elle doit être entre 0 et 360 min", Toast.LENGTH_SHORT).show();
        else if (iTEMP < 300 || iTEMP > 920)
            Toast.makeText(getActivity(), "La valeur " + TEMP + " n'est pas accepté, elle doit être entre 300° et 920°", Toast.LENGTH_SHORT).show();
        else {
            try {
                String trame;
                String msg;
                byte[] data;
                trame = "$FOUR;PARAM;" + T1 + ";" + T2 + ";" + T3 + ";" + TEMP + "\n\r";
                msg = trame;
                data = (trame + newline).getBytes();
                SpannableStringBuilder spn = new SpannableStringBuilder(msg);
                spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                //receiveText.append(spn);
                service.write(data);

                // AlertDialg
                alertDialogue(v);


                Toast.makeText(getActivity(), "Envoyé", Toast.LENGTH_SHORT).show();
                // Lancement d'une nouvelle Activity
            } catch (Exception e) {
                onSerialIoError(e);
            }
        }
    }

    private  void alertDialogue(View v1){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ViewGroup viewGroup = view.findViewById(android.R.id.content);
        dialogView = LayoutInflater.from(v1.getContext()).inflate(R.layout.activity_monitoring, viewGroup, false);
        builder.setView(dialogView);
        builder.setCancelable(false);
        alertDialog = builder.create();

        alertDialog.show();
        Button mButton = dialogView.findViewById(R.id.btn_stop);
        mButton.setOnClickListener(v->{
            stopFour();
        });


    }
    private void stopFour(){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        builder.setMessage("Voulez vous vraiment arrêter le processus ?");
        builder.setTitle("Attention !");

        builder.setCancelable(false);

        builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                sendStop();
                alertDialog.dismiss();
            }
        });

        builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static void sendStop() {
        String trame;
        byte[] data;
        trame = "$FOUR;STOP\n\r";
        data = (trame + '\n').getBytes();
        try {
            service.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void receive(byte[] data) {
        String msg = new String(data);

        if (msg.equals("$")){
            txt = new ArrayList<>();
        }else if(msg.equals("\r")||msg.equals("r")){
            classerInfo();
        }else if (msg.equals(newline)||msg.equals("\\")||msg.equals("n")){
    }
        else {
            txt.add(msg);
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void classerInfo(){
        String msg = String.join("", txt);
        String[] arrayList;
        arrayList= msg.split(";");
        TextView TVtemperature = dialogView.findViewById(R.id.tvTemp);
        TextView TVphase = dialogView.findViewById(R.id.TvPhase);
        TextView TVTemps = dialogView.findViewById(R.id.time);
        TVtemperature.setText(arrayList[2]+"°");
        TVphase.setText(arrayList[3]);
        TVTemps.setText(arrayList[4]);
        if (arrayList[4].equals("00:00")){
            Toast.makeText(getActivity(), "Terminer", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        }


    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * =============================================================================================
     */
    @Override
    public void onSerialConnect() {
        status("Connecté");
        connected = Connected.True;
        String trame;
        byte[] data;
        trame = "$FOUR;START\n\r";
        data = (trame + '\n').getBytes();
        try {
            service.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("Connexion échouée : " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("Connexion perdue : " + e.getMessage());
        disconnect();
    }

}