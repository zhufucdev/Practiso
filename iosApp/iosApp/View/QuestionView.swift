import Foundation
import SwiftUI
@preconcurrency import ComposeApp
import UniformTypeIdentifiers

struct QuestionView: View {
    private let archiveService = ImportService(db: Database.shared.app)
    private let importService = ImportServiceSync(db: Database.shared.app)
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    private let createService = CreateService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel
    
    @State var data = OptionListData<OptionImpl<QuizOption>>()
    
    @State private var isArchiveImporterShown = false
    @State private var isDeletingDialogShown = false
    @State private var editMode: EditMode = .inactive
    @State private var selection = Set<Int64>()

    var body: some View {
        OptionList(
            data: data, selection: Binding(get: {
                selection
            }, set: { newValue in
                if !editMode.isEditing, let id = newValue.first {
                    contentModel.detail = .question(data.items.first(where: {$0.id == id})!.kt)
                }
                selection = newValue
            }),
            onDelete: { options in
                isDeletingDialogShown = true
            }
        ) { option in
            OptionListItem(data: option)
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) {
                        errorHandler.catchAndShowImmediately {
                            try removeService.removeQuizWithResources(id: option.kt.id)
                        }
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
                .draggable(option.kt)
                .contextMenu {
                    Button("Delete \(option.view.header)", systemImage: "trash", role: .destructive) {
                        errorHandler.catchAndShowImmediately {
                            try removeService.removeQuizWithResources(id: option.kt.id)
                        }
                    }
                } preview: {
                    QuestionPreview(data: option.kt.quiz)
                        .padding()
                }
        }
        .confirmationDialog("Deleting \(selection.count) questions", isPresented: $isDeletingDialogShown) {
            Button("Delete", role: .destructive) {
                errorHandler.catchAndShowImmediately {
                    for quizId in selection {
                        try removeService.removeQuizWithResources(id: quizId)
                    }
                }
                editMode = .inactive
            }
            Button("Cancel", role: .cancel) {
                isDeletingDialogShown = false
            }
        } message: {
            if selection.count > 1 {
                Text("Would you like to delete these questions? This operation is inreversible.")
            } else if selection.count > 0 {
                if let selectedId = selection.first {
                    if let selectedName = data.items.first(where: { $0.id == selectedId  })?.view.header {
                        Text("Would you like to remove \"\(selectedName)\"? This operation is inreversible.")
                    }
                }
            }
        }
        .toolbar {
            if editMode == .inactive {
                ToolbarItem(placement: .primaryAction) {
                    Menu("Add", systemImage: "plus") {
                        Button("Import Archive", systemImage: "square.and.arrow.down.on.square") {
                            isArchiveImporterShown = true
                        }
                        
                        Button("Create", systemImage: "document.badge.plus") {
                            Task {
                                let created = try await createService.createNewQuiz()
                                contentModel.detail = .question(created)
                            }
                        }
                    }
                }
            }
        }
        .environment(\.editMode, $editMode)
        .dropDestination(for: CopiedTransfer.self) { data, _ in
            processImport(items: data)
        }
        .task {
            data.isRefreshing = true
            let service = LibraryService(db: Database.shared.app)
            for await items in service.getQuizzes() {
                DispatchQueue.main.schedule {
                    withAnimation {
                        data.items = items.map(OptionImpl.init)
                    }
                }
                data.isRefreshing = false
            }
        }
        .fileImporter(isPresented: $isArchiveImporterShown, allowedContentTypes: [.data], allowsMultipleSelection: true) { result in
            if let data = try? result.get() {
                _ = processImport(items: data.map { url in .url(url: url) })
            }
        }
    }
    
    private func processImport(items: [CopiedTransfer]) -> Bool {
        var packs: [ArchivePack] = []
        packs.reserveCapacity(items.count)
        
        for item in items {
            switch item {
            case .binary(let data):
                if let pack = (errorHandler.catchAndShowImmediately {
                    try archiveService.unarchive(it: NamedSource(data: data))
                }) {
                    packs.append(pack)
                } else {
                    return true
                }
            case .url(let url):
                if let pack = (errorHandler.catchAndShowImmediately {
                    try archiveService.unarchive(it: NamedSource(url: url))
                }) {
                    packs.append(pack)
                } else {
                    return true
                }
            case .error(let description):
                errorHandler.show(message: description)
                return true
            }
        }
        
        for pack in packs {
            errorHandler.catchAndShowImmediately {
                try importService.importAll(pack: pack)
            }
        }
        
        return true
    }
}
