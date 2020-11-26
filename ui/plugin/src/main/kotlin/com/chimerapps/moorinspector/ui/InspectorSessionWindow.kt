package com.chimerapps.moorinspector.ui

import com.chimerapps.discovery.device.DirectPreparedConnection
import com.chimerapps.discovery.device.PreparedDeviceConnection
import com.chimerapps.discovery.device.idevice.IDeviceBootstrap
import com.chimerapps.discovery.ui.ConnectDialog
import com.chimerapps.discovery.ui.ConnectDialogLocalization
import com.chimerapps.discovery.ui.DiscoveredDeviceConnection
import com.chimerapps.discovery.ui.ManualConnection
import com.chimerapps.discovery.utils.freePort
import com.chimerapps.moorinspector.client.MoorInspectorClient
import com.chimerapps.moorinspector.client.MoorInspectorMessageListener
import com.chimerapps.moorinspector.client.protocol.MoorInspectorServerInfo
import com.chimerapps.moorinspector.ui.actions.ConnectAction
import com.chimerapps.moorinspector.ui.actions.DisconnectAction
import com.chimerapps.moorinspector.ui.settings.MoorInspectorSettings
import com.chimerapps.moorinspector.ui.util.NotificationUtil
import com.chimerapps.moorinspector.ui.util.ensureMain
import com.chimerapps.moorinspector.ui.util.preferences.AppPreferences
import com.chimerapps.moorinspector.ui.view.*
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.content.Content
import com.intellij.util.IconUtil
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.SwingUtilities

class InspectorSessionWindow(
    private val project: Project,
    private val toolWindow: InspectorToolWindow
) : JPanel(BorderLayout()), MoorInspectorTableQueryHelper, MoorInspectorMessageListener {

    companion object {
        const val DEFAULT_IDEVICE_PATH = "/usr/local/bin"
        private const val APP_PREFERENCE_SPLITTER_STATE = "${AppPreferences.PREFIX}detailSplitter"
    }

    lateinit var content: Content

    private val rootContent = JPanel(BorderLayout())
    private val connectToolbar = setupConnectToolbar()
    private var client: MoorInspectorClient? = null
    private var lastConnection: PreparedDeviceConnection? = null
    private val statusBar = MoorInspectorStatusBar()
    private val tablesView = MoorInspectorTablesView() { db, table ->
        tableView.update(db.id, db.name, table)
    }
    private val tableView = MoorInspectorTableView(this, project)

    var connectionMode: ConnectionMode = ConnectionMode.MODE_DISCONNECTED
        private set(value) {
            field = value
            ensureMain {
                connectToolbar.updateActionsImmediately()
            }
        }

    init {
        add(rootContent, BorderLayout.CENTER)
        add(statusBar, BorderLayout.SOUTH)

        val splitter = JBSplitter(APP_PREFERENCE_SPLITTER_STATE, 0.2f)
        splitter.firstComponent = tablesView
        splitter.secondComponent = tableView

        rootContent.add(splitter, BorderLayout.CENTER)
    }

    private fun setupConnectToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup()

        actionGroup.add(ConnectAction(this) {
            showConnectDialog()
        })
        actionGroup.add(DisconnectAction(this) {
            disconnect()

            connectionMode = ConnectionMode.MODE_DISCONNECTED
        })

        val toolbar = ActionManager.getInstance().createActionToolbar("Moore Inspector", actionGroup, true)
        val toolbarContainer = JPanel(BorderLayout())
        toolbarContainer.add(toolbar.component, BorderLayout.WEST)

        rootContent.add(toolbarContainer, BorderLayout.NORTH)
        return toolbar
    }

    private fun showConnectDialog() {
        val result = ConnectDialog.show(
            SwingUtilities.getWindowAncestor(this),
            toolWindow.adbInterface ?: return,
            IDeviceBootstrap(File(MoorInspectorSettings.instance.iDeviceBinariesPath ?: DEFAULT_IDEVICE_PATH)),
            6395,
            sessionIconProvider = ProjectSessionIconProvider.instance(project),
            localization = object : ConnectDialogLocalization {}) ?: return

        result.discovered?.let {
            tryConnectSession(it)
        }
        result.direct?.let {
            tryConnectDirect(it)
        }
    }

    private fun disconnect() {
        try {
            client?.close()
        } catch (ignore: Throwable) {
        }
        client = null
        try {
            lastConnection?.tearDown()
        } catch (ignore: Throwable) {
        }
        lastConnection = null
    }

    private fun tryConnectDirect(directConnection: ManualConnection) {
        disconnect()

        connectOnConnection(DirectPreparedConnection(directConnection.ip, directConnection.port))
    }

    private fun tryConnectSession(discovered: DiscoveredDeviceConnection) {
        disconnect()

        val connection = discovered.device.prepareConnection(freePort(), discovered.session.port)
        connectOnConnection(connection)
    }

    private fun connectOnConnection(connection: PreparedDeviceConnection) {
        client =
            MoorInspectorClient(connection.uri).also {
                it.registerMessageListener(statusBar)
                it.registerMessageListener(object : MoorInspectorMessageListener {
                    override fun onClosed() {
                        disconnect()
                        ensureMain {
                            connectionMode = ConnectionMode.MODE_DISCONNECTED
                            content.icon?.let { icon ->
                                content.icon = IconUtil.desaturate(icon)
                            }
                        }
                    }
                })
                it.registerMessageListener(this)
                it.registerMessageListener(object : MoorInspectorMessageListener {
                    override fun onServerInfo(serverInfo: MoorInspectorServerInfo) {
                        ensureMain {
                            val newIcon = serverInfo.icon?.let { iconString ->
                                ProjectSessionIconProvider.instance(project).iconForString(iconString)
                            }
                            content.icon = newIcon
                        }
                    }
                })
                it.registerMessageListener(tablesView)
            }
        client?.connect()
        lastConnection = connection
        connectionMode = ConnectionMode.MODE_CONNECTED
    }

    override fun onFilterData(tableId: String, requestId: String, rows: List<Map<String, Any?>>) {
        tableView.onQueryResults(requestId, rows)
    }

    override fun onUpdateResult(tableId: String, requestId: String, numRowsUpdated: Int) {
        tableView.onUpdateComplete(requestId)
    }

    override fun onError(requestId: String, message: String) {
        tableView.onError(requestId)
        NotificationUtil.error("Request failed", "Failed to execute request: $message", project)
    }

    override fun onBulkUpdateResult(requestId: String) {
        tableView.onUpdateComplete(requestId)
    }

    override fun query(requestId: String, databaseId: String, query: String) {
        client?.query(requestId, databaseId, query)
    }

    override fun updateItem(
        requestId: String,
        databaseId: String,
        query: String,
        affectedTables: List<String>,
        variables: List<MoorInspectorVariable>
    ) {
        client?.update(requestId, databaseId, query, affectedTables, variables)
    }

    override fun bulkUpdate(requestId: String, databaseId: String, data: List<BulkActionData>) {
        client?.bulkUpdate(requestId, databaseId, data)
    }

    fun onWindowClosed() {
        disconnect()
    }
}

enum class ConnectionMode {
    MODE_CONNECTED,
    MODE_DISCONNECTED
}