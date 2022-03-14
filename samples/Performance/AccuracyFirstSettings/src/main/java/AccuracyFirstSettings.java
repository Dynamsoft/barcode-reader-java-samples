import com.dynamsoft.dbr.*;

public class AccuracyFirstSettings {
	
	private static void configAccuracyFirst(BarcodeReader dbr) throws BarcodeReaderException {
	       
		// Obtain current runtime settings of instance.
		PublicRuntimeSettings sts = dbr.getRuntimeSettings();
	    
        // 1. Set expected barcode format
        // The more precise the barcode format is set, the higher the accuracy.
        // Mostly, misreading only occurs on reading oneD barcode. So here we use OneD barcode format to demonstrate.
	    sts.barcodeFormatIds = EnumBarcodeFormat.BF_ONED;
	    sts.barcodeFormatIds_2 = EnumBarcodeFormat.BF_NULL;
	    
	    // 2. Set the minimal result confidence.
        // The greater the confidence, the higher the accuracy.
        // Filter by minimal confidence of the decoded barcode. We recommend using 30 as the default minimal confidence value
	    sts.minResultConfidence = 30;
	    
	    // 3. Sets the minimum length of barcode text for filtering.
        // The more precise the barcode text length is set, the higher the accuracy.
	    sts.minBarcodeTextLength = 6;
	    
	    // Apply the new settings to the instance
	    dbr.updateRuntimeSettings(sts);
	}
	
	private static void configAccuracyFirstByTemplate(BarcodeReader dbr) throws BarcodeReaderException {
		// Compared with PublicRuntimeSettings, parameter templates have a richer ability to control parameter details.
		// Please refer to the parameter explanation in "AccuracyFirstTemplate.json" to understand how to control accuracy first.
		dbr.initRuntimeSettingsWithFile("AccuracyFirstTemplate.json", EnumConflictMode.CM_OVERWRITE);
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
			
		    // 1.Initialize license.
            	    // The organization id 200001 here will grant you a free public trial license. Note that network connection is required for this license to work.
            	    // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java
		    BarcodeReader.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
			
		    // 2.Create an instance of Dynamsoft Barcode Reader.
		    BarcodeReader dbr = new BarcodeReader();
		    
		    TextResult[] results = null;
		    String fileName = "../../../images/AllSupportedBarcodeTypes.png";

		    // Accuracy = The number of correctly decoded barcodes/the number of all decoded barcodes
		    // There are two ways to configure runtime parameters. One is through PublicRuntimeSettings, the other is through parameters template.
		    System.out.println("Decode through PublicRuntimeSettings:");
		    {
		    	// 3.a config through PublicRuntimeSettings
		    	configAccuracyFirst(dbr);
		    	
		    	// 4.a Decode barcodes from an image file by current runtime settings. The second parameter value "" means to decode through the current PublicRuntimeSettings.
		    	results = dbr.decodeFile(fileName, "");
		    	
		    	// 5.a Output the barcode format and barcode text.
		    	outputResults(results);
		    }
		    
		    
		    System.out.println("\nDecode through parameters template:");
		    {
			    // 3.b config through parameters template
		    	configAccuracyFirstByTemplate(dbr);
		    	
		    	// 4.b Decode barcodes from an image file by template. 
		    	results = dbr.decodeFile(fileName, "");
		    	
		    	// 5.b Output the barcode format and barcode text.
		    	outputResults(results);
		    }
		    
		} catch (BarcodeReaderException ex) {
			ex.printStackTrace();
		}
	}
}
