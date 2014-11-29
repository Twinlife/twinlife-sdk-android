/*
 *  Copyright (c) 2012-2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 */

package com.blueprint.outbound;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.twinlife.device.android.twinlife.AccountService;
import org.twinlife.device.android.twinlife.BaseService;
import org.twinlife.device.android.twinlife.BaseService.BaseServiceId;
import org.twinlife.device.android.twinlife.BaseService.ErrorCode;
import org.twinlife.device.android.twinlife.ConnectivityService;
import org.twinlife.device.android.twinlife.ManagementService;
import org.twinlife.device.android.twinlife.PeerConnectionService;
import org.twinlife.device.android.twinlife.TwincodeInboundService;
import org.twinlife.device.android.twinlife.TwincodeOutboundService;
import org.twinlife.device.android.twinlife.TwincodeInboundService.TwincodeInbound;
import org.twinlife.device.android.twinlife.TwincodeOutboundService.TwincodeOutbound;
import org.twinlife.device.android.twinlife.Twinlife;
import org.twinlife.device.android.twinlife.Twinlife.TwinlifeConfiguration;
import org.twinlife.device.android.twinlife.TwinlifeException;
import org.twinlife.device.android.twinlife.TwinlifeImpl;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class TwinlifeApplicationImpl extends Application implements
		ServiceConnection, TwinlifeApplication {
	private static final String LOG_TAG = "TwinlifeApplicationImpl";
	private static final boolean DEBUG = false;

	private class ConnectivityServiceObserver extends
			ConnectivityService.DefaultServiceObserver {

		@Override
		public void onConnect() {
			if (DEBUG) {
				Log.d(LOG_TAG, "ConnectivityServiceObserver.onConnect");
			}

			TwinlifeApplicationImpl.this.onConnect();
		}

		@Override
		public void onDisconnect() {
			if (DEBUG) {
				Log.d(LOG_TAG, "ConnectivityServiceObserver.onDisconnect");
			}

			TwinlifeApplicationImpl.this.onDisconnect();
		}
	}

	private class AccountServiceObserver extends
			AccountService.DefaultServiceObserver {

		@Override
		public void onSignIn() {
			if (DEBUG) {
				Log.d(LOG_TAG, "AccountServiceObserver.onSignIn");
			}

			TwinlifeApplicationImpl.this.onSignIn();
		}

		@Override
		public void onSignOut() {
			if (DEBUG) {
				Log.d(LOG_TAG, "AccountServiceObserver.onSignOut");
			}

			TwinlifeApplicationImpl.this.onSignOut();
		}
	}

	private class ManagementServiceObserver extends
			ManagementService.DefaultServiceObserver {

		@Override
		public void onError(long requestId, final ErrorCode errorCode,
				final String errorParameter) {
			if (DEBUG) {
				Log.e(LOG_TAG, "ManagementServiceObserver.onError: requestId="
						+ requestId + " errorCode=" + errorCode
						+ " errorParameter=" + errorParameter);
			}

			Log.e(LOG_TAG,
					getString(R.string.twinlife_application_wrong_configuration));
		}

		@Override
		public void onValidateConfiguration(long requestId) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"ManagementServiceObserver.onValidateConfiguration: requestId="
								+ requestId);
			}

			TwinlifeApplicationImpl.this.onValidateConfiguration();
		}
	}

	private class TwincodeInboundServiceObserver extends
			TwincodeInboundService.DefaultServiceObserver {

		@Override
		public void onError(long requestId, ErrorCode errorCode,
				String errorParameter) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"TwinlifeApplicationObserver.onError: requestId="
								+ requestId + " errorCode=" + errorCode
								+ " errorParameter=" + errorParameter);
			}

			TwinlifeApplicationImpl.this.onError(requestId, errorCode,
					errorParameter);
		}

		@Override
		public void onGetTwincode(long requestId,
				TwincodeInbound twincodeInbound) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"TwincodeInboundServiceObserver.onGetTwincode requestId="
								+ requestId + " twincodeInbound="
								+ twincodeInbound);
			}

			TwinlifeApplicationImpl.this.onGetTwincodeInbound(requestId,
					twincodeInbound);
		}
	}

	private class TwincodeOutboundServiceObserver extends
			TwincodeOutboundService.DefaultServiceObserver {

		@Override
		public void onError(long requestId, ErrorCode errorCode,
				String errorParameter) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"TwinlifeApplicationObserver.onError: requestId="
								+ requestId + " errorCode=" + errorCode
								+ " errorParameter=" + errorParameter);
			}

			TwinlifeApplicationImpl.this.onError(requestId, errorCode,
					errorParameter);
		}

		@Override
		public void onGetTwincode(long requestId,
				TwincodeOutbound twincodeOutbound) {
			if (DEBUG) {
				Log.d(LOG_TAG,
						"TwincodeInboundServiceObserver.onGetTwincode requestId="
								+ requestId + " twincodeOutbound="
								+ twincodeOutbound);
			}

			TwinlifeApplicationImpl.this.onGetTwincodeOutbound(requestId,
					twincodeOutbound);
		}
	}

	private volatile boolean mRunning;

	private volatile Twinlife mTwinlife;
	private volatile boolean mHasTwinlife;

	private TwinlifeConfiguration mTwinlifeConfiguration;

	private ConnectivityServiceObserver mConnectivityServiceObserver;
	private AccountServiceObserver mAccountServiceObserver;
	private ManagementServiceObserver mManagementServiceObserver;
	private TwincodeInboundServiceObserver mTwincodeInboundServiceObserver;
	private TwincodeOutboundServiceObserver mTwincodeOutboundServiceObserver;

	private CopyOnWriteArraySet<TwinlifeApplication.Observer> mObservers;

	private AtomicLong mRequestId = new AtomicLong(
			BaseService.DEFAULT_REQUEST_ID);

	//
	// Override Application methods
	//

	@Override
	public void onCreate() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onCreate");
		}

		super.onCreate();

		mRunning = true;

		if (!TwinlifeImpl.isStarted()) {
			startService(new Intent(this, TwinlifeImpl.class));
		}

		if (!bindService(new Intent(this, TwinlifeImpl.class), this,
				Context.BIND_AUTO_CREATE)) {

			Log.e(LOG_TAG, "bindService: bindService failed");
		}

		mTwinlifeConfiguration = new BlueprintConfiguration(this);

		mObservers = new CopyOnWriteArraySet<Observer>();

		mConnectivityServiceObserver = new ConnectivityServiceObserver();
		mAccountServiceObserver = new AccountServiceObserver();
		mManagementServiceObserver = new ManagementServiceObserver();

		mTwincodeInboundServiceObserver = new TwincodeInboundServiceObserver();
		mTwincodeOutboundServiceObserver = new TwincodeOutboundServiceObserver();
	}

	//
	// Implementation of ServiceConnection class
	//

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onServiceConnected: name=" + name + " service="
					+ service);
		}

		mTwinlife = ((TwinlifeImpl.LocalBinder) service).getService();
		mHasTwinlife = true;

		if (!isConfigured()) {
			if (!configure(getTwinlifeConfiguration())) {
				Log.e(LOG_TAG,
						getString(R.string.twinlife_application_wrong_configuration));

				return;
			}
		}

		mTwinlife.getConnectivityService().setServiceObserver(
				mConnectivityServiceObserver);
		mTwinlife.getAccountService().setServiceObserver(
				mAccountServiceObserver);
		mTwinlife.getManagementService().setServiceObserver(
				mManagementServiceObserver);

		if (mTwinlifeConfiguration.baseServiceConfigurations[BaseServiceId.TWINCODE_INBOUND_SERVICE_ID
				.ordinal()].serviceOn) {
			mTwinlife.getTwincodeInboundService().setServiceObserver(
					mTwincodeInboundServiceObserver);
		}
		if (mTwinlifeConfiguration.baseServiceConfigurations[BaseServiceId.TWINCODE_OUTBOUND_SERVICE_ID
				.ordinal()].serviceOn) {
			mTwinlife.getTwincodeOutboundService().setServiceObserver(
					mTwincodeOutboundServiceObserver);
		}

		onTwinlifeReady();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onServiceDisconnected: name=" + name);
		}

		mHasTwinlife = false;
		mTwinlife = null;
		mRunning = false;
	}

	//
	// Implementation of TwinlifeApplication interface
	//

	@Override
	public final boolean isRunning() {
		if (DEBUG) {
			Log.d(LOG_TAG, "isRunning");
		}

		return mRunning;
	}

	@Override
	public final void stop() {
		if (DEBUG) {
			Log.d(LOG_TAG, "stop");
		}

		mRunning = false;
		mHasTwinlife = false;
		mTwinlife.stop();
		unbindService(this);
	}

	@Override
	public void restart() {
		if (DEBUG) {
			Log.d(LOG_TAG, "restart");
		}

		if (mRunning) {

			return;
		}
		mRunning = true;

		if (!TwinlifeImpl.isStarted()) {
			startService(new Intent(this, TwinlifeImpl.class));
		}

		if (!bindService(new Intent(this, TwinlifeImpl.class), this,
				Context.BIND_AUTO_CREATE)) {

			Log.e(LOG_TAG, "bindService: bindService failed");
		}
	}

	@Override
	public final boolean hasTwinlife() {
		if (DEBUG) {
			Log.d(LOG_TAG, "hasTwinlife");
		}

		return mHasTwinlife;
	}

	@Override
	public boolean isConfigured() {

		if (mTwinlife == null) {

			return false;
		}

		return mTwinlife.isConfigured();
	}

	@Override
	public boolean configure(TwinlifeConfiguration twinlifeConfiguration) {

		return mTwinlife.configure(twinlifeConfiguration);
	}

	@Override
	public TwinlifeConfiguration getTwinlifeConfiguration() {
		if (DEBUG) {
			Log.d(LOG_TAG, "getTwinlifeConfiguration");
		}

		return mTwinlifeConfiguration;
	}

	@Override
	public final long newRequestId() {
		if (DEBUG) {
			Log.d(LOG_TAG, "newRequestId");
		}

		return mRequestId.incrementAndGet();
	}

	@Override
	public final Executor getObserverExecutor() {
		if (DEBUG) {
			Log.d(LOG_TAG, "getObserverExecutor");
		}

		return mTwinlife.getObserverExecutor();
	}

	@Override
	public final void setObserver(final Observer observer) {
		if (DEBUG) {
			Log.d(LOG_TAG, "setObserver: observer=" + observer);
		}

		if (mObservers.add(observer)) {
			if (hasTwinlife()) {
				fireOnTwinlifeReady();

				if (mTwinlife.getConnectivityService().isConnected()) {
					fireOnConnect();

					if (mTwinlife.getAccountService().isSignIn()) {
						fireOnSignIn();

						if (mTwinlife.getManagementService()
								.hasValidConfiguration()) {
							fireOnReady();
						}
					}
				}
			}
		}
	}

	@Override
	public final void removeObserver(Observer observer) {
		if (DEBUG) {
			Log.d(LOG_TAG, "removeObserver: observer=" + observer);
		}

		mObservers.remove(observer);
	}

	@Override
	public boolean isSignIn() {
		if (DEBUG) {
			Log.d(LOG_TAG, "isSignIn");
		}

		return mTwinlife.getAccountService().isSignIn();
	}

	@Override
	public void signIn(String username, String password,
			boolean rememberPassword) throws TwinlifeException {
		if (DEBUG) {
			Log.d(LOG_TAG, "signIn: username=" + username + " password"
					+ password + " rememberPassword=" + rememberPassword);
		}

		mTwinlife.getAccountService().signIn(username, password,
				rememberPassword);
	}

	@Override
	public void getTwincodeInbound(long requestId, UUID twincodeInboundId) {
		if (DEBUG) {
			Log.d(LOG_TAG, "getTwincodeInbound: requestId=" + requestId
					+ " twincodeInboundId=" + twincodeInboundId);
		}

		mTwinlife.getTwincodeInboundService().getTwincode(requestId,
				twincodeInboundId);
	}

	@Override
	public void getTwincodeOutbound(long requestId, UUID twincodeOutboundId) {
		if (DEBUG) {
			Log.d(LOG_TAG, "getTwincodeInbound: requestId=" + requestId
					+ " twincodeOutboundId=" + twincodeOutboundId);
		}

		mTwinlife.getTwincodeOutboundService().getTwincode(requestId,
				twincodeOutboundId);
	}

	@Override
	public String getPeerId(UUID twincodeOutboundId) {
		if (DEBUG) {
			Log.d(LOG_TAG, "getPeerId: twincodeOutboundId="
					+ twincodeOutboundId);
		}

		return mTwinlife.getTwincodeOutboundService().getPeerId(
				twincodeOutboundId);
	}

	@Override
	public final PeerConnectionService getPeerConnectionService() {
		if (DEBUG) {
			Log.d(LOG_TAG, "getPeerConnectionService");
		}

		return mTwinlife.getPeerConnectionService();
	}

	//
	// Private methods
	//

	private void onTwinlifeReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onTwinlifeReady");
		}

		fireOnTwinlifeReady();
	}

	private void onConnect() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onConnect");
		}

		fireOnConnect();
	}

	private void onDisconnect() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onDisconnect");
		}

		fireOnDisconnect();
	}

	private void onSignIn() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onSignIn");
		}

		mTwinlife.getManagementService().validateConfiguration(
				BaseService.DEFAULT_REQUEST_ID);

		fireOnSignIn();
	}

	private void onSignOut() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onSignOut");
		}

		fireOnSignOut();
	}

	private void onValidateConfiguration() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onValidateConfiguration");
		}

		fireOnReady();
	}

	public void onError(long requestId, ErrorCode errorCode,
			String errorParameter) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onError: requestId=" + requestId + " errorCode="
					+ errorCode + " errorParameter=" + errorParameter);
		}

		fireOnError(requestId, errorCode, errorParameter);
	}

	public void onGetTwincodeInbound(long requestId,
			TwincodeInbound twincodeInbound) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onGetTwincodeInbound: requestId=" + requestId
					+ " twincodeInbound=" + twincodeInbound);
		}

		fireOnGetTwincodeInbound(requestId, twincodeInbound);
	}

	public void onGetTwincodeOutbound(long requestId,
			TwincodeOutbound twincodeOutbound) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onGetTwincodeOutbound: requestId=" + requestId
					+ " twincodeOutbound=" + twincodeOutbound);
		}

		fireOnGetTwincodeOutbound(requestId, twincodeOutbound);
	}

	//
	// Fire Events
	//

	private void fireOnTwinlifeReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnTwinlifeReady");
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onTwinlifeReady();
				}
			});
		}
	}

	private void fireOnReady() {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnReady");
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onReady();
				}
			});
		}
	}

	private void fireOnConnect() {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnConnect");
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onConnect();
				}
			});
		}
	}

	private void fireOnDisconnect() {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnDisconnect");
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onDisconnect();
				}
			});
		}
	}

	private void fireOnSignIn() {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnSignIn");
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onSignIn();
				}
			});
		}
	}

	private void fireOnSignOut() {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnSignOut");
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onSignOut();
				}
			});
		}
	}

	public void fireOnError(final long requestId, final ErrorCode errorCode,
			final String errorParameter) {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnError: requestId=" + requestId
					+ " errorCode=" + errorCode + " errorParameter="
					+ errorParameter);
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onError(requestId, errorCode, errorParameter);
				}
			});
		}
	}

	public void fireOnGetTwincodeInbound(final long requestId,
			final TwincodeInbound twincodeInbound) {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnGetTwincodeInbound: requestId=" + requestId
					+ " twincodeInbound=" + twincodeInbound);
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onGetTwincodeInbound(requestId, twincodeInbound);
				}
			});
		}
	}

	public void fireOnGetTwincodeOutbound(final long requestId,
			final TwincodeOutbound twincodeOutbound) {
		if (DEBUG) {
			Log.d(LOG_TAG, "fireOnGetTwincodeInbound: requestId=" + requestId
					+ " twincodeOutbound=" + twincodeOutbound);
		}

		for (final Observer observer : mObservers) {
			getObserverExecutor().execute(new Runnable() {

				@Override
				public void run() {
					observer.onGetTwincodeOutbound(requestId, twincodeOutbound);
				}
			});
		}
	}
}
