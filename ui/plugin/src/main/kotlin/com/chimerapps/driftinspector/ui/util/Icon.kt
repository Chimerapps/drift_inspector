package com.chimerapps.driftinspector.ui.util

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

private fun Any.loadIcon(path: String): Icon {
    return IconLoader.getIcon(path, javaClass)
}

object IncludedIcons {

    val supportsSvg = ApplicationInfo.getInstance().build.baselineVersion >= 182 //2018.2
    private val isFlat = supportsSvg //also 2018.2
    private val hasSvgExtension = if (supportsSvg) ".svg" else ".png"

    object Status {
        val connected = loadIcon("/ic_connected$hasSvgExtension")
        val disconnected = loadIcon("/ic_disconnected$hasSvgExtension")
    }

}