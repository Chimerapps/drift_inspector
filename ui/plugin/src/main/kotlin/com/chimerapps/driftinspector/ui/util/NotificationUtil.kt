package com.chimerapps.driftinspector.ui.util

import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.notification.*
import com.intellij.openapi.project.Project

object NotificationUtil {

    private const val NOTIFICATION_CHANNEL = "drift_inspector"

    fun info(title: String, message: String, project: Project?) {
        val group = NotificationGroup("${NOTIFICATION_CHANNEL}_info", NotificationDisplayType.BALLOON, true)

        val notification = group.createNotification(
            title,
            message,
            NotificationType.INFORMATION,
            ShowFilePathAction.FILE_SELECTING_LISTENER
        )
        Notifications.Bus.notify(notification, project)
    }

    fun infoWithHTML(title: String, message: String, project: Project?) {
        val group = NotificationGroup("${NOTIFICATION_CHANNEL}_info", NotificationDisplayType.BALLOON, true)

        val notification = group.createNotification(
            title,
            message,
            NotificationType.INFORMATION,
            NotificationListener.UrlOpeningListener(false)
        )
        Notifications.Bus.notify(notification, project)
    }

    fun error(title: String, message: String, project: Project?) {
        val group = NotificationGroup("${NOTIFICATION_CHANNEL}_error", NotificationDisplayType.BALLOON, true)

        val notification = group.createNotification(title, message, NotificationType.ERROR, null)
        Notifications.Bus.notify(notification, project)
    }
}