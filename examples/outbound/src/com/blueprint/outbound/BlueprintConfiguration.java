/*
 *  Copyright (c) 2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 */

package com.blueprint.outbound;

import org.twinlife.device.android.twinlife.AccountService.AccountServiceConfiguration;
import org.twinlife.device.android.twinlife.AccountService.AuthenticationAuthority;
import org.twinlife.device.android.twinlife.BaseService.BaseServiceId;
import org.twinlife.device.android.twinlife.PeerConnectionService.PeerConnectionServiceConfiguration;
import org.twinlife.device.android.twinlife.Twinlife.TwinlifeConfiguration;

import android.graphics.BitmapFactory;

public class BlueprintConfiguration extends TwinlifeConfiguration {

	private static final String SERVICE_ID = "14f65898-fc2d-4a84-a446-a8f7e15094a4";
	private static final String APPLICATION_ID = "f2081245-0fed-4255-b10f-2418107ccb06";

	BlueprintConfiguration(TwinlifeApplicationImpl twinlifeApplication) {

		serviceId = SERVICE_ID;
		applicationId = APPLICATION_ID;

		isRestartable = false;

		// AccountService Configuration
		AccountServiceConfiguration accountServiceConfiguration = (AccountServiceConfiguration) baseServiceConfigurations[BaseServiceId.ACCOUNT_SERVICE_ID
				.ordinal()];
		accountServiceConfiguration.anonymousAvatar = BitmapFactory
				.decodeResource(twinlifeApplication.getResources(),
						R.drawable.anonymous);
		accountServiceConfiguration.defaultAuthenticationAuthority = AuthenticationAuthority.TWINLIFE;

		// PeerConnectionService Configuration
		PeerConnectionServiceConfiguration peerConnectionServiceConfiguration = (PeerConnectionServiceConfiguration) baseServiceConfigurations[BaseServiceId.PEER_CONNECTION_SERVICE_ID
				.ordinal()];
		peerConnectionServiceConfiguration.serviceOn = true;
		peerConnectionServiceConfiguration.acceptIncomingCalls = false;

		// TwincodeOutboundService Configuration
		baseServiceConfigurations[BaseServiceId.TWINCODE_OUTBOUND_SERVICE_ID
				.ordinal()].serviceOn = true;
	}
}
