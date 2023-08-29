import com.dynamsoft.dbr.*;

public class ReadRateFirstSettings {
	
	private static void configReadRateFirst(BarcodeReader dbr) throws BarcodeReaderException {
	    
		// Obtain current runtime settings of instance.
		PublicRuntimeSettings sts = dbr.getRuntimeSettings();
		
	    // Parameter 1. Set expected barcode formats
        // Here the barcode scanner will try to find the maximal barcode formats.
		sts.barcodeFormatIds = EnumBarcodeFormat.BF_ALL;
		sts.barcodeFormatIds_2 = EnumBarcodeFormat_2.BF2_DOTCODE | EnumBarcodeFormat_2.BF2_POSTALCODE;

        // Parameter 2. Set expected barcode count.
        // Here the barcode scanner will try to find 64 barcodes.
        // If the result count does not reach the expected amount, the barcode scanner will try other algorithms in the setting list to find enough barcodes.
        sts.expectedBarcodesCount = 64;

        // Parameter 3. Set more binarization modes.
        sts.binarizationModes = new int[]{EnumBinarizationMode.BM_LOCAL_BLOCK,EnumBinarizationMode.BM_THRESHOLD,0,0,0,0,0,0};

        // Parameter 4. Set more localization modes.
        // LocalizationModes are all enabled as default. Barcode reader will automatically switch between the modes and try decoding continuously until timeout or the expected barcode count is reached.
        // Please manually update the enabled modes list or change the expected barcode count to promote the barcode scanning speed.
        // Read more about localization mode members: https://www.dynamsoft.com/barcode-reader/parameters/enum/parameter-mode-enums.html?ver=latest#localizationmode
        sts.localizationModes = new int[]{EnumLocalizationMode.LM_CONNECTED_BLOCKS,EnumLocalizationMode.LM_SCAN_DIRECTLY,EnumLocalizationMode.LM_STATISTICS,
                EnumLocalizationMode.LM_LINES,EnumLocalizationMode.LM_STATISTICS_MARKS,EnumLocalizationMode.LM_STATISTICS_POSTAL_CODE,0,0};

        // Parameter 5. Set more deblur modes.
        // DeblurModes are all enabled as default. Barcode reader will automatically switch between the modes and try decoding continuously until timeout or the expected barcode count is reached.
        // Please manually update the enabled modes list or change the expected barcode count to promote the barcode scanning speed.
        //Read more about deblur mode members: https://www.dynamsoft.com/barcode-reader/parameters/enum/parameter-mode-enums.html#deblurmode
        sts.deblurModes = new int[]{EnumDeblurMode.DM_DIRECT_BINARIZATION,EnumDeblurMode.DM_THRESHOLD_BINARIZATION,EnumDeblurMode.DM_GRAYE_EQULIZATION,
                EnumDeblurMode.DM_SMOOTHING,EnumDeblurMode.DM_MORPHING,EnumDeblurMode.DM_DEEP_ANALYSIS,EnumDeblurMode.DM_SHARPENING,0,0,0};

        // Parameter 6. Set scale up modes.
        // It is a parameter to control the process for scaling up an image used for detecting barcodes with small module size
        sts.scaleUpModes = new int[]{EnumScaleUpMode.SUM_AUTO,0,0,0,0,0,0,0};

        // Parameter 7. Set grayscale transformation modes.
        // By default, the library can only locate the dark barcodes that stand on a light background. "GTM_INVERTED":The image will be transformed into inverted grayscale.
        sts.furtherModes.grayscaleTransformationModes = new int[]{EnumGrayscaleTransformationMode.GTM_ORIGINAL,EnumGrayscaleTransformationMode.GTM_INVERTED,0,0,0,0,0,0};

        // Parameter 8. Enable dpm modes.
        // It is a parameter to control how to read direct part mark (DPM) barcodes.
        sts.furtherModes.dpmCodeReadingModes = new int[]{EnumDPMCodeReadingMode.DPMCRM_GENERAL,0,0,0,0,0,0,0};

        // Parameter 9. Increase timeout(ms). The barcode scanner will have more chances to find the expected barcode until it times out
        sts.timeout = 30000;
        
	    // Apply the new settings to the instance
	    dbr.updateRuntimeSettings(sts);
	}
	
	private static void configReadRateFirstByTemplate(BarcodeReader dbr) throws BarcodeReaderException {
		// Compared with PublicRuntimeSettings, parameter templates have a richer ability to control parameter details.
		// Please refer to the parameter explanation in "ReadRateFirstTemplate.json" to understand how to control read rate first.
		dbr.initRuntimeSettingsWithFile("ReadRateFirstTemplate.json", EnumConflictMode.CM_OVERWRITE);
	}
	
	private static void outputResults(TextResult[] results) {
		if (results != null && results.length > 0) {
			for (int i = 0; i < results.length; i++) {
				TextResult result = results[i];
				
				String barcodeFormat = result.barcodeFormatString;
				System.out.println("Barcode " + i + ":" + barcodeFormat + ","+ result.barcodeText);
			}
		} else {
			System.out.println("No data detected.");
		}		
	}
	
	public static void main(String[] args) {
		try {
			
		    // 1.Initialize license.
            // The string "DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9" here is a free public trial license. Note that network connection is required for this license to work.
            // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?architecture=dcv&product=dbr&utm_source=samples&package=java
		    BarcodeReader.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
			
		    // 2.Create an instance of Dynamsoft Barcode Reader.
		    BarcodeReader dbr = BarcodeReader.getInstance();
			if(dbr == null)
            {
                throw new Exception("Get Instance Failed.");
            }
		    TextResult[] results = null;
		    String fileName = "../../../images/AllSupportedBarcodeTypes.png";

		    // There are two ways to configure runtime parameters. One is through PublicRuntimeSettings, the other is through parameters template.
		    System.out.println("Decode through PublicRuntimeSettings:");
		    {
		    	// 3.a config through PublicRuntimeSettings
		    	configReadRateFirst(dbr);
		    	

		    	// 4.a Decode barcodes from an image file by current runtime settings. The second parameter value "" means to decode through the current PublicRuntimeSettings.
		    	results = dbr.decodeFile(fileName, "");

              
		    	// 5.a Output the barcode format and barcode text.
		    	outputResults(results);
		    }
		    
		    
		    System.out.println("\nDecode through parameters template:");
		    {
			    // 3.b config through parameters template
		    	configReadRateFirstByTemplate(dbr);
                
		    	// 4.b Decode barcodes from an image file by template.
		    	results = dbr.decodeFile(fileName, "");
		    	
		    	// 5.b Output the barcode format and barcode text.
		    	outputResults(results);
		    }
		    dbr.recycle();
		} catch (BarcodeReaderException ex) {
			ex.printStackTrace();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
