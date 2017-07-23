import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public class FancyDrink {
	
	 public static int vid = 2338; // hex 0922
	    public static int pid = -32765; // hex 8003 => 32771

	    public static void main(String[] args) 
	    {
	        Context context = new Context();
	        int result = LibUsb.init(context);
	        if(result != LibUsb.SUCCESS)
	        {
	            throw new LibUsbException("Unable to initialize the usb device",result);
	        }
	        DeviceList list = new DeviceList();
	        result = LibUsb.getDeviceList(context, list);
	        if(result < 0 )throw new LibUsbException("Unable to get device list",result);
	            try
	            {
	                for(Device device : list)
	                {
	                    DeviceDescriptor device_descriptor = new DeviceDescriptor();
	                    result = LibUsb.getDeviceDescriptor(device, device_descriptor);
	                    if(result != LibUsb.SUCCESS)throw new LibUsbException("Unable to get device descriptor : ",result);
	                    System.out.println("Product id is : "+device_descriptor.idProduct()+" "+"Vendor id is : "+device_descriptor.idVendor());
	                    if(device_descriptor.idProduct()==pid && device_descriptor.idVendor()==vid)
	                    {
	                    	
	                        System.out.println("Product id and vendor id was matched");
	                    }
	                    else
	                    {

	                        System.out.println("Product id and vendor id was not matched");
	                    }

	                }

	            }
	            finally
	            {
	                LibUsb.freeDeviceList(list, true);
	            }


	    }

	}