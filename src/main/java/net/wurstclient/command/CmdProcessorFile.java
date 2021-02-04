package net.wurstclient.command;

import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CmdProcessorFile {

    public static void writePrefix(String prefix){
        // attach a file to FileWriter
        try{
            FileWriter fw = new FileWriter("wurst/prefix.txt");

            // read character wise from string and write
            // into FileWriter
            for (int i = 0; i < prefix.length(); i++)
                fw.write(prefix.charAt(i));

            //close the file
            fw.close();
        }
        catch (IOException io){

        }
    }

    public static String readPrefix(){
        // variable declaration
        int ch;

        // check if File exists or not
        FileReader fr=null;
        try
        {
            fr = new FileReader("wurst/prefix.txt");
        }
        catch (FileNotFoundException fe)
        {
            createFile();
            return ".";
        }

        String prefix = "";

        try {
            // read from FileReader till the end of file
            while ((ch=fr.read())!=-1)
                prefix += (char)ch;

            // close the file
            fr.close();

            if(prefix.length()>=1 && prefix.charAt(0) != '/') {
                return prefix;
            }
            else {
                createFile();
                return ".";
            }
        }
        catch (IOException io){
            createFile();
            return ".";
        }
    }

    private static void createFile(){
        writePrefix(".");
    }
}
