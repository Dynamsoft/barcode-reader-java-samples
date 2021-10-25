import com.dynamsoft.dbr.*;

public class GeneralSettings {
	public static void main(String[] args) {
		try {
			
			// 1.Initialize license.
            // The organization id 200001 here will grant you a public trial license good for 7 days. Note that network connection is required for this license to work.
            // If you want to use an offline license, please contact Dynamsoft Support: https://www.dynamsoft.com/company/contact/
            // You can also request a 30-day trial license in the customer portal: https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java
			DMDLSConnectionParameters para = BarcodeReader.initDLSConnectionParameters();
			para.organizationID = "200001";
			
			BarcodeReader.initLicenseFromDLS(para);
			 
		    // 2.Create an instance of Dynamsoft Barcode Reader.
		    BarcodeReader dbr = new BarcodeReader();
		    
		    // There are two ways to configure runtime parameters. One is through PublicRuntimeSettings, the other is through parameters template.
		    // 3. General settings (including barcode format, barcode count and scan region) through PublicRuntimeSettings
		    // 3.1 Obtain current runtime settings of instance.
		    PublicRuntimeSettings sts = dbr.getRuntimeSettings();
		    
		    // 3.2 Set the expected barcode format you want to read.
		    // The barcode format our library will search for is composed of BarcodeFormat group 1 and BarcodeFormat group 2.
		    // So you need to specify the barcode format in group 1 and group 2 individually.
		    sts.barcodeFormatIds = EnumBarcodeFormat.BF_ONED | EnumBarcodeFormat.BF_QR_CODE | EnumBarcodeFormat.BF_PDF417 | EnumBarcodeFormat.BF_DATAMATRIX;
		    sts.barcodeFormatIds_2 = EnumBarcodeFormat.BF_NULL;
		    
		    // 3.3 Set the expected barcode count you want to read. 
		    sts.expectedBarcodesCount = 10;
		    
		    // 3.4 Set the ROI(region of insterest) to speed up the barcode reading process. 
		    // Note: DBR supports setting coordinates by pixels or percentages. The origin of the coordinate system is the upper left corner point.
		    RegionDefinition region =  new RegionDefinition();
		    region.regionMeasuredByPercentage = 1;
		    region.regionLeft = 0;
		    region.regionRight = 100;
		    region.regionTop = 0;
		    region.regionBottom = 100;
		    sts.region = region;
		    
		    // 3.5 Apply the new settings to the instance
		    dbr.updateRuntimeSettings(sts);
		    
		    // 3. General settings (including barcode format, barcode count and scan region) through parameters template.
		    // dbr.initRuntimeSettingsWithString("{\"ImageParameter\":{\"BarcodeFormatIds\":[\"BF_ONED\",\"BF_PDF417\",\"BF_QR_CODE\",\"BF_DATAMATRIX\"],\"BarcodeFormatIds_2\":null,\"ExpectedBarcodesCount\":10,\"Name\":\"sts\",\"RegionDefinitionNameArray\":[\"region0\"]},\"RegionDefinition\":{\"Bottom\":100,\"Left\":0,\"MeasuredByPercentage\":1,\"Name\":\"region0\",\"Right\":100,\"Top\":0}}", EnumConflictMode.CM_OVERWRITE);
		    
	        // 4.Decode barcodes from an image file.
			TextResult[] results = dbr.decodeFile("../../images/AllSupportedBarcodeTypes.png", "");
			
			// 5.Output the barcode format and barcode text.
			if (results != null && results.length > 0) {
				for (int i = 0; i < results.length; i++) {
					TextResult result = results[i];
					
					String barcodeFormat = result.barcodeFormat==0?result.barcodeFormatString_2:result.barcodeFormatString;
					System.out.println("Barcode " + i + ":" + barcodeFormat + ","+ result.barcodeText);
				}
			} else {
				System.out.println("No data detected.");
			}
		} catch (BarcodeReaderException ex) {
			ex.printStackTrace();
		}
	}
}
