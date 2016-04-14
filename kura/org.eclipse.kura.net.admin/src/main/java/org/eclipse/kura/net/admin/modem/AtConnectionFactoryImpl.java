package org.eclipse.kura.net.admin.modem;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.microedition.io.Connection;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//http://unix.stackexchange.com/questions/99345/sharing-serial-device
	
public class AtConnectionFactoryImpl implements AtConnectionFactory {

	private static final Logger s_logger = LoggerFactory.getLogger(AtConnectionFactoryImpl.class);
	
	private static final int HIP_PORT = 1;
	
	@SuppressWarnings("unused")
	private ComponentContext  m_ctx;
	private BundleContext     s_bundleContext;
	private ConnectionFactory m_connectionFactory;
	private ConnectionFactory m_commConnectionFactory;
	
	private static ScheduledFuture<?> 		m_tracker;
	private ScheduledThreadPoolExecutor		m_executor;
	
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		m_connectionFactory = connectionFactory;
	    m_commConnectionFactory = connectionFactory;
	}

	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		m_connectionFactory = null;
		m_commConnectionFactory = null;
	}
	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext)
	{			
		
		s_logger.debug("AtConnectionFactory activating...");
		//
		// save the bundle context
		m_ctx = componentContext;
		s_bundleContext = componentContext.getBundleContext();
		m_executor = new ScheduledThreadPoolExecutor(1);
		
		if (m_tracker == null) {
			m_tracker = m_executor.schedule(new Runnable() {
				public void run() {
					Thread.currentThread().setName("Tracker");

					// Search for ExtendedConnectionFactory if it is already started... 
					ServiceTracker<ExtendedConnectionFactory, ExtendedConnectionFactory> serviceTracker = new ServiceTracker<ExtendedConnectionFactory, ExtendedConnectionFactory>(s_bundleContext, ExtendedConnectionFactory.class.getName(), null);
					serviceTracker.open(true);
					if (serviceTracker.getService() != null) {
						s_logger.info("Use ExtendedConnectionFactory for at connection.");
						m_connectionFactory = serviceTracker.getService();
						serviceTracker.close();
					} 
					else {
					    // If the service is not started, add a listener for it
						String filter = "(" + Constants.OBJECTCLASS + "=" + ExtendedConnectionFactory.class.getName() + ")";
						try {
							s_bundleContext.addServiceListener(new ServiceListener() {

								@Override
								public void serviceChanged(ServiceEvent event) {
									if (event.getType() == ServiceEvent.REGISTERED) {
										ServiceTracker<ExtendedConnectionFactory, ExtendedConnectionFactory> serviceTracker = new ServiceTracker<ExtendedConnectionFactory, ExtendedConnectionFactory>(s_bundleContext, ExtendedConnectionFactory.class.getName(), null);
										serviceTracker.open(true);
										if (serviceTracker.getService() != null) {
											s_logger.info("Use ExtendedConnectionFactory for at connection.");
											m_connectionFactory = serviceTracker.getService();
											serviceTracker.close();
										}
									}
									else if (event.getType() == ServiceEvent.UNREGISTERING) {
										s_logger.info("Use CommConnection for at connection.");
										m_connectionFactory = m_commConnectionFactory;
									}
								}

							}, filter);
						} catch (InvalidSyntaxException e) {
							s_logger.error("Invalid Syntax", e);
						}
					}
					
				}
			}, 0, TimeUnit.MILLISECONDS);
		}
		
		s_logger.debug("AtConnectionFactory activating...Done.");
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		m_ctx = null;
		s_bundleContext = null;
	}
	
	@Override
	public Connection createConnection(ModemDevice device, int mode, boolean timeouts) throws IOException, KuraException {
		
		String uri = new CommURI.Builder(getAtPort(device))
		.withBaudRate(115200)
		.withDataBits(8)
		.withStopBits(1)
		.withParity(0)
		.withTimeout(2000)
		.build().toString();
		
		return m_connectionFactory.createConnection(uri, mode, timeouts);
	}

	@Override
	public Connection createHipConnection(ModemDevice device, int mode, boolean timeouts) throws IOException, KuraException {
		
		String uri = new CommURI.Builder(getHipPort(device))
		.withBaudRate(115200)
		.withDataBits(8)
		.withStopBits(1)
		.withParity(0)
		.withTimeout(2000)
		.build().toString();
		
		return m_connectionFactory.createConnection(uri, mode, timeouts);
	}
	
    public String getAtPort(ModemDevice device) throws KuraException {
    	String port = null;
    	List <String> ports = device.getSerialPorts();
    	if ((ports != null) && (ports.size() > 0)) {
	    	if (device instanceof UsbModemDevice) {
	    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)device);
	    		if (usbModemInfo != null) {
	    			port = ports.get(usbModemInfo.getAtPort());
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No AT serial port available");
	    		}
	    	} else if (device instanceof SerialModemDevice) {
	    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
	    		if (serialModemInfo != null) {
	    			port = serialModemInfo.getDriver().getComm().getAtPort();
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No AT serial port available");
	    		}
	    	} else {
	    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
	    	}
    	} else {
    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No serial ports available");
    	}
    	return port;
	}
    
	public String getHipPort(ModemDevice device) throws KuraException {
		String port = null;
		
		if (device instanceof UsbModemDevice) {
			UsbModemDevice usbModemDevice = (UsbModemDevice)device;
			List <String> ports = usbModemDevice.getTtyDevs();
			if ((ports != null) && (ports.size() > 0)) {
				port = ports.get(HIP_PORT);
    		} else {
    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No HIP serial port available");
    		}
		} else {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No HIP serial port available");
		}
		
    	return port;
	}

}
