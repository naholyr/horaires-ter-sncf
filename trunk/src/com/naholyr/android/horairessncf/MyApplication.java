package com.naholyr.android.horairessncf;

import com.ubikod.capptain.android.sdk.CapptainAgent;
import com.ubikod.capptain.android.sdk.CapptainApplication;

public class MyApplication extends CapptainApplication {

	@Override
	protected void onApplicationProcessLowMemory() {
		CapptainAgent.getInstance(this).sendError("low_memory", null);
	}

}
