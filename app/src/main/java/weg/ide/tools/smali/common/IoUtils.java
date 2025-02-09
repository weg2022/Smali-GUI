package weg.ide.tools.smali.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Checksum;


public class IoUtils {

    private static final int BUFFER_SIZE = 8192 * 2;

    @NonNull
    public static String readString(@NonNull Reader reader, boolean autoClose) throws IOException {
        try {
            char[] buffer = new char[BUFFER_SIZE];
            StringBuilder builder = new StringBuilder();
            while (true) {
                int read = reader.read(buffer);
                if (read == -1) {
                    break;
                }
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            if (autoClose)
                safeClose(reader);
        }
    }


    public static int copy(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        int byteCount = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            int read = in.read(buffer);
            if (read == -1) break;
            out.write(buffer, 0, read);
            byteCount += read;
        }
        return byteCount;
    }


    public static void safeClose(@Nullable Closeable... closeable) {
        if (closeable != null) {
            try {
                for (Closeable close : closeable) {
                    if (close != null)
                        close.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
