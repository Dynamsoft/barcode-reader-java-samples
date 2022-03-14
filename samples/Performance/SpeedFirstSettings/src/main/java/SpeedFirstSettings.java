import java.util.Date;

import com.dynamsoft.dbr.*;

public class SpeedFirstSettings {
	
	private static void configSpeedFirst(BarcodeReader dbr) throws BarcodeReaderException {
	    
		// Obtain current runtime settings of instance.
		PublicRuntimeSettings sts = dbr.getRuntimeSettings();
	    
		// Parameter 1. Set expected barcode formats
        // The simpler barcode format, the faster decoding speed.
        // Here we use OneD barcode format to demonstrate.
        sts.barcodeFormatIds = EnumBarcodeFormat.BF_EAN_13;
	    
        // Parameter 2. Set expected barcode count
        // The less barcode count, the faster decoding speed.
        sts.expectedBarcodesCount = 1;

        // Parameter 3. Set the threshold for the image shrinking for localization.
        // The smaller the threshold, the smaller the image shrinks.  The default value is 2300.
        sts.scaleDownThreshold = 1200;

        // Parameter 4. Set the binarization mode for convert grayscale image to binary image.
        // Mostly, the less binarization modes set, the faster decoding speed.
        sts.binarizationModes = new int[]{EnumBinarizationMode.BM_LOCAL_BLOCK,0,0,0,0,0,0,0};

        // Parameter 5. Set localization mode.
        // LM_SCAN_DIRECTLY: Localizes barcodes quickly. It is both for OneD and TweD barcodes. This mode is recommended in interactive scenario.
        sts.localizationModes = new int[]{EnumLocalizationMode.LM_SCAN_DIRECTLY,0,0,0,0,0,0,0};

        // LM_ONED_FAST_SCAN: Localizing barcodes quickly. However, it is only for OneD barcodes. It is also recommended in interactive scenario.
        // sts.localizationModes = new int[]{EnumLocalizationMode.LM_ONED_FAST_SCAN, 0,0,0,0,0,0,0};
        
        // Parameter 6. Reduce deblurModes setting
        // DeblurModes will improve the readability and accuracy but decrease the reading speed.
        // Please update your settings here if you want to enable different Deblurmodes.
        sts.deblurModes = new int[]{EnumDeblurMode.DM_BASED_ON_LOC_BIN,EnumDeblurMode.DM_THRESHOLD_BINARIZATION,0,0,0,0,0,0,0,0};

        // Parameter 7. Set timeout(ms) if the application is very time sensitive.
        // If timeout, the decoding thread will exit at the next check point.
        sts.timeout = 100;

        dbr.updateRuntimeSettings(sts);

        // Other potentially accelerated arguments of various modes.

        // Argument 4.a Disable the EnableFillBinaryVacancy argument.
        // Local block binarization process might cause vacant area in barcode. The barcode reader will fill the vacant black by default (default value 1). Set the value 0 to disable this process.
        dbr.setModeArgument("BinarizationModes", 0, "EnableFillBinaryVacancy", "0");

        // Argument 5.a Sets the scan direction when searching barcode.
        // It is valid only when the type of LocalizationMode is LM_ONED_FAST_SCAN or LM_SCAN_DIRECTLY.
        // 0: Both vertical and horizontal direction.
        // 1: Vertical direction.
        // 2: Horizontal direction.
        // Read more about localization mode members: https://www.dynamsoft.com/barcode-reader/parameters/enum/parameter-mode-enums.html?ver=latest#localizationmode
        dbr.setModeArgument("LocalizationModes",0,"ScanDirection","0");
        
	}
	
	private static void configSpeedFirstByTemplate(BarcodeReader dbr) throws BarcodeReaderException {
		// Compared with PublicRuntimeSettings, parameter templates have a richer ability to control parameter details.
		// Please refer to the parameter explanation in "SpeedFirstTemplate.json" to understand how to control speed first.
		dbr.initRuntimeSettingsWithFile("SpeedFirstTemplate.json", EnumConflictMode.CM_OVERWRITE);
	}
	
	private static void outputResults(TextResult[] results, long costTime) {
		System.out.println("Cost time:" + costTime + "ms");
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
			
		    // 1.Initialize license.
            	    // The organization id 200001 here will grant you a free public trial license. Note that network connection is required for this license to work.
            	    // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java
		    BarcodeReader.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
			
		    // 2.Create an instance of Dynamsoft Barcode Reader.
		    BarcodeReader dbr = new BarcodeReader();
		    		    
		    TextResult[] results = null;
		    String fileName = "../../../images/EAN-13.jpg";

		    // There are two ways to configure runtime parameters. One is through PublicRuntimeSettings, the other is through parameters template.
		    System.out.println("Decode through PublicRuntimeSettings:");
		    {
		    	// 3.a config through PublicRuntimeSettings
		    	configSpeedFirst(dbr);
		    	
                long ullTimeBeg = new Date().getTime();
                
		    	// 4.a Decode barcodes from an image file by current runtime settings. The second parameter value "" means to decode through the current PublicRuntimeSettings.
		    	results = dbr.decodeFile(fileName, "");
               
		    	long ullTimeEnd = new Date().getTime();
                
		    	// 5.a Output the barcode format and barcode text.
		    	outputResults(results, ullTimeEnd-ullTimeBeg);
		    }
		    
		    
		    System.out.println("\nDecode through parameters template:");
		    {
			    // 3.b config through parameters template
		    	configSpeedFirstByTemplate(dbr);
		    	
                long ullTimeBeg = new Date().getTime();
                
		    	// 4.b Decode barcodes from an image file by template.
		    	results = dbr.decodeFile(fileName, "");
		    	
		    	long ullTimeEnd = new Date().getTime();
		    	
		    	// 5.b Output the barcode format and barcode text.
		    	outputResults(results, ullTimeEnd-ullTimeBeg);
		    }
		    
		} catch (BarcodeReaderException ex) {
			ex.printStackTrace();
		}
	}
}
