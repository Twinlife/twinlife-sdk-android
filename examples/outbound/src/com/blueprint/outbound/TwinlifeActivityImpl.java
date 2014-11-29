/*
 *  Copyright (c) 2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 */

package com.blueprint.outbound;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class TwinlifeActivityImpl extends Activity implements TwinlifeActivity {
	private static final String LOG_TAG = "TwinlifeActivityImpl";
	private static final boolean DEBUG = false;

	private TwinlifeApplicationImpl mTwinlifeApplicationImpl;

	private Object mDialogLock = new Object();
	private Toast mToast;
	private AlertDialog mAlertDialog;

	//
	// Override Activity methods
	//

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onCreate: savedInstanceState=" + savedInstanceState);
		}

		super.onCreate(savedInstanceState);

		mTwinlifeApplicationImpl = (TwinlifeApplicationImpl) getApplication();
	}

	@Override
	protected void onPause() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onPause");
		}

		super.onPause();

		synchronized (mDialogLock) {
			if (mToast != null) {
				mToast.cancel();
				mToast = null;
			}

			if (mAlertDialog != null) {
				mAlertDialog.dismiss();
				mAlertDialog = null;
			}
		}
	}

	@Override
	protected void onResume() {
		if (DEBUG) {
			Log.d(LOG_TAG, "onResume");
		}

		super.onResume();

		if (!mTwinlifeApplicationImpl.isRunning()) {
			finish();

			return;
		}
	}

	//
	// Implementation of TwinlifeActivity interface
	//

	@Override
	public final TwinlifeApplication getTwinlifeApplication() {
		if (DEBUG) {
			Log.d(LOG_TAG, "getTwinlifeApplication");
		}

		return mTwinlifeApplicationImpl;
	}

	@Override
	public final boolean hasTwinlife() {
		if (DEBUG) {
			Log.d(LOG_TAG, "hasTwinlife");
		}

		return mTwinlifeApplicationImpl.hasTwinlife();
	}

	@Override
	public final boolean isConfigured() {
		if (DEBUG) {
			Log.d(LOG_TAG, "isConfigured");
		}

		return mTwinlifeApplicationImpl.isConfigured();
	}

	@Override
	public long newRequestId() {
		if (DEBUG) {
			Log.d(LOG_TAG, "newRequestId");
		}

		return mTwinlifeApplicationImpl.newRequestId();
	}

	@Override
	public void toast(String text) {
		if (DEBUG) {
			Log.d(LOG_TAG, "toast: text=" + text);
		}

		synchronized (mDialogLock) {
			if (mToast != null) {
				mToast.cancel();
			}

			mToast = Toast.makeText(getApplicationContext(), text,
					Toast.LENGTH_LONG);
			mToast.setGravity(Gravity.BOTTOM, 0, 0);
			mToast.show();
		}
	}

	@Override
	public void message(String message, final MessageCallback messageCallback) {
		if (DEBUG) {
			Log.d(LOG_TAG, "message: message=" + message + "messageCallback="
					+ messageCallback);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Html.fromHtml(message));
		builder.setCancelable(false);

		builder.setPositiveButton(messageCallback.getPositiveButtonId(),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						messageCallback.onClick(dialog);
					}
				});

		synchronized (mDialogLock) {
			if (mAlertDialog != null) {
				mAlertDialog.dismiss();
			}

			mAlertDialog = builder.create();
			mAlertDialog.show();
		}
	}

	@Override
	public void error(String message, final ErrorCallback errorCallback) {
		if (DEBUG) {
			Log.d(LOG_TAG, "error: message=" + message + "errorCallback="
					+ errorCallback);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Html.fromHtml(message));
		builder.setCancelable(false);

		builder.setPositiveButton(errorCallback.getPositiveButtonId(),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						errorCallback.onClick(dialog);
					}
				});

		synchronized (mDialogLock) {
			if (mAlertDialog != null) {
				mAlertDialog.dismiss();
			}

			mAlertDialog = builder.create();
			mAlertDialog.show();
		}
	}

	@Override
	public final void fatalError(String message,
			final ErrorCallback errorCallback) {
		if (DEBUG) {
			Log.d(LOG_TAG, "fatalError: message=" + message + "errorCallback="
					+ errorCallback);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Html.fromHtml(message));
		builder.setCancelable(false);

		builder.setPositiveButton(errorCallback.getPositiveButtonId(),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						errorCallback.onClick(dialog);
					}
				});

		synchronized (mDialogLock) {
			if (mAlertDialog != null) {
				mAlertDialog.dismiss();
			}

			mAlertDialog = builder.create();
			mAlertDialog.show();
		}
	}
}
