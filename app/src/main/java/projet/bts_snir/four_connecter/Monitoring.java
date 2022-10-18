package projet.bts_snir.four_connecter;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class Monitoring extends AppCompatActivity {

    private TextView mTextTime;
    private Button mStopBtn;
    private CountDownTimer countDownTimer;
    private boolean timeRunning;
    private SerialService service;
    private TextView receiveText;

    private long timeCountDown;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




   /* @Override
    public void onBackPressed()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(Monitoring.this);

        builder.setMessage("Celà pourrait corrompre le processus. Voulez vous vraiment quitter ?");
        builder.setTitle("Alerte !");

        builder.setCancelable(false);

        builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                finish();
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
        alertDialog.show();*/
    }

    /*
     * =============================================================================================
     */


    private void stopFour(){
        AlertDialog.Builder builder = new AlertDialog.Builder(Monitoring.this);

        builder.setMessage("Voulez vous vraiment arrêter le processus ?");
        builder.setTitle("Attention !");

        builder.setCancelable(false);

        builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

                finish();
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

    private void modifierLeTexte(){
        int minutes = (int) (timeCountDown/1000/60);
        int seconds = (int) (timeCountDown/1000%60);
        String tempsRestant = String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds);
        mTextTime.setText(tempsRestant);
    }
}