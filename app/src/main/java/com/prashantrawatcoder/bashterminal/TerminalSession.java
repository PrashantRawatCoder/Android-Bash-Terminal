package com.prashantrawatcoder.bashterminal;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;


public class TerminalSession {
    static {
            System.loadLibrary("terminal");
        }
    private String Output="";
    int[] processID = {};
    FileInputStream TerminalInputStream;
    FileOutputStream TerminalOutputStream;
    ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
    TerminalSession (){
        String[] args={"sh"};
        try {
            int ptm=createTerminalSession("sh","/sdcard/",args,processID);
            Output+= "PTM : "+ptm+"\n";
            TerminalInputStream =  new FileInputStream(wrapFileDescriptor(ptm));
            TerminalOutputStream =  new FileOutputStream(wrapFileDescriptor(ptm));
            } catch (Exception error){
                Output+=error+"\n";
            }
        
    }

  public void runCommand(String script) {
        try{
            TerminalOutputStream.write((script+"\n").getBytes());
            output();
            } catch (Exception error){
                Output = Output + error;
            }
        }
        
  public String output() {
    try{
        int length = TerminalInputStream.read(buffer);
        result.write(buffer, 0, length);
        Output = result.toString("UTF-8");
        } catch (Exception error){
            Output = Output + error;
        }
        return Output;
  }
    
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
