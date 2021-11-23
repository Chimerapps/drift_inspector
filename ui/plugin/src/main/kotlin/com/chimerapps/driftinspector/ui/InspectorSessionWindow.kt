package com.chimerapps.driftinspector.ui

import com.chimerapps.discovery.device.DirectPreparedConnection
import com.chimerapps.discovery.device.PreparedDeviceConnection
import com.chimerapps.discovery.device.idevice.IDeviceBootstrap
import com.chimerapps.discovery.ui.ConnectDialog
import com.chimerapps.discovery.ui.DiscoveredDeviceConnection
import com.chimerapps.discovery.ui.ManualConnection
import com.chimerapps.discovery.utils.freePort
import com.chimerapps.driftinspector.client.DriftInspectorClient
import com.chimerapps.driftinspector.client.DriftInspectorMessageListener
import com.chimerapps.driftinspector.client.protocol.ExportResponse
import com.chimerapps.driftinspector.client.protocol.DriftInspectorServerInfo
import com.chimerapps.driftinspector.ui.actions.ConnectAction
import com.chimerapps.driftinspector.ui.actions.DisconnectAction
import com.chimerapps.driftinspector.ui.settings.DriftInspectorSettings
import com.chimerapps.driftinspector.ui.util.NotificationUtil
import com.chimerapps.driftinspector.ui.util.ensureMain
import com.chimerapps.driftinspector.ui.util.file.chooseSaveFile
import com.chimerapps.driftinspector.ui.util.preferences.AppPreferences
import com.chimerapps.driftinspector.ui.view.*
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.content.Content
import com.intellij.util.IconUtil
import java.awt.BorderLayout
import java.io.File
import java.io.FileOutputStream
import javax.swing.JPanel
import javax.swing.SwingUtilities

class InspectorSessionWindow(
    private val project: Project,
    private val toolWindow: InspectorToolWindow
) : JPanel(BorderLayout()), DriftInspectorTableQueryHelper, DriftInspectorMessageListener {

    companion object {
        const val DEFAULT_IDEVICE_PATH = "/usr/local/bin"
        private const val APP_PREFERENCE_SPLITTER_STATE = "${AppPreferences.PREFIX}detailSplitter"
    }

    lateinit var content: Content

    private val rootContent = JPanel(BorderLayout())
    private val connectToolbar = setupConnectToolbar()
    private var client: DriftInspectorClient? = null
    private var lastConnection: PreparedDeviceConnection? = null
    private val statusBar = DriftInspectorStatusBar()
    private val tablesView = DriftInspectorTablesView { db, table ->
        tableView.update(db.id, db, db.name, table)
    }
    private val tableView = DriftInspectorTableView(this, project)
    private var pendingExportFile: File? = null

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

        val toolbar = ActionManager.getInstance().createActionToolbar("Drifte Inspector", actionGroup, true)
        val toolbarContainer = JPanel(BorderLayout())
        toolbarContainer.add(toolbar.component, BorderLayout.WEST)

        rootContent.add(toolbarContainer, BorderLayout.NORTH)
        return toolbar
    }

    private fun showConnectDialog() {
        val result = ConnectDialog.show(
            SwingUtilities.getWindowAncestor(this),
            toolWindow.adbInterface ?: return,
            IDeviceBootstrap(File(DriftInspectorSettings.instance.state.iDeviceBinariesPath ?: DEFAULT_IDEVICE_PATH)),
            6395,
            sessionIconProvider = ProjectSessionIconProvider.instance(project),
            configurePluginCallback = {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Drift Inspector")
                toolWindow.adbInterface!! to IDeviceBootstrap(
                    File(
                        DriftInspectorSettings.instance.state.iDeviceBinariesPath ?: DEFAULT_IDEVICE_PATH
                    )
                )
            }) ?: return

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
            DriftInspectorClient(connection.uri).also {
                it.registerMessageListener(statusBar)
                it.registerMessageListener(object : DriftInspectorMessageListener {
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
                it.registerMessageListener(object : DriftInspectorMessageListener {
                    override fun onServerInfo(serverInfo: DriftInspectorServerInfo) {
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

    override fun onFilterData(
        tableId: String,
        requestId: String,
        rows: List<Map<String, Any?>>,
        columns: List<String>
    ) {
        tableView.onQueryResults(requestId, rows, columns)
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

    override fun onExportResult(databaseId: String, requestId: String, exportResponse: ExportResponse) {
        tableView.onExportResult(databaseId, requestId, exportResponse, pendingExportFile ?: return)
    }

    override fun updateItem(
        requestId: String,
        databaseId: String,
        query: String,
        affectedTables: List<String>,
        variables: List<DriftInspectorVariable>
    ) {
        client?.update(requestId, databaseId, query, affectedTables, variables)
    }

    override fun bulkUpdate(requestId: String, databaseId: String, data: List<BulkActionData>) {
        client?.bulkUpdate(requestId, databaseId, data)
    }

    override fun export(requestId: String, databaseId: String, tableNames: List<String>) {
        val client = client ?: return

        pendingExportFile = chooseSaveFile("Save to", "") ?: return
        client.export(requestId, databaseId, tableNames)
    }

    fun onWindowClosed() {
        disconnect()
    }
}

enum class ConnectionMode {
    MODE_CONNECTED,
    MODE_DISCONNECTED
}