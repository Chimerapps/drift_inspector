package com.chimerapps.driftinspector.ui

import com.chimerapps.discovery.utils.LoggerFactory
import com.chimerapps.driftinspector.ui.util.IdeaLoggerFactory
import com.chimerapps.driftinspector.ui.util.NotificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class InspectorToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (LoggerFactory.instance == null) {
            LoggerFactory.instance = IdeaLoggerFactory()
        }

        val contentService = ContentFactory.SERVICE.getInstance()

        val window = InspectorToolWindow(project, toolWindow.contentManager)

        val content = contentService.createContent(window, "", true)
        toolWindow.contentManager.addContent(content)

        NotificationUtil.infoWithHTML("Drift inspector is going away",
        "Drift inspector is being deprecated :(\n\n" +
                "But! Rejoice! It is being replaced with a more powerful system and plugin called local storage inspector. " +
                "Read more about it <a href=\"https://github.com/Chimerapps/drift_inspector/wiki/Deprecation\">here</a>", project)
    }

}