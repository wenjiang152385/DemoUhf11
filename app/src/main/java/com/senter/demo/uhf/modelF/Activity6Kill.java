package com.senter.demo.uhf.modelF;

import android.widget.Toast;

import com.senter.demo.uhf.App;
import com.senter.demo.uhf.R;
import com.senter.demo.uhf.common.DestinationTagSpecifics;
import com.senter.demo.uhf.common.DestinationTagSpecifics.TargetTagType;
import com.senter.demo.uhf.util.DataTransfer;
import com.senter.support.openapi.StUhf.Function;
import com.senter.support.openapi.StUhf.Result.KillResult;

public final class Activity6Kill extends com.senter.demo.uhf.common.Activity6KillCommonAbstract
{
	protected DestinationTagSpecifics.TargetTagType[] getDestinationType()
	{
		return new DestinationTagSpecifics.TargetTagType[]{TargetTagType.SingleTag,TargetTagType.MatchTag};
	}
	
	@Override
	protected void onKill()
	{
		KillResult rslt = null;
		if (App.uhf().isFunctionSupported(Function.KillSingleTagWithAccessPasswordAndKillPassword))
		{
			rslt = App.uhf().killSingleTagWithAccessPasswordAndKillPassword(getDestinationTagSpecifics().getAccessPassword(), getDestinationTagSpecifics().getKillPassword());
		}

		if (rslt == null || rslt.isSucceeded() == false)
		{
			addNewMassageToListview(getString(R.string.DestructFailure));
		} else
		{
			Toast.makeText(this, getString(R.string.Destruct) + DataTransfer.xGetString(rslt.getUii().getBytes()) + getString(R.string.successful), Toast.LENGTH_SHORT).show();
		}
	}
}
