package com.dynamsoft.concurrentInstanceTest.zz3rdPart;

import com.dynamsoft.dbr.BarcodeReader;
import com.dynamsoft.dbr.TextResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class DynamsoftBarcodeWrapper {

    public static Exception initException = null;

    // For the sake of generality, spring's dependency injection mechanism is not used.
    static {
        // Contact us at https://www.dynamsoft.com/company/contact/ to get a concurrent instance license
		String license = "YOUR-LICENSE-KEY";
        int countForThisDevice = 1;
        int countForThisProcess = 1;
        try {
            BarcodeReader.setMaxConcurrentInstanceCount(countForThisDevice, countForThisProcess);
            BarcodeReader.initLicense(license);
        } catch (Exception ex) {
            ex.printStackTrace();
            initException = ex;
        }
    }
    public static TextResult[] decode(byte[] bytes) throws Exception {
        if(null != initException){ throw  initException; }
        var reader = BarcodeReader.getInstance();
        if(reader == null)
        {
            try {
                BarcodeReader.initLicense("YOUR-LICENSE-KEY");
            } catch (Exception ex) {
                ex.printStackTrace();
                throw  ex;
            }
            reader = BarcodeReader.getInstance();
            if(reader == null)
            {
                throw new Exception("Get Instance Failed.");
            }
        }
        var results = reader.decodeFileInMemory(bytes, "");
        reader.recycle();
        return results;
    }
}
