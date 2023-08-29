import com.dynamsoft.dbr.*;

public class GeneralSettings {
    public static void main(String[] args) {
        try {
            
            // 1.Initialize license.
            // The string "DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9" here is a free public trial license. Note that network connection is required for this license to work.
            // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?architecture=dcv&product=dbr&utm_source=samples&package=java
            BarcodeReader.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
            
            // 2.Create an instance of Dynamsoft Barcode Reader.
            BarcodeReader reader = BarcodeReader.getInstance();
            if(reader==null)
            {
                throw new Exception("Get instance failed.");
            }
            // There are two ways to configure runtime parameters. One is through PublicRuntimeSettings, the other is through parameters template.
            // 3. General settings (including barcode format, barcode count and scan region) through PublicRuntimeSettings
            // 3.1 Obtain current runtime settings of instance.
            PublicRuntimeSettings runtimeSettings = reader.getRuntimeSettings();
            
            // 3.2 Set the expected barcode format you want to read.
            // The barcode format our library will search for is composed of BarcodeFormat group 1 and BarcodeFormat group 2.
            // So you need to specify the barcode format in group 1 and group 2 individually.
            runtimeSettings.barcodeFormatIds = EnumBarcodeFormat.BF_ONED | EnumBarcodeFormat.BF_QR_CODE;
            runtimeSettings.barcodeFormatIds_2 = EnumBarcodeFormat_2.BF2_POSTALCODE | EnumBarcodeFormat_2.BF2_DOTCODE;
            
            // 3.3 Set the expected barcode count you want to read. 
            runtimeSettings.expectedBarcodesCount = 10;
            
            // 3.4 Set the ROI(region of insterest) to speed up the barcode reading process. 
            // Note: DBR supports setting coordinates by pixels or percentages. The origin of the coordinate system is the upper left corner point.
            RegionDefinition region =  new RegionDefinition();
            region.regionMeasuredByPercentage = 1;
            region.regionLeft = 0;
            region.regionRight = 100;
            region.regionTop = 0;
            region.regionBottom = 100;
            runtimeSettings.region = region;
            
            // 3.5 Apply the new settings to the instance
            reader.updateRuntimeSettings(runtimeSettings);
            
            // 3. General settings (including barcode format, barcode count and scan region) through parameters template.
            // reader.initRuntimeSettingsWithString("{\"ImageParameter\":{\"BarcodeFormatIds\":[\"BF_ONED\",\"BF_PDF417\",\"BF_QR_CODE\",\"BF_DATAMATRIX\"],\"BarcodeFormatIds_2\":null,\"ExpectedBarcodesCount\":10,\"Name\":\"IP1\",\"RegionDefinitionNameArray\":[\"region0\"]},\"RegionDefinition\":{\"Bottom\":100,\"Left\":0,\"MeasuredByPercentage\":1,\"Name\":\"region0\",\"Right\":100,\"Top\":0}}", EnumConflictMode.CM_OVERWRITE);
            
            // 4.Decode barcodes from an image file.
            TextResult[] results = reader.decodeFile("../../images/AllSupportedBarcodeTypes.png", "");
            
            // 5.Output the barcode format and barcode text.
            if (results != null && results.length > 0) {
                System.out.println("Total barcodes found: " + results.length);
                for (int iIndex = 0; iIndex < results.length; iIndex++) {
                    System.out.println("Barcode " + (iIndex + 1));
                    System.out.println("    Barcode Format: " + results[iIndex].barcodeFormatString);
                    System.out.println("    Barcode Text: " + results[iIndex].barcodeText);
                }
            } else {
                System.out.println("No barcode detected.");
            }
            reader.recycle();
        } 
        catch (BarcodeReaderException exp) {
            System.out.println(exp.getMessage());
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }
}
