/*
 *  Copyright (c) 2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 */

package com.blueprint.inbound;

import java.util.UUID;

import org.twinlife.device.android.twinlife.BaseService.ErrorCode;
import org.twinlife.device.android.twinlife.PeerConnectionService;
import org.twinlife.device.android.twinlife.PeerConnectionService.CameraConstraints;
import org.twinlife.device.android.twinlife.PeerConnectionService.Offer;
import org.twinlife.device.android.twinlife.PeerConnectionService.OfferToReceive;
import org.twinlife.device.android.twinlife.PeerConnectionService.TerminateReason;
import org.twinlife.device.android.twinlife.TwincodeInboundService.TwincodeInbound;
import org.twinlife.device.android.twinlife.TwinlifeException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

public class InboundActivity extends TwinlifeActivityImpl {
	private static final String LOG_TAG = "InboundActivity";
	private static final boolean DEBUG = true;

	private static final String USERNAME = "blueprint-inbound";
	private static final String PASSWORD = "blueprint-inbound";

	private static final String TWINCODE_INBOUND_ID = "36eedbde-807a-496d-8326-6ded72afd809";

	private class TwinlifeApplicationObserver extends
			TwinlifeApplication.DefaultObserver {

		@Override
		public void onError(final long requestId, final ErrorCode errorCode,
				final String errorParameter) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"TwinlifeApplicationObserver.onError: requestId="
								+ requestId + " errorCode=" + errorCode
								+ " errorParameter=" + errorParameter);
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					InboundActivity.this.onError(requestId, errorCode,
							errorParameter);
				}
			});
		}

		@Override
		public void onTwinlifeReady() {
			if (DEBUG) {
				Log.d(LOG_TAG, "TwinlifeApplicationObserver.onTwinlifeReady");
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					InboundActivity.this.onTwinlifeReady();
				}
			});
		}

		@Override
		public void onReady() {
			if (DEBUG) {
				Log.d(LOG_TAG, "TwinlifeApplicationObserver.onReady");
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					InboundActivity.this.onReady();
				}
			});
		}

		@Override
		public void onConnect() {
			if (DEBUG) {
				Log.d(LOG_TAG, "TwinlifeApplicationObserver.onConnect");
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					InboundActivity.this.onConnect();
				}
			});
		}

		@Override
		public void onGetTwincodeInbound(long requestId,
				final TwincodeInbound twincodeInbound) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"TwinlifeApplicationObserver.onGetTwincodeInbound requestId="
								+ requestId + " twincodeInbound="
								+ twincodeInbound);
			}

			if (!mTwincodeInboundId.equals(twincodeInbound.getId())) {

				return;
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					InboundActivity.this.onGetTwincodeInbound(twincodeInbound);
				}
			});
		}
	}

	private TextView mTwincodeNameTextView;
	private ImageButton mTwincodeImageButton;
	private TextView mMessageTextView;

	private UUID mTwincodeInboundId;
	private TwincodeInbound mTwincodeInbound;
	private String mPeerId;
	private UUID mPeerConnectionId;

	private TwinlifeApplicationObserver mTwinlifeApplicationObserver;

	//
	// Override BlueprintActivity methods
	//

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onCreate savedInstanceState=" + savedInstanceState);
		}

		super.onCreate(savedInstanceState);

		setContentView(R.layout.inbound_activity);

		mTwincodeNameTextView = (TextView) findViewById(R.id.inbound_activity_twincode_name);

		mTwincodeImageButton = (ImageButton) findViewById(R.id.inbound_activity_twincode_image);

		mMessageTextView = (TextView) findViewById(R.id.inbound_activity_message);

		mTwinlifeApplicationObserver = new TwinlifeApplicationObserver();
	}

	@Override
	protected void onPause() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onPause");
		}

		super.onPause();

		getTwinlifeApplication().removeObserver(mTwinlifeApplicationObserver);
	}

	@Override
	public void onResume() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onResume");
		}

		super.onResume();

		if (!getTwinlifeApplication().isRunning()) {

			return;
		}

		mMessageTextView.setText("");

		getTwinlifeApplication().setObserver(mTwinlifeApplicationObserver);
	}

	//
	// Override Activity methods
	//

	@Override
	protected void onNewIntent(Intent intent) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onNewIntent: intent=" + intent);
		}

		mPeerConnectionId = UUID
				.fromString(intent
						.getStringExtra(PeerConnectionService.INTENT_PEER_CONNECTION_ID));
		mPeerId = intent
				.getStringExtra(PeerConnectionService.INTENT_PEER_CONNECTION_PEER_ID);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.inbound_activity_incoming_call);
		builder.setCancelable(false);

		builder.setPositiveButton(
				R.string.inbound_activity_hangup_confirmation_yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						onAccept();
					}
				});

		builder.setNegativeButton(
				R.string.inbound_activity_hangup_confirmation_no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						onTerminate();
					}
				});

		AlertDialog alert = builder.create();
		alert.show();

	}

	//
	// Private methods
	//

	private void onTwinlifeReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onTwinlifeReady");
		}
	}

	private void onReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onReady");
		}

		long requestId = newRequestId();
		mTwincodeInboundId = UUID.fromString(TWINCODE_INBOUND_ID);
		getTwinlifeApplication().getTwincodeInbound(requestId,
				mTwincodeInboundId);
	}

	private void onConnect() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onConnect");
		}

		if (!getTwinlifeApplication().isSignIn()) {
			try {
				getTwinlifeApplication().signIn(USERNAME, PASSWORD, false);
			} catch (TwinlifeException exception) {
				fatalError("signIn exception=" + exception,
						new DefaultErrorCallback() {
							@Override
							public void onClick(DialogInterface dialog) {

								finish();
							}
						});
			}
		}
	}

	private void onError(long requestId, ErrorCode errorCode,
			String errorParameter) {
		if (DEBUG) {
			Log.d(LOG_TAG, " onError: requestId=" + requestId + " errorCode="
					+ errorCode + " errorParameter=" + errorParameter);
		}

		fatalError("onError: requestId=" + requestId + " errorCode="
				+ errorCode + " errorParameter=" + errorParameter,
				new DefaultErrorCallback() {
					@Override
					public void onClick(DialogInterface dialog) {

						finish();
					}
				});
	}

	private void onGetTwincodeInbound(TwincodeInbound twincodeInbound) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onGetTwincodeInbound: twincodeInbound="
					+ twincodeInbound);
		}

		mTwincodeInbound = twincodeInbound;

		String name = (String) mTwincodeInbound.getAttribute("name");
		if (name != null) {
			mTwincodeNameTextView.setText(name);
		}
		Bitmap bitmap = (Bitmap) mTwincodeInbound.getAttribute("image");
		if (bitmap != null) {
			mTwincodeImageButton.setImageBitmap(bitmap);
		}
	}

	private void onAccept() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onAccept");
		}

		mMessageTextView.setText(R.string.inbound_activity_on_accept);

		Offer peerOffer = getTwinlifeApplication().getPeerConnectionService()
				.getPeerOffer(mPeerConnectionId);
		if (peerOffer == null) {
			peerOffer = new Offer(true, true, false, false);
		}

		Offer offer = peerOffer;

		OfferToReceive offerToReceive = new OfferToReceive(offer.audio,
				offer.video, offer.text, offer.files);

		if (!getTwinlifeApplication().getPeerConnectionService()
				.createIncomingPeerConnection(mPeerConnectionId, offer,
						offerToReceive)) {

			return;
		}

		if (!getTwinlifeApplication().getPeerConnectionService()
				.initLocalPeerConnection(mPeerConnectionId, offer.audio,
						offer.video, CameraConstraints.ANY_CAMERA, offer.text)) {

			return;
		}

		Intent intent = new Intent();
		intent.putExtra(PeerConnectionService.INTENT_PEER_CONNECTION_ID,
				mPeerConnectionId.toString());
		intent.putExtra(PeerConnectionService.INTENT_PEER_CONNECTION_PEER_ID,
				mPeerId);
		intent.setClass(this, VideoCallActivity.class);

		startActivity(intent);
	}

	private void onTerminate() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onTerminate");
		}

		mMessageTextView.setText(R.string.inbound_activity_on_terminate);

		getTwinlifeApplication().getPeerConnectionService()
				.terminatePeerConnection(mPeerConnectionId,
						TerminateReason.DECLINE);
	}
}
