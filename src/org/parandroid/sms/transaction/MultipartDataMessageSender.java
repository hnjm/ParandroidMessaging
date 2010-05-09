package org.parandroid.sms.transaction;

import java.util.Random;

import org.parandroid.encoding.Base64Coder;

import android.app.PendingIntent;
import android.telephony.SmsManager;

public class MultipartDataMessageSender {
	
	private static final String TAG = "Parandroid MultipartDataMessageSender";
	public static short MAX_BYTES = 140;
	
	public static void sendMultipartDataMessage(SmsManager smsManager, String dest, short port, byte[] message, PendingIntent sentIntent, PendingIntent deliveryIntent){
		message = new String(Base64Coder.encode(message)).getBytes();
		int rest = message.length % MAX_BYTES;
		int numMessages = message.length / MAX_BYTES + (rest == 0 ? 0 : 1);

		int msgId = new Random().nextInt() >> 24; 
		
		for(int i = 0; i < numMessages; i++){
			boolean last = i == numMessages - 1;
			
			byte[] part = new byte[3 + ((!last || rest == 0) ? MAX_BYTES : rest)];
			part[0] = new Integer(numMessages - i - 1).byteValue();
			part[1] = new Integer(numMessages).byteValue();
			part[2] = new Integer(msgId).byteValue();
			
			System.arraycopy(message, i * MAX_BYTES, part, 3, part.length - 3);
			
			smsManager.sendDataMessage(dest, null, port, part, sentIntent, deliveryIntent);
		}
	}
}