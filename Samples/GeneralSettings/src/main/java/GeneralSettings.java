import com.dynamsoft.core.EnumErrorCode;
import com.dynamsoft.core.EnumGrayscaleTransformationMode;
import com.dynamsoft.core.basic_structures.Point;
import com.dynamsoft.cvr.*;
import com.dynamsoft.dbr.BarcodeResultItem;
import com.dynamsoft.dbr.DecodedBarcodesResult;
import com.dynamsoft.dbr.EnumBarcodeFormat;
import com.dynamsoft.license.LicenseError;
import com.dynamsoft.license.LicenseException;
import com.dynamsoft.license.LicenseManager;

import java.nio.file.Paths;
import java.util.Scanner;

public class GeneralSettings {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try
        {
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
            SimplifiedCaptureVisionSettings settings = null;

            try {
                settings = cvRouter.getSimplifiedSettings(EnumPresetTemplate.PT_READ_BARCODES);
            } catch (CaptureVisionException e) {
                settings = new SimplifiedCaptureVisionSettings();
            }

            settings.barcodeSettings.barcodeFormatIds = EnumBarcodeFormat.BF_QR_CODE | EnumBarcodeFormat.BF_CODE_128;
            settings.barcodeSettings.expectedBarcodesCount = 10;
            settings.barcodeSettings.grayscaleTransformationModes[0] = EnumGrayscaleTransformationMode.GTM_AUTO;

            settings.roiMeasuredInPercentage = 1;
            Point[] points = settings.roi.points;
            points[0].set(0, 0);
            points[1].set(100, 0);
            points[2].set(100, 100);
            points[3].set(0, 100);

            try {
                cvRouter.updateSettings(EnumPresetTemplate.PT_READ_BARCODES, settings);
            } catch (CaptureVisionException e) {
                System.out.println("Update settings failed: ErrorCode: " + e.getErrorCode() + ", ErrorString: " + e.getErrorString());
                return;
            }

            String imagePath = Paths.get(System.getProperty("user.dir"), "../../Images/GeneralBarcodes.png").normalize().toString();

            CapturedResult[] results = cvRouter.captureMultiPages(imagePath, EnumPresetTemplate.PT_READ_BARCODES);
            if (results == null || results.length == 0) {
                System.out.println("No Captured result.");
            } else {
                for (int index = 0; index < results.length; index++) {
                    CapturedResult result = results[index];
                    if (result.getErrorCode() == EnumErrorCode.EC_UNSUPPORTED_JSON_KEY_WARNING) {
                        System.out.println("Warning: " + result.getErrorCode() + ", " + result.getErrorString());
                    } else if (result.getErrorCode() != EnumErrorCode.EC_OK) {
                        System.out.println("Error: " + result.getErrorCode() + ", " + result.getErrorString());
                    }

                    DecodedBarcodesResult barcodeResult = result != null ? result.getDecodedBarcodesResult() : null;
                    BarcodeResultItem[] items = barcodeResult != null ? barcodeResult.getItems() : null;
                    if (items == null || items.length == 0) {
                        System.out.println("Page-" + (index + 1) + " No barcode detected.");
                    } else {
                        System.out.println("Page-" + (index + 1) + " Decoded " + items.length + " barcodes.");
                        for (int i = 0; i < items.length; i++) {
                            BarcodeResultItem item = items[i];
                            System.out.println();
                            System.out.println("Barcode " + (i + 1));
                            System.out.println("Barcode Format: " + item.getFormatString());
                            System.out.println("Barcode Text: " + item.getText());
                        }
                    }
                }
            }
        }
        finally {
            System.out.print("Press Enter to quit...");
            scanner.nextLine();
            scanner.close();
        }
    }
}