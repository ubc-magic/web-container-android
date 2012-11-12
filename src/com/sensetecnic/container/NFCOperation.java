package com.sensetecnic.container;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;

public class NFCOperation extends Activity {

	NfcAdapter mNfcAdapter;
	NdefMessage msgs[];
	TextView textView;
	PendingIntent mNfcPendingIntent;
	IntentFilter [] mNdefExchangeFilters;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //TextView textView = (TextView) findViewById(R.id.textView);
        // Check for available NFC Adapter
        
        
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }   
        else 
        {
        	System.out.println("NFC should be enabled");
        }
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // Intent filters for exchanging over p2p.
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefDetected.addDataType("text/plain");
        } catch (MalformedMimeTypeException e) {
        }
        mNdefExchangeFilters = new IntentFilter[] { ndefDetected };
        
    }
	
	
	public NdefMessage createNFCMessage(int NFC_Type) {
		
		NdefRecord [] mimeRecord = {null};
		switch (NFC_Type) {
		// Absolute URI
		case 1: NdefRecord uriRecord = new NdefRecord(
			    NdefRecord.TNF_ABSOLUTE_URI ,
			    "http://developer.android.com/index.html".getBytes(Charset.forName("US-ASCII")),
			    new byte[0], new byte[0]);
				break;
		// MIME Media
		case 2: mimeRecord[0] = new NdefRecord(
			    NdefRecord.TNF_MIME_MEDIA ,
			    "application/com.example.android.beam".getBytes(Charset.forName("US-ASCII")),
			    new byte[0], "Beam me up, Android!".getBytes(Charset.forName("US-ASCII")));
				break;
		case 3: mimeRecord[0] = new NdefRecord(
				NdefRecord.TNF_MIME_MEDIA , "image/jpeg".getBytes(Charset.forName("US-ASCII")),
				new byte[0], "hello".getBytes(Charset.forName("US-ASCII")));
				break;
		default: break;
		
		}
		NdefMessage theMessage = new NdefMessage(mimeRecord);
		
		return theMessage;
		
	}

	private void enableNdefExchangeMode() {
	    mNfcAdapter.enableForegroundNdefPush(NFCOperation.this, createNFCMessage(2) );
	    mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, 
	        mNdefExchangeFilters, null);
	}
	
    private void disableNdefExchangeMode() {
        mNfcAdapter.disableForegroundNdefPush(this);
        mNfcAdapter.disableForegroundDispatch(this);
    }
	

    
	public void onResume() {
		super.onResume();
		
		Intent intent = getIntent();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i< rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			}
			System.out.println("Message Received: " + msgs.toString());
			
		}
		enableNdefExchangeMode();
		
		//process NDEF MESSAGE here
	}
	
    protected void onPause()
    {
    	super.onPause();
    	disableNdefExchangeMode();
    }
	
	@Override
	protected void onNewIntent(Intent intent) {
	    // NDEF exchange mode
	    if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
	        NdefMessage[] msgs = getNdefMessages(intent);
	        Toast.makeText(this, "Say Hi", Toast.LENGTH_LONG).show();
	        System.out.println("msgs = " + msgs.toString());
	    }
	}
	
	NdefMessage[] getNdefMessages(Intent intent) {
	    // Parse the intent
	    NdefMessage[] msgs = null;
	    String action = intent.getAction();
	    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
	        || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
	        Parcelable[] rawMsgs = 
	            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
	        if (rawMsgs != null) {
	            msgs = new NdefMessage[rawMsgs.length];
	            for (int i = 0; i < rawMsgs.length; i++) {
	                msgs[i] = (NdefMessage) rawMsgs[i];
	            }
	        } else {
	            // Unknown tag type
	            byte[] empty = new byte[] {};
	            NdefRecord record = 
	                new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
	            NdefMessage msg = new NdefMessage(new NdefRecord[] {
	                record
	            });
	            msgs = new NdefMessage[] {
	                msg
	            };
	        }
	    } else {
	        Log.d("NFCOperation", "Unknown intent.");
	        finish();
	    }
	    return msgs;
	}

	
	
}
