package com.droiddeck.launcher.gpu

import android.content.Context
import android.util.Log
import com.droiddeck.launcher.core.DeviceSupport
import com.droiddeck.launcher.runtime.GuestCommand
import com.droiddeck.launcher.runtime.LinuxRuntime
import java.io.File

/**
 * The runtime's Vulkan on a GPU its Turnip cannot drive (Mali, Xclipse, PowerVR): Mesa's lavapipe,
 * which draws on the CPU. Turnip only talks to KGSL, so on those phones it finds no device and
 * gamescope exits before Steam starts. With lavapipe the session comes up: the client and light
 * games draw, slowly, and the phone's own Vulkan driver still puts each frame on the screen in the
 * app's compositor (TurnipDriver picks the system driver there).
 *
 * The runtime image does not carry lavapipe, so the first session on such a phone fetches it
 * (droiddeck-swrast-setup, ~10 MB from Arch Linux ARM, signature-checked). A failure is logged
 * and the session starts anyway; the next one tries again.
 */
object SoftwareVulkan {
    private const val TAG = "SoftwareVulkan"
    private const val SETUP = "/usr/local/bin/droiddeck-swrast-setup"
    /** Where vulkan-swrast puts its manifest, as the guest sees it. */
    const val ICD = "usr/share/vulkan/icd.d/lvp_icd.aarch64.json"
    private const val LIB = "usr/lib/libvulkan_lvp.so"

    /** True on a GPU the runtime's Turnip cannot drive. */
    fun needed(): Boolean = !DeviceSupport.adreno()

    /** lavapipe's manifest in [root], when it and its library are both there. */
    fun icd(root: File): File? = File(root, ICD).takeIf { it.isFile && File(root, LIB).isFile }

    /** Fetches lavapipe into the runtime when this phone needs it and it is not there. Blocks; call off the main thread. */
    fun ensure(context: Context): String? {
        if (!needed()) return null
        val root = LinuxRuntime.rootDir(context)
        if (icd(root) != null) return "software vulkan: lavapipe present"
        val status = GuestCommand.run(context, listOf("/bin/bash", SETUP), fakeRoot = true, logName = "swrast-setup") { line ->
            Log.i(TAG, line)
        }
        return if (status == 0 && icd(root) != null) "software vulkan: lavapipe installed"
            else "software vulkan: setup failed (status $status); see logs/tools/swrast-setup.log"
    }
}
