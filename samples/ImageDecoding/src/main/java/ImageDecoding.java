import java.io.*;
import java.util.Base64;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import javax.imageio.ImageIO;

import com.dynamsoft.dbr.*;

class ImageData {
	public byte[] bytes;
	public int width;
	public int height;
	public int stride;
	public int format;
}

public class ImageDecoding {
	
	private static ImageData cvtToImageData(String filePath) throws FileNotFoundException, IOException {
        BufferedImage in = ImageIO.read(new FileInputStream(filePath));
        
        BufferedImage image = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(in, null, 0, 0);
        graphics.dispose();

        // calculate stride (c)
        int stride = ((24 * image.getWidth() + 31) / 32) * 4;

        ByteArrayOutputStream output = new ByteArrayOutputStream(image.getHeight() * stride);

        // Output rows bottom-up (b)
        Raster raster = image.getRaster()
                             .createChild(0, 0, image.getWidth(), image.getHeight(), 0, 0, new int[]{2, 1, 0});
        byte[] row = new byte[stride];

        for (int i = 0; i < image.getHeight(); i++) {
            row = (byte[]) raster.getDataElements(0, i, image.getWidth(), 1, row);
            output.write(row);
        }

		ImageData imgData = new ImageData();
		imgData.width = image.getWidth();
		imgData.height = image.getHeight();
		imgData.bytes = output.toByteArray();
		imgData.stride = stride;
		imgData.format = EnumImagePixelFormat.IPF_BGR_888;
        
        return imgData;
	}
	
	private static BufferedImage getBufferedImage(String filePath) {
		BufferedImage bufferedImage = null;
		
		try {
			bufferedImage = ImageIO.read(new File(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return bufferedImage;
	}
	
	private static String getFileBase64(String filePath) {
		byte[] fileBytes = getFileBytes(filePath);
		String encodedText = null;
		
		encodedText = Base64.getEncoder().encodeToString(fileBytes);
		
		return encodedText;
	}
	
	private static InputStream getFileStream(String filePath) {
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(new File(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return fis;
	}
	
	private static byte[] getFileBytes(String filePath) {
        byte[] buffer = null;
        
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;

        try {
            fis = new FileInputStream(new File(filePath));
            bos = new ByteArrayOutputStream();

            byte[] tempBuffer = new byte[1024];

            int iReadSize;

            while ((iReadSize = fis.read(tempBuffer)) != -1) {
                bos.write(tempBuffer, 0, iReadSize);
            }
            
            buffer = bos.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (null != bos) {
                try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
            
            if (null != fis) {
            	try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
        
        return buffer;
    }
	
	private static int chooseNumber() {
        System.out.println();
        System.out.println(">> Choose a number for diffent decoding interfaces:");
        System.out.println("   0: exit program");
        System.out.println("   1: decodeFile");
        System.out.println("   2: decodeBase64String");
        System.out.println("   3: decodeBufferedImage");
        System.out.println("   4: decodeFileInMemory-file bytes");
        System.out.println("   5: decodeFileInMemory-input stream");
        System.out.println("   6: decodeBuffer");
        
		BufferedReader cin = new BufferedReader(new java.io.InputStreamReader(System.in));
        
		int iNum = -1;
		
		String strLine = null;
		try {
			strLine = cin.readLine().trim();
			iNum = Integer.parseInt(strLine);
		} catch (IOException e) {
			e.printStackTrace();
		} catch(NumberFormatException exp) {
        	
        }
        
        return iNum;
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
		    String filePath = "../../images/AllSupportedBarcodeTypes.png";
		    TextResult[] results = null;
	        
	        while (true) {
	        	
	        	int iNum = -1;
	            while (true) {	                
	                iNum = chooseNumber();

	                if(iNum < 0 || iNum > 6)
	                	System.out.println("Please choose a valid number.");
	                else
	                	break;
	            }

	            if(iNum == 0)
	            	break;
	            
	            // 3. DBR supports decoding of various data types, including file paths, file input streams, base64 encoded files, file raw bytes, etc.
	            switch(iNum) {
	            	case 1: {
	            		//3.1 Decoding with file path
	            		results = dbr.decodeFile(filePath, "");
	            		break;
	            	}
	            	case 2: {
	            		String base64Str = getFileBase64(filePath);
	            		
	            		//3.2 Decoding with base64 encoded file
	            		results = dbr.decodeBase64String(base64Str, "");
	            		break;
	            	}         	
	            	case 3: {
	            		BufferedImage bufferedImage = getBufferedImage(filePath);
	            		
	            		//3.3 Decoding with buffered image
	            		results = dbr.decodeBufferedImage(bufferedImage, "");
	            		break;
	            	}
	            	case 4: {         		
	            		byte[] bytes = getFileBytes(filePath);
	            		
	            		//3.4 Decoding with file bytes
	            		results = dbr.decodeFileInMemory(bytes, "");
	            		break;
	            	}
	            	case 5: {
	            		InputStream fs = getFileStream(filePath);
	            		
	            		//3.5 Decoding with input stream
	            		results = dbr.decodeFileInMemory(fs, "");
	            		break;
	            	}
	            	case 6: {
	            		ImageData img = cvtToImageData(filePath);
	            		
	            		//3.6 Decoding with raw buffer
	            		results = dbr.decodeBuffer(img.bytes, img.width, img.height, img.stride, img.format, "");
	            		break;
	            	}
	            	default:
	            		break;
	            }
	                
				// 4.Output the barcode format and barcode text.
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
			dbr.recycle();
		} catch (BarcodeReaderException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
