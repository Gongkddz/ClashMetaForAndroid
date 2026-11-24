package com.github.kr328.clash.design

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.ScrollView
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import android.view.View
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.databinding.DesignSettingsOverideBinding
import com.github.kr328.clash.design.databinding.DialogPreferenceListBinding
import com.github.kr328.clash.design.dialog.FullScreenDialog
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class OverrideSettingsDesign(
    context: Context,
    private val configuration: ConfigurationOverride
) : Design<OverrideSettingsDesign.Request>(context) {
    enum class Request {
        ResetOverride, ImportRoutingOverride
    }

    private val binding = DesignSettingsOverideBinding
        .inflate(context.layoutInflater, context.root, false)

    private lateinit var routingPreference: ClickablePreference

    override val root: View
        get() = binding.root

    suspend fun requestResetConfirm(): Boolean {
        return suspendCancellableCoroutine { ctx ->
            val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.reset_override_settings)
                .setMessage(R.string.reset_override_settings_message)
                .setPositiveButton(R.string.ok) { _, _ -> ctx.resume(true) }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()

            dialog.setOnDismissListener {
                if (!ctx.isCompleted)
                    ctx.resume(false)
            }

            ctx.invokeOnCancellation {
                dialog.dismiss()
            }
        }
    }

    init {
        binding.self = this

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        val booleanValues: Array<Boolean?> = arrayOf(
            null,
            true,
            false
        )
        val booleanValuesText: Array<Int> = arrayOf(
            R.string.dont_modify,
            R.string.enabled,
            R.string.disabled
        )

        val screen = preferenceScreen(context) {
            routingPreference = clickable(R.string.routing_override) {
                clicked { editRoutingOverride() }
            }
            updateRoutingSummary()
            clickable(R.string.routing_override_import) {
                clicked { requests.trySend(Request.ImportRoutingOverride) }
            }

            category(R.string.general)

            editableText(
                value = configuration::httpPort,
                adapter = NullableTextAdapter.Port,
                title = R.string.http_port,
                placeholder = R.string.dont_modify,
                empty = R.string.disabled,
            )

            editableText(
                value = configuration::socksPort,
                adapter = NullableTextAdapter.Port,
                title = R.string.socks_port,
                placeholder = R.string.dont_modify,
                empty = R.string.disabled,
            )

            editableText(
                value = configuration::redirectPort,
                adapter = NullableTextAdapter.Port,
                title = R.string.redirect_port,
                placeholder = R.string.dont_modify,
                empty = R.string.disabled,
            )

            editableText(
                value = configuration::tproxyPort,
                adapter = NullableTextAdapter.Port,
                title = R.string.tproxy_port,
                placeholder = R.string.dont_modify,
                empty = R.string.disabled,
            )

            editableText(
                value = configuration::mixedPort,
                adapter = NullableTextAdapter.Port,
                title = R.string.mixed_port,
                placeholder = R.string.dont_modify,
                empty = R.string.disabled,
            )

            editableTextList(
                value = configuration::authentication,
                adapter = TextAdapter.String,
                title = R.string.authentication,
                placeholder = R.string.dont_modify,
            )

            selectableList(
                value = configuration::allowLan,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.allow_lan,
            )

            selectableList(
                value = configuration::ipv6,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.ipv6,
            )

            editableText(
                value = configuration::bindAddress,
                adapter = NullableTextAdapter.String,
                title = R.string.bind_address,
                placeholder = R.string.dont_modify,
                empty = R.string.default_
            )

            editableText(
                value = configuration::externalController,
                adapter = NullableTextAdapter.String,
                title = R.string.external_controller,
                placeholder = R.string.dont_modify,
                empty = R.string.default_
            )

            editableText(
                value = configuration::externalControllerTLS,
                adapter = NullableTextAdapter.String,
                title = R.string.external_controller_tls,
                placeholder = R.string.dont_modify,
                empty = R.string.default_
            )

            editableTextList(
                value = configuration.externalControllerCors::allowOrigins,
                adapter = TextAdapter.String,
                title = R.string.allow_origins,
                placeholder = R.string.dont_modify,
            )

            selectableList(
                value = configuration.externalControllerCors::allowPrivateNetwork,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.allow_private_network,
            )

            editableText(
                value = configuration::secret,
                adapter = NullableTextAdapter.String,
                title = R.string.secret,
                placeholder = R.string.dont_modify,
                empty = R.string.default_
            )

            selectableList(
                value = configuration::mode,
                values = arrayOf(
                    null,
                    TunnelState.Mode.Direct,
                    TunnelState.Mode.Global,
                    TunnelState.Mode.Rule
                ),
                valuesText = arrayOf(
                    R.string.dont_modify,
                    R.string.direct_mode,
                    R.string.global_mode,
                    R.string.rule_mode
                ),
                title = R.string.mode
            )

            selectableList(
                value = configuration::logLevel,
                values = arrayOf(
                    null,
                    LogMessage.Level.Info,
                    LogMessage.Level.Warning,
                    LogMessage.Level.Error,
                    LogMessage.Level.Debug,
                    LogMessage.Level.Silent,
                ),
                valuesText = arrayOf(
                    R.string.dont_modify,
                    R.string.info,
                    R.string.warning,
                    R.string.error,
                    R.string.debug,
                    R.string.silent,
                ),
                title = R.string.log_level,
            )

            editableTextMap(
                value = configuration::hosts,
                keyAdapter = TextAdapter.String,
                valueAdapter = TextAdapter.String,
                title = R.string.hosts,
                placeholder = R.string.dont_modify,
            )

            category(R.string.dns)

            val dnsDependencies: MutableList<Preference> = mutableListOf()

            val dns = selectableList(
                value = configuration.dns::enable,
                values = arrayOf(
                    null,
                    true,
                    false
                ),
                valuesText = arrayOf(
                    R.string.dont_modify,
                    R.string.force_enable,
                    R.string.use_built_in,
                ),
                title = R.string.strategy
            ) {
                listener = OnChangedListener {
                    if (configuration.dns.enable == false) {
                        dnsDependencies.forEach {
                            it.enabled = false
                        }
                    } else {
                        dnsDependencies.forEach {
                            it.enabled = true
                        }
                    }
                }
            }

            selectableList(
                value = configuration.dns::preferH3,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.prefer_h3,
                configure = dnsDependencies::add,
            )

            editableText(
                value = configuration.dns::listen,
                adapter = NullableTextAdapter.String,
                title = R.string.listen,
                placeholder = R.string.dont_modify,
                empty = R.string.disabled,
                configure = dnsDependencies::add,
            )

            selectableList(
                value = configuration.app::appendSystemDns,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.append_system_dns,
                configure = dnsDependencies::add,
            )

            selectableList(
                value = configuration.dns::ipv6,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.ipv6,
                configure = dnsDependencies::add,
            )

            selectableList(
                value = configuration.dns::useHosts,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.use_hosts,
                configure = dnsDependencies::add,
            )

            selectableList(
                value = configuration.dns::enhancedMode,
                values = arrayOf(
                    null,
                    ConfigurationOverride.DnsEnhancedMode.None,
                    ConfigurationOverride.DnsEnhancedMode.FakeIp,
                    ConfigurationOverride.DnsEnhancedMode.Mapping
                ),
                valuesText = arrayOf(
                    R.string.dont_modify,
                    R.string.disabled,
                    R.string.fakeip,
                    R.string.mapping
                ),
                title = R.string.enhanced_mode,
                configure = dnsDependencies::add,
            )

            editableTextList(
                value = configuration.dns::nameServer,
                adapter = TextAdapter.String,
                title = R.string.name_server,
                placeholder = R.string.dont_modify,
                configure = dnsDependencies::add,
            )

            editableTextList(
                value = configuration.dns::fallback,
                adapter = TextAdapter.String,
                title = R.string.fallback,
                placeholder = R.string.dont_modify,
                configure = dnsDependencies::add,
            )

            editableTextList(
                value = configuration.dns::defaultServer,
                adapter = TextAdapter.String,
                title = R.string.default_name_server,
                placeholder = R.string.dont_modify,
                configure = dnsDependencies::add,
            )

            editableTextList(
                value = configuration.dns::fakeIpFilter,
                adapter = TextAdapter.String,
                title = R.string.fakeip_filter,
                placeholder = R.string.dont_modify,
                configure = dnsDependencies::add,
            )

            selectableList(
                value = configuration.dns::fakeIPFilterMode,
                values = arrayOf(
                    null,
                    ConfigurationOverride.FilterMode.BlackList,
                    ConfigurationOverride.FilterMode.WhiteList,
                    ConfigurationOverride.FilterMode.Rule
                ),
                valuesText = arrayOf(
                    R.string.dont_modify,
                    R.string.blacklist,
                    R.string.whitelist,
                    R.string.rule
                ),
                title = R.string.fakeip_filter_mode,
                configure = dnsDependencies::add,
            )

            selectableList(
                value = configuration.dns.fallbackFilter::geoIp,
                values = booleanValues,
                valuesText = booleanValuesText,
                title = R.string.geoip_fallback,
                configure = dnsDependencies::add,
            )

            editableText(
                value = configuration.dns.fallbackFilter::geoIpCode,
                adapter = NullableTextAdapter.String,
                title = R.string.geoip_fallback_code,
                placeholder = R.string.dont_modify,
                empty = R.string.raw_cn,
                configure = dnsDependencies::add,
            )

            editableTextList(
                value = configuration.dns.fallbackFilter::domain,
                adapter = TextAdapter.String,
                title = R.string.domain_fallback,
                placeholder = R.string.dont_modify,
                configure = dnsDependencies::add,
            )

            editableTextList(
                value = configuration.dns.fallbackFilter::ipcidr,
                adapter = TextAdapter.String,
                title = R.string.ipcidr_fallback,
                placeholder = R.string.dont_modify,
                configure = dnsDependencies::add,
            )

            editableTextMap(
                value = configuration.dns::nameserverPolicy,
                keyAdapter = TextAdapter.String,
                valueAdapter = TextAdapter.String,
                title = R.string.name_server_policy,
                placeholder = R.string.dont_modify,
                configure = dnsDependencies::add,
            )

            dns.listener?.onChanged()
        }

        binding.content.addView(screen.root)
    }

    private fun updateRoutingSummary() {
        routingPreference.summary = context.getText(
            if (configuration.routingOverride == null) R.string.dont_modify
            else R.string.routing_override_active
        )
    }

    fun editRoutingOverride(initial: String? = configuration.routingOverride) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 4
            maxLines = 10
            hint = "rules:\n  - MATCH,DIRECT"
            setText(initial.orEmpty())
        }
        val view = ScrollView(context).apply { addView(input) }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.routing_override)
            .setMessage(R.string.routing_override_help)
            .setView(view)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.routing_override_disable) { _, _ ->
                configuration.routingOverride = null
                updateRoutingSummary()
            }
            .show()

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            try {
                configuration.routingOverride = parseRoutingOverride(input.text.toString())
                updateRoutingSummary()
                dialog.dismiss()
            } catch (_: Exception) {
                input.error = context.getString(R.string.routing_override_invalid)
            }
        }
    }

    fun showRoutingImportError() {
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.routing_override_invalid)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    companion object {
        const val ROUTING_YAML_LIMIT = 512 * 1024

        fun parseRoutingOverride(source: String): String {
            require(source.length <= ROUTING_YAML_LIMIT)
            val loader = LoaderOptions().apply {
                codePointLimit = ROUTING_YAML_LIMIT
                isAllowDuplicateKeys = false
            }
            val document = Yaml(SafeConstructor(loader)).load<Any>(source) as? Map<*, *>
                ?: throw IllegalArgumentException()
            val fields = setOf("rules", "rule-providers", "sub-rules")
            val routing = document.filterKeys { it in fields }
            require(routing.isNotEmpty())
            for ((key, value) in routing) {
                when (key) {
                    "rules" -> require(isRuleList(value))
                    "rule-providers" -> require(value is Map<*, *> &&
                        value.all { (name, provider) -> name is String && provider is Map<*, *> })
                    "sub-rules" -> require(value is Map<*, *> &&
                        value.all { (name, rules) -> name is String && isRuleList(rules) })
                }
            }
            val dumper = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
            return Yaml(dumper).dump(routing).also { require(it.length <= ROUTING_YAML_LIMIT) }
        }

        private fun isRuleList(value: Any?): Boolean =
            value is List<*> && value.all { it is String && it.isNotBlank() }
    }

    fun requestClear() {
        requests.trySend(Request.ResetOverride)
    }
}
