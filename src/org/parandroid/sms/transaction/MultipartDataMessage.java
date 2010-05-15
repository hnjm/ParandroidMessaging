package org.parandroid.sms.transaction;

import java.util.ArrayList;
import java.util.HashMap;

import org.parandroid.encoding.Base64Coder;

import android.app.PendingIntent;
import android.telephony.SmsManager;

/**
 * Send Parandroid messages using the Parandroid Messaging protocol.
 *
 * The message is base64-encoded and then split in parts with the maximum length of MAX_BYTES.
 * Each part is then sent appended to a 'header', containing:
 * 8 bits: Parandroid Messaging protocol version (for backward compatibility)
 * 4 bits: Current message count
 * 4 bits: Total message count
 * 8 bits: Message id
 */
public class MultipartDataMessage {
	
	private static final String TAG = "Parandroid MultipartDataMessageSender";
    private static final int ID_LENGTH = 255;
    private static final int PROTOCOL_VERSION = 0;

    private static HashMap<String, Integer> messageIds = new HashMap<String, Integer>();

	public static final short HEADER_LENGTH 	= 3;
	public static final short MESSAGE_LENGTH 	= 130;
	
	private ArrayList<byte[]> parts;
	private SmsManager smsManager;
	private String destination;
	private short port;
	private PendingIntent sentIntent;
	private PendingIntent deliveryIntent;
    
    public MultipartDataMessage(String destination, short port, byte[] message, PendingIntent sentIntent, PendingIntent deliveryIntent){
    	smsManager = SmsManager.getDefault();
    	parts = new ArrayList<byte[]>();
    	
    	this.destination = destination;
    	this.port = port;
    	this.sentIntent = sentIntent;
    	this.deliveryIntent = deliveryIntent;
    	
    	split(message);
    }
    
    private void split(byte[] message){
    	message = new String(Base64Coder.encode(message)).getBytes();
		int rest = message.length % MESSAGE_LENGTH;
		int numMessages = message.length / MESSAGE_LENGTH + (rest == 0 ? 0 : 1);
        
        int messageId = 0;
		if(messageIds.containsKey(destination)) messageId = (messageIds.get(destination) + 1) % ID_LENGTH;
        messageIds.put(destination, messageId);
		
		for(int i = 0; i < numMessages; i++){
			boolean last = i == numMessages - 1;
			byte[] part = new byte[HEADER_LENGTH + ((!last || rest == 0) ? MESSAGE_LENGTH : rest)];
			
			// Construct header: see class comments
			part[0] = new Integer(PROTOCOL_VERSION).byteValue();
			part[1] = new Integer((numMessages - i - 1) << 4 | numMessages).byteValue();
			part[2] = new Integer(messageId).byteValue();
			
			// Concat header and message part
			System.arraycopy(message, i * MESSAGE_LENGTH, part, HEADER_LENGTH, part.length - HEADER_LENGTH);
			
			parts.add(part);
		}
    }
    
    public int getPartCount(){
    	return parts.size();
    }
    
	public boolean send(){
		for(byte[] part : parts){
			try{
				smsManager.sendDataMessage(destination, null, port, part, sentIntent, deliveryIntent);
			}catch(Exception e){
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
}