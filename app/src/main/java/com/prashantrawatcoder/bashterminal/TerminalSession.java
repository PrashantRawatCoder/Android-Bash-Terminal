package com.prashantrawatcoder.bashterminal;


import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

public class TerminalSession {
    static {
            System.loadLibrary("terminal");
        }
    // Used to call update text function
    final private MainActivity mainInstance;
    
    private final int MAX_OUTPUT_SIZE;
    
    private final int ptm;
    
    private final FileDescriptor terminalFileDescriptor ;
    
    // Used to write commands entered by user to Ptm
    private final FileOutputStream UiToPtmStream ;
    
    // Used to store Output of PTM
    String PtmToUiStream ;
    
    
    private final String[] args={"sh"};
    private int[] processID={};
    TerminalSession(MainActivity mainInstanceParam,String CWD){
        this.mainInstance = mainInstanceParam;
         MAX_OUTPUT_SIZE=4096;
         ptm=createTerminalSession("sh",CWD,args,processID);
         terminalFileDescriptor = wrapFileDescriptor(ptm);
         UiToPtmStream = new FileOutputStream(terminalFileDescriptor);
         PtmToUiStream = "Pseudoterminal Master : " + ptm +"\n";
    
    }
    

    public void writeToPtm(String command){
                try {
                        UiToPtmStream.write(command.getBytes());
                } catch (Exception e) {
                    PtmToUiStream += "\n[ERROR] Cannot write to ptm : " + e +"\n";
                }
            }
    
    public void startPtmReaderThread(){
        new Thread("Input thread [PID=" + processID + "]") {
            @Override
            public void run() {
                try (FileInputStream termIn = new FileInputStream(terminalFileDescriptor)) {
                    final byte[] buffer = new byte[1024];
                    while (true) {
                        int read = termIn.read(buffer);
                        if (read == -1){ 
                            PtmToUiStream += "\nPTM Input Stream Closed , Returned -1.\n";
                            mainInstance.updateText(PtmToUiStream);
                            return ;
                            }
                        if (read>0){
                            PtmToUiStream += new String(buffer, 0, read, StandardCharsets.UTF_8);
                            if (PtmToUiStream.length()>MAX_OUTPUT_SIZE){
                                PtmToUiStream=PtmToUiStream.substring(PtmToUiStream.length() - MAX_OUTPUT_SIZE ,MAX_OUTPUT_SIZE);
                            }
                            mainInstance.updateText(PtmToUiStream);
                            }
                        }
                    } catch (Exception error) {
                    PtmToUiStream += "Input Thread error : "+error;
                    mainInstance.updateText(PtmToUiStream);
                }
                
            }
        }.start();
    }
    
    // used so we can open the file of PTM , we cannot directly open file descriptor of ptm
    private static FileDescriptor wrapFileDescriptor(int fileDescriptor) {
        FileDescriptor result = new FileDescriptor();
        try {
            Field descriptorField;
            try {
                descriptorField = FileDescriptor.class.getDeclaredField("descriptor");
            } catch (NoSuchFieldException e) {
                // For desktop java:
                descriptorField = FileDescriptor.class.getDeclaredField("fd");
            }
            descriptorField.setAccessible(true);
            descriptorField.set(result, fileDescriptor);
        } catch (Exception error) {
            System.exit(1);
        }
        return result;
    }

    public native int createTerminalSession(String cmd,String cwd, Object[] args, int[] processID);
}
