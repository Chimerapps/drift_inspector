package com.chimerapps.driftinspector.ui.util.file

import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * @author Nicola Verbeeck
 */
@Suppress("UNUSED_PARAMETER")
fun chooseSaveFile(title: String, extension: String): File? {
    val descriptor = FileSaverDescriptor(title, "", extension)
    val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
    val result = dialog.save(null as VirtualFile?, null)

    return result?.file
}
