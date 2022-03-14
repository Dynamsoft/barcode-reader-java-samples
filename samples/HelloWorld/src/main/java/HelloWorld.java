import com.dynamsoft.dbr.*;

public class HelloWorld {
	public static void main(String[] args) {
		try {
            
		    // 1.Initialize license.
            	    // The organization id 200001 here will grant you a free public trial license. Note that network connection is required for this license to work.
            	    // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java
		    BarcodeReader.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
			
		    // 2.Create an instance of Barcode Reader.
		    BarcodeReader dbr = new BarcodeReader();
		    
	        // 3.Decode barcodes from an image file.
			TextResult[] results = dbr.decodeFile("../../images/AllSupportedBarcodeTypes.png", "");
			
			// 4.Output the barcode text.
			if (results != null && results.length > 0) {
				for (int i = 0; i < results.length; i++) {
					TextResult result = results[i];
					System.out.println("Barcode " + i + ":" + result.barcodeText);
				}
			} else {
				System.out.println("No data detected.");
			}
		} catch (BarcodeReaderException ex) {
			ex.printStackTrace();
		}
	}
}
