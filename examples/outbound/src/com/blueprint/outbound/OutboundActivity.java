/*
 *  Copyright (c) 2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 */

package com.blueprint.outbound;

import java.util.UUID;

import org.twinlife.device.android.twinlife.BaseService.ErrorCode;
import org.twinlife.device.android.twinlife.PeerConnectionService;
import org.twinlife.device.android.twinlife.PeerConnectionService.CameraConstraints;
import org.twinlife.device.android.twinlife.PeerConnectionService.Offer;
import org.twinlife.device.android.twinlife.PeerConnectionService.OfferToReceive;
import org.twinlife.device.android.twinlife.PeerConnectionService.TerminateReason;
import org.twinlife.device.android.twinlife.TwincodeOutboundService.TwincodeOutbound;
import org.twinlife.device.android.twinlife.TwinlifeException;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class OutboundActivity extends TwinlifeActivityImpl {
	private static final String LOG_TAG = "OutboundActivity";
	private static final boolean DEBUG = true;

	private static final String USERNAME = "blueprint-outbound";
	private static final String PASSWORD = "blueprint-outbound";

	private static final String TWINCODE_ID = "6d68585c-8dc7-43eb-9d65-c38fcb7b3474";

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

					OutboundActivity.this.onError(requestId, errorCode,
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

					OutboundActivity.this.onTwinlifeReady();
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

					OutboundActivity.this.onReady();
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

					OutboundActivity.this.onConnect();
				}
			});
		}

		@Override
		public void onGetTwincodeOutbound(long requestId,
				final TwincodeOutbound twincodeOutbound) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"TwinlifeApplicationObserver.onGetTwincodeOutbound requestId="
								+ requestId + " twincodeOutbound="
								+ twincodeOutbound);
			}

			if (!mTwincodeId.equals(twincodeOutbound.getId())) {

				return;
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					OutboundActivity.this
							.onGetTwincodeOutbound(twincodeOutbound);
				}
			});
		}
	}

	private class PeerConnectionServiceObserver extends
			PeerConnectionService.DefaultServiceObserver {

		@Override
		public void onAcceptPeerConnection(final UUID peerConnectionId) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"PeerConnectionServiceObserver.onAcceptPeerConnection peerConnectionId="
								+ peerConnectionId);
			}

			if (!mPeerConnectionId.equals(peerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					OutboundActivity.this.onAccept();
				}
			});
		}

		@Override
		public void onTerminatePeerConnection(UUID peerConnectionId,
				final TerminateReason reason) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"PeerConnectionServiceObserver.onTerminatePeerConnection peerConnectionId="
								+ peerConnectionId + " reason=" + reason);
			}

			if (!mPeerConnectionId.equals(peerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					OutboundActivity.this.onTerminate(reason);
				}
			});
		}
	}

	private TextView mTwincodeNameTextView;
	private ImageButton mTwincodeImageButton;
	private TextView mMessageTextView;

	private UUID mTwincodeId;
	private TwincodeOutbound mTwincodeOutbound;
	private String mPeerId;
	private UUID mPeerConnectionId;

	private TwinlifeApplicationObserver mTwinlifeApplicationObserver;
	private PeerConnectionServiceObserver mPeerConnectionServiceObserver;

	//
	// Override BlueprintActivity methods
	//

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onCreate savedInstanceState=" + savedInstanceState);
		}

		super.onCreate(savedInstanceState);

		setContentView(R.layout.outbound_activity);

		mTwincodeNameTextView = (TextView) findViewById(R.id.outbound_activity_twincode_name);

		mTwincodeImageButton = (ImageButton) findViewById(R.id.outbound_activity_twincode_image);
		mTwincodeImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {

				OutboundActivity.this.onClick();
			}
		});

		mMessageTextView = (TextView) findViewById(R.id.outbound_activity_message);

		mTwinlifeApplicationObserver = new TwinlifeApplicationObserver();
		mPeerConnectionServiceObserver = new PeerConnectionServiceObserver();
	}

	@Override
	protected void onPause() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onPause");
		}

		super.onPause();

		getTwinlifeApplication().removeObserver(mTwinlifeApplicationObserver);

		if (hasTwinlife()) {
			getTwinlifeApplication().getPeerConnectionService()
					.removeServiceObserver(mPeerConnectionServiceObserver);
		}
	}

	@Override
	protected void onResume() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onResume");
		}

		super.onResume();

		mMessageTextView.setText("");
		
		if (!getTwinlifeApplication().isRunning()) {

			return;
		}

		getTwinlifeApplication().setObserver(mTwinlifeApplicationObserver);
	}

	//
	// Private methods
	//

	private void onTwinlifeReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onTwinlifeReady");
		}

		getTwinlifeApplication().getPeerConnectionService().setServiceObserver(
				mPeerConnectionServiceObserver);
	}

	private void onReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onReady");
		}

		long requestId = newRequestId();
		mTwincodeId = UUID.fromString(TWINCODE_ID);
		getTwinlifeApplication().getTwincodeOutbound(requestId, mTwincodeId);
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

	private void onGetTwincodeOutbound(TwincodeOutbound twincodeOutbound) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onGetTwincodeOutbound: twincodeOutbound="
					+ twincodeOutbound);
		}

		mTwincodeOutbound = twincodeOutbound;
		String name = (String) mTwincodeOutbound.getAttribute("name");
		if (name != null) {
			mTwincodeNameTextView.setText(name);
		}
		Bitmap bitmap = (Bitmap) mTwincodeOutbound.getAttribute("image");
		if (bitmap != null) {
			mTwincodeImageButton.setImageBitmap(bitmap);
		}
	}

	private void onClick() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onClick");
		}

		mPeerId = getTwinlifeApplication().getPeerId(mTwincodeId);

		mMessageTextView.setText(R.string.outbound_activity_calling);

		Offer offer = new Offer(true, true, false, false);

		OfferToReceive offerToReceive = new OfferToReceive(offer.audio,
				offer.video, offer.text, offer.files);

		mPeerConnectionId = getTwinlifeApplication().getPeerConnectionService()
				.createOutgoingPeerConnection(mPeerId, offer, offerToReceive);
		if (mPeerConnectionId != null) {
			getTwinlifeApplication().getPeerConnectionService()
					.initLocalPeerConnection(mPeerConnectionId, offer.audio,
							offer.video, CameraConstraints.ANY_CAMERA,
							offer.text);
		}
	}

	private void onAccept() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onAccept");
		}

		mMessageTextView.setText(R.string.outbound_activity_on_accept);

		Intent intent = new Intent();
		intent.putExtra(PeerConnectionService.INTENT_PEER_CONNECTION_ID,
				mPeerConnectionId.toString());
		intent.putExtra(PeerConnectionService.INTENT_PEER_CONNECTION_PEER_ID,
				mPeerId);
		intent.setClass(this, VideoCallActivity.class);

		startActivity(intent);
	}

	private void onTerminate(TerminateReason reason) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onTerminate");
		}

		mMessageTextView.setText(R.string.outbound_activity_on_terminate);
	}
}
