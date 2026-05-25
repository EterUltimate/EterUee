package com.eterultimate.eteruee.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EterUeeSharedTest {
    @Test
    fun supportedAppleTargetsCoverIosIpadosAndMacos() {
        val targets = EterUeeShared.supportedAppleTargets.map { it.kotlinTarget }.toSet()

        assertTrue("iosArm64" in targets)
        assertTrue("iosSimulatorArm64" in targets)
        assertTrue("iosX64" in targets)
        assertTrue("macosArm64" in targets)
    }

    @Test
    fun frameworkNameIsStableForSwiftImport() {
        assertEquals("EterUeeShared", EterUeeShared.frameworkName)
    }
}
