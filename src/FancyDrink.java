import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;

import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public class FancyDrink implements UsbPipeListener {
	
	 public static int vid = 2338; // hex 0922
	    public static int pid = -32765; // hex 8003 => 32771
		private UsbDevice device;
		private UsbInterface iface;
		private UsbPipe pipe;
		private byte[] data = new byte[6];

	    public FancyDrink(UsbDevice device) {
			this.device = device; 
		}

		public static void main(String[] args) throws UsbException
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
	            
	            FancyDrink scale = FancyDrink.findScale();
	            scale.open();
	            try {
	              for (int i = 0; i < 60; i++) {
	                scale.syncSubmit();
	              }
	            } finally {
	              scale.close();
	            }


	    }
	    
	    public static FancyDrink findScale() throws UsbException {
	    	UsbServices services = UsbHostManager.getUsbServices();
	        UsbHub rootHub = services.getRootUsbHub();
	        // Dymo M10 Scale:
	        UsbDevice device = findDevice(rootHub, (short) 0x0922, (short) 0x8003);
	        // Dymo M25 Scale:
	        if (device == null) {
	          device = findDevice(rootHub, (short) 0x0922, (short) 0x8004);
	        }
	        if (device == null) {
	          return null;
	        }
	        return new FancyDrink(device);
	      }
	    
	    private static UsbDevice findDevice(UsbHub hub, short vendorId, short productId) {
	        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
	          UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
	          if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
	            return device;
	          }
	          if (device.isUsbHub()) {
	            device = findDevice((UsbHub) device, vendorId, productId);
	            if (device != null) {
	              return device;
	            }
	          }
	        }
	        return null;
	      }
	    
	    private void open() throws UsbException {
	        UsbConfiguration configuration = device.getActiveUsbConfiguration();
	        iface = configuration.getUsbInterface((byte) 0);
	        // this allows us to steal the lock from the kernel
	        iface.claim(usbInterface -> true);
	        final List<UsbEndpoint> endpoints = iface.getUsbEndpoints();
	        pipe = endpoints.get(0).getUsbPipe(); // there is only 1 endpoint
	        pipe.addUsbPipeListener(this);
	        pipe.open();
	      }

	      private void syncSubmit() throws UsbException {
	        pipe.syncSubmit(data);
	      }

	      public void close() throws UsbException {
	        pipe.close();
	        iface.release();
	      }

	      @Override
	      public void dataEventOccurred(UsbPipeDataEvent upde) {
	        boolean empty = data[1] == 2;
	        boolean overweight = data[1] == 6;
	        boolean negative = data[1] == 5;
	        boolean grams = data[2] == 2;
	        int scalingFactor = data[3];
	        int weight = (data[4] & 0xFF) + (data[5] << 8);
	        if (empty) {
	          System.out.println("EMPTY");
	        } else if (overweight) {
	          System.out.println("OVERWEIGHT");
	        } else if (negative) {
	          System.out.println("NEGATIVE");
	        } else { // Use String.format since printf causes problems on remote exec
	          System.out.println(String.format("Weight = %,.1f%s",
	              scaleWeight(weight, scalingFactor),
	              grams ? "g" : "oz"));
	        }
	      }

	      private double scaleWeight(int weight, int scalingFactor) {
	        return weight * Math.pow(10, scalingFactor);
	      }

	      @Override
	      public void errorEventOccurred(UsbPipeErrorEvent upee) {
	        Logger.getLogger(FancyDrink.class.getName()).log(Level.SEVERE, "Scale Error", upee);
	      }

	}