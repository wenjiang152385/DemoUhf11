package com.senter.demo.uhf.modelD2;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.senter.demo.uhf.App;
import com.senter.demo.uhf.R;
import com.senter.support.openapi.StUhf;
import com.senter.support.openapi.StUhf.InterrogatorModelDs.UmdErrorCode;
import com.senter.support.openapi.StUhf.InterrogatorModelDs.UmdFrequencyPoint;
import com.senter.support.openapi.StUhf.InterrogatorModelDs.UmdOnGetInventoryBuffer;
import com.senter.support.openapi.StUhf.InterrogatorModelDs.UmdOnIso18k6cInventory;
import com.senter.support.openapi.StUhf.InterrogatorModelDs.UmdOnIso18k6cRealTimeInventory;
import com.senter.support.openapi.StUhf.InterrogatorModelDs.UmdRssi;
import com.senter.support.openapi.StUhf.UII;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Activity0Inventory extends com.senter.demo.uhf.common.Activity0InventoryCommonAbstract
{

	/**
	 * if the stop operation is caused by back button
	 */
	private boolean			finishOnStop		= false;

	private ProgressDialog	waitingDialog	= null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getViews().setModes(InventoryMode.Custom);
	}

	@Override
	protected void uiOnInverntryButton()
	{
		if (worker.isInventroing())
		{
			worker.stopInventory();
		} else
		{
			viewsSetAsStarted();
			worker.startInventory();
		}
	}

	@Override
	public boolean onKeyDown(	int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_BACK:
			{
				if (event.getAction()==KeyEvent.ACTION_DOWN)
				{
					if (worker.isInventroing())
					{
						finishOnStop = true;
						waitDialogShow();
						worker.stopInventory();
					}else {
						App.clearMaskSettings();
						waitDialogDismiss();
						finish();
					}
					return true;
				}
				break;
			}
			default:
				break;
		}

		return super.onKeyDown(keyCode, event);
	}

	private final ContinuousInventoryListener	workerLisener=new ContinuousInventoryListener()
	{

		@Override
		public void onTagInventory(	UII uii, UmdFrequencyPoint frequencyPoint, Integer antennaId, UmdRssi rssi)
		{
			Log.e("jw","uii=="+uii);
			addNewUiiMassageToListview(uii);
		}
		
		@Override
		public void onFinished()
		{
			if (finishOnStop)
			{
				App.clearMaskSettings();
				waitDialogDismiss();
				finish();
			} else
			{
				viewsSetAsStoped();
			}
		}
	};
	private final ContinuousInventoryWorker	worker = new ContinuousInventoryWorker(workerLisener);

	/**
	 * show waiting dialog
	 */
	private void waitDialogShow()
	{
		if (waitingDialog != null && waitingDialog.isShowing())
		{
			return;
		}

		waitingDialog = new ProgressDialog(mActivityAbstract);
		waitingDialog.setCancelable(false);
		waitingDialog.setMessage(getResources().getText(R.string.strWait4Close));
		waitingDialog.show();
	}

	/**
	 * dismiss waiting diaglog
	 */
	private void waitDialogDismiss()
	{

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (waitingDialog != null && waitingDialog.isShowing())
				{
					waitingDialog.dismiss();
				}
			}
		});

	}

	private static class ContinuousInventoryWorker
	{
		/**
		 * go on inventoring after one inventory cycle finished.
		 */
		private boolean					goOnInventoring	= true;

		private ContinuousInventoryListener	mListener	= null;

		private boolean			isInventoring		= false;

		/**
		 * 
		 * @param listener
		 *            must no be null
		 */
		public ContinuousInventoryWorker(ContinuousInventoryListener listener) {
			if (listener == null)
			{
				throw new NullPointerException();
			}
			mListener = listener;
		}

		public void startInventory()
		{
			goOnInventoring = true;
			isInventoring=true;

			App.uhfInterfaceAsModelD2().iso18k6cRealTimeInventory(1, new UmdOnIso18k6cRealTimeInventory()
			{

				@Override
				public void onFinishedWithError(UmdErrorCode error)
				{
					onFinishedOnce();
				}

				@Override
				public void onFinishedSuccessfully(	Integer antennaId, int readRate, int totalRead)
				{
					onFinishedOnce();
				}
				
				private void onFinishedOnce()
				{
					if (goOnInventoring)
					{
						startInventory();
					} else
					{
						isInventoring=false;
						mListener.onFinished();
					}
				}

				@Override
				public void onTagInventory(	UII uii, UmdFrequencyPoint frequencyPoint, Integer antennaId, UmdRssi rssi)
				{
					mListener.onTagInventory(uii, frequencyPoint, antennaId, rssi);
				}
			});
		}

		public void stopInventory()
		{
			goOnInventoring = false;
		}

		public boolean isInventroing()
		{
			return isInventoring;
		}
	}

	private interface ContinuousInventoryListener
	{
		/**
		 * will be called on finished completely
		 */
		public void onFinished();

		public void onTagInventory(	UII uii, UmdFrequencyPoint frequencyPoint, Integer antennaId, UmdRssi rssi);
	}
	
	
	
	
	
	/**
	 * start a inventory process with tag buffer,the result is tags inventoried。
	 */
	private ArrayList<UII> startInventoryWithBuffer() throws InterruptedException{
		final ArrayList<UII> ret=new ArrayList<StUhf.UII>();
		
		final AtomicBoolean inventoryFinished=new AtomicBoolean(false);
		final AtomicBoolean inventoryBufferFinished=new AtomicBoolean(false);
		
		UmdOnIso18k6cInventory inventoryLisener=new UmdOnIso18k6cInventory() {
			//inventory complete
			@Override
			public void onSuccessed(Integer antennaId, final int tagCountResult, int readRate, long totalRead) {
				Log.v("", "iso18k6cInventory:onSuccessed antennaId:"+antennaId+" tagCountResult:"+tagCountResult+" readRate:"+readRate+" totalRead:"+totalRead);
				if (tagCountResult!=0) {
					new Thread(){
						public void run() {
							App.uhfInterfaceAsModelD2().inventoryBufferGet(new UmdOnGetInventoryBuffer() {
								@Override
								public void onTagReceived(int tagCount, UII uii, UmdRssi rssi, UmdFrequencyPoint frequencyPoint, Integer antennaId,int InventoriedCount) {
									Log.v("", "iso18k6cInventory:inventoryBufferGet:onTagReceived tagCount:"+tagCount+" uii:"+uii+" InventoriedCount:"+InventoriedCount);
									ret.add(uii);
									if (ret.size()==tagCountResult) {
										notifyInventoryTagGettingFinished();
									}
								}
								
								@Override
								public void onFailed(UmdErrorCode errorCode) {
									Log.v("", "iso18k6cInventory:inventoryBufferGet:onFailed errorCode:"+errorCode);
									notifyInventoryTagGettingFinished();
								}
							});
							Log.v("", "iso18k6cInventory:inventoryBufferGet:begain");
						};
					}.start();
				}else {
					Log.v("", "iso18k6cInventory:inventoryBufferGet:no tag inventoried");
					notifyInventoryTagGettingFinished();
				}
				notifyInventoryFinished();
			}
			
			//inventory failed
			@Override
			public void onFailed(UmdErrorCode errorCode) {
				Log.v("", "iso18k6cInventory:onFailed errorCode:"+errorCode);
				notifyInventoryFinished();
			}
			void notifyInventoryFinished()
			{
				synchronized (inventoryFinished) {
					if (inventoryFinished.compareAndSet(false, true)==false) {
						throw new AssertionError();
					}
					inventoryFinished.set(true);
					inventoryFinished.notifyAll();
				}
			}
			void notifyInventoryTagGettingFinished()
			{
				synchronized (inventoryBufferFinished) {
					if (inventoryBufferFinished.compareAndSet(false, true)==false) {
						throw new AssertionError();
					}
					inventoryBufferFinished.set(true);
					inventoryBufferFinished.notifyAll();
				}
			}
		};
		
		Log.v("", "iso18k6cInventory:begain");
		App.uhfInterfaceAsModelD2().iso18k6cInventory(inventoryLisener);
		Log.v("", "iso18k6cInventory:waiting");
		
		for (;;) {
			synchronized (inventoryFinished) {
				if (inventoryFinished.get()) {
					break;
				}
				inventoryFinished.wait();
			}
		}
		Log.v("", "iso18k6cInventory:inventory:finished");
		for (;;) {
			synchronized (inventoryBufferFinished) {
				if (inventoryBufferFinished.get()) {
					break;
				}
				inventoryBufferFinished.wait();
			}
		}
		Log.v("", "iso18k6cInventory:get tags in buffer:finished");
		
		return ret;
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
