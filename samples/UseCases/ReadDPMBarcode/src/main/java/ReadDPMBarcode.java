import com.dynamsoft.dbr.*;

public class ReadDPMBarcode {

    private static void configReadDPMBarcode(BarcodeReader dbr) throws BarcodeReaderException {

        // Obtain current runtime settings of instance.
        PublicRuntimeSettings sts = dbr.getRuntimeSettings();

        // Set expected barcode formats.
        // Generally, the most common dpm barcode is datamatrix or qrcode
        sts.barcode_format_ids = EnumBarcodeFormat.BF_DATAMATRIX | EnumBarcodeFormat.BF_QR_CODE;

        // Set grayscale transformation modes.
        // DPM barcodes may be dark and printed on the light background, or it may be light and printed on the dark background.
        // By default, the library can only locate the dark barcodes that stand on a light background. "GTM_INVERTED":The image will be transformed into inverted grayscale.
        sts.furtherModes.grayscaleTransformationModes = new int[]{EnumGrayscaleTransformationMode.GTM_ORIGINAL,EnumGrayscaleTransformationMode.GTM_INVERTED,0,0,0,0,0,0};

        // Enable dpm modes.
        // It is a parameter to control how to read direct part mark (DPM) barcodes.
        sts.furtherModes.dpmCodeReadingModes = new int[]{EnumDPMCodeReadingMode.DPMCRM_GENERAL,0,0,0,0,0,0,0};

        // Apply the new settings to the instance
        dbr.updateRuntimeSettings(sts);
    }

    private static void outputResults(TextResult[] results) {
        if (results != null && results.length > 0) {
            for (int i = 0; i < results.length; i++) {
                TextResult result = results[i];

                String barcodeFormat = result.barcodeFormat==0?result.barcodeFormatString_2:result.barcodeFormatString;
                System.out.println("Barcode " + i + ":" + barcodeFormat + ","+ result.barcodeText);
            }
        } else {
            System.out.println("No data detected.");
        }
    }
    public static void main(String[] args) {
        try {

            // Initialize license.
            // The organization id 200001 here will grant you a public trial license good for 7 days. Note that network connection is required for this license to work.
            // If you want to use an offline license, please contact Dynamsoft Support: https://www.dynamsoft.com/company/contact/
            // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java
            DMDLSConnectionParameters para = BarcodeReader.initDLSConnectionParameters();
            para.organizationID = "200001";

            BarcodeReader.initLicenseFromDLS(para);

            // Create an instance of Dynamsoft Barcode Reader.
            BarcodeReader dbr = new BarcodeReader();

            TextResult[] results = null;
            String fileName = "../../../images/dpm.jpg";

            configReadDPMBarcode(dbr);

            // Decode barcodes from an image file by current runtime settings. The second parameter value "" means to decode through the current PublicRuntimeSettings.
            results = dbr.decodeFile(fileName, "");

            // Output the barcode format and barcode text.
            outputResults(results);

        } catch (BarcodeReaderException ex) {
            ex.printStackTrace();
        }
    }
}
