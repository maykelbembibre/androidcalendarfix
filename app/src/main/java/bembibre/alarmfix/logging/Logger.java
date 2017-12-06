package bembibre.alarmfix.logging;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by REGALIZNEGRO on 02/12/2017.
 */

public class Logger {
    private static final String DIRECTORY_NAME = "Alarm fix logs";
    private static final int PRESERVED_FILES = 3;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final DateFormat FILE_NAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM");
    private static File logsDirectory;
    private static List<String> managedFiles;

    public static void log(String message) {
        Logger.write(message + "\n");
    }

    public static void log(Throwable t) {
        StringBuffer stringBuffer = new StringBuffer(" Ocurrió una excepción: " + t.getClass().getName() + "\n");
        String detailMessage = t.getMessage();
        if ((detailMessage != null) && (!detailMessage.isEmpty())) {
            stringBuffer.append("Descripción del error: " + detailMessage + "\n");
        }
        StackTraceElement[] stackTraceElements = t.getStackTrace();
        if (stackTraceElements != null) {
            boolean first = true;
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                if (!first) {
                    stringBuffer.append("\n");
                }
                stringBuffer.append("at " + stackTraceElement.toString());
                first = false;
            }
        }
        stringBuffer.append("\n\n");
        Logger.write(stringBuffer.toString());
    }

    public static void log(String message, Throwable t) {
        Logger.log(message + " - Acompañando a este error se produjo la siguiente excepción:\n");
        Logger.log(t);
    }

    synchronized private static void write(String text) {
        File log = Logger.getCurrentFile();
        if (log == null) {
            // La hemos jodido.
            System.out.println("No hay acceso de escritura a la memoria interna del dispositivo.");
        } else {
            FileOutputStream outputStream = null;
            OutputStreamWriter writer = null;
            BufferedWriter bufferedWriter = null;
            try {
                outputStream = new FileOutputStream(log, true);
                writer = new OutputStreamWriter(outputStream);
                bufferedWriter = new BufferedWriter(writer);
                String message = Logger.getLogPrefix() + text;
                bufferedWriter.write(message);
                System.out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static File getLogsDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (Logger.logsDirectory == null) {
                Logger.logsDirectory = new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);
            }
            if (!Logger.logsDirectory.exists()) {
                boolean created = Logger.logsDirectory.mkdir();
                if (!created) {
                    Logger.logsDirectory = null;
                }
            }
        } else {
            Logger.logsDirectory = null;
        }
        return Logger.logsDirectory;
    }

    private static String getCurrentFileName() {
        return FILE_NAME_DATE_FORMAT.format(new Date()) + ".txt";
    }

    /**
     *
     * @param logsDirectory directory that stores logs. null is not allowed.
     * @return
     */
    private static List<String> getManagedFiles(File logsDirectory) {
        if (Logger.managedFiles == null) {
            File[] files = logsDirectory.listFiles();
            Logger.managedFiles = new ArrayList<>();
            for (File file : files) {
                Logger.managedFiles.add(file.getName());
            }
            Collections.sort(Logger.managedFiles, String.CASE_INSENSITIVE_ORDER);
        }
        return Logger.managedFiles;
    }

    private static File getCurrentFile() {
        String currentFileName = Logger.getCurrentFileName();
        File logsDirectory = Logger.getLogsDirectory();
        File currentFile;
        if (logsDirectory == null) {
            currentFile = null;
        } else {
            currentFile = new File(logsDirectory, currentFileName);
            if (!currentFile.exists()) {
                List<String> managedFiles = Logger.getManagedFiles(logsDirectory);
                int length = managedFiles.size();
                int top = length - Logger.PRESERVED_FILES;
                int index;
                File file;
                String name;
                for (index = 0;index < top;index++) {
                    name = managedFiles.get(index);
                    file = new File(logsDirectory, name);

                    // If the file doesn't exist, returns false and nothing happens.
                    file.delete();
                    managedFiles.remove(index);
                    System.out.println("Removed log file for obsolescence: " + name);
                }
                try {
                    currentFile.createNewFile();
                } catch (IOException e) {
                    currentFile = null;
                }
                if (currentFile != null) {
                    managedFiles.add(currentFileName);
                }
            }
        }
        return currentFile;
    }

    private static String getLogPrefix() {
        return "[" + Logger.DATE_FORMAT.format(new Date()) + "] ";
    }
}
