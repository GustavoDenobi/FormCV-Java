package com.formcv;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Config {

    private int[] pattern = {0, 1, 2, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 0, 0, 1, 2, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3};
    private String[] months = {"JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"};
    private String[] cvFormats = {".jpeg", ".jpg", ".png", ".bmp"};
    private String logDir, databaseFile, errorLogFile, imgDir, certificateDir;
    private int imgPreviewSize, minimumHours;
    private float threshold;
    private boolean inDatabase;
    private String iniFile = "config.ini";


    public Config() {
        this.reloadIni();
    }

    public void reloadIni() {
        try {
            Wini ini = new Wini(new File(this.iniFile));
            this.logDir = ini.get("const", "logdir", String.class);
            this.databaseFile = ini.get("const", "databasefile", String.class);
            this.errorLogFile = ini.get("const", "errorlogfile", String.class);
            this.imgDir = ini.get("const", "imgdir", String.class);
            this.certificateDir = ini.get("const", "certificatedir", String.class);
            this.imgPreviewSize = ini.get("const", "imgpreviewsize", int.class);
            this.minimumHours = ini.get("const", "minimumhours", int.class);
            this.threshold = ini.get("const", "threshold", float.class);
        } catch (InvalidFileFormatException e) {
            System.out.println("Invalid file format.");
        } catch (IOException e) {
            System.out.println("Problem reading file.");
        }
    }

    public void setParameter(String parameter, String value) {
        try {
            Wini ini = new Wini(new File(this.iniFile));
            ini.put("const", parameter, value);
        } catch (InvalidFileFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkBackup() {
        try {
            Wini ini;
            ini = new Wini(new File(this.iniFile));
            int lastBackup = ini.get("backup", "lastbackup", int.class);
            int maxDays = ini.get("backup", "maxdays", int.class);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            Date date = new Date(System.currentTimeMillis());
            int now = Integer.parseInt(formatter.format(date));
            if(now - lastBackup >= maxDays) {
                System.out.println("Maximum backup time exceeded: " + (now - lastBackup) + " days");
                return true;
            } else {
                System.out.println("No backup scheduled.");
                return false;
            }
        } catch (InvalidFileFormatException e) {
            System.out.println("Invalid file format.");
        } catch (IOException e) {
            System.out.println("Problem reading file.");
        }
        return false;
    }
}
