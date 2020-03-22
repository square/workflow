if (typeof kotlin === 'undefined') {
  throw new Error("Error loading module 'instant-execution-report'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'instant-execution-report'.");
}
this['instant-execution-report'] = function (_, Kotlin) {
  'use strict';
  var $$importsForInline$$ = _.$$importsForInline$$ || (_.$$importsForInline$$ = {});
  var Kind_CLASS = Kotlin.Kind.CLASS;
  var Enum = Kotlin.kotlin.Enum;
  var throwISE = Kotlin.throwISE;
  var Unit = Kotlin.kotlin.Unit;
  var getCallableRef = Kotlin.getCallableRef;
  var filter = Kotlin.kotlin.sequences.filter_euau3h$;
  var Kind_OBJECT = Kotlin.Kind.OBJECT;
  var collectionSizeOrDefault = Kotlin.kotlin.collections.collectionSizeOrDefault_ba2ldo$;
  var ArrayList_init = Kotlin.kotlin.collections.ArrayList_init_ww73n8$;
  var asSequence = Kotlin.kotlin.collections.asSequence_7wnvza$;
  var map = Kotlin.kotlin.sequences.map_z5avom$;
  var asReversed = Kotlin.kotlin.collections.asReversed_2p1efm$;
  var plus = Kotlin.kotlin.collections.plus_qloxvw$;
  var toString = Kotlin.toString;
  var toList = Kotlin.kotlin.sequences.toList_veqyi0$;
  var ArrayList_init_0 = Kotlin.kotlin.collections.ArrayList_init_287e2$;
  var checkIndexOverflow = Kotlin.kotlin.collections.checkIndexOverflow_za3lpa$;
  var sortedWith = Kotlin.kotlin.sequences.sortedWith_vjgqpk$;
  var wrapFunction = Kotlin.wrapFunction;
  var Comparator = Kotlin.kotlin.Comparator;
  var defineInlineFunction = Kotlin.defineInlineFunction;
  var asSequence_0 = Kotlin.kotlin.collections.asSequence_abgq59$;
  var Map = Kotlin.kotlin.collections.Map;
  var to = Kotlin.kotlin.to_ujzrz7$;
  var HashMap = Kotlin.kotlin.collections.HashMap;
  var throwCCE = Kotlin.throwCCE;
  var HashMap_init = Kotlin.kotlin.collections.HashMap_init_q3lmfv$;
  var Kind_INTERFACE = Kotlin.Kind.INTERFACE;
  var println = Kotlin.kotlin.io.println_s8jyv4$;
  var IllegalStateException_init = Kotlin.kotlin.IllegalStateException_init_pdl1vj$;
  var asList = Kotlin.kotlin.collections.asList_us0mfu$;
  var emptyList = Kotlin.kotlin.collections.emptyList_287e2$;
  var appendText = Kotlin.kotlin.dom.appendText_46n0ku$;
  var addClass = Kotlin.kotlin.dom.addClass_hhb33f$;
  var appendElement = Kotlin.kotlin.dom.appendElement_ldvnw0$;
  var equals = Kotlin.equals;
  var get_indices = Kotlin.kotlin.collections.get_indices_gzk92b$;
  ProblemNode$Error.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Error.prototype.constructor = ProblemNode$Error;
  ProblemNode$Warning.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Warning.prototype.constructor = ProblemNode$Warning;
  ProblemNode$Task.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Task.prototype.constructor = ProblemNode$Task;
  ProblemNode$Bean.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Bean.prototype.constructor = ProblemNode$Bean;
  ProblemNode$Property.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Property.prototype.constructor = ProblemNode$Property;
  ProblemNode$Label.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Label.prototype.constructor = ProblemNode$Label;
  ProblemNode$Message.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Message.prototype.constructor = ProblemNode$Message;
  ProblemNode$Exception.prototype = Object.create(ProblemNode.prototype);
  ProblemNode$Exception.prototype.constructor = ProblemNode$Exception;
  PrettyText$Fragment$Text.prototype = Object.create(PrettyText$Fragment.prototype);
  PrettyText$Fragment$Text.prototype.constructor = PrettyText$Fragment$Text;
  PrettyText$Fragment$Reference.prototype = Object.create(PrettyText$Fragment.prototype);
  PrettyText$Fragment$Reference.prototype.constructor = PrettyText$Fragment$Reference;
  InstantExecutionReportPage$DisplayFilter.prototype = Object.create(Enum.prototype);
  InstantExecutionReportPage$DisplayFilter.prototype.constructor = InstantExecutionReportPage$DisplayFilter;
  InstantExecutionReportPage$Intent$TaskTreeIntent.prototype = Object.create(InstantExecutionReportPage$Intent.prototype);
  InstantExecutionReportPage$Intent$TaskTreeIntent.prototype.constructor = InstantExecutionReportPage$Intent$TaskTreeIntent;
  InstantExecutionReportPage$Intent$MessageTreeIntent.prototype = Object.create(InstantExecutionReportPage$Intent.prototype);
  InstantExecutionReportPage$Intent$MessageTreeIntent.prototype.constructor = InstantExecutionReportPage$Intent$MessageTreeIntent;
  InstantExecutionReportPage$Intent$Copy.prototype = Object.create(InstantExecutionReportPage$Intent.prototype);
  InstantExecutionReportPage$Intent$Copy.prototype.constructor = InstantExecutionReportPage$Intent$Copy;
  InstantExecutionReportPage$Intent$SetFilter.prototype = Object.create(InstantExecutionReportPage$Intent.prototype);
  InstantExecutionReportPage$Intent$SetFilter.prototype.constructor = InstantExecutionReportPage$Intent$SetFilter;
  View$Empty.prototype = Object.create(View.prototype);
  View$Empty.prototype.constructor = View$Empty;
  View$Element.prototype = Object.create(View.prototype);
  View$Element.prototype.constructor = View$Element;
  View$MappedView.prototype = Object.create(View.prototype);
  View$MappedView.prototype.constructor = View$MappedView;
  Attribute$OnEvent.prototype = Object.create(Attribute.prototype);
  Attribute$OnEvent.prototype.constructor = Attribute$OnEvent;
  Attribute$ClassName.prototype = Object.create(Attribute.prototype);
  Attribute$ClassName.prototype.constructor = Attribute$ClassName;
  Attribute$Named.prototype = Object.create(Attribute.prototype);
  Attribute$Named.prototype.constructor = Attribute$Named;
  TreeView$Intent$Toggle.prototype = Object.create(TreeView$Intent.prototype);
  TreeView$Intent$Toggle.prototype.constructor = TreeView$Intent$Toggle;
  Tree$ViewState.prototype = Object.create(Enum.prototype);
  Tree$ViewState.prototype.constructor = Tree$ViewState;
  Tree$Focus$Original.prototype = Object.create(Tree$Focus.prototype);
  Tree$Focus$Original.prototype.constructor = Tree$Focus$Original;
  Tree$Focus$Child.prototype = Object.create(Tree$Focus.prototype);
  Tree$Focus$Child.prototype.constructor = Tree$Focus$Child;
  function ProblemNode() {
  }
  function ProblemNode$Error(label) {
    ProblemNode.call(this);
    this.label = label;
  }
  ProblemNode$Error.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Error',
    interfaces: [ProblemNode]
  };
  ProblemNode$Error.prototype.component1 = function () {
    return this.label;
  };
  ProblemNode$Error.prototype.copy_r3l0kh$ = function (label) {
    return new ProblemNode$Error(label === void 0 ? this.label : label);
  };
  ProblemNode$Error.prototype.toString = function () {
    return 'Error(label=' + Kotlin.toString(this.label) + ')';
  };
  ProblemNode$Error.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.label) | 0;
    return result;
  };
  ProblemNode$Error.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.label, other.label))));
  };
  function ProblemNode$Warning(label) {
    ProblemNode.call(this);
    this.label = label;
  }
  ProblemNode$Warning.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Warning',
    interfaces: [ProblemNode]
  };
  ProblemNode$Warning.prototype.component1 = function () {
    return this.label;
  };
  ProblemNode$Warning.prototype.copy_r3l0kh$ = function (label) {
    return new ProblemNode$Warning(label === void 0 ? this.label : label);
  };
  ProblemNode$Warning.prototype.toString = function () {
    return 'Warning(label=' + Kotlin.toString(this.label) + ')';
  };
  ProblemNode$Warning.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.label) | 0;
    return result;
  };
  ProblemNode$Warning.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.label, other.label))));
  };
  function ProblemNode$Task(path, type) {
    ProblemNode.call(this);
    this.path = path;
    this.type = type;
  }
  ProblemNode$Task.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Task',
    interfaces: [ProblemNode]
  };
  ProblemNode$Task.prototype.component1 = function () {
    return this.path;
  };
  ProblemNode$Task.prototype.component2 = function () {
    return this.type;
  };
  ProblemNode$Task.prototype.copy_puj7f4$ = function (path, type) {
    return new ProblemNode$Task(path === void 0 ? this.path : path, type === void 0 ? this.type : type);
  };
  ProblemNode$Task.prototype.toString = function () {
    return 'Task(path=' + Kotlin.toString(this.path) + (', type=' + Kotlin.toString(this.type)) + ')';
  };
  ProblemNode$Task.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.path) | 0;
    result = result * 31 + Kotlin.hashCode(this.type) | 0;
    return result;
  };
  ProblemNode$Task.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.path, other.path) && Kotlin.equals(this.type, other.type)))));
  };
  function ProblemNode$Bean(type) {
    ProblemNode.call(this);
    this.type = type;
  }
  ProblemNode$Bean.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Bean',
    interfaces: [ProblemNode]
  };
  ProblemNode$Bean.prototype.component1 = function () {
    return this.type;
  };
  ProblemNode$Bean.prototype.copy_61zpoe$ = function (type) {
    return new ProblemNode$Bean(type === void 0 ? this.type : type);
  };
  ProblemNode$Bean.prototype.toString = function () {
    return 'Bean(type=' + Kotlin.toString(this.type) + ')';
  };
  ProblemNode$Bean.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.type) | 0;
    return result;
  };
  ProblemNode$Bean.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.type, other.type))));
  };
  function ProblemNode$Property(kind, name, owner) {
    ProblemNode.call(this);
    this.kind = kind;
    this.name = name;
    this.owner = owner;
  }
  ProblemNode$Property.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Property',
    interfaces: [ProblemNode]
  };
  ProblemNode$Property.prototype.component1 = function () {
    return this.kind;
  };
  ProblemNode$Property.prototype.component2 = function () {
    return this.name;
  };
  ProblemNode$Property.prototype.component3 = function () {
    return this.owner;
  };
  ProblemNode$Property.prototype.copy_6hosri$ = function (kind, name, owner) {
    return new ProblemNode$Property(kind === void 0 ? this.kind : kind, name === void 0 ? this.name : name, owner === void 0 ? this.owner : owner);
  };
  ProblemNode$Property.prototype.toString = function () {
    return 'Property(kind=' + Kotlin.toString(this.kind) + (', name=' + Kotlin.toString(this.name)) + (', owner=' + Kotlin.toString(this.owner)) + ')';
  };
  ProblemNode$Property.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.kind) | 0;
    result = result * 31 + Kotlin.hashCode(this.name) | 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    return result;
  };
  ProblemNode$Property.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.kind, other.kind) && Kotlin.equals(this.name, other.name) && Kotlin.equals(this.owner, other.owner)))));
  };
  function ProblemNode$Label(text) {
    ProblemNode.call(this);
    this.text = text;
  }
  ProblemNode$Label.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Label',
    interfaces: [ProblemNode]
  };
  ProblemNode$Label.prototype.component1 = function () {
    return this.text;
  };
  ProblemNode$Label.prototype.copy_61zpoe$ = function (text) {
    return new ProblemNode$Label(text === void 0 ? this.text : text);
  };
  ProblemNode$Label.prototype.toString = function () {
    return 'Label(text=' + Kotlin.toString(this.text) + ')';
  };
  ProblemNode$Label.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.text) | 0;
    return result;
  };
  ProblemNode$Label.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.text, other.text))));
  };
  function ProblemNode$Message(prettyText) {
    ProblemNode.call(this);
    this.prettyText = prettyText;
  }
  ProblemNode$Message.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Message',
    interfaces: [ProblemNode]
  };
  ProblemNode$Message.prototype.component1 = function () {
    return this.prettyText;
  };
  ProblemNode$Message.prototype.copy_8f5aeb$ = function (prettyText) {
    return new ProblemNode$Message(prettyText === void 0 ? this.prettyText : prettyText);
  };
  ProblemNode$Message.prototype.toString = function () {
    return 'Message(prettyText=' + Kotlin.toString(this.prettyText) + ')';
  };
  ProblemNode$Message.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.prettyText) | 0;
    return result;
  };
  ProblemNode$Message.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.prettyText, other.prettyText))));
  };
  function ProblemNode$Exception(stackTrace) {
    ProblemNode.call(this);
    this.stackTrace = stackTrace;
  }
  ProblemNode$Exception.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Exception',
    interfaces: [ProblemNode]
  };
  ProblemNode$Exception.prototype.component1 = function () {
    return this.stackTrace;
  };
  ProblemNode$Exception.prototype.copy_61zpoe$ = function (stackTrace) {
    return new ProblemNode$Exception(stackTrace === void 0 ? this.stackTrace : stackTrace);
  };
  ProblemNode$Exception.prototype.toString = function () {
    return 'Exception(stackTrace=' + Kotlin.toString(this.stackTrace) + ')';
  };
  ProblemNode$Exception.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.stackTrace) | 0;
    return result;
  };
  ProblemNode$Exception.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.stackTrace, other.stackTrace))));
  };
  ProblemNode.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ProblemNode',
    interfaces: []
  };
  function PrettyText(fragments) {
    this.fragments = fragments;
  }
  function PrettyText$Fragment() {
  }
  function PrettyText$Fragment$Text(text) {
    PrettyText$Fragment.call(this);
    this.text = text;
  }
  PrettyText$Fragment$Text.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Text',
    interfaces: [PrettyText$Fragment]
  };
  PrettyText$Fragment$Text.prototype.component1 = function () {
    return this.text;
  };
  PrettyText$Fragment$Text.prototype.copy_61zpoe$ = function (text) {
    return new PrettyText$Fragment$Text(text === void 0 ? this.text : text);
  };
  PrettyText$Fragment$Text.prototype.toString = function () {
    return 'Text(text=' + Kotlin.toString(this.text) + ')';
  };
  PrettyText$Fragment$Text.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.text) | 0;
    return result;
  };
  PrettyText$Fragment$Text.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.text, other.text))));
  };
  function PrettyText$Fragment$Reference(name) {
    PrettyText$Fragment.call(this);
    this.name = name;
  }
  PrettyText$Fragment$Reference.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Reference',
    interfaces: [PrettyText$Fragment]
  };
  PrettyText$Fragment$Reference.prototype.component1 = function () {
    return this.name;
  };
  PrettyText$Fragment$Reference.prototype.copy_61zpoe$ = function (name) {
    return new PrettyText$Fragment$Reference(name === void 0 ? this.name : name);
  };
  PrettyText$Fragment$Reference.prototype.toString = function () {
    return 'Reference(name=' + Kotlin.toString(this.name) + ')';
  };
  PrettyText$Fragment$Reference.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.name) | 0;
    return result;
  };
  PrettyText$Fragment$Reference.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.name, other.name))));
  };
  PrettyText$Fragment.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Fragment',
    interfaces: []
  };
  PrettyText.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'PrettyText',
    interfaces: []
  };
  PrettyText.prototype.component1 = function () {
    return this.fragments;
  };
  PrettyText.prototype.copy_kfe0c6$ = function (fragments) {
    return new PrettyText(fragments === void 0 ? this.fragments : fragments);
  };
  PrettyText.prototype.toString = function () {
    return 'PrettyText(fragments=' + Kotlin.toString(this.fragments) + ')';
  };
  PrettyText.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.fragments) | 0;
    return result;
  };
  PrettyText.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.fragments, other.fragments))));
  };
  function InstantExecutionReportPage() {
    InstantExecutionReportPage_instance = this;
    this.errorIcon_0 = span.invoke_ytbaoo$(' \u274C');
    this.warningIcon_0 = span.invoke_ytbaoo$(' \u26A0\uFE0F');
    this.emptyTreeIcon_0 = span.invoke_h5n6wx$(attributes(InstantExecutionReportPage$emptyTreeIcon$lambda), '\u25A0 ');
  }
  function InstantExecutionReportPage$Model(totalProblems, messageTree, taskTree, displayFilter) {
    if (displayFilter === void 0)
      displayFilter = InstantExecutionReportPage$DisplayFilter$All_getInstance();
    this.totalProblems = totalProblems;
    this.messageTree = messageTree;
    this.taskTree = taskTree;
    this.displayFilter = displayFilter;
  }
  InstantExecutionReportPage$Model.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Model',
    interfaces: []
  };
  InstantExecutionReportPage$Model.prototype.component1 = function () {
    return this.totalProblems;
  };
  InstantExecutionReportPage$Model.prototype.component2 = function () {
    return this.messageTree;
  };
  InstantExecutionReportPage$Model.prototype.component3 = function () {
    return this.taskTree;
  };
  InstantExecutionReportPage$Model.prototype.component4 = function () {
    return this.displayFilter;
  };
  InstantExecutionReportPage$Model.prototype.copy_2gvnba$ = function (totalProblems, messageTree, taskTree, displayFilter) {
    return new InstantExecutionReportPage$Model(totalProblems === void 0 ? this.totalProblems : totalProblems, messageTree === void 0 ? this.messageTree : messageTree, taskTree === void 0 ? this.taskTree : taskTree, displayFilter === void 0 ? this.displayFilter : displayFilter);
  };
  InstantExecutionReportPage$Model.prototype.toString = function () {
    return 'Model(totalProblems=' + Kotlin.toString(this.totalProblems) + (', messageTree=' + Kotlin.toString(this.messageTree)) + (', taskTree=' + Kotlin.toString(this.taskTree)) + (', displayFilter=' + Kotlin.toString(this.displayFilter)) + ')';
  };
  InstantExecutionReportPage$Model.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.totalProblems) | 0;
    result = result * 31 + Kotlin.hashCode(this.messageTree) | 0;
    result = result * 31 + Kotlin.hashCode(this.taskTree) | 0;
    result = result * 31 + Kotlin.hashCode(this.displayFilter) | 0;
    return result;
  };
  InstantExecutionReportPage$Model.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.totalProblems, other.totalProblems) && Kotlin.equals(this.messageTree, other.messageTree) && Kotlin.equals(this.taskTree, other.taskTree) && Kotlin.equals(this.displayFilter, other.displayFilter)))));
  };
  function InstantExecutionReportPage$DisplayFilter(name, ordinal) {
    Enum.call(this);
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function InstantExecutionReportPage$DisplayFilter_initFields() {
    InstantExecutionReportPage$DisplayFilter_initFields = function () {
    };
    InstantExecutionReportPage$DisplayFilter$All_instance = new InstantExecutionReportPage$DisplayFilter('All', 0);
    InstantExecutionReportPage$DisplayFilter$Errors_instance = new InstantExecutionReportPage$DisplayFilter('Errors', 1);
    InstantExecutionReportPage$DisplayFilter$Warnings_instance = new InstantExecutionReportPage$DisplayFilter('Warnings', 2);
  }
  var InstantExecutionReportPage$DisplayFilter$All_instance;
  function InstantExecutionReportPage$DisplayFilter$All_getInstance() {
    InstantExecutionReportPage$DisplayFilter_initFields();
    return InstantExecutionReportPage$DisplayFilter$All_instance;
  }
  var InstantExecutionReportPage$DisplayFilter$Errors_instance;
  function InstantExecutionReportPage$DisplayFilter$Errors_getInstance() {
    InstantExecutionReportPage$DisplayFilter_initFields();
    return InstantExecutionReportPage$DisplayFilter$Errors_instance;
  }
  var InstantExecutionReportPage$DisplayFilter$Warnings_instance;
  function InstantExecutionReportPage$DisplayFilter$Warnings_getInstance() {
    InstantExecutionReportPage$DisplayFilter_initFields();
    return InstantExecutionReportPage$DisplayFilter$Warnings_instance;
  }
  InstantExecutionReportPage$DisplayFilter.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'DisplayFilter',
    interfaces: [Enum]
  };
  function InstantExecutionReportPage$DisplayFilter$values() {
    return [InstantExecutionReportPage$DisplayFilter$All_getInstance(), InstantExecutionReportPage$DisplayFilter$Errors_getInstance(), InstantExecutionReportPage$DisplayFilter$Warnings_getInstance()];
  }
  InstantExecutionReportPage$DisplayFilter.values = InstantExecutionReportPage$DisplayFilter$values;
  function InstantExecutionReportPage$DisplayFilter$valueOf(name) {
    switch (name) {
      case 'All':
        return InstantExecutionReportPage$DisplayFilter$All_getInstance();
      case 'Errors':
        return InstantExecutionReportPage$DisplayFilter$Errors_getInstance();
      case 'Warnings':
        return InstantExecutionReportPage$DisplayFilter$Warnings_getInstance();
      default:throwISE('No enum constant InstantExecutionReportPage.DisplayFilter.' + name);
    }
  }
  InstantExecutionReportPage$DisplayFilter.valueOf_61zpoe$ = InstantExecutionReportPage$DisplayFilter$valueOf;
  function InstantExecutionReportPage$Intent() {
  }
  function InstantExecutionReportPage$Intent$TaskTreeIntent(delegate) {
    InstantExecutionReportPage$Intent.call(this);
    this.delegate = delegate;
  }
  InstantExecutionReportPage$Intent$TaskTreeIntent.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'TaskTreeIntent',
    interfaces: [InstantExecutionReportPage$Intent]
  };
  InstantExecutionReportPage$Intent$TaskTreeIntent.prototype.component1 = function () {
    return this.delegate;
  };
  InstantExecutionReportPage$Intent$TaskTreeIntent.prototype.copy_nskjp0$ = function (delegate) {
    return new InstantExecutionReportPage$Intent$TaskTreeIntent(delegate === void 0 ? this.delegate : delegate);
  };
  InstantExecutionReportPage$Intent$TaskTreeIntent.prototype.toString = function () {
    return 'TaskTreeIntent(delegate=' + Kotlin.toString(this.delegate) + ')';
  };
  InstantExecutionReportPage$Intent$TaskTreeIntent.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.delegate) | 0;
    return result;
  };
  InstantExecutionReportPage$Intent$TaskTreeIntent.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.delegate, other.delegate))));
  };
  function InstantExecutionReportPage$Intent$MessageTreeIntent(delegate) {
    InstantExecutionReportPage$Intent.call(this);
    this.delegate = delegate;
  }
  InstantExecutionReportPage$Intent$MessageTreeIntent.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'MessageTreeIntent',
    interfaces: [InstantExecutionReportPage$Intent]
  };
  InstantExecutionReportPage$Intent$MessageTreeIntent.prototype.component1 = function () {
    return this.delegate;
  };
  InstantExecutionReportPage$Intent$MessageTreeIntent.prototype.copy_nskjp0$ = function (delegate) {
    return new InstantExecutionReportPage$Intent$MessageTreeIntent(delegate === void 0 ? this.delegate : delegate);
  };
  InstantExecutionReportPage$Intent$MessageTreeIntent.prototype.toString = function () {
    return 'MessageTreeIntent(delegate=' + Kotlin.toString(this.delegate) + ')';
  };
  InstantExecutionReportPage$Intent$MessageTreeIntent.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.delegate) | 0;
    return result;
  };
  InstantExecutionReportPage$Intent$MessageTreeIntent.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.delegate, other.delegate))));
  };
  function InstantExecutionReportPage$Intent$Copy(text) {
    InstantExecutionReportPage$Intent.call(this);
    this.text = text;
  }
  InstantExecutionReportPage$Intent$Copy.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Copy',
    interfaces: [InstantExecutionReportPage$Intent]
  };
  InstantExecutionReportPage$Intent$Copy.prototype.component1 = function () {
    return this.text;
  };
  InstantExecutionReportPage$Intent$Copy.prototype.copy_61zpoe$ = function (text) {
    return new InstantExecutionReportPage$Intent$Copy(text === void 0 ? this.text : text);
  };
  InstantExecutionReportPage$Intent$Copy.prototype.toString = function () {
    return 'Copy(text=' + Kotlin.toString(this.text) + ')';
  };
  InstantExecutionReportPage$Intent$Copy.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.text) | 0;
    return result;
  };
  InstantExecutionReportPage$Intent$Copy.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.text, other.text))));
  };
  function InstantExecutionReportPage$Intent$SetFilter(displayFilter) {
    InstantExecutionReportPage$Intent.call(this);
    this.displayFilter = displayFilter;
  }
  InstantExecutionReportPage$Intent$SetFilter.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'SetFilter',
    interfaces: [InstantExecutionReportPage$Intent]
  };
  InstantExecutionReportPage$Intent$SetFilter.prototype.component1 = function () {
    return this.displayFilter;
  };
  InstantExecutionReportPage$Intent$SetFilter.prototype.copy_ihlpwm$ = function (displayFilter) {
    return new InstantExecutionReportPage$Intent$SetFilter(displayFilter === void 0 ? this.displayFilter : displayFilter);
  };
  InstantExecutionReportPage$Intent$SetFilter.prototype.toString = function () {
    return 'SetFilter(displayFilter=' + Kotlin.toString(this.displayFilter) + ')';
  };
  InstantExecutionReportPage$Intent$SetFilter.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.displayFilter) | 0;
    return result;
  };
  InstantExecutionReportPage$Intent$SetFilter.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.displayFilter, other.displayFilter))));
  };
  InstantExecutionReportPage$Intent.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Intent',
    interfaces: []
  };
  InstantExecutionReportPage.prototype.step_xwfjod$ = function (intent, model) {
    if (Kotlin.isType(intent, InstantExecutionReportPage$Intent$TaskTreeIntent))
      return model.copy_2gvnba$(void 0, void 0, TreeView_getInstance().step_byll7j$(intent.delegate, model.taskTree));
    else if (Kotlin.isType(intent, InstantExecutionReportPage$Intent$MessageTreeIntent))
      return model.copy_2gvnba$(void 0, TreeView_getInstance().step_byll7j$(intent.delegate, model.messageTree));
    else if (Kotlin.isType(intent, InstantExecutionReportPage$Intent$Copy)) {
      window.navigator.clipboard.writeText(intent.text);
      return model;
    }
     else if (Kotlin.isType(intent, InstantExecutionReportPage$Intent$SetFilter))
      return model.copy_2gvnba$(void 0, void 0, void 0, intent.displayFilter);
    else
      return Kotlin.noWhenBranchMatched();
  };
  function InstantExecutionReportPage$view$lambda($receiver) {
    $receiver.className_61zpoe$('container');
    return Unit;
  }
  function InstantExecutionReportPage$view$lambda_0($receiver) {
    $receiver.className_61zpoe$('right');
    return Unit;
  }
  function InstantExecutionReportPage$view$lambda_1($receiver) {
    $receiver.className_61zpoe$('left');
    return Unit;
  }
  InstantExecutionReportPage.prototype.view_11rb$ = function (model) {
    return div.invoke_iwuj7m$(attributes(InstantExecutionReportPage$view$lambda), [div.invoke_ecd2jv$([div.invoke_iwuj7m$(attributes(InstantExecutionReportPage$view$lambda_0), [div.invoke_ecd2jv$([this.displayFilterButton_0(InstantExecutionReportPage$DisplayFilter$All_getInstance(), model.displayFilter), this.displayFilterButton_0(InstantExecutionReportPage$DisplayFilter$Errors_getInstance(), model.displayFilter), this.displayFilterButton_0(InstantExecutionReportPage$DisplayFilter$Warnings_getInstance(), model.displayFilter)])]), div.invoke_iwuj7m$(attributes(InstantExecutionReportPage$view$lambda_1), [h1.invoke_ytbaoo$(model.totalProblems.toString() + ' instant execution problems were found'), this.learnMore_0(), this.viewTree_0(model.messageTree, getCallableRef('MessageTreeIntent', function (delegate) {
      return new InstantExecutionReportPage$Intent$MessageTreeIntent(delegate);
    }), model.displayFilter), this.viewTree_0(model.taskTree, getCallableRef('TaskTreeIntent', function (delegate) {
      return new InstantExecutionReportPage$Intent$TaskTreeIntent(delegate);
    }), model.displayFilter)])])]);
  };
  function InstantExecutionReportPage$displayFilterButton$lambda$lambda(closure$displayFilter) {
    return function (it) {
      return new InstantExecutionReportPage$Intent$SetFilter(closure$displayFilter);
    };
  }
  function InstantExecutionReportPage$displayFilterButton$lambda(closure$displayFilter, closure$activeFilter) {
    return function ($receiver) {
      $receiver.className_61zpoe$('btn');
      if (closure$displayFilter === closure$activeFilter) {
        $receiver.className_61zpoe$('btn-active');
      }
      $receiver.onClick_os4ted$(InstantExecutionReportPage$displayFilterButton$lambda$lambda(closure$displayFilter));
      return Unit;
    };
  }
  InstantExecutionReportPage.prototype.displayFilterButton_0 = function (displayFilter, activeFilter) {
    return span.invoke_h5n6wx$(attributes(InstantExecutionReportPage$displayFilterButton$lambda(displayFilter, activeFilter)), displayFilter.name);
  };
  function InstantExecutionReportPage$learnMore$lambda($receiver) {
    $receiver.href_61zpoe$('https://gradle.github.io/instant-execution/');
    return Unit;
  }
  InstantExecutionReportPage.prototype.learnMore_0 = function () {
    return div.invoke_ecd2jv$([span.invoke_ytbaoo$('Learn more about '), a.invoke_h5n6wx$(attributes(InstantExecutionReportPage$learnMore$lambda), 'Gradle Instant Execution'), span.invoke_ytbaoo$('.')]);
  };
  function InstantExecutionReportPage$viewTree$lambda(closure$treeIntent, this$InstantExecutionReportPage) {
    return function (child) {
      var node = child.tree.label;
      if (Kotlin.isType(node, ProblemNode$Error))
        return this$InstantExecutionReportPage.viewLabel_0(closure$treeIntent, child, node.label, this$InstantExecutionReportPage.errorIcon_0);
      else if (Kotlin.isType(node, ProblemNode$Warning))
        return this$InstantExecutionReportPage.viewLabel_0(closure$treeIntent, child, node.label, this$InstantExecutionReportPage.warningIcon_0);
      else if (Kotlin.isType(node, ProblemNode$Exception))
        return this$InstantExecutionReportPage.viewException_0(closure$treeIntent, child, node);
      else {
        return this$InstantExecutionReportPage.viewLabel_0(closure$treeIntent, child, node);
      }
    };
  }
  InstantExecutionReportPage.prototype.viewTree_0 = function (model, treeIntent, displayFilter) {
    return div.invoke_ecd2jv$([h2.invoke_ytbaoo$(model.tree.label.text), ol.invoke_wmgtvr$(viewSubTrees(this.applyFilter_0(displayFilter, model), InstantExecutionReportPage$viewTree$lambda(treeIntent, this)))]);
  };
  function InstantExecutionReportPage$applyFilter$lambda(it) {
    return Kotlin.isType(it.tree.label, ProblemNode$Error);
  }
  function InstantExecutionReportPage$applyFilter$lambda_0(it) {
    return Kotlin.isType(it.tree.label, ProblemNode$Warning);
  }
  InstantExecutionReportPage.prototype.applyFilter_0 = function (displayFilter, model) {
    var tmp$;
    var children = model.tree.focus().children;
    switch (displayFilter.name) {
      case 'All':
        tmp$ = children;
        break;
      case 'Errors':
        tmp$ = filter(children, InstantExecutionReportPage$applyFilter$lambda);
        break;
      case 'Warnings':
        tmp$ = filter(children, InstantExecutionReportPage$applyFilter$lambda_0);
        break;
      default:tmp$ = Kotlin.noWhenBranchMatched();
        break;
    }
    return tmp$;
  };
  InstantExecutionReportPage.prototype.viewNode_0 = function (node) {
    if (Kotlin.isType(node, ProblemNode$Property))
      return span.invoke_ecd2jv$([span.invoke_ytbaoo$(node.kind), this.reference_0(node.name), span.invoke_ytbaoo$(' of '), this.reference_0(node.owner)]);
    else if (Kotlin.isType(node, ProblemNode$Task))
      return span.invoke_ecd2jv$([span.invoke_ytbaoo$('task'), this.reference_0(node.path), span.invoke_ytbaoo$(' of type '), this.reference_0(node.type)]);
    else if (Kotlin.isType(node, ProblemNode$Bean))
      return span.invoke_ecd2jv$([span.invoke_ytbaoo$('bean of type '), this.reference_0(node.type)]);
    else if (Kotlin.isType(node, ProblemNode$Label))
      return span.invoke_ytbaoo$(node.text);
    else if (Kotlin.isType(node, ProblemNode$Message))
      return this.viewPrettyText_0(node.prettyText);
    else
      return span.invoke_ytbaoo$(node.toString());
  };
  InstantExecutionReportPage.prototype.viewLabel_0 = function (treeIntent, child, label, decoration) {
    if (decoration === void 0)
      decoration = empty;
    return div.invoke_ecd2jv$([this.treeButtonFor_0(child, treeIntent), decoration, span.invoke_ytbaoo$(' '), this.viewNode_0(label)]);
  };
  InstantExecutionReportPage.prototype.treeButtonFor_0 = function (child, treeIntent) {
    if (child.tree.isNotEmpty())
      return this.viewTreeButton_0(child, treeIntent);
    else
      return this.emptyTreeIcon_0;
  };
  function InstantExecutionReportPage$viewTreeButton$lambda$lambda(closure$treeIntent, closure$child) {
    return function (it) {
      return closure$treeIntent(new TreeView$Intent$Toggle(closure$child));
    };
  }
  function InstantExecutionReportPage$viewTreeButton$lambda(closure$child, this$InstantExecutionReportPage, closure$treeIntent) {
    return function ($receiver) {
      $receiver.className_61zpoe$('tree-btn');
      $receiver.title_61zpoe$('Click to ' + this$InstantExecutionReportPage.toggleVerb_0(closure$child.tree.state));
      $receiver.onClick_os4ted$(InstantExecutionReportPage$viewTreeButton$lambda$lambda(closure$treeIntent, closure$child));
      return Unit;
    };
  }
  InstantExecutionReportPage.prototype.viewTreeButton_0 = function (child, treeIntent) {
    var tmp$, tmp$_0;
    tmp$ = attributes(InstantExecutionReportPage$viewTreeButton$lambda(child, this, treeIntent));
    switch (child.tree.state.name) {
      case 'Collapsed':
        tmp$_0 = '\u25BA ';
        break;
      case 'Expanded':
        tmp$_0 = '\u25BC ';
        break;
      default:tmp$_0 = Kotlin.noWhenBranchMatched();
        break;
    }
    return span.invoke_h5n6wx$(tmp$, tmp$_0);
  };
  InstantExecutionReportPage.prototype.viewPrettyText_0 = function (text) {
    var tmp$ = span;
    var $receiver = text.fragments;
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$_0;
    tmp$_0 = $receiver.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1 = destination.add_11rb$;
      var transform$result;
      if (Kotlin.isType(item, PrettyText$Fragment$Text)) {
        transform$result = span.invoke_ytbaoo$(item.text);
      }
       else if (Kotlin.isType(item, PrettyText$Fragment$Reference)) {
        transform$result = this.reference_0(item.name);
      }
       else {
        transform$result = Kotlin.noWhenBranchMatched();
      }
      tmp$_1.call(destination, transform$result);
    }
    return tmp$.invoke_wmgtvr$(destination);
  };
  InstantExecutionReportPage.prototype.reference_0 = function (name) {
    return span.invoke_ecd2jv$([code.invoke_ytbaoo$(name), this.copyButton_0(name, 'Copy reference to the clipboard')]);
  };
  function InstantExecutionReportPage$copyButton$lambda$lambda(closure$text) {
    return function (it) {
      return new InstantExecutionReportPage$Intent$Copy(closure$text);
    };
  }
  function InstantExecutionReportPage$copyButton$lambda(closure$tooltip, closure$text) {
    return function ($receiver) {
      $receiver.title_61zpoe$(closure$tooltip);
      $receiver.className_61zpoe$('copy-button');
      $receiver.onClick_os4ted$(InstantExecutionReportPage$copyButton$lambda$lambda(closure$text));
      return Unit;
    };
  }
  InstantExecutionReportPage.prototype.copyButton_0 = function (text, tooltip) {
    return small.invoke_h5n6wx$(attributes(InstantExecutionReportPage$copyButton$lambda(tooltip, text)), '\uD83D\uDCCB');
  };
  function InstantExecutionReportPage$viewException$lambda($receiver) {
    $receiver.className_61zpoe$('stacktrace');
    return Unit;
  }
  InstantExecutionReportPage.prototype.viewException_0 = function (treeIntent, child, node) {
    var tmp$, tmp$_0, tmp$_1, tmp$_2;
    tmp$ = this.viewTreeButton_0(child, treeIntent);
    tmp$_0 = span.invoke_ytbaoo$('exception stack trace ');
    tmp$_1 = this.copyButton_0(node.stackTrace, 'Copy original stacktrace to the clipboard');
    switch (child.tree.state.name) {
      case 'Collapsed':
        tmp$_2 = empty;
        break;
      case 'Expanded':
        tmp$_2 = pre.invoke_h5n6wx$(attributes(InstantExecutionReportPage$viewException$lambda), node.stackTrace);
        break;
      default:tmp$_2 = Kotlin.noWhenBranchMatched();
        break;
    }
    return div.invoke_ecd2jv$([tmp$, tmp$_0, tmp$_1, tmp$_2]);
  };
  InstantExecutionReportPage.prototype.toggleVerb_0 = function (state) {
    switch (state.name) {
      case 'Collapsed':
        return 'expand';
      case 'Expanded':
        return 'collapse';
      default:return Kotlin.noWhenBranchMatched();
    }
  };
  function InstantExecutionReportPage$emptyTreeIcon$lambda($receiver) {
    $receiver.className_61zpoe$('tree-icon');
    return Unit;
  }
  InstantExecutionReportPage.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'InstantExecutionReportPage',
    interfaces: [Component]
  };
  var InstantExecutionReportPage_instance = null;
  function InstantExecutionReportPage_getInstance() {
    if (InstantExecutionReportPage_instance === null) {
      new InstantExecutionReportPage();
    }
    return InstantExecutionReportPage_instance;
  }
  function Comparator$ObjectLiteral(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  var compareBy$lambda = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function main() {
    mountComponentAt(elementById('report'), InstantExecutionReportPage_getInstance(), reportPageModelFromJsModel(instantExecutionProblems()));
  }
  function ImportedProblem(problem, message, trace) {
    this.problem = problem;
    this.message = message;
    this.trace = trace;
  }
  ImportedProblem.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ImportedProblem',
    interfaces: []
  };
  ImportedProblem.prototype.component1 = function () {
    return this.problem;
  };
  ImportedProblem.prototype.component2 = function () {
    return this.message;
  };
  ImportedProblem.prototype.component3 = function () {
    return this.trace;
  };
  ImportedProblem.prototype.copy_cqdesj$ = function (problem, message, trace) {
    return new ImportedProblem(problem === void 0 ? this.problem : problem, message === void 0 ? this.message : message, trace === void 0 ? this.trace : trace);
  };
  ImportedProblem.prototype.toString = function () {
    return 'ImportedProblem(problem=' + Kotlin.toString(this.problem) + (', message=' + Kotlin.toString(this.message)) + (', trace=' + Kotlin.toString(this.trace)) + ')';
  };
  ImportedProblem.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.problem) | 0;
    result = result * 31 + Kotlin.hashCode(this.message) | 0;
    result = result * 31 + Kotlin.hashCode(this.trace) | 0;
    return result;
  };
  ImportedProblem.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.problem, other.problem) && Kotlin.equals(this.message, other.message) && Kotlin.equals(this.trace, other.trace)))));
  };
  function reportPageModelFromJsModel(jsProblems) {
    var destination = ArrayList_init(jsProblems.length);
    var tmp$;
    for (tmp$ = 0; tmp$ !== jsProblems.length; ++tmp$) {
      var item = jsProblems[tmp$];
      var tmp$_0 = destination.add_11rb$;
      var tmp$_1 = toPrettyText(item.message);
      var $receiver = item.trace;
      var destination_0 = ArrayList_init($receiver.length);
      var tmp$_2;
      for (tmp$_2 = 0; tmp$_2 !== $receiver.length; ++tmp$_2) {
        var item_0 = $receiver[tmp$_2];
        destination_0.add_11rb$(toProblemNode(item_0));
      }
      tmp$_0.call(destination, new ImportedProblem(item, tmp$_1, destination_0));
    }
    var problems = destination;
    return new InstantExecutionReportPage$Model(jsProblems.length, treeModelFor(new ProblemNode$Label('Problems grouped by message'), problemNodesByMessage(problems)), treeModelFor(new ProblemNode$Label('Problems grouped by task'), problemNodesByTask(problems)));
  }
  function problemNodesByMessage$lambda(imported) {
    var $receiver = ArrayList_init_0();
    var tmp$;
    $receiver.add_11rb$(errorOrWarningNodeFor(imported.problem, messageNodeFor(imported)));
    var tmp$_0;
    tmp$_0 = imported.trace.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      $receiver.add_11rb$(element);
    }
    if ((tmp$ = exceptionNodeFor(imported.problem)) != null) {
      $receiver.add_11rb$(tmp$);
    }
    return $receiver;
  }
  function problemNodesByMessage(problems) {
    return map(asSequence(problems), problemNodesByMessage$lambda);
  }
  function problemNodesByTask$lambda(imported) {
    var $receiver = asReversed(imported.trace);
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$, tmp$_0;
    var index = 0;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var tmp$_1 = destination.add_11rb$;
      var transform$result;
      if (checkIndexOverflow((tmp$_0 = index, index = tmp$_0 + 1 | 0, tmp$_0)) === 0) {
        transform$result = errorOrWarningNodeFor(imported.problem, item);
      }
       else {
        transform$result = item;
      }
      tmp$_1.call(destination, transform$result);
    }
    return plus(destination, exceptionOrMessageNodeFor(imported));
  }
  function problemNodesByTask(problems) {
    return map(asSequence(problems), problemNodesByTask$lambda);
  }
  function toPrettyText(message) {
    var destination = ArrayList_init(message.length);
    var tmp$;
    for (tmp$ = 0; tmp$ !== message.length; ++tmp$) {
      var item = message[tmp$];
      var tmp$_0, tmp$_1, tmp$_2, tmp$_3;
      destination.add_11rb$((tmp$_3 = (tmp$_2 = (tmp$_0 = item.text) != null ? new PrettyText$Fragment$Text(tmp$_0) : null) != null ? tmp$_2 : (tmp$_1 = item.name) != null ? new PrettyText$Fragment$Reference(tmp$_1) : null) != null ? tmp$_3 : new PrettyText$Fragment$Text('Unrecognised message fragment: ' + JSON.stringify(item)));
    }
    return new PrettyText(destination);
  }
  function toProblemNode(trace) {
    switch (trace.kind) {
      case 'Task':
        return new ProblemNode$Task(trace.path, trace.type);
      case 'Bean':
        return new ProblemNode$Bean(trace.type);
      case 'Field':
        return new ProblemNode$Property('field', trace.name, trace.declaringType);
      case 'InputProperty':
        return new ProblemNode$Property('input property', trace.name, trace.task);
      case 'OutputProperty':
        return new ProblemNode$Property('output property', trace.name, trace.task);
      default:return new ProblemNode$Label('Gradle runtime');
    }
  }
  function errorOrWarningNodeFor(problem, label) {
    var tmp$;
    return (tmp$ = problem.error != null ? new ProblemNode$Error(label) : null) != null ? tmp$ : new ProblemNode$Warning(label);
  }
  function exceptionOrMessageNodeFor(importedProblem) {
    var tmp$;
    return (tmp$ = exceptionNodeFor(importedProblem.problem)) != null ? tmp$ : messageNodeFor(importedProblem);
  }
  function messageNodeFor(importedProblem) {
    return new ProblemNode$Message(importedProblem.message);
  }
  function exceptionNodeFor(it) {
    var tmp$;
    return (tmp$ = it.error) != null ? new ProblemNode$Exception(tmp$) : null;
  }
  function treeModelFor(label, sequence) {
    return new TreeView$Model(treeFromTrie(label, Trie$Companion_getInstance().from_fn8g3a$(sequence), Tree$ViewState$Collapsed_getInstance()));
  }
  function treeFromTrie(label, trie, state) {
    var subTreeState = trie.size === 1 ? Tree$ViewState$Expanded_getInstance() : Tree$ViewState$Collapsed_getInstance();
    return new Tree(label, subTreesFromTrie(trie, subTreeState), trie.size === 0 ? Tree$ViewState$Collapsed_getInstance() : state);
  }
  function subTreesFromTrie$lambda(f) {
    var label = f.component1();
    return toString(label);
  }
  function subTreesFromTrie$lambda_0(closure$state) {
    return function (f) {
      var label = f.component1()
      , subTrie = f.component2();
      return treeFromTrie(label, subTrie, closure$state);
    };
  }
  function subTreesFromTrie(trie, state) {
    return toList(map(sortedWith(trie.entries, new Comparator$ObjectLiteral(compareBy$lambda(subTreesFromTrie$lambda))), subTreesFromTrie$lambda_0(state)));
  }
  var mapAt = defineInlineFunction('instant-execution-report.data.mapAt_5avcl8$', wrapFunction(function () {
    var collectionSizeOrDefault = Kotlin.kotlin.collections.collectionSizeOrDefault_ba2ldo$;
    var ArrayList_init = Kotlin.kotlin.collections.ArrayList_init_ww73n8$;
    var checkIndexOverflow = Kotlin.kotlin.collections.checkIndexOverflow_za3lpa$;
    return function ($receiver, index, transform) {
      var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
      var tmp$, tmp$_0;
      var index_0 = 0;
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var item = tmp$.next();
        destination.add_11rb$(index === checkIndexOverflow((tmp$_0 = index_0, index_0 = tmp$_0 + 1 | 0, tmp$_0)) ? transform(item) : item);
      }
      return destination;
    };
  }));
  function Trie(nestedMaps) {
    Trie$Companion_getInstance();
    this.nestedMaps_0 = nestedMaps;
  }
  function Trie$Companion() {
    Trie$Companion_instance = this;
  }
  Trie$Companion.prototype.from_fn8g3a$ = function (paths) {
    return new Trie(nestedMapsFrom(paths));
  };
  Trie$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Trie$Companion_instance = null;
  function Trie$Companion_getInstance() {
    if (Trie$Companion_instance === null) {
      new Trie$Companion();
    }
    return Trie$Companion_instance;
  }
  function Trie$get_Trie$entries$lambda(f) {
    var label = f.key;
    var subTrie = f.value;
    var tmp$;
    return to(label, new Trie(Kotlin.isType(tmp$ = subTrie, Map) ? tmp$ : throwCCE()));
  }
  Object.defineProperty(Trie.prototype, 'entries', {
    get: function () {
      return map(asSequence_0(this.nestedMaps_0), Trie$get_Trie$entries$lambda);
    }
  });
  Object.defineProperty(Trie.prototype, 'size', {
    get: function () {
      return this.nestedMaps_0.size;
    }
  });
  Trie.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Trie',
    interfaces: []
  };
  Trie.prototype.unbox = function () {
    return this.nestedMaps_0;
  };
  Trie.prototype.toString = function () {
    return 'Trie(nestedMaps=' + Kotlin.toString(this.nestedMaps_0) + ')';
  };
  Trie.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.nestedMaps_0) | 0;
    return result;
  };
  Trie.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.nestedMaps_0, other.nestedMaps_0))));
  };
  function nestedMapsFrom(paths) {
    var tmp$, tmp$_0;
    var root = HashMap_init();
    tmp$ = paths.iterator();
    while (tmp$.hasNext()) {
      var path = tmp$.next();
      var node = root;
      tmp$_0 = path.iterator();
      while (tmp$_0.hasNext()) {
        var segment = tmp$_0.next();
        var $receiver = node;
        var tmp$_1;
        var value = $receiver.get_11rb$(segment);
        if (value == null) {
          var answer = HashMap_init();
          $receiver.put_xwzc9p$(segment, answer);
          tmp$_1 = answer;
        }
         else {
          tmp$_1 = value;
        }
        var tmp$_2;
        node = Kotlin.isType(tmp$_2 = tmp$_1, HashMap) ? tmp$_2 : throwCCE();
      }
    }
    return root;
  }
  function uncheckedCast(T_0, isT, $receiver) {
    var tmp$;
    return isT(tmp$ = $receiver) ? tmp$ : throwCCE();
  }
  function Component() {
  }
  Component.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'Component',
    interfaces: []
  };
  function component$ObjectLiteral(closure$view, closure$step) {
    this.closure$view = closure$view;
    this.closure$step = closure$step;
  }
  component$ObjectLiteral.prototype.view_11rb$ = function (model) {
    return this.closure$view(model);
  };
  component$ObjectLiteral.prototype.step_xwfjod$ = function (intent, model) {
    return this.closure$step(intent, model);
  };
  component$ObjectLiteral.$metadata$ = {
    kind: Kind_CLASS,
    interfaces: [Component]
  };
  function component(view, step) {
    return new component$ObjectLiteral(view, step);
  }
  function mountComponentAt$loop$lambda(closure$component, closure$model, closure$loop) {
    return function (intent) {
      closure$loop(closure$component.step_xwfjod$(intent, closure$model));
      return Unit;
    };
  }
  function mountComponentAt$loop(closure$component, closure$element) {
    return function closure$loop(model) {
      render(closure$component.view_11rb$(model), closure$element, mountComponentAt$loop$lambda(closure$component, model, closure$loop));
    };
  }
  function mountComponentAt(element, component, init) {
    var loop = mountComponentAt$loop(component, element);
    loop(init);
    println('Component mounted at #' + element.id + '.');
  }
  function elementById(id) {
    var tmp$;
    tmp$ = document.getElementById(id);
    if (tmp$ == null) {
      throw IllegalStateException_init("'" + id + "' element missing");
    }
    return tmp$;
  }
  var empty;
  var h1;
  var h2;
  var div;
  var pre;
  var code;
  var span;
  var small;
  var ol;
  var ul;
  var li;
  var a;
  function render(view, into, send) {
    into.innerHTML = '';
    appendElementFor(into, view, send);
  }
  function ViewFactory(elementName) {
    this.elementName = elementName;
  }
  ViewFactory.prototype.invoke_ytbaoo$ = function (innerText) {
    return View$Companion_getInstance().invoke_otaziq$(this.elementName, void 0, innerText);
  };
  ViewFactory.prototype.invoke_wmgtvr$ = function (children) {
    return View$Companion_getInstance().invoke_otaziq$(this.elementName, void 0, void 0, children);
  };
  ViewFactory.prototype.invoke_ecd2jv$ = function (children) {
    return View$Companion_getInstance().invoke_otaziq$(this.elementName, void 0, void 0, asList(children));
  };
  ViewFactory.prototype.invoke_iwuj7m$ = function (attributes, children) {
    return View$Companion_getInstance().invoke_otaziq$(this.elementName, attributes, void 0, asList(children));
  };
  ViewFactory.prototype.invoke_h5n6wx$ = function (attributes, innerText) {
    return View$Companion_getInstance().invoke_otaziq$(this.elementName, attributes, innerText);
  };
  ViewFactory.prototype.invoke_9goo8t$ = function (innerText, children) {
    return View$Companion_getInstance().invoke_otaziq$(this.elementName, void 0, innerText, asList(children));
  };
  ViewFactory.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ViewFactory',
    interfaces: []
  };
  ViewFactory.prototype.component1 = function () {
    return this.elementName;
  };
  ViewFactory.prototype.copy_61zpoe$ = function (elementName) {
    return new ViewFactory(elementName === void 0 ? this.elementName : elementName);
  };
  ViewFactory.prototype.toString = function () {
    return 'ViewFactory(elementName=' + Kotlin.toString(this.elementName) + ')';
  };
  ViewFactory.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.elementName) | 0;
    return result;
  };
  ViewFactory.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.elementName, other.elementName))));
  };
  function View() {
    View$Companion_getInstance();
  }
  View.prototype.map_2o04qz$ = function (f) {
    return new View$MappedView(this, f);
  };
  function View$Companion() {
    View$Companion_instance = this;
  }
  View$Companion.prototype.invoke_otaziq$ = function (elementName, attributes, innerText, children) {
    if (attributes === void 0)
      attributes = emptyList();
    if (innerText === void 0)
      innerText = null;
    if (children === void 0)
      children = emptyList();
    return new View$Element(elementName, attributes, innerText, children);
  };
  View$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var View$Companion_instance = null;
  function View$Companion_getInstance() {
    if (View$Companion_instance === null) {
      new View$Companion();
    }
    return View$Companion_instance;
  }
  function View$Empty() {
    View$Empty_instance = this;
    View.call(this);
  }
  View$Empty.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Empty',
    interfaces: [View]
  };
  var View$Empty_instance = null;
  function View$Empty_getInstance() {
    if (View$Empty_instance === null) {
      new View$Empty();
    }
    return View$Empty_instance;
  }
  function View$Element(elementName, attributes, innerText, children) {
    if (attributes === void 0)
      attributes = emptyList();
    if (innerText === void 0)
      innerText = null;
    if (children === void 0)
      children = emptyList();
    View.call(this);
    this.elementName = elementName;
    this.attributes = attributes;
    this.innerText = innerText;
    this.children = children;
  }
  View$Element.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Element',
    interfaces: [View]
  };
  View$Element.prototype.component1 = function () {
    return this.elementName;
  };
  View$Element.prototype.component2 = function () {
    return this.attributes;
  };
  View$Element.prototype.component3 = function () {
    return this.innerText;
  };
  View$Element.prototype.component4 = function () {
    return this.children;
  };
  View$Element.prototype.copy_vd9jms$ = function (elementName, attributes, innerText, children) {
    return new View$Element(elementName === void 0 ? this.elementName : elementName, attributes === void 0 ? this.attributes : attributes, innerText === void 0 ? this.innerText : innerText, children === void 0 ? this.children : children);
  };
  View$Element.prototype.toString = function () {
    return 'Element(elementName=' + Kotlin.toString(this.elementName) + (', attributes=' + Kotlin.toString(this.attributes)) + (', innerText=' + Kotlin.toString(this.innerText)) + (', children=' + Kotlin.toString(this.children)) + ')';
  };
  View$Element.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.elementName) | 0;
    result = result * 31 + Kotlin.hashCode(this.attributes) | 0;
    result = result * 31 + Kotlin.hashCode(this.innerText) | 0;
    result = result * 31 + Kotlin.hashCode(this.children) | 0;
    return result;
  };
  View$Element.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.elementName, other.elementName) && Kotlin.equals(this.attributes, other.attributes) && Kotlin.equals(this.innerText, other.innerText) && Kotlin.equals(this.children, other.children)))));
  };
  function View$MappedView(view, mapping) {
    View.call(this);
    this.view = view;
    this.mapping = mapping;
  }
  View$MappedView.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'MappedView',
    interfaces: [View]
  };
  View$MappedView.prototype.component1 = function () {
    return this.view;
  };
  View$MappedView.prototype.component2 = function () {
    return this.mapping;
  };
  View$MappedView.prototype.copy_cezeno$ = function (view, mapping) {
    return new View$MappedView(view === void 0 ? this.view : view, mapping === void 0 ? this.mapping : mapping);
  };
  View$MappedView.prototype.toString = function () {
    return 'MappedView(view=' + Kotlin.toString(this.view) + (', mapping=' + Kotlin.toString(this.mapping)) + ')';
  };
  View$MappedView.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.view) | 0;
    result = result * 31 + Kotlin.hashCode(this.mapping) | 0;
    return result;
  };
  View$MappedView.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.view, other.view) && Kotlin.equals(this.mapping, other.mapping)))));
  };
  View.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'View',
    interfaces: []
  };
  function attributes$lambda$lambda(closure$attributes) {
    return function (it) {
      closure$attributes.add_11rb$(it);
      return Unit;
    };
  }
  function attributes(builder) {
    var $receiver = ArrayList_init_0();
    builder(new Attributes(attributes$lambda$lambda($receiver)));
    return $receiver;
  }
  function Attributes(add) {
    this.add_0 = add;
  }
  Attributes.prototype.onClick_os4ted$ = function (handler) {
    this.add_0(new Attribute$OnEvent('click', handler));
  };
  Attributes.prototype.className_61zpoe$ = function (value) {
    this.add_0(new Attribute$ClassName(value));
  };
  Attributes.prototype.title_61zpoe$ = function (value) {
    this.add_0(new Attribute$Named('title', value));
  };
  Attributes.prototype.href_61zpoe$ = function (value) {
    this.add_0(new Attribute$Named('href', value));
  };
  Attributes.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Attributes',
    interfaces: []
  };
  function Attribute() {
  }
  function Attribute$OnEvent(eventName, handler) {
    Attribute.call(this);
    this.eventName = eventName;
    this.handler = handler;
  }
  Attribute$OnEvent.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'OnEvent',
    interfaces: [Attribute]
  };
  function Attribute$ClassName(value) {
    Attribute.call(this);
    this.value = value;
  }
  Attribute$ClassName.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ClassName',
    interfaces: [Attribute]
  };
  function Attribute$Named(name, value) {
    Attribute.call(this);
    this.name = name;
    this.value = value;
  }
  Attribute$Named.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Named',
    interfaces: [Attribute]
  };
  Attribute.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Attribute',
    interfaces: []
  };
  function appendElementFor$lambda$lambda$lambda(closure$send, closure$a) {
    return function (event) {
      event.stopPropagation();
      closure$send(closure$a.handler(event));
      return Unit;
    };
  }
  function appendElementFor$lambda(closure$view, closure$send) {
    return function ($receiver) {
      var tmp$;
      if ((tmp$ = closure$view.innerText) != null) {
        getCallableRef('appendText', function ($receiver, text) {
          return appendText($receiver, text);
        }.bind(null, $receiver))(tmp$);
      }
      var $receiver_0 = closure$view.children;
      var tmp$_0;
      tmp$_0 = $receiver_0.iterator();
      while (tmp$_0.hasNext()) {
        var element = tmp$_0.next();
        appendElementFor($receiver, element, closure$send);
      }
      var $receiver_1 = closure$view.attributes;
      var tmp$_1;
      tmp$_1 = $receiver_1.iterator();
      while (tmp$_1.hasNext()) {
        var element_0 = tmp$_1.next();
        var closure$send_0 = closure$send;
        if (Kotlin.isType(element_0, Attribute$OnEvent))
          $receiver.addEventListener(element_0.eventName, appendElementFor$lambda$lambda$lambda(closure$send_0, element_0));
        else if (Kotlin.isType(element_0, Attribute$ClassName))
          addClass($receiver, [element_0.value]);
        else if (Kotlin.isType(element_0, Attribute$Named))
          $receiver.setAttribute(element_0.name, element_0.value);
        else
          Kotlin.noWhenBranchMatched();
      }
      return Unit;
    };
  }
  function appendElementFor$lambda_0(closure$send, closure$mappedView) {
    return function (it) {
      closure$send(closure$mappedView.mapping(it));
      return Unit;
    };
  }
  function appendElementFor($receiver, view, send) {
    var tmp$;
    if (Kotlin.isType(view, View$Element))
      appendElement($receiver, view.elementName, appendElementFor$lambda(view, send));
    else if (Kotlin.isType(view, View$MappedView)) {
      var mappedView = Kotlin.isType(tmp$ = view, View$MappedView) ? tmp$ : throwCCE();
      appendElementFor($receiver, mappedView.view, appendElementFor$lambda_0(send, mappedView));
    }
     else if (equals(view, View$Empty_getInstance()))
      return;
    else
      Kotlin.noWhenBranchMatched();
  }
  function TreeView() {
    TreeView_instance = this;
  }
  function TreeView$Model(tree) {
    this.tree = tree;
  }
  TreeView$Model.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Model',
    interfaces: []
  };
  TreeView$Model.prototype.component1 = function () {
    return this.tree;
  };
  TreeView$Model.prototype.copy_seadfp$ = function (tree) {
    return new TreeView$Model(tree === void 0 ? this.tree : tree);
  };
  TreeView$Model.prototype.toString = function () {
    return 'Model(tree=' + Kotlin.toString(this.tree) + ')';
  };
  TreeView$Model.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.tree) | 0;
    return result;
  };
  TreeView$Model.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.tree, other.tree))));
  };
  function TreeView$Intent() {
  }
  function TreeView$Intent$Toggle(focus) {
    TreeView$Intent.call(this);
    this.focus = focus;
  }
  TreeView$Intent$Toggle.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Toggle',
    interfaces: [TreeView$Intent]
  };
  TreeView$Intent$Toggle.prototype.component1 = function () {
    return this.focus;
  };
  TreeView$Intent$Toggle.prototype.copy_6uxthr$ = function (focus) {
    return new TreeView$Intent$Toggle(focus === void 0 ? this.focus : focus);
  };
  TreeView$Intent$Toggle.prototype.toString = function () {
    return 'Toggle(focus=' + Kotlin.toString(this.focus) + ')';
  };
  TreeView$Intent$Toggle.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.focus) | 0;
    return result;
  };
  TreeView$Intent$Toggle.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.focus, other.focus))));
  };
  TreeView$Intent.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Intent',
    interfaces: []
  };
  function TreeView$view$lambda$lambda$lambda(closure$focus) {
    return function (it) {
      return new TreeView$Intent$Toggle(closure$focus);
    };
  }
  function TreeView$view$lambda$lambda(closure$focus) {
    return function ($receiver) {
      $receiver.onClick_os4ted$(TreeView$view$lambda$lambda$lambda(closure$focus));
      return Unit;
    };
  }
  function TreeView$view$lambda(focus) {
    return div.invoke_h5n6wx$(attributes(TreeView$view$lambda$lambda(focus)), toString(focus.tree.label));
  }
  TreeView.prototype.view_vn2lui$ = function (model) {
    return viewTree(model.tree.focus(), TreeView$view$lambda);
  };
  function TreeView$step$lambda($receiver) {
    return $receiver.copy_sc9rlb$(void 0, void 0, $receiver.state.toggle());
  }
  TreeView.prototype.step_byll7j$ = function (intent, model) {
    if (Kotlin.isType(intent, TreeView$Intent$Toggle))
      return model.copy_seadfp$(intent.focus.update_qxu3nk$(TreeView$step$lambda));
    else
      return Kotlin.noWhenBranchMatched();
  };
  TreeView.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'TreeView',
    interfaces: []
  };
  var TreeView_instance = null;
  function TreeView_getInstance() {
    if (TreeView_instance === null) {
      new TreeView();
    }
    return TreeView_instance;
  }
  function Tree(label, children, state) {
    if (children === void 0)
      children = emptyList();
    if (state === void 0)
      state = Tree$ViewState$Collapsed_getInstance();
    this.label = label;
    this.children = children;
    this.state = state;
  }
  function Tree$ViewState(name, ordinal) {
    Enum.call(this);
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Tree$ViewState_initFields() {
    Tree$ViewState_initFields = function () {
    };
    Tree$ViewState$Collapsed_instance = new Tree$ViewState('Collapsed', 0);
    Tree$ViewState$Expanded_instance = new Tree$ViewState('Expanded', 1);
  }
  var Tree$ViewState$Collapsed_instance;
  function Tree$ViewState$Collapsed_getInstance() {
    Tree$ViewState_initFields();
    return Tree$ViewState$Collapsed_instance;
  }
  var Tree$ViewState$Expanded_instance;
  function Tree$ViewState$Expanded_getInstance() {
    Tree$ViewState_initFields();
    return Tree$ViewState$Expanded_instance;
  }
  Tree$ViewState.prototype.toggle = function () {
    switch (this.name) {
      case 'Collapsed':
        return Tree$ViewState$Expanded_getInstance();
      case 'Expanded':
        return Tree$ViewState$Collapsed_getInstance();
      default:return Kotlin.noWhenBranchMatched();
    }
  };
  Tree$ViewState.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ViewState',
    interfaces: [Enum]
  };
  function Tree$ViewState$values() {
    return [Tree$ViewState$Collapsed_getInstance(), Tree$ViewState$Expanded_getInstance()];
  }
  Tree$ViewState.values = Tree$ViewState$values;
  function Tree$ViewState$valueOf(name) {
    switch (name) {
      case 'Collapsed':
        return Tree$ViewState$Collapsed_getInstance();
      case 'Expanded':
        return Tree$ViewState$Expanded_getInstance();
      default:throwISE('No enum constant elmish.tree.Tree.ViewState.' + name);
    }
  }
  Tree$ViewState.valueOf_61zpoe$ = Tree$ViewState$valueOf;
  Tree.prototype.focus = function () {
    return new Tree$Focus$Original(this);
  };
  Tree.prototype.isNotEmpty = function () {
    return !this.children.isEmpty();
  };
  function Tree$Focus() {
  }
  Object.defineProperty(Tree$Focus.prototype, 'children', {
    get: function () {
      return map(asSequence(get_indices(this.tree.children)), getCallableRef('child', function ($receiver, index) {
        return $receiver.child_za3lpa$(index);
      }.bind(null, this)));
    }
  });
  Tree$Focus.prototype.child_za3lpa$ = function (index) {
    return new Tree$Focus$Child(this, index, this.tree.children.get_za3lpa$(index));
  };
  function Tree$Focus$Original(tree) {
    Tree$Focus.call(this);
    this.tree_7gv33p$_0 = tree;
  }
  Object.defineProperty(Tree$Focus$Original.prototype, 'tree', {
    get: function () {
      return this.tree_7gv33p$_0;
    }
  });
  Tree$Focus$Original.prototype.update_qxu3nk$ = function (f) {
    return f(this.tree);
  };
  Tree$Focus$Original.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Original',
    interfaces: [Tree$Focus]
  };
  Tree$Focus$Original.prototype.component1 = function () {
    return this.tree;
  };
  Tree$Focus$Original.prototype.copy_seadfp$ = function (tree) {
    return new Tree$Focus$Original(tree === void 0 ? this.tree : tree);
  };
  Tree$Focus$Original.prototype.toString = function () {
    return 'Original(tree=' + Kotlin.toString(this.tree) + ')';
  };
  Tree$Focus$Original.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.tree) | 0;
    return result;
  };
  Tree$Focus$Original.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.tree, other.tree))));
  };
  function Tree$Focus$Child(parent, index, tree) {
    Tree$Focus.call(this);
    this.parent = parent;
    this.index = index;
    this.tree_pt1on6$_0 = tree;
  }
  Object.defineProperty(Tree$Focus$Child.prototype, 'tree', {
    get: function () {
      return this.tree_pt1on6$_0;
    }
  });
  function Tree$Focus$Child$update$lambda(this$Child, closure$f) {
    return function ($receiver) {
      var tmp$ = void 0;
      var $receiver_0 = $receiver.children;
      var index = this$Child.index;
      var transform = closure$f;
      var destination = ArrayList_init(collectionSizeOrDefault($receiver_0, 10));
      var tmp$_0, tmp$_0_0;
      var index_0 = 0;
      tmp$_0 = $receiver_0.iterator();
      while (tmp$_0.hasNext()) {
        var item = tmp$_0.next();
        destination.add_11rb$(index === checkIndexOverflow((tmp$_0_0 = index_0, index_0 = tmp$_0_0 + 1 | 0, tmp$_0_0)) ? transform(item) : item);
      }
      return $receiver.copy_sc9rlb$(tmp$, destination);
    };
  }
  Tree$Focus$Child.prototype.update_qxu3nk$ = function (f) {
    return this.parent.update_qxu3nk$(Tree$Focus$Child$update$lambda(this, f));
  };
  Tree$Focus$Child.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Child',
    interfaces: [Tree$Focus]
  };
  Tree$Focus$Child.prototype.component1 = function () {
    return this.parent;
  };
  Tree$Focus$Child.prototype.component2 = function () {
    return this.index;
  };
  Tree$Focus$Child.prototype.component3 = function () {
    return this.tree;
  };
  Tree$Focus$Child.prototype.copy_vzl9jg$ = function (parent, index, tree) {
    return new Tree$Focus$Child(parent === void 0 ? this.parent : parent, index === void 0 ? this.index : index, tree === void 0 ? this.tree : tree);
  };
  Tree$Focus$Child.prototype.toString = function () {
    return 'Child(parent=' + Kotlin.toString(this.parent) + (', index=' + Kotlin.toString(this.index)) + (', tree=' + Kotlin.toString(this.tree)) + ')';
  };
  Tree$Focus$Child.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.parent) | 0;
    result = result * 31 + Kotlin.hashCode(this.index) | 0;
    result = result * 31 + Kotlin.hashCode(this.tree) | 0;
    return result;
  };
  Tree$Focus$Child.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.parent, other.parent) && Kotlin.equals(this.index, other.index) && Kotlin.equals(this.tree, other.tree)))));
  };
  Tree$Focus.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Focus',
    interfaces: []
  };
  Tree.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Tree',
    interfaces: []
  };
  Tree.prototype.component1 = function () {
    return this.label;
  };
  Tree.prototype.component2 = function () {
    return this.children;
  };
  Tree.prototype.component3 = function () {
    return this.state;
  };
  Tree.prototype.copy_sc9rlb$ = function (label, children, state) {
    return new Tree(label === void 0 ? this.label : label, children === void 0 ? this.children : children, state === void 0 ? this.state : state);
  };
  Tree.prototype.toString = function () {
    return 'Tree(label=' + Kotlin.toString(this.label) + (', children=' + Kotlin.toString(this.children)) + (', state=' + Kotlin.toString(this.state)) + ')';
  };
  Tree.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.label) | 0;
    result = result * 31 + Kotlin.hashCode(this.children) | 0;
    result = result * 31 + Kotlin.hashCode(this.state) | 0;
    return result;
  };
  Tree.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.label, other.label) && Kotlin.equals(this.children, other.children) && Kotlin.equals(this.state, other.state)))));
  };
  function viewTree(focus, viewLabel) {
    return ul.invoke_ecd2jv$([viewSubTree(focus, viewLabel)]);
  }
  function viewSubTree(focus, viewLabel) {
    var $receiver = focus.tree;
    var tmp$;
    var tmp$_0;
    tmp$ = viewLabel(focus);
    var $receiver_0 = $receiver.children;
    var tmp$_1 = $receiver.state === Tree$ViewState$Expanded_getInstance();
    if (tmp$_1) {
      tmp$_1 = !$receiver_0.isEmpty();
    }
    return li.invoke_ecd2jv$([tmp$, (tmp$_0 = (tmp$_1 ? $receiver_0 : null) != null ? viewExpanded(focus, viewLabel) : null) != null ? tmp$_0 : empty]);
  }
  function viewExpanded(focus, viewLabel) {
    return ul.invoke_wmgtvr$(viewChildrenOf(focus, viewLabel));
  }
  function viewChildrenOf(focus, viewLabel) {
    return viewSubTrees(focus.children, viewLabel);
  }
  function viewSubTrees$lambda(closure$viewLabel) {
    return function (subTree) {
      return viewSubTree(subTree, closure$viewLabel);
    };
  }
  function viewSubTrees(subTrees, viewLabel) {
    return toList(map(subTrees, viewSubTrees$lambda(viewLabel)));
  }
  ProblemNode.Error = ProblemNode$Error;
  ProblemNode.Warning = ProblemNode$Warning;
  ProblemNode.Task = ProblemNode$Task;
  ProblemNode.Bean = ProblemNode$Bean;
  ProblemNode.Property = ProblemNode$Property;
  ProblemNode.Label = ProblemNode$Label;
  ProblemNode.Message = ProblemNode$Message;
  ProblemNode.Exception = ProblemNode$Exception;
  _.ProblemNode = ProblemNode;
  PrettyText$Fragment.Text = PrettyText$Fragment$Text;
  PrettyText$Fragment.Reference = PrettyText$Fragment$Reference;
  PrettyText.Fragment = PrettyText$Fragment;
  _.PrettyText = PrettyText;
  InstantExecutionReportPage.prototype.Model = InstantExecutionReportPage$Model;
  Object.defineProperty(InstantExecutionReportPage$DisplayFilter, 'All', {
    get: InstantExecutionReportPage$DisplayFilter$All_getInstance
  });
  Object.defineProperty(InstantExecutionReportPage$DisplayFilter, 'Errors', {
    get: InstantExecutionReportPage$DisplayFilter$Errors_getInstance
  });
  Object.defineProperty(InstantExecutionReportPage$DisplayFilter, 'Warnings', {
    get: InstantExecutionReportPage$DisplayFilter$Warnings_getInstance
  });
  InstantExecutionReportPage.prototype.DisplayFilter = InstantExecutionReportPage$DisplayFilter;
  InstantExecutionReportPage$Intent.TaskTreeIntent = InstantExecutionReportPage$Intent$TaskTreeIntent;
  InstantExecutionReportPage$Intent.MessageTreeIntent = InstantExecutionReportPage$Intent$MessageTreeIntent;
  InstantExecutionReportPage$Intent.Copy = InstantExecutionReportPage$Intent$Copy;
  InstantExecutionReportPage$Intent.SetFilter = InstantExecutionReportPage$Intent$SetFilter;
  InstantExecutionReportPage.prototype.Intent = InstantExecutionReportPage$Intent;
  Object.defineProperty(_, 'InstantExecutionReportPage', {
    get: InstantExecutionReportPage_getInstance
  });
  _.main = main;
  $$importsForInline$$['instant-execution-report'] = _;
  var package$data = _.data || (_.data = {});
  package$data.mapAt_5avcl8$ = mapAt;
  Object.defineProperty(Trie, 'Companion', {
    get: Trie$Companion_getInstance
  });
  package$data.Trie = Trie;
  var package$elmish = _.elmish || (_.elmish = {});
  package$elmish.Component = Component;
  package$elmish.component_k7z6jj$ = component;
  package$elmish.mountComponentAt_9kj4xw$ = mountComponentAt;
  package$elmish.elementById_61zpoe$ = elementById;
  Object.defineProperty(package$elmish, 'empty', {
    get: function () {
      return empty;
    }
  });
  Object.defineProperty(package$elmish, 'h1', {
    get: function () {
      return h1;
    }
  });
  Object.defineProperty(package$elmish, 'h2', {
    get: function () {
      return h2;
    }
  });
  Object.defineProperty(package$elmish, 'div', {
    get: function () {
      return div;
    }
  });
  Object.defineProperty(package$elmish, 'pre', {
    get: function () {
      return pre;
    }
  });
  Object.defineProperty(package$elmish, 'code', {
    get: function () {
      return code;
    }
  });
  Object.defineProperty(package$elmish, 'span', {
    get: function () {
      return span;
    }
  });
  Object.defineProperty(package$elmish, 'small', {
    get: function () {
      return small;
    }
  });
  Object.defineProperty(package$elmish, 'ol', {
    get: function () {
      return ol;
    }
  });
  Object.defineProperty(package$elmish, 'ul', {
    get: function () {
      return ul;
    }
  });
  Object.defineProperty(package$elmish, 'li', {
    get: function () {
      return li;
    }
  });
  Object.defineProperty(package$elmish, 'a', {
    get: function () {
      return a;
    }
  });
  package$elmish.render_jv83zc$ = render;
  package$elmish.ViewFactory = ViewFactory;
  Object.defineProperty(View, 'Companion', {
    get: View$Companion_getInstance
  });
  Object.defineProperty(View, 'Empty', {
    get: View$Empty_getInstance
  });
  View.Element = View$Element;
  View.MappedView = View$MappedView;
  package$elmish.View = View;
  package$elmish.attributes_v53lvq$ = attributes;
  package$elmish.Attributes = Attributes;
  Attribute.OnEvent = Attribute$OnEvent;
  Attribute.ClassName = Attribute$ClassName;
  Attribute.Named = Attribute$Named;
  package$elmish.Attribute = Attribute;
  TreeView.prototype.Model = TreeView$Model;
  TreeView$Intent.Toggle = TreeView$Intent$Toggle;
  TreeView.prototype.Intent = TreeView$Intent;
  var package$tree = package$elmish.tree || (package$elmish.tree = {});
  Object.defineProperty(package$tree, 'TreeView', {
    get: TreeView_getInstance
  });
  Object.defineProperty(Tree$ViewState, 'Collapsed', {
    get: Tree$ViewState$Collapsed_getInstance
  });
  Object.defineProperty(Tree$ViewState, 'Expanded', {
    get: Tree$ViewState$Expanded_getInstance
  });
  Tree.ViewState = Tree$ViewState;
  Tree$Focus.Original = Tree$Focus$Original;
  Tree$Focus.Child = Tree$Focus$Child;
  Tree.Focus = Tree$Focus;
  package$tree.Tree = Tree;
  package$tree.viewTree_rojxr8$ = viewTree;
  package$tree.viewSubTree_rojxr8$ = viewSubTree;
  package$tree.viewExpanded_rojxr8$ = viewExpanded;
  package$tree.viewChildrenOf_kk79nd$ = viewChildrenOf;
  package$tree.viewSubTrees_9pvd5a$ = viewSubTrees;
  empty = View$Empty_getInstance();
  h1 = new ViewFactory('h1');
  h2 = new ViewFactory('h2');
  div = new ViewFactory('div');
  pre = new ViewFactory('pre');
  code = new ViewFactory('code');
  span = new ViewFactory('span');
  small = new ViewFactory('small');
  ol = new ViewFactory('ol');
  ul = new ViewFactory('ul');
  li = new ViewFactory('li');
  a = new ViewFactory('a');
  main();
  Kotlin.defineModule('instant-execution-report', _);
  return _;
}(typeof this['instant-execution-report'] === 'undefined' ? {} : this['instant-execution-report'], kotlin);
