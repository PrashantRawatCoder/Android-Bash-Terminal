
package com.prashantrawatcoder.bashterminal;

import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.prashantrawatcoder.bashterminal.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    TextView screenText;
    TerminalSession session= new TerminalSession();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        checkAppPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,41);
        checkAppPermission(Manifest.permission.READ_EXTERNAL_STORAGE,42);
        
        screenText= (TextView) findViewById(R.id.screenText);
        screenText.setText(session.output());
        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
    
    public void checkAppPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
  }
    
    public void command(View view) {
        String script = ((EditText) findViewById(R.id.command)).getText().toString();
        session.runCommand(script);
        screenText.setText(session.output());
    }
}
