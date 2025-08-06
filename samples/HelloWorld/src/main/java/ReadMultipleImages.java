import com.dynamsoft.core.EnumErrorCode;
import com.dynamsoft.core.basic_structures.FileImageTag;
import com.dynamsoft.core.basic_structures.ImageTag;
import com.dynamsoft.cvr.*;
import com.dynamsoft.dbr.BarcodeResultItem;
import com.dynamsoft.dbr.DecodedBarcodesResult;
import com.dynamsoft.license.LicenseError;
import com.dynamsoft.license.LicenseException;
import com.dynamsoft.license.LicenseManager;
import com.dynamsoft.utility.DirectoryFetcher;
import com.dynamsoft.utility.UtilityException;

import java.util.Scanner;

class MyCapturedResultReceiver extends CapturedResultReceiver {

    @Override
    public void onDecodedBarcodesReceived(DecodedBarcodesResult result) {
        ImageTag tag = result.getOriginalImageTag();
        if (tag instanceof FileImageTag) {
            System.out.println("File: " + ((FileImageTag)tag).getFilePath());
        }

        if (result.getErrorCode() == EnumErrorCode.EC_UNSUPPORTED_JSON_KEY_WARNING) {
            System.out.println("Warning: " + result.getErrorCode() + ", " + result.getErrorString());
        } else if (result.getErrorCode() != EnumErrorCode.EC_OK) {
            System.out.println("Error: " + result.getErrorString());
        }

        BarcodeResultItem[] items = result.getItems();
        System.out.println("Decoded " + items.length + " barcodes.");
        for (int index = 0; index < items.length; index++) {
            BarcodeResultItem item = items[index];
            System.out.println();
            System.out.println("Result " + (index + 1));
            System.out.println("Barcode Format: " + item.getFormatString());
            System.out.println("Barcode Text: " + item.getText());
        }
    }
}

class MyImageSourceStateListener implements ImageSourceStateListener {
    CaptureVisionRouter cvRouter;

    public MyImageSourceStateListener(CaptureVisionRouter cvRouter) {
        this.cvRouter = cvRouter;
    }

    @Override
    public void onImageSourceStateReceived(int state) {
        if (state == EnumImageSourceState.ISS_EXHAUSTED) {
            if (cvRouter != null) {
                cvRouter.stopCapturing();
            }
        }
    }
}

public class ReadMultipleImages {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            int errorCode = 0;
            String errorMsg = "";

            // Initialize license.
            // You can request and extend a trial license from https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java
            // The string 'DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9' here is a free public trial license. Note that network connection is required for this license to work.
            try {
                LicenseError licenseError = LicenseManager.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
                if (licenseError.getErrorCode() != EnumErrorCode.EC_OK) {
                    errorCode = licenseError.getErrorCode();
                    errorMsg = licenseError.getErrorString();
                }
            } catch (LicenseException e) {
                errorCode = e.getErrorCode();
                errorMsg = e.getErrorString();
            }

            if (errorCode != EnumErrorCode.EC_OK) {
                System.out.println("License initialization failed: ErrorCode: " + errorCode + ", ErrorString: " + errorMsg);
                return;
            }

            CaptureVisionRouter cvRouter = new CaptureVisionRouter();
            try {
                DirectoryFetcher fetcher = new DirectoryFetcher();
                fetcher.setDirectory("../../images");
                cvRouter.setInput(fetcher);

                MyCapturedResultReceiver receiver = new MyCapturedResultReceiver();
                cvRouter.addResultReceiver(receiver);

                MyImageSourceStateListener listener = new MyImageSourceStateListener(cvRouter);
                cvRouter.addImageSourceStateListener(listener);
            } catch (UtilityException | CaptureVisionException ignored) {
            }

            try {
                cvRouter.startCapturing("", true);
            } catch (CaptureVisionException e) {
                if (e.getErrorCode() != EnumErrorCode.EC_OK) {
                    System.out.println("Error: " + e.getErrorCode());
                }
            }
        } finally {
            System.out.print("Press Enter to quit...");
            scanner.nextLine();
            scanner.close();
        }
    }
}