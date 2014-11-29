/*
 *  Copyright (c) 2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 */

package com.blueprint.outbound;

import android.content.DialogInterface;

public interface TwinlifeActivity {

	interface MessageCallback {

		int getPositiveButtonId();

		void onClick(DialogInterface dialog);
	}

	class DefaultMessageCallback implements MessageCallback {

		int positiveButtonId;

		public DefaultMessageCallback() {
			this.positiveButtonId = R.string.twinlife_activity_ok_button_label;
		}

		@Override
		public int getPositiveButtonId() {

			return positiveButtonId;
		}

		@Override
		public void onClick(DialogInterface dialog) {
		}
	}

	interface ErrorCallback {

		int getPositiveButtonId();

		void onClick(DialogInterface dialog);
	}

	class DefaultErrorCallback implements ErrorCallback {

		int positiveButtonId;

		public DefaultErrorCallback() {
			this.positiveButtonId = R.string.twinlife_activity_ok_button_label;
		}

		@Override
		public int getPositiveButtonId() {
			return positiveButtonId;
		}

		@Override
		public void onClick(DialogInterface dialog) {
		}
	}

	TwinlifeApplication getTwinlifeApplication();

	boolean hasTwinlife();

	boolean isConfigured();

	long newRequestId();

	void toast(String message);

	void message(String message, MessageCallback messageCallback);

	void error(String message, ErrorCallback errorCallback);

	void fatalError(String message, ErrorCallback errorCallback);
}
