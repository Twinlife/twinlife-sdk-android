/*
 *  Copyright (c) 2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 */

package com.blueprint.inbound;

import java.util.UUID;
import java.util.concurrent.Executor;

import org.twinlife.device.android.twinlife.BaseService.ErrorCode;
import org.twinlife.device.android.twinlife.PeerConnectionService;
import org.twinlife.device.android.twinlife.TwincodeInboundService.TwincodeInbound;
import org.twinlife.device.android.twinlife.TwincodeOutboundService.TwincodeOutbound;
import org.twinlife.device.android.twinlife.Twinlife.TwinlifeConfiguration;
import org.twinlife.device.android.twinlife.TwinlifeException;

public interface TwinlifeApplication {

	interface Observer {

		void onError(long requestId, ErrorCode errorCode, String errorParameter);

		void onTwinlifeReady();

		void onReady();

		//
		// ConnectivityService Events
		//

		void onConnect();

		void onDisconnect();

		//
		// AccountService Events
		//

		void onSignIn();

		void onSignOut();

		//
		// TwincodeInboundService Events
		//

		void onGetTwincodeInbound(long requestId,
				TwincodeInbound twincodeInbound);

		//
		// TwincodeOutboundService Events
		//

		void onGetTwincodeOutbound(long requestId,
				TwincodeOutbound twincodeOutbound);
	};

	class DefaultObserver implements Observer {

		@Override
		public void onError(long requestId, ErrorCode errorCode,
				String errorParameter) {
		}

		@Override
		public void onTwinlifeReady() {
		}

		@Override
		public void onReady() {
		}

		@Override
		public void onConnect() {
		}

		@Override
		public void onDisconnect() {
		}

		@Override
		public void onSignIn() {
		}

		@Override
		public void onSignOut() {
		}

		@Override
		public void onGetTwincodeInbound(long requestId,
				TwincodeInbound twincodeInbound) {
		}

		@Override
		public void onGetTwincodeOutbound(long requestId,
				TwincodeOutbound twincodeOutbound) {
		}
	};

	boolean isRunning();

	void stop();

	void restart();

	boolean hasTwinlife();

	boolean isConfigured();

	boolean configure(TwinlifeConfiguration twinlifeConfiguration);

	TwinlifeConfiguration getTwinlifeConfiguration();

	long newRequestId();

	//
	// Observer Management
	//

	Executor getObserverExecutor();

	void setObserver(Observer observer);

	void removeObserver(Observer observer);

	//
	// AccountService
	//

	boolean isSignIn();

	void signIn(String username, String password, boolean rememberPassword)
			throws TwinlifeException;

	//
	// TwincodeInboundService
	//

	void getTwincodeInbound(long requestId, UUID twincodeInboundId);

	//
	// TwincodeOutboundService
	//

	void getTwincodeOutbound(long requestId, UUID twincodeOutboundId);

	String getPeerId(UUID twincodeOutboundId);

	//
	// PeerConnectionService
	//

	PeerConnectionService getPeerConnectionService();
}
