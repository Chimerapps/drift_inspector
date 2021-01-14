// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'moor.dart';

// **************************************************************************
// MoorGenerator
// **************************************************************************

// ignore_for_file: unnecessary_brace_in_string_interps, unnecessary_this
class Category extends DataClass implements Insertable<Category> {
  final int id;
  final String? description;
  final bool exampleBool;
  Category({required this.id, this.description, required this.exampleBool});
  factory Category.fromData(Map<String, dynamic> data, GeneratedDatabase db,
      {String? prefix}) {
    final effectivePrefix = prefix ?? '';
    final intType = db.typeSystem.forDartType<int>();
    final stringType = db.typeSystem.forDartType<String>();
    final boolType = db.typeSystem.forDartType<bool>();
    return Category(
      id: intType.mapFromDatabaseResponse(data['${effectivePrefix}id'])!,
      description: stringType
          .mapFromDatabaseResponse(data['${effectivePrefix}description']),
      exampleBool: boolType
          .mapFromDatabaseResponse(data['${effectivePrefix}example_bool'])!,
    );
  }
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    if (!nullToAbsent || description != null) {
      map['description'] = Variable<String?>(description);
    }
    map['example_bool'] = Variable<bool>(exampleBool);
    return map;
  }

  CategoriesCompanion toCompanion(bool nullToAbsent) {
    return CategoriesCompanion(
      id: Value(id),
      description: description == null && nullToAbsent
          ? const Value.absent()
          : Value(description),
      exampleBool: Value(exampleBool),
    );
  }

  factory Category.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return Category(
      id: serializer.fromJson<int>(json['id']),
      description: serializer.fromJson<String?>(json['description']),
      exampleBool: serializer.fromJson<bool>(json['exampleBool']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'description': serializer.toJson<String?>(description),
      'exampleBool': serializer.toJson<bool>(exampleBool),
    };
  }

  Category copyWith({int? id, String? description, bool? exampleBool}) =>
      Category(
        id: id ?? this.id,
        description: description ?? this.description,
        exampleBool: exampleBool ?? this.exampleBool,
      );
  @override
  String toString() {
    return (StringBuffer('Category(')
          ..write('id: $id, ')
          ..write('description: $description, ')
          ..write('exampleBool: $exampleBool')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => $mrjf(
      $mrjc(id.hashCode, $mrjc(description.hashCode, exampleBool.hashCode)));
  @override
  bool operator ==(dynamic other) =>
      identical(this, other) ||
      (other is Category &&
          other.id == this.id &&
          other.description == this.description &&
          other.exampleBool == this.exampleBool);
}

class CategoriesCompanion extends UpdateCompanion<Category> {
  final Value<int> id;
  final Value<String?> description;
  final Value<bool> exampleBool;
  const CategoriesCompanion({
    this.id = const Value.absent(),
    this.description = const Value.absent(),
    this.exampleBool = const Value.absent(),
  });
  CategoriesCompanion.insert({
    this.id = const Value.absent(),
    this.description = const Value.absent(),
    required bool exampleBool,
  }) : exampleBool = Value(exampleBool);
  static Insertable<Category> custom({
    Expression<int>? id,
    Expression<String?>? description,
    Expression<bool>? exampleBool,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (description != null) 'description': description,
      if (exampleBool != null) 'example_bool': exampleBool,
    });
  }

  CategoriesCompanion copyWith(
      {Value<int>? id, Value<String?>? description, Value<bool>? exampleBool}) {
    return CategoriesCompanion(
      id: id ?? this.id,
      description: description ?? this.description,
      exampleBool: exampleBool ?? this.exampleBool,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (description.present) {
      map['description'] = Variable<String?>(description.value);
    }
    if (exampleBool.present) {
      map['example_bool'] = Variable<bool>(exampleBool.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('CategoriesCompanion(')
          ..write('id: $id, ')
          ..write('description: $description, ')
          ..write('exampleBool: $exampleBool')
          ..write(')'))
        .toString();
  }
}

class $CategoriesTable extends Categories
    with TableInfo<$CategoriesTable, Category> {
  final GeneratedDatabase _db;
  final String? _alias;
  $CategoriesTable(this._db, [this._alias]);
  final VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedIntColumn id = _constructId();
  GeneratedIntColumn _constructId() {
    return GeneratedIntColumn('id', $tableName, false,
        hasAutoIncrement: true, declaredAsPrimaryKey: true);
  }

  final VerificationMeta _descriptionMeta =
      const VerificationMeta('description');
  @override
  late final GeneratedTextColumn description = _constructDescription();
  GeneratedTextColumn _constructDescription() {
    return GeneratedTextColumn(
      'description',
      $tableName,
      true,
    );
  }

  final VerificationMeta _exampleBoolMeta =
      const VerificationMeta('exampleBool');
  @override
  late final GeneratedBoolColumn exampleBool = _constructExampleBool();
  GeneratedBoolColumn _constructExampleBool() {
    return GeneratedBoolColumn(
      'example_bool',
      $tableName,
      false,
    );
  }

  @override
  List<GeneratedColumn> get $columns => [id, description, exampleBool];
  @override
  $CategoriesTable get asDslTable => this;
  @override
  String get $tableName => _alias ?? 'categories';
  @override
  final String actualTableName = 'categories';
  @override
  VerificationContext validateIntegrity(Insertable<Category> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('description')) {
      context.handle(
          _descriptionMeta,
          description.isAcceptableOrUnknown(
              data['description']!, _descriptionMeta));
    }
    if (data.containsKey('example_bool')) {
      context.handle(
          _exampleBoolMeta,
          exampleBool.isAcceptableOrUnknown(
              data['example_bool']!, _exampleBoolMeta));
    } else if (isInserting) {
      context.missing(_exampleBoolMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  Category map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : null;
    return Category.fromData(data, _db, prefix: effectivePrefix);
  }

  @override
  $CategoriesTable createAlias(String alias) {
    return $CategoriesTable(_db, alias);
  }
}

class Recipe extends DataClass implements Insertable<Recipe> {
  final int id;
  final String title;
  final String instructions;
  final int? category;
  Recipe(
      {required this.id,
      required this.title,
      required this.instructions,
      this.category});
  factory Recipe.fromData(Map<String, dynamic> data, GeneratedDatabase db,
      {String? prefix}) {
    final effectivePrefix = prefix ?? '';
    final intType = db.typeSystem.forDartType<int>();
    final stringType = db.typeSystem.forDartType<String>();
    return Recipe(
      id: intType.mapFromDatabaseResponse(data['${effectivePrefix}id'])!,
      title:
          stringType.mapFromDatabaseResponse(data['${effectivePrefix}title'])!,
      instructions: stringType
          .mapFromDatabaseResponse(data['${effectivePrefix}instructions'])!,
      category:
          intType.mapFromDatabaseResponse(data['${effectivePrefix}category']),
    );
  }
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['title'] = Variable<String>(title);
    map['instructions'] = Variable<String>(instructions);
    if (!nullToAbsent || category != null) {
      map['category'] = Variable<int?>(category);
    }
    return map;
  }

  RecipesCompanion toCompanion(bool nullToAbsent) {
    return RecipesCompanion(
      id: Value(id),
      title: Value(title),
      instructions: Value(instructions),
      category: category == null && nullToAbsent
          ? const Value.absent()
          : Value(category),
    );
  }

  factory Recipe.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return Recipe(
      id: serializer.fromJson<int>(json['id']),
      title: serializer.fromJson<String>(json['title']),
      instructions: serializer.fromJson<String>(json['instructions']),
      category: serializer.fromJson<int?>(json['category']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'title': serializer.toJson<String>(title),
      'instructions': serializer.toJson<String>(instructions),
      'category': serializer.toJson<int?>(category),
    };
  }

  Recipe copyWith(
          {int? id, String? title, String? instructions, int? category}) =>
      Recipe(
        id: id ?? this.id,
        title: title ?? this.title,
        instructions: instructions ?? this.instructions,
        category: category ?? this.category,
      );
  @override
  String toString() {
    return (StringBuffer('Recipe(')
          ..write('id: $id, ')
          ..write('title: $title, ')
          ..write('instructions: $instructions, ')
          ..write('category: $category')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => $mrjf($mrjc(id.hashCode,
      $mrjc(title.hashCode, $mrjc(instructions.hashCode, category.hashCode))));
  @override
  bool operator ==(dynamic other) =>
      identical(this, other) ||
      (other is Recipe &&
          other.id == this.id &&
          other.title == this.title &&
          other.instructions == this.instructions &&
          other.category == this.category);
}

class RecipesCompanion extends UpdateCompanion<Recipe> {
  final Value<int> id;
  final Value<String> title;
  final Value<String> instructions;
  final Value<int?> category;
  const RecipesCompanion({
    this.id = const Value.absent(),
    this.title = const Value.absent(),
    this.instructions = const Value.absent(),
    this.category = const Value.absent(),
  });
  RecipesCompanion.insert({
    this.id = const Value.absent(),
    required String title,
    required String instructions,
    this.category = const Value.absent(),
  })  : title = Value(title),
        instructions = Value(instructions);
  static Insertable<Recipe> custom({
    Expression<int>? id,
    Expression<String>? title,
    Expression<String>? instructions,
    Expression<int?>? category,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (title != null) 'title': title,
      if (instructions != null) 'instructions': instructions,
      if (category != null) 'category': category,
    });
  }

  RecipesCompanion copyWith(
      {Value<int>? id,
      Value<String>? title,
      Value<String>? instructions,
      Value<int?>? category}) {
    return RecipesCompanion(
      id: id ?? this.id,
      title: title ?? this.title,
      instructions: instructions ?? this.instructions,
      category: category ?? this.category,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (title.present) {
      map['title'] = Variable<String>(title.value);
    }
    if (instructions.present) {
      map['instructions'] = Variable<String>(instructions.value);
    }
    if (category.present) {
      map['category'] = Variable<int?>(category.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RecipesCompanion(')
          ..write('id: $id, ')
          ..write('title: $title, ')
          ..write('instructions: $instructions, ')
          ..write('category: $category')
          ..write(')'))
        .toString();
  }
}

class $RecipesTable extends Recipes with TableInfo<$RecipesTable, Recipe> {
  final GeneratedDatabase _db;
  final String? _alias;
  $RecipesTable(this._db, [this._alias]);
  final VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedIntColumn id = _constructId();
  GeneratedIntColumn _constructId() {
    return GeneratedIntColumn('id', $tableName, false,
        hasAutoIncrement: true, declaredAsPrimaryKey: true);
  }

  final VerificationMeta _titleMeta = const VerificationMeta('title');
  @override
  late final GeneratedTextColumn title = _constructTitle();
  GeneratedTextColumn _constructTitle() {
    return GeneratedTextColumn('title', $tableName, false, maxTextLength: 16);
  }

  final VerificationMeta _instructionsMeta =
      const VerificationMeta('instructions');
  @override
  late final GeneratedTextColumn instructions = _constructInstructions();
  GeneratedTextColumn _constructInstructions() {
    return GeneratedTextColumn(
      'instructions',
      $tableName,
      false,
    );
  }

  final VerificationMeta _categoryMeta = const VerificationMeta('category');
  @override
  late final GeneratedIntColumn category = _constructCategory();
  GeneratedIntColumn _constructCategory() {
    return GeneratedIntColumn(
      'category',
      $tableName,
      true,
    );
  }

  @override
  List<GeneratedColumn> get $columns => [id, title, instructions, category];
  @override
  $RecipesTable get asDslTable => this;
  @override
  String get $tableName => _alias ?? 'recipes';
  @override
  final String actualTableName = 'recipes';
  @override
  VerificationContext validateIntegrity(Insertable<Recipe> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('title')) {
      context.handle(
          _titleMeta, title.isAcceptableOrUnknown(data['title']!, _titleMeta));
    } else if (isInserting) {
      context.missing(_titleMeta);
    }
    if (data.containsKey('instructions')) {
      context.handle(
          _instructionsMeta,
          instructions.isAcceptableOrUnknown(
              data['instructions']!, _instructionsMeta));
    } else if (isInserting) {
      context.missing(_instructionsMeta);
    }
    if (data.containsKey('category')) {
      context.handle(_categoryMeta,
          category.isAcceptableOrUnknown(data['category']!, _categoryMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  Recipe map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : null;
    return Recipe.fromData(data, _db, prefix: effectivePrefix);
  }

  @override
  $RecipesTable createAlias(String alias) {
    return $RecipesTable(_db, alias);
  }
}

class Ingredient extends DataClass implements Insertable<Ingredient> {
  final int id;
  final String name;
  final int caloriesPer100g;
  Ingredient(
      {required this.id, required this.name, required this.caloriesPer100g});
  factory Ingredient.fromData(Map<String, dynamic> data, GeneratedDatabase db,
      {String? prefix}) {
    final effectivePrefix = prefix ?? '';
    final intType = db.typeSystem.forDartType<int>();
    final stringType = db.typeSystem.forDartType<String>();
    return Ingredient(
      id: intType.mapFromDatabaseResponse(data['${effectivePrefix}id'])!,
      name: stringType.mapFromDatabaseResponse(data['${effectivePrefix}name'])!,
      caloriesPer100g:
          intType.mapFromDatabaseResponse(data['${effectivePrefix}calories'])!,
    );
  }
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['name'] = Variable<String>(name);
    map['calories'] = Variable<int>(caloriesPer100g);
    return map;
  }

  IngredientsCompanion toCompanion(bool nullToAbsent) {
    return IngredientsCompanion(
      id: Value(id),
      name: Value(name),
      caloriesPer100g: Value(caloriesPer100g),
    );
  }

  factory Ingredient.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return Ingredient(
      id: serializer.fromJson<int>(json['id']),
      name: serializer.fromJson<String>(json['name']),
      caloriesPer100g: serializer.fromJson<int>(json['caloriesPer100g']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'name': serializer.toJson<String>(name),
      'caloriesPer100g': serializer.toJson<int>(caloriesPer100g),
    };
  }

  Ingredient copyWith({int? id, String? name, int? caloriesPer100g}) =>
      Ingredient(
        id: id ?? this.id,
        name: name ?? this.name,
        caloriesPer100g: caloriesPer100g ?? this.caloriesPer100g,
      );
  @override
  String toString() {
    return (StringBuffer('Ingredient(')
          ..write('id: $id, ')
          ..write('name: $name, ')
          ..write('caloriesPer100g: $caloriesPer100g')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode =>
      $mrjf($mrjc(id.hashCode, $mrjc(name.hashCode, caloriesPer100g.hashCode)));
  @override
  bool operator ==(dynamic other) =>
      identical(this, other) ||
      (other is Ingredient &&
          other.id == this.id &&
          other.name == this.name &&
          other.caloriesPer100g == this.caloriesPer100g);
}

class IngredientsCompanion extends UpdateCompanion<Ingredient> {
  final Value<int> id;
  final Value<String> name;
  final Value<int> caloriesPer100g;
  const IngredientsCompanion({
    this.id = const Value.absent(),
    this.name = const Value.absent(),
    this.caloriesPer100g = const Value.absent(),
  });
  IngredientsCompanion.insert({
    this.id = const Value.absent(),
    required String name,
    required int caloriesPer100g,
  })   : name = Value(name),
        caloriesPer100g = Value(caloriesPer100g);
  static Insertable<Ingredient> custom({
    Expression<int>? id,
    Expression<String>? name,
    Expression<int>? caloriesPer100g,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (name != null) 'name': name,
      if (caloriesPer100g != null) 'calories': caloriesPer100g,
    });
  }

  IngredientsCompanion copyWith(
      {Value<int>? id, Value<String>? name, Value<int>? caloriesPer100g}) {
    return IngredientsCompanion(
      id: id ?? this.id,
      name: name ?? this.name,
      caloriesPer100g: caloriesPer100g ?? this.caloriesPer100g,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (name.present) {
      map['name'] = Variable<String>(name.value);
    }
    if (caloriesPer100g.present) {
      map['calories'] = Variable<int>(caloriesPer100g.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('IngredientsCompanion(')
          ..write('id: $id, ')
          ..write('name: $name, ')
          ..write('caloriesPer100g: $caloriesPer100g')
          ..write(')'))
        .toString();
  }
}

class $IngredientsTable extends Ingredients
    with TableInfo<$IngredientsTable, Ingredient> {
  final GeneratedDatabase _db;
  final String? _alias;
  $IngredientsTable(this._db, [this._alias]);
  final VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedIntColumn id = _constructId();
  GeneratedIntColumn _constructId() {
    return GeneratedIntColumn('id', $tableName, false,
        hasAutoIncrement: true, declaredAsPrimaryKey: true);
  }

  final VerificationMeta _nameMeta = const VerificationMeta('name');
  @override
  late final GeneratedTextColumn name = _constructName();
  GeneratedTextColumn _constructName() {
    return GeneratedTextColumn(
      'name',
      $tableName,
      false,
    );
  }

  final VerificationMeta _caloriesPer100gMeta =
      const VerificationMeta('caloriesPer100g');
  @override
  late final GeneratedIntColumn caloriesPer100g = _constructCaloriesPer100g();
  GeneratedIntColumn _constructCaloriesPer100g() {
    return GeneratedIntColumn(
      'calories',
      $tableName,
      false,
    );
  }

  @override
  List<GeneratedColumn> get $columns => [id, name, caloriesPer100g];
  @override
  $IngredientsTable get asDslTable => this;
  @override
  String get $tableName => _alias ?? 'ingredients';
  @override
  final String actualTableName = 'ingredients';
  @override
  VerificationContext validateIntegrity(Insertable<Ingredient> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('name')) {
      context.handle(
          _nameMeta, name.isAcceptableOrUnknown(data['name']!, _nameMeta));
    } else if (isInserting) {
      context.missing(_nameMeta);
    }
    if (data.containsKey('calories')) {
      context.handle(
          _caloriesPer100gMeta,
          caloriesPer100g.isAcceptableOrUnknown(
              data['calories']!, _caloriesPer100gMeta));
    } else if (isInserting) {
      context.missing(_caloriesPer100gMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  Ingredient map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : null;
    return Ingredient.fromData(data, _db, prefix: effectivePrefix);
  }

  @override
  $IngredientsTable createAlias(String alias) {
    return $IngredientsTable(_db, alias);
  }
}

class IngredientInRecipe extends DataClass
    implements Insertable<IngredientInRecipe> {
  final int recipe;
  final int ingredient;
  final int amountInGrams;
  IngredientInRecipe(
      {required this.recipe,
      required this.ingredient,
      required this.amountInGrams});
  factory IngredientInRecipe.fromData(
      Map<String, dynamic> data, GeneratedDatabase db,
      {String? prefix}) {
    final effectivePrefix = prefix ?? '';
    final intType = db.typeSystem.forDartType<int>();
    return IngredientInRecipe(
      recipe:
          intType.mapFromDatabaseResponse(data['${effectivePrefix}recipe'])!,
      ingredient: intType
          .mapFromDatabaseResponse(data['${effectivePrefix}ingredient'])!,
      amountInGrams:
          intType.mapFromDatabaseResponse(data['${effectivePrefix}amount'])!,
    );
  }
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['recipe'] = Variable<int>(recipe);
    map['ingredient'] = Variable<int>(ingredient);
    map['amount'] = Variable<int>(amountInGrams);
    return map;
  }

  IngredientInRecipesCompanion toCompanion(bool nullToAbsent) {
    return IngredientInRecipesCompanion(
      recipe: Value(recipe),
      ingredient: Value(ingredient),
      amountInGrams: Value(amountInGrams),
    );
  }

  factory IngredientInRecipe.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return IngredientInRecipe(
      recipe: serializer.fromJson<int>(json['recipe']),
      ingredient: serializer.fromJson<int>(json['ingredient']),
      amountInGrams: serializer.fromJson<int>(json['amountInGrams']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= moorRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'recipe': serializer.toJson<int>(recipe),
      'ingredient': serializer.toJson<int>(ingredient),
      'amountInGrams': serializer.toJson<int>(amountInGrams),
    };
  }

  IngredientInRecipe copyWith(
          {int? recipe, int? ingredient, int? amountInGrams}) =>
      IngredientInRecipe(
        recipe: recipe ?? this.recipe,
        ingredient: ingredient ?? this.ingredient,
        amountInGrams: amountInGrams ?? this.amountInGrams,
      );
  @override
  String toString() {
    return (StringBuffer('IngredientInRecipe(')
          ..write('recipe: $recipe, ')
          ..write('ingredient: $ingredient, ')
          ..write('amountInGrams: $amountInGrams')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => $mrjf($mrjc(
      recipe.hashCode, $mrjc(ingredient.hashCode, amountInGrams.hashCode)));
  @override
  bool operator ==(dynamic other) =>
      identical(this, other) ||
      (other is IngredientInRecipe &&
          other.recipe == this.recipe &&
          other.ingredient == this.ingredient &&
          other.amountInGrams == this.amountInGrams);
}

class IngredientInRecipesCompanion extends UpdateCompanion<IngredientInRecipe> {
  final Value<int> recipe;
  final Value<int> ingredient;
  final Value<int> amountInGrams;
  const IngredientInRecipesCompanion({
    this.recipe = const Value.absent(),
    this.ingredient = const Value.absent(),
    this.amountInGrams = const Value.absent(),
  });
  IngredientInRecipesCompanion.insert({
    required int recipe,
    required int ingredient,
    required int amountInGrams,
  })   : recipe = Value(recipe),
        ingredient = Value(ingredient),
        amountInGrams = Value(amountInGrams);
  static Insertable<IngredientInRecipe> custom({
    Expression<int>? recipe,
    Expression<int>? ingredient,
    Expression<int>? amountInGrams,
  }) {
    return RawValuesInsertable({
      if (recipe != null) 'recipe': recipe,
      if (ingredient != null) 'ingredient': ingredient,
      if (amountInGrams != null) 'amount': amountInGrams,
    });
  }

  IngredientInRecipesCompanion copyWith(
      {Value<int>? recipe, Value<int>? ingredient, Value<int>? amountInGrams}) {
    return IngredientInRecipesCompanion(
      recipe: recipe ?? this.recipe,
      ingredient: ingredient ?? this.ingredient,
      amountInGrams: amountInGrams ?? this.amountInGrams,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (recipe.present) {
      map['recipe'] = Variable<int>(recipe.value);
    }
    if (ingredient.present) {
      map['ingredient'] = Variable<int>(ingredient.value);
    }
    if (amountInGrams.present) {
      map['amount'] = Variable<int>(amountInGrams.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('IngredientInRecipesCompanion(')
          ..write('recipe: $recipe, ')
          ..write('ingredient: $ingredient, ')
          ..write('amountInGrams: $amountInGrams')
          ..write(')'))
        .toString();
  }
}

class $IngredientInRecipesTable extends IngredientInRecipes
    with TableInfo<$IngredientInRecipesTable, IngredientInRecipe> {
  final GeneratedDatabase _db;
  final String? _alias;
  $IngredientInRecipesTable(this._db, [this._alias]);
  final VerificationMeta _recipeMeta = const VerificationMeta('recipe');
  @override
  late final GeneratedIntColumn recipe = _constructRecipe();
  GeneratedIntColumn _constructRecipe() {
    return GeneratedIntColumn(
      'recipe',
      $tableName,
      false,
    );
  }

  final VerificationMeta _ingredientMeta = const VerificationMeta('ingredient');
  @override
  late final GeneratedIntColumn ingredient = _constructIngredient();
  GeneratedIntColumn _constructIngredient() {
    return GeneratedIntColumn(
      'ingredient',
      $tableName,
      false,
    );
  }

  final VerificationMeta _amountInGramsMeta =
      const VerificationMeta('amountInGrams');
  @override
  late final GeneratedIntColumn amountInGrams = _constructAmountInGrams();
  GeneratedIntColumn _constructAmountInGrams() {
    return GeneratedIntColumn(
      'amount',
      $tableName,
      false,
    );
  }

  @override
  List<GeneratedColumn> get $columns => [recipe, ingredient, amountInGrams];
  @override
  $IngredientInRecipesTable get asDslTable => this;
  @override
  String get $tableName => _alias ?? 'recipe_ingredients';
  @override
  final String actualTableName = 'recipe_ingredients';
  @override
  VerificationContext validateIntegrity(Insertable<IngredientInRecipe> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('recipe')) {
      context.handle(_recipeMeta,
          recipe.isAcceptableOrUnknown(data['recipe']!, _recipeMeta));
    } else if (isInserting) {
      context.missing(_recipeMeta);
    }
    if (data.containsKey('ingredient')) {
      context.handle(
          _ingredientMeta,
          ingredient.isAcceptableOrUnknown(
              data['ingredient']!, _ingredientMeta));
    } else if (isInserting) {
      context.missing(_ingredientMeta);
    }
    if (data.containsKey('amount')) {
      context.handle(
          _amountInGramsMeta,
          amountInGrams.isAcceptableOrUnknown(
              data['amount']!, _amountInGramsMeta));
    } else if (isInserting) {
      context.missing(_amountInGramsMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {recipe, ingredient};
  @override
  IngredientInRecipe map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : null;
    return IngredientInRecipe.fromData(data, _db, prefix: effectivePrefix);
  }

  @override
  $IngredientInRecipesTable createAlias(String alias) {
    return $IngredientInRecipesTable(_db, alias);
  }
}

abstract class _$Database extends GeneratedDatabase {
  _$Database(QueryExecutor e) : super(SqlTypeSystem.defaultInstance, e);
  late final $CategoriesTable categories = $CategoriesTable(this);
  late final $RecipesTable recipes = $RecipesTable(this);
  late final $IngredientsTable ingredients = $IngredientsTable(this);
  late final $IngredientInRecipesTable ingredientInRecipes =
      $IngredientInRecipesTable(this);
  late final ExampleDao exampleDao = ExampleDao(this as Database);
  Selectable<TotalWeightResult> totalWeight() {
    return customSelect(
        'SELECT r.title, SUM(ir.amount) AS total_weight\n        FROM recipes r\n        INNER JOIN recipe_ingredients ir ON ir.recipe = r.id\n      GROUP BY r.id',
        variables: [],
        readsFrom: {recipes, ingredientInRecipes}).map((QueryRow row) {
      return TotalWeightResult(
        title: row.readString('title'),
        totalWeight: row.readInt('total_weight'),
      );
    });
  }

  @override
  Iterable<TableInfo> get allTables => allSchemaEntities.whereType<TableInfo>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities =>
      [categories, recipes, ingredients, ingredientInRecipes];
}

class TotalWeightResult {
  final String title;
  final int totalWeight;
  TotalWeightResult({
    required this.title,
    required this.totalWeight,
  });
}

// **************************************************************************
// DaoGenerator
// **************************************************************************

mixin _$ExampleDaoMixin on DatabaseAccessor<Database> {
  $IngredientsTable get ingredients => attachedDatabase.ingredients;
  $CategoriesTable get categories => attachedDatabase.categories;
  $RecipesTable get recipes => attachedDatabase.recipes;
  $IngredientInRecipesTable get ingredientInRecipes =>
      attachedDatabase.ingredientInRecipes;
}
