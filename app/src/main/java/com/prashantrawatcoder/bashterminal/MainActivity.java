
package com.prashantrawatcoder.bashterminal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.prashantrawatcoder.bashterminal.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    
    // Text Output of terminal is displayed on screenText.
    TextView screenText;
    
    // Gets command entered by user
    EditText commandText ;
    
    // ScrollView in which output text is displayed
    // used to scroll to bottom automatically whenever new output is displayed
    ScrollView scroll;
    
    TerminalSession mainTerminalSession;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        checkAppPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,41);
        checkAppPermission(Manifest.permission.READ_EXTERNAL_STORAGE,42);
        
        scroll = (ScrollView) findViewById(R.id.scrollview);
        screenText= (TextView) findViewById(R.id.screenText);
        commandText = (EditText) findViewById(R.id.command);
        // Getting user input continiously
        commandText.setOnEditorActionListener(commandInputHandler);
        
        mainTerminalSession= new TerminalSession(MainActivity.this,getApplicationInfo().dataDir);
        // Starting Thread which reads output of PTM and displayes the output on screenText
        mainTerminalSession.startPtmReaderThread();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
    
    // Handel Input entered by user in EditText - sends input to PTM writer and clear the text of Edittext
    TextView.OnEditorActionListener commandInputHandler = new TextView.OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // checks if next button is pressed on virtual keyboard , OR if enter button is pressed on PHysical keyboard
        if ((actionId == EditorInfo.IME_ACTION_NEXT ) || 
            (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            String command = commandText.getText().toString();
            mainTerminalSession.writeToPtm(command + "\n");
                
            //makes text of editText empty again
            commandText.setText("");
            return true;
        }
        return false;
    }
};
    
    
    // called by mainTerminalSession.startPtmReaderThread() whenever a new output is readed from PTM
    public void updateText(String outputText){
        // it updates the text in ui thread , it is not good to update the text from mainTerminalSession.startPtmReaderThread
        // Because it will stop reading in between if we do so
        runOnUiThread(() -> {
                screenText.setText(outputText);
                // scroll down
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                // set text of commandText to empty
                commandText.post(() -> commandText.requestFocus());
                }
            );
        
    }
    
    public void checkAppPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
  }
    
    
}
