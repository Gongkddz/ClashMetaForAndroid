package com.github.kr328.clash

import androidx.activity.result.contract.ActivityResultContracts
import com.github.kr328.clash.common.compat.getDrawableCompat
import com.github.kr328.clash.common.constants.Metadata
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.design.OverrideSettingsDesign
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.util.toAppInfo
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class OverrideSettingsActivity : BaseActivity<OverrideSettingsDesign>() {
    override suspend fun main() {
        val configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }
        val service = ServiceStore(this)

        defer {
            withClash {
                patchOverride(Clash.OverrideSlot.Persist, configuration)
            }
        }

        val design = OverrideSettingsDesign(
            this,
            configuration
        )

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        OverrideSettingsDesign.Request.ImportRoutingOverride -> {
                            val uri = startActivityForResult(
                                ActivityResultContracts.OpenDocument(), arrayOf("*/*")
                            ) ?: return@onReceive
                            val imported = withContext(Dispatchers.IO) {
                                try {
                                    val source = contentResolver.openInputStream(uri)?.bufferedReader()
                                        ?.use { reader ->
                                            val limit = OverrideSettingsDesign.ROUTING_YAML_LIMIT
                                            val buffer = CharArray(4096)
                                            val text = StringBuilder()
                                            while (true) {
                                                val count = reader.read(buffer)
                                                if (count < 0) break
                                                require(text.length + count <= limit)
                                                text.append(buffer, 0, count)
                                            }
                                            text.toString()
                                        } ?: throw IllegalArgumentException()
                                    OverrideSettingsDesign.parseRoutingOverride(source)
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            if (imported == null) design.showRoutingImportError()
                            else design.editRoutingOverride(imported)
                        }
                        OverrideSettingsDesign.Request.ResetOverride -> {
                            if (design.requestResetConfirm()) {
                                defer {
                                    withClash {
                                        clearOverride(Clash.OverrideSlot.Persist)
                                    }
                                }

                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}