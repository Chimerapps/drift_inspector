package com.chimerapps.driftinspector.ui

import com.chimerapps.discovery.utils.LoggerFactory
import com.chimerapps.driftinspector.ui.util.IdeaLoggerFactory
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
    }

}