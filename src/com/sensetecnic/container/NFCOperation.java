package com.sensetecnic.container;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class NFCOperation extends Activity {

	private boolean mWriteMode = false;
	NfcAdapter mNfcAdapter;
	NdefMessage msgs[];
	PendingIntent mNfcPendingIntent;
	IntentFilter [] mNdefExchangeFilters;
    IntentFilter [] mWriteTagFilters;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_transfer);
        findViewById(R.id.write_tag).setOnClickListener(mTagWriter);
        
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
	
	
	public NdefMessage createNFCMessage() {
		
		Intent intent = getIntent();
		String data = intent.getStringExtra("ndefmessage");
		
		System.out.println("Input message to tag " + data);
		NdefRecord [] mimeRecord = {null};
		// mime data type string
		mimeRecord[0] = new NdefRecord(
			    NdefRecord.TNF_MIME_MEDIA ,
			    "application/com.sensetecnic.container.NFCOperation".getBytes(Charset.forName("US-ASCII")),
			    new byte[0], data.getBytes(Charset.forName("US-ASCII")));
		
		NdefMessage theMessage = new NdefMessage(mimeRecord);
		System.out.println("the message : " + theMessage.toString());
		
		return theMessage;
		
	}

	private void enableNdefExchangeMode() {
	    mNfcAdapter.enableForegroundNdefPush(NFCOperation.this, createNFCMessage() );
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
			byte[] payload = msgs[0].getRecords()[0].getPayload();
			System.out.println("Payload :" + new String(payload));
	        Intent NFC_transmission = getIntent();
	        NFC_transmission.putExtra("nfc_value", new String(payload));
	        setResult(RESULT_OK, NFC_transmission);
	        
			SharedPreferences settings = getSharedPreferences("container_prefs", 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("nfc_retrieved_message", new String(payload));
			editor.commit();
	        
			finish();
			
		}
		
//		enableNdefExchangeMode();
		
		//process NDEF MESSAGE here
	}
	
    protected void onPause()
    {
    	super.onPause();
    	disableNdefExchangeMode();
    }
	
	public void onBackPressed() {
		finish();
	}
    
	@Override
	protected void onNewIntent(Intent intent) {
	    // NDEF exchange mode for peer to peer
	    if (!mWriteMode &&  NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
	        NdefMessage[] msgs = getNdefMessages(intent);
	        Toast.makeText(this, "Item received", Toast.LENGTH_LONG).show();
	        System.out.println("msgs = " + msgs.toString());
	        Intent NFC_transmission = getIntent();
	        NFC_transmission.putExtra("nfc_value", msgs.toString());
	        setResult(RESULT_OK, NFC_transmission);
			finish();
	    }
	    
	 // Tag writing mode
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeTag(createNFCMessage(), detectedTag);
        }
	    
	}
	
	// when he select the button to write to it gives a dialog box to write to a tag, and will remain unless back button is hit or tag is touched
	 private View.OnClickListener mTagWriter = new View.OnClickListener() {
	        public void onClick(View arg0) {
	            // Write to a tag for as long as the dialog is shown.
	            disableNdefExchangeMode();
	            enableTagWriteMode();

	            new AlertDialog.Builder(NFCOperation.this).setTitle("Touch tag to write")
	                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
	                        public void onCancel(DialogInterface dialog) {
	                            disableTagWriteMode();
	                            enableNdefExchangeMode();
	                        }
	                    }).create().show();
	        }
	    };
	
	private void enableTagWriteMode() {
	    mWriteMode = true;
	    IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
	    mWriteTagFilters = new IntentFilter[] { tagDetected };
	    mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
	}
	
    private void disableTagWriteMode() {
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
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

	boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    toast("Tag is read-only.");
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    toast("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.");
                    return false;
                }

                ndef.writeNdefMessage(message);
                toast("Wrote message to pre-formatted tag.");
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        toast("Formatted tag and wrote message");
                        return true;
                    } catch (IOException e) {
                        toast("Failed to format tag.");
                        return false;
                    }
                } else {
                    toast("Tag doesn't support NDEF.");
                    return false;
                }
            }
        } catch (Exception e) {
            toast("Failed to write tag");
        }

        return false;
    }
	
    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    
}
