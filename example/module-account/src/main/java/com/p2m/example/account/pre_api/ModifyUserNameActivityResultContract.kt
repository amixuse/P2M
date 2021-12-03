package com.p2m.example.account.pre_api

import android.content.Intent
import com.p2m.annotation.module.api.ApiLauncherActivityResultContractFor
import com.p2m.core.launcher.ActivityResultContractP2MCompact

@ApiLauncherActivityResultContractFor("ModifyAccountName")
class ModifyUserNameActivityResultContract: ActivityResultContractP2MCompact<Unit?, String>() {
    override fun inputIntoCreatedIntent(input: Unit?, intent: Intent) {
        // NOTHING
    }

    override fun outputFromResultIntent(resultCode: Int, intent: Intent?): String? {
        return intent?.getStringExtra("result_new_user_name")
    }
}