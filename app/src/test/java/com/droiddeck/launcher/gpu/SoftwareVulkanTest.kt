package com.droiddeck.launcher.gpu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SoftwareVulkanTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun lavapipeCountsOnlyWithItsLibrary() {
        val root = tmp.newFolder("rootfs")
        assertNull(SoftwareVulkan.icd(root))
        val icd = File(root, SoftwareVulkan.ICD).apply { parentFile!!.mkdirs(); writeText("{}") }
        // A manifest whose library never arrived would leave the session with no Vulkan at all.
        assertNull(SoftwareVulkan.icd(root))
        File(root, "usr/lib/libvulkan_lvp.so").apply { parentFile!!.mkdirs(); writeText("") }
        assertEquals(icd, SoftwareVulkan.icd(root))
    }
}
