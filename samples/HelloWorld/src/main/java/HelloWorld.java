import com.dynamsoft.dbr.*;

public class HelloWorld {
	public static void main(String[] args) {
		try {
            
		    // 1.Initialize license.
            // The string "DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9" here is a free public trial license. Note that network connection is required for this license to work.
            // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?architecture=dcv&product=dbr&utm_source=samples&package=java
		    BarcodeReader.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
			
		    // 2.Create an instance of Barcode Reader.
		    BarcodeReader dbr = BarcodeReader.getInstance();
			if(dbr == null)
            {
                throw new Exception("Get Instance Failed.");
            }
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
			dbr.recycle();
		} catch (BarcodeReaderException ex) {
			ex.printStackTrace();
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}
}
