// Automatically generated file by P2M. DO NOT MODIFY
package com.p2m.example.external.im.p2m.impl

import com.p2m.core.module.ModuleInit
import com.p2m.example.external.im.p2m.api.IM
import com.p2m.example.external.im.p2m.api.NoneModuleApi

public class _IM : IM() {
  public override val api: NoneModuleApi by lazy { _NoneModuleApi() }

  protected override val `init`: ModuleInit by lazy {
      _IMModuleInit() }
}
