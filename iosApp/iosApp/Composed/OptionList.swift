import Foundation
import SwiftUI
import ComposeApp

struct OptionList<Content : View, Item : Option>: View {
    @Environment(\.editMode) var editMode: Binding<EditMode>?
    @ObservedObject var data: OptionListData<Item>
    @Binding private var selection: Set<Int64>
    var onDelete: (Set<Int64>) -> Void
    var content: (Item) -> Content
    
    init(data: OptionListData<Item>, selection: Binding<Set<Int64>> = Binding.constant(Set()), onDelete: @escaping (Set<Int64>) -> Void, content: @escaping (Item) -> Content) {
        self.data = data
        self._selection = selection
        self.onDelete = onDelete
        self.content = content
    }
    
    @State private var searchText: String = ""
    @State private var sorting: OptionListSort = .name(.acending)
    
    private var itemModel: [Item] {
        let filtered =
        if searchText.isEmpty { data.items }
        else { data.items.filter { $0.view.header.contains(searchText) || $0.view.title?.contains(searchText) == true || $0.view.subtitle?.contains(searchText) == true } }
        
        let sorted: [Item] = switch sorting {
        case .name(.acending):
            filtered.sorted { ($0.kt as! any NameComparable).nameCompare < ($1.kt as! any NameComparable).nameCompare }
        case .name(.decending):
            filtered.sorted { ($0.kt as! any NameComparable).nameCompare > ($1.kt as! any NameComparable).nameCompare }
        case .modification(.acending):
            filtered.sorted { ($0.kt as! any ModificationComparable).modificationCompare < ($1.kt as! any ModificationComparable).modificationCompare }
        case .modification(.decending):
            filtered.sorted { ($0.kt as! any ModificationComparable).modificationCompare > ($1.kt as! any ModificationComparable).modificationCompare }
        case .creation(.acending):
            filtered.sorted { ($0.kt as! any CreationComparable).creationCompare < ($1.kt as! any CreationComparable).creationCompare }
        case .creation(.decending):
            filtered.sorted { ($0.kt as! any CreationComparable).creationCompare > ($1.kt as! any CreationComparable).creationCompare }
        }
        
        return sorted
    }

    var body: some View {
        Group {
            if data.items.isEmpty {
                OptionListPlaceholder()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(.background)
            } else {
                List(itemModel, selection: $selection) { option in
                    content(option)
                }
                .listStyle(.plain)
                .toolbar {
                    if editMode?.wrappedValue.isEditing == false {
                        ToolbarItem {
                            Menu("More", systemImage: "ellipsis.circle") {
                                Button("Select", systemImage: "checkmark.circle") {
                                    withAnimation {
                                        editMode?.wrappedValue = .active
                                    }
                                }
                                
                                Divider()
                                
                                if Item.KtType.self is any NameComparable.Type {
                                    Toggle(isOn: Binding(get: {
                                        switch sorting {
                                        case .name(_):
                                            true
                                        default:
                                            false
                                        }
                                    }, set: { _ in
                                        sorting = switch sorting {
                                        case .name(.acending):
                                                .name(.decending)
                                        default:
                                                .name(.acending)
                                        }
                                    }), label: {
                                        switch sorting {
                                        case .name(.acending):
                                            Label("Name", systemImage: "chevron.up")
                                        case .name(.decending):
                                            Label("Name", systemImage: "chevron.down")
                                        default:
                                            Text("Name")
                                        }
                                    })
                                }
                                
                                if Item.KtType.self is any ModificationComparable.Type {
                                    Toggle(isOn: Binding(get: {
                                        switch sorting {
                                        case .modification(_):
                                            true
                                        default:
                                            false
                                        }
                                    }, set: { _ in
                                        sorting = switch sorting {
                                        case .modification(.acending):
                                                .modification(.decending)
                                        default:
                                                .modification(.acending)
                                        }
                                    }), label: {
                                        switch sorting {
                                        case .modification(.acending):
                                            Label("Modification", systemImage: "chevron.up")
                                        case .modification(.decending):
                                            Label("Modification", systemImage: "chevron.down")
                                        default:
                                            Text("Modification")
                                        }
                                    })
                                }
                                
                                if Item.KtType.self is any CreationComparable.Type {
                                        Toggle(isOn: Binding(get: {
                                            switch sorting {
                                            case .creation(_):
                                                true
                                            default:
                                                false
                                            }
                                        }, set: { _ in
                                            sorting = switch sorting {
                                            case .creation(.acending):
                                                    .creation(.decending)
                                            default:
                                                    .creation(.acending)
                                            }
                                    }), label: {
                                        switch sorting {
                                        case .creation(.acending):
                                            Label("Creation", systemImage: "chevron.up")
                                        case .creation(.decending):
                                            Label("Creation", systemImage: "chevron.down")
                                        default:
                                            Text("Creation")
                                        }
                                    })
                                }
                            }
                        }
                    } else {
                        ToolbarItem(placement: .topBarTrailing) {
                            if selection.count == data.items.count {
                                Button("Deselect All") {
                                    selection = Set()
                                }
                            } else {
                                Button("Select All") {
                                    selection = Set(data.items.map(\.id))
                                }
                            }
                        }
                        
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("Done") {
                                withAnimation {
                                    editMode?.wrappedValue = .inactive
                                }
                            }
                        }
                        
                        ToolbarItem(placement: .bottomBar) {
                            Button("Delete", role: .destructive) {
                                onDelete(selection)
                            }
                            .disabled(selection.isEmpty)
                        }
                    }
                }
                .searchable(text: $searchText)
            }
        }
        .toolbar {
            if data.isRefreshing {
                ToolbarItem(placement: .primaryAction) {
                    ProgressView()
                }
            }
        }
    }
}

#Preview {
    let future = Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE
    let items: [OptionImpl<QuizOption>] = (0...10).map { i in
        return OptionImpl(kt: QuizOption(quiz: Quiz(id: Int64(i), name: "Sample \(i)", creationTimeISO: future, modificationTimeISO: future), preview: "Lore Ipsum"))
    }
    
    OptionList(
        data: OptionListData(items: items),
        onDelete: { _ in },
        content: { option in
            OptionListItem(data: option)
        }
    )
}
