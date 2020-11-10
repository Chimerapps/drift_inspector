import 'dart:convert';

import 'package:dart_service_announcement/dart_service_announcement.dart';
import 'package:moor/moor.dart';
import 'package:uuid/uuid.dart';

import 'moor_inspector_server_base.dart';
import 'moore_inspector_empty.dart'
    if (dart.library.html) 'package:moor_inspector/src/moor_inspector_server_web.dart'
    if (dart.library.io) 'package:moor_inspector/src/moor_inspector_server.dart';

const _ANNOUNCEMENT_PORT = 6395;

const _VARIABLE_TYPE_STRING = 'string';
const _VARIABLE_TYPE_BOOL = 'bool';
const _VARIABLE_TYPE_INT = 'int';
const _VARIABLE_TYPE_REAL = 'real';
const _VARIABLE_TYPE_BLOB = 'blob';
const _VARIABLE_TYPE_DATETIME = 'datetime';

class MooreInspectorDriver extends ToolingServer implements ConnectionListener {
  final _databases = List<DatabaseHolder>();
  final MoorInspectorServer _server;
  final String _bundleId;
  final String _icon;
  final String _tag = Uuid().v4().substring(0, 6);
  BaseServerAnnouncementManager _announcementManager;
  List<int> _serverIdData;
  List<int> _serverProtocolData;

  @override
  int get port => _server.port;

  @override
  int get protocolVersion => 1;

  MooreInspectorDriver(
    List<DatabaseHolder> databases,
    this._bundleId,
    this._icon,
    int port,
  ) : _server = createServer(port) {
    _databases.addAll(databases);

    _serverProtocolData = utf8.encode(
        json.encode({'type': 'protocol', 'protocolVersion': protocolVersion}));

    _server.connectionListener = this;
    _announcementManager =
        ServerAnnouncementManager(_bundleId, _ANNOUNCEMENT_PORT, this);
    if (_icon != null) {
      _announcementManager.addExtension(IconExtension(_icon));
    }
    _announcementManager.addExtension(TagExtension(_tag));

    _buildServerIdData();
  }

  Future<void> start() async {
    await _server.start();
    await _announcementManager.start();
  }

  Future<void> stop() async {
    await _server.stop();
    await _announcementManager.stop();
  }

  @override
  void onNewConnection(MooreInspectorConnection connection) {
    connection
      ..sendMessageUTF8(_serverProtocolData)
      ..sendMessageUTF8(_serverIdData);
  }

  void _buildServerIdData() {
    final tableModels = _databases
        .map((tuple) => _buildTableModel(tuple.name, tuple.id, tuple.database))
        .toList();

    final jsonObject = Map<String, dynamic>();
    jsonObject['databases'] = tableModels;
    jsonObject['bundleId'] = _bundleId;
    jsonObject['icon'] = _icon;
    jsonObject['protocolVersion'] = protocolVersion;

    final wrapper = Map<String, dynamic>();
    wrapper['type'] = 'serverInfo';
    wrapper['body'] = jsonObject;

    _serverIdData = utf8.encode(json.encode(wrapper));
  }

  Map<String, dynamic> _buildTableModel(
      String name, String id, GeneratedDatabase db) {
    final root = Map<String, dynamic>();
    root['name'] = name;
    root['id'] = id;

    final structure = Map<String, dynamic>();
    root['structure'] = structure;
    structure['version'] = db.schemaVersion;

    structure['tables'] = db.allTables.map((tableInfo) {
      final table = Map<String, dynamic>();
      table['sqlName'] = tableInfo.actualTableName;
      table['withoutRowId'] = tableInfo.withoutRowId;
      table['primaryKey'] =
          tableInfo.$primaryKey?.map((column) => column.$name)?.toList();

      table['columns'] = tableInfo.$columns.map((column) {
        final columnData = Map<String, dynamic>();

        columnData['name'] = column.$name;
        columnData['isRequired'] = column.isRequired;
        columnData['type'] = column.typeName;
        columnData['nullable'] = column.$nullable;

        return columnData;
      }).toList();
      return table;
    }).toList();

    return root;
  }

  @override
  Future<List<int>> filterTable(
    String databaseId,
    String requestId,
    String query,
    List<InspectorVariable> variables, {
    bool sendResponse = true,
  }) async {
    final db = _databases
        .firstWhere((element) => element.id == databaseId, orElse: () => null)
        ?.database;
    if (db == null) return Future.error(const NoSuchDatabaseException());

    final select =
        db.customSelect(query, variables: variables.map(_mapVariable).toList());
    final data = await select.get();
    if (!sendResponse) return Future.value(List.empty());

    final jsonData = Map<String, dynamic>();
    jsonData['databaseId'] = databaseId;
    jsonData['requestId'] = requestId;

    if (data.isNotEmpty) {
      jsonData['data'] = data.map((row) {
        final rowItem = Map<String, dynamic>();
        row.data.forEach((key, value) {
          rowItem[key] = value;
        });
        return rowItem;
      }).toList();
    }

    final wrapper = Map<String, dynamic>();
    wrapper['type'] = 'filterResult';
    wrapper['body'] = jsonData;

    return utf8.encode(json.encode(wrapper));
  }

  @override
  Future<List<int>> update(
    String databaseId,
    String requestId,
    String query,
    List<String> affectedTables,
    List<InspectorVariable> variables, {
    bool sendResponse = true,
  }) async {
    final db = _databases
        .firstWhere((element) => element.id == databaseId, orElse: () => null)
        ?.database;
    if (db == null) return Future.error(const NoSuchDatabaseException());

    final numUpdated = await db.customUpdate(
      query,
      updates: db.allTables
          .where((element) => affectedTables.contains(element.actualTableName))
          .toSet(),
      variables: variables.map(_mapVariable).toList(),
    );
    if (!sendResponse) return Future.value(List.empty());

    final jsonData = Map<String, dynamic>();
    jsonData['databaseId'] = databaseId;
    jsonData['requestId'] = requestId;
    jsonData['numUpdated'] = numUpdated;

    final wrapper = Map<String, dynamic>();
    wrapper['type'] = 'updateResult';
    wrapper['body'] = jsonData;

    return utf8.encode(json.encode(wrapper));
  }

  Variable<dynamic> _mapVariable(InspectorVariable e) {
    if (e.data == null) {
      return const Variable(null);
    }
    switch (e.type) {
      case _VARIABLE_TYPE_STRING:
        return Variable.withString(e.data as String);
      case _VARIABLE_TYPE_BOOL:
        return Variable.withBool(e.data as bool);
      case _VARIABLE_TYPE_INT:
        return Variable.withInt(e.data as int);
      case _VARIABLE_TYPE_REAL:
        return Variable.withReal(e.data as double);
      case _VARIABLE_TYPE_BLOB:
        return Variable.withBlob(Uint8List.fromList(e.data as List<int>));
      case _VARIABLE_TYPE_DATETIME:
        return Variable.withDateTime(
            DateTime.fromMicrosecondsSinceEpoch(e.data as int, isUtc: true));
    }
    throw MoorInspectorException(
        'Could not map variable type: ${e.type}, no mapping known');
  }
}

class DatabaseHolder {
  final String name;
  final String id;
  final GeneratedDatabase database;

  DatabaseHolder(this.name, this.id, this.database);
}

class NoSuchDatabaseException implements Exception {
  const NoSuchDatabaseException();

  @override
  String toString() => 'No database with given id found';
}

class MoorInspectorException implements Exception {
  final String message;

  MoorInspectorException(this.message);

  @override
  String toString() => message;
}
