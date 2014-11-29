/*
 *  Copyright (c) 2012-2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Tengfei Wang (Tengfei.Wang@twinlife-systems.com)
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 *  Contributor: Xiaobo Xie (Xiaobo.Xie@twinlife-systems.com)
 */

package com.blueprint.inbound;

import java.util.HashMap;
import java.util.UUID;

import org.twinlife.device.android.twinlife.PeerConnectionService;
import org.twinlife.device.android.twinlife.PeerConnectionService.TerminateReason;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VideoCallActivity extends TwinlifeActivityImpl {
	private static final String LOG_TAG = "VideoCallActivity";
	private static final boolean DEBUG = false;

	private class TwinlifeApplicationObserver extends
			TwinlifeApplication.DefaultObserver {

		@Override
		public void onTwinlifeReady() {
			if (DEBUG) {
				Log.d(LOG_TAG, "TwinlifeApplicationObserver.onTwinlifeReady");
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					VideoCallActivity.this.onTwinlifeReady();
				}
			});
		}

		@Override
		public void onReady() {
			if (DEBUG) {
				Log.d(LOG_TAG, "TwinlifeApplicationObserver.onTwinmeReady");
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					VideoCallActivity.this.onReady();
				}
			});
		}
	}

	private class PeerConnectionServiceObserver extends
			PeerConnectionService.DefaultServiceObserver {

		@Override
		public void onTerminatePeerConnection(final UUID peerConnectionId,
				final TerminateReason terminateReason) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"PeerConnectionServiceObserver.onTerminatePeerConnection peerConnectionId="
								+ peerConnectionId + " terminateReason="
								+ terminateReason);
			}

			if (!peerConnectionId.equals(mPeerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					VideoCallActivity.this
							.onTerminatePeerConnection(terminateReason);
				}
			});
		}

		@Override
		public void onStateChange(final UUID peerConnectionId,
				final SignalingState signalingState,
				final IceGatheringState iceGatheringState,
				final IceConnectionState iceConnectionState) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"PeerConnectionServiceObserver.onStateChange peerConnectionId="
								+ peerConnectionId + " signalingState="
								+ signalingState + " iceGatheringState="
								+ iceGatheringState + "iceConnectionState"
								+ iceConnectionState);
			}

			if (!peerConnectionId.equals(mPeerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {
				public void run() {

					VideoCallActivity.this.onStateChange(signalingState,
							iceGatheringState, iceConnectionState);
				}
			});
		}

		@Override
		public void onAddLocalVideoTrack(final UUID peerConnectionId,
				final MediaStream mediaStream, final VideoTrack videoTrack) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"PeerConnectionServiceObserver.onAddLocalVideoTrack peerConnectionId="
								+ peerConnectionId + " mediaStream="
								+ mediaStream.label() + " videoTrack="
								+ videoTrack.id());
			}

			if (!peerConnectionId.equals(mPeerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {
				public void run() {

					VideoCallActivity.this.onAddLocalVideoTrack(mediaStream,
							videoTrack);
				}
			});
		}

		@Override
		public void onAddRemoteVideoTrack(final UUID peerConnectionId,
				final MediaStream mediaStream, final VideoTrack videoTrack) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"PeerConnectionServiceObserver.onAddRemoteVideoTrack peerConnectionId="
								+ peerConnectionId + " mediaStream="
								+ mediaStream.label() + " videoTrack="
								+ videoTrack.id());
			}

			if (!peerConnectionId.equals(mPeerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {
				public void run() {

					VideoCallActivity.this.onAddRemoteVideoTrack(mediaStream,
							videoTrack);
				}
			});
		}

		@Override
		public void onRemoveLocalVideoTrack(final UUID peerConnectionId,
				final MediaStream mediaStream, final VideoTrack videoTrack) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"onRemoveLocalVideoTrack peerConnectionId="
								+ peerConnectionId + " mediaStream="
								+ mediaStream.label() + " videoTrack="
								+ videoTrack.id());
			}

			if (!peerConnectionId.equals(mPeerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {
				public void run() {

					VideoCallActivity.this.onRemoveLocalVideoTrack(mediaStream,
							videoTrack);
				}
			});
		}

		@Override
		public void onRemoveRemoteVideoTrack(final UUID peerConnectionId,
				final MediaStream mediaStream, final VideoTrack videoTrack) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"onRemoveRemoteVideoTrack peerConnectionId="
								+ peerConnectionId + " mediaStream="
								+ mediaStream.label() + " videoTrack="
								+ videoTrack.id());
			}

			if (!peerConnectionId.equals(mPeerConnectionId)) {

				return;
			}

			runOnUiThread(new Runnable() {
				public void run() {

					VideoCallActivity.this.onRemoveRemoteVideoTrack(
							mediaStream, videoTrack);
				}
			});
		}
	}

	private RelativeLayout mVideoRelativeLayout;

	private VideoStreamsView mVideoStreamView;
	private HashMap<MediaStreamTrack, VideoRenderer> mVideoRenderers = new HashMap<MediaStreamTrack, VideoRenderer>();

	private ImageButton mSwitchCameraButton;

	private boolean mIsAudioMute = false;
	private boolean mIsCameraMute = false;

	private SeekBar mZoomSeekBar;

	private UUID mPeerConnectionId;
	private String mPeerId;

	private TwinlifeApplicationObserver mTwinlifeApplicationObserver;
	private PeerConnectionServiceObserver mPeerConnectionServiceObserver;

	//
	// Override BlueprintActivity methods
	//

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onCreate: savedInstanceState=" + savedInstanceState);
		}

		super.onCreate(savedInstanceState);

		WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
		layoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
		getWindow().setAttributes(layoutParams);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().hide();

		setContentView(R.layout.video_call_activity);
		mVideoRelativeLayout = (RelativeLayout) findViewById(R.id.video_call_activity_view);

		mVideoStreamView = new VideoStreamsView(VideoCallActivity.this);
		mVideoRelativeLayout.addView(mVideoStreamView);

		if (savedInstanceState != null) {
			mPeerConnectionId = UUID
					.fromString(savedInstanceState
							.getString(PeerConnectionService.INTENT_PEER_CONNECTION_ID));
			mPeerId = savedInstanceState
					.getString(PeerConnectionService.INTENT_PEER_CONNECTION_PEER_ID);
		} else {
			Intent intent = getIntent();
			mPeerConnectionId = UUID
					.fromString(intent
							.getStringExtra(PeerConnectionService.INTENT_PEER_CONNECTION_ID));
			mPeerId = intent
					.getStringExtra(PeerConnectionService.INTENT_PEER_CONNECTION_PEER_ID);
		}

		Button hangUpButton = (Button) findViewById(R.id.video_call_activity_hang_up);
		hangUpButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				terminateCall(TerminateReason.SUCCESS);

				finish();
			}
		});

		final ImageButton microButton = (ImageButton) findViewById(R.id.video_call_activity_micro);
		microButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mIsAudioMute) {
					mIsAudioMute = false;
					microButton.setBackgroundResource(R.drawable.micro_off);
				} else {
					mIsAudioMute = true;
					microButton.setBackgroundResource(R.drawable.micro_on);
				}

				getTwinlifeApplication().getPeerConnectionService()
						.setAudioMute(mPeerConnectionId, mIsAudioMute);
			}
		});

		final ImageButton muteCameraButton = (ImageButton) findViewById(R.id.video_call_activity_mute_camera);
		muteCameraButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mIsCameraMute) {
					muteCameraButton
							.setBackgroundResource(R.drawable.camera_off);
					mIsCameraMute = false;
					mSwitchCameraButton.setVisibility(View.VISIBLE);
				} else {
					muteCameraButton
							.setBackgroundResource(R.drawable.camera_on);
					mIsCameraMute = true;
					mSwitchCameraButton.setVisibility(View.INVISIBLE);
				}

				getTwinlifeApplication().getPeerConnectionService()
						.setCameraMute(mPeerConnectionId, mIsCameraMute);
			}
		});

		mSwitchCameraButton = (ImageButton) findViewById(R.id.video_call_activity_camera);
		mSwitchCameraButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				getTwinlifeApplication().getPeerConnectionService()
						.switchCamera(
								mPeerConnectionId,
								getWindowManager().getDefaultDisplay()
										.getRotation());
				if (getTwinlifeApplication().getPeerConnectionService()
						.isZoomSupported(mPeerConnectionId)) {
					mZoomSeekBar.setVisibility(View.VISIBLE);
				} else {
					mZoomSeekBar.setVisibility(View.INVISIBLE);
				}
			}
		});

		mZoomSeekBar = (SeekBar) findViewById(R.id.video_call_activity_camera_zoom);
		mZoomSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				getTwinlifeApplication().getPeerConnectionService().setZoom(
						mPeerConnectionId, progress);
				mZoomSeekBar.setVisibility(View.VISIBLE);
			}
		});

		mTwinlifeApplicationObserver = new TwinlifeApplicationObserver();
		mPeerConnectionServiceObserver = new PeerConnectionServiceObserver();
	}

	@Override
	protected void onPause() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onPause");
		}

		super.onPause();

		mVideoStreamView.disposeVideoRenderers();

		getTwinlifeApplication().removeObserver(mTwinlifeApplicationObserver);

		if (hasTwinlife()) {
			getTwinlifeApplication().getPeerConnectionService()
					.removeServiceObserver(mPeerConnectionServiceObserver);
		}

		mVideoStreamView.onPause();
	}

	@Override
	protected void onResume() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onResume");
		}

		super.onResume();

		if (!getTwinlifeApplication().isRunning()) {

			return;
		}

		mVideoStreamView.onResume();

		getTwinlifeApplication().setObserver(mTwinlifeApplicationObserver);
	}

	//
	// Override Activity methods
	//

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onSaveInstanceState");
		}

		outState.putString(PeerConnectionService.INTENT_PEER_CONNECTION_ID,
				mPeerConnectionId.toString());
		outState.putString(
				PeerConnectionService.INTENT_PEER_CONNECTION_PEER_ID, mPeerId);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onConfigurationChanged");
		}

		super.onConfigurationChanged(newConfig);

		getTwinlifeApplication().getPeerConnectionService().setCaptureRotation(
				mPeerConnectionId,
				getWindowManager().getDefaultDisplay().getRotation());

		mVideoStreamView.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onBackPressed");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.video_call_activity_hangup_confirmation);
		builder.setCancelable(false);

		builder.setPositiveButton(
				R.string.video_call_activity_hangup_confirmation_yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						terminateCall(TerminateReason.SUCCESS);

						finish();
					}
				});

		builder.setNegativeButton(
				R.string.video_call_activity_hangup_confirmation_no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onUserLeaveHint() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onUserLeaveHint");
		}

		terminateCall(TerminateReason.SUCCESS);

		finish();
	}

	//
	// Private methods
	//

	//
	// Private methods
	//

	private void onTwinlifeReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onTwinlifeReady");
		}

		getTwinlifeApplication().getPeerConnectionService().setServiceObserver(
				mPeerConnectionServiceObserver);

		getTwinlifeApplication().getPeerConnectionService().setCaptureRotation(
				mPeerConnectionId,
				getWindowManager().getDefaultDisplay().getRotation());

		if (getTwinlifeApplication().getPeerConnectionService()
				.isZoomSupported(mPeerConnectionId)) {
			mZoomSeekBar.setVisibility(View.VISIBLE);
		}
	}

	private void onReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onReady");
		}
	}

	private void onTerminatePeerConnection(TerminateReason terminateReason) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onTerminatePeerConnection terminateReason="
					+ terminateReason);
		}

		String message = getString(R.string.video_call_activity_terminate);

		message(message, new DefaultMessageCallback() {

			@Override
			public void onClick(DialogInterface dialog) {

				finish();
			}
		});
	}

	private void onStateChange(SignalingState signalingState,
			IceGatheringState iceGatheringState,
			IceConnectionState iceConnectionState) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onStateChange signalingState=" + signalingState
					+ " iceGatheringState=" + iceGatheringState
					+ " iceConnectionState" + iceConnectionState);
		}

		if (signalingState == SignalingState.CLOSED
				|| iceConnectionState == IceConnectionState.CLOSED
				|| iceConnectionState == IceConnectionState.DISCONNECTED
				|| iceConnectionState == IceConnectionState.FAILED) {
			terminateCall(TerminateReason.CONNECTIVITY_ERROR);

			String message = getString(R.string.video_call_activity_terminate);
			message(message, new DefaultMessageCallback() {

				@Override
				public void onClick(DialogInterface dialog) {

					finish();
				}
			});
		}
	}

	private void onAddLocalVideoTrack(MediaStream mediaStream,
			VideoTrack videoTrack) {
		if (DEBUG) {
			Log.d(LOG_TAG,
					"PeerConnectionServiceObserver.onAddLocalVideoTrack mediaStream="
							+ mediaStream.label() + " videoTrack="
							+ videoTrack.id());
		}

		VideoRenderer videoRenderer = mVideoStreamView.getLocalVideoRenderer();
		if (videoRenderer == null) {
			// TBD

			return;
		}

		videoTrack.addRenderer(videoRenderer);
		mVideoRenderers.put(videoTrack, videoRenderer);
	}

	private void onAddRemoteVideoTrack(MediaStream mediaStream,
			VideoTrack videoTrack) {
		if (DEBUG) {
			Log.d(LOG_TAG,
					"PeerConnectionServiceObserver.onAddRemoteVideoTrack mediaStream="
							+ mediaStream.label() + " videoTrack="
							+ videoTrack.id());
		}

		VideoRenderer videoRenderer = mVideoStreamView.getRemoteVideoRenderer();
		if (videoRenderer == null) {
			// TBD

			return;
		}

		videoTrack.addRenderer(videoRenderer);
		mVideoRenderers.put(videoTrack, videoRenderer);
	}

	private void onRemoveLocalVideoTrack(MediaStream mediaStream,
			VideoTrack videoTrack) {
		if (DEBUG) {
			Log.d(LOG_TAG,
					"onRemoveLocalVideoTrack mediaStream="
							+ mediaStream.label() + " videoTrack="
							+ videoTrack.id());
		}

		VideoRenderer videoRenderer = mVideoRenderers.remove(videoTrack);
		if (videoRenderer == null) {
			return;
		}
		videoTrack.removeRenderer(videoRenderer);
		mVideoStreamView.disposeVideoRenderer(videoRenderer);
	}

	private void onRemoveRemoteVideoTrack(MediaStream mediaStream,
			VideoTrack videoTrack) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onRemoveRemoteVideoTrack mediaStream="
					+ mediaStream.label() + " videoTrack=" + videoTrack.id());
		}

		VideoRenderer videoRenderer = mVideoRenderers.remove(videoTrack);
		if (videoRenderer == null) {
			return;
		}

		videoTrack.removeRenderer(videoRenderer);
		mVideoStreamView.disposeVideoRenderer(videoRenderer);
	}

	private void terminateCall(TerminateReason reason) {
		if (DEBUG) {
			Log.d(LOG_TAG, "terminateCall: reason=" + reason);
		}

		getTwinlifeApplication().getPeerConnectionService()
				.terminatePeerConnection(mPeerConnectionId, reason);
	}
}
