package bembibre.alarmfix.storage;

/**
 * Created by Max Power on 13/12/2017.
 */

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import bembibre.alarmfix.logging.Logger;

/**
 * Class for dealing with secondary storage, for saving files and directories.
 */
public class Storage {

    /**
     * Returns a directory for the application. If it doesn't still exist, it gets created.
     *
     * @param directoryName name of the directory.
     * @return the directory or <code>null</code> if there is no access to the secondary storage;
     * e.g. lack of permission.
     */
    public static File getApplicationDirectory(String directoryName) {
        File directory;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            directory = new File(Environment.getExternalStorageDirectory(), directoryName);
            if (!directory.exists()) {
                boolean created = directory.mkdir();
                if (!created) {
                    directory = null;
                }
            }
        } else {
            directory = null;
        }
        return directory;
    }

    public static boolean writeStringToFile(File file, String string, boolean append) {
        boolean ok;
        FileOutputStream outputStream = null;
        OutputStreamWriter writer = null;
        BufferedWriter bufferedWriter = null;
        try {
            outputStream = new FileOutputStream(file, append);
            writer = new OutputStreamWriter(outputStream);
            bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(string);
            ok = true;
        } catch (IOException e) {
            ok = false;
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
        return ok;
    }

    public static String readStringFromFile(File file) {
        StringBuilder content;
        FileInputStream inputStream = null;
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = new FileInputStream(file);
            reader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(reader);
            String line;
            content = new StringBuilder();
            boolean first = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (!first) {
                    content.append('\n');
                }
                content.append(line);
                first = false;
            }
        } catch (IOException e) {
            content = null;
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String result;
        if (content == null) {
            result = null;
        } else {
            result = content.toString();
        }
        return result;
    }
}
